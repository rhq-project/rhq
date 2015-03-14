package org.rhq.cassandra.schema;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;

/**
 * @author John Sanda
 */
public class ReplaceRHQ411Index {

    private static final Log log = LogFactory.getLog(ReplaceRHQ411Index.class);

    private static final int NUM_PARTITIONS = 10;

    private static final int PAGE_SIZE = Integer.parseInt(System.getProperty("rhq.metrics.index.page-size", "2500"));

    private Session session;

    private PreparedStatement find411IndexEntries;

    private PreparedStatement find411IndexEntriesAfterScheduleId;

    private PreparedStatement updateNewIndex;

    private ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4,
        new SchemaUpdateThreadFactory()));

    private AtomicReference<RateLimiter> writePermitsRef = new AtomicReference<RateLimiter>();

    private RateMonitor rateMonitor;

    public ReplaceRHQ411Index(Session session) {
        this.session = session;
    }

    public void execute(DateRanges dateRanges) {
        log.info("Updating indexes");
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            waitForSchemaPropagation();

            initPreparedStatements();

            writePermitsRef.set(RateLimiter.create(Integer.parseInt(System.getProperty(
                "rhq.storage.request.write-limit", "20000"))));
            // We only care about throttling writes, but RateMonitor expects a RateLimiter
            // for both writes and reads.
            AtomicReference<RateLimiter> readPermitsRef = new AtomicReference<RateLimiter>(
                RateLimiter.create(100));

            rateMonitor = new RateMonitor(readPermitsRef, writePermitsRef);
            threadPool.submit(rateMonitor);

            updateRawIndex(dateRanges.rawStartTime, dateRanges.rawEndTime);
            update1HourIndex(dateRanges.oneHourStartTime, dateRanges.oneHourEndTime);
            update6HourIndex(dateRanges.sixHourStartTime, dateRanges.sixHourEndTime);
            drop411Index();
        } catch (InterruptedException e) {
            throw new RuntimeException("The index update failed due to an interrupt", e);
        } finally {
            stopwatch.stop();
            shutdown();
            log.info("Finished updating indexes in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    private void shutdown() {
        rateMonitor.shutdown();
        threadPool.shutdown();
    }

    private void initPreparedStatements() {
        find411IndexEntries = session.prepare(
            "SELECT schedule_id FROM rhq.metrics_index WHERE bucket = ? AND time = ? LIMIT " + PAGE_SIZE);

        find411IndexEntriesAfterScheduleId = session.prepare(
            "SELECT schedule_id FROM rhq.metrics_index WHERE bucket = ? AND time = ? AND schedule_id > ? " +
            "LIMIT " + PAGE_SIZE);

        updateNewIndex = session.prepare(
            "INSERT INTO rhq.metrics_idx (bucket, partition, time, schedule_id) VALUES (?, ?, ?, ?)");
    }

    private void waitForSchemaPropagation() throws InterruptedException {
        for (int i = 0; i < 10; ++i) {
            ResultSet resultSet = session.execute("SELECT columnfamily_name FROM system.schema_columnfamilies " +
                "WHERE keyspace_name = 'rhq' AND columnfamily_name = 'metrics_idx'");
            if (resultSet.isExhausted()) {
                Thread.sleep(1000);
            } else {
                return;
            }
        }
        throw new RuntimeException("The metrics_idx table does not exist. The upgrade needs to be rerun to ensure " +
            "that the index table is created and is updated.");
    }

    private void updateRawIndex(DateTime start, DateTime end) {
        log.info("Updating raw index");
        updateIndex("one_hour_metrics", "raw", start, end, Hours.ONE.toStandardDuration());
    }

    private void update1HourIndex(DateTime start, DateTime end) {
        log.info("Updating one_hour index");
        updateIndex("six_hour_metrics", "one_hour", start, end, Hours.SIX.toStandardDuration());
    }

    private void update6HourIndex(DateTime start, DateTime end) {
        log.info("Updating six_hour index");
        updateIndex("twenty_four_hour_metrics", "six_hour", start, end, Days.ONE.toStandardDuration());
    }

    private void updateIndex(String oldBucket, String newBucket, DateTime start, DateTime end, Duration timeSlice) {
        try {
            DateTime time = start;
            BoundStatement statement = find411IndexEntries.bind(oldBucket, start.toDate());
            ResultSet resultSet = session.execute(statement);
            int count = 0;
            int scheduleId = 0;
            final TaskTracker taskTracker = new TaskTracker();

            do {
                for (Row row : resultSet) {
                    scheduleId = row.getInt(0);
                    ++count;
                    addScheduleIdToIndex(newBucket, timeSlice, time, scheduleId, taskTracker);
                }
                if (count < PAGE_SIZE) {
                    time = DateUtils.plusDSTAware(time, timeSlice);
                    statement = find411IndexEntries.bind(oldBucket, time.toDate());
                } else {
                    statement = find411IndexEntriesAfterScheduleId.bind(oldBucket, time.toDate(), scheduleId);
                }
                count = 0;
                resultSet = session.execute(statement);
            } while (!time.isAfter(end));

            taskTracker.finishedSchedulingTasks();
            taskTracker.waitForTasksToFinish();
        } catch (InterruptedException e) {
            throw new RuntimeException("The index update did not complete due to an interrupt", e);
        } catch (AbortedException e) {
            throw new RuntimeException("The index update was aborted", e);
        }
    }

    private void addScheduleIdToIndex(String newBucket, Duration timeSlice, DateTime time, int scheduleId,
        final TaskTracker taskTracker) {

        taskTracker.addTask();
        int partition = (scheduleId % NUM_PARTITIONS);
        writePermitsRef.get().acquire();
        ResultSetFuture insertFuture = session.executeAsync(updateNewIndex.bind(newBucket, partition,
            DateUtils.getUTCTimeSlice(time, timeSlice).toDate(), scheduleId));
        Futures.addCallback(insertFuture, indexUpdated(newBucket, timeSlice, time, scheduleId, taskTracker),
            threadPool);
    }

    private FutureCallback<ResultSet> indexUpdated(final String newBucket, final Duration timeSlice,
        final DateTime time, final int scheduleId, final TaskTracker taskTracker) {

        return new FutureCallback<ResultSet>() {
            @Override
            public void onSuccess(ResultSet resultSet) {
                taskTracker.finishedTask();
            }

            @Override
            public void onFailure(Throwable t) {
                rateMonitor.requestFailed();
                addScheduleIdToIndex(newBucket, timeSlice, time, scheduleId, taskTracker);
            }
        };
    }

    private void drop411Index() {
        log.info("Dropping table metrics_index");
        session.execute("DROP table rhq.metrics_index");
    }

}
