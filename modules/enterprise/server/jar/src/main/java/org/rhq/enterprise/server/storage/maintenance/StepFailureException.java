package org.rhq.enterprise.server.storage.maintenance;

/**
 * @author John Sanda
 */
public class StepFailureException extends Exception {

    public StepFailureException() {
        super();
    }

    public StepFailureException(String message) {
        super(message);
    }

    public StepFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public StepFailureException(Throwable cause) {
        super(cause);
    }
}
