/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;

import org.rhq.core.clientapi.server.core.AgentVersion;
import org.rhq.core.clientapi.server.core.PingRequest;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.composite.AgentLastAvailabilityPingComposite;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.cloud.FailoverListManagerLocal;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.concurrent.AvailabilityReportSerializer;

/**
 * Manages the access to {@link Agent} objects.<br>
 * <br>
 * Some of these methods need to execute as fast as possible. So, @ExcludeDefaultInterceptors has been added
 * to all methods that don't explicitly need a permission check (those without Subject as the first parameter).
 *
 * @author John Mazzitelli
 */
@Stateless
public class AgentManagerBean implements AgentManagerLocal {
    private static final Log LOG = LogFactory.getLog(AgentManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    //@IgnoreDependency
    private FailoverListManagerLocal failoverListManager;

    @EJB
    //@IgnoreDependency
    private AvailabilityManagerLocal availabilityManager;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private SystemManagerLocal systemManager;

    @EJB
    //@IgnoreDependency
    private SubjectManagerLocal subjectManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    // constants used for the agent update version file
    private static final String RHQ_SERVER_VERSION = "rhq-server.version";
    private static final String RHQ_SERVER_BUILD_NUMBER = "rhq-server.build-number";
    private static final String RHQ_AGENT_LATEST_VERSION = "rhq-agent.latest.version";
    private static final String RHQ_AGENT_LATEST_BUILD_NUMBER = "rhq-agent.latest.build-number";
    private static final String RHQ_AGENT_LATEST_MD5 = "rhq-agent.latest.md5";

    @ExcludeDefaultInterceptors
    public void createAgent(Agent agent) {
        entityManager.persist(agent);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Persisted new agent: " + agent);
        }
    }

    @ExcludeDefaultInterceptors
    public void deleteAgent(Agent agent) {
        agent = entityManager.find(Agent.class, agent.getId());
        failoverListManager.deleteServerListsForAgent(agent);
        entityManager.remove(agent);
        destroyAgentClient(agent);
        LOG.info("Removed agent: " + agent);
    }

    @ExcludeDefaultInterceptors
    public void destroyAgentClient(Agent agent) {
        ServerCommunicationsServiceMBean bootstrap = ServerCommunicationsServiceUtil.getService();
        try {
            bootstrap.destroyKnownAgentClient(agent);
            if (LOG.isDebugEnabled()) {
                LOG.debug("agent client destroyed for agent: " + agent);
            }
        } catch (Exception e) {
            // certain unit tests won't create the agentClient
            LOG.warn("Could not destroy agent client for agent [" + agent + "]: " + ThrowableUtil.getAllMessages(e));
        }
    }

    @ExcludeDefaultInterceptors
    public Agent updateAgent(Agent agent) {
        agent = entityManager.merge(agent);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated agent: " + agent);
        }
        return agent;
    }

    @ExcludeDefaultInterceptors
    public AgentClient getAgentClient(Agent agent) {
        AgentClient client = null;

        try {
            ServerCommunicationsServiceMBean bootstrap = ServerCommunicationsServiceUtil.getService();
            client = bootstrap.getKnownAgentClient(agent);

            if (client != null) {
                // We assume the caller is asking for a client so it can send the agent messages,
                // so let's start the sender automatically for the caller so it doesn't need to remember to do it
                client.startSending();
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("There is no agent client for agent: " + agent);
                }
            }
        } catch (Throwable t) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not get agent client for agent: " + agent, t);
            }
        }

        return client;
    }

    /*
     * Removed ExcludeDefaultInterceptors annotation to enable permission and session check by the container.
     */
    public AgentClient getAgentClient(Subject subject, int resourceId) {
        Agent agent = getAgentByResourceId(subject, resourceId);

        if (agent == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resource [" + resourceId + "] does not exist or has no agent assigned");
            }
            return null;
        }

        return getAgentClient(agent);
    }

    @ExcludeDefaultInterceptors
    public void agentIsShuttingDown(String agentName) {
        Agent downedAgent = getAgentByName(agentName);

        ServerCommunicationsServiceMBean server_bootstrap = ServerCommunicationsServiceUtil.getService();
        server_bootstrap.removeDownedAgent(downedAgent.getRemoteEndpoint());
        LOG.info("Agent with name [" + agentName + "] just went down");

        agentManager.backfillAgentInNewTransaction(subjectManager.getOverlord(), agentName, downedAgent.getId());
        return;
    }

    @ExcludeDefaultInterceptors
    public void agentIsAlive(Agent agent) {
        // This method needs to be very fast.  It is currently designed to be called from
        // the security token command authenticator.  It calls this method every time
        // a message comes in and the auth token needs to be verified.  This method takes
        // a detached agent rather than an agent name because I don't want to waste a round
        // trip looking up the agent from the DB when I already know the caller (the
        // authenticator) just got the agent object.
        // When the authenticator calls us, it means it just got a message from the agent,
        // so we can use this to our benefit.  Since we got a message from the given agent,
        // we know it is up!  So this is a simple way for us to detect agents coming online
        // without us having to periodically poll each and every agent.

        try {
            ServerCommunicationsServiceMBean server_comm = ServerCommunicationsServiceUtil.getService();
            server_comm.addStartedAgent(agent);
        } catch (Exception e) {
            LOG.info("Cannot flag the agent as started for some reason", e);
        }

        return;
    }

    @SuppressWarnings("unchecked")
    @ExcludeDefaultInterceptors
    public void checkForSuspectAgents() {
        LOG.debug("Checking to see if there are agents that we suspect are down...");

        long maximumQuietTimeAllowed = 300000L;
        try {
            String prop = systemManager.getUnmaskedSystemSettings(true).get(SystemSetting.AGENT_MAX_QUIET_TIME_ALLOWED);
            if (prop != null) {
                maximumQuietTimeAllowed = Long.parseLong(prop);
            }
        } catch (Exception e) {
            LOG.warn("Agent quiet time config is invalid in DB, defaulting to: " + maximumQuietTimeAllowed, e);
        }

        List<AgentLastAvailabilityPingComposite> records;

        long nowEpoch = System.currentTimeMillis();

        Query q = entityManager.createNamedQuery(Agent.QUERY_FIND_ALL_SUSPECT_AGENTS);
        q.setParameter("dateThreshold", nowEpoch - maximumQuietTimeAllowed);
        records = q.getResultList();

        ServerCommunicationsServiceMBean serverComm = null;

        for (AgentLastAvailabilityPingComposite record : records) {
            long lastReport = record.getLastAvailabilityPing();
            long timeSinceLastReport = nowEpoch - lastReport;

            // Only show this message a few times so we do not flood the log with the same message if the agent is down a long time
            // we show it as soon as we detect it going down (within twice max quiet time allowed) or we show it
            // after every 6th hour it's detected to be down (again, within twice max quiet time).  Effectively, you'll see
            // this message appear about 4 times per 6-hours for a downed agent if using the default max quiet time.
            // Note that in here we also make sure the agent client is shutdown. We do it here because, even if the agent
            // was already backfilled, we still want to do this in case somehow the client was started again.
            if ((timeSinceLastReport % 21600000L) < (maximumQuietTimeAllowed * 2L)) {
                if (serverComm == null) {
                    serverComm = ServerCommunicationsServiceUtil.getService();
                }

                serverComm.removeDownedAgent(record.getRemoteEndpoint());
            }

            // we can avoid doing this over and over again for agents that are down a long time by seeing if it's
            // already backfilled.  Note that we do not log the above warn message down here in this if-statement,
            // because I think we still want to log that we think an agent is down periodically.
            // If it turns out we do not want to be that noisy, just move that warn message down in here so we only ever log
            // about a downed agent once, at the time it is first backfilled.
            if (!record.isBackFilled()) {
                LOG.info("Have not heard from agent [" + record.getAgentName() + "] since ["
                    + new Date(record.getLastAvailabilityPing()) + "]. Will be backfilled since we suspect it is down");

                agentManager.backfillAgentInNewTransaction(subjectManager.getOverlord(), record.getAgentName(),
                    record.getAgentId());
            }
        }

        LOG.debug("Finished checking for suspected agents");

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void backfillAgentInNewTransaction(Subject subject, String agentName, int agentId) {
        // make sure we lock out all processing of any availability reports that might come our way to avoid concurrency
        // problems
        AvailabilityReportSerializer.getSingleton().lock(agentName);
        try {
            // This marks the top-level platform DOWN since we have not heard from it and all
            // child resources UNKNOWN since they are not reporting and may very well be up
            availabilityManager.updateAgentResourceAvailabilities(agentId, AvailabilityType.DOWN,
                AvailabilityType.UNKNOWN);

            // This marks the Agent as backfilled=true. Although doing this in its own transaction means the
            // agent will be reflected as backfilled before the resource availability changes are committed, that
            // should not matter, as it affects only availability reporting which 1) is locked out (see a few line above)
            // and 2) at worst would only request a full avail report anyway, when detecting a backfilled agent.
            // What this buys is it shortens the locking on the agent table, because backfilling a large inventory
            // can take time.  Performing this after the backfill to minimize the window between the commits as
            // much as possible.
            agentManager.setAgentBackfilledInNewTransaction(agentId, true);

        } finally {
            AvailabilityReportSerializer.getSingleton().unlock(agentName);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void setAgentBackfilledInNewTransaction(int agentId, boolean backfilled) {
        Query query = entityManager.createNamedQuery(Agent.QUERY_SET_AGENT_BACKFILLED);
        query.setParameter("agentId", agentId);
        query.setParameter("backfilled", backfilled);
        query.executeUpdate();
    }

    public boolean isAgentBackfilled(int agentId) {
        // query returns 0 if the agent's platform is DOWN (or does not exist), 1 if not
        Query query = entityManager.createNamedQuery(Agent.QUERY_IS_AGENT_BACKFILLED);
        query.setParameter("agentId", agentId);
        Long backfilledCount = (Long) query.getSingleResult();
        return backfilledCount != 0L;
    }

    @SuppressWarnings("unchecked")
    @ExcludeDefaultInterceptors
    public List<Agent> getAllAgents() {
        return entityManager.createNamedQuery(Agent.QUERY_FIND_ALL).getResultList();
    }

    /** Methods with page control are typically accessed by the GUI, as such apply permission check. */
    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public PageList<Agent> getAgentsByServer(Subject subject, Integer serverId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("a.name");

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, Agent.QUERY_FIND_BY_SERVER, pageControl);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, Agent.QUERY_FIND_BY_SERVER);

        query.setParameter("serverId", serverId);
        countQuery.setParameter("serverId", serverId);

        long count = (Long) countQuery.getSingleResult();
        List<Agent> results = query.getResultList();

        return new PageList<Agent>(results, (int) count, pageControl);
    }

    @ExcludeDefaultInterceptors
    public int getAgentCount() {
        return ((Number) entityManager.createNamedQuery(Agent.QUERY_COUNT_ALL).getSingleResult()).intValue();
    }

    @ExcludeDefaultInterceptors
    public Agent getAgentByAgentToken(String token) {
        Agent agent;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_AGENT_TOKEN);
            query.setParameter("agentToken", token);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lookup agent - none exist with token [" + token + "] : " + e);
            }
            agent = null;
        }

        return agent;
    }

    @ExcludeDefaultInterceptors
    public Agent getAgentByName(String agentName) {
        Agent agent;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_NAME);
            query.setParameter("name", agentName);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lookup agent - none exist with name [" + agentName + "] : " + e);
            }
            agent = null;
        }

        return agent;
    }

    @ExcludeDefaultInterceptors
    public Agent getAgentByID(int agentId) {
        Agent agent = entityManager.find(Agent.class, agentId);
        return agent;
    }

    @ExcludeDefaultInterceptors
    public Agent getAgentByAddressAndPort(String address, int port) {
        Agent agent;
        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_ADDRESS_AND_PORT);
            query.setParameter("address", address);
            query.setParameter("port", port);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Agent not found with address/port: " + address + "/" + port);
            }
            agent = null;
        }

        return agent;
    }

    /*
     * Removed ExcludeDefaultInterceptors annotation to enable permission and session check by the container.
     */
    public Agent getAgentByResourceId(Subject subject, int resourceId) {
        Agent agent;

        try {
            //insert logged in check and view resources perm check as method called from GWT*Service
            if ((subject != null) && (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS))) {
                throw new PermissionException("Can not get agent details - " + subject + " lacks "
                    + Permission.MANAGE_SETTINGS + " for resource[id=" + resourceId + "]");
            }

            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            agent = (Agent) query.getSingleResult();
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lookup agent for resource with ID of [" + resourceId + "] : " + e);
            }
            agent = null;
        }

        return agent;
    }

    @ExcludeDefaultInterceptors
    public Integer getAgentIdByResourceId(int resourceId) {
        Integer agentId;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_AGENT_ID_BY_RESOURCE_ID);
            query.setParameter("resourceId", resourceId);
            agentId = (Integer) query.getSingleResult();
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lookup agent for resource with ID of [" + resourceId + "] : " + e);
            }
            agentId = null;
        }

        return agentId;
    }

    @ExcludeDefaultInterceptors
    public Integer getAgentIdByName(String agentName) {
        Integer agentId;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_AGENT_ID_BY_NAME);
            query.setParameter("name", agentName);
            agentId = (Integer) query.getSingleResult();
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lookup agent for name of [" + agentName + "] : " + e);
            }
            agentId = null;
        }

        return agentId;
    }

    @ExcludeDefaultInterceptors
    public Integer getAgentIdByScheduleId(int scheduleId) {
        Integer agentId;

        try {
            Query query = entityManager.createNamedQuery(Agent.QUERY_FIND_AGENT_ID_BY_SCHEDULE_ID);
            query.setParameter("scheduleId", scheduleId);
            agentId = (Integer) query.getSingleResult();
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lookup agent for resource with ID of [" + scheduleId + "] : " + e);
            }
            agentId = null;
        }

        return agentId;
    }

    @ExcludeDefaultInterceptors
    public boolean isAgentVersionSupported(AgentVersion agentVersionInfo) {
        try {
            Properties properties = getAgentUpdateVersionFileContent();

            // Prime Directive: whatever agent update the server has installed is the one we support,
            // so both the version AND build number must match.
            // For developers, however, we want to allow to be less strict - only version needs to match.
            String supportedAgentVersion = properties.getProperty(RHQ_AGENT_LATEST_VERSION);
            if (supportedAgentVersion == null) {
                throw new NullPointerException("no agent version in file");
            }
            ComparableVersion agent = new ComparableVersion(agentVersionInfo.getVersion());
            ComparableVersion server = new ComparableVersion(supportedAgentVersion);
            if (Boolean.getBoolean("rhq.server.agent-update.nonstrict-version-check")) {
                return agent.equals(server);
            } else {
                String supportedAgentBuild = properties.getProperty(RHQ_AGENT_LATEST_BUILD_NUMBER);
                return agent.equals(server) && agentVersionInfo.getBuild().equals(supportedAgentBuild);
            }
        } catch (Exception e) {
            LOG.warn("Cannot determine if agent version [" + agentVersionInfo + "] is supported. Cause: " + e);
            return false; // assume we can't talk to it
        }
    }

    @ExcludeDefaultInterceptors
    public File getAgentUpdateVersionFile() throws Exception {
        File agentDownloadDir = getAgentDataDownloadDir();
        File versionFile = new File(agentDownloadDir, "rhq-server-agent-versions.properties");
        File binaryFile = getAgentUpdateBinaryFile();
        Boolean needVersionFile = FileUtil.isNewer(binaryFile, versionFile);
        if (needVersionFile == null || needVersionFile.booleanValue()) {
            // we do not have the version properties file yet or it must be regenerated; let's extract some info and create it
            StringBuilder serverVersionInfo = new StringBuilder();

            // first, get the server version info (by asking our server for the info)
            CoreServerMBean coreServer = LookupUtil.getCoreServer();
            serverVersionInfo.append(RHQ_SERVER_VERSION + '=').append(coreServer.getVersion()).append('\n');
            serverVersionInfo.append(RHQ_SERVER_BUILD_NUMBER + '=').append(coreServer.getBuildNumber()).append('\n');

            // calculate the MD5 of the agent update binary file
            String md5Property = RHQ_AGENT_LATEST_MD5 + '=' + MessageDigestGenerator.getDigestString(binaryFile) + '\n';

            // second, get the agent version info (by peeking into the agent update binary jar)
            JarFile binaryJarFile = new JarFile(binaryFile);
            try {
                JarEntry binaryJarFileEntry = binaryJarFile.getJarEntry("rhq-agent-update-version.properties");
                InputStream binaryJarFileEntryStream = binaryJarFile.getInputStream(binaryJarFileEntry);

                // now write the server and agent version info in our internal version file our servlet will use
                FileOutputStream versionFileOutputStream = new FileOutputStream(versionFile);
                try {
                    versionFileOutputStream.write(serverVersionInfo.toString().getBytes());
                    versionFileOutputStream.write(md5Property.getBytes());
                    StreamUtil.copy(binaryJarFileEntryStream, versionFileOutputStream, false);
                } finally {
                    try {
                        versionFileOutputStream.close();
                    } catch (Exception e) {
                    }
                    try {
                        binaryJarFileEntryStream.close();
                    } catch (Exception e) {
                    }
                }
            } finally {
                binaryJarFile.close();
            }
        }

        return versionFile;
    }

    @ExcludeDefaultInterceptors
    public Properties getAgentUpdateVersionFileContent() throws Exception {
        FileInputStream stream = new FileInputStream(getAgentUpdateVersionFile());
        try {
            Properties props = new Properties();
            props.load(stream);
            return props;
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    @ExcludeDefaultInterceptors
    public File getAgentUpdateBinaryFile() throws Exception {
        File agentDownloadDir = getAgentDownloadDir();
        for (File file : agentDownloadDir.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                return file;
            }
        }

        throw new FileNotFoundException("Missing agent update binary in [" + agentDownloadDir + "]");
    }

    /**
     * The directory on the server's file system where the agent update binary file is found.
     *
     * @return directory where the agent binary downloads are found
     *
     * @throws Exception if could not determine the location or it does not exist
     */
    // SHOULD BE PRIVATE AND REMOVED FROM LOCAL INTERFACE - NOTHING (OTHER THAN THIS CLASS ITSELF) SHOULD USE THIS
    public File getAgentDownloadDir() throws Exception {
        File earDir = LookupUtil.getCoreServer().getEarDeploymentDir();
        File agentDownloadDir = new File(earDir, "rhq-downloads/rhq-agent");
        if (!agentDownloadDir.exists()) {
            throw new FileNotFoundException("Missing agent downloads directory at [" + agentDownloadDir + "]");
        }
        return agentDownloadDir;
    }

    /**
     * The directory on the server's file system where the agent update version file is found.
     *
     * @return directory where the agent version file is found
     *
     * @throws Exception if could not determine the location or it does not exist
     */
    private File getAgentDataDownloadDir() throws Exception {
        File earDir = LookupUtil.getCoreServer().getJBossServerDataDir();
        File agentDownloadDir = new File(earDir, "rhq-downloads/rhq-agent");
        if (!agentDownloadDir.exists()) {
            agentDownloadDir.mkdirs();
            if (!agentDownloadDir.exists()) {
                throw new FileNotFoundException("Missing agent data downloads directory at [" + agentDownloadDir + "]");
            }
        }
        return agentDownloadDir;
    }

    @Override
    public PingRequest handlePingRequest(PingRequest request) {

        // set the ping time without delay, so the return time is as close to the request time as possible,
        // for clock sync reasons.
        long now = System.currentTimeMillis();

        if (request.isRequestUpdateAvailability()) {
            updateLastAvailabilityPing(request.getAgentName(), now);
            request.setReplyUpdateAvailability(true);
        }

        if (request.isRequestServerTimestamp()) {
            request.setReplyServerTimestamp(now);
        }

        return request;
    }

    private void updateLastAvailabilityPing(String agentName, long now) {
        /*
         * since we already know we have to update the agent row with the last avail ping time, might as well
         * set the backfilled to false here (as opposed to called agentManager.setBackfilled(agentId, false)
         */
        Query query = entityManager.createNamedQuery(Agent.QUERY_UPDATE_LAST_AVAIL_PING);
        query.setParameter("now", now);
        query.setParameter("agentName", agentName);

        query.executeUpdate();
    }

    @ExcludeDefaultInterceptors
    public Boolean pingAgentByResourceId(Subject subject, int resourceId) {
        Boolean pingResults = Boolean.FALSE;
        Agent agent;

        try {
            //insert call to method doing logged in check and view resources perm check as method calld from GWT*Service
            agent = getAgentByResourceId(subject, resourceId);

            //now ping
            AgentClient client = getAgentClient(agent);
            pingResults = client.ping(5000L);

        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to lookup agent for resource with ID of [" + resourceId + "] : " + e);
            }
            agent = null;
        }

        return pingResults;
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public PageList<Agent> findAgentsByCriteria(Subject subject, AgentCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        CriteriaQueryRunner<Agent> runner = new CriteriaQueryRunner<Agent>(criteria, generator, entityManager);
        return runner.execute();
    }

}
