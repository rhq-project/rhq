/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.installer.server.servlet;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.installer.client.shared.ServerDetails;

/**
 * Provides utility methods necessary to complete the server installation.
 *
 * @author John Mazzitelli
 */
public class ServerInstallUtil {
    private static final Log LOG = LogFactory.getLog(ServerInstallUtil.class);

    public enum ExistingSchemaOption {
        OVERWRITE, KEEP, SKIP
    };

    public static boolean isKeepExistingSchema(ExistingSchemaOption existingSchemaOption) {
        return ExistingSchemaOption.KEEP.name().equals(existingSchemaOption)
            || ExistingSchemaOption.SKIP.name().equals(existingSchemaOption);
    }

    /**
     * Returns <code>true</code> if the database already has the database schema created for it. It will not be known
     * what version of schema or if its the latest, all this method tells you is that some RHQ database schema exists.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return <code>true</code> if the database can be connected to
     *
     * @throws Exception if failed to communicate with the database
     */
    public static boolean isDatabaseSchemaExist(String connectionUrl, String username, String password)
        throws Exception {

        Connection conn = getDatabaseConnection(connectionUrl, username, password);
        DatabaseType db = DatabaseTypeFactory.getDatabaseType(conn);

        try {
            return db.checkTableExists(conn, "RHQ_PRINCIPAL");
        } catch (IllegalStateException e) {
            return false;
        } finally {
            db.closeConnection(conn);
        }
    }

    /**
     * Get the list of existing servers from an existing schema.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return List of server names registered in the database. Empty list if the table does not exist or there are no entries in the table.
     *
     * @throws Exception if failed to communicate with the database
     */
    public static ArrayList<String> getServerNames(String connectionUrl, String username, String password)
        throws Exception {
        DatabaseType db = null;
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        ArrayList<String> result = new ArrayList<String>();

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            if (db.checkTableExists(conn, "rhq_server")) {

                stm = conn.createStatement();
                rs = stm.executeQuery("SELECT name FROM rhq_server ORDER BY name asc");

                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        } catch (IllegalStateException e) {
            // table does not exist
        } catch (SQLException e) {
            LOG.info("Unable to fetch existing server info: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeJDBCObjects(conn, stm, rs);
            }
        }

        return result;
    }

    /**
     * Returns information on the server as found in the database (port numbers, affinity group, etc).
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @param serverName the server whose details are to be returned
     * @return the information on the named server
     */
    public static ServerDetails getServerDetails(String connectionUrl, String username, String password,
        String serverName) {

        DatabaseType db = null;
        Connection conn = null;
        ServerDetails result = null;

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            result = getServerDetails(db, conn, serverName);

        } catch (Exception e) {
            LOG.info("Unable to get server detail: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeConnection(conn);
            }
        }

        return result;
    }

    private static ServerDetails getServerDetails(DatabaseType db, Connection conn, String serverName) {
        PreparedStatement stm = null;
        ResultSet rs = null;
        ServerDetails result = null;

        if (null == serverName) {
            return result;
        }

        try {
            stm = conn.prepareStatement("" //
                + "SELECT s.address, s.port, s.secure_port, ag.name " //
                + "  FROM rhq_server s LEFT JOIN rhq_affinity_group ag ON ag.id = s.affinity_group_id " //
                + " WHERE s.name = ?");
            stm.setString(1, serverName.trim());

            rs = stm.executeQuery();

            if (rs.next()) {
                result = new ServerDetails(serverName, rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(4));
            }

        } catch (SQLException e) {
            LOG.info("Unable to get affinity group name for server: " + e.getMessage());
        } finally {
            if (null != db) {
                db.closeResultSet(rs);
                db.closeStatement(stm);
            }
        }

        return result;
    }

    /**
     * Tests to make sure the server can be connected to with the given settings.
     * If the test is successful, <code>null</code>. If the test fails, the returned string
     * will be the error message to indicate the problem.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return error message if test failed; <code>null</code> if test succeeded
     */
    public static String testConnection(String connectionUrl, String username, String password) {

        // its possible the JDBC URL was changed, clear the factory cache in case the DB version is different now
        DatabaseTypeFactory.clearDatabaseTypeCache();

        try {
            ensureDatabaseIsSupported(connectionUrl, username, password);
            return null;
        } catch (Exception e) {
            LOG.warn("Installer failed to test connection", e);
            return ThrowableUtil.getAllMessages(e);
        }
    }

    /**
     * Call this when you need to confirm that the database is supported.
     *
     * @param connectionUrl
     * @param username
     * @param password
     *
     * @throws Exception if the database is not supported
     */
    public static void ensureDatabaseIsSupported(String connectionUrl, String username, String password)
        throws Exception {
        Connection conn = null;
        DatabaseType db = null;

        try {
            conn = getDatabaseConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            String version = db.getVersion();

            if (DatabaseTypeFactory.isPostgres(db)) {
                if (version.startsWith("7") || version.equals("8") || version.startsWith("8.0")
                    || version.startsWith("8.1")) {
                    throw new Exception("Unsupported PostgreSQL [" + db + "]");
                }
            } else if (DatabaseTypeFactory.isOracle(db)) {
                if (version.startsWith("8") || version.startsWith("9")) {
                    throw new Exception("Unsupported Oracle [" + db + "]");
                }
            } else {
                throw new Exception("Unsupported DB [" + db + "]");
            }

            LOG.info("Database is supported: " + db);
        } finally {
            if (db != null) {
                db.closeConnection(conn);
            }
        }

        return;
    }

    /**
     * Returns a database connection with the given set of properties providing the settings that allow for a successful
     * database connection. If <code>props</code> is <code>null</code>, it will use the server properties from
     * {@link #getServerProperties()}.
     *
     * @param connectionUrl 
     * @param userName 
     * @param password 
     * @return the database connection
     *
     * @throws SQLException if cannot successfully connect to the database
     */
    public static Connection getDatabaseConnection(String connectionUrl, String userName, String password)
        throws SQLException {
        return DbUtil.getConnection(connectionUrl, userName, password);
    }

    /**
     * Use the internal JBossAS mechanism to obfuscate a password. This is not true encryption.
     *
     * @param password the clear text of the password to obfuscate
     * @return the obfuscated password
     */
    private static String obfuscatePassword(String password) {

        // We need to do some mumbo jumbo, as the interesting method is private
        // in SecureIdentityLoginModule

        try {
            String className = "org.picketbox.datasource.security.SecureIdentityLoginModule";
            Class<?> clazz = Class.forName(className);
            Object object = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("encode", String.class);
            method.setAccessible(true);
            String result = method.invoke(object, password).toString();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("obfuscating db password failed: ", e);
        }
    }

    /**
     * Use the internal JBossAS mechanism to de-obfuscate a password back to its
     * clear text form. This is not true encryption.
     *
     * @param obfuscatedPasswordd the obfuscated password
     * @return the clear-text password
     */
    private static String deobfuscatePassword(String obfuscatedPassword) {

        // We need to do some mumbo jumbo, as the interesting method is private
        // in SecureIdentityLoginModule

        try {
            String className = "org.picketbox.datasource.security.SecureIdentityLoginModule";
            Class<?> clazz = Class.forName(className);
            Object object = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("decode", String.class);
            method.setAccessible(true);
            char[] result = (char[]) method.invoke(object, obfuscatedPassword);
            return new String(result);
        } catch (Exception e) {
            throw new RuntimeException("de-obfuscating db password failed: ", e);
        }
    }
}
