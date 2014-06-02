package org.rhq.cassandra.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;
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
import org.joda.time.Period;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * This is a custom step that performs data migration for users migrating to RHQ 4.11 from either 4.9 or 4.10. Before
 * 4.11 we had the numeric data tables and the metrics_index table which serves as a custom index. In 4.11, two new
 * tables are being added - metrics_cache and metrics_cache_index. The metrics_index table is being dropped. This
 * class performs several tasks.
 *
 * <ul>
 *   <li>Migrates data from metrics_index to metrics_cache</li>
 *   <li>Delete rows in metrics_index of migrated data</li>
 *   <li>Deactivates the metrics cache (if necessary)</li>
 *   <li>Drops the metrics_index table</li>
 * </ul>
 *
 * A row in metrics_index is deleted only after successfully migrating the data into metrics_cache_index. And at the
 * end of the process, metrics_index is dropped only if there are no errors. If there are any errors inserting data
 * into metrics_cache_index, an exception is thrown which will cause the installation/upgrade to fail. This allows
 * for the upgrade to be retried so that any remaining data in metrics_index can be migrated.
 *
 * @author John Sanda
 */
public class PopulateCacheIndex implements Step {

    private static final Log log = LogFactory.getLog(PopulateCacheIndex.class);

    private static final int CACHE_INDEX_PARTITION = 0;

    private static final String INDEX_TABLE = "metrics_index";

    private static final String CACHE_INDEX_TABLE = "metrics_cache_index";

    private static enum Bucket {

        RAW("raw_metrics"),
        ONE_HOUR("one_hour_metrics"),
        SIX_HOUR("six_hour_metrics"),
        TWENTY_FOUR_HOUR("twenty_four_hour_metrics");

        private String text;

        private Bucket(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    private class CacheIndexUpdatedCallback implements FutureCallback<ResultSet> {

        private Bucket bucket;

        private int scheduleId;

        private Date time;

        private CountDownLatch updatesFinished;

        public CacheIndexUpdatedCallback(Bucket bucket, int scheduleId, Date time, CountDownLatch updatesFinished) {
            this.bucket = bucket;
            this.scheduleId = scheduleId;
            this.time = time;
            this.updatesFinished = updatesFinished;
        }

        @Override
        public void onSuccess(ResultSet result) {
            permits.acquire();
            BoundStatement statement = deleteIndexEntry.bind(bucket.text(), time, scheduleId);
            ResultSetFuture future = session.executeAsync(statement);
            Futures.addCallback(future, new IndexUpdatedCallback(bucket, scheduleId, updatesFinished), tasks);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to update cache index for {bucket: " + bucket.text() + ", scheduleId: " + scheduleId +
                "}: ", ThrowableUtil.getRootCause(t));
            failedUpdates.incrementAndGet();
            updatesFinished.countDown();
        }
    }

    private class IndexUpdatedCallback implements FutureCallback<ResultSet> {

        private Bucket bucket;

        private int scheduleId;

        private CountDownLatch updatesFinished;

        public IndexUpdatedCallback(Bucket bucket, int scheduleId, CountDownLatch updatesFinished) {
            this.bucket = bucket;
            this.scheduleId = scheduleId;
            this.updatesFinished = updatesFinished;
        }

        @Override
        public void onSuccess(ResultSet result) {
            updatesFinished.countDown();
        }

        @Override
        public void onFailure(Throwable t) {
            // If we fail to delete a row in metrics_index, we can just log it and keep going because at the end of the
            // process we will drop the table assuming there are no errors updating metrics_cache_index.
            log.info("Failed to delete {bucket: " + bucket.text() + ", scheduleId: " + scheduleId + "} from " +
                INDEX_TABLE + ": " + ThrowableUtil.getRootMessage(t));
            updatesFinished.countDown();
        }
    }

    private Session session;

    private int cacheBlockSize = Integer.parseInt(System.getProperty("rhq.metrics.cache.block-size", "5"));

    private RateLimiter permits = RateLimiter.create(20000);

    private PreparedStatement updateCacheIndex;

    private PreparedStatement findIndexTimeSlice;

    private PreparedStatement findIndexEntries;

    private PreparedStatement deleteIndexEntry;

    private AtomicInteger failedUpdates = new AtomicInteger();

    private ListeningExecutorService tasks;

    private DBConnectionFactory dbConnectionFactory;

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
        // new schema and cluster. In this case, we do not need to do anything since it is not an ugprade scenario.
        if (dbConnectionFactory == null) {
            log.info("The relational database connection factory is not set. Skipping execution.");
            return;
        }

        tasks = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3, new SchemaUpdateThreadFactory()));

        initPreparedStatements();

        Date mostRecent1HourTimeSlice = findMostRecentRawDataSinceLastShutdown();

        try {
            if (mostRecent1HourTimeSlice == null) {
                log.info("The " + CACHE_INDEX_TABLE + " table will not be updated. No raw data was found.");
            } else {
                log.debug("The most recent hour with raw data is " + mostRecent1HourTimeSlice);

                Date mostRecent6HourTimeSlice = get6HourTimeSlice(mostRecent1HourTimeSlice).toDate();
                Date mostRecent24HourTimeSlice = get24HourTimeSlice(mostRecent1HourTimeSlice).toDate();
                Date day = mostRecent24HourTimeSlice;

                updateCacheIndex(fetchRawIndexEntries(mostRecent1HourTimeSlice), Bucket.RAW, day,
                    current1HourTimeSlice().toDate());
                updateCacheIndex(fetch1HourIndexEntries(mostRecent6HourTimeSlice), Bucket.ONE_HOUR, day,
                    mostRecent6HourTimeSlice);
                updateCacheIndex(fetch6HourIndexEntries(mostRecent24HourTimeSlice), Bucket.SIX_HOUR, day,
                    mostRecent24HourTimeSlice);

                if (failedUpdates.get() > 0) {
                    throw new RuntimeException("Cannot complete upgrade step due to previous errors. There were " +
                        failedUpdates.get() + " failed updates.");
                }

                dropIndex();

                deactivateCacheIfNecessary(mostRecent24HourTimeSlice);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("The " + CACHE_INDEX_TABLE + " updates have not completed due to an " +
                "interrupt. The schema upgrade will have to be run again to complete the updates.", e);
        }
    }

    private void initPreparedStatements() {
        findIndexEntries = session.prepare("SELECT schedule_id FROM rhq." + INDEX_TABLE + " WHERE bucket = ? AND time = ?");

        updateCacheIndex = session.prepare(
            "UPDATE rhq." + CACHE_INDEX_TABLE + " " +
                "SET schedule_ids = schedule_ids + ? " +
                "WHERE bucket = ? AND day = ? AND partition = ? AND collection_time_slice = ? AND " +
                "      start_schedule_id = ? AND insert_time_slice = ?");

        findIndexTimeSlice = session.prepare("SELECT time FROM rhq." + INDEX_TABLE + " WHERE bucket = ? AND time = ?");

        deleteIndexEntry = session.prepare(
            "DELETE FROM rhq." + INDEX_TABLE + " WHERE bucket = ? AND time = ? AND schedule_id = ?");
    }

    private void updateCacheIndex(ResultSet resultSet, Bucket bucket, Date day, Date timeSlice)
        throws InterruptedException {

        List<Row> rows = resultSet.all();
        CountDownLatch updatesFinished = new CountDownLatch(rows.size());

        log.info("Preparing to update " + CACHE_INDEX_TABLE + " for " + rows.size() + " schedules from the " +
            bucket.text() + " bucket");

        // We need collectionTimeSlice != insertTimeSlice to make sure that data is pulled
        // from the historical tables during aggregation. The METRICS_CACHE_ACTIVATION_TIME
        // sys config property is set to the start of the next day to indicate that the
        // cache table should not be used until then. There is an edge case though that can
        // occur if the data for which updates are being made does not get aggregated until
        // after METRICS_CACHE_ACTIVATION_TIME has passed. This could happen if the server
        // is shutdown for a while after upgrading. We therefore need to use a different
        // value for the insertTimeSlice column to ensure the data gets pulled from the
        // historical tables during aggregation.
        Date insertTimeSlice = new Date(timeSlice.getTime() + 100);

        for (Row row : rows) {
            permits.acquire();
            int scheduleId = row.getInt(0);
            BoundStatement statement = updateCacheIndex.bind(ImmutableSet.of(scheduleId), bucket.text(), day,
                CACHE_INDEX_PARTITION, timeSlice, startId(scheduleId), insertTimeSlice);
            ResultSetFuture future = session.executeAsync(statement);
            Futures.addCallback(future, new CacheIndexUpdatedCallback(bucket, scheduleId, timeSlice, updatesFinished),
                tasks);
        }

        updatesFinished.await();

        log.info("Finished updating " + CACHE_INDEX_TABLE + " for " + bucket.text() + " bucket");
    }

    private Date findMostRecentRawDataSinceLastShutdown() {
        log.info("Searching for most recent hour having raw data");

        DateTime previousHour = current1HourTimeSlice();
        DateTime oldestRawTime = previousHour.minus(Days.days(7));

        ResultSet resultSet = getIndexTimeSlice(Bucket.ONE_HOUR, previousHour);
        Row row = resultSet.one();
        while (row == null && previousHour.compareTo(oldestRawTime) > 0) {
            previousHour = previousHour.minusHours(1);
            resultSet = getIndexTimeSlice(Bucket.ONE_HOUR, previousHour);
            row = resultSet.one();
        }

        if (row == null) {
            log.info("No data found in metrics_index table");
            return null;
        } else {
            Date date = row.getDate(0);
            log.info("The latest hour with raw data is " + date);
            return date;
        }
    }

    private ResultSet getIndexTimeSlice(Bucket bucket, DateTime time) {
        BoundStatement statement = findIndexTimeSlice.bind(bucket.text(), time.toDate());
        return session.execute(statement);
    }

    private ResultSet fetchRawIndexEntries(Date timeSlice) {
        return queryMetricsIndex(Bucket.ONE_HOUR, timeSlice);
    }

    private ResultSet fetch1HourIndexEntries(Date timeSlice) {
        return queryMetricsIndex(Bucket.SIX_HOUR, timeSlice);
    }

    private ResultSet fetch6HourIndexEntries(Date timeSlice) {
        return queryMetricsIndex(Bucket.TWENTY_FOUR_HOUR, timeSlice);
    }

    private ResultSet queryMetricsIndex(Bucket bucket, Date timeSlice) {
        BoundStatement statement = findIndexEntries.bind(bucket.text(), timeSlice);
        return session.execute(statement);
    }

    private DateTime current1HourTimeSlice() {
        return getTimeSlice(DateTime.now(), Duration.standardHours(1));
    }

    private DateTime get6HourTimeSlice(Date date) {
        return getTimeSlice(new DateTime(date.getTime()), Duration.standardHours(6));
    }

    private DateTime get24HourTimeSlice(Date date) {
        return getTimeSlice(new DateTime(date.getTime()), Duration.standardHours(24));
    }

    private DateTime getTimeSlice(DateTime dt, Duration duration) {
        Period p = duration.toPeriod();

        if (p.getYears() != 0) {
            return dt.yearOfEra().roundFloorCopy().minusYears(dt.getYearOfEra() % p.getYears());
        } else if (p.getMonths() != 0) {
            return dt.monthOfYear().roundFloorCopy().minusMonths((dt.getMonthOfYear() - 1) % p.getMonths());
        } else if (p.getWeeks() != 0) {
            return dt.weekOfWeekyear().roundFloorCopy().minusWeeks((dt.getWeekOfWeekyear() - 1) % p.getWeeks());
        } else if (p.getDays() != 0) {
            return dt.dayOfMonth().roundFloorCopy().minusDays((dt.getDayOfMonth() - 1) % p.getDays());
        } else if (p.getHours() != 0) {
            return dt.hourOfDay().roundFloorCopy().minusHours(dt.getHourOfDay() % p.getHours());
        } else if (p.getMinutes() != 0) {
            return dt.minuteOfHour().roundFloorCopy().minusMinutes(dt.getMinuteOfHour() % p.getMinutes());
        } else if (p.getSeconds() != 0) {
            return dt.secondOfMinute().roundFloorCopy().minusSeconds(dt.getSecondOfMinute() % p.getSeconds());
        }
        return dt.millisOfSecond().roundCeilingCopy().minusMillis(dt.getMillisOfSecond() % p.getMillis());
    }

    private int startId(int scheduleId) {
        return (scheduleId / cacheBlockSize) * cacheBlockSize;
    }

    private void deactivateCacheIfNecessary(Date mostRecent24HourTimeSlice) {
        Connection connection = null;
        Statement statement = null;
        try {
            DateTime current24HourTimeSlice = get24HourTimeSlice(new Date());
            if (current24HourTimeSlice.isAfter(mostRecent24HourTimeSlice.getTime())) {
                log.info("The metrics cache will not be deactivated since the most recent raw data is from before today - "
                    + mostRecent24HourTimeSlice);
            } else {
                DateTime next24HourTimeSlice = current24HourTimeSlice.plusDays(1);
                log.info("The metrics cache will be come active at " + next24HourTimeSlice);

                connection =  dbConnectionFactory.newConnection();
                statement = connection.createStatement();
                statement.executeUpdate("UPDATE rhq_system_config SET property_value = '" +
                    next24HourTimeSlice.getMillis() + "' WHERE property_key = 'METRICS_CACHE_ACTIVATION_TIME'");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate metrics cache", e);
        } finally {
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

    private void dropIndex() {
        session.execute("DROP TABLE rhq.metrics_index");
    }

}
