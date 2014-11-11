package org.rhq.cassandra.schema;

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
public class ReplaceRHQ411Index {

    private static final Log log = LogFactory.getLog(ReplaceRHQ411Index.class);

    private static final int NUM_PARTITIONS = 10;

    private static final int PAGE_SIZE = Integer.parseInt(System.getProperty("rhq.metrics.index.page-size", "2500"));

    private Session session;

    private PreparedStatement find411IndexEntries;

    private PreparedStatement find411IndexEntriesAfterScheduleId;

    private PreparedStatement updateNewIndex;

    public ReplaceRHQ411Index(Session session) {
        this.session = session;
    }

    public void execute(DateRanges dateRanges) {
        initPreparedStatements();

        updateRawIndex(dateRanges.rawStartTime, dateRanges.rawEndTime);
        update1HourIndex(dateRanges.oneHourStartTime, dateRanges.oneHourEndTime);
        update6HourIndex(dateRanges.sixHourStartTime, dateRanges.sixHourEndTime);
        drop411Index();
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
        DateTime time = start;
        BoundStatement statement = find411IndexEntries.bind(oldBucket, start.toDate());
        ResultSet resultSet = session.execute(statement);
        int count = 0;
        int scheduleId = 0;
        int partition = 0;

        do {
            for (Row row : resultSet) {
                scheduleId = row.getInt(0);
                partition = (scheduleId % NUM_PARTITIONS);
                ++count;
                session.execute(updateNewIndex.bind(newBucket, partition, ReplaceIndex.getUTCTimeSlice(time, timeSlice)
                    .toDate(), scheduleId));
            }
            if (count < PAGE_SIZE) {
                time = ReplaceIndex.plusDSTAware(time, timeSlice);
                statement = find411IndexEntries.bind(oldBucket, time.toDate());
            } else {
                statement = find411IndexEntriesAfterScheduleId.bind(oldBucket, time.toDate(), scheduleId);
            }
            count = 0;
            resultSet = session.execute(statement);
        } while (!time.isAfter(end));
    }

    private void drop411Index() {
        log.info("Dropping table metrics_index");
        session.execute("DROP table rhq.metrics_index");
    }

}
