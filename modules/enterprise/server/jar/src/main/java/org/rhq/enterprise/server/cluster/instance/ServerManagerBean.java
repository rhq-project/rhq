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
package org.rhq.enterprise.server.cluster.instance;

import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cluster.PartitionEventType;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.communications.GlobalSuspendCommandListener;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cluster.AgentStatusManagerLocal;
import org.rhq.enterprise.server.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * If you want to manipulate or report on the {@link Server} instance that
 * some piece of code is currently executing on, use the {@link ServerManagerBean}.
 * 
 * This session bean determines the identity of the server it's running on by
 * reading the <code>rhq.server.high-availability.name</code> property from the
 * rhq-server.properties file.
 * 
 * The functionality provided here is useful when you need to execute something
 * on every server in the cloud, such as partitioned services and data.
 * 
 * @author Joseph Marques
 */
@Stateless
public class ServerManagerBean implements ServerManagerLocal {
    private final Log log = LogFactory.getLog(ServerManagerBean.class);

    static private final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.high-availability.name";

    static private Server.OperationMode lastEstablishedServerMode = null;

    @Resource
    TimerService timerService;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    ClusterManagerLocal clusterManager;

    @EJB
    AgentStatusManagerLocal agentStatusManager;

    @SuppressWarnings("unchecked")
    public void scheduleServerHeartbeat() {
        /* each time the webapp is reloaded, it would create 
         * duplicate events if we don't cancel the existing ones
         */
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            log.debug("Found timer: " + existingTimer.toString());
            existingTimer.cancel();
        }
        // start it now, and repeat every 30 seconds
        timerService.createTimer(0, 30000, "ServerManagerBean.beat");
    }

    @Timeout
    public void handleHeartbeatTimer(Timer timer) {
        beat();
    }

    public int create(Server server) {
        entityManager.persist(server);
        return server.getId();
    }

    public String getIdentity() {
        String identity = System.getProperty(RHQ_SERVER_NAME_PROPERTY, "");
        if (identity.equals("")) {
            return "localhost";
        }
        return identity;
    }

    public List<Agent> getAgents() {
        String identity = getIdentity();
        List<Agent> results = clusterManager.getAgentsByServerName(identity);
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<Agent> getAgentsWithStatus() {
        List<Agent> results = agentStatusManager.getAgentsWithStatusForServer(getIdentity());
        return results;
    }

    public Server getServer() throws ServerNotFoundException {
        String identity = getIdentity();
        Server result = clusterManager.getServerByName(identity);
        if (result == null) {
            throw new ServerNotFoundException("Could not find server; is the " + RHQ_SERVER_NAME_PROPERTY
                + " property set in rhq-server.properties?");
        }
        return result;
    }

    public void establishCurrentServerMode() {
        Server server = getServer();
        Server.OperationMode serverMode = server.getOperationMode();

        // don't add or remove the same listener twice in a row
        if (serverMode == lastEstablishedServerMode)
            return;

        try {
            if (Server.OperationMode.NORMAL == serverMode) {

                if (Server.OperationMode.MAINTENANCE == lastEstablishedServerMode) {
                    ServerCommunicationsServiceUtil.getService().getServiceContainer().removeCommandListener(
                        getMaintenanceModeListener());
                    log.info("Notified communication layer of server operation mode " + serverMode);
                }

            } else if (Server.OperationMode.MAINTENANCE == serverMode) {

                ServerCommunicationsServiceUtil.getService().getServiceContainer().addCommandListener(
                    getMaintenanceModeListener());
                log.info("Notified communication layer of server operation mode " + serverMode);

            } else if (Server.OperationMode.DOWN == serverMode) {

                // The server can't be DOWN if this code is executing, it means the server must be coming
                // up as of this call. So, update the mode to NORMAL and update mtime as an initial heart beat.
                // This will prevent a running ClusterManagerJob from resetting to DOWN before the real
                // ServerManagerJob starts updating the heart beat regularly.
                serverMode = Server.OperationMode.NORMAL;
                server.setOperationMode(serverMode);
                server.setMtime(System.currentTimeMillis());
            }

            // If the server mode is NORMAL and the lastEstablishedServerMode is different (which it must be to 
            // be in this code section) then this server is joining the cloud. Changing the number of servers in 
            // the cloud requires agent distribution work, even if this is a 1-Server cloud. Generate a request for 
            // a repartitioning of agent load, it will be executed on the next invocation of the cluster manager job.
            if (Server.OperationMode.NORMAL == serverMode) {
                LookupUtil.getPartitionEventManager().cloudPartitionEventRequest(
                    LookupUtil.getSubjectManager().getOverlord(), PartitionEventType.SERVER_JOIN, server.getName());
            }

            lastEstablishedServerMode = serverMode;

        } catch (Exception e) {
            log.error("Unable to change HA Server Mode from " + lastEstablishedServerMode + " to " + serverMode + ": "
                + e);
        }
    }

    // use this to ensure a listener of the same name. not using static singleton in case of class reload by different
    // classloaders (in case an exception bubbles up to the slsb layer)
    private GlobalSuspendCommandListener getMaintenanceModeListener() {
        return new GlobalSuspendCommandListener(Server.OperationMode.MAINTENANCE.name(),
            Server.OperationMode.MAINTENANCE.name());
    }

    public void beat() {
        Server server = getServer();
        server.setMtime(System.currentTimeMillis());

        // Handles server mode state changes 
        // note: this call should be fast. if not we need to break the heart beat into its own job
        establishCurrentServerMode();
    }

}
