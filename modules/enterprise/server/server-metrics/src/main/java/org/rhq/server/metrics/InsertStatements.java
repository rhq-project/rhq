package org.rhq.server.metrics;

import java.util.concurrent.ConcurrentSkipListMap;

import com.datastax.driver.core.PreparedStatement;

import org.joda.time.DateTime;

import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * <p>
 * A cache of prepared statements for inserting metric data. We want to use prepared statements to insert metric data
 * to avoid the query parsing overhead that would otherwise be incurred on each write request. In Cassandra 1.2.x,
 * neither the TTL nor the column timestamp can be specified as bind parameters in a prepared statement. In RHQ 4.12
 * we added support for storing and aggregating data from late measurement reports. The TTLs for late data could be
 * wrong which in turn could throw of aggregate metrics when they got recomputed.
 * </p>
 * <p>
 * This class ensures that data is expired consistently, regardless of whether or not it is coming in a late measurement
 * report. Column timestamps are rounded down to the start of the day on which the metric was collected. This means that
 * a metric collected at 11:00 AM will expire at the same time as one collected at 11:00 PM on the same day.
 * </p>
 *
 * @author John Sanda
 */
public class InsertStatements {

    private StorageSession session;

    private DateTimeService dateTimeService;

    private MetricsConfiguration configuration;

    public ConcurrentSkipListMap<DateTime, PreparedStatement> raw =
        new ConcurrentSkipListMap<DateTime, PreparedStatement>();

    public ConcurrentSkipListMap<DateTime, PreparedStatement> oneHour =
        new ConcurrentSkipListMap<DateTime, PreparedStatement>();

    public ConcurrentSkipListMap<DateTime, PreparedStatement> sixHour =
        new ConcurrentSkipListMap<DateTime, PreparedStatement>();

    public ConcurrentSkipListMap<DateTime, PreparedStatement> twentyFourHour =
        new ConcurrentSkipListMap<DateTime, PreparedStatement>();

    public InsertStatements(StorageSession session, DateTimeService dateTimeService,
        MetricsConfiguration configuration) {
        this.session = session;
        this.dateTimeService = dateTimeService;
        this.configuration = configuration;
    }

    /**
     * Initializes the prepared statement caches for raw, 1 hr, 6 hr, and 24 hr data. A separate statement is
     * maintained in each map for each day for the past 7 days since that is the raw retention period. We also a
     * statement for "tomorrow" so that we already have prepared statements ready when inserting data right at
     * midnight.
     */
    public void init() {
        DateTime today = dateTimeService.current24HourTimeSlice();
        DateTime endDay = today.plusDays(2);
        DateTime day = today.minusDays(7);

        while (day.isBefore(endDay)) {
            raw.put(day, session.prepare(
                "INSERT INTO " + MetricsTable.RAW + " (schedule_id, time, value) VALUES (?, ?, ?) " +
                "USING TTL " + configuration.getRawTTL() + " AND TIMESTAMP " + toMicroSeconds(day)
            ));
            oneHour.put(day, session.prepare(
                "INSERT INTO " + MetricsTable.AGGREGATE + " (schedule_id, bucket, time, avg, max, min) VALUES (" +
                "?, '" + Bucket.ONE_HOUR + "', ?, ?, ?, ?) USING TTL " + configuration.getOneHourTTL() + " AND " +
                "TIMESTAMP " + toMicroSeconds(day)
            ));
            sixHour.put(day, session.prepare(
                "INSERT INTO " + MetricsTable.AGGREGATE + " (schedule_id, bucket, time, avg, max, min) VALUES (" +
                    "?, '" + Bucket.SIX_HOUR + "', ?, ?, ?, ?) USING TTL " + configuration.getSixHourTTL() + " AND " +
                    "TIMESTAMP " + toMicroSeconds(day)
            ));
            twentyFourHour.put(day, session.prepare(
                "INSERT INTO " + MetricsTable.AGGREGATE + " (schedule_id, bucket, time, avg, max, min) VALUES (" +
                    "?, '" + Bucket.TWENTY_FOUR_HOUR + "', ?, ?, ?, ?) USING TTL " +
                    configuration.getTwentyFourHourTTL() + " AND TIMESTAMP " + toMicroSeconds(day)
            ));
            day = day.plusDays(1);
        }
    }

    /**
     * It is expected that this method is invoked by a reoccurring job that runs daily. The oldest statement, which
     * would be from 8 days ago, is removed from each map. And then a new statement is added for tomorrow.
     */
    public void update() {
        DateTime oldestDay = raw.firstKey();
        DateTime day = raw.lastKey();
        DateTime tomorrow = day.plusDays(1);

        raw.put(tomorrow, session.prepare(
            "INSERT INTO " + MetricsTable.RAW + " (schedule_id, time, value) VALUES (?, ?, ?) " +
            "USING TTL " + configuration.getRawTTL() + " AND TIMESTAMP " + toMicroSeconds(tomorrow)
        ));
        raw.remove(oldestDay);

        oneHour.put(tomorrow, session.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + " (schedule_id, bucket, time, avg, max, min) VALUES (" +
            "?, '" + Bucket.ONE_HOUR + "', ?, ?, ?, ?) USING TTL " + configuration.getOneHourTTL() + " AND " +
            "TIMESTAMP " + toMicroSeconds(tomorrow)
        ));
        oneHour.remove(oldestDay);

        sixHour.put(tomorrow, session.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + " (schedule_id, bucket, time, avg, max, min) VALUES (" +
            "?, '" + Bucket.SIX_HOUR + "', ?, ?, ?, ?) USING TTL " + configuration.getSixHourTTL() + " AND " +
            "TIMESTAMP " + toMicroSeconds(tomorrow)
        ));
        sixHour.remove(oldestDay);

        twentyFourHour.put(tomorrow, session.prepare(
            "INSERT INTO " + MetricsTable.AGGREGATE + " (schedule_id, bucket, time, avg, max, min) VALUES (" +
            "?, '" + Bucket.TWENTY_FOUR_HOUR + "', ?, ?, ?, ?) USING TTL " + configuration.getTwentyFourHourTTL() +
            " AND TIMESTAMP " + toMicroSeconds(tomorrow)
        ));
        twentyFourHour.remove(oldestDay);
    }

    private long toMicroSeconds(DateTime date) {
        return date.getMillis() * 1000;
    }

}
