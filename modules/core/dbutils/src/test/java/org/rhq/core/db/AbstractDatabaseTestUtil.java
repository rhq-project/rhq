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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

/**
 * Provides some methods that database tests can use - create a database unit test class and have it extend this class.
 *
 * @author John Mazzitelli
 *
 */
public class AbstractDatabaseTestUtil {
    private static final String PROPERTY_PREFIX = "test.";
    private Properties databaseConnectionInfo;
    private Connection databaseConnection;

    /**
     * Loads in the test .properties file that contains the JDBC connection information to our test databases.
     *
     * @throws Exception
     */
    @BeforeClass
    protected void loadTestDatabaseConnectionInfo() throws Exception {
        Properties default_info = new Properties();
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("test-databases.properties");
        default_info.load(is);

        databaseConnectionInfo = new Properties(default_info);

        // any properties found in the system properties will override the defaults
        Properties overrides = System.getProperties();
        for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
            String prop_name = entry.getKey().toString();

            if (prop_name.toString().startsWith(PROPERTY_PREFIX)) {
                String prop_value = entry.getValue().toString();
                databaseConnectionInfo.setProperty(prop_name, prop_value);
            }
        }

        return;
    }

    /**
     * If a test opened a connection, close it.
     */
    @AfterMethod
    protected void closeConnection() {
        if (databaseConnection != null) {
            try {
                if (!databaseConnection.isClosed()) {
                    databaseConnection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace(System.err);
            } finally {
                databaseConnection = null;
            }
        }

        return;
    }

    /**
     * Gets connection to the test postgresql database, will be <code>null</code> if not defined.
     *
     * @return connection
     *
     * @throws Exception
     */
    protected Connection getPostgresConnection() throws Exception {
        return getConnection("postgresql");
    }

    /**
     * Gets connection to the test postgresql database of the specified version, will be <code>null</code> if not
     * defined.
     *
     * @param  version
     *
     * @return connection
     *
     * @throws Exception
     */
    protected Connection getPostgresConnection(String version) throws Exception {
        return getConnection("postgresql" + version);
    }

    /**
     * Gets connection to the test oracle database, will be <code>null</code> if not defined.
     *
     * @return connection
     *
     * @throws Exception
     */
    protected Connection getOracleConnection() throws Exception {
        return getConnection("oracle");
    }

    /**
     * Gets connection to the test oracle database of the specified version, will be <code>null</code> if not defined.
     *
     * @param  version
     *
     * @return connection
     *
     * @throws Exception
     */
    protected Connection getOracleConnection(String version) throws Exception {
        return getConnection("oracle" + version);
    }

    /**
     * Gets connection to the test database, will be <code>null</code> if not defined.
     *
     * @param  database identifies the database (the key to the test .properties file)
     *
     * @return connection
     *
     * @throws Exception
     */
    protected Connection getConnection(String database) throws Exception {
        String url = getTestDatabaseConnectionUrl(database);
        String username = getTestDatabaseConnectionUsername(database);
        String password = getTestDatabaseConnectionPassword(database);
        Connection conn = null;

        try {
            if (url == null) {
                throw new Exception("No test database named '" + database
                    + "' defined; cannot run tests that need that DB");
            }

            conn = DbUtil.getConnection(url, username, password);
        } catch (Exception e) {
            if (!Boolean.getBoolean("DatabaseTest.nofail")) {
                throw new UnsupportedOperationException(
                    (database + " is not available; set DatabaseTest.nofail system property if you want to skip the tests"),
                    e);
            }

            System.err.println(database + " is not available. DatabaseTest.nofail is set - skipping test");
        }

        // just do some simple things with the connection to make sure we can use this
        // this also crudely exercises the jdbc logging if we use the logging jdbc URL
        if (conn != null) {
            conn.getAutoCommit();
            conn.getCatalog();
            conn.getHoldability();
            conn.getMetaData();
            conn.getTransactionIsolation();
            conn.getTypeMap();
            conn.getWarnings();
        }

        return conn;
    }

    /**
     * Returns the JDBC URL to be used to connect to the given test database.
     *
     * @param  database
     *
     * @return the JDBC URL
     */
    protected String getTestDatabaseConnectionUrl(String database) {
        String str = databaseConnectionInfo.getProperty(getTestDatabasePropertyPrefix(database) + "url");
        return str;
    }

    /**
     * Returns the username to be used to connect to the given test database.
     *
     * @param  database
     *
     * @return the username
     */
    protected String getTestDatabaseConnectionUsername(String database) {
        String str = databaseConnectionInfo.getProperty(getTestDatabasePropertyPrefix(database) + "username");
        return str;
    }

    /**
     * Returns the password to be used to connect to the given test database.
     *
     * @param  database
     *
     * @return the password
     */
    protected String getTestDatabaseConnectionPassword(String database) {
        String str = databaseConnectionInfo.getProperty(getTestDatabasePropertyPrefix(database) + "password");
        return str;
    }

    /**
     * Returns the property prefix for all database connection property names for the given test database. Use this as
     * part of the property name when looking up things in {@link #databaseConnectionInfo}.
     *
     * @param  database
     *
     * @return property name prefix to identify those connection properties for the given test database.
     */
    protected String getTestDatabasePropertyPrefix(String database) {
        return PROPERTY_PREFIX + database + ".";
    }
}