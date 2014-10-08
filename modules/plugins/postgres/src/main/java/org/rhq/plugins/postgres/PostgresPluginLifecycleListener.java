/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.postgres;

import java.lang.reflect.Field;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.util.exception.ThrowableUtil;

public class PostgresPluginLifecycleListener implements PluginLifecycleListener {
    private final Log log = LogFactory.getLog(PostgresPluginLifecycleListener.class);

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

        try {
            // additionally, we need to work around a known classloader leak
            // in the postgres driver (see https://github.com/pgjdbc/pgjdbc/pull/188 and
            // https://github.com/pgjdbc/pgjdbc/pull/197)
            cleanupDriverTimerThread();
        } catch (Exception e) {
            log.warn("Failed to clean up Postgresql JDBC driver classloader leak. " +
                "If this warning appears multiple times during the lifetime of the agent, there's possibility of " +
                "permgen depletion.", e);
        }

        log.debug(this.getClass().getSimpleName() + " completed shutdown.");
    }

    private void cleanupDriverTimerThread() throws ClassNotFoundException {
        Class<?> driverClass = Class.forName("org.postgresql.Driver");
        try {
            Field cancelTimerField = driverClass.getDeclaredField("cancelTimer");
            cancelTimerField.setAccessible(true);
            Timer cancelTimer = (Timer) cancelTimerField.get(null);
            if (cancelTimer != null) {
                cancelTimer.cancel();
            }
        } catch (NoSuchFieldException e) {
            // great, so the "cancelTimer" field of the driver class has disappeared - maybe we're using a new version
            // of the driver which fixed the leak?
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to shutdown the leaking statement cancellation timer of Postgresql JDBC driver.", e);
        }
    }
}
