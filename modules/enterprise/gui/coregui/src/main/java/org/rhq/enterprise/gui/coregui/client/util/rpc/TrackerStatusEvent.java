package org.rhq.enterprise.gui.coregui.client.util.rpc;

/**
 * Represents an immutable tracked RPC callback event.
 */
public final class TrackerStatusEvent {

    /**
     * GWT has fired an RPC request and the response has been successfully
     * received, but the callback has yet to de-serialize and process the
     * response payload; the AsyncCallback.onSuccess method will be called.
     */
    public final static int RECV_SUCCESS = 0;
    /**
     * GWT has fired an RPC request and the response has not been successfully
     * received; the AsyncCallback.onFailure
     */
    public final static int RECV_FAILURE = 1;
    /**
     * GWT has fired an RPC request and the response was not received before
     * the user selected a different view.
     */
    public final static int VIEW_CHANGED = 2;

    private final TrackingRequestCallback callback;

    private final String name;

    private final long age;

    private final int kind;

    public TrackerStatusEvent(TrackingRequestCallback callback, String name, long age, int kind) {
        this.callback = callback;
        this.name = name;
        this.age = age;
        this.kind = kind;
    }

    public TrackingRequestCallback getCallback() {
        return callback;
    }

    public int getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public long getAge() {
        return age;
    }
}
