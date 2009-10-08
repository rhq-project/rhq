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
package org.rhq.core.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import mazz.i18n.Logger;

/**
 * Provides database utilities. See also {@link DatabaseTypeFactory} for additional database functionality.
 *
 * @author John Mazzitelli
 *
 * @see     DatabaseTypeFactory
 */
public class DbUtil {
    private static final Logger LOG = DbUtilsI18NFactory.getLogger(DbUtil.class);

    /**
     * Prevents instantiation.
     */
    private DbUtil() {
    }

    /**
     * Given a JDBC URL, this will attempt to create a connection to the database. The connection can then be used to
     * get the {@link DatabaseType} object via {@link DatabaseTypeFactory#getDatabaseType(Connection)}.
     *
     * @param  jdbc_url a connection URL to the database
     * @param  username may be <code>null</code> in which case an anonymous login will be attempted
     * @param  password
     *
     * @return a live database connection
     *
     * @throws SQLException if failed to connect to the database
     */
    public static Connection getConnection(String jdbc_url, String username, String password) throws SQLException {
        DatabaseTypeFactory.loadJdbcDriver(jdbc_url); // ignore null return - let the connection attempt fail with exception

        Connection conn;

        if (username != null) {
            conn = DriverManager.getConnection(jdbc_url, username, password);
        } else {
            conn = DriverManager.getConnection(jdbc_url);
        }

        return conn;
    }

    /**
     * Pings a database by simply connecting to it. If the user credentials allow for a successful connection to the
     * given JDBC URL, this returns <code>true</code>. If the connection attempt fails, this returns <code>false</code>.
     *
     * @param  jdbc_url
     * @param  username
     * @param  password
     *
     * @return <code>true</code> if a connection to the database can be made
     */
    public static boolean ping(String jdbc_url, String username, String password) {
        Connection conn = null;
        boolean ping;

        try {
            conn = getConnection(jdbc_url, username, password);
            ping = true;
        } catch (Exception e) {
            conn = null;
            ping = false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }

        return ping;
    }

    /**
     * Given an SQL exception, this will return a string that contains the error code and message for it and its causes.
     *
     * @param  e
     *
     * @return error message containing all the SQL error codes and messages
     */
    public static String getSQLExceptionString(SQLException e) {
        StringBuffer str = new StringBuffer();

        do {
            str.append("ErrorCode=[" + e.getErrorCode() + "]; ");
            str.append("SQLState=[" + e.getSQLState() + "]; ");
            str.append("Message=[" + e.getMessage() + "]; ");
            str.append("Type=[" + e.getClass().getName() + "]");

            e = e.getNextException();
            if (e != null) {
                str.append(" -> ");
            }
        } while (e != null);

        return str.toString();
    }
}