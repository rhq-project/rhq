package org.rhq.enterprise.gui.coregui.client.util.rpc;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple event mediator for tracker events between event publishers and
 * consumers.
 */
public final class TrackerEventDispatcher {

    private static class LazyHolder {
        public static final TrackerEventDispatcher INSTANCE;

        static {
            INSTANCE = new TrackerEventDispatcher();
        }
    }

    public static TrackerEventDispatcher getInstance() {
        return LazyHolder.INSTANCE;
    }

    private final Set<TrackerChangedEventListener> trackerChangedEventListener;

    private final Set<TrackerStatusEventListener> trackerStatusEventListeners;

    protected TrackerEventDispatcher() {
        trackerChangedEventListener = new HashSet<TrackerChangedEventListener>();
        trackerStatusEventListeners = new HashSet<TrackerStatusEventListener>();

        ActivityIndicator.registerListeners(this);
        RemoteServiceStatistics.registerListeners(this);
    }

    public final void addTrackerChangedEventListener(TrackerChangedEventListener listener) {
        trackerChangedEventListener.add(listener);
    }

    public final void addTrackerStatusEventListener(TrackerStatusEventListener listener) {
        trackerStatusEventListeners.add(listener);
    }

    public void fireTrackerChanged(TrackerChangedEvent event) {
        for (TrackerChangedEventListener listener : trackerChangedEventListener) {
            listener.onTrackerChanged(event);
        }
    }

    public final void fireStatusUpdate(TrackerStatusEvent event) {
        for (TrackerStatusEventListener listener : trackerStatusEventListeners) {
            listener.onStatusChanged(event);
        }
    }
}
