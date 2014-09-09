package org.rhq.cassandra.schema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * @author John Sanda
 */
public class MigrateAggregateMetricsTest extends SchemaUpgradeTest {

    private static final Log log = LogFactory.getLog(MigrateAggregateMetricsTest.class);

    private static final int RESOURCE_TYPE_ID = -1;

    private static final int RESOURCE_ID = -1;

    private Connection connection;

    private Session session;

    private RateLimiter permits = RateLimiter.create(20000, 20, TimeUnit.SECONDS);

    private PreparedStatement insert1HourData;

    private PreparedStatement insert6HourData;

    private PreparedStatement insert24HourData;

    @BeforeClass
    public void setupClass() throws Exception {
        connection = newJDBCConnection();
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        JDBCUtil.safeClose(connection);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        resetDB();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        resetDB();
    }

    private void resetDB() throws Exception {
        executeUpdate("delete from rhq_measurement_sched where id < 0");
        executeUpdate("delete from rhq_measurement_def where id < 0");
        executeUpdate("delete from rhq_resource where id = " + RESOURCE_ID);
        executeUpdate("delete from rhq_resource_type where id = " + RESOURCE_TYPE_ID);
    }

    @Test
    public void runMigration() throws Exception {
        int numSchedules = Integer.parseInt(System.getProperty("numSchedules", "10"));
        createResource();
        createMeasurementSchedules(numSchedules);

        Properties properties = new Properties();
        properties.put(SchemaManager.RELATIONAL_DB_CONNECTION_FACTORY_PROP, new DBConnectionFactory() {
            @Override
            public Connection newConnection() throws SQLException {
                return newJDBCConnection();
            }
        });
        properties.put(SchemaManager.DATA_DIR, "target");

        TestSchemaManager schemaManager = new TestSchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
            new String[] {"127.0.0.1"}, 9042);
        schemaManager.setUpdateFolderFactory(new TestUpdateFolderFactory(VersionManager.Task.Update.getFolder())
            .removeFiles("0005.xml", "0006.xml", "0007.xml"));
        schemaManager.drop();
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


        schemaManager = new TestSchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
            new String[] {"127.0.0.1"}, 9042);
        schemaManager.updateAggregateMetrics = true;
        schemaManager.install(properties);
    }

    private void initPreparedStatements() {
//        insert1HourData = session.prepare("insert into one_hour_metrics (schedule_id, time, type, value) values " +
//            "(?, ?, ?, ?) USING TTL " + Minutes.minutes(40).toStandardSeconds().getSeconds());

        insert1HourData = session.prepare("insert into one_hour_metrics (schedule_id, time, type, value) values " +
            "(?, ?, ?, ?)");

//        insert6HourData = session.prepare("insert into six_hour_metrics (schedule_id, time, type, value) values " +
//            "(?, ?, ?, ?) USING TTL " + Minutes.minutes(50).toStandardSeconds().getSeconds());

        insert6HourData = session.prepare("insert into six_hour_metrics (schedule_id, time, type, value) values " +
            "(?, ?, ?, ?)");

//        insert24HourData = session.prepare("insert into twenty_four_hour_metrics (schedule_id, time, type, value) " +
//                "values (?, ?, ?, ?) USING TTL " + Minutes.minutes(60).toStandardSeconds().getSeconds());

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

    private void createResource() throws Exception {
        String insertResourceType = "insert into rhq_resource_type (id, name, category, creation_data_type, " +
            "create_delete_policy, singleton, supports_manual_add, missing_policy) values (" + RESOURCE_TYPE_ID +
            ", 'migration-test-type', 'test', 'test', 'test', " + jdbcBooleanValue(true) + ", " +
            jdbcBooleanValue(true) + ", 'test')";

        String insertResource = "insert into rhq_resource (id, resource_type_id, uuid, resource_key) values (" +
            RESOURCE_ID + ", " + RESOURCE_TYPE_ID + ", 'migration-test', 'migration-test')";

        executeUpdate(insertResourceType);
        executeUpdate(insertResource);
    }

    private void createMeasurementSchedules(int count) throws Exception {
        for (int i = -1; i > -count; --i) {
            executeUpdate("insert into rhq_measurement_def (id, resource_type_id, name, data_type) values (" + i +
                ", " + RESOURCE_TYPE_ID + ", 'migration-test-def', 0)");
            executeUpdate("insert into rhq_measurement_sched (id, definition, resource_id) values (" + i + ", " +
                i + ", " + RESOURCE_ID + ")");
        }
    }

    private void executeUpdate(String sql) throws Exception {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(sql);
        } finally {
            JDBCUtil.safeClose(statement);
        }
    }

    private class TestSchemaManager extends SchemaManager {

        public boolean updateAggregateMetrics;

        private String username;

        private String password;

        private String[] nodes;

        private int cqlPort;

        public TestSchemaManager(String username, String password, String[] nodes, int cqlPort) {
            super(username, password, nodes, cqlPort);
            this.username = username;
            this.password = password;
            this.nodes = nodes;
            this.cqlPort = cqlPort;
        }

        @Override
        public void install(Properties properties) throws Exception {
            TestVersionManager versionManager = new TestVersionManager(username, password, nodes, cqlPort);
            versionManager.updateAggregateMetrics = updateAggregateMetrics;
            versionManager.install(properties);
        }
    }

    private class TestVersionManager extends VersionManager {

        public boolean updateAggregateMetrics;

        public TestVersionManager(String username, String password, String[] nodes, int cqlPort) throws Exception {
            super(username, password, nodes, cqlPort, new SessionManager());
        }

        @Override
        protected void execute(UpdateFile updateFile, Properties properties) {
            if (updateAggregateMetrics) {
                super.execute(updateFile, properties);
            } else {
                if (!updateFile.getFile().equals("schema/update//0005.xml")) {
                    super.execute(updateFile, properties);
                }
            }
        }
    }

    private Connection newJDBCConnection() throws SQLException {
        return DriverManager.getConnection(System.getProperty("rhq.db.url"),
            System.getProperty("rhq.db.username"), System.getProperty("rhq.db.password"));
    }

    private String jdbcBooleanValue(boolean value) {
        if (System.getProperty("rhq.db.url").contains("postgres")) {
            return value ? "true" : "false";
        }
        return value ? "1" : "0";
    }

}
