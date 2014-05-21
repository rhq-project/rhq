package org.rhq.modules.plugins.jbossas7.json;

public class ResultFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResultFailedException(String message) {
        super(message);
    }
}
