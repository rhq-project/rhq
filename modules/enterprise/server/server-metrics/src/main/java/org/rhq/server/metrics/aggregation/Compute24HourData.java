package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;

/**
 * Computes 24 hour data for a batch of raw data result sets. The generated 6 hour aggregates are inserted.
 *
 * @author John Sanda
 */
class Compute24HourData implements AsyncFunction<List<ResultSet>, List<ResultSet>> {

    private final Log log = LogFactory.getLog(Compute24HourData.class);

    private DateTime startTime;

    private MetricsDAO dao;

    public Compute24HourData(DateTime startTime, MetricsDAO dao) {
        this.startTime = startTime;
        this.dao = dao;
    }

    @Override
    public ListenableFuture<List<ResultSet>> apply(List<ResultSet> sixHourDataResultSets) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Computing and storing 24 hour data for " + sixHourDataResultSets.size() + " schedules");
        }
        Stopwatch stopwatch = new Stopwatch().start();
        try {
            List<StorageResultSetFuture> insertFutures =
                new ArrayList<StorageResultSetFuture>(sixHourDataResultSets.size());
            for (ResultSet resultSet : sixHourDataResultSets) {
                if (resultSet == null) {
                    // resultSet could be null if the 6 hr data query failed for whatever reason. We currently lack
                    // a way of correlating the failed query back to a schedule id. It could be useful to log the
                    // schedule id, possibly for debugging purposes.
                    continue;
                }
                AggregateNumericMetric aggregate = calculateAggregate(resultSet);
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
                stopwatch.stop();
                log.debug("Finished computing and storing 24 hour data for " + sixHourDataResultSets.size() +
                    " schedules in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
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
