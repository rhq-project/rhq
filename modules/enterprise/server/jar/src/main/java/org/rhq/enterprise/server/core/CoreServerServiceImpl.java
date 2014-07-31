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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
import org.rhq.core.clientapi.server.core.PingRequest;
import org.rhq.core.domain.cloud.PartitionEventType;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.Base64;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.FailoverListManagerLocal;
import org.rhq.enterprise.server.cloud.PartitionEventManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
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

    private SecureRandom random;

    /**
     * @see CoreServerService#registerAgent(AgentRegistrationRequest)
     */
    @Override
    public AgentRegistrationResults registerAgent(AgentRegistrationRequest request) throws AgentRegistrationException,
        AgentNotSupportedException {

        // fail-fast if we can't even support this agent
        if (!getAgentManager().isAgentVersionSupported(request.getAgentVersion()).isSupported()) {
            log.warn("Agent [" + request.getName() + "][" + request.getAddress() + ':' + request.getPort() + "]["
                + request.getAgentVersion() + "] would like to register with this server but it is not supported");
            throw new AgentNotSupportedException("Agent [" + request.getName() + "] is an unsupported agent: "
                + request.getAgentVersion());
        }

        // Make a very quick test to verify the agent's remote endpoint can be connected to.
        // If not, no point in continuing - the server won't be able to talk to the agent anyway.
        pingEndpoint(request.getRemoteEndpoint());

        Agent agentByName = getAgentManager().getAgentByName(request.getName());

        /*
         * As part of the registration request, a previously registered agent should send its current token (aka the
         * original token). We will check this original token in the database and see what agent name is associated with
         * it.  If it is different than the name of the agent that is asking for this registration, we abort. In effect,
         * we are telling the agent "you are already registered under the name "A" - you cannot change your name to
         * something else".
         *
         * If there is no original token with the request, this is either a brand new agent never before registered, or it
         * is an agent that has been registered before but for some reason lost its token.
         * In this case, if there is no agent with the name being requested, we register this as a new agent.
         * If, however, the agent name is already in use, we abort the request. An agent cannot register with an
         * existing agent without sending that agent's security token.
         */

        if (request.getOriginalToken() != null) {
            Agent agentByToken = getAgentManager().getAgentByAgentToken(request.getOriginalToken());

            if (agentByToken != null) {
                if (!agentByToken.getName().equals(request.getName())) {
                    String msg = "The agent asking for registration is already registered with the name ["
                        + agentByToken.getName() + "], it cannot change its name to [" + request.getName() + "]";
                    throw new AgentRegistrationException(msg);
                } else {
                    Agent agentByAddressPort = getAgentManager().getAgentByAddressAndPort(request.getAddress(),
                        request.getPort());
                    if (agentByAddressPort != null && !agentByAddressPort.getName().equals(request.getName())) {
                        // the agent request provided information about an authentic agent but it is trying to 
                        // steal another agent's host/port. Thus, we will abort this request.
                        String msg = "The agent asking for registration [" + request.getName()
                            + "] is trying to register the same address/port [" + request.getAddress() + ":"
                            + request.getPort() + "] that is already registered under a different name ["
                            + agentByAddressPort.getName() + "]";
                        throw new AgentRegistrationException(msg);
                    }
                }
            } else {
                if (agentByName != null) {
                    // the agent request provided a name that already is in use by an agent. However, the request
                    // provided a security token that was not assigned to any agent! How can this be? Something is fishy.
                    String msg = "The agent asking for registration under the name [" + request.getName()
                        + "] provided an invalid security token. This request will fail. " + securityTokenMessage();
                    throw new AgentRegistrationException(msg);
                }
                Agent agentByAddressPort = getAgentManager().getAgentByAddressAndPort(request.getAddress(),
                    request.getPort());
                if (agentByAddressPort != null) {
                    // The agent is requesting to register an unused agent name - so this is considered a new agent.
                    // It provided a security token but it is an unknown/obsolete/bogus token (usually due to the
                    // fact that someone purged the platform/agent from the server database but the old agent is
                    // still around with its old token).
                    // However, the IP/port it wants to use is already in-use. This sounds fishy. If we let this
                    // go through, this agent with an unknown/bogus token will essentially hijack this IP/port
                    // belonging to an existing agent. If the agent wants to reuse an IP/port already in existence, it should
                    // already know its security token associated with that IP/port. Thus, we will abort this request.
                    String msg = "The agent asking for registration under the name [" + request.getName()
                        + "] is attempting to take another agent's address/port [" + request.getAddress() + ":"
                        + request.getPort() + "] with an unknown security token. This request will fail.";
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
                        + "]. If this new agent is actually the same as the original, then re-register with the same name"
                        + " and same security token. Otherwise, you should uninventory the Platform resource for agent ["
                        + agentByAddressPort.getName() + "] and re-register this new agent.";
                    throw new AgentRegistrationException(msg);
                } else {
                    String msg = "The agent [" + request.getName()
                        + "] is attempting to re-register without a security token. " + securityTokenMessage();
                    throw new AgentRegistrationException(msg);

                }
            } else {
                if (agentByName != null) {
                    // the name being registered already exists - but there is no security token to authenticate this request!
                    // Therefore, because this agent name is already registered and because this current request
                    // cannot authenticate itself with the proper security token, we fail.
                    String msg = "An agent is trying to register with an existing agent name ["
                        + request.getName()
                        + "] without providing a valid security token. If you are attempting to re-register this agent, "
                        + "you will need the agent's security token. " + securityTokenMessage();
                    throw new AgentRegistrationException(msg);
                }
            }
        }

        Server registeringServer = getServerManager().getServer();

        // if the agent is registering with a loopback address, log a warning since you probably do not want this in production
        try {
            String address = request.getAddress();
            String fullEndpoint = request.getRemoteEndpoint();
            boolean loopbackRegistration = false;
            if (address.equals("127.0.0.1") || address.equalsIgnoreCase("localhost")) {
                loopbackRegistration = true;
            } else {
                // jboss/remoting transport params might be telling us to have our client connect to a different address
                if (fullEndpoint != null) {
                    if (fullEndpoint.contains("127.0.0.1") || fullEndpoint.contains("localhost")) {
                        loopbackRegistration = true;
                    }
                }
            }

            if (loopbackRegistration) {
                log.warn("An agent [" + request.getName()
                    + "] has registered with a loopback address. This should only be done "
                    + "for testing or demo purposes - this agent can only ever interact with this server. " + request);
            }
        } catch (Exception ignore) {
        }

        if (agentByName != null) {
            log.info("Got agent registration request for existing agent: " + agentByName.getName() + "["
                + agentByName.getAddress() + ":" + agentByName.getPort() + "][" + request.getAgentVersion()
                + "] - Will " + (request.getRegenerateToken() ? "" : "not") + " regenerate a new token");

            final String oldAddress = agentByName.getAddress();
            final int oldPort = agentByName.getPort();
            final String oldRemoteEndpoint = agentByName.getRemoteEndpoint();

            agentByName.setServer(registeringServer);
            agentByName.setAddress(request.getAddress());
            agentByName.setPort(request.getPort());
            agentByName.setRemoteEndpoint(request.getRemoteEndpoint());

            if (request.getRegenerateToken()) {
                agentByName.setAgentToken(generateAgentToken());
            }

            try {
                agentManager.updateAgent(agentByName);
            } catch (Exception e) {
                log.warn("Could not update the agent in database", e);
                throw new AgentRegistrationException(new WrappedRemotingException(e));
            }

            // if agent is re-registering in order to change its remote endpoint, destroy our old client.
            if (!oldAddress.equals(request.getAddress()) || oldPort != request.getPort()
                || !oldRemoteEndpoint.equals(request.getRemoteEndpoint())) {
                try {
                    final Agent oldAgent = new Agent();
                    oldAgent.setName(agentByName.getName());
                    oldAgent.setAddress(oldAddress);
                    oldAgent.setPort(oldPort);
                    oldAgent.setRemoteEndpoint(oldRemoteEndpoint);
                    agentManager.destroyAgentClient(oldAgent);
                } catch (Exception e) {
                    log.warn("Could not destroy the agent client - will continue but agent comm may be broken", e);
                }
            }
        } else {
            log.info("Got agent registration request for new agent: " + request.getName() + "[" + request.getAddress()
                + ":" + request.getPort() + "][" + request.getAgentVersion() + "]");

            // the agent does not yet exist, we need to create it
            try {
                agentByName = new Agent(request.getName(), request.getAddress(), request.getPort(),
                    request.getRemoteEndpoint(), generateAgentToken());

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

    private String securityTokenMessage() {
        return "Please consult an administrator to obtain the agent's proper security token "
            + "and restart the agent with the option \"-Drhq.agent.security-token=<the valid security token>\". "
            + "An administrator can find the agent's security token by navigating to the GUI page "
            + "\"Administration (Topology) > Agents\" and drilling down to this specific agent. "
            + "You will see the long security token string there. For more information, read: "
            + "https://docs.jboss.org/author/display/RHQ/Agent+Registration";
    }

    /**
     * @see CoreServerService#connectAgent(ConnectAgentRequest)
     */
    @Override
    public ConnectAgentResults connectAgent(ConnectAgentRequest request) throws AgentRegistrationException,
        AgentNotSupportedException {

        String agentName = request.getAgentName();
        AgentVersion agentVersion = request.getAgentVersion();

        log.info("Agent [" + agentName + "][" + agentVersion + "] would like to connect to this server");

        AgentVersionCheckResults agentVersionCheckResults = getAgentManager().isAgentVersionSupported(agentVersion);
        if (!agentVersionCheckResults.isSupported()) {
            log.warn("Agent [" + agentName + "][" + agentVersion
                + "] would like to connect to this server but it is not supported");
            throw new AgentNotSupportedException("Agent [" + agentName + "] is an unsupported agent: " + agentVersion);
        }

        Agent agent = getAgentManager().getAgentByName(agentName);
        if (agent == null) {
            throw new AgentRegistrationException("Agent [" + agentName + "] is not registered");
        }

        Server server = getServerManager().getServer();
        agent.setServer(server);
        agent.setLastAvailabilityPing(Long.valueOf(System.currentTimeMillis()));
        getAgentManager().updateAgent(agent);

        getAlertConditionCacheManager().reloadCachesForAgent(agent.getId());

        getPartitionEventManager().auditPartitionEvent(getSubjectManager().getOverlord(),
            PartitionEventType.AGENT_CONNECT, agentName + " - " + server.getName());

        log.info("Agent [" + agentName + "] has connected to this server at " + new Date());
        return new ConnectAgentResults(System.currentTimeMillis(), agent.isBackFilled(),
            agentVersionCheckResults.getLatestAgentVersion());
    }

    /**
     * @see CoreServerService#getLatestPlugins()
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Plugin> getLatestPlugins() {
        EntityManager em = null;
        List<Plugin> plugins = new ArrayList<Plugin>();

        try {
            em = LookupUtil.getEntityManager();
            Query q = em.createNamedQuery(Plugin.QUERY_FIND_ALL_INSTALLED);
            List<Plugin> resultList = q.getResultList();
            for (Plugin potentialPlugin : resultList) {
                if (potentialPlugin.isEnabled()) {
                    plugins.add(potentialPlugin);
                }
            }
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
    @Override
    public InputStream getPluginArchive(String pluginName) {
        EntityManager em = null;

        try {
            em = LookupUtil.getEntityManager();
            Query q = em.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
            q.setParameter("name", pluginName);
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
    @Override
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

            // Make sure file_to_stream exists - its possible another server deployed this
            // but our server hasn't had a chance to download it from the database yet.
            // If this file does not exist, we need to immediately perform an agent scan
            // which will pull down the plugin file from the database.
            if (!file_to_stream.exists()) {
                log.debug("Agent is asking for a plugin that isn't on file system [" + file_to_stream
                    + "] - performing plugin scan");
                LookupUtil.getPluginDeploymentScanner().scan();
            }

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
    @Override
    public void agentIsShuttingDown(String agentName) {
        log.debug("Agent [" + agentName + "] is sending a notification that it is going down!");

        getAgentManager().agentIsShuttingDown(agentName);

        getPartitionEventManager().auditPartitionEvent(getSubjectManager().getOverlord(),
            PartitionEventType.AGENT_SHUTDOWN, agentName);

        return;
    }

    /**
     * @see CoreServerService#getFailoverList(String)
     */
    @Override
    public FailoverListComposite getFailoverList(String agentName) {
        return getFailoverListManager().getExistingForSingleAgent(agentName);
    }

    /**
     * @see CoreServerService#ping(PingRequest)
     */
    @Override
    public PingRequest ping(PingRequest request) {
        return getAgentManager().handlePingRequest(request);
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
                // this is mainly to support agentspawn environments but I suppose this could happen
                // on "real" systems.  If the agent's machine is heavily loaded, it is possible that
                // the agent wasn't given enough time to respond to the ping.  Let's at least try
                // one more time, because once this ping failure is confirmed, it means the agent
                // will be dead in the water and hang until an admin can reconfigure it (a second
                // failure probably means it really is a configuration problem) 
                ping = sc.pingEndpoint(endpoint, 20000L);

                if (!ping) {
                    failure = new AgentRegistrationException("Server cannot ping the agent's endpoint. "
                        + "The agent's endpoint is probably invalid "
                        + "or there is a firewall preventing the server from connecting to the agent. " + "Endpoint: "
                        + endpoint);
                }
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

    private synchronized String generateAgentToken() {
        if (random == null) {
            try {
                random = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not load SecureRandom algorithm", e);
            }
        }

        byte[] tokenBytes = new byte[50];
        random.nextBytes(tokenBytes);
        return Base64.encode(tokenBytes);
    }

}