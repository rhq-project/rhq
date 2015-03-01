package org.rhq.cassandra.schema;

import static org.testng.Assert.assertFalse;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author John Sanda
 */
public class MigrateAggregateMetricsTest extends SchemaUpgradeTest {

    private static final Log log = LogFactory.getLog(MigrateAggregateMetricsTest.class);

    private Session session;

    private RateLimiter permits = RateLimiter.create(20000, 20, TimeUnit.SECONDS);

    private PreparedStatement insert1HourData;

    private PreparedStatement insert6HourData;

    private PreparedStatement insert24HourData;

    @BeforeMethod
    public void setUp() throws Exception {
        for (File file : new File("target").listFiles()) {
            if (file.getName().endsWith("_migration.log")) {
                file.delete();
            }
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
    }

    @Test
    public void runMigration() throws Exception {
        int numSchedules = Integer.parseInt(System.getProperty("numSchedules", "10"));
        Properties properties = new Properties();
        properties.put(SchemaManager.RELATIONAL_DB_CONNECTION_FACTORY_PROP, new DBConnectionFactory() {
            @Override
            public Connection newConnection() throws SQLException {
                return null;
            }
        });
        properties.put(SchemaManager.DATA_DIR, "target");

        SchemaManager schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
            new String[] {"127.0.0.1"}, 9042);
        schemaManager.setUpdateFolderFactory(new TestUpdateFolderFactory(VersionManager.Task.Update.getFolder())
            .removeFiles("0005.xml", "0006.xml", "0007.xml"));
        schemaManager.drop();
        schemaManager.shutdown();
        schemaManager.install(properties);
        schemaManager.shutdown();

        Cluster cluster = new Cluster.Builder()
            .addContactPoint("127.0.0.1")
            .withCredentials("rhqadmin", "rhqadmin")
            .withPoolingOptions(new PoolingOptions()
                .setCoreConnectionsPerHost(HostDistance.LOCAL, 30)
                .setMaxConnectionsPerHost(HostDistance.LOCAL, 50))
            .build();
        session = cluster.connect("rhq");
        initPreparedStatements();

        DateTime endTime = DateTime.now();
        insert1HourData(numSchedules, endTime);
        insert6HourData(numSchedules, endTime);
        insert24HourData(numSchedules, endTime);

        schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
            new String[] {"127.0.0.1"}, 9042);
        schemaManager.install(properties);
        schemaManager.shutdown();

        assert1HourDataMigrated(numSchedules);
        assert6HourDataMigrated(numSchedules);
        assert24HourDataMigrated(numSchedules);

        assertTablesDropped("one_hour_metrics", "six_hour_metrics", "twenty_four_hour_metrics");
    }

    private void initPreparedStatements() {
        insert1HourData = session.prepare("insert into one_hour_metrics (schedule_id, time, type, value) values " +
            "(?, ?, ?, ?)");

        insert6HourData = session.prepare("insert into six_hour_metrics (schedule_id, time, type, value) values " +
            "(?, ?, ?, ?)");

        insert24HourData = session.prepare("insert into twenty_four_hour_metrics (schedule_id, time, type, value) " +
            "values (?, ?, ?, ?)");
    }

    private void insert1HourData(int numSchedules, DateTime endTime) {
        log.info("Inserting 1 hour data");
        DateTime startTime = DateTime.now().minusDays(14);
        Duration duration = Hours.ONE.toStandardDuration();

        insertData(numSchedules, startTime, endTime, duration, insert1HourData);
    }

    private void insert6HourData(int numSchedules, DateTime endTime) {
        log.info("Inserting 6 hour data");
        DateTime startTime = endTime.minusDays(31);
        Duration duration = Hours.SIX.toStandardDuration();

        insertData(numSchedules, startTime, endTime, duration, insert6HourData);
    }

    private void insert24HourData(int numSchedules, DateTime endTime) {
        log.info("Inserting 24 hour data");
        DateTime startTime = endTime.minusDays(365);
        Duration duration = Days.ONE.toStandardDuration();

        insertData(numSchedules, startTime, endTime, duration, insert24HourData);
    }

    private void insertData(int numSchedules, DateTime startTime, DateTime endTime, Duration duration,
        final PreparedStatement insert) {
        DateTime time = startTime;

        while (time.isBefore(endTime)) {
            for (int i = -1; i > -numSchedules; --i) {
                List<ResultSetFuture> futures = new ArrayList<ResultSetFuture>(3);
                permits.acquire(3);
                futures.add(session.executeAsync(insert.bind(i, time.toDate(), 0, 3.14)));
                futures.add(session.executeAsync(insert.bind(i, time.toDate(), 1, 3.14)));
                futures.add(session.executeAsync(insert.bind(i, time.toDate(), 2, 3.14)));
                ListenableFuture<List<ResultSet>> insertsFuture = Futures.allAsList(futures);
                Futures.addCallback(insertsFuture, new FutureCallback<List<ResultSet>>() {
                    @Override
                    public void onSuccess(List<ResultSet> result) {
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("Insert [" + insert.getQueryString() + "] failed", t);
                    }
                });
            }
            time = time.plus(duration);
        }
    }

    /**
     * Verifies that there exists 1 hour data in the new tables. The actual data is not is not inspected.
     */
    private void assert1HourDataMigrated(int numSchedules) {
        assertDataMigrated(numSchedules, "one_hour");
    }

    /**
     * Verifies that there exists 6 hour data in the new tables. The actual data is not is not inspected.
     */
    private void assert6HourDataMigrated(int numSchedules) {
        assertDataMigrated(numSchedules, "six_hour");
    }

    /**
     * Verifies that there exists 24 hour data in the new tables. The actual data is not is not inspected.
     */
    private void assert24HourDataMigrated(int numSchedules) {
        assertDataMigrated(numSchedules, "twenty_four_hour");
    }

    private void assertDataMigrated(int numSchedules, String bucket) {
        for (int i = -1; i > -numSchedules; --i) {
            ResultSet resultSet = session.execute("select * from " + Table.AGGREGATE_METRICS + " where schedule_id = " +
                i + " and bucket = '" + bucket + "' limit 1");
            assertFalse(resultSet.isExhausted(), "Failed to migrate " + bucket + " data for schedule id " + i);
        }
    }

    private void assertTablesDropped(String... tables) {
        Set<String> existingTables = new HashSet<String>();
        ResultSet resultSet = session.execute("SELECT columnfamily_name FROM system.schema_columnfamilies " +
            "WHERE keyspace_name = 'rhq'");
        for (Row row : resultSet) {
            existingTables.add(row.getString(0));
        }
        for (String table : tables) {
            assertFalse(existingTables.contains(table), table + " should have been dropped");
        }
    }

}
