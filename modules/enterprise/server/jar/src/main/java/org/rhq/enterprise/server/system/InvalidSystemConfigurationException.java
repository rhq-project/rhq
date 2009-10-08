package org.rhq.enterprise.server.system;

public class InvalidSystemConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidSystemConfigurationException() {
    }

    public InvalidSystemConfigurationException(String message) {
        super(message);
    }

    public InvalidSystemConfigurationException(Throwable cause) {
        super(cause);
    }

    public InvalidSystemConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
