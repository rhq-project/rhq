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

package org.rhq.server.metrics;

import static org.testng.Assert.fail;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.rhq.server.metrics.domain.NumericMetric;
import org.rhq.server.metrics.domain.ResultSetMapper;
import org.rhq.server.metrics.domain.SimplePagedResult;

/**
 * @author John Sanda
 */
@Listeners({CCMTestNGListener.class})
public class CassandraIntegrationTest {

    private static final String RHQADMIN = "rhqadmin";
    private static final String RHQADMIN_PASSWORD = "1eeb2f255e832171df8592078de921bc";

    protected static Session session;

    protected static StorageSession storageSession;

    private static DateTimeService dateTimeService;

    private final Log log = LogFactory.getLog(CassandraIntegrationTest.class);

    @BeforeSuite
    @DeployCluster(numNodes = 1, username = RHQADMIN, password = RHQADMIN_PASSWORD, waitForSchemaAgreement = true)
    public void deployCluster() throws Exception {
        dateTimeService = new DateTimeService();

        Cluster cluster = new ClusterBuilder()
            .addContactPoints("127.0.0.1")
            .withCredentialsObfuscated(RHQADMIN, RHQADMIN_PASSWORD)
            .build();

        PoolingOptions poolingOptions = cluster.getConfiguration().getPoolingOptions();
        poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, 24);
        poolingOptions.setCoreConnectionsPerHost(HostDistance.REMOTE, 24);
        poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, 32);
        poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, 32);

        cluster.register(new Host.StateListener() {
            @Override
            public void onAdd(Host host) {
                log.info("host " + host + " added");
            }

            @Override
            public void onUp(Host host) {
                log.info("host " + host + " up");
            }

            @Override
            public void onDown(Host host) {
                log.info("host " + host + " down");
            }

            @Override
            public void onRemove(Host host) {
                log.info("host " + host + " removed");
            }
        });

        session = cluster.connect("rhq");
        storageSession = new StorageSession(session);
    }

    @AfterSuite(alwaysRun = true)
    @ShutdownCluster
    public void shutdownCluster() throws Exception {
    }

    protected DateTime hour0() {
        return dateTimeService.hour0();
    }

    protected DateTime hour(int hours) {
        return dateTimeService.hour0().plusHours(hours);
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
            storageSession);
    }

    protected static class WaitForRead<T extends NumericMetric> implements FutureCallback<ResultSet> {
        private final Log log = LogFactory.getLog(WaitForRead.class);

        private CountDownLatch latch;

        private Throwable throwable;

        private ResultSetMapper<T> mapper;

        private List<T> results;

        public  WaitForRead(ResultSetMapper<T> mapper) {
            latch = new CountDownLatch(1);
            this.mapper = mapper;
        }

        @Override
        public void onSuccess(ResultSet rows) {
            try {
                results = mapper.mapAll(rows);
            } catch (Exception e) {
                throwable = e;
                log.error("There was an error getting the results", e);
            } finally {
                latch.countDown();
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            this.throwable = throwable;
            log.error("An async operation failed", throwable);
            latch.countDown();
        }

        public void await(String errorMsg) throws InterruptedException {
            latch.await();
            if (throwable != null) {
                fail(errorMsg, Throwables.getRootCause(throwable));
            }
        }

        public List<T> getResults() {
            return results;
        }
    }
}
