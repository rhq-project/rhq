package org.rhq.enterprise.server.content;

public class ChannelException extends ContentException {

    private static final long serialVersionUID = 1L;

    public ChannelException() {
    }

    public ChannelException(String message) {
        super(message);
    }

    public ChannelException(Throwable cause) {
        super(cause);
    }

    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }

}
