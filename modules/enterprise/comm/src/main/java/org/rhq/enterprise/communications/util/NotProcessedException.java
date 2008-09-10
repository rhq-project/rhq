package org.rhq.enterprise.communications.util;

/** 
 * Thrown when command processing is unavailable.  This is typically due to temporary global suspension of command processing.
 * It is different from {@link NotPermittedException} in that the command request itself did not violate any
 * constraint (e.g. security, concurrency limit).
 * 
 * Can optionally be provided a String supplying a reason for not processing the command.
 */
public class NotProcessedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    String reason = null;

    public NotProcessedException(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
