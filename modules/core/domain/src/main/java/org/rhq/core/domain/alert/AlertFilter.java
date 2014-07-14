package org.rhq.core.domain.alert;

/**
 * Enum to support available Alert filters.
 *
 * @author Michael Burman
 */
public enum AlertFilter {
    ACKNOWLEDGED_STATUS("Acknowledged"), RECOVERED_STATUS("Recovered"), RECOVERY_TYPE("Recovery alerts");

    private String displayName;

    private AlertFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
