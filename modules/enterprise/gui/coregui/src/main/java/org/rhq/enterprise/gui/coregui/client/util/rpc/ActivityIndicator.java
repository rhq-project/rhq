/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.util.rpc;

import com.allen_sauer.gwt.log.client.Log;
import com.smartgwt.client.widgets.Img;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

import java.util.HashSet;
import java.util.Set;

/**
 * Updates the activity indicator based upon tracked callback status and life-cycle.
 */
public class ActivityIndicator implements TrackerChangedEventListener, TrackerStatusEventListener {

    private static class LazyHolder {
        public static final ActivityIndicator INSTANCE = new ActivityIndicator();
    }

    public static ActivityIndicator getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static final Messages MSG = CoreGUI.getMessages();

    private Set<TrackingRequestCallback> inProgress = new HashSet<TrackingRequestCallback>();

    private Img activityIndicator;

    private ActivityIndicator() {
        activityIndicator = new Img("/coregui/images/ajax-loader.gif", 16, 16);
        activityIndicator.setZIndex(10000);
        activityIndicator.setLeft(10);
        activityIndicator.setTop(40);
        activityIndicator.draw();
    }

    public static void registerListeners(TrackerEventDispatcher dispatcher) {
        dispatcher.addTrackerChangedEventListener(getInstance());
        dispatcher.addTrackerStatusEventListener(getInstance());
    }

    public int getQueueDepth() {
        return inProgress.size();
    }

    public void refresh() {
        Log.trace("RPCTracker queue depth is " + getQueueDepth());
        if (getQueueDepth() > 0) {
            activityIndicator.show();

            int numberOfActiveRequests = inProgress.size();
            String message = MSG.util_rpcManager_activeRequests(String.valueOf(numberOfActiveRequests));
            StringBuilder buf = new StringBuilder().append("<b>").append(message).append("</b>");
            for (TrackingRequestCallback callback : inProgress) {
                buf.append("<br/>");
                buf.append(callback);
            }

            activityIndicator.setTooltip(buf.toString());
        } else {
            activityIndicator.hide();
        }
    }

    @Override
    public void onTrackerChanged(TrackerChangedEvent event) {
        TrackingRequestCallback callback = event.getCallback();
        if (event.getKind() == TrackerChangedEvent.CALL_REGISTER) {
            Log.debug("RPCTracker added: " + callback);
            inProgress.add(callback);
        } else {
            Log.debug("RPCTracker removed: " + callback);
            inProgress.remove(callback);
        }
        refresh();
    }

    @Override
    public void onStatusChanged(TrackerStatusEvent event) {
        TrackingRequestCallback callback = event.getCallback();
        switch (event.getKind()) {
            case TrackerStatusEvent.RECV_SUCCESS:
                Log.trace("RPCTracker success: " + callback);
                break;
            case TrackerStatusEvent.VIEW_CHANGED:
                Log.trace("RPCTracker dropped: " + callback);
                break;
            case TrackerStatusEvent.RECV_FAILURE:
                Log.trace("RPCTracker failure: " + callback);
                break;
        }
    }
}
