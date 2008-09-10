package org.rhq.enterprise.server.cluster;

public class AffinityGroupCreationException extends AffinityGroupException {

    private static final long serialVersionUID = 1L;

    public AffinityGroupCreationException() {
    }

    public AffinityGroupCreationException(String message) {
        super(message);
    }

    public AffinityGroupCreationException(Throwable cause) {
        super(cause);
    }

    public AffinityGroupCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}
