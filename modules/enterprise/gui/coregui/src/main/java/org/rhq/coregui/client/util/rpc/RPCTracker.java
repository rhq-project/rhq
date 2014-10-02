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

import java.util.HashSet;
import java.util.Set;

import com.smartgwt.client.widgets.Img;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.util.Log;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class RPCTracker {

    private static final Messages MSG = CoreGUI.getMessages();

    private static final RPCTracker INSTANCE = new RPCTracker();

    private Set<TrackingRequestCallback> inProgress = new HashSet<TrackingRequestCallback>();

    private RPCTracker() {
    }

    public static RPCTracker getInstance() {
        return INSTANCE;
    }

    public void register(TrackingRequestCallback callback) {
        Log.debug("RPCTracker register: " + callback);

        inProgress.add(callback);
        refresh();
    }

    public void failCall(TrackingRequestCallback callback) {
        Log.trace("RPCTracker failure: " + callback);

        inProgress.remove(callback);
        refresh();
    }

    public void succeedCall(TrackingRequestCallback callback) {
        Log.trace("RPCTracker success: " + callback);

        inProgress.remove(callback);
        refresh();
    }

    public int getQueueDepth() {
        return inProgress.size();
    }

    public void refresh() {
        Log.trace("RPCTracker queue depth is " + getQueueDepth());
        if (getQueueDepth() > 0) {

            int numberOfActiveRequests = inProgress.size();
            String message = MSG.util_rpcManager_activeRequests(String.valueOf(numberOfActiveRequests));
            StringBuilder buf = new StringBuilder().append("<b>").append(message).append("</b>");
            for (TrackingRequestCallback callback : inProgress) {
                buf.append("<br/>");
                buf.append(callback);
            }
        }
    }

}
