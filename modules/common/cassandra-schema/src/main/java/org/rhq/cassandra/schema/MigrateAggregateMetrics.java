package org.rhq.cassandra.schema;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricsRegistry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;

import org.rhq.cassandra.schema.migration.QueryExecutor;
import org.rhq.core.util.exception.ThrowableUtil;

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

    public static enum Bucket {

        ONE_HOUR("one_hour", "one_hour_metrics"),

        SIX_HOUR("six_hour", "six_hour_metrics"),

        TWENTY_FOUR_HOUR("twenty_four_hour", "twenty_four_hour_metrics");

        private String text;

        private String tableName;

        private Bucket(String text, String tableName) {
            this.text = text;
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static final int DEFAULT_WARM_UP = 20;

    private static final double RATE_INCREASE_PER_NODE = 0.3;

    private Session session;

    private String dataDir;

    private DBConnectionFactory dbConnectionFactory;

    private AtomicReference<RateLimiter> readPermitsRef = new AtomicReference<RateLimiter>();

    private AtomicReference<RateLimiter> writePermitsRef = new AtomicReference<RateLimiter>();

    private CountDownLatch remaining1HourMetrics;

    private CountDownLatch remaining6HourMetrics;

    private CountDownLatch remaining24HourMetrics;

    SchemaUpdateThreadFactory threadFactory = new SchemaUpdateThreadFactory();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(6, threadFactory);

//    private ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(6,
//        new SchemaUpdateThreadFactory()));
    private ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(executor);

    private MetricsRegistry metricsRegistry;

    private Meter migrationsMeter;

    private RateMonitor rateMonitor;

    private KeyScanner keyScanner;

    private MigrationProgressLogger progressLogger;

    private AtomicInteger readErrors = new AtomicInteger();

    private AtomicInteger writeErrors = new AtomicInteger();

    private QueryExecutor queryExecutor;

    private FutureFallback<ResultSet> countReadErrors = new FutureFallback<ResultSet>() {
        @Override
        public ListenableFuture<ResultSet> create(Throwable t) throws Exception {
            readErrors.incrementAndGet();
            return Futures.immediateFailedFuture(t);
        }
    };

    private FutureFallback<List<ResultSet>> countWriteErrors = new FutureFallback<List<ResultSet>>() {
        @Override
        public ListenableFuture<List<ResultSet>> create(Throwable t) throws Exception {
            writeErrors.incrementAndGet();
            return Futures.immediateFailedFuture(t);
        }
    };

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
    public void execute() {
        log.info("Starting data migration");
        metricsRegistry = new MetricsRegistry();
        migrationsMeter = metricsRegistry.newMeter(MigrateAggregateMetrics.class, "migrations", "migrations",
            TimeUnit.MINUTES);

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            // dbConnectionFactory can be null in test environments which is fine because we start tests with a brand
            // new schema and cluster. In this case, we do not need to do anything since it is not an upgrade scenario.
            if (dbConnectionFactory == null) {
                log.info("The relational database connection factory is not set. No data migration necessary");
                return;
            }

            writePermitsRef.set(RateLimiter.create(getWriteLimit(getNumberOfUpNodes()), DEFAULT_WARM_UP,
                TimeUnit.SECONDS));
            readPermitsRef.set(RateLimiter.create(getReadLimit(getNumberOfUpNodes()), DEFAULT_WARM_UP,
                TimeUnit.SECONDS));

            log.info("The request limits are " + writePermitsRef.get().getRate() + " writes/sec and " +
                readPermitsRef.get().getRate() + " reads/sec");

            progressLogger = new MigrationProgressLogger();
            rateMonitor = new RateMonitor(readPermitsRef, writePermitsRef);
            queryExecutor = new QueryExecutor(session, readPermitsRef, writePermitsRef);

            keyScanner = new KeyScanner(session);
            Set<Integer> scheduleIdsWith1HourData = Collections.emptySet();
            Set<Integer> scheduleIdsWith6HourData = Collections.emptySet();
            Set<Integer> scheduleIdsWith24HourData = Collections.emptySet();

            Set<String> tables = getTables();
            if (tables.contains(Bucket.ONE_HOUR.getTableName())) {
                scheduleIdsWith1HourData = keyScanner.scanFor1HourKeys();
                log.info("There are " + scheduleIdsWith1HourData.size() + " schedule ids with " + Bucket.ONE_HOUR +
                    " data");
                remaining1HourMetrics = new CountDownLatch(scheduleIdsWith1HourData.size());
            } else {
                log.info(Bucket.ONE_HOUR + " data has already been migrated");
                remaining1HourMetrics = new CountDownLatch(0);
            }
            if (tables.contains(Bucket.SIX_HOUR.getTableName())) {
                scheduleIdsWith6HourData = keyScanner.scanFor6HourKeys();
                log.info("There are " + scheduleIdsWith6HourData.size() + " schedule ids with " + Bucket.SIX_HOUR +
                    " data");
                remaining6HourMetrics = new CountDownLatch(scheduleIdsWith6HourData.size());
            } else {
                log.info(Bucket.SIX_HOUR + " data has already been migrated");
                remaining6HourMetrics = new CountDownLatch(0);
            }
            if (tables.contains(Bucket.TWENTY_FOUR_HOUR.getTableName())) {
                scheduleIdsWith24HourData = keyScanner.scanFor24HourKeys();
                log.info("There are " + scheduleIdsWith24HourData.size() + " schedule ids with " +
                    Bucket.TWENTY_FOUR_HOUR + " data");
                remaining24HourMetrics = new CountDownLatch(scheduleIdsWith24HourData.size());
            } else {
                log.info(Bucket.TWENTY_FOUR_HOUR + " data has already been migrated");
                remaining24HourMetrics = new CountDownLatch(0);
            }
            keyScanner.shutdown();

            threadPool.submit(progressLogger);
            threadPool.submit(rateMonitor);

            if (!scheduleIdsWith1HourData.isEmpty()) {
                migrate1HourData(scheduleIdsWith1HourData);
            }
            if (!scheduleIdsWith6HourData.isEmpty()) {
                migrate6HourData(scheduleIdsWith6HourData);
            }
            if (!scheduleIdsWith24HourData.isEmpty()) {
                migrate24HourData(scheduleIdsWith24HourData);
            }

            stopwatch.stop();
            log.info("Finished migrating " + scheduleIdsWith1HourData.size() + " " + Bucket.ONE_HOUR + ", " +
                scheduleIdsWith6HourData.size() + " " + Bucket.SIX_HOUR + ", " + scheduleIdsWith24HourData.size() +
                " " + Bucket.TWENTY_FOUR_HOUR + " metrics in " + stopwatch.elapsed(TimeUnit.SECONDS) + " sec");
        } catch (IOException e) {
            throw new RuntimeException("There was an unexpected I/O error. There are unfinished metrics migrations " +
                "- " + getRemainingMetrics() + ". The upgrade will have to be run again.");
        } catch (AbortedException e) {
            throw new RuntimeException("The key scan was aborted. The upgrade will have to be rerun.", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("The migration was interrupted. There are still " + getRemainingMetrics() +
                " unfinished metrics migrations. The upgrade will have to be run again.");
        } finally {
            shutdown();
        }
    }

    private String getRemainingMetrics() {
        return "{" + Bucket.ONE_HOUR + ": " + remaining1HourMetrics.getCount() + ", " + Bucket.SIX_HOUR + ": " +
            remaining6HourMetrics.getCount() + ", " + Bucket.TWENTY_FOUR_HOUR + ": " +
            remaining24HourMetrics.getCount() + "}";
    }

    private double getWriteLimit(int numNodes) {
        int baseLimit = Integer.parseInt(System.getProperty("rhq.storage.request.write-limit", "10000"));
        double increase = baseLimit * RATE_INCREASE_PER_NODE;
        return baseLimit + (increase * (numNodes - 1));
    }

    private double getReadLimit(int numNodes) {
        int baseLimit = Integer.parseInt(System.getProperty("rhq.storage.request.read-limit", "25"));
        double increase = baseLimit * RATE_INCREASE_PER_NODE;
        return baseLimit + (increase * (numNodes - 1));
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

    private void shutdown() {
        if (dbConnectionFactory != null) {
            try {
                log.info("Shutting down migration thread pools...");
                rateMonitor.shutdown();
                progressLogger.finished();
                keyScanner.shutdown();
                threadPool.shutdown();
                threadPool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
    }

    private void migrate1HourData(Set<Integer> scheduleIds) throws IOException, InterruptedException {
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(14));
        PreparedStatement query = session.prepare(
            "SELECT time, type, value FROM rhq.one_hour_metrics " +
            "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());
        PreparedStatement delete = session.prepare("DELETE FROM rhq.one_hour_metrics WHERE schedule_id = ?");
        migrateData(scheduleIds, query, delete, Bucket.ONE_HOUR, remaining1HourMetrics, Days.days(14));
    }

    private void migrate6HourData(Set<Integer> scheduleIds) throws IOException, InterruptedException {
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(31));
        PreparedStatement query = session.prepare(
            "SELECT time, type, value FROM rhq.six_hour_metrics " +
            "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());
        PreparedStatement delete = session.prepare("DELETE FROM rhq.six_hour_metrics WHERE schedule_id = ?");
        migrateData(scheduleIds, query, delete, Bucket.SIX_HOUR, remaining6HourMetrics, Days.days(31));
    }

    private void migrate24HourData(Set<Integer> scheduleIds) throws IOException, InterruptedException {
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(365));
        PreparedStatement query = session.prepare(
            "SELECT time, type, value FROM rhq.twenty_four_hour_metrics " +
            "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());
        PreparedStatement delete = session.prepare("DELETE FROM rhq.twenty_four_hour_metrics WHERE schedule_id = ?");
        migrateData(scheduleIds, query, delete, Bucket.TWENTY_FOUR_HOUR, remaining24HourMetrics, Days.days(365));
    }

    private void migrateData(Set<Integer> scheduleIds, PreparedStatement query, PreparedStatement delete, Bucket bucket,
        CountDownLatch remainingMetrics, Days ttl) throws IOException, InterruptedException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("Starting data migration for " + bucket + " data");
        final MigrationLog migrationLog = new MigrationLog(new File(dataDir, bucket + "_migration.log"));
        final Set<Integer> migratedScheduleIds = migrationLog.read();
        int count = 1;
        boolean isLast = false;

        for (final Integer scheduleId : scheduleIds) {
            if (count++ == scheduleIds.size()) {
                log.info("last schedule id = " + scheduleId);
                isLast = true;
            }
            if (migratedScheduleIds.contains(scheduleId)) {
                if (log.isDebugEnabled()) {
                    log.debug(bucket + " data for schedule id " + scheduleId + " has already been migrated");
                }
                remainingMetrics.countDown();
            } else {
                if (isLast) {
                    log.info("Starting migration for last schedule id");
                }
                migrateData(query, delete, bucket, remainingMetrics, migrationLog, scheduleId, ttl, isLast);
            }
        }

        long remaining = remainingMetrics.getCount();
        count = 0;
        while (true) {
            log.info("remaining migrations: " + remainingMetrics.getCount());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            if (remainingMetrics.getCount() == remaining) {
                ++count;
            } else {
                count = 0;
                remaining = remainingMetrics.getCount();
            }
            if (count == 3) {
                log.warn("Migration is blocked");
                log.info("threadPool = " + executor);
                log.info("worker queue = " + executor.getQueue());
                for (Thread t : threadFactory.getThreads()) {
                    log.info("\nstack trace for " + t);
                    t.dumpStack();
                }
                throw new RuntimeException("Migration is blocked!");
            }
            if (remainingMetrics.getCount() == 0) {
                break;
            }
        }
        remainingMetrics.await();
        dropTable(bucket);
        stopwatch.stop();
        log.info("Finished migrating " + bucket + " data in " + stopwatch.elapsed(TimeUnit.SECONDS) + " sec");
    }

    private void migrateData(PreparedStatement query, PreparedStatement delete, Bucket bucket,
        CountDownLatch remainingMetrics, MigrationLog migrationLog, final Integer scheduleId, Days ttl, boolean isLast) {
        try {
            ListenableFuture<ResultSet> queryFuture = queryExecutor.executeRead(query.bind(scheduleId));
            queryFuture = Futures.withFallback(queryFuture, countReadErrors);
            ListenableFuture<List<ResultSet>> migrationFuture = Futures.transform(queryFuture,
                new MigrateData(scheduleId, bucket, queryExecutor, ttl.toStandardSeconds(), writeErrors, threadPool,
                    rateMonitor), threadPool);
            migrationFuture = Futures.withFallback(migrationFuture, countWriteErrors);
              Futures.addCallback(migrationFuture, migrationFinished(query, delete, scheduleId, bucket, migrationLog,
                remainingMetrics, ttl, isLast), threadPool);
        } catch (Exception e) {
            log.warn("FAILED to submit " + bucket + " data migration tasks for schedule id " + scheduleId, e);
        }
    }

    private FutureCallback<List<ResultSet>> migrationFinished(final PreparedStatement query, final PreparedStatement delete,
        final Integer scheduleId, final Bucket bucket, final MigrationLog migrationLog,
        final CountDownLatch remainingMetrics, final Days ttl, final boolean isLast) {

        return new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(List<ResultSet> resultSet) {
                try {
                    remainingMetrics.countDown();
                    if (isLast) {
                        log.info("Last metric finished. There are " + remainingMetrics.getCount() + " remaining metrics");
                    }
                    migrationLog.write(scheduleId);
                    rateMonitor.requestSucceeded();
                    migrationsMeter.mark();
                    if (log.isDebugEnabled()) {
                        log.debug("Finished migrating " + bucket + " data for schedule id " + scheduleId);
                    }
                } catch (IOException e) {
                    log.warn("Failed to log successful migration of " + bucket + " data for schedule id " +
                        scheduleId, e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to migrate " + bucket + " data for schedule id " + scheduleId +
                        ". Migration will be rescheduled.", t);
                } else {
                    log.info("Failed to migrate " + bucket + " data for schedule id " + scheduleId + ": " +
                        ThrowableUtil.getRootMessage(t) + ". Migration will be rescheduled");
                }
                rateMonitor.requestFailed();
                try {
                    migrateData(query, delete, bucket, remainingMetrics, migrationLog, scheduleId, ttl, isLast);
                } catch (Exception e) {
                    log.warn("FAILED to resubmit " + bucket + " data migration task for schedule id " + scheduleId);
                }
            }
        };
    }

    private class MigrationProgressLogger implements Runnable {

        private boolean finished;

        private boolean reportMigrationRates;

        public void finished() {
            finished = true;
        }

        @Override
        public void run() {
            try {
                while (!finished) {
                    log.info("Remaining metrics to migrate\n" +
                        Bucket.ONE_HOUR + ": " + remaining1HourMetrics.getCount() + "\n" +
                        Bucket.SIX_HOUR + ": " + remaining6HourMetrics.getCount() + "\n" +
                        Bucket.TWENTY_FOUR_HOUR + ": " + remaining24HourMetrics.getCount() + "\n");
                    log.info("ErrorCounts{read:" + readErrors + ", write: " + writeErrors + "}");
                    if (reportMigrationRates) {
                        log.info("Metrics migration rates:\n" +
                            "1 min rate: "  + migrationsMeter.oneMinuteRate() + "\n" +
                            "5 min rate: " + migrationsMeter.fiveMinuteRate() + " \n" +
                            "15 min rate: " + migrationsMeter.fifteenMinuteRate() + "\n");
                        reportMigrationRates = false;
                    } else {
                        reportMigrationRates = true;
                    }

                    Thread.sleep(30000);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private void dropTable(Bucket bucket) {
        log.info("Dropping table " + bucket.getTableName());
        session.execute("DROP table rhq." + bucket.getTableName());
    }

    private Set<String> getTables() {
        ResultSet resultSet = session.execute("SELECT columnfamily_name FROM system.schema_columnfamilies " +
            "WHERE keyspace_name = 'rhq'");
        Set<String> tables = new HashSet<String>();

        for (Row row : resultSet) {
            tables.add(row.getString(0));
        }

        return tables;
    }

}
