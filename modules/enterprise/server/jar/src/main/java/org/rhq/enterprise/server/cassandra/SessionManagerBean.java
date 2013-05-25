/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.enterprise.server.cassandra;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.server.metrics.CQLException;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;

/**
 * @author John Sanda
 */
@Singleton
public class SessionManagerBean {

    private Session session;

    private MetricsConfiguration metricsConfiguration = new MetricsConfiguration();

    private MetricsDAO metricsDAO;

    private MetricsServer metricsServer;

    @EJB
    private StorageNodeManagerLocal storageNodeManager;

    @PostConstruct
    private void createSession() {
        try {
            String username = System.getProperty("rhq.cassandra.username");
            if (username == null) {
                throw new CQLException("The rhq.cassandra.username property is null. Cannot create session.");
            }

            String password = System.getProperty("rhq.cassandra.password");
            if (password == null) {
                throw new CQLException("The rhq.cassandra.password property is null. Cannot create session.");
            }

            List<StorageNode> storageNodes = storageNodeManager.getStorageNodes();
            if (storageNodes.size() <= 0) {
                throw new CQLException("Storage node seed list not available.");
            }

            List<String> hostNames = new ArrayList<String>();
            for (StorageNode storageNode : storageNodes) {
                hostNames.add(storageNode.getAddress());
            }
            int port = storageNodes.get(0).getCqlPort();

            Cluster cluster = new ClusterBuilder()
                .addContactPoints(hostNames.toArray(new String[hostNames.size()]))
                .withCredentials(username, password)
                .withPort(port)
                .build();
            PoolingOptions poolingOptions = cluster.getConfiguration().getPoolingOptions();
            poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, 16);
            poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, 16);
            session = cluster.connect("rhq");

            metricsDAO = new MetricsDAO(session, metricsConfiguration);
        } catch (Exception  e) {
            throw new CQLException("Unable to create session", e);
        }
    }

    public MetricsDAO getMetricsDAO() {
        return metricsDAO;
    }

    public MetricsServer getMetricsServer() {
        if (metricsServer != null) {
            return metricsServer;
        }

        metricsServer = new MetricsServer();
        metricsServer.setDAO(metricsDAO);
        metricsServer.setSession(getSession());
        metricsServer.setConfiguration(metricsConfiguration);

        DateTimeService dateTimeService = new DateTimeService();
        dateTimeService.setConfiguration(metricsConfiguration);
        metricsServer.setDateTimeService(dateTimeService);

        return metricsServer;
    }


    public Session getSession() {
        return session;
    }

    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }

}
