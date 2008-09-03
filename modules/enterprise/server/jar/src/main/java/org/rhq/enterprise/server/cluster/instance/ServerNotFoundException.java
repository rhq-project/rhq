package org.rhq.enterprise.server.cluster.instance;

public class ServerNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServerNotFoundException() {
        super();
    }

    public ServerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerNotFoundException(String message) {
        super(message);
    }

    public ServerNotFoundException(Throwable cause) {
        super(cause);
    }

}
