package org.rhq.server.metrics;

public enum MetricsTable {

    INDEX("metrics_index"),
    RAW("raw_metrics"),
    ONE_HOUR("one_hour_metrics"),
    SIX_HOUR("six_hour_metrics"),
    TWENTY_FOUR_HOUR("twenty_four_hour_metrics");

    private final String tableName;

    private MetricsTable(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        return this.tableName;
    }
}