package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.ArithmeticMeanCalculator;
import org.rhq.server.metrics.CacheMapper;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.NumericMetric;

/**
 * @author John Sanda
 */
class ProcessBatch implements AsyncFunction<ResultSet, BatchResult> {

    private final Log log = LogFactory.getLog(ProcessBatch.class);

    private MetricsDAO dao;

    private ComputeMetric computeMetric;

    private int startScheduleId;

    private DateTime timeSlice;

    private AggregationType aggregationType;

    private int batchSize;

    public ProcessBatch(MetricsDAO dao, ComputeMetric computeMetric, int startScheduleId,
        DateTime timeSlice, AggregationType aggregationType, int batchSize) {
        this.dao = dao;
        this.computeMetric = computeMetric;
        this.startScheduleId = startScheduleId;
        this.timeSlice = timeSlice;
        this.aggregationType = aggregationType;
        this.batchSize = batchSize;
    }

    @Override
    public ListenableFuture<BatchResult> apply(ResultSet resultSet) throws Exception {
        Stopwatch stopwatch = new Stopwatch().start();
        if (log.isDebugEnabled()) {
            log.debug("Aggregating batch of " + aggregationType + " with starting schedule id " + startScheduleId);
        }
        List<StorageResultSetFuture> insertFutures = new ArrayList<StorageResultSetFuture>(batchSize * 4);
        try {
            if (resultSet.isExhausted()) {
                return Futures.immediateFuture(new BatchResult(timeSlice, startScheduleId));
            }

            CacheMapper cacheMapper = aggregationType.getCacheMapper();
            NumericMetric currentMetric = cacheMapper.map(resultSet.one());
            Double min = currentMetric.getMin();
            Double max = currentMetric.getMax();
            ArithmeticMeanCalculator mean = new ArithmeticMeanCalculator();
            mean.add(currentMetric.getAvg());

            for (Row row : resultSet) {
                NumericMetric nextMetric = cacheMapper.map(row);
                if (currentMetric.getScheduleId() == nextMetric.getScheduleId()) {
                    currentMetric = nextMetric;
                    if (currentMetric.getMin() < min) {
                        min = currentMetric.getMin();
                    } else if (currentMetric.getMax() > max) {
                        max = currentMetric.getMax();
                    }
                    mean.add(currentMetric.getAvg());
                } else {
                    insertFutures.addAll(computeMetric.execute(startScheduleId, currentMetric.getScheduleId(), min, max,
                        mean));

                    currentMetric = nextMetric;
                    min = currentMetric.getMin();
                    max = currentMetric.getMax();
                    mean = new ArithmeticMeanCalculator();
                    mean.add(currentMetric.getAvg());
                }
            }
            insertFutures.addAll(computeMetric.execute(startScheduleId, currentMetric.getScheduleId(), min, max, mean));
            ListenableFuture<List<ResultSet>> insertsFuture = Futures.successfulAsList(insertFutures);

            return Futures.transform(insertsFuture, new AsyncFunction<List<ResultSet>, BatchResult>() {
                @Override
                public ListenableFuture<BatchResult> apply(final List<ResultSet> resultSets) {
                    StorageResultSetFuture deleteFuture = dao.deleteCacheEntries(
                        aggregationType.getCacheTable(), timeSlice.getMillis(), startScheduleId);
                    return Futures.transform(deleteFuture, new Function<ResultSet, BatchResult>() {
                        @Override
                        public BatchResult apply(ResultSet deleteFuture) {
                            return new BatchResult(resultSets, timeSlice, startScheduleId, deleteFuture);
                        }
                    });
                }
            });
        } finally {
            stopwatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Finished aggregating batch of " + aggregationType + " for " + (insertFutures.size() / 4) +
                    " schedules with starting schedule id " + startScheduleId + " in " +
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }

}
