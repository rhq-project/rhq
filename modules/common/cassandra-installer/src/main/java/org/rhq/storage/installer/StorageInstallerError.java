package org.rhq.storage.installer;

/**
 * Thrown to indicate an unexpected, internal error occurred during storage node installation.
 *
 * @author John Sanda
 */
public class StorageInstallerError extends StorageInstallerException {

    public StorageInstallerError() {
        super();
    }

    public StorageInstallerError(String message) {
        super(message);
    }

    public StorageInstallerError(String message, int errorCode) {
        super(message, errorCode);
    }

    public StorageInstallerError(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageInstallerError(String message, Throwable cause, int errorCode) {
        super(message, cause, errorCode);
    }

    public StorageInstallerError(Throwable cause) {
        super(cause);
    }
}
