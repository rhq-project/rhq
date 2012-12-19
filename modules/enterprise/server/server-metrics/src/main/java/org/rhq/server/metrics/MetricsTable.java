package org.rhq.server.metrics;

public enum MetricsTable {

    INDEX("metrics_index", -1),
    RAW("raw_metrics", DateTimeService.SEVEN_DAYS),
    ONE_HOUR("one_hour_metrics", DateTimeService.TWO_WEEKS),
    SIX_HOUR("six_hour_metrics", DateTimeService.ONE_MONTH),
    TWENTY_FOUR_HOUR("twenty_four_hour_metrics", DateTimeService.ONE_YEAR);

    private final String tableName;
    private final int ttl;

    private MetricsTable(String tableName, int ttl) {
        this.tableName = tableName;
        this.ttl = ttl;
    }

    public String getTableName() {
        return this.tableName;
    }

    public int getTTL() {
        return this.ttl;
    }

    @Override
    public String toString() {
        return this.tableName;
    }
}