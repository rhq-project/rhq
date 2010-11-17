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
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class MonitoringRequestCallback implements RequestCallback {

    private int id;
    private String name;
    private long start = System.currentTimeMillis();

    private static final int STATUS_CODE_OK = 200;

    private RequestCallback callback;

    public MonitoringRequestCallback(int callId, String name, RequestCallback callback) {
        this.name = name;
        this.id = callId;
        this.callback = callback;
    }

    public void onError(Request request, Throwable exception) {
        Log.trace("MonitoringRequestCallback(" + this + "): onError " + exception.getMessage());
        RPCManager.getInstance().failCall(this);
        callback.onError(request, exception);
    }

    public void onResponseReceived(Request request, Response response) {
        Log.trace("MonitoringRequestCallback(" + this + "): " + response.getStatusCode() + "/"
            + response.getStatusText());
        if (STATUS_CODE_OK == response.getStatusCode()) {
            RPCManager.getInstance().succeedCall(this);
            callback.onResponseReceived(request, response);
        } else {
            RPCManager.getInstance().failCall(this);
            callback.onResponseReceived(request, response);

            // if we have a rich and coordinated client-side loggedIn state, do we need to check upon failure here?
            UserSessionManager.checkLoginStatus(Cookies.getCookie("username"), null, new AsyncCallback<Subject>() {
                @Override
                public void onSuccess(Subject result) {
                    History.fireCurrentHistoryState();
                }

                @Override
                public void onFailure(Throwable caught) {
                    SC.say("Unable to determine login status - check Server status.");
                }
            });
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long age() {
        return System.currentTimeMillis() - start;
    }

    public String toString() {
        return "id=" + id + ", name=" + name + ", age=" + age();
    }

}
