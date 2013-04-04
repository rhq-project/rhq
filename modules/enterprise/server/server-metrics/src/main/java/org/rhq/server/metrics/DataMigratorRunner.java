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

package org.rhq.server.metrics;

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
import com.datastax.driver.core.SimpleAuthInfoProvider;
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

import org.rhq.cassandra.CassandraNode;


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
    private Option cassandraUserOption = OptionBuilder.withLongOpt("cassandra-user").hasArg()
        .withType(String.class).create();
    private Option cassandraPasswordOption = OptionBuilder.withLongOpt("cassandra-password").hasArg()
        .withType(String.class).create();
    private Option cassandraHostsOption = OptionBuilder.withLongOpt("cassandra-hosts").hasArg()
        .withType(String.class).create();
    private Option cassandraCompressionOption = OptionBuilder.withLongOpt("cassandra-compression")
        .withType(String.class).create();

    //SQL
    private Option sqlUserOption = OptionBuilder.withLongOpt("sql-user").hasArg()
        .withType(String.class).create();
    private Option sqlPasswordOption = OptionBuilder.withLongOpt("sql-password").hasArg()
        .withType(String.class).create();
    private Option sqlHostOption = OptionBuilder.withLongOpt("sql-host").hasArg()
        .withType(String.class).create();
    private Option sqlPortOption = OptionBuilder.withLongOpt("sql-port").hasArg()
        .withType(String.class).create();
    private Option sqlDBOption = OptionBuilder.withLongOpt("sql-db").hasArg()
        .withType(String.class).create();

    //Migration
    private Option disableRawOption = OptionBuilder.withLongOpt("disable-raw-migration")
        .withType(Boolean.class).create();
    private Option disable1HOption = OptionBuilder.withLongOpt("disable-1h-migration")
        .withType(Boolean.class).create();
    private Option disable6HOption = OptionBuilder.withLongOpt("disable-6h-migration")
        .withType(Boolean.class).create();
    private Option disable1DOption = OptionBuilder.withLongOpt("disable-1d-migration")
        .withType(Boolean.class).create();
    private Option preserveDataOption = OptionBuilder.withLongOpt("preserve-data")
        .withType(Boolean.class).create();
    private Option deleteDataOption = OptionBuilder.withLongOpt("delete-data")
        .withType(Boolean.class).create();
    private Option estimateOnlyOption = OptionBuilder.withLongOpt("estimate-only")
        .withType(Boolean.class).create();

    //Misc
    private Option helpOption = OptionBuilder.withLongOpt("help").create("h");
    private Option debugLogOption = OptionBuilder.withLongOpt("debugLog").create("X");
    private Option configFileOption = OptionBuilder.withLongOpt("config-file").hasArg().create();


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
            System.out.println(e);
            e.printStackTrace();
        }

        System.exit(0);
    }

    private void configure(String args[]) throws Exception {
        options = new Options();

        options.addOption(cassandraUserOption);
        options.addOption(cassandraPasswordOption);
        options.addOption(cassandraHostsOption);
        options.addOption(cassandraCompressionOption);

        options.addOption(sqlUserOption);
        options.addOption(sqlPasswordOption);
        options.addOption(sqlHostOption);
        options.addOption(sqlPortOption);
        options.addOption(sqlDBOption);

        options.addOption(disableRawOption);
        options.addOption(disable1HOption);
        options.addOption(disable6HOption);
        options.addOption(disable1DOption);
        options.addOption(preserveDataOption);
        options.addOption(deleteDataOption);
        options.addOption(estimateOnlyOption);

        options.addOption(helpOption);
        options.addOption(debugLogOption);
        options.addOption(configFileOption);

        CommandLine commandLine;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
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

        parseCassandraOptionsWithDefault(commandLine);
        parseSQLOptionsWithDefault(commandLine);
        parseMigrationOptionsWithDefault(commandLine);
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

        Logger cassandraLogging = root.getLoggerRepository().getLogger("log4j.logger.org.apache.cassandra.cql.jdbc");
        cassandraLogging.setLevel(level);

        Logger hibernateLogging = root.getLoggerRepository().getLogger("org.hibernate");
        hibernateLogging.setLevel(level);
    }

    private void run() throws Exception {
        log.info("Creating Entity Manager");
        EntityManager entityManager = this.createEntityManager();
        log.info("Done creating Entity Manager");

        log.info("Creating Cassandra session");
        Session session = this.createCassandraSession();
        log.info("Done creating Cassandra session");

        DataMigrator migrator = new DataMigrator(entityManager, session);

        if ((Boolean) configuration.get(preserveDataOption)) {
            migrator.preserveData();
        } else {
            migrator.deleteAllDataAtEndOfMigration();
        }

        migrator.runRawDataMigration(!(Boolean) configuration.get(disableRawOption));
        migrator.run1HAggregateDataMigration(!(Boolean) configuration.get(disable1HOption));
        migrator.run6HAggregateDataMigration(!(Boolean) configuration.get(disable6HOption));
        migrator.run1DAggregateDataMigration(!(Boolean) configuration.get(disable1DOption));

        long estimate = migrator.estimate();
        log.info("The migration process will take approximately: " + TimeUnit.MILLISECONDS.toMinutes(estimate)
            + " minutes (or " + estimate + " milliseconds)");
        if (!(Boolean) configuration.get(estimateOnlyOption)) {
            migrator.migrateData();
        }
    }

    private Session createCassandraSession() throws Exception {
        Compression selectedCompression = Compression.NONE;
        if ((Boolean) configuration.get(cassandraCompressionOption)) {
            selectedCompression = Compression.SNAPPY;
        }

        Cluster cluster = Cluster
            .builder()
            .addContactPoints((String[]) configuration.get(cassandraHostsOption))
            .withCompression(selectedCompression)
            .withoutMetrics()
            .withAuthInfoProvider(
                new SimpleAuthInfoProvider().add("username", (String) configuration.get(cassandraUserOption)).add(
                    "password", (String) configuration.get(cassandraPasswordOption))).build();

        return cluster.connect("rhq");
    }

    private EntityManager createEntityManager() throws Exception {
        Properties properties = new Properties();
        properties.put("javax.persistence.provider", "org.hibernate.ejb.HibernatePersistence");
        properties.put("hibernate.connection.username", (String) configuration.get(sqlUserOption));
        properties.put("hibernate.connection.password", (String) configuration.get(sqlPasswordOption));

        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.driver_class", "org.postgresql.Driver");
        properties.put("hibernate.connection.url", "jdbc:postgresql://" + (String) configuration.get(sqlHostOption)
            + ":" + (String) configuration.get(sqlPortOption) + "/" + (String) configuration.get(sqlDBOption));

        Ejb3Configuration configuration = new Ejb3Configuration();
        configuration.setProperties(properties);
        EntityManagerFactory factory = configuration.buildEntityManagerFactory();
        return factory.createEntityManager();
     }

    private void parseCassandraOptionsWithDefault(CommandLine commandLine) throws NoHostAvailableException {
        if (commandLine.hasOption(cassandraUserOption.getLongOpt())) {
            configuration.put(cassandraUserOption, commandLine.getOptionValue(cassandraUserOption.getLongOpt()));
        }

        if (commandLine.hasOption(cassandraPasswordOption.getLongOpt())) {
            configuration.put(cassandraPasswordOption,
                commandLine.getOptionValue(cassandraPasswordOption.getLongOpt()));
        }

        if (commandLine.hasOption(cassandraHostsOption.getLongOpt())) {
            String[] cassandraHosts = parseCassandraHosts(commandLine.getOptionValue(cassandraHostsOption.getLongOpt()));
            configuration.put(cassandraHostsOption, cassandraHosts);
        }

        if (commandLine.hasOption(cassandraCompressionOption.getLongOpt())) {
            configuration.put(cassandraCompressionOption, true);
        }
    }

    private String[] parseCassandraHosts(String stringValue) {
        String[] seeds = stringValue.split(",");
        String[] cassandraHosts = new String[seeds.length];
        for (int i = 0; i < seeds.length; ++i) {
            CassandraNode node = CassandraNode.parseNode(seeds[i]);
            cassandraHosts[i] = node.getHostName();
        }
        return cassandraHosts;
    }

    private void parseSQLOptionsWithDefault(CommandLine commandLine) throws NoHostAvailableException {
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
    }

    private void parseMigrationOptionsWithDefault(CommandLine commandLine) {
        if (commandLine.hasOption(disableRawOption.getLongOpt())) {
            configuration.put(disableRawOption, true);
        }

        if (commandLine.hasOption(disable1HOption.getLongOpt())) {
            configuration.put(disable1HOption, true);
        }

        if (commandLine.hasOption(disable6HOption.getLongOpt())) {
            configuration.put(disable6HOption, true);
        }

        if (commandLine.hasOption(disable1DOption.getLongOpt())) {
            configuration.put(disable1DOption, true);
        }

        if (commandLine.hasOption(preserveDataOption.getLongOpt())) {
            configuration.put(preserveDataOption, true);
        } else if (commandLine.hasOption(deleteDataOption.getLongOpt())) {
            configuration.put(preserveDataOption, false);
        }

        if (commandLine.hasOption(estimateOnlyOption.getLongOpt())) {
            configuration.put(estimateOnlyOption, true);
        }
    }

    private void loadDefaultConfiguration() {
        //default Cassandra configuration
        configuration.put(cassandraUserOption, "rhqadmin");
        configuration.put(cassandraPasswordOption, "rhqadmin");
        configuration.put(cassandraHostsOption, new String[] { "127.0.0.1", "127.0.0.2" });
        configuration.put(cassandraCompressionOption, false);

        //default SQL configuration
        configuration.put(sqlUserOption, "rhqadmin");
        configuration.put(sqlPasswordOption, "rhqadmin");
        configuration.put(sqlHostOption, "localhost");
        configuration.put(sqlPortOption, "5432");
        configuration.put(sqlDBOption, "rhq_db");

        //default runner options
        configuration.put(disableRawOption, false);
        configuration.put(disable1HOption, false);
        configuration.put(disable6HOption, false);
        configuration.put(disable1DOption, false);
        configuration.put(preserveDataOption, true);
        configuration.put(estimateOnlyOption, false);
    }

    private void loadConfigFile(String file) {
        try {
            File configFile = new File(file);
            if (!configFile.exists()){
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
                    log.info("Configuration option loaded: " + option.getLongOpt() + " (" + option.getType() + ") -> "
                        + optionValue);

                    if (option.equals(cassandraHostsOption)) {
                        String[] cassandraHosts = parseCassandraHosts(optionValue.toString());
                        configuration.put(option, cassandraHosts);
                    } else if (option.getType().equals(Boolean.class)) {
                        configuration.put(option, Boolean.parseBoolean(optionValue.toString()));
                    } else {
                        configuration.put(option, optionValue.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to load or process the configuration file.", e);
            System.exit(1);
        }

        log.error(configuration.toString());
    }

    @SuppressWarnings("serial")
    private class HelpRequestedException extends Exception {
        public HelpRequestedException() {
            super("Help Requested");
        }
    }
}

