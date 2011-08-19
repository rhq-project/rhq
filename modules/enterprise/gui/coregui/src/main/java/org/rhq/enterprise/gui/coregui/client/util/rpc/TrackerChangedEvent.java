package org.rhq.enterprise.gui.coregui.client.util.rpc;

/**
 * Tracking system changed due to a callback being added or removed.
 */
public class TrackerChangedEvent {
    /**
     * Callback has been created and is about to be registered with GWT
     * to handle a message response.
     */
    public final static int CALL_REGISTER = 3;
    /**
     * Callback has been created and is about to be registered with GWT
     * to handle a message response.
     */
    public final static int CALL_COMPLETE = 4;

    private final TrackingRequestCallback callback;
    private final int kind;

    public TrackerChangedEvent(TrackingRequestCallback callback, int kind) {
        this.callback = callback;
        this.kind = kind;
    }

    public TrackingRequestCallback getCallback() {
        return callback;
    }

    public int getKind() {
        return kind;
    }
}
