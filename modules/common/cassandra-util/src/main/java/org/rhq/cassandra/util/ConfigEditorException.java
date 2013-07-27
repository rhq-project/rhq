package org.rhq.cassandra.util;

/**
 * @author John Sanda
 */
public class ConfigEditorException extends RuntimeException {

    public ConfigEditorException() {
    }

    public ConfigEditorException(String message) {
    }

    public ConfigEditorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigEditorException(Throwable cause) {
        super(cause);
    }
}
