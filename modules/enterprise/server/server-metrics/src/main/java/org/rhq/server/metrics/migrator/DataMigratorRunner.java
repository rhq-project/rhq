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
import java.util.HashMap;
import java.util.Map;
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

import org.rhq.server.metrics.migrator.DataMigrator.DatabaseType;


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

    private final Log log = LogFactory.getLog(DataMigratorRunner.class);

    //Cassandra
    private Option cassandraUserOption = OptionBuilder.withLongOpt("cassandra-user").hasArg().withType(String.class)
        .withDescription("Cassandra user (default: rhqadmin)").create();
    private Option cassandraPasswordOption = OptionBuilder.withLongOpt("cassandra-password").hasArg()
        .withDescription("Cassandra password (default: rhqadmin)").withType(String.class).create();
    private Option cassandraHostsOption = OptionBuilder.withLongOpt("cassandra-hosts").hasArg().withType(String.class)
        .withDescription("Cassandra hosts, format host_ip_1,host_ip_2,... (default: 127.0.0.1")
        .create();
    private Option cassandraPortOption = OptionBuilder.withLongOpt("cassandra-port").hasArg().withType(Integer.class)
        .withDescription("Cassandra native binary protocol port (default: 9142)").create();
    private Option cassandraCompressionOption = OptionBuilder.withLongOpt("cassandra-compression").hasOptionalArg()
        .withType(Boolean.class).withDescription("Enable compression for communication with Cassandra (default: true)")
        .create();

    //SQL
    private Option sqlUserOption = OptionBuilder.withLongOpt("sql-user").hasArg().withType(String.class)
        .withDescription("SQL server user (default: rhqadmin)").create();
    private Option sqlPasswordOption = OptionBuilder.withLongOpt("sql-password").hasArg().withType(String.class)
        .withDescription("SQL server password (default: rhqadmin)").create();
    private Option sqlHostOption = OptionBuilder.withLongOpt("sql-host").hasArg().withType(String.class)
        .withDescription("SQL server host address (default: localhost)").create();
    private Option sqlPortOption = OptionBuilder.withLongOpt("sql-port").hasArg().withType(String.class)
        .withDescription("SQL server port (default: 5432)").create();
    private Option sqlDBOption = OptionBuilder.withLongOpt("sql-db").hasArg().withType(String.class)
        .withDescription("SQL database (default: rhq)").create();

    private Option sqlServerType = OptionBuilder.withLongOpt("sql-server-type").hasArg().withType(String.class)
        .withDescription("SQL server type, only postgres and oracle are supported (default: postgres)").create();
    private Option sqlPostgresServer = OptionBuilder.withLongOpt("sql-server-postgres").hasOptionalArg()
        .withType(Boolean.class).withDescription("Postgres SQL server.").create();
    private Option sqlOracleServer = OptionBuilder.withLongOpt("sql-server-oracle").hasOptionalArg()
        .withType(Boolean.class)
        .withDescription("Oracle SQL server.").create();

    //Migration
    private Option disableRawOption = OptionBuilder.withLongOpt("disable-raw-migration").hasOptionalArg().withType(Boolean.class)
        .withDescription("Disable raw table migration (default: false)").create();
    private Option disable1HOption = OptionBuilder.withLongOpt("disable-1h-migration").hasOptionalArg().withType(Boolean.class)
        .withDescription("Disable 1 hour aggregates table migration (default: false)").create();
    private Option disable6HOption = OptionBuilder.withLongOpt("disable-6h-migration").hasOptionalArg().withType(Boolean.class)
        .withDescription("Disable 6 hours aggregates table migration (default: false)").create();
    private Option disable1DOption = OptionBuilder.withLongOpt("disable-1d-migration").hasOptionalArg().withType(Boolean.class)
        .withDescription("Disable 24 hours aggregates table migration (default: false)").create();
    private Option preserveDataOption = OptionBuilder.withLongOpt("preserve-data").hasOptionalArg().withType(Boolean.class)
        .withDescription("Preserve SQL data post migration (default: true)").create();
    private Option deleteDataOption = OptionBuilder.withLongOpt("delete-data").hasOptionalArg().withType(Boolean.class)
        .withDescription("Delete SQL data at the end of migration (default: false)").create();
    private Option estimateOnlyOption = OptionBuilder.withLongOpt("estimate-only").hasOptionalArg().withType(Boolean.class)
        .withDescription("Only estimate how long the migration will take (default: false)").create();
    private Option deleteOnlyOption = OptionBuilder.withLongOpt("delete-only").hasOptionalArg().withType(Boolean.class)
        .withDescription("Only delete data from the old SQL server, no migration will be performed (default: false)")
        .create();
    private Option experimentalExportOption = OptionBuilder.withLongOpt("experimental-export").hasOptionalArg().withType(Boolean.class)
        .withDescription("Enable experimental bulk export for Postgres, option ignored for Oracle migration (default: false)")
        .create();

    //Runner
    private Option helpOption = OptionBuilder.withLongOpt("help").create("h");
    private Option debugLogOption = OptionBuilder.withLongOpt("debugLog")
        .withDescription("Enable debug level logs for the communication with Cassandra and SQL Server (default: false)")
        .create("X");
    private Option configFileOption = OptionBuilder.withLongOpt("config-file").hasArg()
        .withDescription("Configuration file. All the command line options can be set in a typical properties file. " +
                    "Command line arguments take precedence over default and configuration file options.")
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
    }

    private void configure(String args[]) throws Exception {
        options = new Options();

        options.addOption(cassandraUserOption);
        options.addOption(cassandraPasswordOption);
        options.addOption(cassandraHostsOption);
        options.addOption(cassandraPortOption);
        options.addOption(cassandraCompressionOption);

        options.addOption(sqlUserOption);
        options.addOption(sqlPasswordOption);
        options.addOption(sqlHostOption);
        options.addOption(sqlPortOption);
        options.addOption(sqlDBOption);
        options.addOption(sqlServerType);
        options.addOption(sqlPostgresServer);
        options.addOption(sqlOracleServer);

        options.addOption(disableRawOption);
        options.addOption(disable1HOption);
        options.addOption(disable6HOption);
        options.addOption(disable1DOption);
        options.addOption(preserveDataOption);
        options.addOption(deleteDataOption);
        options.addOption(estimateOnlyOption);
        options.addOption(deleteOnlyOption);
        options.addOption(experimentalExportOption);

        options.addOption(helpOption);
        options.addOption(debugLogOption);
        options.addOption(configFileOption);

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
        if (commandLine.hasOption(configFileOption.getLongOpt())) {
            loadConfigFile(commandLine.getOptionValue(configFileOption.getLongOpt()));
        }

        parseCassandraOptions(commandLine);
        parseSQLOptions(commandLine);
        parseMigrationOptions(commandLine);
    }

    /**
     * Add default configuration options to the configuration store.
     */
    private void loadDefaultConfiguration() {
        //default Cassandra configuration
        configuration.put(cassandraUserOption, "rhqadmin");
        configuration.put(cassandraPasswordOption, "rhqadmin");
        configuration.put(cassandraHostsOption, new String[] { "127.0.0.1" });
        configuration.put(cassandraPortOption, 9042);
        configuration.put(cassandraCompressionOption, true);

        //default SQL configuration
        configuration.put(sqlUserOption, "rhqadmin");
        configuration.put(sqlPasswordOption, "rhqadmin");
        configuration.put(sqlHostOption, "localhost");
        configuration.put(sqlPortOption, "5432");
        configuration.put(sqlDBOption, "rhq");
        configuration.put(sqlServerType, "postgres");

        //default runner options
        configuration.put(disableRawOption, false);
        configuration.put(disable1HOption, false);
        configuration.put(disable6HOption, false);
        configuration.put(disable1DOption, false);
        configuration.put(preserveDataOption, true);
        configuration.put(estimateOnlyOption, false);
        configuration.put(deleteOnlyOption, false);
        configuration.put(experimentalExportOption, false);
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
                    } else if (option.equals(sqlServerType)) {
                        if ("oracle".equals(optionValue)) {
                            configuration.put(option, "oracle");
                        } else {
                            configuration.put(option, "postgres");
                        }
                    } else if (option.equals(sqlPostgresServer)) {
                        boolean value = tryParseBoolean(optionValue.toString(), true);
                        if (value == true) {
                            configuration.put(sqlServerType, "postgres");
                        }
                    } else if (option.equals(sqlOracleServer)) {
                        boolean value = tryParseBoolean(optionValue.toString(), true);
                        if (value == true) {
                            configuration.put(sqlServerType, "oracle");
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

        log.debug(configuration.toString());
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
            Integer cassandraPort = tryParseInteger(commandLine.getOptionValue(cassandraPortOption.getLongOpt()), 9142);
            configuration.put(cassandraPortOption, cassandraPort);
        }

        if (commandLine.hasOption(cassandraCompressionOption.getLongOpt())) {
            boolean value = tryParseBoolean(commandLine.getOptionValue(cassandraCompressionOption.getLongOpt()), true);
            configuration.put(cassandraCompressionOption, value);
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

        if (commandLine.hasOption(sqlServerType.getLongOpt())) {
            if ("oracle".equals(commandLine.getOptionValue(sqlServerType.getLongOpt()))) {
                configuration.put(sqlServerType, "oracle");
            } else {
                configuration.put(sqlServerType, "postgres");
            }
        } else if (commandLine.hasOption(sqlPostgresServer.getLongOpt())) {
            configuration.put(sqlServerType, "postgres");
        } else if (commandLine.hasOption(sqlOracleServer.getLongOpt())) {
            configuration.put(sqlServerType, "oracle");
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

        if (commandLine.hasOption(preserveDataOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(preserveDataOption.getLongOpt()), true);
            configuration.put(preserveDataOption, value);
        } else if (commandLine.hasOption(deleteDataOption.getLongOpt())) {
            value = tryParseBoolean(commandLine.getOptionValue(deleteDataOption.getLongOpt()), true);
            configuration.put(preserveDataOption, value);
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
        EntityManager entityManager = this.createEntityManager();
        log.debug("Done creating Entity Manager");

        log.debug("Creating Cassandra session");
        Session session = this.createCassandraSession();
        log.debug("Done creating Cassandra session");

        DatabaseType databaseType = DatabaseType.Postgres;
        if ("oracle".equals(configuration.get(sqlServerType))) {
            databaseType = databaseType.Oracle;
        }
        DataMigrator migrator = new DataMigrator(entityManager, session, databaseType, tryParseBoolean(
            configuration.get(experimentalExportOption), false));

        if (!(Boolean) configuration.get(deleteOnlyOption)) {
            if ((Boolean) configuration.get(preserveDataOption)) {
                migrator.preserveData();
            } else {
                migrator.deleteAllDataAtEndOfMigration();
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
            migrator.runRawDataMigration(true);
            migrator.run1HAggregateDataMigration(true);
            migrator.run6HAggregateDataMigration(true);
            migrator.run1DAggregateDataMigration(true);

            System.out.println("Estimation process - starting\n");
            long estimate = migrator.estimate();
            System.out.println("The deletion of old data will take approximately: "
                + TimeUnit.MILLISECONDS.toMinutes(estimate) + " minutes (or " + estimate + " milliseconds)\n");
            System.out.println("Estimation process - ended\n\n");

            if (!(Boolean) configuration.get(estimateOnlyOption)) {
                System.out.println("Old data deletion process - starting\n");
                long startTime = System.currentTimeMillis();
                migrator.deleteOldData();
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("The deletion process took: " + TimeUnit.MILLISECONDS.toMinutes(duration)
                    + " minutes (or " + duration + " milliseconds)\n");
                System.out.println("Old data deletion process - ended\n");
            }
        }

        /*if (entityManager != null) {
            entityManager.close();
        }
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }*/
    }

    /**
     * Create a Cassandra session based on configuration options.
     *
     * @return Cassandra session
     * @throws Exception
     */
    private Session createCassandraSession() throws Exception {
        Compression selectedCompression = Compression.NONE;
        if ((Boolean) configuration.get(cassandraCompressionOption)) {
            selectedCompression = Compression.SNAPPY;
        }

        Cluster cluster = Cluster
            .builder()
            .addContactPoints((String[]) configuration.get(cassandraHostsOption))
            .withPort((Integer) configuration.get(cassandraPortOption))
            .withCompression(selectedCompression)
            .withoutMetrics()
            .withCredentials((String) configuration.get(cassandraUserOption),
                (String) configuration.get(cassandraPasswordOption))
            .build();

        return cluster.connect("rhq");
    }

    /**
     * Create a hibernate session to the SQL server.
     *
     * @return
     * @throws Exception
     */
    private EntityManager createEntityManager() throws Exception {
        Properties properties = new Properties();
        properties.put("javax.persistence.provider", "org.hibernate.ejb.HibernatePersistence");
        properties.put("hibernate.connection.username", (String) configuration.get(sqlUserOption));
        properties.put("hibernate.connection.password", (String) configuration.get(sqlPasswordOption));
        properties.put("javax.persistence.query.timeout", DataMigrator.SQL_TIMEOUT);
        properties.put("hibernate.c3p0.timeout", DataMigrator.SQL_TIMEOUT);

        if ("oracle".equals(configuration.get(sqlServerType))) {
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
            properties.put("hibernate.connection.url", "jdbc:oracle:thin:@" + (String) configuration.get(sqlHostOption)
                + ":" + (String) configuration.get(sqlPortOption) + ":" + (String) configuration.get(sqlDBOption));
            properties.put("hibernate.default_schema", (String) configuration.get(sqlDBOption));
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
            properties.put("hibernate.connection.url", "jdbc:postgresql://" + (String) configuration.get(sqlHostOption)
                + ":" + (String) configuration.get(sqlPortOption) + "/" + (String) configuration.get(sqlDBOption));
        }

        log.debug("Creating entity manager with the following configuration:");
        log.debug(properties);

        Ejb3Configuration configuration = new Ejb3Configuration();
        configuration.setProperties(properties);
        EntityManagerFactory factory = configuration.buildEntityManagerFactory();
        return factory.createEntityManager();
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