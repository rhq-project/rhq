package org.rhq.cassandra.schema;

import static org.testng.Assert.assertEquals;
import static org.testng.FileAssert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;
import org.testng.annotations.Test;

/**
 * @author John Sanda
 */
public class ReplaceIndexTest extends SchemaUpgradeTest {

    private static final int NUM_PARTITIONS = 5;

    private Session session;

    @Test
    public void replace411Index() throws Exception {
        SchemaManager schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
            new String[] {"127.0.0.1"}, 9042);
        schemaManager.setUpdateFolderFactory(new TestUpdateFolderFactory(VersionManager.Task.Update.getFolder())
            .removeFiles("0006.xml", "0007.xml"));
        schemaManager.drop();
        schemaManager.shutdown();
        schemaManager.install(new Properties());
        schemaManager.shutdown();

        Cluster cluster = new Cluster.Builder()
            .addContactPoint("127.0.0.1")
            .withCredentials("rhqadmin", "rhqadmin")
            .build();
        session = cluster.connect("rhq");

        System.setProperty("rhq.metrics.index.page-size", "5");

        DateTime endTime =  DateTime.now().hourOfDay().roundFloorCopy();
        DateTime startTime = endTime.minusDays(3);
        DateTime today = endTime.hourOfDay().roundFloorCopy().minusHours(endTime.hourOfDay().roundFloorCopy()
            .hourOfDay().get());

        populateRaw411Index(startTime, startTime.plusDays(1), scheduleIds(100, 112));
        populateRaw411Index(startTime.plusDays(1), startTime.plusDays(2), scheduleIds(100, 109));
        populateRaw411Index(endTime.minusHours(1), endTime, scheduleIds(105, 123));
        populate1Hour411Index(today, today.plusHours(6), scheduleIds(105, 123));
        populate6Hour411Index(today, today.plusDays(1), scheduleIds(100, 125));

        schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
            new String[] {"127.0.0.1"}, 9042);
        schemaManager.install(new Properties());
        schemaManager.shutdown();

        assertRawIndexUpdated(startTime, startTime.plusDays(1), scheduleIds(100, 112));
        assertRawIndexUpdated(startTime.plusDays(1), startTime.plusDays(2), scheduleIds(100, 109));
        assertRawIndexUpdated(endTime.minusHours(1), endTime, scheduleIds(105, 123));
        assert1HourIndexUpdated(today, today.plusHours(6), scheduleIds(105, 123));
        assert6HourIndexUpdated(today, today.plusDays(1), scheduleIds(100, 125));
    }

    private void populateRaw411Index(DateTime startTime, DateTime endTime, List<Integer> scheduleIds) {
        updateIndex("one_hour_metrics", startTime, endTime, Hours.ONE.toStandardDuration(), scheduleIds);
    }

    private void populate1Hour411Index(DateTime startTime, DateTime endTime, List<Integer> scheduleIds) {
        updateIndex("six_hour_metrics", startTime, endTime, Hours.SIX.toStandardDuration(), scheduleIds);
    }

    private void populate6Hour411Index(DateTime startTime, DateTime endTime, List<Integer> scheduleIds) {
        updateIndex("twenty_four_hour_metrics", startTime, endTime, Days.ONE.toStandardDuration(), scheduleIds);
    }

    private void updateIndex(String bucket, DateTime startTime, DateTime endTime, Duration timeSlice,
        List<Integer> scheduleIds) {
        DateTime time = startTime;
        while (time.isBefore(endTime)) {
            for (Integer scheduleId : scheduleIds) {
                session.execute("insert into metrics_index (bucket, time, schedule_id) values ('" + bucket +
                    "', " + time.getMillis() + ", " + scheduleId + ")");
            }
            time = time.plus(timeSlice);
        }
    }

    private List<Integer> scheduleIds(int start, int end) {
        List<Integer> ids = new ArrayList<Integer>(end - start + 1);
        for (int i = start; i < end; ++i) {
            ids.add(i);
        }
        return ids;
    }

    private void assertRawIndexUpdated(DateTime startTime, DateTime endTime, List<Integer> expectedScheduleIds)
        throws InterruptedException {
        assertIndexUpdated("raw", startTime, endTime, Hours.ONE.toStandardDuration(), expectedScheduleIds);
    }

    private void assert1HourIndexUpdated(DateTime startTime, DateTime endTime, List<Integer> expectedScheduleIds)
        throws InterruptedException {
        assertIndexUpdated("one_hour", startTime, endTime, Hours.SIX.toStandardDuration(), expectedScheduleIds);
    }

    private void assert6HourIndexUpdated(DateTime startTime, DateTime endTime, List<Integer> expectedScheduleIds)
        throws InterruptedException {
        assertIndexUpdated("six_hour", startTime, endTime, Days.ONE.toStandardDuration(), expectedScheduleIds);
    }

    private void assertIndexUpdated(String bucket, DateTime startTime, DateTime endTime, Duration timeSlice,
        List<Integer> expectedScheduleIds) throws InterruptedException {
        DateTime time = startTime;
        while (time.isBefore(endTime)) {
            List<Integer> actualScheduleIds = loadIndex(bucket, time);
            // Put them schedule ids in a set before comparing. The order ids is not
            // important and will likely not match since loadIndex() merges results from
            // queries across multiple index partitions.
            Set<Integer> expected = ImmutableSet.copyOf(expectedScheduleIds);
            Set<Integer> actual = ImmutableSet.copyOf(actualScheduleIds);
            assertEquals(actual, expected, "The " + bucket + " data index is wrong for " + time);
            time = time.plus(timeSlice);
        }
    }

    private List<Integer> loadIndex(String bucket, DateTime time) throws InterruptedException {
        List<ResultSetFuture> queryFutures = new ArrayList<ResultSetFuture>(NUM_PARTITIONS);
        for (int i = 0; i < NUM_PARTITIONS; ++i) {
            queryFutures.add(session.executeAsync("select schedule_id from metrics_idx where bucket = '" + bucket +
                "' and partition = " + i + " and time = " + time.getMillis()));
        }
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final List<Integer> scheduleIds = new ArrayList<Integer>();
        final CountDownLatch latch = new CountDownLatch(1);
        ListenableFuture<List<ResultSet>> queriesFuture = Futures.allAsList(queryFutures);
        Futures.addCallback(queriesFuture, new FutureCallback<List<ResultSet>>() {
            @Override
            public void onSuccess(List<ResultSet> resultSets) {
                for (ResultSet resultSet : resultSets) {
                    for (Row row : resultSet) {
                        scheduleIds.add(row.getInt(0));
                    }
                }
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                error.set(t);
                latch.countDown();
            }
        });
        latch.await();
        if (error.get() != null) {
            fail("Failed to load " + bucket + " data", error.get());
        }
        return scheduleIds;
    }

}
