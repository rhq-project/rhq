package org.rhq.enterprise.gui.coregui.client.util.rpc;

/**
 * Interface for subscribers of tracked RPC status events.
 */
public interface TrackerStatusEventListener {
    /**
     * An RPC response was received and is ready to be processed or dropped;
     * this event indicates how the response will be handled.
     *
     * @param event the event indicating how the RPC response will be handled.
     */
    void onStatusChanged(TrackerStatusEvent event);
}
