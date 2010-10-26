package org.rhq.core.clientapi.server.discovery;

/**
 * Exception to indicate that a report contains one or more resource types that have been marked for
 * deletion. 
 */
public class StaleTypeException extends InvalidInventoryReportException {
    public StaleTypeException() {
        super();
    }

    public StaleTypeException(String message) {
        super(message);
    }

    public StaleTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public StaleTypeException(Throwable cause) {
        super(cause);
    }
}
