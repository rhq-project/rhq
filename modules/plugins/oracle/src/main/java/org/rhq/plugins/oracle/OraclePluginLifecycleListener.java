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
package org.rhq.plugins.oracle;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.util.exception.ThrowableUtil;

public class OraclePluginLifecycleListener implements PluginLifecycleListener {
    private final Log log = LogFactory.getLog(OraclePluginLifecycleListener.class);

    public void initialize(PluginContext context) throws Exception {
        // no-op
    }

    public void shutdown() {
        // so we do not cause our classloader to leak perm gen, we need to de-register
        // any and all JDBC drivers this plugin registered
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            try {
                Driver driver = drivers.nextElement();
                DriverManager.deregisterDriver(driver);
                log.debug("Deregistered JDBC driver: " + driver.getClass());
            } catch (Exception e) {
                log.warn("Failed to deregister JDBC drivers - memory might leak" + ThrowableUtil.getAllMessages(e));
            }
        }

        log.debug(this.getClass().getSimpleName() + " completed shutdown.");
        return;
    }
}
