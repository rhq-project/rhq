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

import com.smartgwt.client.widgets.Img;

import java.util.HashSet;

/**
 * @author Greg Hinkle
 */
public class RPCManager {


    int nextCallId = 0;

    HashSet<MonitoringRequestCallback> inProgress = new HashSet<MonitoringRequestCallback>();

    Img activityIndicator;

    public RPCManager() {
        activityIndicator = new Img("/coregui/images/ajax-loader.gif", 16, 16);
        activityIndicator.setZIndex(10000);
        activityIndicator.setLeft(10);
        activityIndicator.setTop(40);
        activityIndicator.hide();
        activityIndicator.draw();
    }

    public int register(MonitoringRequestCallback callback) {
        inProgress.add(callback);
        refresh();
        return nextCallId++;
    }

    public void failCall(MonitoringRequestCallback callback) {
        System.out.println("RPC [" + callback.getName() + "] failed in [" + callback.age() + "]");
        inProgress.remove(callback);
        refresh();
    }

    public void succeedCall(MonitoringRequestCallback callback) {
        System.out.println("RPC [" + callback.getName() + "] succeeded in [" + callback.age() + "]");
        inProgress.remove(callback);
        refresh();
    }

    public int getQueueDepth() {
        return inProgress.size();
    }

    public void refresh() {
        if (getQueueDepth() > 0) {
            activityIndicator.show();

            StringBuilder buf = new StringBuilder().append("<b>").append(inProgress.size()).append(" active requests</b>");
            for (MonitoringRequestCallback cb : inProgress) {
                buf.append("<br/>");
                buf.append(cb.getName());
            }

            activityIndicator.setTooltip(buf.toString());

        } else {
            activityIndicator.hide();
        }
    }


    public static RPCManager INSTANCE = new RPCManager();

    public static RPCManager getInstance() {
        return INSTANCE;
    }

}
