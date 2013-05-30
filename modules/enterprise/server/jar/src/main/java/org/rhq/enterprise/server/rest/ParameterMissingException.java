package org.rhq.enterprise.server.rest;

import javax.ejb.ApplicationException;

/**
 * Exception thrown if (Query) Parameters are missing
 * @author Heiko W. Rupp
 */
@ApplicationException(rollback = false, inherited = true)
public class ParameterMissingException extends RuntimeException {

    public ParameterMissingException(String what) {
        super("(Query)param " + what + " is missing");
    }
}
