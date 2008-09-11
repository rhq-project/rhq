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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.cluster.AffinityGroup;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.communications.GlobalSuspendCommandListener;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.cluster.AgentStatusManagerLocal;
import org.rhq.enterprise.server.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.cluster.FailoverListManagerLocal;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;

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

    static private Server.OperationMode lastSetMode = null;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    @IgnoreDependency
    ClusterManagerLocal clusterManager;

    @EJB
    FailoverListManagerLocal failoverListManager;

    @EJB
    AgentStatusManagerLocal agentStatusManager;

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

    synchronized public void establishCurrentServerMode() {
        Server.OperationMode currentMode = getServer().getOperationMode();

        // don't add or remove the same listener twice in a row
        if (currentMode == lastSetMode)
            return;

        try {
            if (Server.OperationMode.NORMAL == currentMode) {
                if (lastSetMode == Server.OperationMode.MAINTENANCE) {
                    ServerCommunicationsServiceUtil.getService().getServiceContainer().removeCommandListener(
                        getMaintenanceModeListener());
                }
            } else if (Server.OperationMode.MAINTENANCE == currentMode) {
                ServerCommunicationsServiceUtil.getService().getServiceContainer().addCommandListener(
                    getMaintenanceModeListener());
            } else {
                return;
            }

            lastSetMode = currentMode;
            log.info("Notified communication layer of server operation mode " + currentMode);

        } catch (Exception e) {
            log.error("Unable to change HA Server Mode from " + lastSetMode + " to " + currentMode + ": " + e);
        }
    }

    // use this to ensure a listener of the same name. not using static singleton in case of class reload by different
    // classloaders (in case an exception bubbles up to the slsb layer)
    private GlobalSuspendCommandListener getMaintenanceModeListener() {
        return new GlobalSuspendCommandListener(Server.OperationMode.MAINTENANCE.name(),
            Server.OperationMode.MAINTENANCE.name());
    }

    public void deleteServer(Server server) {
        server = entityManager.find(Server.class, server.getId());
        failoverListManager.deleteServerListDetailsForServer(server);
        entityManager.remove(server);

        log.info("Removed server: " + server);
    }

    public void deleteAffinityGroup(AffinityGroup affinityGroup) {
        affinityGroup = entityManager.find(AffinityGroup.class, affinityGroup.getId());
        entityManager.remove(affinityGroup);

        log.info("Removed affinityGroup: " + affinityGroup);
    }

}
