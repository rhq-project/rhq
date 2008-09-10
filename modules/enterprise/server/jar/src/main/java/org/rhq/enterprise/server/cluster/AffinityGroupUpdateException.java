package org.rhq.enterprise.server.cluster;

public class AffinityGroupUpdateException extends AffinityGroupException {

    private static final long serialVersionUID = 1L;

    public AffinityGroupUpdateException() {
    }

    public AffinityGroupUpdateException(String message) {
        super(message);
    }

    public AffinityGroupUpdateException(Throwable cause) {
        super(cause);
    }

    public AffinityGroupUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

}
