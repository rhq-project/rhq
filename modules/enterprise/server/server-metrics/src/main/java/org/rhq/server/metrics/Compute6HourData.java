package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class Compute6HourData implements AsyncFunction<List<ResultSet>, List<ResultSet>> {

    private final Log log = LogFactory.getLog(Compute6HourData.class);

    private DateTime startTime;

    private RateLimiter writePermits;

    private MetricsDAO dao;

    private DateTime twentyFourHourTimeSlice;

    public Compute6HourData(DateTime startTime, DateTime twentyFourHourTimeSlice, RateLimiter writePermits,
        MetricsDAO dao) {
        this.startTime = startTime;
        this.twentyFourHourTimeSlice = twentyFourHourTimeSlice;
        this.writePermits = writePermits;
        this.dao = dao;
    }

    @Override
    public ListenableFuture<List<ResultSet>> apply(List<ResultSet> oneHourDataResultSets) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Computing and storing 6 hour data for " + oneHourDataResultSets.size() + " schedules");
        }
        long start = System.currentTimeMillis();
        try {
            List<StorageResultSetFuture> insertFutures =
                new ArrayList<StorageResultSetFuture>(oneHourDataResultSets.size());
            for (ResultSet resultSet : oneHourDataResultSets) {
                AggregateNumericMetric aggregate = calculateAggregate(resultSet);
                writePermits.acquire(4);
                insertFutures.add(dao.insertSixHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MIN, aggregate.getMin()));
                insertFutures.add(dao.insertSixHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MAX, aggregate.getMax()));
                insertFutures.add(dao.insertSixHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.AVG, aggregate.getAvg()));
                insertFutures.add(dao.updateMetricsIndex(MetricsTable.TWENTY_FOUR_HOUR, aggregate.getScheduleId(),
                    twentyFourHourTimeSlice.getMillis()));
            }
            ListenableFuture<List<ResultSet>> resultSetFutures = Futures.allAsList(insertFutures);
            return Futures.withFallback(resultSetFutures, new FutureFallback<List<ResultSet>>() {
                @Override
                public ListenableFuture<List<ResultSet>> create(Throwable t) throws Exception {
                    log.error("An error occurred while inserting six hour data", t);
                    return Futures.immediateFailedFuture(t);
                }
            });
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Finished computing and storing 6 hour data for " + oneHourDataResultSets.size() +
                    " schedules in " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }

    private AggregateNumericMetric calculateAggregate(ResultSet resultSet) {
        double min = Double.NaN;
        double max = min;
        ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
        List<Row> rows = resultSet.all();

        for (int i = 0; i < rows.size(); i += 3) {
            if (i == 0) {
                min = rows.get(i + 1).getDouble(3);
                max = rows.get(i).getDouble(3);
            } else {
                if (rows.get(i + 1).getDouble(3) < min) {
                    min = rows.get(i + 1).getDouble(3);
                }
                if (rows.get(i).getDouble(3) > max) {
                    max = rows.get(i).getDouble(3);
                }
            }
            mean.add(rows.get(i + 2).getDouble(3));
        }
        return new AggregateNumericMetric(rows.get(0).getInt(0), mean.getArithmeticMean(), min, max,
            startTime.getMillis());
    }
}
