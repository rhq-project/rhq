package org.rhq.server.metrics.aggregation;

import org.rhq.server.metrics.AggregateCacheMapper;
import org.rhq.server.metrics.CacheMapper;
import org.rhq.server.metrics.RawCacheMapper;
import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public enum AggregationType {

    RAW("raw data", MetricsTable.ONE_HOUR, new RawCacheMapper()),

    ONE_HOUR("one hour data", MetricsTable.SIX_HOUR, new AggregateCacheMapper()),

    SIX_HOUR("six hour data", MetricsTable.TWENTY_FOUR_HOUR, new AggregateCacheMapper());

    private String type;

    private MetricsTable cacheTable;

    private CacheMapper cacheMapper;

    private AggregationType(String type, MetricsTable cacheTable, CacheMapper cacheMapper) {
        this.type = type;
        this.cacheTable = cacheTable;
        this.cacheMapper = cacheMapper;
    }

    public MetricsTable getCacheTable() {
        return cacheTable;
    }

    public CacheMapper getCacheMapper() {
        return cacheMapper;
    }

    @Override
    public String toString() {
        return type;
    }
}
