/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;

import org.rhq.core.db.setup.DBSetup;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Provides utility methods for doing dbresets, dbsetups, and dbupgrades.
 *
 * @author Ian Springer
 */
public class DbSetupUtility {

    public static final String JON300_SCHEMA_VERSION = "2.116";

    private static final String BASE_RESOURCE_PATH = DbSetupUtility.class.getPackage().getName().replace('.', '/');

    private static TestDatasourceConfiguration testDsConfig;

    private static TestDatasourceConfiguration getTestDatasourceConfiguration() {
        if (testDsConfig == null) {
            Properties testDsProperties = new Properties();
            InputStream resourceAsStream = DbSetupUtility.class.getResourceAsStream("test-ds.properties");
            try {
                testDsProperties.load(resourceAsStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load test-ds.properties from classloader.", e);
            }
            testDsConfig = new TestDatasourceConfiguration(testDsProperties);
            System.out.println("Using test datasource with config " + testDsConfig + "...");
        }
        return testDsConfig;
    }
    
    public static void dbreset() throws Exception {
        TestDatasourceConfiguration testDs = getTestDatasourceConfiguration();
        System.out.println("Resetting DB at " + testDs.connectionUrl + "...");

        // NOTE: We do not use DBReset.performDBReset() here, since DBReset deletes the schema, which requires there to
        //       be no active connections to the DB. Liquibase.dropAll(), on the other hand, just deletes all the
        //       objects in the DB, which has no such requirement.
        String dbDriver = DatabaseFactory.getInstance().findDefaultDriver(testDs.connectionUrl);
        Database database = CommandLineUtils.createDatabaseObject(DbSetupUtility.class.getClassLoader(),
            testDs.connectionUrl, testDs.userName, testDs.password, dbDriver, null, null, null, null);
        //Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
        //    new JdbcConnection(AbstractEJB3Test.getConnection()));
        Liquibase liquibase = new Liquibase(null, new ClassLoaderResourceAccessor(), database);
        liquibase.dropAll();
        dropRhqCalltimeDataKeyTable(database);
    }

    public static void dbsetup() throws Exception {
        dbsetup(null);
    }

    public static void dbsetup(String jonVersion) throws Exception {
        String schemaFileResourcePath;
        String dataFileResourcePath;
        if (jonVersion != null) {
            System.out.println("Installing new RHQ DB with schema from JON version [" + jonVersion + "]...");
            schemaFileResourcePath =
                BASE_RESOURCE_PATH + "/" + "db-schema-combined-" + jonVersion + ".xml";
            dataFileResourcePath =
                            BASE_RESOURCE_PATH + "/" + "db-data-combined-" + jonVersion + ".xml";
        } else {
            System.out.println("Installing new RHQ DB with latest schema version...");
            schemaFileResourcePath = "db-schema-combined.xml";
            dataFileResourcePath = "db-data-combined.xml";
        }

        TestDatasourceConfiguration testDs = getTestDatasourceConfiguration();
        
        DBSetup dbsetup = new DBSetup(testDs.connectionUrl, testDs.userName, testDs.password);
        dbsetup.setup(schemaFileResourcePath);
        dbsetup.setup(dataFileResourcePath);
    }

    public static void dbupgrade(String targetSchemaVersion) throws Exception {
        System.out.println("Upgrading RHQ DB to schema version [" + targetSchemaVersion + "]...");
        File logfile = File.createTempFile("rhq.test", "dbupgrade.log");

        TestDatasourceConfiguration testDs = getTestDatasourceConfiguration();
        try {
            // Get the URL for the dbupgrade Ant script, which is located in the dbutils jar.
            URL dbupgradeXmlFileUrl = DbSetupUtility.class.getClassLoader().getResource("db-upgrade.xml");

            Properties antProps = new Properties();
            antProps.setProperty("jdbc.url", testDs.connectionUrl);
            antProps.setProperty("jdbc.user", testDs.userName);
            antProps.setProperty("jdbc.password", testDs.password);
            antProps.setProperty("target.schema.version", targetSchemaVersion);

            startAnt(dbupgradeXmlFileUrl, "db-ant-tasks.properties", antProps, logfile);
        } catch (Exception e) {
            throw new RuntimeException("Cannot upgrade the RHQ DB at [" + testDs.connectionUrl + "] to schema version ["
                + targetSchemaVersion + "].", e);
        }

        return;
    }

    private static void dropRhqCalltimeDataKeyTable(Database database) throws LiquibaseException {
        // For some reason Liquibase always fails to drop the rhq_calltime_data_key table, logging the following:
        //
        //    WARNING 2/22/12 4:56 PM:liquibase: Foreign key rhq_calltime_data_value_key_id_fkey references table
        //                                       rhq_calltime_data_key, which is in a different schema. Retaining FK in
        //                                       diff, but table will not be diffed.
        //
        // The workaround is to manually drop the table via JDBC once Liquibase has cleared out everything else.

        System.out.println("Dropping rhq_calltime_data_key table...");
        // NOTE: The below attempt to delete the table individually via Liquibase doesn't even work...
        /*SqlVisitor sqlVisitor = new AbstractSqlVisitor() {
            public String modifySql(String sql, Database database) {
                log.info("Liquibase executing SQL [" + sql + "]...");
                return sql;
            }

            public String getName() {
                return null;
            }
        };

        database.execute(new SqlStatement[]{new DropTableStatement(null, "rhq_calltime_data_key", false)},
            Arrays.asList(sqlVisitor));*/

        try {
            PreparedStatement statement = AbstractEJB3Test.getConnection().prepareStatement("DROP TABLE rhq_calltime_data_key");
            statement.execute();
        } catch (SQLException e) {
            // ignore
        }
    }

    /**
     * Launches Ant and runs the default target in the given build file.
     *
     * @param buildFile      the build file that Ant will run
     * @param customTaskDefs the properties file found in classloader that contains all the taskdef definitions
     * @param properties     set of properties to set for the ANT task to access
     * @param logFile        where Ant messages will be logged (in addition to the app server's log file)
     * @throws RuntimeException
     */
    private static void startAnt(URL buildFile, String customTaskDefs, Properties properties, File logFile) {
        PrintWriter logFileOutput = null;

        try {
            logFileOutput = new PrintWriter(new FileOutputStream(logFile));

            ClassLoader classLoader = DbSetupUtility.class.getClassLoader();

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
                project.addTaskDefinition(taskDef.getKey().toString(), Class.forName(taskDef.getValue().toString(),
                    true, classLoader));
            }

            new ProjectHelper2().parse(project, buildFile);
            project.executeTarget(project.getDefaultTarget());
        } catch (Exception e) {
            throw new RuntimeException("Cannot run Ant on script [" + buildFile + "] - cause: " + e, e);
        } finally {
            if (logFileOutput != null) {
                logFileOutput.close();
            }
        }
    }
    
    private static class TestDatasourceConfiguration {
        abstract class Property {
            public static final String DB_CONNECTION_URL = "rhq.test.ds.connection-url";
            public static final String DB_SERVER_NAME = "rhq.test.ds.server-name";
            public static final String DB_NAME = "rhq.test.ds.db-name";
            public static final String DB_USER_NAME = "rhq.test.ds.user-name";
            public static final String DB_PASSWORD = "rhq.test.ds.password";    
        }

        String connectionUrl;
        String serverName;
        String dbName;
        String userName;
        String password;
        
        private TestDatasourceConfiguration(Properties props) {
            connectionUrl = props.getProperty(Property.DB_CONNECTION_URL);
            serverName = props.getProperty(Property.DB_SERVER_NAME);
            dbName = props.getProperty(Property.DB_NAME);
            userName = props.getProperty(Property.DB_USER_NAME);
            password = props.getProperty(Property.DB_PASSWORD);
        }

        @Override
        public String toString() {
            // NOTE: For the sake of security, we don't include the password here.
            return "{" +
                "connectionUrl='" + connectionUrl + '\'' +
                ", serverName='" + serverName + '\'' +
                ", dbName='" + dbName + '\'' +
                ", userName='" + userName + '\'' +
                '}';
        }
    }

}
