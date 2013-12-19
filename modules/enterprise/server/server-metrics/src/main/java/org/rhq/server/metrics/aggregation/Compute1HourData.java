package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * Computes 1 hour data for a batch of raw data result sets. The generated 1 hour aggregates are inserted along with
 * their corresponding index updates.
 *
 * @author John Sanda
 */
class Compute1HourData implements AsyncFunction<List<ResultSet>, List<ResultSet>> {

    private final Log log = LogFactory.getLog(Compute1HourData.class);

    private DateTime startTime;

    private RateLimiter writePermits;

    private MetricsDAO dao;

    private DateTime sixHourTimeSlice;

    private Set<AggregateNumericMetric> oneHourData;

    public Compute1HourData(DateTime startTime, DateTime sixHourTimeSlice, RateLimiter writePermits, MetricsDAO dao,
        Set<AggregateNumericMetric> oneHourData) {
        this.startTime = startTime;
        this.sixHourTimeSlice = sixHourTimeSlice;
        this.writePermits = writePermits;
        this.dao = dao;
        this.oneHourData = oneHourData;
    }

    @Override
    public ListenableFuture<List<ResultSet>> apply(List<ResultSet> rawDataResultSets) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Computing and storing 1 hour data for " + rawDataResultSets.size() + " schedules");
        }
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            List<StorageResultSetFuture> insertFutures = new ArrayList<StorageResultSetFuture>(rawDataResultSets.size());
            for (ResultSet resultSet : rawDataResultSets) {
                AggregateNumericMetric aggregate = calculateAggregatedRaw(resultSet);
                oneHourData.add(aggregate);
                writePermits.acquire(4);
                insertFutures.add(dao.insertOneHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MIN, aggregate.getMin()));
                insertFutures.add(dao.insertOneHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MAX, aggregate.getMax()));
                insertFutures.add(dao.insertOneHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.AVG, aggregate.getAvg()));
                insertFutures.add(dao.updateMetricsIndex(MetricsTable.SIX_HOUR, aggregate.getScheduleId(),
                    sixHourTimeSlice.getMillis()));
            }
            return Futures.successfulAsList(insertFutures);
        } finally {
            if (log.isDebugEnabled()) {
                stopwatch.stop();
                log.debug("Finished computing and storing 1 hour data for " + rawDataResultSets.size() +
                    " schedules in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

    private AggregateNumericMetric calculateAggregatedRaw(ResultSet resultSet) {
        double min = Double.NaN;
        double max = min;
        int count = 0;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        double value;
        List<Row> rows = resultSet.all();

        for (Row row : rows) {
            value = row.getDouble(2);
            if (count == 0) {
                min = value;
                max = min;
            }
            if (value < min) {
                min = value;
            } else if (value > max) {
                max = value;
            }
            mean.add(value);
            ++count;
        }

        return new AggregateNumericMetric(rows.get(0).getInt(0), mean.getArithmeticMean(), min, max,
            startTime.getMillis());
    }
}
