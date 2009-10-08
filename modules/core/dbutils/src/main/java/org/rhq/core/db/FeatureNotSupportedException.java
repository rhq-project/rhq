package org.rhq.core.db;

public class FeatureNotSupportedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FeatureNotSupportedException(String message) {
        super(message);
    }

}
