package org.rhq.cassandra.schema;

/**
 * @author John Sanda
 */
public enum Table {

    METRICS_CACHE("rhq.metrics_cache"),
    METRICS_CACHE_INDEX("rhq.metrics_cache_index"),
    RAW_METRICS("rhq.raw_metrics"),
    AGGREGATE_METRICS("rhq.aggregate_metrics"),
    SCHEMA_VERSION("rhq.schema_version");


    private String tableName;

    private Table(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

}
