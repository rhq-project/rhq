package org.rhq.server.metrics;

/**
 * @author John Sanda
 */
public class AbortedException extends Exception {

    public AbortedException() {
        super();
    }

    public AbortedException(String message) {
        super(message);
    }

    public AbortedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbortedException(Throwable cause) {
        super(cause);
    }
}
