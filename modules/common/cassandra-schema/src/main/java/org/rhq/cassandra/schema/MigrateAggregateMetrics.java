package org.rhq.cassandra.schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.io.LineReader;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Migrates aggregate metrics from the one_hour_metrics, six_hour_metrics, and twenty_four_hour_metrics tables to the
 * new aggregate_metrics table. The failure to migrate data for a single measurement schedule will result in an
 * exception being thrown that causes the upgrade to fail; however, all schedules will be processed even if there are
 * failures. An exception is thrown only after going through data for all schedules.
 * </p>
 * <p>
 * When data for a measurement schedule is successfully migrated, the schedule id is recorded in a log. There are
 * separate log files for each of the 1 hour, 6 hour, and 24 hour tables. They are stored in the server data directory.
 * Each table log is read prior to starting the migration to determine what schedule ids have data to be migrated.
 * </p>
 * <p>
 * After all data has been successfully migrated, the one_hour_metrics, six_hour_metrics, and twenty_four_hour_metrics
 * tables are dropped.
 * </p>
 *
 *
 * @author John Sanda
 */
public class MigrateAggregateMetrics implements Step {

    private static final Log log = LogFactory.getLog(MigrateAggregateMetrics.class);

    private static enum Bucket {

        ONE_HOUR("one_hour"),

        SIX_HOUR("six_hour"),

        TWENTY_FOUR_HOUR("twenty_four_hour");

        private String tableName;

        private Bucket(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public String toString() {
            return tableName;
        }
    }

    private Session session;

    private DBConnectionFactory dbConnectionFactory;

    private PreparedStatement find1HourData;

    private PreparedStatement find6HourData;

    private PreparedStatement find24HourData;

    private int jobBatchSize = 8;

    private int concurrentJobLimit = 5;

    private RateLimiter writePermits;

    private Semaphore readPermits = new Semaphore(jobBatchSize * concurrentJobLimit);

    private AtomicInteger failedMigrations = new AtomicInteger();

    private ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
        new SchemaUpdateThreadFactory()));

    private String dataDir;

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void bind(Properties properties) {
        dbConnectionFactory = (DBConnectionFactory) properties.get(SchemaManager.RELATIONAL_DB_CONNECTION_FACTORY_PROP);
        dataDir = properties.getProperty("data.dir", System.getProperty("jboss.server.data.dir"));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void execute() {
        // dbConnectionFactory can be null in test environments which is fine because we start tests with a brand
        // new schema and cluster. In this case, we do not need to do anything since it is not an upgrade scenario.
        if (dbConnectionFactory == null) {
            log.info("The relational database connection factory is not set. No data migration necessary");
        } else {
            writePermits = RateLimiter.create(calculatePermits(), 30, TimeUnit.SECONDS);

            Stopwatch stopwatch = new Stopwatch().start();
            initPreparedStatements();
            Set<Integer> scheduleIds = loadScheduleIds();

            log.info("Migrating aggregate metrics for " + scheduleIds.size() + " schedule ids");

            migrate(scheduleIds, find1HourData, Bucket.ONE_HOUR);
            migrate(scheduleIds, find6HourData, Bucket.SIX_HOUR);
            migrate(scheduleIds, find24HourData, Bucket.TWENTY_FOUR_HOUR);

            stopwatch.stop();
            log.info("Finished aggregate metrics migration in " + stopwatch.elapsed(TimeUnit.SECONDS) + " seconds");

            if (failedMigrations.get() > 0) {
                throw new RuntimeException("There were " + failedMigrations.get() + " failed migrations. The " +
                    "upgrade will have to be run again to complete the migration.");
            }
        }
        dropTables();
    }

    private int calculatePermits() {
        int requestLimit = Integer.parseInt(System.getProperty("rhq.storage.request.limit", "20000"));
        return requestLimit * getNumberOfUpNodes();
    }

    private int getNumberOfUpNodes() {
        int count = 0;
        for (Host host : session.getCluster().getMetadata().getAllHosts()) {
            if (host.isUp()) {
                ++count;
            }
        }
        return count;
    }

    private void migrate(Set<Integer> scheduleIds, PreparedStatement query, final Bucket bucket) {
        log.info("Migrating " + bucket + " data for " + scheduleIds.size() + " schedules");

        CountDownLatch latch = new CountDownLatch(scheduleIds.size());
        MigrationProgressLogger progressLogger = new MigrationProgressLogger(bucket, latch);
        File logFile = new File(dataDir, bucket + "_migration.log");
        MigrationLog migrationLog = null;
        try {
            migrationLog = new MigrationLog(logFile);
            Set<Integer> migratedScheduleIds = migrationLog.read();
            threadPool.submit(progressLogger);
            List<Integer> scheduleIdsBatch = new ArrayList<Integer>(jobBatchSize);
            for (Integer scheduleId : scheduleIds) {
                scheduleIdsBatch.add(scheduleId);
                if (scheduleIdsBatch.size() == jobBatchSize) {
                    submitBatch(scheduleIdsBatch, migratedScheduleIds, latch, bucket, query, migrationLog);
                    scheduleIdsBatch = new ArrayList<Integer>(jobBatchSize);
                }
            }
            if (!scheduleIds.isEmpty()) {
                submitBatch(scheduleIdsBatch, migratedScheduleIds, latch, bucket, query, migrationLog);
            }
            latch.await();
            log.info("Finished migrating " + bucket + " data");
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            throw new RuntimeException("Migration of " + bucket + " data did not complete due to an interrupt. The " +
                "upgrade will have to be run again to finish the migration", e);
        } catch (IOException e) {
            throw new RuntimeException("Migration of " + bucket + " data did not complete due to an I/O error. The " +
                "upgrade will have to be run again to finish the migration", e);
        } finally {
            progressLogger.finished();
            try {
                migrationLog.close();
            } catch (IOException e) {
                log.warn("There was an error closing " + logFile.getAbsolutePath(), e);
            }
        }
    }

    private void submitBatch(final List<Integer> scheduleIds, final Set<Integer> migratedScheduleIds,
        final CountDownLatch latch, final MigrateAggregateMetrics.Bucket bucket, final PreparedStatement query,
        final MigrationLog migrationLog) throws InterruptedException {
        readPermits.acquire(jobBatchSize);
        threadPool.submit(processBatch(scheduleIds, migratedScheduleIds, latch, bucket, query,
            migrationLog));
    }

    private Runnable processBatch(final List<Integer> scheduleIds, final Set<Integer> migratedScheduleIds,
        final CountDownLatch latch, final MigrateAggregateMetrics.Bucket bucket, final PreparedStatement query,
        final MigrationLog migrationLog) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    for (Integer scheduleId : scheduleIds) {
                        if (migratedScheduleIds.contains(scheduleId)) {
                            if (log.isDebugEnabled()) {
                                log.debug(bucket + " data for schedule id " + scheduleId + " has already been " +
                                    "migrated. It will be skipped.");
                            }
                            readPermits.release();
                            latch.countDown();
                        } else {
                            ResultSetFuture queryFuture = session.executeAsync(query.bind(scheduleId));
                            ListenableFuture<Integer> migrationFuture = Futures.transform(queryFuture,
                                new MigrateData(scheduleId, bucket), threadPool);
                            Futures.addCallback(migrationFuture, migrationFinished(scheduleId, bucket, latch, migrationLog));
                        }
                    }
                } catch (Exception e) {
                    log.error("There was an unexpected error processing data", e);
                }
            }
        };
    }

    private void initPreparedStatements() {
        find1HourData = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.one_hour_metrics " +
            "WHERE schedule_id = ?");

        find6HourData = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.six_hour_metrics " +
            "WHERE schedule_id = ?");

        find24HourData = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.twenty_four_hour_metrics " +
            "WHERE schedule_id = ?");
    }

    private Set<Integer> loadScheduleIds() {
//        Connection connection = null;
//        Statement statement = null;
//        java.sql.ResultSet resultSet = null;
//        try {
//            connection = dbConnectionFactory.newConnection();
//            statement = connection.createStatement();
//            resultSet = statement.executeQuery(
//                "SELECT s.id FROM rhq_measurement_sched s INNER JOIN rhq_measurement_def d on s.definition = d.id " +
//                "WHERE d.data_type = 0");
//            Set<Integer> scheduleIds = new HashSet<Integer>();
//
//            while (resultSet.next()) {
//                scheduleIds.add(resultSet.getInt(1));
//            }
//
//            return scheduleIds;
//        } catch (SQLException e) {
//            throw new RuntimeException("Cannot migrate aggregate metrics. There was an error loading schedule ids", e);
//        } finally {
//            if (resultSet != null) {
//                try {
//                    resultSet.close();
//                } catch (SQLException e) {
//                    log.info("There was an error closing the SQL result set", e);
//                }
//            }
//            if (statement != null) {
//                try {
//                    statement.close();
//                } catch (SQLException e) {
//                    log.info("There was an error closing the SQL statement", e);
//                }
//            }
//            if (connection != null) {
//                try {
//                    connection.close();
//                } catch (SQLException e) {
//                    log.info("There was an error closing the SQL connection", e);
//                }
//            }
//        }
        NumberFormat formatter = new DecimalFormat("\"#,#\"");
        Set<Integer> scheduleIds = new HashSet<Integer>();
        InputStream stream = getClass().getResourceAsStream("/schedule_ids");
        LineReader reader = new LineReader(new InputStreamReader(stream));

        try {
            String line = reader.readLine();
            while (line != null) {
                scheduleIds.add(formatter.parse(line).intValue());
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schedule ids");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to load schedule ids");
        }

        return scheduleIds;
    }

    private List<Integer> migrate1hrData() {
        List<Integer> scheduleIds = new ArrayList<Integer>(100000);
        List<Integer> batch = new ArrayList<Integer>(jobBatchSize);

        NumberFormat formatter = new DecimalFormat("\"#,#\"");
        InputStream stream = getClass().getResourceAsStream("/schedule_ids");
        LineReader reader = new LineReader(new InputStreamReader(stream));

        try {
            String line = reader.readLine();
            while (line != null) {
                Integer scheduleId = formatter.parse(line).intValue();
                scheduleIds.add(scheduleId);
                batch.add(scheduleId);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schedule ids");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to load schedule ids");
        }

        return scheduleIds;
    }

    private void dropTables() {
//        ResultSet resultSet = session.execute("SELECT columnfamily_name FROM system.schema_columnfamilies " +
//            "WHERE keyspace_name = 'rhq'");
//        for (Row row : resultSet) {
//            String table = row.getString(0);
//            if (table.equals("one_hour_metrics") || table.equals("six_hour_metrics") ||
//                table.equals("twenty_four_hour_metrics")) {
//                log.info("Dropping table " +  table);
//                session.execute("DROP table rhq." + table);
//            }
//        }
    }

    private FutureCallback<Integer> migrationFinished(final Integer scheduleId, final Bucket bucket,
        final CountDownLatch latch, final MigrationLog migrationLog) {
        return new FutureCallback<Integer>() {
            @Override
            public void onSuccess(Integer metricsWritten) {
                latch.countDown();
                readPermits.release();
                log.debug("Finished migration for schedule id " + scheduleId);
                try {
                    migrationLog.write(scheduleId);
                } catch (IOException e) {
                    log.warn("Failed to update migration log for bucket " + bucket + " and schedule id " + scheduleId);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                latch.countDown();
                readPermits.release();
                log.debug("Finished migration for schedule id " + scheduleId);
                failedMigrations.incrementAndGet();
            }
        };
    }

    private class MigrateData implements Function<ResultSet, Integer>, FutureCallback<ResultSet> {

        private Integer scheduleId;

        private Bucket bucket;

        private boolean writeFailed;

        private AtomicInteger metricsMigrated = new AtomicInteger();

        public MigrateData(Integer scheduleId, Bucket bucket) {
            this.scheduleId = scheduleId;
            this.bucket = bucket;
        }

        @Override
        public Integer apply(ResultSet resultSet) {
            List<Row> rows = resultSet.all();
            if (rows.isEmpty()) {
                log.debug("No " + bucket + " data to migrate for schedule id " + scheduleId);
                return 0;
            }
            Date time = rows.get(0).getDate(1);
            Date nextTime;
            Double max = null;
            Double min = null;
            Double avg = null;
            Long writeTime = rows.get(0).getLong(5);
            Integer ttl = rows.get(0).getInt(4);
            int batchSize = 30;
            List<Statement> statements = new ArrayList<Statement>(batchSize);

            for (Row row : rows) {
                if (writeFailed) {
                    throw new RuntimeException("Migration of " + bucket + " data for schedule id " + scheduleId + " failed");
                }
                nextTime = row.getDate(1);
                if (nextTime.equals(time)) {
                    int type = row.getInt(2);
                    switch (type) {
                    case 0 :
                        max = row.getDouble(3);
                        break;
                    case 1:
                        min = row.getDouble(3);
                        break;
                    default:
                        avg = row.getDouble(3);
                    }
                } else {
                    if (isDataMissing(avg, max, min)) {
                        log.debug("We only have a partial " + bucket + " metric for {scheduleId: " + scheduleId +
                            ", time: " + time.getTime() + "}. It will not be migrated.");
                    } else {
                        statements.add(createInsertStatement(time, avg, max, min, ttl, writeTime));
                        if (statements.size() == batchSize) {
                            Futures.addCallback(writeBatch(statements), this);
                            statements.clear();
                        }
                    }

                    time = nextTime;
                    max = row.getDouble(3);
                    min = null;
                    avg = null;
                    ttl = row.getInt(4);
                    writeTime = row.getLong(5);
                }
            }

            if (!statements.isEmpty()) {
                Futures.addCallback(writeBatch(statements), this);
            }

            if (writeFailed) {
                throw new RuntimeException("Migration of " + bucket + " data for schedule id " + scheduleId + " failed");
            }

            return metricsMigrated.get();
        }

        @Override
        public void onSuccess(ResultSet result) {
            metricsMigrated.incrementAndGet();
        }

        @Override
        public void onFailure(Throwable t) {
            writeFailed = true;
            // TODO only log a warning once
            // If we have a failure, changes are that we will get them in bunches. Since we
            // want to stop on the first failed write, it would be nice to only log the
            // first failure in order to avoid spamming the log.
            log.warn("Migration of " + bucket + " data for schedule id " + scheduleId + " failed", t);
        }

        private boolean isDataMissing(Double avg, Double max, Double min) {
            if (avg == null || Double.isNaN(avg)) return true;
            if (max == null || Double.isNaN(max)) return true;
            if (min == null || Double.isNaN(min)) return true;

            return false;
        }

        private ResultSetFuture writeBatch(List<Statement> statements) {
            Batch batch = QueryBuilder.batch(statements.toArray(new Statement[statements.size()]));
            writePermits.acquire();
            return session.executeAsync(batch);
        }

        private SimpleStatement createInsertStatement(Date time, Double avg, Double max, Double min, Integer ttl,
            Long writeTime) {
            return new SimpleStatement("INSERT INTO rhq.aggregate_metrics(schedule_id, bucket, time, avg, max, min) VALUES " +
                "(" + scheduleId + ", '" + bucket + "', " + time.getTime() + ", " + avg + ", " + max + ", " +
                min + ") USING TTL " + ttl + " AND TIMESTAMP " + writeTime);
        }
    }

    private class MetricsWriter implements Callable<Integer>, FutureCallback<ResultSet> {

        private Integer scheduleId;

        private Bucket bucket;

        private ResultSet resultSet;

        private boolean writeFailed;

        private AtomicInteger metricsMigrated = new AtomicInteger();

        public MetricsWriter(Integer scheduleId, Bucket bucket, ResultSet resultSet) {
            this.scheduleId = scheduleId;
            this.bucket = bucket;
            this.resultSet = resultSet;
        }

        @Override
        public Integer call() throws Exception {
            List<Row> rows = resultSet.all();
            if (rows.isEmpty()) {
                log.debug("No " + bucket + " data to migrate for schedule id " + scheduleId);
                return 0;
            }
            Date time = rows.get(0).getDate(1);
            Date nextTime;
            Double max = null;
            Double min = null;
            Double avg = null;
            Long writeTime = rows.get(0).getLong(5);
            Integer ttl = rows.get(0).getInt(4);
            int batchSize = 30;
            List<Statement> statements = new ArrayList<Statement>(batchSize);

            for (Row row : rows) {
                if (writeFailed) {
                    throw new Exception("Migration of " + bucket + " data for schedule id " + scheduleId + " failed");
                }
                nextTime = row.getDate(1);
                if (nextTime.equals(time)) {
                    int type = row.getInt(2);
                    switch (type) {
                        case 0 :
                            max = row.getDouble(3);
                            break;
                        case 1:
                            min = row.getDouble(3);
                            break;
                        default:
                            avg = row.getDouble(3);
                    }
                } else {
                    if (isDataMissing(avg, max, min)) {
                        if (log.isDebugEnabled()) {
                            log.debug("We only have a partial " + bucket + " metric for {scheduleId: " + scheduleId +
                                ", time: " + time.getTime() + "}. It will not be migrated.");
                        }
                    } else {
                        statements.add(createInsertStatement(time, avg, max, min, ttl, writeTime));
                        if (statements.size() == batchSize) {
                            Futures.addCallback(writeBatch(statements), this);
                            statements.clear();
                        }
                    }

                    time = nextTime;
                    max = row.getDouble(3);
                    min = null;
                    avg = null;
                    ttl = row.getInt(4);
                    writeTime = row.getLong(5);
                }
            }

            if (!statements.isEmpty()) {
                Futures.addCallback(writeBatch(statements), this);
            }

            if (writeFailed) {
                throw new Exception("Migration of " + bucket + " data for schedule id " + scheduleId + " failed");
            }

            return metricsMigrated.get();
        }

        private boolean isDataMissing(Double avg, Double max, Double min) {
            if (avg == null || Double.isNaN(avg)) return true;
            if (max == null || Double.isNaN(max)) return true;
            if (min == null || Double.isNaN(min)) return true;

            return false;
        }

        @Override
        public void onSuccess(ResultSet resultSet) {
            metricsMigrated.incrementAndGet();
        }

        @Override
        public void onFailure(Throwable t) {
            writeFailed = true;
            // TODO only log a warning once
            // If we have a failure, changes are that we will get them in bunches. Since we
            // want to stop on the first failed write, it would be nice to only log the
            // first failure in order to avoid spamming the log.
            log.warn("Migration of " + bucket + " data for schedule id " + scheduleId + " failed", t);
        }

        private SimpleStatement createInsertStatement(Date time, Double avg, Double max, Double min, Integer ttl,
            Long writeTime) {
            return new SimpleStatement("INSERT INTO rhq.aggregate_metrics(schedule_id, bucket, time, avg, max, min) VALUES " +
                "(" + scheduleId + ", '" + bucket + "', " + time.getTime() + ", " + avg + ", " + max + ", " +
                min + ") USING TTL " + ttl + " AND TIMESTAMP " + writeTime);
        }

        private ResultSetFuture writeBatch(List<Statement> statements) {
            Batch batch = QueryBuilder.batch(statements.toArray(new Statement[statements.size()]));
            writePermits.acquire();
            return session.executeAsync(batch);
        }

        private ResultSetFuture writeMetrics(Date time, Double avg, Double max, Double min, Integer ttl,
            Long writeTime) {
            writePermits.acquire();
            return session.executeAsync(
                "INSERT INTO rhq.aggregate_metrics(schedule_id, bucket, time, avg, max, min) VALUES " +
                "(" + scheduleId + ", '" + bucket + "', " + time.getTime() + ", " + avg + ", " + max + ", " +
                    min + ") USING TTL " + ttl + " AND TIMESTAMP " + writeTime);
        }
    }

    private class MigrationProgressLogger implements Runnable {

        private Bucket bucket;

        private CountDownLatch latch;

        private boolean finished;

        public MigrationProgressLogger(Bucket bucket, CountDownLatch latch) {
            this.bucket = bucket;
            this.latch = latch;
        }

        public void finished() {
            finished = true;
        }

        @Override
        public void run() {
            try {
                while (!finished && latch.getCount() > 0) {
                    log.info("There are " + latch.getCount() + " remaining schedules for the " + bucket +
                        " data migration");
                    Thread.sleep(30000);
                }
            } catch (InterruptedException e) {
            }
        }
    }

}
