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

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleAuthInfoProvider;

import org.rhq.cassandra.CassandraNode;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.server.metrics.CQLException;
import org.rhq.server.metrics.MetricsConfiguration;

/**
 * @author John Sanda
 */
@Singleton
public class SessionManagerBean {

    private Session session;

    private MetricsConfiguration metricsConfiguration = new MetricsConfiguration();

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

            String seedProp = System.getProperty("rhq.cassandra.seeds");
            if (seedProp == null) {
                throw new CQLException("The rhq.cassandra.seeds property is null. Cannot create session.");
            }
            int port = -1;
            String[] seeds = seedProp.split(",");
            String[] hostNames = new String[seeds.length];
            for (int i = 0; i < seeds.length; ++i) {
                CassandraNode node = CassandraNode.parseNode(seeds[i]);
                port = node.getNativeTransportPort();
                hostNames[i] = node.getHostName();
            }

            if (port == -1) {
                throw new RuntimeException("Failed to initialize client port. Cannot create " +
                    Session.class.getName() + " object.");
            }

            Cluster cluster = new ClusterBuilder()
                .addContactPoints(hostNames)
                .withAuthInfoProvider(new SimpleAuthInfoProvider().add("username", username).add("password", password))
                .withPort(port)
                .build();
            session = cluster.connect("rhq");
        } catch (Exception  e) {
            throw new CQLException("Unable to create session", e);
        }
    }

    public Session getSession() {
        return session;
    }

    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }

}
