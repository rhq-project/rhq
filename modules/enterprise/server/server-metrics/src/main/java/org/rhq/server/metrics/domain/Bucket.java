package org.rhq.server.metrics.domain;

/**
 * @author John Sanda
 */
public enum Bucket {

    ONE_HOUR("one_hour"),

    SIX_HOUR("six_hour"),

    TWENTY_FOUR_HOUR("twenty_four_hour");

    private String tableName;

    private Bucket(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        return tableName;
    }

    public static Bucket fromString(String table) {
        if (table.equals(ONE_HOUR.tableName)) {
            return ONE_HOUR;
        } else if (table.equals(SIX_HOUR.tableName)) {
            return SIX_HOUR;
        } else if (table.equals(TWENTY_FOUR_HOUR.tableName)) {
            return TWENTY_FOUR_HOUR;
        } else {
            throw new IllegalArgumentException(table + " is not a recognized table name");
        }
    }
}
