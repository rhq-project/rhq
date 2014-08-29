package org.rhq.cassandra.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
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

        public static Bucket fromString(String table) {
            if (table.equals(ONE_HOUR.tableName)) {
                return ONE_HOUR;
            } else if (table.equals(SIX_HOUR.tableName)) {
                return SIX_HOUR;
            } else if (table.equals(TWENTY_FOUR_HOUR.tableName)) {
                return TWENTY_FOUR_HOUR;
            } else {
                throw new IllegalArgumentException(table + " is not a recognized table name");
            }
        }
    }

    private Session session;

    private DBConnectionFactory dbConnectionFactory;

    private PreparedStatement find1HourData;

    private PreparedStatement find6HourData;

    private PreparedStatement find24HourData;

    private RateLimiter writePermits = RateLimiter.create(7500, 30, TimeUnit.SECONDS);

    private Semaphore readPermits = new Semaphore(1);

    private AtomicInteger failedMigrations = new AtomicInteger();

    private ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4,
        new SchemaUpdateThreadFactory()));

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void bind(Properties properties) {
        dbConnectionFactory = (DBConnectionFactory) properties.get(SchemaManager.RELATIONAL_DB_CONNECTION_FACTORY_PROP);
    }

    @Override
    public void execute() {
        // dbConnectionFactory can be null in test environments which is fine because we start tests with a brand
        // new schema and cluster. In this case, we do not need to do anything since it is not an upgrade scenario.
        if (dbConnectionFactory == null) {
            log.info("The relational database connection factory is not set. No data migration necessary");
        } else {
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
        //dropTables();
    }

    private void migrate(Set<Integer> scheduleIds, PreparedStatement query, final Bucket bucket) {
        log.info("Migrating " + bucket + " data for " + scheduleIds.size() + " schedules");

        CountDownLatch latch = new CountDownLatch(scheduleIds.size());
        MigrationProgressLogger progressLogger = new MigrationProgressLogger(bucket, latch);
        try {
            threadPool.submit(progressLogger);
            for (Integer scheduleId : scheduleIds) {
                readPermits.acquire();
                ResultSet resultSet = session.execute(query.bind(scheduleId));
                ListenableFuture<Integer> migrationFuture = threadPool.submit(new MetricsWriter(scheduleId, bucket,
                    resultSet));
                Futures.addCallback(migrationFuture, migrationFinished(scheduleId, bucket, latch));
            }
            latch.await();
            log.info("Finished migrating " + bucket + " data");
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            throw new RuntimeException("Migration of " + bucket + " data did not complete due to an interrupt. The " +
                "upgrade will have to be run again to finish the migration", e);
        } finally {
            progressLogger.finished();
        }
    }

    private void initPreparedStatements() {
        find1HourData = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value) FROM rhq.one_hour_metrics WHERE schedule_id = ?");

        find6HourData = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value) FROM rhq.six_hour_metrics WHERE schedule_id = ?");

        find24HourData = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value) FROM rhq.twenty_four_hour_metrics WHERE schedule_id = ?");
    }

    private Set<Integer> loadScheduleIds() {
        Connection connection = null;
        Statement statement = null;
        java.sql.ResultSet resultSet = null;
        try {
            connection = dbConnectionFactory.newConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(
                "SELECT s.id FROM rhq_measurement_sched s INNER JOIN rhq_measurement_def d on s.definition = d.id " +
                "WHERE d.data_type = 0");
            Set<Integer> scheduleIds = new HashSet<Integer>();

            while (resultSet.next()) {
                scheduleIds.add(resultSet.getInt(1));
            }

            return scheduleIds;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot migrate aggregate metrics. There was an error loading schedule ids", e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    log.info("There was an error closing the SQL result set", e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    log.info("There was an error closing the SQL statement", e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.info("There was an error closing the SQL connection", e);
                }
            }
        }
    }

    private void dropTables() {
        session.execute("DROP table rhq.one_hour_metrics");
        session.execute("DROP table rhq.six_hour_metrics");
        session.execute("DROP table rhq.twenty_four_hour_metrics");
    }

    private FutureCallback<Integer> migrationFinished(final Integer scheduleId, final Bucket bucket,
        final CountDownLatch latch) {
        return new FutureCallback<Integer>() {
            @Override
            public void onSuccess(Integer metricsWritten) {
                latch.countDown();
                readPermits.release();
            }

            @Override
            public void onFailure(Throwable t) {
                latch.countDown();
                readPermits.release();
                failedMigrations.incrementAndGet();
            }
        };
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
            Integer ttl = rows.get(0).getInt(4);

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
                        log.debug("We only have a partial " + bucket + " metric for {scheduleId: " + scheduleId +
                            ", time: " + time.getTime() + "}. It will not be migrated.");
                    } else {
                        ResultSetFuture writeFuture = writeMetrics(time, avg, max, min, ttl);
                        Futures.addCallback(writeFuture, this);
                    }

                    time = nextTime;
                    max = row.getDouble(3);
                    min = null;
                    avg = null;
                    ttl = row.getInt(4);
                }
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

        private ResultSetFuture writeMetrics(Date time, Double avg, Double max, Double min, Integer ttl) {
            writePermits.acquire();
            return session.executeAsync(
                "INSERT INTO rhq.aggregate_metrics(schedule_id, bucket, time, avg, max, min) VALUES " +
                "(" + scheduleId + ", '" + bucket + "', " + time.getTime() + ", " + avg + ", " + max + ", " +
                    min + ") USING TTL " + ttl);
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
