package org.rhq.modules.plugins.jbossas7.json;

public class UnauthorizedException extends Exception {

    /**
     * "JBAS013456: Unauthorized to execute operation '...' for resource '...' -- \"JBAS013475: Permission denied\"
     */
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

}
