package org.rhq.cassandra.ccm.cli;

/**
 * @author John Sanda
 */
public class CLIException extends Exception {
    public CLIException() {
        super();
    }

    public CLIException(String message) {
        super(message);
    }

    public CLIException(String message, Throwable cause) {
        super(message, cause);
    }

    public CLIException(Throwable cause) {
        super(cause);
    }

}
