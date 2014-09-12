package org.rhq.cassandra.schema;

/**
 * @author John Sanda
 */
public enum Table {

    METRICS_INDEX("rhq.metrics_idx"),
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

    @Override
    public String toString() {
        return tableName;
    }
}
