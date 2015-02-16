package org.rhq.cassandra.schema;

import java.util.Set;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Hours;

/**
 * @author John Sanda
 */
public class Replace412Index {

    private static final Log log = LogFactory.getLog(Replace412Index.class);

    private static final int NUM_PARTITIONS = 10;

    private static final int PAGE_SIZE = Integer.parseInt(System.getProperty("rhq.metrics.index.page-size", "2500"));

    private Session session;

    private PreparedStatement find412IndexEntries;

    private PreparedStatement find412IndexEntriesAfterScheduleId;

    private PreparedStatement updateNewIndex;

    public Replace412Index(Session session) {
        this.session = session;
    }

    public void execute(DateRanges dateRanges) {
        initPreparedStatements();

        DateTime startDay = dateRanges.sixHourStartTime;
        updateRawIndex(dateRanges.rawStartTime, dateRanges.rawEndTime, startDay);
        update1HourIndex(dateRanges.oneHourStartTime, dateRanges.oneHourEndTime, startDay);
        update6HourIndex(dateRanges.sixHourStartTime, dateRanges.sixHourEndTime, startDay);
        dropTables("rhq.metrics_cache", "rhq.metrics_cache_index");
    }

    private void initPreparedStatements() {
        find412IndexEntries = session.prepare(
            "SELECT day, start_schedule_id, schedule_ids FROM rhq.metrics_cache_index WHERE bucket = ? " +
            "AND partition = 0 AND day = ? AND collection_time_slice = ? LIMIT " + PAGE_SIZE);

        find412IndexEntriesAfterScheduleId = session.prepare(
            "SELECT day, start_schedule_id, schedule_ids FROM rhq.metrics_cache_index WHERE bucket = ? " +
            "AND partition = 0 AND day = ? AND collection_time_slice = ? AND start_schedule_id > ? LIMIT " + PAGE_SIZE);

        updateNewIndex = session.prepare(
            "INSERT INTO rhq.metrics_idx (bucket, partition, time, schedule_id) VALUES (?, ?, ?, ?)");
    }

    private void updateRawIndex(DateTime start, DateTime end, DateTime startDay) {
        log.info("Updating raw index");
        updateIndex("raw_metrics", "raw", startDay, start, end, Hours.ONE.toStandardDuration());
    }

    private void update1HourIndex(DateTime start, DateTime end, DateTime startDay) {
        log.info("Updating one_hour index");
        updateIndex("one_hour_metrics", "one_hour", startDay, start, end, Hours.SIX.toStandardDuration());
    }

    private void update6HourIndex(DateTime start, DateTime end, DateTime startDay) {
        log.info("Updating six_hour index");
        updateIndex("six_hour_metrics", "six_hour", startDay, start, end, Days.ONE.toStandardDuration());
    }

    private void updateIndex(String oldBucket, String newBucket, DateTime startDay, DateTime start, DateTime end,
        Duration timeSlice) {
        DateTime time = start;
        BoundStatement statement = find412IndexEntries.bind(oldBucket, startDay.toDate(), start.toDate());
        ResultSet resultSet = session.execute(statement);
        DateTime day = startDay;
        int startScheduleId = 0;
        int count = 0;
        int partition = 0;

        do {
            for (Row row : resultSet) {
                day = new DateTime(row.getDate(0));
                startScheduleId = row.getInt(1);
                Set<Integer> scheduleIds = row.getSet(2, Integer.class);
                ++count;
                for (Integer scheduleId : scheduleIds) {
                    partition = (scheduleId % NUM_PARTITIONS);
                    session.execute(updateNewIndex.bind(newBucket, partition,
                        DateUtils.getUTCTimeSlice(time, timeSlice).toDate(), scheduleId));
                }
            }
            if (count < PAGE_SIZE) {
                time = DateUtils.plusDSTAware(time, timeSlice);
                DateTime tempDay = DateUtils.plusDSTAware(day, Days.ONE.toStandardDuration());
                if (time.equals(tempDay)) {
                    day = DateUtils.plusDSTAware(day, Days.ONE.toStandardDuration());
                }
                statement = find412IndexEntries.bind(oldBucket, day.toDate(), time.toDate());
            } else {
                statement = find412IndexEntriesAfterScheduleId.bind(oldBucket, day.toDate(), time.toDate(),
                    startScheduleId);
            }
            count = 0;
            resultSet = session.execute(statement);
        } while (!time.isAfter(end));
    }

    private void dropTables(String... tables) {
        for (String table : tables) {
            log.info("Dropping table " + table);
            session.execute("DROP table " + table);
        }
    }


}
