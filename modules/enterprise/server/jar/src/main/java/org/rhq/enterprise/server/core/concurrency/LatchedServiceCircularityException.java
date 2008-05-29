package org.rhq.enterprise.server.core.concurrency;

public class LatchedServiceCircularityException extends LatchedServiceException {

    private static final long serialVersionUID = 1L;

    public LatchedServiceCircularityException() {
        super();
    }

    public LatchedServiceCircularityException(String message, Throwable cause) {
        super(message, cause);
    }

    public LatchedServiceCircularityException(String message) {
        super(message);
    }

    public LatchedServiceCircularityException(Throwable cause) {
        super(cause);
    }

}
