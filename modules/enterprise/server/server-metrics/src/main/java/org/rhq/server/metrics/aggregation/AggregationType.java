package org.rhq.server.metrics.aggregation;

import org.rhq.server.metrics.domain.MetricsTable;

/**
 * @author John Sanda
 */
public enum AggregationType {

    RAW("raw data", MetricsTable.RAW),

    ONE_HOUR("one hour data", MetricsTable.ONE_HOUR),

    SIX_HOUR("six hour data", MetricsTable.SIX_HOUR);

    private String type;

    private MetricsTable cacheTable;

    private AggregationType(String type, MetricsTable cacheTable) {
        this.type = type;
        this.cacheTable = cacheTable;
    }

    public MetricsTable getCacheTable() {
        return cacheTable;
    }

    @Override
    public String toString() {
        return type;
    }
}
