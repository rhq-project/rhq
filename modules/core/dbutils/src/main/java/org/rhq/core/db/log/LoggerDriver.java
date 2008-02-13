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
package org.rhq.core.db.log;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.rhq.core.db.DbUtil;

/**
 * A wrapper that logs access to the wrapped real JDBC driver.
 */
public class LoggerDriver implements Driver {
    /**
     * System property whose value is the logger listener class to use.
     */
    public static final String PROP_LOGLISTENER = "jdbcLogListener";

    /**
     * System property whose value is a boolean which, if <code>true</code>, forces only SQL executions to be logged.
     */
    public static final String PROP_LOGSQLONLY = "jdbcLogSqlOnly";

    /**
     * The JDBC URL prefix that indicates this JDBC driver. The rest of the URL is the "real" JDBC URL.
     */
    public static final String JDBC_URL_PREFIX = "jdbc:jon-log:";

    static {
        try {
            DriverManager.registerDriver(new LoggerDriver());
        } catch (Exception e) {
            throw new Error("Cannot register our JDBC LoggerDriver");
        }
    }

    /**
     * The logger that will log notifications for this driver and all JDBC objects created by this driver.
     */
    private LoggerListener loggerListener;

    /**
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(JDBC_URL_PREFIX);
    }

    /**
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        // we assume all wrapped drivers need user and password
        DriverPropertyInfo user = new DriverPropertyInfo("user", null);
        DriverPropertyInfo password = new DriverPropertyInfo("password", null);
        return new DriverPropertyInfo[] { user, password };
    }

    /**
     * @see java.sql.Driver#getMajorVersion()
     */
    public int getMajorVersion() {
        return 2;
    }

    /**
     * @see java.sql.Driver#getMinorVersion()
     */
    public int getMinorVersion() {
        return 0;
    }

    /**
     * @see java.sql.Driver#jdbcCompliant()
     */
    public boolean jdbcCompliant() {
        return true; // assume any driver we wrap will be compliant
    }

    /**
     * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
     */
    public Connection connect(String url, Properties info) throws SQLException {
        if (!url.startsWith(JDBC_URL_PREFIX)) { // nope, not us
            return null;
        }

        // Strip leading prefix - the remaining part of the URL is the "real" JDBC URL
        try {
            url = url.substring(JDBC_URL_PREFIX.length());
        } catch (Exception e) {
            throw new SQLException("Invalid URL: " + url);
        }

        loggerListener = createAndInitializeDefaultListener();

        String user = info.getProperty("user");
        String password = info.getProperty("password");

        loggerListener.log("LoggerDriver: connect(" + url + ',' + user + ",****)");

        Connection conn = DbUtil.getConnection(url, user, password);

        return new LoggerConnection(this, conn);
    }

    /**
     * Returns the listener that is responsible for logging notifications.
     *
     * @return listener
     */
    public LoggerListener getLoggerListener() {
        return loggerListener;
    }

    /**
     * Determines the default logger listener used by this driver and all its JDBC objects that it creates. This not
     * only creates the listener but also {@link LoggerListener#initialize(boolean) initializes it}.
     *
     * @return logger listener
     */
    private LoggerListener createAndInitializeDefaultListener() {
        String clazz_name = System.getProperty(PROP_LOGLISTENER);
        boolean log_sql_only = Boolean.getBoolean(PROP_LOGSQLONLY);

        LoggerListener listener;

        try {
            listener = (LoggerListener) Class.forName(clazz_name).newInstance();
        } catch (Exception e) {
            listener = new StdErrLoggerListener();
        }

        listener.initialize(log_sql_only);

        return listener;
    }
}