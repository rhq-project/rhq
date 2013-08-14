package org.rhq.enterprise.server.storage;

/**
 * @author John Sanda
 */
public class StorageNodeDeploymentException extends RuntimeException {

    public StorageNodeDeploymentException() {
    }

    public StorageNodeDeploymentException(String message) {
        super(message);
    }

    public StorageNodeDeploymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageNodeDeploymentException(Throwable cause) {
        super(cause);
    }
}
