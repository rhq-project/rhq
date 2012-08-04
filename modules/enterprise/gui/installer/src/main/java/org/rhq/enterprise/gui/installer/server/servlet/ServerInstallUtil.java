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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.setup.DBSetup;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.util.SecurityUtil;
import org.rhq.enterprise.gui.installer.client.shared.ServerDetails;
import org.rhq.enterprise.gui.installer.client.shared.ServerProperties;

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

    /**
     * Determines if we are in auto-install mode. This means the properties file is
     * fully configured and the installation can begin without asking the user
     * for more input.
     *
     * @param serverProperties the full set of server properties
     *
     * @return true if we are in auto-install mode; false if the user must give us more
     *         information before we can complete the installation.
     */
    public static boolean isAutoinstallEnabled(HashMap<String, String> serverProperties) {
        String enableProp = serverProperties.get(ServerProperties.PROP_AUTOINSTALL_ENABLE);
        if (enableProp != null) {
            return Boolean.parseBoolean(enableProp);
        }
        return false;
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
                + "SELECT s.address, s.port, s.secure_port " //
                + "  FROM rhq_server s " //
                + " WHERE s.name = ?");
            stm.setString(1, serverName.trim());

            rs = stm.executeQuery();

            if (rs.next()) {
                result = new ServerDetails(serverName, rs.getString(1), rs.getInt(2), rs.getInt(3));
            }
        } catch (SQLException e) {
            LOG.info("Unable to get server details for server [" + serverName + "]: " + e.getMessage());
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
    public static String obfuscatePassword(String password) {

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
    public static String deobfuscatePassword(String obfuscatedPassword) {

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

    /**
     * Stores the server details (such as the public endpoint) in the database. If the server definition already
     * exists, it will be updated; otherwise, a new server will be added to the HA cloud.
     *
     * @param serverProperties the server properties
     * @param password clear text password to connect to the database
     * @param serverDetails the details of the server to put into the database
     * @throws Exception
     */
    public static void storeServerDetails(HashMap<String, String> serverProperties, String password,
        ServerDetails serverDetails) throws Exception {

        DatabaseType db = null;
        Connection conn = null;

        try {
            String dbUrl = serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
            String userName = serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME);
            conn = getDatabaseConnection(dbUrl, userName, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            updateOrInsertServer(db, conn, serverDetails);

        } catch (SQLException e) {
            LOG.info("Unable to store server entry in the database: " + ThrowableUtil.getAllMessages(e));
        } finally {
            if (null != db) {
                db.closeConnection(conn);
            }
        }
    }

    private static void updateOrInsertServer(DatabaseType db, Connection conn, ServerDetails serverDetails) {
        PreparedStatement stm = null;
        ResultSet rs = null;

        if (null == serverDetails || isEmpty(serverDetails.getName())) {
            return;
        }

        try {
            stm = conn.prepareStatement("UPDATE rhq_server SET address=?, port=?, secure_port=? WHERE name=?");
            stm.setString(1, serverDetails.getEndpointAddress());
            stm.setInt(2, serverDetails.getEndpointPort());
            stm.setInt(3, serverDetails.getEndpointSecurePort());
            stm.setString(4, serverDetails.getName());
            if (0 == stm.executeUpdate()) {
                stm.close();

                // set all new servers to operation_mode=INSTALLED
                int i = 1;
                if (db instanceof PostgresqlDatabaseType || db instanceof OracleDatabaseType) {
                    stm = conn.prepareStatement("INSERT INTO rhq_server " //
                        + " ( id, name, address, port, secure_port, ctime, mtime, operation_mode, compute_power ) " //
                        + "VALUES ( ?, ?, ?, ?, ?, ?, ?, 'INSTALLED', 1 )");
                    stm.setInt(i++, db.getNextSequenceValue(conn, "rhq_server", "id"));
                } else {
                    throw new IllegalArgumentException("Unknown database type, can't continue: " + db);
                }

                stm.setString(i++, serverDetails.getName());
                stm.setString(i++, serverDetails.getEndpointAddress());
                stm.setInt(i++, serverDetails.getEndpointPort());
                stm.setInt(i++, serverDetails.getEndpointSecurePort());
                long now = System.currentTimeMillis();
                stm.setLong(i++, now);
                stm.setLong(i++, now);
                stm.executeUpdate();
            }

        } catch (SQLException e) {
            LOG.info("Unable to put the server details in the database: " + ThrowableUtil.getAllMessages(e));
        } finally {
            if (null != db) {
                db.closeResultSet(rs);
                db.closeStatement(stm);
            }
        }
    }

    /**
     * This will create the database schema in the database. <code>props</code> define the connection to the database -
     *
     * <p>Note that if the {@link #isDatabaseSchemaExist(Properties) schema already exists}, it will be purged of all
     * data/tables and recreated.</p>
     *
     * @param props the full set of server properties
     * @param serverDetails additional information about the server being installed
     * @param password the database password in clear text
     * @param logDir a directory where the db schema upgrade logs can be written
     *
     * @throws Exception if failed to create the new schema for some reason
     */
    public static void createNewDatabaseSchema(HashMap<String, String> props, ServerDetails serverDetails,
        String password, String logDir)
        throws Exception {
        String dbUrl = props.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        String userName = props.get(ServerProperties.PROP_DATABASE_USERNAME);

        try {
            // extract the dbsetup files which are located in the dbutils jar
            String dbsetupSchemaXmlFile = extractDatabaseXmlFile("db-schema-combined.xml", props, serverDetails, logDir);
            String dbsetupDataXmlFile = extractDatabaseXmlFile("db-data-combined.xml", props, serverDetails, logDir);

            // first uninstall any old existing schema, then create the tables then insert the data
            DBSetup dbsetup = new DBSetup(dbUrl, userName, password);
            dbsetup.uninstall(dbsetupSchemaXmlFile);
            dbsetup.setup(dbsetupSchemaXmlFile);
            dbsetup.setup(dbsetupDataXmlFile, null, true, false);
        } catch (Exception e) {
            LOG.fatal("Cannot install the database schema - the server will not run properly.", e);
            throw e;
        }

        return;
    }

    /**
     * This will update an existing database schema so it can be upgraded to the latest schema version.
     *
     * <p>Note that if the {@link #isDatabaseSchemaExist(Properties) schema does not already exist}, errors will
     * occur.</p>
     *
     * @param props the full set of server properties
     * @param serverDetails additional information about the server being installed
     * @param password the database password in clear text
     * @param logDir a directory where the db schema upgrade logs can be written
     *
     * @throws Exception if the upgrade failed for some reason
     */
    public static void upgradeExistingDatabaseSchema(HashMap<String, String> props, ServerDetails serverDetails,
        String password, String logDir)
        throws Exception {
        String dbUrl = props.get(ServerProperties.PROP_DATABASE_CONNECTION_URL);
        String userName = props.get(ServerProperties.PROP_DATABASE_USERNAME);

        File logfile = new File(logDir, "rhq-installer-dbupgrade.log");

        logfile.delete(); // do not keep logs from previous dbupgrade runs

        try {
            // extract the dbupgrade ANT script which is located in the dbutils jar
            String dbupgradeXmlFile = extractDatabaseXmlFile("db-upgrade.xml", props, serverDetails, logDir);

            Properties antProps = new Properties();
            antProps.setProperty("jdbc.url", dbUrl);
            antProps.setProperty("jdbc.user", userName);
            antProps.setProperty("jdbc.password", password);
            antProps.setProperty("target.schema.version", "LATEST");

            startAnt(new File(dbupgradeXmlFile), "db-ant-tasks.properties", antProps, logfile);
        } catch (Exception e) {
            LOG.fatal("Cannot upgrade the database schema - the server will not run properly.", e);
            throw e;
        }

        return;
    }

    /**
     * Given a server property value string, returns true if it is not specified.
     *
     * @param s the property string value
     *
     * @return true if it is null or empty
     */
    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    /**
     * Takes the named XML file from the classloader and writes the file to the log directory. This is meant to extract
     * the schema/data xml files from the dbutils jar file. It can also be used to extract the db upgrade XML file.
     *
     * @param xmlFileName the name of the XML file, as found in the classloader
     * @param props properties whose values are used to replace the replacement strings found in the XML file
     * @param serverDetails additional information about the server being installed
     * @param logDir a directory where the db schema upgrade logs can be written
     *
     * @return the absolute path to the extracted file
     *
     * @throws IOException if failed to extract the file to the log directory
     */
    private static String extractDatabaseXmlFile(String xmlFileName, HashMap<String, String> props,
        ServerDetails serverDetails, String logDir)
        throws IOException {

        // first slurp the file contents in memory
        InputStream resourceInStream = ServerInstallUtil.class.getClassLoader().getResourceAsStream(xmlFileName);
        ByteArrayOutputStream contentOutStream = new ByteArrayOutputStream();
        StreamUtil.copy(resourceInStream, contentOutStream);

        // now replace their replacement strings with values from the properties
        String emailFromAddress = props.get(ServerProperties.PROP_EMAIL_FROM_ADDRESS);
        if (isEmpty(emailFromAddress)) {
            emailFromAddress = "rhqadmin@localhost";
        }

        String httpPort = props.get(ServerProperties.PROP_WEB_HTTP_PORT);
        if (isEmpty(httpPort)) {
            httpPort = String.valueOf(ServerDetails.DEFAULT_ENDPOINT_PORT);
        }

        String publicEndpoint = serverDetails.getEndpointAddress();
        if (isEmpty(publicEndpoint)) {
            try {
                publicEndpoint = props.get(ServerProperties.PROP_JBOSS_BIND_ADDRESS);
                if (isEmpty(publicEndpoint) || ("0.0.0.0".equals(publicEndpoint))) {
                    publicEndpoint = InetAddress.getLocalHost().getHostAddress();
                }
            } catch (Exception e) {
                publicEndpoint = "127.0.0.1";
            }
        }

        String content = contentOutStream.toString();
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_DATA@@@", "DEFAULT");
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_INDEX@@@", "DEFAULT");
        content = content.replaceAll("@@@ADMINUSERNAME@@@", "rhqadmin");
        content = content.replaceAll("@@@ADMINPASSWORD@@@", "x1XwrxKuPvYUILiOnOZTLg=="); // rhqadmin
        content = content.replaceAll("@@@ADMINEMAIL@@@", emailFromAddress);
        content = content.replaceAll("@@@BASEURL@@@", "http://" + publicEndpoint + ":" + httpPort + "/");
        content = content.replaceAll("@@@JAASPROVIDER@@@", "JDBC");
        content = content.replaceAll("@@@LDAPURL@@@", "ldap://localhost/");
        content = content.replaceAll("@@@LDAPPROTOCOL@@@", "");
        content = content.replaceAll("@@@LDAPLOGINPROP@@@", "cn");
        content = content.replaceAll("@@@LDAPBASEDN@@@", "o=JBoss,c=US");
        content = content.replaceAll("@@@LDAPSEARCHFILTER@@@", "");
        content = content.replaceAll("@@@LDAPBINDDN@@@", "");
        content = content.replaceAll("@@@LDAPBINDPW@@@", "");
        content = content.replaceAll("@@@MULTICAST_ADDR@@@", "");
        content = content.replaceAll("@@@MULTICAST_PORT@@@", "");

        // we now have the finished XML content - write out the file to the log directory
        File xmlFile = new File(logDir, xmlFileName);
        FileOutputStream xmlFileOutStream = new FileOutputStream(xmlFile);
        ByteArrayInputStream contentInStream = new ByteArrayInputStream(content.getBytes());
        StreamUtil.copy(contentInStream, xmlFileOutStream);

        return xmlFile.getAbsolutePath();
    }

    /**
     * Launches ANT and runs the default target in the given build file.
     *
     * @param  buildFile      the build file that ANT will run
     * @param  customTaskDefs the properties file found in classloader that contains all the taskdef definitions
     * @param  properties     set of properties to set for the ANT task to access
     * @param  logFile        where ANT messages will be logged (in addition to the app server's log file)
     *
     * @throws RuntimeException
     */
    private static void startAnt(File buildFile, String customTaskDefs, Properties properties, File logFile) {
        PrintWriter logFileOutput = null;

        try {
            logFileOutput = new PrintWriter(new FileOutputStream(logFile));

            ClassLoader classLoader = ServerInstallUtil.class.getClassLoader();

            Properties taskDefs = new Properties();
            InputStream taskDefsStream = classLoader.getResourceAsStream(customTaskDefs);
            try {
                taskDefs.load(taskDefsStream);
            } finally {
                taskDefsStream.close();
            }

            Project project = new Project();
            project.setCoreLoader(classLoader);
            project.init();

            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                project.setProperty(property.getKey().toString(), property.getValue().toString());
            }

            // notice we add our listener after we set the properties - we do not want the password to be in the log file
            // our dbupgrade script will echo the property settings, so we can still get the other values
            project.addBuildListener(new LoggerAntBuildListener(logFileOutput));

            for (Map.Entry<Object, Object> taskDef : taskDefs.entrySet()) {
                project.addTaskDefinition(taskDef.getKey().toString(),
                    Class.forName(taskDef.getValue().toString(), true, classLoader));
            }

            new ProjectHelper2().parse(project, buildFile);
            project.executeTarget(project.getDefaultTarget());

        } catch (Exception e) {
            throw new RuntimeException("Cannot run ANT on script [" + buildFile + "]. Cause: " + e, e);
        } finally {
            if (logFileOutput != null) {
                logFileOutput.close();
            }
        }
    }

    /**
     * Creates a keystore whose cert has a CN of this server's public endpoint address.
     * 
     * @param serverDetails details of the server being installed
     * @param configDirStr location of a configuration directory where the keystore is to be stored
     */
    public static void createKeystore(ServerDetails serverDetails, String configDirStr) {
        File confDir = new File(configDirStr);
        File keystore = new File(confDir, "rhq.keystore");
        File keystoreBackup = new File(confDir, "rhq.keystore.backup");

        // if there is one out-of-box, we want to remove it and create one with our proper CN
        if (keystore.exists()) {
            keystoreBackup.delete();
            if (!keystore.renameTo(keystoreBackup)) {
                LOG.warn("Cannot backup existing keystore - cannot generate a new cert with a proper domain name. ["
                    + keystore + "] will be the keystore used by this server");
                return;
            }
        }

        try {
            String keystorePath = keystore.getAbsolutePath();
            String keyAlias = "RHQ";
            String domainName = "CN=" + serverDetails.getEndpointAddress() + ", OU=RHQ, O=rhq-project.org, C=US";
            String keystorePassword = "RHQManagement";
            String keyPassword = keystorePassword;
            String keyAlgorithm = "rsa";
            int validity = 7300;
            SecurityUtil.createKeyStore(keystorePath, keyAlias, domainName, keystorePassword, keyPassword,
                keyAlgorithm, validity);
            LOG.info("New keystore created [" + keystorePath + "] with cert domain name of [" + domainName + "]");
        } catch (Exception e) {
            LOG.warn("Could not generate a new cert with a proper domain name, will use the original keystore");
            keystore.delete();
            if (!keystoreBackup.renameTo(keystore)) {
                LOG.warn("Failed to restore the original keystore from backup - please rename [" + keystoreBackup
                    + "] to [" + keystore + "]");
            }
        }
    }
}
