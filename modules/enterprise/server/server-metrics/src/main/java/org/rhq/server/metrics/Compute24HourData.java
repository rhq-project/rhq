package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;

/**
 * @author John Sanda
 */
public class Compute24HourData implements AsyncFunction<List<ResultSet>, List<ResultSet>> {

    private final Log log = LogFactory.getLog(Compute24HourData.class);

    private DateTime startTime;

    private RateLimiter writePermits;

    private MetricsDAO dao;

    public Compute24HourData(DateTime startTime, RateLimiter writePermits, MetricsDAO dao) {
        this.startTime = startTime;
        this.writePermits = writePermits;
        this.dao = dao;
    }

    @Override
    public ListenableFuture<List<ResultSet>> apply(List<ResultSet> sixHourDataResultSets) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Computing and storing 24 hour data for " + (sixHourDataResultSets.size() / 3) + " values");
        }
        long start = System.currentTimeMillis();
        try {
            List<StorageResultSetFuture> insertFutures =
                new ArrayList<StorageResultSetFuture>(sixHourDataResultSets.size());
            for (ResultSet resultSet : sixHourDataResultSets) {
                AggregateNumericMetric aggregate = calculateAggregate(resultSet);
                writePermits.acquire(3);
                insertFutures.add(dao.insertTwentyFourHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MIN, aggregate.getMin()));
                insertFutures.add(dao.insertTwentyFourHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MAX, aggregate.getMax()));
                insertFutures.add(dao.insertTwentyFourHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.AVG, aggregate.getAvg()));
            }
            return Futures.successfulAsList(insertFutures);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Finished computing and storing 24 hour data for " + (sixHourDataResultSets.size() / 3) +
                    " values in " + (System.currentTimeMillis() - start) + " ms");
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
