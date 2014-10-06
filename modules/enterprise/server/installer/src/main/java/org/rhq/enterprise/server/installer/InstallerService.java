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
package org.rhq.enterprise.server.installer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author John Mazzitelli
 */
public interface InstallerService {

    /**
     * Given a database password, this will obfuscate it. The obfuscated password
     * should be used within rhq-server.properties.
     *
     * @param clearTextPassword the password to obfuscate
     * @return the obfuscated password
     * @throws Exception
     */
    String obfuscatePassword(String clearTextPassword) throws Exception;

    /**
     * This simply logs a list of all known registered servers found in the database.
     *
     * @throws Exception
     */
    void listServers() throws Exception;

    /**
     * This simply logs a list of all known registered server and storage node versions, found in the database.
     *
     * @throws Exception
     */
    void listVersions() throws Exception;

    /**
     * This simply verifies the server configuration but doesn't perform the actual install.
     * You can use this to see if, for example, the database settings are correct or the installer
     * can successfully connect to the running AS instance where RHQ is to be installed.
     *
     * @throws AutoInstallDisabledException if the server configuration properties does not have auto-install enabled
     * @throws AlreadyInstalledException if it appears the installer was already run and the server is fully installed
     * @throws Exception some other exception that should disallow the installation from continuing
     */
    void test() throws AutoInstallDisabledException, AlreadyInstalledException, Exception;

    /**
     * Call this prior to installing to see if we are ready to install.
     * This will do some pre-install checks - if the installation should proceed, the map of server properties is returned.
     * Exceptions are thrown if the install should not proceed.
     *
     * @return properties if the caller should next call {@link #install(HashMap, ServerDetails, String)}.
     *
     * @throws AutoInstallDisabledException if the server configuration properties does not have auto-install enabled
     * @throws AlreadyInstalledException if it appears the installer was already run and the server is fully installed
     * @throws Exception some other exception that should disallow the installation from continuing
     */
    HashMap<String, String> preInstall() throws AutoInstallDisabledException, AlreadyInstalledException, Exception;

    /**
     * Use this to determine if the server has already been completely installed or not.
     * This returns a String to indicate the last installation attempt. If this returns null,
     * then the installer has not done anything yet - you need to complete the installation.
     * If an empty string is returned, it means the installer ran and it appears the application
     * has successfully installed. If a non-empty string is returned, it means the installer attempted
     * to complete the installation, but an error occurred.  When a non-empty string is returned,
     * it means that an administrator probably has to do some manual intervention because something
     * went wrong with the application deployment.
     *
     * @return indicating of the last installation attempt (see above)
     * @throws Exception if it cannot be determined if the server has been fully installed.
     */
    String getInstallationResults() throws Exception;

    /**
     * Starts the installation process. Once complete, the installer has nothing more it needs to do.
     *
     * @param serverProperties the server's settings to use. These will be persisted to
     *                         the server's .properties file.
     * @param serverDetails details on the server being installed.
     *                      If in auto-install mode, this value is ignored and can be anything.
     * @param existingSchemaOption if not in auto-install mode, this tells the installer what to do with any
     *                             existing schema. Must be one of the names of the
     *                             {@link ServerInstallUtil.ExistingSchemaOption} enum.
     *                             If in auto-install mode, this value is ignored and can be anything.
     * @throws AutoInstallDisabledException if the server configuration properties does not have auto-install enabled
     * @throws AlreadyInstalledException if it appears the installer was already run and the server is fully installed
     * @throws Exception some other exception that should disallow the installation from continuing
     */
    void install(HashMap<String, String> serverProperties, ServerDetails serverDetails, String existingSchemaOption)
        throws AutoInstallDisabledException, AlreadyInstalledException, Exception;

    /**
     * Prepares the database. This is either going to do a full clean db setup or a db upgrade
     * depending on the existingSchemaOption parameter unless autoinstall setting is on, in which case
     * the autoinstall schema setting in serverProperties parameter will tell this method what to do.
     * NOTE: if not in auto-install mode, the database password is assumed to be in clear text (i.e. unencoded).
     * If in auto-install mode, the database password must be encoded already.
     * <p/>
     * This will also create or overwrite the storage cluster schema, but it will not update an existing
     * storage cluster schema, because that is a cluster-wide operation and is handled separately. See
     * {@link #updateStorageSchema(HashMap, ServerDetails, String)}.
     *
     * @param serverProperties the server's settings to use. These will be persisted to
     *                         the server's .properties file.
     * @param serverDetails details on the server being installed.
     *                      If in auto-install mode, this value is ignored and can be anything.
     * @param existingSchemaOption if not in auto-install mode, this tells the installer what to do with any
     *                             existing schema. Must be one of the names of the
     *                             {@link ServerInstallUtil.ExistingSchemaOption} enum.
     *                             If in auto-install mode, this value is ignored and can be anything.
     * @throws Exception failed to successfully prepare the database
     */
    void prepareDatabase(HashMap<String, String> serverProperties, ServerDetails serverDetails,
        String existingSchemaOption) throws Exception;

    /**
     * Update the existing storage cluster schema.  All storage nodes must be up and running the bits associated
     * with this schema.<b/>
     * NOTE: if not in auto-install mode, the database password is assumed to be in clear text (i.e. unencoded).
     * If in auto-install mode, the database password must be encoded already.
     *
     * @param serverProperties the server's settings to use. These will be persisted to
     *                         the server's .properties file.
     * @throws Exception failed to successfully prepare the database
     */
    void updateStorageSchema(HashMap<String, String> serverProperties) throws Exception;

    /**
     * Returns a list of all registered servers in the database.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return list of all existing servers registered in the database.
     * @throws Exception
     */
    ArrayList<String> getServerNames(String connectionUrl, String username, String password) throws Exception;

    /**
     * Returns details on all servers that are registered in the database.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return the details of all known servers
     * @throws Exception
     */
    ArrayList<ServerDetails> getAllServerDetails(String connectionUrl, String username, String password)
        throws Exception;

    /**
     * Returns details on a specific server that is registered in the database.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @param serverName the name of the server whose details are to be retrieved
     * @return the details of the server or null if the server does not exist
     * @throws Exception
     */
    ServerDetails getServerDetails(String connectionUrl, String username, String password, String serverName)
        throws Exception;

    /**
     * Tests to see if there is already a schema installed.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @return <code>true</code> if there appears to be a schema installed in the database already.
     * @throws Exception
     */
    boolean isDatabaseSchemaExist(String connectionUrl, String username, String password) throws Exception;

    /**
     * Tests that the database can be connected to with the given URL and credentials.
     * @param connectionUrl
     * @param username
     * @param password
     * @return <code>null</code> if the connection succeeded; this will be an error message if failed
     * @throws Exception
     */
    String testConnection(String connectionUrl, String username, String password) throws Exception;

    /**
     * Returns the rhq-server.properties values in a map.
     * @return server properties
     * @throws Exception
     */
    HashMap<String, String> getServerProperties() throws Exception;

    /**
     * Returns the version string for the app server itself (e.g. "7.1.2.Final").
     * @return version string of app server
     * @throws Exception
     */
    String getAppServerVersion() throws Exception;

    /**
     * Returns the general type of operating system the server is running on (e.g. "Linux").
     * @return os type name
     * @throws Exception
     */
    String getOperatingSystem() throws Exception;

    class AutoInstallDisabledException extends Exception {
        private static final long serialVersionUID = 1L;

        public AutoInstallDisabledException(String msg) {
            super(msg);
        }
    }

    class AlreadyInstalledException extends Exception {
        private static final long serialVersionUID = 1L;

        public AlreadyInstalledException(String msg) {
            super(msg);
        }
    }
}
