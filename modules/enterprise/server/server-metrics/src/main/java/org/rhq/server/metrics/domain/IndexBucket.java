package org.rhq.server.metrics.domain;

/**
 * @author John Sanda
 */
public enum IndexBucket {

    RAW("raw"),

    ONE_HOUR("one_hour"),

    SIX_HOUR("six_hour");

    private String tableName;

    private IndexBucket(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        return tableName;
    }
}
