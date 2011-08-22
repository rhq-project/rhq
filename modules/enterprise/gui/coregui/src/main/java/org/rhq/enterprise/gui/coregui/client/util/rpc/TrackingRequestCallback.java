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
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class TrackingRequestCallback implements RequestCallback {

    private int id;
    private String name;
    private long start = System.currentTimeMillis();

    private static final int STATUS_CODE_OK = 200;

    private RequestCallback callback;

    public TrackingRequestCallback(int callId, String name, RequestCallback callback) {
        this.name = name;
        this.id = callId;
        this.callback = callback;
    }

    public void onError(Request request, Throwable exception) {
        if (Log.isTraceEnabled()) {
            Log.trace(toString() + ": onError " + exception.getMessage());
        }

        try {
            TrackerEventDispatcher.getInstance().fireStatusUpdate(
                new TrackerStatusEvent(this, getName(), getAge(),
                    TrackerStatusEvent.RECV_FAILURE));

            if (UserSessionManager.isLoggedIn()) { // only handle failures if user still logged in
                callback.onError(request, exception);
            }
        } finally {
            TrackerEventDispatcher.getInstance().fireTrackerChanged(
                new TrackerChangedEvent(this, TrackerChangedEvent.CALL_COMPLETE));
        }
    }

    public void onResponseReceived(Request request, Response response) {
        if (Log.isTraceEnabled()) {
            Log.trace(toString() + ": " + response.getStatusCode() + "/" + response.getStatusText());
        }

        try {
            if (STATUS_CODE_OK == response.getStatusCode()) {
                final String RHQ_REQUEST_ID_HEADER = "x-rhq-request-id";
                String requestId = response.getHeader(RHQ_REQUEST_ID_HEADER);
                if (requestId != null && !requestId.equals(CoreGUI.get().getRequestId())) {
                    TrackerEventDispatcher.getInstance().fireStatusUpdate(
                        new TrackerStatusEvent(this, getName(), getAge(),
                            TrackerStatusEvent.VIEW_CHANGED));
                } else {
                    TrackerEventDispatcher.getInstance().fireStatusUpdate(
                        new TrackerStatusEvent(this, getName(), getAge(),
                            TrackerStatusEvent.RECV_SUCCESS));
                }

                // todo, n.b. if this is moved into the 'else' clause the entire gui breaks. why?
                callback.onResponseReceived(request, response);
            } else {
                if (UserSessionManager.isLoggedIn()) { // only handle failures if user still logged in
                    TrackerEventDispatcher.getInstance().fireStatusUpdate(
                        new TrackerStatusEvent(this, getName(), getAge(),
                            TrackerStatusEvent.RECV_FAILURE));

                    callback.onResponseReceived(request, response);
                }
            }
        } finally {
            TrackerEventDispatcher.getInstance().fireTrackerChanged(
                new TrackerChangedEvent(this, TrackerChangedEvent.CALL_COMPLETE));
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getAge() {
        return System.currentTimeMillis() - start;
    }

    public String toString() {
        return "TrackingRequestCallback[id=" + id + ", name=" + name + ", age=" + getAge() + "]";
    }

}
