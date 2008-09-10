package org.rhq.enterprise.server.cluster;

public class AffinityGroupException extends Exception {

    private static final long serialVersionUID = 1L;

    public AffinityGroupException() {
    }

    public AffinityGroupException(String message) {
        super(message);
    }

    public AffinityGroupException(Throwable cause) {
        super(cause);
    }

    public AffinityGroupException(String message, Throwable cause) {
        super(message, cause);
    }

}
