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

package org.rhq.server.metrics;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleAuthInfoProvider;

import org.joda.time.DateTime;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

import org.rhq.cassandra.CCMTestNGListener;
import org.rhq.cassandra.DeployCluster;
import org.rhq.cassandra.ShutdownCluster;

/**
 * @author John Sanda
 */
@Listeners({CCMTestNGListener.class})
public class CassandraIntegrationTest {

    protected Session session;

    private DateTimeService dateTimeService;

    @BeforeClass
    @DeployCluster(numNodes = 2)
    public void deployCluster() throws Exception {
        dateTimeService = new DateTimeService();

        SimpleAuthInfoProvider authInfoProvider = new SimpleAuthInfoProvider();
        authInfoProvider.add("username", "rhqadmin").add("password", "rhqadmin");

        Cluster cluster = Cluster.builder()
            .addContactPoints("127.0.0.1", "127.0.0.2")
            .withAuthInfoProvider(authInfoProvider)
            .build();
        session = cluster.connect("rhq");
    }

    @AfterClass
    @ShutdownCluster
    public void shutdownCluster() throws Exception {
    }

    protected DateTime hour0() {
        return dateTimeService.hour0();
    }
}
