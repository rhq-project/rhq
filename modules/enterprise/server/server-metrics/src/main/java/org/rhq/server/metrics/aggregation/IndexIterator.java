package org.rhq.server.metrics.aggregation;

import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.collect.Iterators;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import org.rhq.server.metrics.MetricsConfiguration;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.domain.IndexBucket;
import org.rhq.server.metrics.domain.IndexEntry;

/**
 * An iterator for the index with paging support. The iterator will scan through all partitions for a particular date
 * range, one page at a time.
 *
 * @author John Sanda
 */
public class IndexIterator implements Iterator<IndexEntry> {

    private DateTime time;

    private Duration duration;

    private DateTime endTime;

    private IndexBucket bucket;

    private MetricsDAO dao;

    private Iterator<Row> rowIterator;

    private int numPartitions;

    private int partition;

    private int pageSize;

    private int lastScheduleId;

    private int rowCount;

    /**
     *
     * @param startTime The start time inclusive of the date range to query
     * @param endTime The end time exlusive of the date range to query
     * @param bucket Either raw, 1 hour, or 6 hour
     * @param dao Used for querying the index
     * @param configuration The metrics configuration which provides the total number of partitions and page size
     */
    public IndexIterator(DateTime startTime, DateTime endTime, IndexBucket bucket, MetricsDAO dao,
        MetricsConfiguration configuration) {
        time = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.bucket = bucket;
        this.dao = dao;
        this.numPartitions = configuration.getIndexPartitions();
        this.pageSize = configuration.getIndexPageSize();
        switch (bucket) {
            case RAW:
                this.duration = configuration.getRawTimeSliceDuration();
                break;
            case ONE_HOUR:
                this.duration = configuration.getOneHourTimeSliceDuration();
                break;
            default:
                this.duration = configuration.getSixHourTimeSliceDuration();
        }

       loadPage();
    }

    @Override
    public boolean hasNext() {
        return rowIterator.hasNext();
    }

    @Override
    public IndexEntry next() {
        Row row = rowIterator.next();
        lastScheduleId = row.getInt(0);
        IndexEntry indexEntry = new IndexEntry(bucket, partition, time.getMillis(), lastScheduleId);
        if (!rowIterator.hasNext()) {
            loadPage();
        }
        return indexEntry;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void loadPage() {
        if (rowIterator == null) {
            nextPage(dao.findIndexEntries(bucket, partition, time.getMillis()).get());
        } else {
            if (rowCount < pageSize) {
                // When we get here, it means that we have gone through all the pages in
                // the current partition; consequently, we query the next partition.
                nextPage(dao.findIndexEntries(bucket, ++partition, time.getMillis()).get());
            } else{
                // We query the current partition again because there could be more pages.
                nextPage(dao.findIndexEntries(bucket, partition, time.getMillis(), lastScheduleId).get());
            }
        }
    }

    /**
     * This method moves to the next page of data if one exists. If the result set is empty, we query the next
     * partition. If we have queried the last partition, then we wrap around to the first partition of the next time
     * slice. We continue searching for a non-empty result set until  we hit endTime.
     */
    private void nextPage(ResultSet resultSet) {
        ResultSet nextResultSet = resultSet;
        while (nextResultSet.isExhausted() && time.isBefore(endTime)) {
            if (partition < numPartitions - 1) {
                ++partition;
            } else {
                partition = 0;
                time = time.plus(duration);
            }
            nextResultSet = dao.findIndexEntries(bucket, partition, time.getMillis()).get();
        }
        if (time.isBefore(endTime)) {
            List<Row> rows = nextResultSet.all();
            rowCount = rows.size();
            rowIterator = rows.iterator();
        } else {
            rowCount = 0;
            rowIterator = Iterators.emptyIterator();
        }
    }

}
