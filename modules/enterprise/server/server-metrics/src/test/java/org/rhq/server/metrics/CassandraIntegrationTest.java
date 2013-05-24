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

import java.util.Date;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.joda.time.DateTime;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import org.rhq.cassandra.CCMTestNGListener;
import org.rhq.cassandra.DeployCluster;
import org.rhq.cassandra.ShutdownCluster;
import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.domain.SimplePagedResult;

/**
 * @author John Sanda
 */
@Listeners({CCMTestNGListener.class})
public class CassandraIntegrationTest {

    protected static Session session;

    private static DateTimeService dateTimeService;

    @BeforeSuite
    @DeployCluster(numNodes = 2, username = "cassandra", password = "cassandra")
    public void deployCluster() throws Exception {
        dateTimeService = new DateTimeService();

        Cluster cluster = new ClusterBuilder()
            .addContactPoints("127.0.0.1")
            .withCredentials("cassandra", "cassandra")
            .build();
        session = cluster.connect("rhq");
    }

    @AfterSuite(alwaysRun = true)
    @ShutdownCluster
    public void shutdownCluster() throws Exception {
    }

    protected DateTime hour0() {
        return dateTimeService.hour0();
    }

    protected Iterable<AggregateNumericMetric> findAggregateMetrics(MetricsTable table, int scheduleId) {
        String cql =
            "SELECT schedule_id, time, type, value " +
                "FROM " + table + " " +
                "WHERE schedule_id = ? " +
                "ORDER BY time, type";
        PreparedStatement statement = session.prepare(cql);
        BoundStatement boundStatement = statement.bind(scheduleId);

        return new SimplePagedResult<AggregateNumericMetric>(boundStatement, new AggregateNumericMetricMapper(),
            session);
    }

    protected Iterable<AggregateNumericMetric> findAggregateMetricsWithMetadata(MetricsTable table, int scheduleId,
        long startTime, long endTime) {

        try {
            String cql =
                "SELECT schedule_id, time, type, value, ttl(value), writetime(value) " +
                    "FROM " + table + " " +
                    "WHERE schedule_id = ? AND time >= ? AND time < ?";
            PreparedStatement statement = session.prepare(cql);
            BoundStatement boundStatement = statement.bind(scheduleId, new Date(startTime), new Date(endTime));

            return new SimplePagedResult<AggregateNumericMetric>(boundStatement, new AggregateNumericMetricMapper(true),
                session);
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }
}
