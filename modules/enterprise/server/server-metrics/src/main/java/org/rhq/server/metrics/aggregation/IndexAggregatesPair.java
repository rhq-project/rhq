package org.rhq.server.metrics.aggregation;

import java.util.List;

import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.CacheIndexEntry;

/**
 * @author John Sanda
 */
class IndexAggregatesPair {

    public final CacheIndexEntry cacheIndexEntry;

    public final List<AggregateNumericMetric> metrics;

    public IndexAggregatesPair(CacheIndexEntry cacheIndexEntry, List<AggregateNumericMetric> metrics) {
        this.cacheIndexEntry = cacheIndexEntry;
        this.metrics = metrics;
    }

}
