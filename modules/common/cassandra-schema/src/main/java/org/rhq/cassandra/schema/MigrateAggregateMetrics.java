package org.rhq.cassandra.schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.io.LineReader;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.SimpleAuthenticationCredentials;
import com.netflix.astyanax.connectionpool.impl.Slf4jConnectionPoolMonitorImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Composite;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.recipes.reader.AllRowsReader;
import com.netflix.astyanax.serializers.CompositeSerializer;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricsRegistry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;

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

    public static final int DEFAULT_WARM_UP = 20;

    private static final double RATE_INCREASE_PER_NODE = 0.3;

    private Session session;

    private String dataDir;

    private DBConnectionFactory dbConnectionFactory;

    private AtomicReference<RateLimiter> readPermitsRef = new AtomicReference<RateLimiter>();

    private AtomicReference<RateLimiter> writePermitsRef = new AtomicReference<RateLimiter>();

    private AtomicInteger remaining1HourMetrics = new AtomicInteger();

    private AtomicInteger remaining6HourMetrics = new AtomicInteger();

    private AtomicInteger remaining24HourMetrics = new AtomicInteger();

    private AtomicInteger migrated1HourMetrics = new AtomicInteger();

    private AtomicInteger migrated6HourMetrics = new AtomicInteger();

    private AtomicInteger migrated24HourMetrics = new AtomicInteger();

    private AtomicInteger failedMigrations = new AtomicInteger();

    private ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(6,
        new SchemaUpdateThreadFactory()));

    private MetricsRegistry metricsRegistry;

    private Meter migrationsMeter;

    private TaskTracker migrations = new TaskTracker();

    private int totalMetrics;

    private double failureThreshold = Double.parseDouble(System.getProperty("rhq.storage.failure-threshold", "0.05"));

    private RateMonitor rateMonitor;

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

            AstyanaxContext<Keyspace> context = createContext();
            context.start();
            Keyspace keyspace = context.getClient();

            Set<Integer> scheduleIdsWith1HourData = loadScheduleIds(keyspace, ColumnFamily.newColumnFamily(
                "one_hour_metrics", IntegerSerializer.get(), CompositeSerializer.get()), Bucket.ONE_HOUR);
            Set<Integer> scheduleIdsWith6HourData = loadScheduleIds(keyspace, ColumnFamily.newColumnFamily(
                "six_hour_metrics", IntegerSerializer.get(), CompositeSerializer.get()), Bucket.SIX_HOUR);
            Set<Integer> scheduleIdsWith24HourData = loadScheduleIds(keyspace, ColumnFamily.newColumnFamily(
                "twenty_four_hour_metrics", IntegerSerializer.get(), CompositeSerializer.get()),
                Bucket.TWENTY_FOUR_HOUR);

            Stopwatch contextStopWatch = Stopwatch.createStarted();
            context.shutdown();
            contextStopWatch.stop();
            log.info("Shut down astyanax in " + contextStopWatch.elapsed(TimeUnit.MILLISECONDS) + " ms");

            writePermitsRef.set(RateLimiter.create(getWriteLimit(getNumberOfUpNodes()), DEFAULT_WARM_UP,
                TimeUnit.SECONDS));
            readPermitsRef.set(RateLimiter.create(getReadLimit(getNumberOfUpNodes()), DEFAULT_WARM_UP,
                TimeUnit.SECONDS));

            log.info("The request limits are " + writePermitsRef.get().getRate() + " writes/sec and " +
                readPermitsRef.get().getRate() + " reads/sec");

            remaining1HourMetrics.set(scheduleIdsWith1HourData.size());
            remaining6HourMetrics.set(scheduleIdsWith6HourData.size());
            remaining24HourMetrics.set(scheduleIdsWith24HourData.size());
            totalMetrics = remaining1HourMetrics.get() + remaining6HourMetrics.get() + remaining24HourMetrics.get();

            MigrationProgressLogger progressLogger = new MigrationProgressLogger();
            rateMonitor = new RateMonitor(readPermitsRef, writePermitsRef);

            threadPool.submit(progressLogger);
            threadPool.submit(rateMonitor);

            migrate1HourData(scheduleIdsWith1HourData);
            migrate6HourData(scheduleIdsWith6HourData);
            migrate24HourData(scheduleIdsWith24HourData);

            migrations.finishedSchedulingTasks();
            migrations.waitForTasksToFinish();
            rateMonitor.shutdown();
            progressLogger.finished();
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
            log.info("There were " + failedMigrations + " failed migrations");
            shutdown();
        }
    }

    private AstyanaxContext<Keyspace> createContext() {
        return new AstyanaxContext.Builder()
            .forCluster("rhq")
            .forKeyspace("rhq")
            .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE))
            .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("astyanax_connection_pool")
                .setPort(9160)
                .setSeeds(System.getProperty("rhq.storage.nodes"))
                .setMaxConnsPerHost(5)
                .setConnectTimeout(10000)
                .setAuthenticationCredentials(new SimpleAuthenticationCredentials("rhqadmin", "rhqadmin")))
            .withConnectionPoolMonitor(new Slf4jConnectionPoolMonitorImpl())
            .buildKeyspace(ThriftFamilyFactory.getInstance());
    }

    private double getWriteLimit(int numNodes) {
        int baseLimit = Integer.parseInt(System.getProperty("rhq.storage.request.write-limit", "20000"));
        double increase = baseLimit * RATE_INCREASE_PER_NODE;
        return baseLimit + (increase * (numNodes - 1));
    }

    private double getReadLimit(int numNodes) {
        int baseLimit = Integer.parseInt(System.getProperty("rhq.storage.request.read-limit", "200"));
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
        try {
            log.info("Shutting down migration thread pools...");
            threadPool.shutdown();
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    private Set<Integer> loadScheduleIds() {
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
            return scheduleIds;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schedule ids");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to load schedule ids");
        }
    }

    private Set<Integer> loadScheduleIds(Keyspace keyspace, ColumnFamily<Integer, Composite> table, Bucket bucket) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        final Set<Integer> scheduleIds = new ConcurrentSkipListSet<Integer>();
        try {
            log.info("Scanning for schedule ids with " + bucket + " data");
            int concurrencyLevel = Integer.parseInt(System.getProperty("astyanax.concurrency-level", "4"));
            new AllRowsReader.Builder<Integer, Composite>(keyspace, table)
                .withColumnRange(null, null, false, 0)
                .withPartitioner(null) // this will use keyspace's partitioner
                .withConcurrencyLevel(concurrencyLevel)
                .forEachRow(new Function<Row<Integer, Composite>, Boolean>() {
                    @Override
                    public Boolean apply(Row<Integer, Composite> row) {
                        boolean added = scheduleIds.add(row.getKey());
                        if (!added) {
                            log.info("schedule id " + row.getKey() + " has already been loaded");
                        }
                        return true;
                    }
                })
            .build()
            .call();
            return scheduleIds;
        } catch (Exception e) {
            throw new RuntimeException("There was an unexpected error scanning scanning for schedule ids with " +
                bucket + " data. The migration will have to be rerun.", e);
        } finally {
            stopwatch.stop();
            log.info("Found " + scheduleIds.size() + " schedule ids with " + bucket + " data in " +
                stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private void migrate1HourData(Set<Integer> scheduleIds) throws IOException {
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(14));
        PreparedStatement query = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.one_hour_metrics " +
                "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());
        PreparedStatement delete = session.prepare("DELETE FROM rhq.one_hour_metrics WHERE schedule_id = ?");
        migrateData(scheduleIds, query, delete, Bucket.ONE_HOUR, remaining1HourMetrics, migrated1HourMetrics);
    }

    private void migrate6HourData(Set<Integer> scheduleIds) throws IOException {
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(14));
        PreparedStatement query = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.six_hour_metrics " +
                "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());
        PreparedStatement delete = session.prepare("DELETE FROM rhq.six_hour_metrics WHERE schedule_id = ?");
        migrateData(scheduleIds, query, delete, Bucket.SIX_HOUR, remaining6HourMetrics, migrated6HourMetrics);
    }

    private void migrate24HourData(Set<Integer> scheduleIds) throws IOException {
        DateTime endTime = DateUtils.get1HourTimeSlice(DateTime.now());
        DateTime startTime = endTime.minus(Days.days(14));
        PreparedStatement query = session.prepare(
            "SELECT schedule_id, time, type, value, ttl(value), writetime(value) FROM rhq.twenty_four_hour_metrics " +
                "WHERE schedule_id = ? AND time >= " + startTime.getMillis() + " AND time <= " + endTime.getMillis());
        PreparedStatement delete = session.prepare("DELETE FROM rhq.twenty_four_hour_metrics WHERE schedule_id = ?");
        migrateData(scheduleIds, query, delete, Bucket.TWENTY_FOUR_HOUR, remaining24HourMetrics, migrated24HourMetrics);
    }

    private void migrateData(Set<Integer> scheduleIds, PreparedStatement query, final PreparedStatement delete, 
        Bucket bucket, final AtomicInteger remainingMetrics, final AtomicInteger migratedMetrics) throws IOException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("Scheduling data migration tasks for " + bucket + " data");
        final MigrationLog migrationLog = new MigrationLog(new File(dataDir, bucket + "_migration.log"));
        final Set<Integer> migratedScheduleIds = migrationLog.read();

        for (final Integer scheduleId : scheduleIds) {
            if (!migratedScheduleIds.contains(scheduleId)) {
                migrations.addTask();
                migrateData(query, delete, bucket, remainingMetrics, migratedMetrics, migrationLog, scheduleId);
            }
        }

        stopwatch.stop();
        log.info("Finished scheduling migration tasks for " + bucket + " data in " +
            stopwatch.elapsed(TimeUnit.SECONDS) + " sec");
    }

    private void migrateData(PreparedStatement query, final PreparedStatement delete, Bucket bucket,
        AtomicInteger remainingMetrics, AtomicInteger migratedMetrics, MigrationLog migrationLog,
        final Integer scheduleId) {
        readPermitsRef.get().acquire();
        ResultSetFuture queryFuture = session.executeAsync(query.bind(scheduleId));
        ListenableFuture<List<ResultSet>> migrationFuture = Futures.transform(queryFuture,
            new MigrateData(scheduleId, bucket, writePermitsRef.get(), session), threadPool);
        ListenableFuture<ResultSet> deleteFuture = Futures.transform(migrationFuture,
            new AsyncFunction<List<ResultSet>, ResultSet>() {
                @Override
                public ListenableFuture<ResultSet> apply(List<ResultSet> resultSets) throws Exception {
                    writePermitsRef.get().acquire();
                    return session.executeAsync(delete.bind(scheduleId));
                }
            }, threadPool);
        Futures.addCallback(deleteFuture, migrationFinished(query, delete, scheduleId, bucket, migrationLog,
            remainingMetrics, migratedMetrics), threadPool);
    }

    private FutureCallback<ResultSet> migrationFinished(final PreparedStatement query, final PreparedStatement delete,
        final Integer scheduleId, final Bucket bucket, final MigrationLog migrationLog, final AtomicInteger
        remainingMetrics, final AtomicInteger migratedMetrics) {

        return new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet deleteResultSet) {
                try {
                    migrations.finishedTask();
                    remainingMetrics.decrementAndGet();
                    migratedMetrics.incrementAndGet();
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
                log.info("Failed to migrate " + bucket + " data for schedule id " + scheduleId +
                    ". Migration will be rescheduled");
                rateMonitor.requestFailed();
                failedMigrations.incrementAndGet();
                migrateData(query, delete, bucket, remainingMetrics, migratedMetrics, migrationLog, scheduleId);
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
                    log.info("Failed migrations: " + failedMigrations);
                    if (reportMigrationRates) {
                        log.info("Metrics migration rates:\n" +
                            "1 min rate: "  + migrationsMeter.oneMinuteRate() + "\n" +
                            "5 min rate: " + migrationsMeter.fiveMinuteRate() + " \n" +
                            "15 min rate: " + migrationsMeter.fifteenMinuteRate() + "\n");
                        reportMigrationRates = false;
                    } else {
                        reportMigrationRates = true;
                    }

//                    if (totalMetrics > 0 && (failedMigrations.get() / totalMetrics) > failureThreshold) {
//                        migrations.abort("The failure threshold has been exceeded with " + failedMigrations.get() +
//                            " failed migrations");
//                    }

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
