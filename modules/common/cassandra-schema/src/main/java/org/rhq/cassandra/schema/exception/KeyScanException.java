package org.rhq.cassandra.schema.exception;

/**
 * @author John Sanda
 */
public class KeyScanException extends Exception {

    public KeyScanException() {
        super();
    }

    public KeyScanException(String message) {
        super(message);
    }

    public KeyScanException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyScanException(Throwable cause) {
        super(cause);
    }
}
