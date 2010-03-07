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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter;

/**
 * @author Greg Hinkle
 */
public class MonitoringRequestCallback implements RequestCallback {

    private int id;
    private String name;
    private long start = System.currentTimeMillis();

    private RequestCallback callback;

    public MonitoringRequestCallback(String name, RequestCallback callback) {
        this.name = name;
        id = RPCManager.getInstance().register(this);
        this.callback = callback;
        if (callback instanceof RequestCallbackAdapter) {
//            ((RequestCallbackAdapter)callback)

        }

    }

    public void onError(Request request, Throwable exception) {
        RPCManager.getInstance().failCall(this);
        callback.onError(request, exception);
    }

    public void onResponseReceived(Request request, Response response) {
        RPCManager.getInstance().succeedCall(this);
        callback.onResponseReceived(request, response);
    }


    public String getName() {
        return name;
    }

    public long age() {
        return System.currentTimeMillis() - start;
    }
}
