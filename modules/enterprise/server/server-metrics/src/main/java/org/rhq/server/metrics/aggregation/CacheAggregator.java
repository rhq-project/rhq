package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.IndexEntry;
import org.rhq.server.metrics.domain.RawNumericMetric;
import org.rhq.server.metrics.domain.ResultSetMapper;

/**
 * @author John Sanda
 */
class CacheAggregator extends BaseAggregator {

    private static final Log LOG = LogFactory.getLog(CacheAggregator.class);

    static interface BatchFinishedListener {
        void onFinish(List<AggregateNumericMetric> metrics);
    }

    private ResultSetMapper resultSetMapper;

    public void setBatchFinishedListener(BatchFinishedListener batchFinishedListener) {
        this.batchFinishedListener = batchFinishedListener;
    }

    void setResultSetMapper(ResultSetMapper resultSetMapper) {
        this.resultSetMapper = resultSetMapper;
    }

    @Override
    protected String getDebugType() {
        return aggregationType.toString();
    }

    @Override
    protected List<IndexEntry> loadIndexEntries() {
        ResultSet resultSet = dao.findIndexEntries(aggregationType.getCacheTable(), 0, startTime.getMillis()).get();
        List<IndexEntry> indexEntries = new ArrayList<IndexEntry>();

        for (Row row : resultSet) {
            indexEntries.add(new IndexEntry(aggregationType.getCacheTable(), 0, startTime.getMillis(), row.getInt(0)));
        }

        return indexEntries;
    }

    @Override
    protected AggregationTask createAggregationTask(List<IndexEntry> batch) {
        return new AggregationTask(batch) {
            @Override
            void run(List<IndexEntry> batch) {
                switch (aggregationType) {
                    case RAW:
                        processRawBatch(batch);
                        break;
                    case ONE_HOUR:
                        process1HourBatch(batch);
                        break;
                    default:
                        process6HourBatch(batch);
                }
            }
        };
    }

    private void processRawBatch(List<IndexEntry> batch) {
        List<StorageResultSetFuture> queryFutures = new ArrayList(batch.size());
        long endTime = new DateTime(startTime).plusHours(1).getMillis();
        for (IndexEntry indexEntry : batch) {
            queryFutures.add(dao.findRawMetricsAsync(indexEntry.getScheduleId(), indexEntry.getTimestamp(), endTime));
        }
        processBatch(queryFutures, batch, startTime, Bucket.ONE_HOUR);
    }

    private void process1HourBatch(List<IndexEntry> batch) {
        List<StorageResultSetFuture> queryFutures = new ArrayList(batch.size());
        DateTime endTime = dateTimeService.get6HourTimeSliceEnd(startTime);
        for (IndexEntry indexEntry : batch) {
            queryFutures.add(dao.findAggregateMetricsAsync(indexEntry.getScheduleId(), Bucket.ONE_HOUR,
                startTime.getMillis(), endTime.getMillis()));
        }
        processBatch(queryFutures, batch, startTime, Bucket.SIX_HOUR);
    }

    private void process6HourBatch(List<IndexEntry> batch) {
        List<StorageResultSetFuture> queryFutures = new ArrayList(batch.size());
        DateTime endTime = dateTimeService.get24HourTimeSliceEnd(startTime);
        for (IndexEntry indexEntry : batch) {
            queryFutures.add(dao.findAggregateMetricsAsync(indexEntry.getScheduleId(), Bucket.SIX_HOUR,
                startTime.getMillis(), endTime.getMillis()));
        }
        processBatch(queryFutures, batch, startTime, Bucket.TWENTY_FOUR_HOUR);
    }

    @Override
    protected Map<AggregationType, Integer> getAggregationCounts() {
        return ImmutableMap.of(aggregationType, schedulesCount.get());
    }

    /*
     * <p>
     * This method provides the core aggregation logic. It performs the following steps:
     *
     * <ul>
     *  <li>Iterate over a cache result set (which may contain data for multiple schedules)</li>
     *  <li>Compute aggregate metrics</li>
     *  <li>Persist the aggregate metrics</li>
     *  <li>Delete the cache partition</li>
     *  <li>Delete the cache index row</li>
     * </ul>
     * </p>
     * <p>
     * Be aware that this method is completely asynchronous. Each of the preceding steps correspond to function calls
     * that return a ListenableFuture. While this method is asynchronous, the steps will execute in the order listed.
     * </p>
     * <p>
     * It is also important to note that if one of the function calls fails, then the functions for the steps that
     * follow are <strong>not</strong> executed. This is by design so that the task can be retried during a subsequent
     * aggregation run.
     * </p>
     *
     * @param indexEntry The index entry for which data is being aggregated
     * @param cacheFuture A future of the cache query result set
     * @param persistMetricsFn The function that will be used to persist the aggregate metrics
     */


    private void processBatch(List<StorageResultSetFuture> queryFutures, List<IndexEntry> indexEntries,
        DateTime timeSlice, Bucket bucket) {
        ListenableFuture<List<ResultSet>> queriesFuture = Futures.allAsList(queryFutures);

        ListenableFuture<Iterable<List<RawNumericMetric>>> iterableFuture = Futures.transform(queriesFuture,
            toIterable(resultSetMapper), aggregationTasks);

        ListenableFuture<List<AggregateNumericMetric>> metricsFuture = Futures.transform(iterableFuture,
            computeAggregates(timeSlice.getMillis(), RawNumericMetric.class, bucket), aggregationTasks);

        ListenableFuture<List<ResultSet>> insertsFuture = Futures.transform(metricsFuture, persistMetrics,
            aggregationTasks);

        ListenableFuture<List<ResultSet>> deleteIndexEntriesFuture = Futures.transform(insertsFuture,
            deleteIndexEntries(indexEntries), aggregationTasks);

        aggregationTaskFinished(metricsFuture, deleteIndexEntriesFuture);
    }

}
