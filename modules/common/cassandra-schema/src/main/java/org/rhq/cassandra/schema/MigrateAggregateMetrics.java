package org.rhq.cassandra.schema;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
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

import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.KeyIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

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

    private String dataDir;

    private DBConnectionFactory dbConnectionFactory;

    private RateLimiter readPermits;

    private RateLimiter writePermits;

    private AtomicInteger remaining1HourMetrics = new AtomicInteger();

    private AtomicInteger remaining6HourMetrics = new AtomicInteger();

    private AtomicInteger remaining24HourMetrics = new AtomicInteger();

    private AtomicInteger failedMigrations = new AtomicInteger();

    private ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5,
        new SchemaUpdateThreadFactory()));

    private ExecutorService scheduler = Executors.newFixedThreadPool(1);

    private TaskTracker migrations = new TaskTracker();

    private Meter migrationsMeter;

    private MetricsRegistry metricsRegistry;

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
            writePermits = RateLimiter.create(getWriteLimit() * getNumberOfUpNodes(), 10, TimeUnit.SECONDS);
            readPermits = RateLimiter.create(getReadLimit() * getNumberOfUpNodes(), 90, TimeUnit.SECONDS);
            Keyspace keyspace = createKeyspace();
            MigrationProgressLogger progressLogger = new MigrationProgressLogger();

            threadPool.submit(progressLogger);

            schedule1HourDataMigrations(keyspace);
            schedule6HourDataMigrations(keyspace);
            schedule24HourDataMigrations(keyspace);

            migrations.finishedSchedulingTasks();
            migrations.waitForTasksToFinish();
            progressLogger.finished();

            if (failedMigrations.get() > 0) {
                throw new RuntimeException("There were " + failedMigrations + " failed migration tasks. The upgrade " +
                    "needs to be run again in order to complete the migration.");
            }
            dropTables();
        } catch (IOException e) {
            throw new RuntimeException("There was an unexpected I/O error. The are still " +
                migrations.getRemainingTasks() + " outstanding migration tasks. The upgrade must be run again to " +
                "complete the migration.", e);
        } catch (AbortedException e) {
            throw new RuntimeException("The migration was aborted. There are are still " +
                migrations.getRemainingTasks() +" outstanding migration tasks. The upgrade must be run again to " +
                "complete the migration.", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("The migration was interrupted. There are are still " +
                migrations.getRemainingTasks() +" outstanding migration tasks. The upgrade must be run again to " +
                "complete the migration.", e);
        } finally {
            stopwatch.stop();
            log.info("Finished data migration in " + stopwatch.elapsed(TimeUnit.SECONDS) + " sec");
        }
    }

    private int getWriteLimit() {
        return Integer.parseInt(System.getProperty("rhq.storage.request.limit", "20000"));
    }

    private int getReadLimit() {
        return Integer.parseInt(System.getProperty("rhq.storage.request.read-limit", "100"));
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

    private Keyspace createKeyspace() {
        CassandraHostConfigurator configurator = new CassandraHostConfigurator("127.0.01");
        Map<String, String> credentials = ImmutableMap.of(
            "username", "rhqadmin",
            "password", "rhqadmin"
        );
        me.prettyprint.hector.api.Cluster cluster = HFactory.createCluster("rhq", configurator, credentials);
        return HFactory.createKeyspace("rhq", cluster);
    }

    private Iterator<Integer> createKeyIterator(Keyspace keyspace, String table) {
        KeyIterator<Integer> keyIterator = new KeyIterator<Integer>(keyspace, table, IntegerSerializer.get());
        return keyIterator.iterator();
    }

    private void schedule1HourDataMigrations(Keyspace keyspace) throws IOException {
        Iterator<Integer> keyIterator = createKeyIterator(keyspace, "one_hour_metrics");
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(14));
        PreparedStatement query = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.one_hour_metrics " +
                "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());

        scheduleMigrations(keyIterator, query, Bucket.ONE_HOUR, remaining1HourMetrics);
    }

    private void schedule6HourDataMigrations(Keyspace keyspace) throws IOException {
        Iterator<Integer> keyIterator = createKeyIterator(keyspace, "six_hour_metrics");
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(31));
        PreparedStatement query = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.six_hour_metrics " +
                "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());

        scheduleMigrations(keyIterator, query, Bucket.SIX_HOUR, remaining6HourMetrics);
    }

    private void schedule24HourDataMigrations(Keyspace keyspace) throws IOException {
        Iterator<Integer> keyIterator = createKeyIterator(keyspace, "twenty_four_hour_metrics");
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(365));
        PreparedStatement query = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.twenty_four_hour_metrics " +
                "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());

        scheduleMigrations(keyIterator, query, Bucket.TWENTY_FOUR_HOUR, remaining24HourMetrics);
    }

    private void scheduleMigrations(Iterator<Integer> keyIterator, final PreparedStatement query, final Bucket bucket,
        final AtomicInteger remainingMetrics) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("Scanning for schedule ids with " + bucket + " data");
        final MigrationLog migrationLog = new MigrationLog(new File(dataDir, bucket + "_migration.log"));
        final Set<Integer> migratedScheduleIds = migrationLog.read();
        int count = 0;

        while (keyIterator.hasNext()) {
            final Integer scheduleId = keyIterator.next();
            count++;
            remainingMetrics.incrementAndGet();
            if (log.isDebugEnabled()) {
                log.debug("Scheduling " + bucket + " data migration for schedule id " + scheduleId);
            }
            migrations.addTask();
//            migrationsQeue.offer(new MigrationArgs(scheduleId, bucket, query, migratedScheduleIds, migrationLog));
            scheduler.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!migratedScheduleIds.contains(scheduleId)) {
                            readPermits.acquire();
                            ResultSetFuture queryFuture = session.executeAsync(query.bind(scheduleId));
                            ListenableFuture<List<ResultSet>> migrationFuture = Futures.transform(queryFuture,
                                new MigrateData(scheduleId, bucket, writePermits, session), threadPool);
                            Futures.addCallback(migrationFuture, migrationFinished(scheduleId, bucket, migrationLog,
                                remainingMetrics), threadPool);
                        }
                    } catch (Exception e) {
                        log.warn("There was an error migrating data", e);
                    }
                }
            });
        }

        stopwatch.stop();
        log.info("Finished scanning for schedule ids with " + bucket + " data in " +
            stopwatch.elapsed(TimeUnit.SECONDS) + " sec");
        log.info("There are a total of " + count + " schedule ids with " + bucket + " data to migrate");
    }

    private FutureCallback<List<ResultSet>> migrationFinished(final Integer scheduleId, final Bucket bucket,
        final MigrationLog migrationLog, final AtomicInteger remainingMetrics) {
        return new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(List<ResultSet> resultSets) {
                try {
                    migrations.finishedTask();
                    remainingMetrics.decrementAndGet();
                    migrationLog.write(scheduleId);
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
                log.info("Failed to migrate " + bucket + " data for schedule id " + scheduleId);
                migrations.finishedTask();
                migrationsMeter.mark();
                remainingMetrics.decrementAndGet();
                failedMigrations.incrementAndGet();
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
                        Bucket.ONE_HOUR + ": " + remaining1HourMetrics + "\n" +
                        Bucket.SIX_HOUR + ": " + remaining6HourMetrics + "\n" +
                        Bucket.TWENTY_FOUR_HOUR + ": " + remaining24HourMetrics + "\n");
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

}
