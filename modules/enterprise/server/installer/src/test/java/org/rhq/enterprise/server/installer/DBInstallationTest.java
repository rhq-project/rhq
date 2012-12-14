/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.installer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.db.reset.DBReset;
import org.rhq.core.db.setup.DBSetup;
import org.rhq.core.util.stream.StreamUtil;

/**
 * The tests in this class exercise the dbsetup/dbupgrade code that is run in the installer. The tests currently only
 * run against postgresql. Support needs to be added for oracle. The tests do not do any post-install/upgrade
 * verification at the time of this writing. They just simply exercise the dbsetup/dbupgrade scripts to ensure that
 * they do not contain any errors.
 *
 * @author John Sanda
 */
public class DBInstallationTest {

    private final String LOG_DIRECTORY = System.getProperty("java.io.tmpdir", "rhq/installer-test");
    private final String DB_NAME = System.getProperty("rhq.test.ds.db-name", "rhq_installer_test_db");
    private final String USERNAME = System.getProperty("rhq.test.ds.user-name", "rhqadmin");
    private final String PASSWORD = System.getProperty("rhq.test.ds.password", "rhqadmin");
    private final String SERVER = System.getProperty("rhq.test.ds.server-name", "127.0.0.1");
    private final String DB_URL = System.getProperty("rhq.test.ds.connection-url", "jdbc:postgresql://" + SERVER
        + ":5432/" + DB_NAME);
    private final String ADMIN_USERNAME = System.getProperty("rhq.db.admin.username", "postgres");
    private final String ADMIN_PASSWORD = System.getProperty("rhq.db.admin.password", "postgres");
    private static final String DB_TYPE_MAPPING = System.getProperty("rhq.test.ds.type-mapping", "PostgreSQL");

    private String logDir;
    private ServerDetails serverDetails;

    @BeforeMethod
    public void prepareForInstallation() throws Exception {
        serverDetails = new ServerDetails("testserver", "127.0.0.1", 7080, 7443);
        initLogDirectory();
        recreateTestDatabase();
    }

    @AfterMethod
    public void afterInstallation() throws Exception {
        recreateTestDatabase();
        ServerInstallUtil.createNewDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }

    @Test
    public void overwriteJON240Schema() throws Exception {
        installSchemaAndData("2.4.0");
        ServerInstallUtil.createNewDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }

    @Test
    public void upgradeJON240Schema() throws Exception {
        installSchemaAndData("2.4.0");
        ServerInstallUtil.upgradeExistingDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }

    @Test
    public void overwriteJON300Schema() throws Exception {
        installSchemaAndData("3.0.0");
        ServerInstallUtil.createNewDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }

    @Test
    public void upgradeJON300Schema() throws Exception {
        installSchemaAndData("3.0.0");
        ServerInstallUtil.upgradeExistingDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }

    @Test
    public void upgradeRHQ420Schema() throws Exception {
        installSchemaAndData("rhq-4.2.0");
        ServerInstallUtil.upgradeExistingDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }

    @Test
    public void upgradeRHQ430Schema() throws Exception {
        installSchemaAndData("rhq-4.3.0");
        ServerInstallUtil.upgradeExistingDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }
    
    @Test
    public void upgradeRHQ440Schema() throws Exception {
        installSchemaAndData("rhq-4.4.0");
        ServerInstallUtil.upgradeExistingDatabaseSchema(getInstallProperties(), serverDetails, PASSWORD, logDir);
    }

    private void initLogDirectory() {
        File logDirFile = new File(LOG_DIRECTORY);
        if (logDirFile.exists()) {
            logDirFile.delete();
        }
        logDirFile.mkdirs();
        this.logDir = logDirFile.getAbsolutePath();
    }

    private void recreateTestDatabase() throws Exception {
        DBReset dbReset = new DBReset();
        dbReset.performDBReset(DB_TYPE_MAPPING, DB_URL, DB_NAME, USERNAME, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private void installSchemaAndData(String jonVersion) throws Exception {
        DBSetup dbsetup = new DBSetup(DB_URL, USERNAME, PASSWORD);
        File schemaFile = getSchemaFile(jonVersion);
        dbsetup.setup(schemaFile.getAbsolutePath());
        File dataFile = getDataFile(jonVersion);
        dbsetup.setup(dataFile.getAbsolutePath());
    }

    private File getSchemaFile(String version) throws Exception {
        URL url = getClass().getResource("db-schema-combined-" + version + ".xml");

        if (url == null) {
            throw new RuntimeException("Failed to find schema file for version " + version);
        }

        File file = new File(url.toURI().getPath());
        File filteredFile = filterXmlFile(file, getInstallProperties());

        return filteredFile;
    }

    private File getDataFile(String version) throws Exception {
        URL url = getClass().getResource("db-data-combined-" + version + ".xml");

        if (url == null) {
            throw new RuntimeException("Failed to find data file for version " + version);
        }

        File file = new File(url.toURI().getPath());
        File filteredFile = filterXmlFile(file, getInstallProperties());

        return filteredFile;
    }

    private HashMap<String, String> getInstallProperties() {
        HashMap<String, String> dbProperties = new HashMap<String, String>();
        dbProperties.put(ServerProperties.PROP_DATABASE_CONNECTION_URL, DB_URL);
        dbProperties.put(ServerProperties.PROP_DATABASE_USERNAME, USERNAME);
        dbProperties.put(ServerProperties.PROP_DATABASE_PASSWORD, PASSWORD);
        dbProperties.put(ServerProperties.PROP_EMAIL_FROM_ADDRESS, "rhqadmin@localhost.com");
        return dbProperties;
    }
    
    private static File filterXmlFile(File xmlFile, HashMap<String, String> props) throws IOException {
        // first slurp the file contents in memory
        InputStream fileInStream = new FileInputStream(xmlFile);
        ByteArrayOutputStream contentOutStream = new ByteArrayOutputStream();
        StreamUtil.copy(fileInStream, contentOutStream);

        // now replace their replacement strings with values from the properties
        String content = contentOutStream.toString();
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_DATA@@@", "DEFAULT");
        content = content.replaceAll("@@@LARGE_TABLESPACE_FOR_INDEX@@@", "DEFAULT");
        content = content.replaceAll("@@@ADMINUSERNAME@@@", "rhqadmin");
        content = content.replaceAll("@@@ADMINPASSWORD@@@", "x1XwrxKuPvYUILiOnOZTLg=="); // rhqadmin
        content = content.replaceAll("@@@ADMINEMAIL@@@", "rhqadmin@localhost");
        content = content.replaceAll("@@@BASEURL@@@", "http://localhost:7080/");
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

        // we now have the finished XML content - write out the file to the tmp directory
        File filteredXmlFile = File.createTempFile("rhq", xmlFile.getName());
        FileOutputStream xmlFileOutStream = new FileOutputStream(filteredXmlFile);
        ByteArrayInputStream contentInStream = new ByteArrayInputStream(content.getBytes());
        StreamUtil.copy(contentInStream, xmlFileOutStream);

        return filteredXmlFile.getAbsoluteFile();
    }

}
