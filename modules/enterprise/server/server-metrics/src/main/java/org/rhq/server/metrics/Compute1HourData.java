package org.rhq.server.metrics;

import static org.rhq.server.metrics.MetricsUtil.indexPartitionKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public class Compute1HourData implements AsyncFunction<List<ResultSet>, List<ResultSet>> {

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
            log.debug("Computing and storing 1 hour data for " + rawDataResultSets.size() + " values");
        }
        long start = System.currentTimeMillis();
        try {
            List<StorageResultSetFuture> insertFutures = new ArrayList<StorageResultSetFuture>(rawDataResultSets.size());
            for (ResultSet resultSet : rawDataResultSets) {
                AggregateNumericMetric aggregate = calculateAggregatedRaw(resultSet);
                oneHourData.add(aggregate);
                String partitionKey = indexPartitionKey(MetricsTable.SIX_HOUR, aggregate.getScheduleId());
                writePermits.acquire(4);
                insertFutures.add(dao.insertOneHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MIN, aggregate.getMin()));
                insertFutures.add(dao.insertOneHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.MAX, aggregate.getMax()));
                insertFutures.add(dao.insertOneHourDataAsync(aggregate.getScheduleId(), aggregate.getTimestamp(),
                    AggregateType.AVG, aggregate.getAvg()));
                insertFutures.add(dao.updateMetricsIndex(partitionKey, aggregate.getScheduleId(),
                    sixHourTimeSlice.getMillis()));
            }
            return Futures.successfulAsList(insertFutures);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Finished computing and storing 1 hour data for " + rawDataResultSets.size() +
                    " values in " + (System.currentTimeMillis() - start) + " ms");
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
