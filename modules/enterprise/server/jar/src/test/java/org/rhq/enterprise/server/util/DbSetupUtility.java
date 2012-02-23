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
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.rhq.core.db.setup.DBSetup;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

/**
 * Provides utility methods for doing dbresets, dbsetups, and dbupgrades.
 *
 * @author Ian Springer
 */
public class DbSetupUtility {

    public static final String JON300_SCHEMA_VERSION = "2.116";

    private static final String DB_NAME = System.getProperty("rhq.test.ds.db-name", "rhq");
    private static final String DB_SERVER_NAME = System.getProperty("rhq.test.ds.server-name", "127.0.0.1");
    private static final String DB_CONNECTION_URL = System.getProperty("rhq.test.ds.connection-url",
        "jdbc:postgresql://" + DB_SERVER_NAME + ":5432/" + DB_NAME);
    private static final String DB_USER_NAME = System.getProperty("rhq.test.ds.user-name", "rhqadmin");
    private static final String DB_PASSWORD = System.getProperty("rhq.test.ds.password", "rhqadmin");

    private static final String BASE_RESOURCE_PATH = DbSetupUtility.class.getPackage().getName().replace('.', '/');

    public static void dbreset() throws Exception {
        System.out.println("Resetting DB at " + DB_CONNECTION_URL + "...");

        // NOTE: We do not use DBReset.performDBReset() here, since DBReset deletes the schema, which requires there to
        //       be no active connections to the DB. Liquibase.dropAll(), on the other hand, just deletes all the
        //       objects in the DB, which has no such requirement.
        String dbDriver = DatabaseFactory.getInstance().findDefaultDriver(DB_CONNECTION_URL);
        Database database = CommandLineUtils.createDatabaseObject(DbSetupUtility.class.getClassLoader(),
            DB_CONNECTION_URL, DB_USER_NAME, DB_PASSWORD, dbDriver, null, null, null);
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

        DBSetup dbsetup = new DBSetup(DB_CONNECTION_URL, DB_USER_NAME, DB_PASSWORD);
        dbsetup.setup(schemaFileResourcePath);
        dbsetup.setup(dataFileResourcePath);
    }

    public static void dbupgrade(String targetSchemaVersion) throws Exception {
        System.out.println("Upgrading RHQ DB to schema version [" + targetSchemaVersion + "]...");
        File logfile = File.createTempFile("rhq.test", "dbupgrade.log");

        try {
            // get the URL for the dbupgrade Ant script, which is located in the dbutils jar
            URL dbupgradeXmlFileUrl = DbSetupUtility.class.getClassLoader().getResource("db-upgrade.xml");

            Properties antProps = new Properties();
            antProps.setProperty("jdbc.url", DB_CONNECTION_URL);
            antProps.setProperty("jdbc.user", DB_USER_NAME);
            antProps.setProperty("jdbc.password", DB_PASSWORD);
            antProps.setProperty("target.schema.version", targetSchemaVersion);

            startAnt(dbupgradeXmlFileUrl, "db-ant-tasks.properties", antProps, logfile);
        } catch (Exception e) {
            throw new RuntimeException("Cannot upgrade the RHQ DB at URL " + DB_CONNECTION_URL + " to schema version "
                + targetSchemaVersion + ".", e);
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

}
