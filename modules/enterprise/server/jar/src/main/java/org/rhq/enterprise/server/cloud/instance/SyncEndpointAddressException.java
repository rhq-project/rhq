package org.rhq.enterprise.server.cloud.instance;

public class SyncEndpointAddressException extends Exception {

    public SyncEndpointAddressException() {
        super();
    }

    public SyncEndpointAddressException(String message) {
        super(message);
    }

    public SyncEndpointAddressException(String message, Throwable cause) {
        super(message, cause);
    }

    public SyncEndpointAddressException(Throwable cause) {
        super(cause);
    }
}
