/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.server.metrics.migrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.hibernate.ejb.Ejb3Configuration;

import org.rhq.core.util.obfuscation.PicketBoxObfuscator;
import org.rhq.server.metrics.migrator.DataMigrator.DatabaseType;
import org.rhq.server.metrics.migrator.workers.AggregateDataMigrator;
import org.rhq.server.metrics.migrator.workers.DeleteAllData;
import org.rhq.server.metrics.migrator.workers.MetricsIndexMigrator;
import org.rhq.server.metrics.migrator.workers.RawDataMigrator;


/**
 * @author Stefan Negrea
 *
 * Only postgres is supported by the runner, however the data migrator itself can run
 * with any database.
 *
 * Maven command to run this from the command line:
 *
 * mvn install -DskipTests exec:java -Dexec.mainClass="org.rhq.server.metrics.DataMigratorRunner"
 *
 *
 */
@SuppressWarnings({ "static-access", "deprecation" })
public class DataMigratorRunner {

    private static final int DEFAULT_CASSANDRA_PORT = 9142;
    private final Log log = LogFactory.getLog(DataMigratorRunner.class);

    //Cassandra
    private final Option cassandraUserOption = OptionBuilder.withLongOpt("cassandra-user").hasArg()
        .withType(String.class).withDescription("Cassandra user (default: rhqadmin)").create();
    private final Option cassandraPasswordOption = OptionBuilder.withLongOpt("cassandra-password").hasArg()
        .withDescription("Cassandra password (default: rhqadmin)").withType(String.class).create();
    private final Option cassandraHostsOption = OptionBuilder.withLongOpt("cassandra-hosts").hasArg()
        .withType(String.class).withDescription("Cassandra hosts, format host_ip_1,host_ip_2,... (default: 127.0.0.1")
        .create();
    private final Option cassandraPortOption = OptionBuilder.withLongOpt("cassandra-port").hasArg()
        .withType(Integer.class).withDescription("Cassandra native binary protocol port (default: 9142)").create();

    //SQL
    private final Option sqlUserOption = OptionBuilder.withLongOpt("sql-user").hasArg().withType(String.class)
        .withDescription("SQL server user (default: rhqadmin)").create();
    private final Option sqlPasswordOption = OptionBuilder.withLongOpt("sql-password").hasArg().withType(String.class)
        .withDescription("SQL server password (default: rhqadmin)").create();
    private final Option sqlConnectionUrlOption = OptionBuilder.withLongOpt("sql-connection-url").hasArg()
        .withType(String.class)
        .withDescription("SQL connection url. Not used by default. If specified will override host, port and db SQL options.")
        .create();
    private final Option sqlHostOption = OptionBuilder.withLongOpt("sql-host").hasArg().withType(String.class)
        .withDescription("SQL server host address (default: localhost)").create();
    private final Option sqlPortOption = OptionBuilder.withLongOpt("sql-port").hasArg().withType(String.class)
        .withDescription("SQL server port (default: 5432)").create();
    private Option sqlDBOption = OptionBuilder.withLongOpt("sql-db").hasArg().withType(String.class)
        .withDescription("SQL database (default: rhq)").create();

    private final Option sqlServerTypeOption = OptionBuilder.withLongOpt("sql-server-type").hasArg().withType(String.class)
        .withDescription("SQL server type, only postgres and oracle are supported (default: postgres)").create();
    private final Option sqlPostgresServerOption = OptionBuilder.withLongOpt("sql-server-postgres").hasOptionalArg()
        .withType(Boolean.class).withDescription("Postgres SQL server.").create();
    private final Option sqlOracleServerOption = OptionBuilder.withLongOpt("sql-server-oracle").hasOptionalArg()
        .withType(Boolean.class).withDescription("Oracle SQL server.").create();

    //Migration
    private final Option disableRawOption = OptionBuilder.withLongOpt("disable-raw-migration").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Disable raw table migration (default: false)").create();
    private final Option disable1HOption = OptionBuilder.withLongOpt("disable-1h-migration").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Disable 1 hour aggregates table migration (default: false)").create();
    private final Option disable6HOption = OptionBuilder.withLongOpt("disable-6h-migration").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Disable 6 hours aggregates table migration (default: false)").create();
    private final Option disable1DOption = OptionBuilder.withLongOpt("disable-1d-migration").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Disable 24 hours aggregates table migration (default: false)").create();
    private final Option deleteDataOption = OptionBuilder.withLongOpt("delete-data").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Delete SQL data at the end of migration (default: false)").create();
    private final Option estimateOnlyOption = OptionBuilder.withLongOpt("estimate-only").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Only estimate how long the migration will take (default: false)").create();
    private final Option deleteOnlyOption = OptionBuilder.withLongOpt("delete-only").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Only delete data from the old SQL server, no migration will be performed (default: false)")
        .create();
    private final Option experimentalExportOption = OptionBuilder
        .withLongOpt("experimental-export").hasOptionalArg().withType(Boolean.class)
        .withDescription("Enable experimental bulk export for Postgres, option ignored for Oracle migration (default: false)")
        .create();

    //Runner
    private final Option helpOption = OptionBuilder.withLongOpt("help").create("h");
    private final Option debugLogOption = OptionBuilder.withLongOpt("debugLog")
        .withDescription("Enable debug level logs for the communication with Cassandra and SQL Server (default: false)")
        .create("X");
    private final Option configFileOption = OptionBuilder.withLongOpt("config-file").hasArg()
        .withDescription("Configuration file. All the command line options can be set in a typical properties file. " +
                    "Command line arguments take precedence over default, RHQ server properties,  and configuration file options.")
        .create();
    private final Option serverPropertiesFileOption = OptionBuilder.withLongOpt("rhq-server-properties-file").hasArg()
        .withDescription("RHQ Server configuration file (rhq-server.properties). The RHQ server properties will be used for SQL server configuration. "
                +"Command line arguments take precedence over default, RHQ server properties, and configuration file options.")
        .create();

    private Map<Object, Object> configuration = new HashMap<Object, Object>();
    private Options options;

    /**
     * @param args
     * @throws ParseException
     */
    public static void main(String[] args) throws Exception {
        initLogging();
        try{
            DataMigratorRunner runner = new DataMigratorRunner();
            runner.configure(args);
            runner.run();
        } catch (HelpRequestedException h) {
            //do nothing
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    private static void initLogging() {
        Logger root = Logger.getRootLogger();
        if (!root.getAllAppenders().hasMoreElements()) {
            root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
            setLogLevel(Level.ERROR);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void setLogLevel(Level level) {
        Logger root = Logger.getRootLogger();
        root.setLevel(level);

        Logger cassandraLogging = root.getLoggerRepository().getLogger("log4j.logger.org.apache.cassandra.cql.jdbc");
        cassandraLogging.setLevel(level);

        Logger cassandraDriverLogging = root.getLoggerRepository().getLogger("com.datastax.driver");
        cassandraDriverLogging.setLevel(level);

        Logger hibernateLogging = root.getLoggerRepository().getLogger("org.hibernate");
        hibernateLogging.setLevel(Level.ERROR);

        Logger migratorLogging = root.getLoggerRepository().getLogger("org.rhq");
        if (Level.DEBUG.equals(level)) {
            migratorLogging.setLevel(Level.ALL);
        } else {
            migratorLogging.setLevel(level);
        }

        //force change some of the logger levels
        Class[] clazzes = new Class[] { DataMigratorRunner.class, DataMigrator.class, RawDataMigrator.class,
            DeleteAllData.class, AggregateDataMigrator.class, MetricsIndexMigrator.class };
        for (Class clazz : clazzes) {
            migratorLogging = root.getLogger(clazz);
            if (Level.DEBUG.equals(level)) {
                migratorLogging.setLevel(Level.ALL);
            } else {
                migratorLogging.setLevel(level);
            }
        }
    }

    private void configure(String args[]) throws Exception {
        options = new Options();

        options.addOption(cassandraUserOption);
        options.addOption(cassandraPasswordOption);
        options.addOption(cassandraHostsOption);
        options.addOption(cassandraPortOption);

        options.addOption(sqlUserOption);
        options.addOption(sqlPasswordOption);
        options.addOption(sqlHostOption);
        options.addOption(sqlPortOption);
        options.addOption(sqlDBOption);
        options.addOption(sqlServerTypeOption);
        options.addOption(sqlPostgresServerOption);
        options.addOption(sqlOracleServerOption);
        options.addOption(sqlConnectionUrlOption);

        options.addOption(disableRawOption);
        options.addOption(disable1HOption);
        options.addOption(disable6HOption);
        options.addOption(disable1DOption);
        options.addOption(deleteDataOption);
        options.addOption(estimateOnlyOption);
        options.addOption(deleteOnlyOption);
        options.addOption(experimentalExportOption);

        options.addOption(helpOption);
        options.addOption(debugLogOption);
        options.addOption(configFileOption);
        options.addOption(serverPropertiesFileOption);

        CommandLine commandLine;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(120);
            formatter.printHelp("DataMigrationRunner", options);
            throw new Exception("Error parsing command line arguments");
        }

        if (commandLine.hasOption(helpOption.getLongOpt()) || commandLine.hasOption(helpOption.getOpt())) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("DataMigrationRunner", options);
            throw new HelpRequestedException();
        }

        if (commandLine.hasOption(debugLogOption.getLongOpt()) || commandLine.hasOption(debugLogOption.getOpt())) {
            DataMigratorRunner.setLogLevel(Level.DEBUG);
        }

        loadDefaultConfiguration();

        if (commandLine.hasOption(serverPropertiesFileOption.getLongOpt())) {
            log.debug("Server configuration file option enabled. Loading server configuration from file: "
                + serverPropertiesFileOption.getLongOpt());
            loadConfigurationFromServerPropertiesFile(commandLine.getOptionValue(serverPropertiesFileOption.getLongOpt()));
            log.debug("Server configuration file from system properties will not be loaded even if set because of the manual override.");
        } else if (System.getProperty("rhq.server.properties-file") != null) {
            log.debug("Server configuration file system property detected. Loading the file: "
                + System.getProperty("rhq.server.properties-file"));
            loadConfigurationFromServerPropertiesFile(System.getProperty("rhq.server.properties-file"));
            log.debug("Server configuration file loaded based on system properties options.");
        }

        if (commandLine.hasOption(configFileOption.getLongOpt())) {
            loadConfigFile(commandLine.getOptionValue(configFileOption.getLongOpt()));
        }

        parseCassandraOptions(commandLine);
        parseSQLOptions(commandLine);
        parseMigrationOptions(commandLine);

        if (commandLine.hasOption(debugLogOption.getLongOpt()) || commandLine.hasOption(debugLogOption.getOpt())) {
            printOptions();
        }
    }

    /**
     * Add default configuration options to the configuration store.
     * @throws Exception
     */
    private void loadDefaultConfiguration() throws Exception {
        //default Cassandra configuration
        configuration.put(cassandraUserOption, "rhqadmin");
        configuration.put(cassandraPasswordOption, "rhqadmin");
        configuration.put(cassandraHostsOption, new String[] { InetAddress.getLocalHost().getHostAddress() });
        configuration.put(cassandraPortOption, DEFAULT_CASSANDRA_PORT);

        //default SQL configuration
        configuration.put(sqlUserOption, "rhqadmin");
        configuration.put(sqlPasswordOption, "rhqadmin");
        configuration.put(sqlHostOption, "localhost");
        configuration.put(sqlPortOption, "5432");
        configuration.put(sqlDBOption, "rhq");
        configuration.put(sqlServerTypeOption, DatabaseType.Postgres);

        //default runner options
        configuration.put(disableRawOption, false);
        configuration.put(disable1HOption, false);
        configuration.put(disable6HOption, false);
        configuration.put(disable1DOption, false);
        configuration.put(estimateOnlyOption, false);
        configuration.put(deleteDataOption, false);
        configuration.put(deleteOnlyOption, false);
        configuration.put(experimentalExportOption, false);
    }


    private void loadConfigurationFromServerPropertiesFile(String file) throws Exception {
        File configFile = new File(file);
        if (!configFile.exists()) {
            throw new FileNotFoundException("RHQ server properties file not found! File: " + file);
        }

        Properties serverProperties = new Properties();
        FileInputStream stream = new FileInputStream(configFile);
        serverProperties.load(stream);
        stream.close();

        //SQL options
        String dbType = serverProperties.getProperty("rhq.server.database.type-mapping");
        DatabaseType databaseType = DatabaseType.Postgres;
        if (dbType != null && dbType.toLowerCase().contains("oracle")) {
            databaseType = databaseType.Oracle;
        }

        configuration.put(sqlServerTypeOption, databaseType);
        configuration.put(sqlUserOption, serverProperties.getProperty("rhq.server.database.user-name"));
        String dbPasswordProperty = serverProperties.getProperty("rhq.server.database.password");
        configuration.put(sqlPasswordOption, PicketBoxObfuscator.decode(dbPasswordProperty));
        configuration.put(sqlConnectionUrlOption, serverProperties.getProperty("rhq.server.database.connection-url"));

        //Storage Node options
        configuration.put(cassandraUserOption, serverProperties.getProperty("rhq.storage.username"));
        String cassandraPasswordProperty = serverProperties.getProperty("rhq.storage.password");
        configuration.put(cassandraPasswordOption, PicketBoxObfuscator.decode(cassandraPasswordProperty));

        if (serverProperties.getProperty("rhq.storage.nodes") != null
            && !serverProperties.getProperty("rhq.storage.nodes").trim().isEmpty()) {
            String[] storageNodes = serverProperties.getProperty("rhq.storage.nodes").split(",");
            configuration.put(cassandraHostsOption, storageNodes);
        }

        if (serverProperties.getProperty("rhq.storage.cql-port") != null
            && !serverProperties.getProperty("rhq.storage.cql-port").trim().isEmpty()) {
            Integer cassandraPort = Integer.parseInt(serverProperties.getProperty("rhq.storage.cql-port"));
            configuration.put(cassandraPortOption, cassandraPort);
        }
    }

    /**
     * Load the configuration options from file and overlay them on top of the default
     * options.
     *
     * @param file config file
     */
    private void loadConfigFile(String file) {
        try {
            File configFile = new File(file);
            if (!configFile.exists()) {
                throw new FileNotFoundException("Configuration file not found!");
            }

            Properties configProperties = new Properties();
            FileInputStream stream = new FileInputStream(configFile);
            configProperties.load(stream);
            stream.close();

            for (Object optionObject : options.getOptions()) {
                Option option = (Option) optionObject;
                Object optionValue;

                if ((optionValue = configProperties.get(option.getLongOpt())) != null) {
                    log.debug("Configuration option loaded: " + option.getLongOpt() + " (" + option.getType() + ") -> "
                        + optionValue);

                    if (option.equals(cassandraHostsOption)) {
                        String[] cassandraHosts = parseCassandraHosts(optionValue.toString());
                        configuration.put(option, cassandraHosts);
                    } else if (option.equals(sqlServerTypeOption)) {
                        if ("oracle".equals(optionValue)) {
                            configuration.put(option, DatabaseType.Oracle);
                        } else {
                            configuration.put(option, DatabaseType.Postgres);
                        }
                    } else if (option.equals(sqlPostgresServerOption)) {
                        boolean value = tryParseBoolean(optionValue.toString(), true);
                        if (value == true) {
                            configuration.put(sqlServerTypeOption, DatabaseType.Postgres);
                        }
                    } else if (option.equals(sqlOracleServerOption)) {
                        boolean value = tryParseBoolean(optionValue.toString(), true);
                        if (value == true) {
                            configuration.put(sqlServerTypeOption, DatabaseType.Oracle);
                        }
                    } else if (option.getType().equals(Boolean.class)) {
                        configuration.put(option, tryParseBoolean(optionValue.toString(), true));
                    } else if (option.getType().equals(Integer.class)) {
                        configuration.put(option, tryParseInteger(optionValue.toString(), 0));
                    } else {
                        configuration.put(option, optionValue.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to load or process the configuration file.", e);
            System.exit(1);
        }
    }

    /**
     * Parse command line options for Cassandra.
     *
     * @param commandLine command line
     * @throws NoHostAvailableException
     */
    private void parseCassandraOptions(CommandLine commandLine) throws Exception {
        if (commandLine.hasOption(cassandraUserOption.getLongOpt())) {
            configuration.put(cassandraUserOption, commandLine.getOptionValue(cassandraUserOption.getLongOpt()));
        }

        if (commandLine.hasOption(cassandraPasswordOption.getLongOpt())) {
            configuration
                .put(cassandraPasswordOption, commandLine.getOptionValue(cassandraPasswordOption.getLongOpt()));
        }

        if (commandLine.hasOption(cassandraHostsOption.getLongOpt())) {
            String[] cassandraHosts = parseCassandraHosts(commandLine.getOptionValue(cassandraHostsOption.getLongOpt()));
            configuration.put(cassandraHostsOption, cassandraHosts);
        }

        if (commandLine.hasOption(cassandraPortOption.getLongOpt())) {
            Integer cassandraPort = tryParseInteger(commandLine.getOptionValue(cassandraPortOption.getLongOpt()),
                DEFAULT_CASSANDRA_PORT);
            configuration.put(cassandraPortOption, cassandraPort);
        }
    }

    /**
     * Parse command line options for SQL.
     *
     * @param commandLine command line
     * @throws NoHostAvailableException
     */
    private void parseSQLOptions(CommandLine commandLine) throws NoHostAvailableException {
        if (commandLine.hasOption(sqlUserOption.getLongOpt())) {
            configuration.put(sqlUserOption, commandLine.getOptionValue(sqlUserOption.getLongOpt()));
        }

        if (commandLine.hasOption(sqlPasswordOption.getLongOpt())) {
            configuration.put(sqlPasswordOption, commandLine.getOptionValue(sqlPasswordOption.getLongOpt()));
        }

        if (commandLine.hasOption(sqlHostOption.getLongOpt())) {
            configuration.put(sqlHostOption, commandLine.getOptionValue(sqlHostOption.getLongOpt()));
        }

        if (commandLine.hasOption(sqlPortOption.getLongOpt())) {
            configuration.put(sqlPortOption, commandLine.getOptionValue(sqlPortOption.getLongOpt()));
        }

        if (commandLine.hasOption(sqlDBOption.getLongOpt())) {
            configuration.put(sqlDBOption, commandLine.getOptionValue(sqlDBOption.getLongOpt()));
        }

        if (commandLine.hasOption(sqlConnectionUrlOption.getLongOpt())) {
            configuration.put(sqlConnectionUrlOption, commandLine.getOptionValue(sqlConnectionUrlOption.getLongOpt()));
        }

        if (commandLine.hasOption(sqlServerTypeOption.getLongOpt())) {
            if ("oracle".equals(commandLine.getOptionValue(sqlServerTypeOption.getLongOpt()))) {
                configuration.put(sqlServerTypeOption, DatabaseType.Oracle);
            } else {
                configuration.put(sqlServerTypeOption, DatabaseType.Postgres);
            }
        } else if (commandLine.hasOption(sqlPostgresServerOption.getLongOpt())) {
            configuration.put(sqlServerTypeOption, DatabaseType.Postgres);
        } else if (commandLine.hasOption(sqlOracleServerOption.getLongOpt())) {
            configuration.put(sqlServerTypeOption, DatabaseType.Oracle);
        }
    }

    /**
     * Parse command line options for the actual migration progress.
     *
     * @param commandLine
     */
    private void parseMigrationOptions(CommandLine commandLine) {
        boolean value;

        if (commandLine.hasOption(disableRawOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(disableRawOption.getLongOpt()), true);
            configuration.put(disableRawOption, value);
        }

        if (commandLine.hasOption(disable1HOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(disable1HOption.getLongOpt()), true);
            configuration.put(disable1HOption, value);
        }

        if (commandLine.hasOption(disable6HOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(disable6HOption.getLongOpt()), true);
            configuration.put(disable6HOption, value);
        }

        if (commandLine.hasOption(disable1DOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(disable1DOption.getLongOpt()), true);
            configuration.put(disable1DOption, value);
        }

        if (commandLine.hasOption(deleteDataOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(deleteDataOption.getLongOpt()), true);
            configuration.put(deleteDataOption, value);
        }

        if (commandLine.hasOption(deleteOnlyOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(deleteOnlyOption.getLongOpt()), true);
            configuration.put(deleteOnlyOption, value);
        }

        if (commandLine.hasOption(estimateOnlyOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(estimateOnlyOption.getLongOpt()), true);
            configuration.put(estimateOnlyOption, value);
        }

        if (commandLine.hasOption(experimentalExportOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(experimentalExportOption.getLongOpt()), true);
            configuration.put(experimentalExportOption, value);
        }
    }

    private void run() throws Exception {
        log.debug("Creating Entity Manager");
        EntityManagerFactory entityManagerFactory = this.createEntityManagerFactory();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        log.debug("Done creating Entity Manager");

        log.debug("Creating Cassandra session");
        Session cassandraSession = this.createCassandraSession();
        log.debug("Done creating Cassandra session");

        DatabaseType databaseType = DatabaseType.Postgres;
        if (configuration.get(sqlServerTypeOption) != null) {
            databaseType = (DatabaseType) configuration.get(sqlServerTypeOption);
        }

        DataMigrator migrator = new DataMigrator(entityManager, cassandraSession, databaseType, tryParseBoolean(
            configuration.get(experimentalExportOption), false));

        if (!(Boolean) configuration.get(deleteOnlyOption)) {
            if ((Boolean) configuration.get(deleteDataOption)) {
                migrator.deleteAllDataAtEndOfMigration();
            } else {
                migrator.preserveData();
            }

            migrator.runRawDataMigration(!(Boolean) configuration.get(disableRawOption));
            migrator.run1HAggregateDataMigration(!(Boolean) configuration.get(disable1HOption));
            migrator.run6HAggregateDataMigration(!(Boolean) configuration.get(disable6HOption));
            migrator.run1DAggregateDataMigration(!(Boolean) configuration.get(disable1DOption));

            System.out.println("Estimation process - starting\n");
            long estimate = migrator.estimate();
            System.out.println("The migration process will take approximately: "
                + TimeUnit.MILLISECONDS.toMinutes(estimate) + " minutes (or " + estimate + " milliseconds)\n");
            System.out.println("Estimation process - ended\n\n");

            if (!(Boolean) configuration.get(estimateOnlyOption)) {
                System.out.println("Migration process - starting\n");
                long startTime = System.currentTimeMillis();
                migrator.migrateData();
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("The migration process took: " + TimeUnit.MILLISECONDS.toMinutes(duration)
                    + " minutes (or " + duration + " milliseconds)\n");
                System.out.println("Migration process - ended\n");
            }
        } else {
            migrator.deleteAllDataAtEndOfMigration();
            migrator.runRawDataMigration(false);
            migrator.run1HAggregateDataMigration(false);
            migrator.run6HAggregateDataMigration(false);
            migrator.run1DAggregateDataMigration(false);

            System.out.println("Estimation process - starting\n");
            long estimate = migrator.estimate();
            System.out.println("The deletion of old data will take approximately: "
                + TimeUnit.MILLISECONDS.toMinutes(estimate) + " minutes (or " + estimate + " milliseconds)\n");
            System.out.println("Estimation process - ended\n\n");

            if (!(Boolean) configuration.get(estimateOnlyOption)) {
                migrator.runRawDataMigration(true);
                migrator.run1HAggregateDataMigration(true);
                migrator.run6HAggregateDataMigration(true);
                migrator.run1DAggregateDataMigration(true);

                System.out.println("Old data deletion process - starting\n");
                long startTime = System.currentTimeMillis();
                migrator.deleteOldData();
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("The deletion process took: " + TimeUnit.MILLISECONDS.toMinutes(duration)
                    + " minutes (or " + duration + " milliseconds)\n");
                System.out.println("Old data deletion process - ended\n");
            }
        }

        this.closeCassandraSession(cassandraSession);
        this.closeEntityManagerFactory(entityManagerFactory);
    }

    /**
     * Closes an existing cassandra session.
     *
     * @param session
     */
    private void closeCassandraSession(Session session) {
        session.getCluster().shutdown();
    }

    /**
     * Closes an existing entity manager factory
     *
     * @param entityManagerFactory
     */
    private void closeEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    /**
     * Create a Cassandra session based on configuration options.
     *
     * @return Cassandra session
     * @throws Exception
     */
    private Session createCassandraSession() throws Exception {
        Cluster cluster = Cluster
            .builder()
            .addContactPoints((String[]) configuration.get(cassandraHostsOption))
            .withPort((Integer) configuration.get(cassandraPortOption))
            .withCompression(Compression.NONE)
            .withoutMetrics()
            .withCredentials((String) configuration.get(cassandraUserOption),
                (String) configuration.get(cassandraPasswordOption))
            .build();

        try {
            return cluster.connect("rhq");
        } catch (Exception e) {
            log.debug("Failed to connect to the storage cluster.", e);
            cluster.shutdown();
            throw e;
        }
    }

    /**
     * Create an entity manager factory with the SQL configuration from properties.
     *
     * @return an entity manager factory
     * @throws Exception
     */
    private EntityManagerFactory createEntityManagerFactory() throws Exception {
        Properties properties = new Properties();
        properties.put("javax.persistence.provider", "org.hibernate.ejb.HibernatePersistence");
        properties.put("hibernate.connection.username", (String) configuration.get(sqlUserOption));
        properties.put("hibernate.connection.password", (String) configuration.get(sqlPasswordOption));
        properties.put("javax.persistence.query.timeout", DataMigrator.SQL_TIMEOUT);
        properties.put("hibernate.c3p0.timeout", DataMigrator.SQL_TIMEOUT);

        if (DatabaseType.Oracle.equals(configuration.get(sqlServerTypeOption))) {
            String driverClassName = "oracle.jdbc.driver.OracleDriver";

            try {
                //Required to preload the driver manually.
                //Without this the driver load will fail due to the packaging.
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                log.debug(e);
                throw new Exception("Oracle SQL Driver class could not be loaded. Missing class: " + driverClassName);
            }

            properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
            properties.put("hibernate.driver_class", driverClassName);

            if (configuration.get(sqlConnectionUrlOption) != null) {
                properties.put("hibernate.connection.url", (String) configuration.get(sqlConnectionUrlOption));
            } else {
                properties.put("hibernate.connection.url", "jdbc:oracle:thin:@" + (String) configuration.get(sqlHostOption)
                    + ":" + (String) configuration.get(sqlPortOption) + ":" + (String) configuration.get(sqlDBOption));
                properties.put("hibernate.default_schema", (String) configuration.get(sqlDBOption));
            }

            properties.put("hibernate.connection.oracle.jdbc.ReadTimeout", DataMigrator.SQL_TIMEOUT);
        } else {
            String driverClassName = "org.postgresql.Driver";

            try {
                //Required to preload the driver manually.
                //Without this the driver load will fail due to the packaging.
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                log.debug(e);
                throw new Exception("Postgres SQL Driver class could not be loaded. Missing class: " + driverClassName);
            }

            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("hibernate.driver_class", driverClassName);

            if (configuration.get(sqlConnectionUrlOption) != null) {
                properties.put("hibernate.connection.url", (String) configuration.get(sqlConnectionUrlOption));
            } else {
            properties.put("hibernate.connection.url", "jdbc:postgresql://" + (String) configuration.get(sqlHostOption)
                + ":" + (String) configuration.get(sqlPortOption) + "/" + (String) configuration.get(sqlDBOption));
            }
        }

        log.debug("Creating entity manager with the following configuration:");
        log.debug(properties);

        Ejb3Configuration configuration = new Ejb3Configuration();
        configuration.setProperties(properties);
        EntityManagerFactory factory = configuration.buildEntityManagerFactory();
        return factory;
    }


    /**
      * Print the options used to run the migration process
      */
    private void printOptions() {
        log.debug("Running migration with the following optons: ");
        for (Entry<Object, Object> configOption : this.configuration.entrySet()) {
            Option option = (Option) configOption.getKey();
            if (option.getLongOpt() != null && !option.getLongOpt().contains("pass")) {
                if (!(configOption.getValue() instanceof Object[])) {
                    log.debug("  " + option.getLongOpt() + " : " + configOption.getValue());
                } else {
                    StringBuffer arrayProperty = new StringBuffer();
                    arrayProperty.append("  ").append(option.getLongOpt()).append(" : [");
                    boolean first = true;
                    for (Object value : (Object[]) configOption.getValue()){
                        if (!first) {
                            arrayProperty.append(", ");
                        }
                        arrayProperty.append(value);
                        first = false;
                    }
                    arrayProperty.append("]");

                    log.debug(arrayProperty.toString());
                }
            } else {
                log.debug("  " + option.getLongOpt() + " : <obscured value>");
            }
        }
    }

    /**
     * Parse Cassandra host information submitted in the form:
     * host_addres,jmx_port,native_port|host_address_2,jmx_port,native_port
     *
     * @param stringValue
     * @return
     */
    private String[] parseCassandraHosts(String stringValue) {
        String[] seeds = stringValue.split(",");
        return seeds;
    }

    /**
     * @param value object value to parse
     * @param defaultValue default value
     * @return
     */
    private boolean tryParseBoolean(Object value, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * @param value object value to parse
     * @param defaultValue default value
     * @return
     */
    private Integer tryParseInteger(Object value, int defaultValue) {
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("serial")
    private class HelpRequestedException extends Exception {
        public HelpRequestedException() {
            super("Help Requested");
        }
    }
}