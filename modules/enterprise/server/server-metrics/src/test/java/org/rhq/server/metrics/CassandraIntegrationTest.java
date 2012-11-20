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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.cassandra.cql.jdbc.CassandraDataSource;
import org.joda.time.DateTime;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.CassandraException;
import org.rhq.cassandra.DeployCluster;
import org.rhq.cassandra.ShutdownCluster;

/**
 * @author John Sanda
 */
@Listeners({CassandraClusterManager.class})
public class CassandraIntegrationTest {

    protected CassandraDataSource dataSource;

    protected Connection connection;

    private DateTimeService dateTimeService;

    @BeforeClass
    @DeployCluster(numNodes = 2)
    public void deployCluster() throws CassandraException {
        dateTimeService = new DateTimeService();

//        List<CassandraHost> hosts = asList(new CassandraHost("127.0.0.1", 9160), new CassandraHost("127.0.0.2", 9160));
//        List<CassandraHost> hosts = asList(new CassandraHost("127.0.0.1", 9160));
//        ClusterInitService initService = new ClusterInitService();
//
//        initService.waitForClusterToStart(hosts);
//        initService.waitForSchemaAgreement("rhq", hosts);

        dataSource = new CassandraDataSource("127.0.0.1", 9160, "rhq", null, null, "3.0.0");
        try {
            connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            statement.execute("use rhq;");
        } catch (SQLException e) {
            throw new CassandraException("Unable to get JDBC connection.", e);
        }
    }

    @AfterClass
    @ShutdownCluster
    public void shutdownCluster() throws Exception {
    }

    protected DateTime hour0() {
        return dateTimeService.hour0();
    }
}
