package org.rhq.server.metrics.aggregation;

/**
 * @author John Sanda
 */
public class CacheIndexQueryException extends RuntimeException {

    public CacheIndexQueryException() {
    }

    public CacheIndexQueryException(String message) {
        super(message);
    }

    public CacheIndexQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheIndexQueryException(Throwable cause) {
        super(cause);
    }
}
