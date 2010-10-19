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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class to manage the connections to MySQL
 * This class keeps a cache of connections to MySQL and reuses them on demand
 * We assume single threaded access to the Connection in the agent
 * this will need to be reworked if that assumption is not correct
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
class MySqlConnectionManager {

    private HashMap<MySqlConnectionInfo, Connection> connections;
    private static MySqlConnectionManager singleton;
    private Log logger = LogFactory.getLog(MySqlConnectionManager.class);

    private MySqlConnectionManager() {
        connections = new HashMap<MySqlConnectionInfo,Connection>();
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            logger.error("Unable to find com.mysql.jdbc.Driver");
        }
    }

    static MySqlConnectionManager getConnectionManager() {
        if (singleton == null) {
            singleton = new MySqlConnectionManager();
        }
        return singleton;
    }

    public void shutdown() {
        Driver driver = null;
        for (Connection conn : connections.values()) {
            try {
                if (driver == null) {
                    String driverName = conn.getMetaData().getDriverName();
                    driver = DriverManager.getDriver(driverName);
                }
                conn.close();
            }catch(SQLException e) { logger.info("Problem closing connection on Shutdown ignoring...");}
        }
        // deregister driver as well
        if (driver != null) {
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException ex) {
                logger.warn("Unable to deregister MySQL Driver on  shutdown");
            }
        }
    }

    void closeConnection(MySqlConnectionInfo info) {
        Connection conn = connections.get(info);
        if (conn != null) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Closing Connection to " + info.buildURL());
                }
                conn.close();
            } catch (SQLException e) {
                logger.warn("Problem closing connection to " + info.buildURL() + " on close");
            }
        }
        connections.remove(info);
    }

    Connection getConnection (MySqlConnectionInfo info) throws SQLException {
       Connection conn = connections.get(info);
       String url = info.buildURL();
       if (conn == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Attemping connection to " + url);
            }
            conn = DriverManager.getConnection(url,info.getUser(), info.getPassword());
            if (logger.isInfoEnabled()) {
                logger.info("Successfully connected to " + url);
            }
            connections.put(info, conn);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Reusing existing connection to " + url);
            }
        }

        // check the validity of the connection
        if (!conn.isValid(0)) {
            // attempt a single reconnect here and now
            conn.close();
            conn = DriverManager.getConnection(url,info.getUser(), info.getPassword());
            connections.put(info, conn);
            logger.info("Refreshed a connection to " + url);
        }
        return conn;
    }

}
