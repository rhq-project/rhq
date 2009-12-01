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
package org.rhq.enterprise.server.plugin.pc.generic;

import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;

/**
 * A sample lifecycle listener for the sample generic plugin. This listener will be
 * the main interface the server uses to start and stop the plugin.
 */
public class TestLifecycleListener implements ServerPluginComponent {

    public enum LifecycleState {
        UNINITIALIZED, INITIALIZED, STARTED, STOPPED
    };

    public ServerPluginContext context;
    public LifecycleState state = LifecycleState.UNINITIALIZED;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        if (state == LifecycleState.UNINITIALIZED) {
            state = LifecycleState.INITIALIZED;
        } else {
            println("!!! lifecycle listener is in a bad state - this is a BUG !!!: " + state);
        }
        println("test server plugin has been initialized");
    }

    public void start() {
        if (state == LifecycleState.INITIALIZED) {
            state = LifecycleState.STARTED;
        } else {
            println("!!! lifecycle listener is in a bad state - this is a BUG !!!: " + state);
        }
        println("test server plugin has started");
    }

    public void stop() {
        if (state == LifecycleState.STARTED) {
            state = LifecycleState.STOPPED;
        } else {
            println("!!! lifecycle listener is in a bad state - this is a BUG !!!: " + state);
        }
        println("test server plugin has stopped");
    }

    public void shutdown() {
        if (state == LifecycleState.STOPPED) {
            state = LifecycleState.UNINITIALIZED;
        } else {
            println("!!! lifecycle listener is in a bad state - this is a BUG !!!: " + state);
        }
        println("test server plugin has been shut down");
    }

    private void println(String msg) {
        System.out.println(msg + "!!! : " + this);
    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }

        StringBuilder str = new StringBuilder();
        str.append("plugin-key=").append(this.context.getPluginEnvironment().getPluginKey()).append(",");
        str.append("plugin-url=").append(this.context.getPluginEnvironment().getPluginUrl()).append(",");
        str.append("data-dir=").append(this.context.getDataDirectory()).append(",");
        str.append("tmp-dir=").append(this.context.getTemporaryDirectory()); // do not append ,
        return str.toString();
    }
}
