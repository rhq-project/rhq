package org.rhq.enterprise.server.content;

public class ContentSourceException extends ContentException {

    private static final long serialVersionUID = 1L;

    public ContentSourceException() {
    }

    public ContentSourceException(String message) {
        super(message);
    }

    public ContentSourceException(Throwable cause) {
        super(cause);
    }

    public ContentSourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
