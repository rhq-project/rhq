package org.rhq.server.metrics.aggregation;

/**
 * @author John Sanda
 */
public enum AggregationType {

    RAW("raw data"),

    ONE_HOUR("one hour data"),

    SIX_HOUR("six hour data");

    private String type;

    private AggregationType(String type) {
        this.type = type;
    }


    @Override
    public String toString() {
        return type;
    }
}
