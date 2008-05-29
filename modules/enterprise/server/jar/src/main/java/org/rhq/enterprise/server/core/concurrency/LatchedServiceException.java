package org.rhq.enterprise.server.core.concurrency;

public class LatchedServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LatchedServiceException() {
        super();
    }

    public LatchedServiceException(String message) {
        super(message);
    }

    public LatchedServiceException(Throwable cause) {
        super(cause);
    }

    public LatchedServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
