/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.core.AgentNotSupportedException;
import org.rhq.core.clientapi.server.core.AgentRegistrationException;
import org.rhq.core.clientapi.server.core.AgentRegistrationRequest;
import org.rhq.core.clientapi.server.core.AgentRegistrationResults;
import org.rhq.core.clientapi.server.core.AgentVersion;
import org.rhq.core.clientapi.server.core.ConnectAgentRequest;
import org.rhq.core.clientapi.server.core.ConnectAgentResults;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cluster.FailoverListManagerLocal;
import org.rhq.enterprise.server.cluster.PartitionEventManagerLocal;
import org.rhq.enterprise.server.cluster.instance.ServerManagerLocal;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The service that receives agent requests to perform functionality such as registering agents.
 *
 * @author John Mazzitelli
 */
public class CoreServerServiceImpl implements CoreServerService {
    private final Log log = LogFactory.getLog(CoreServerServiceImpl.class);

    private AgentManagerLocal agentManager;
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
    private FailoverListManagerLocal failoverListManager;
    private PartitionEventManagerLocal partitionEventManager;
    private ServerManagerLocal serverManager;
    private SubjectManagerLocal subjectManager;

    /**
     * @see CoreServerService#registerAgent(AgentRegistrationRequest)
     */
    public AgentRegistrationResults registerAgent(AgentRegistrationRequest request) throws AgentRegistrationException,
        AgentNotSupportedException {

        // fail-fast if we can't even support this agent
        if (!getAgentManager().isAgentVersionSupported(request.getAgentVersion())) {
            log.warn("Agent [" + request.getName() + "][" + request.getAddress() + ':' + request.getPort() + "]["
                + request.getAgentVersion() + "] would like to register with this server but it is not supported");
            throw new AgentNotSupportedException("Unsupported agent version: " + request.getAgentVersion());
        }

        // Make a very quick test to verify the agent's remote endpoint can be connected to.
        // If not, no point in continuing - the server won't be able to talk to the agent anyway.
        pingEndpoint(request.getRemoteEndpoint());

        // TODO (ghinkle): Check platform limit - do we still care about this?
        //getPlatformManager().enforceLicenseLimit(args.getCpuCount());

        Agent agentByName = getAgentManager().getAgentByName(request.getName());

        /*
         * As part of the registration request, a previously registered agent should send its current token (aka the
         * original token). We will check this original token in the database and see what agent name is associated with
         * it.  If it is different than the name of the agent that is asking for this registration, we abort. In effect,
         * we are telling the agent "you are already registered under the name "A" - you cannot change your name to
         * something else".
         *
         * If there is no original token with the request, this is either a brand new agent never before registered, or it
         * is an agent that has been registered before but for some reason lost its token. In this case, we will look at
         * this registration's host/port of this new agent.  If it matches the host/port of another agent but the
         * existing name and the new agent's name don't match, the server will abort and tell the agent, "You don't know
         * who you are, but I know there is already an agent with the host and port you are trying to register with
         * under a different name - so I'm going to trust this original agent and not allow you to register that name
         * under a different host/port. If you are that original agent, then you need to register with that original
         * name". When the agent registers again, this time with the correct name, the agent will be given its token.
         * This usually will occur if you reinstall the agent and try to register it under a different name.
         */

        if (request.getOriginalToken() != null) {
            Agent agentByToken = getAgentManager().getAgentByAgentToken(request.getOriginalToken());

            if (agentByToken != null) {
                if (!agentByToken.getName().equals(request.getName())) {
                    String msg = "The agent asking for registration is already registered with the name ["
                        + agentByToken.getName() + "], it cannot change its name to [" + request.getName() + "]";
                    throw new AgentRegistrationException(msg);
                }
            }
        } else {
            Agent agentByAddressPort = getAgentManager().getAgentByAddressAndPort(request.getAddress(),
                request.getPort());
            if (agentByAddressPort != null) {
                if (!agentByAddressPort.getName().equals(request.getName())) {
                    String msg = "The agent asking for registration is trying to register the same address/port ["
                        + request.getAddress()
                        + ":"
                        + request.getPort()
                        + "] that is already registered under a different name ["
                        + agentByAddressPort.getName()
                        + "]; if this new agent is actually the same as the original, then re-register with the same name";
                    throw new AgentRegistrationException(msg);
                }
            }
        }

        Server registeringServer = getServerManager().getServer();

        if (agentByName != null) {
            log.info("Got agent registration request for existing agent: " + agentByName.getName() + "["
                + agentByName.getAddress() + ":" + agentByName.getPort() + "][" + request.getAgentVersion()
                + "] - Will " + (request.getRegenerateToken() ? "" : "not") + " regenerate a new token");

            agentByName.setServer(registeringServer);
            agentByName.setAddress(request.getAddress());
            agentByName.setPort(request.getPort());
            agentByName.setRemoteEndpoint(request.getRemoteEndpoint());

            if (request.getRegenerateToken()) {
                agentByName.setAgentToken(Agent.generateRandomToken(request.getName()));
            }

            try {
                agentManager.updateAgent(agentByName);
            } catch (Exception e) {
                log.warn("Could not update the agent in database", e);
                throw new AgentRegistrationException(new WrappedRemotingException(e));
            }
        } else {
            log.info("Got agent registration request for new agent: " + request.getName() + "[" + request.getAddress()
                + ":" + request.getPort() + "][" + request.getAgentVersion() + "]");

            // the agent does not yet exist, we need to create it
            try {
                agentByName = new Agent(request.getName(), request.getAddress(), request.getPort(), request
                    .getRemoteEndpoint(), Agent.generateRandomToken(request.getName()));

                agentByName.setServer(registeringServer);
                agentManager.createAgent(agentByName);
            } catch (Exception e) {
                log.warn("Failed to create agent in database", e);
                throw new AgentRegistrationException(new WrappedRemotingException(e));
            }
        }

        // get existing or generate new server list for the registering agent
        FailoverListComposite failoverList = getPartitionEventManager().agentPartitionEvent(
            getSubjectManager().getOverlord(), agentByName.getName(), PartitionEventType.AGENT_REGISTRATION,
            agentByName.getName() + " - " + registeringServer.getName());

        AgentRegistrationResults results = new AgentRegistrationResults();
        results.setAgentToken(agentByName.getAgentToken());
        results.setFailoverList(failoverList);

        return results;
    }

    /**
     * @see CoreServerService#connectAgent(ConnectAgentRequest)
     */
    public ConnectAgentResults connectAgent(ConnectAgentRequest request) throws AgentRegistrationException,
        AgentNotSupportedException {

        String agentName = request.getAgentName();
        AgentVersion agentVersion = request.getAgentVersion();

        log.info("Agent [" + agentName + "][" + agentVersion + "] would like to connect to this server");

        if (!getAgentManager().isAgentVersionSupported(agentVersion)) {
            throw new AgentNotSupportedException("Agent [" + agentName + "] is an unsupported agent: " + agentVersion);
        }

        Agent agent = getAgentManager().getAgentByName(agentName);
        if (agent == null) {
            throw new AgentRegistrationException("Agent [" + agentName + "] is not registered");
        }

        Server server = getServerManager().getServer();
        agent.setServer(server);
        getAgentManager().updateAgent(agent);

        getAlertConditionCacheManager().reloadCachesForAgent(agent.getId());

        getPartitionEventManager().auditPartitionEvent(getSubjectManager().getOverlord(),
            PartitionEventType.AGENT_CONNECT, agentName + " - " + server.getName());

        log.info("Agent [" + agentName + "] has connected to this server at " + new Date());
        return new ConnectAgentResults(System.currentTimeMillis());
    }

    /**
     * @see CoreServerService#getLatestPlugins()
     */
    @SuppressWarnings("unchecked")
    public List<Plugin> getLatestPlugins() {
        EntityManager em = null;
        List<Plugin> plugins = new ArrayList<Plugin>();

        try {
            em = LookupUtil.getEntityManager();
            Query q = em.createNamedQuery("Plugin.findAll");
            plugins.addAll(q.getResultList());
        } catch (Exception e) {
            log.warn("Failed to get the list of latest plugins", e);
            throw new WrappedRemotingException(e);
        } finally {
            if (em != null) {
                em.close();
            }
        }

        return plugins;
    }

    /**
     * @see CoreServerService#getPluginArchive(String)
     */
    public InputStream getPluginArchive(String pluginName) {
        EntityManager em = null;

        try {
            em = LookupUtil.getEntityManager();
            Query q = em.createNamedQuery("Plugin.findByPath");
            q.setParameter("path", pluginName);
            Plugin plugin = (Plugin) q.getSingleResult();

            // we know our plugins are in a subdirectory within our downloads location
            return getFileContents(new File("rhq-plugins", plugin.getPath()).getPath());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * @see CoreServerService#getFileContents(String)
     */
    public InputStream getFileContents(String file) {
        // for security purposes, do not let the agent ask for any file outside of the agent-files location
        if (file.indexOf("..") >= 0) {
            String err_string = "WARNING: agent is attempting to download a file from an invalid location: " + file;
            log.error(err_string);
            throw new IllegalArgumentException(err_string);
        }

        try {
            ServerCommunicationsServiceMBean sc = ServerCommunicationsServiceUtil.getService();

            String dir = sc.getConfiguration().getAgentFilesDirectory();
            File file_to_stream = new File(dir, file);
            FileInputStream fis = new FileInputStream(file_to_stream);
            BufferedInputStream bis = new BufferedInputStream(fis, 1024 * 32);
            RemoteInputStream in = ServerCommunicationsServiceUtil.remoteInputStream(bis);

            return in;
        } catch (Exception e) {
            throw new WrappedRemotingException(e);
        }
    }

    /**
     * @see CoreServerService#agentIsShuttingDown(String)
     */
    public void agentIsShuttingDown(String agentName) {
        log.debug("Agent [" + agentName + "] is sending a notification that it is going down!");

        getAgentManager().agentIsShuttingDown(agentName);

        getPartitionEventManager().auditPartitionEvent(getSubjectManager().getOverlord(),
            PartitionEventType.AGENT_SHUTDOWN, agentName);

        return;
    }

    public FailoverListComposite getFailoverList(String agentName) {
        return getFailoverListManager().getExistingForSingleAgent(agentName);
    }

    private AgentManagerLocal getAgentManager() {
        if (this.agentManager == null) {
            this.agentManager = LookupUtil.getAgentManager();
        }

        return this.agentManager;
    }

    private AlertConditionCacheManagerLocal getAlertConditionCacheManager() {
        if (this.alertConditionCacheManager == null) {
            this.alertConditionCacheManager = LookupUtil.getAlertConditionCacheManager();
        }

        return this.alertConditionCacheManager;
    }

    private FailoverListManagerLocal getFailoverListManager() {
        if (this.failoverListManager == null) {
            this.failoverListManager = LookupUtil.getFailoverListManager();
        }

        return this.failoverListManager;
    }

    private PartitionEventManagerLocal getPartitionEventManager() {
        if (this.partitionEventManager == null) {
            this.partitionEventManager = LookupUtil.getPartitionEventManager();
        }

        return this.partitionEventManager;
    }

    private ServerManagerLocal getServerManager() {
        if (this.serverManager == null) {
            this.serverManager = LookupUtil.getServerManager();
        }

        return this.serverManager;
    }

    private SubjectManagerLocal getSubjectManager() {
        if (this.subjectManager == null) {
            this.subjectManager = LookupUtil.getSubjectManager();
        }

        return this.subjectManager;
    }

    private void pingEndpoint(String endpoint) throws AgentRegistrationException {
        AgentRegistrationException failure = null;

        try {
            ServerCommunicationsServiceMBean sc = ServerCommunicationsServiceUtil.getService();
            boolean ping = sc.pingEndpoint(endpoint, 10000L);

            if (!ping) {
                failure = new AgentRegistrationException("Server cannot ping the agent's endpoint. "
                    + "The agent's endpoint is probably invalid "
                    + "or there is a firewall preventing the server from connecting to the agent. " + "Endpoint: "
                    + endpoint);
            }
        } catch (Exception e) {
            failure = new AgentRegistrationException("Cannot verify agent endpoint due to internal error.",
                new WrappedRemotingException(e));
        }

        if (failure != null) {
            throw failure;
        }

        log.debug("A new agent has passed its endpoint verification test: " + endpoint);

        return;
    }

}