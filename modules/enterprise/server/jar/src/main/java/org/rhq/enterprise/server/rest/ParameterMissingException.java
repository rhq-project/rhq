package org.rhq.enterprise.server.rest;

/**
 * Exception thrown if (Query) Parameters are missing
 * @author Heiko W. Rupp
 */
public class ParameterMissingException extends RuntimeException {

    public ParameterMissingException(String what) {
        super("(Query)param " + what + " is missing");
    }
}
