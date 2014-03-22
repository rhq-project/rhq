package org.rhq.server.metrics.aggregation;

import java.util.Collections;
import java.util.List;

import com.datastax.driver.core.ResultSet;

import org.rhq.server.metrics.domain.CacheIndexEntry;

/**
 * @author John Sanda
 */
class BatchResult {

    private CacheIndexEntry cacheIndexEntry;

    private List<ResultSet> insertResultSets;

    private ResultSet purgeCacheResultSet;

    public BatchResult(List<ResultSet> insertResultSets, CacheIndexEntry cacheIndexEntry,
        ResultSet purgeCacheResultSet) {
        this.insertResultSets = insertResultSets;
        this.cacheIndexEntry = cacheIndexEntry;
        this.purgeCacheResultSet = purgeCacheResultSet;
    }

    public BatchResult(CacheIndexEntry cacheIndexEntry) {
        this.cacheIndexEntry = cacheIndexEntry;
        insertResultSets = Collections.emptyList();
    }

    boolean isEmpty() {
        return insertResultSets.isEmpty();
    }

    public List<ResultSet> getInsertResultSets() {
        return insertResultSets;
    }

    CacheIndexEntry getCacheIndexEntry() {
        return cacheIndexEntry;
    }

    public ResultSet getPurgeCacheResultSet() {
        return purgeCacheResultSet;
    }
}
