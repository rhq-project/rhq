package org.rhq.server.metrics;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.util.ClusterBuilder;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;

/**
 * @author John Sanda
 */
public class ClusterMonitorTest {

    private final Log log = LogFactory.getLog(ClusterMonitorTest.class);

//    @Test
    public void monitorCluster() {
        Cluster cluster = new ClusterBuilder()
            .addContactPoints("127.0.0.1")
//            .withCredentials("cassandra", "cassandra")
            .withCredentials("rhqadmin", "rhqadmin")
            .build();

        cluster.register(new Host.StateListener() {
            @Override
            public void onAdd(Host host) {
                log.info("host " + host + " ADDED");
            }

            @Override
            public void onUp(Host host) {
                log.info("host " + host + " UP");
            }

            @Override
            public void onDown(Host host) {
                log.info("host " + host + " DOWN");
            }

            @Override
            public void onRemove(Host host) {
                log.info("host " + host + " REMOVED");
            }
        });

        Session session = cluster.connect("rhq");
        StorageSession storageSession = new StorageSession(session);

        MetricsDAO dao = new MetricsDAO(storageSession, new MetricsConfiguration());

        while (true) {
            try {
                Thread.sleep(10000);
                try {
//                    session.execute("select * from system.schema_keyspaces");
                    com.datastax.driver.core.Query query = QueryBuilder.select().from("rhq", "raw_metrics").setConsistencyLevel(
                        ConsistencyLevel.ALL);
//                    session.execute(query);
//                    session.execute("select * from rhq.raw_metrics");
//                    log.info("query succeeded");
                    StorageResultSetFuture future = dao.insertRawData
                        (new MeasurementDataNumeric(System.currentTimeMillis(), 123, 1.1), ConsistencyLevel.ONE);

                    Futures.addCallback(future, new FutureCallback<ResultSet>() {
                        @Override
                        public void onSuccess(ResultSet rows) {
                            log.info("insert succeeded");
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            log.error("insert failed", throwable);
                        }
                    });
                } catch (Exception e) {
                    log.error("query failed", e);
                }
            } catch (InterruptedException e) {
            }
        }
    }

}
