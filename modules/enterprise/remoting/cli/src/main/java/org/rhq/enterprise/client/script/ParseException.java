package org.rhq.enterprise.client.script;

import java.util.Arrays;

public class ParseException extends Exception {

    public ParseException() {
        super();
    }

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, String[] cmdLineTokens) {
        super(message + ": " + Arrays.toString(cmdLineTokens));
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }
}
