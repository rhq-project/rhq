package org.rhq.enterprise.server.cluster;

public class ClusterManagerException extends AffinityGroupException {

    private static final long serialVersionUID = 1L;

    public ClusterManagerException() {
    }

    public ClusterManagerException(String message) {
        super(message);
    }

    public ClusterManagerException(Throwable cause) {
        super(cause);
    }

    public ClusterManagerException(String message, Throwable cause) {
        super(message, cause);
    }

}
