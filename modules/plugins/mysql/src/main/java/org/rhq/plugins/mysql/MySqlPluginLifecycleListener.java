/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.mysql;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;

/**
 *
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlPluginLifecycleListener implements PluginLifecycleListener {
    private final Log log = LogFactory.getLog(MySqlPluginLifecycleListener.class);
    private String pluginName;

    public void initialize(PluginContext context) throws Exception {
        pluginName = context.getPluginName();
    }

    public void shutdown() {
        if (log.isDebugEnabled()) {
            log.debug(new StringBuilder().append(pluginName).append(" Plugin Shutdown").toString());
        }
        MySqlConnectionManager.getConnectionManager().shutdown();
    }
}
