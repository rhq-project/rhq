package org.rhq.enterprise.gui.coregui.client.util.rpc;

/**
 * Interface for subscribers of tracked RPC events.
 */
public interface TrackerChangedEventListener {
    void onTrackerChanged(TrackerChangedEvent event);
}
