package org.rhq.enterprise.server.storage;

/**
 * @author John Sanda
 */
public class StorageConfigurationException extends RuntimeException {

    public StorageConfigurationException() {
        super();
    }

    public StorageConfigurationException(String message) {
        super(message);
    }

    public StorageConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageConfigurationException(Throwable cause) {
        super(cause);
    }
}
