/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.custom;

import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;

/**
 * A sample lifecycle listener for the sample generic plugin. This listener will be
 * the main plugin component the server uses to start and stop the plugin.
 */
public class MyLifecycleListener implements ServerPluginComponent {

    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        System.out.println("The sample plugin has been initialized!!! : " + this);
    }

    public void start() {
        System.out.println("The sample plugin has started!!! : " + this);
    }

    public void stop() {
        System.out.println("The sample plugin has stopped!!! : " + this);
    }

    public void shutdown() {
        System.out.println("The sample plugin has been shut down!!! : " + this);
    }

    public void myScheduledJobMethod1() throws Exception {
        System.out.println("The sample plugin scheduled job [myScheduledJobMethod1] has triggered!!! : " + this);
    }

    public void myScheduledJobMethod2() throws Exception {
        System.out.println("The sample plugin scheduled job [myScheduledJobMethod2] has triggered!!! : " + this);
    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }

        StringBuilder str = new StringBuilder();
        str.append("plugin-name=").append(this.context.getPluginEnvironment().getPluginName()).append(",");
        str.append("plugin-url=").append(this.context.getPluginEnvironment().getPluginUrl()); // do not append ,
        return str.toString();
    }
}
