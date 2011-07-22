/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.installer;

import org.rhq.core.db.DbUtil;
import org.rhq.core.db.setup.DBSetup;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

/**
 * The tests in this class exercise the dbsetup/dbupgrade code that is run in the installer. The tests currently only
 * run against postgresql. Support needs to be added for oracle. The tests do not do any post-install/upgrade
 * verification at the time of this writing. They just simply exercise the dbsetup/dbupgrade scripts to ensure that
 * they do not contain any errors.
 *
 * @author John Sanda
 */
public class DBInstallatonTest {

    private final String LOG_DIRECTORY = System.getProperty("java.io.tmpdir", "rhq/installer-test");
    private final String TEST_DB = System.getProperty("rhq.ds.db-name","rhq_installer_test_db");
    private final String USERNAME = System.getProperty("rhq.ds.user-name", "rhqadmin");
    private final String PASSWORD = System.getProperty("rhq.ds.password", "rhqadmin");
    private final String SERVER = System.getProperty("rhq.ds.server-name", "127.0.0.1");
    private final String DB_URL = System.getProperty("rhq.ds.connection-url", "jdbc:postgresql://" + SERVER + ":5432/"+TEST_DB);
    private final String ADMIN_USERNAME = System.getProperty("rhq.db.admin.username", "postgres");
    private final String ADMIN_PASSWORD = System.getProperty("rhq.db.admin.password", "postgres");

    private ServerInformation installer;

    @BeforeMethod
    public void prepareForInstallation() throws Exception {
        initLogDirectory();
        recreateTestDatabase();

        installer = new ServerInformation();
        installer.setLogDirectory(new File(LOG_DIRECTORY));
    }

    @Test
    public void overwriteJON230Schema() throws Exception {
        installSchemaAndData("2.3.0");
        installer.createNewDatabaseSchema(getInstallProperties());
    }

    @Test
    public void upgradeJON230Schema() throws Exception {
        installSchemaAndData("2.3.0");
        installer.upgradeExistingDatabaseSchema(getInstallProperties());
    }

    @Test
    public void overwriteJON231Schema() throws Exception {
            installSchemaAndData("2.3.1");
            installer.createNewDatabaseSchema(getInstallProperties());
    }

    @Test
    public void upgradeJON231Schema() throws Exception {
        installSchemaAndData("2.3.1");
        installer.upgradeExistingDatabaseSchema(getInstallProperties());
    }
    
    @Test
    public void overwriteJON240Schema() throws Exception {
            installSchemaAndData("2.4.0");
            installer.createNewDatabaseSchema(getInstallProperties());
    }
    
    @Test
    public void upgradeJON240Schema() throws Exception {
        installSchemaAndData("2.4.0");
        installer.upgradeExistingDatabaseSchema(getInstallProperties());
    }

    private void initLogDirectory() {
        File logDir = new File(LOG_DIRECTORY);
        if (logDir.exists()) {
            logDir.delete();
        }
        logDir.mkdirs();
    }

    private void recreateTestDatabase() throws Exception {
        Connection connection = null;
        Statement dropDB = null;
        Statement createDB = null;

        try {
            connection = DbUtil.getConnection(DB_URL.replace(TEST_DB,"postgres"), ADMIN_USERNAME, ADMIN_PASSWORD);
            
            dropDB = connection.createStatement();
            dropDB.execute("drop database if exists " + TEST_DB);
            
            createDB = connection.createStatement();
            createDB.execute("create database " + TEST_DB + " with owner " + USERNAME);
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (dropDB != null) {
                dropDB.close();
            }
            if (createDB != null) {
                createDB.close();
            }
        }
    }

    private void installSchemaAndData(String jonVersion) throws Exception {
        DBSetup dbsetup = new DBSetup(DB_URL, USERNAME, PASSWORD);
        dbsetup.setup(getSchemaFile(jonVersion).getAbsolutePath());
        dbsetup.setup(getDataFile(jonVersion).getAbsolutePath());
    }

    private File getSchemaFile(String version) throws Exception {
        URL url = getClass().getResource("db-schema-combined-" + version + ".xml");

        if (url == null) {
            throw new RuntimeException("Failed to find schema file for version " + version);
        }

        return new File(url.toURI().getPath());
    }

    private File getDataFile(String version) throws Exception {
        URL url = getClass().getResource("db-data-combined-" + version + ".xml");

        if (url == null) {
            throw new RuntimeException("Failed to find data file for version " + version);
        }

        return new File(url.toURI().getPath());
    }

    private Properties getInstallProperties() {
        Properties dbProperties = new Properties();
        dbProperties.put(ServerProperties.PROP_DATABASE_CONNECTION_URL, DB_URL);
        dbProperties.put(ServerProperties.PROP_DATABASE_USERNAME, USERNAME);
        dbProperties.put(ServerProperties.PROP_DATABASE_PASSWORD, PASSWORD);
        dbProperties.put(ServerProperties.PROP_EMAIL_FROM_ADDRESS, "rhqadmin@localhost.com");
        return dbProperties;
    }
}
