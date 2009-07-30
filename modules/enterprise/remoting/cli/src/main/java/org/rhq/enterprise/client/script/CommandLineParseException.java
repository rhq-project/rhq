package org.rhq.enterprise.client.script;

import java.util.Arrays;

public class CommandLineParseException extends Exception {

    public CommandLineParseException() {
        super();
    }

    public CommandLineParseException(String message) {
        super(message);
    }

    public CommandLineParseException(String message, String[] cmdLineTokens) {
        super(message + ": " + Arrays.toString(cmdLineTokens));
    }

    public CommandLineParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandLineParseException(Throwable cause) {
        super(cause);
    }
}
