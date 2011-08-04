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

        RemoteServiceStatistics.record(getName(), getAge());
        RPCTracker.getInstance().failCall(this);
        if (UserSessionManager.isLoggedIn()) { // only handle failures if user still logged in
            callback.onError(request, exception);
        }
    }

    public void onResponseReceived(Request request, Response response) {
        if (Log.isTraceEnabled()) {
            Log.trace(toString() + ": " + response.getStatusCode() + "/" + response.getStatusText());
        }

        RemoteServiceStatistics.record(getName(), getAge());
        if (STATUS_CODE_OK == response.getStatusCode()) {
            RPCTracker.getInstance().succeedCall(this);
            callback.onResponseReceived(request, response);
        } else {
            RPCTracker.getInstance().failCall(this);
            if (UserSessionManager.isLoggedIn()) { // only handle failures if user still logged in
                callback.onResponseReceived(request, response);
            }
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
