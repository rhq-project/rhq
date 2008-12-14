package org.rhq.enterprise.server.event;

public class EventException extends Exception {

    private static final long serialVersionUID = 1L;

    public EventException() {
    }

    public EventException(String message) {
        super(message);
    }

    public EventException(Throwable cause) {
        super(cause);
    }

    public EventException(String message, Throwable cause) {
        super(message, cause);
    }

}
