package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.DateTime;

import org.rhq.server.metrics.domain.Bucket;
import org.rhq.server.metrics.domain.IndexEntry;

/**
 * @author John Sanda
 */
public class Batch implements Iterable<IndexEntry> {

    private DateTime startTime;

    private DateTime endTime;

    private List<IndexEntry> indexEntries = new ArrayList<IndexEntry>();

    private Bucket targetBucket;

    private ListenableFuture<List<ResultSet>> queriesFuture;

    public DateTime getStartTime() {
        return startTime;
    }

    public Batch setStartTime(long timestamp) {
        startTime = new DateTime(timestamp);
        return this;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public Batch setEndTime(DateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public List<IndexEntry> getIndexEntries() {
        return indexEntries;
    }

    public Batch add(IndexEntry indexEntry) {
        indexEntries.add(indexEntry);
        return this;
    }

    public int size() {
        return indexEntries.size();
    }

    public ListenableFuture<List<ResultSet>> getQueriesFuture() {
        return queriesFuture;
    }

    public Batch setQueriesFuture(ListenableFuture<List<ResultSet>> queriesFuture) {
        this.queriesFuture = queriesFuture;
        return this;
    }

    @Override
    public Iterator<IndexEntry> iterator() {
        return indexEntries.iterator();
    }
}
