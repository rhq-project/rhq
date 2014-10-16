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
package org.rhq.coregui.client.util.rpc;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LoginView;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.util.Log;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class TrackingRequestCallback implements RequestCallback {

    private int id;
    private String name;
    private long start = System.currentTimeMillis();

    private static final int STATUS_CODE_OK = 200;
    private static final int STATUS_CODE_ERROR_INTERNET_NO_RESPONSE = 0;
    private static final int STATUS_CODE_ERROR_INTERNET_CANNOT_CONNECT = 12029;
    private static final int STATUS_CODE_ERROR_INTERNET_CONNECTION_ABORTED = 12030;

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
        int statusCode;
        String statusText;

        try {
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
        } catch (Throwable t) {
            // If the server is unreachable or has terminated firefox may generate a JavaScript exception
            // when trying to read the response object. Let the user know the server is unreachable.
            // (http://helpful.knobs-dials.com/index.php/0x80004005_%28NS_ERROR_FAILURE%29_and_other_firefox_errors)) 
            if (UserSessionManager.isLoggedIn()) {
                CoreGUI.getErrorHandler().handleError(CoreGUI.getMessages().view_core_serverUnreachable(), t);
            }
            return;
        }

        if (Log.isTraceEnabled()) {
            Log.trace(toString() + ": " + statusCode + "/" + statusText);
        }

        RemoteServiceStatistics.record(getName(), getAge());

        switch (statusCode) {
        case STATUS_CODE_OK:
            if (response != null && response.getText() != null && response.getText().isEmpty()
                && !LoginView.isLoginShowing()) { // this happens when the RHQ server was restarted
                Log.error("RHQ server was probably restarted. Showing the login page.");
                new LoginView().showLoginDialog(true);
                break;
            }
            RPCTracker.getInstance().succeedCall(this);
            callback.onResponseReceived(request, response);
            break;

        // these status codes are known to be returned from various browsers when the server is lost or not responding
        case STATUS_CODE_ERROR_INTERNET_NO_RESPONSE:
        case STATUS_CODE_ERROR_INTERNET_CANNOT_CONNECT:
        case STATUS_CODE_ERROR_INTERNET_CONNECTION_ABORTED:
            RPCTracker.getInstance().failCall(this);
            // If the server is unreachable or has terminated, and the user is still logged in,
            // let them know the server is now unreachable. 
            if (UserSessionManager.isLoggedIn()) {
                CoreGUI.getErrorHandler().handleError(CoreGUI.getMessages().view_core_serverUnreachable());
            } else {
                new LoginView().showLoginDialog(true);
            }
            break;

        default:
            RPCTracker.getInstance().failCall(this);
            // process the failure only if the user still logged in
            if (UserSessionManager.isLoggedIn()) {
                callback.onResponseReceived(request, response);
            } else {
                new LoginView().showLoginDialog(true);
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
