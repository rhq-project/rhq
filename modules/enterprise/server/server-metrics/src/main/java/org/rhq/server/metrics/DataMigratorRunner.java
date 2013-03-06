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

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleAuthInfoProvider;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    private String cassandraUser;
    private Option cassandraUserOption = OptionBuilder.withLongOpt("cassandra-user").hasArg().create();

    private String cassandraPassword;
    private Option cassandraPasswordOption = OptionBuilder.withLongOpt("cassandra-password").hasArg().create();

    private String[] cassandraHosts;
    private Option cassandraHostsOption = OptionBuilder.withLongOpt("cassandra-hosts").hasArg().create();

    private boolean cassandraCompression;
    private Option cassandraCompressionOption = OptionBuilder.withLongOpt("cassandra-compression").create();

    //SQL
    private String sqlUser;
    private Option sqlUserOption = OptionBuilder.withLongOpt("sql-user").hasArg().create();

    private String sqlPassword;
    private Option sqlPasswordOption = OptionBuilder.withLongOpt("sql-password").hasArg().create();

    private String sqlHost;
    private Option sqlHostOption = OptionBuilder.withLongOpt("sql-host").hasArg().create();

    private String sqlPort;
    private Option sqlPortOption = OptionBuilder.withLongOpt("sql-port").hasArg().create();

    private String sqlDB;
    private Option sqlDBOption = OptionBuilder.withLongOpt("sql-db").hasArg().create();

    //Migration
    private boolean disableRaw;
    private Option disableRawOption = OptionBuilder.withLongOpt("disable-raw-migration").create();

    private boolean disable1H;
    private Option disable1HOption = OptionBuilder.withLongOpt("disable-1h-migration").create();

    private boolean disable6H;
    private Option disable6HOption = OptionBuilder.withLongOpt("disable-6h-migration").create();

    private boolean disable1D;
    private Option disable1DOption = OptionBuilder.withLongOpt("disable-1d-migration").create();

    /**
     * @param args
     * @throws ParseException
     */
    public static void main(String[] args) throws Exception {
        try{
            DataMigratorRunner runner = new DataMigratorRunner();
            runner.configure(args);
            runner.run();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

        System.exit(0);
    }

    private void configure(String args[]) throws Exception {
        Options options = new Options();
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

        CommandLineParser parser = new PosixParser();
        CommandLine commandLine = parser.parse(options, args);

        parseCassandraOptionsWithDefault(commandLine);
        parseSQLOptionsWithDefault(commandLine);
        parseMigrationOptionsWithDefault(commandLine);
    }

    private void run() throws Exception {
        log.info("Creating Entity Manager");
        EntityManager entityManager = this.createEntityManager();
        log.info("Done creating Entity Manager");

        log.info("Creating Cassandra session");
        Session session = this.createCassandraSession();
        log.info("Done creating Cassandra session");

        DataMigrator migrator = new DataMigrator(entityManager, session);

        migrator.preserveData();
        migrator.runRawDataMigration(!disableRaw);
        migrator.run1HAggregateDataMigration(!disable1H);
        migrator.run6HAggregateDataMigration(!disable6H);
        migrator.run1DAggregateDataMigration(!disable1D);

        migrator.migrateData();
    }

    private Session createCassandraSession() throws Exception {
        Compression selectedCompression = Compression.NONE;
        if (cassandraCompression) {
            selectedCompression = Compression.SNAPPY;
        }

        Cluster cluster = Cluster
            .builder()
            .addContactPoints(cassandraHosts)
            .withCompression(selectedCompression)
            .withoutMetrics()
            .withAuthInfoProvider(
                new SimpleAuthInfoProvider().add("username", cassandraUser).add("password", cassandraPassword)).build();

        return cluster.connect("rhq");
    }

    private EntityManager createEntityManager() throws Exception {
        Properties properties = new Properties();
        properties.put("javax.persistence.provider", "org.hibernate.ejb.HibernatePersistence");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.driver_class", "org.postgresql.Driver");
        properties.put("hibernate.connection.username", sqlUser);
        properties.put("hibernate.connection.password", sqlPassword);
        properties.put("hibernate.connection.url", "jdbc:postgresql://" + sqlHost + ":" + sqlPort + "/" + sqlDB);

        Ejb3Configuration configuration = new Ejb3Configuration();
        configuration.setProperties(properties);
        EntityManagerFactory factory = configuration.buildEntityManagerFactory();
        return factory.createEntityManager();
     }

    private void parseCassandraOptionsWithDefault(CommandLine commandLine) throws NoHostAvailableException {
        if (commandLine.hasOption(cassandraUserOption.getLongOpt())) {
            cassandraUser = commandLine.getOptionValue(cassandraUserOption.getLongOpt());
        } else {
            cassandraUser = "rhqadmin";
        }

        if (commandLine.hasOption(cassandraPasswordOption.getLongOpt())) {
            cassandraPassword = commandLine.getOptionValue(cassandraPasswordOption.getLongOpt());
        } else {
            cassandraPassword = "rhqadmin";
        }

        if (commandLine.hasOption(cassandraHostsOption.getLongOpt())) {
            String[] seeds = commandLine.getOptionValue(cassandraHostsOption.getLongOpt()).split(",");
            cassandraHosts = new String[seeds.length];
            for (int i = 0; i < seeds.length; ++i) {
                CassandraNode node = CassandraNode.parseNode(seeds[i]);
                cassandraHosts[i] = node.getHostName();
            }
        } else {
            cassandraHosts = new String[] { "127.0.0.1", "127.0.0.2" };
        }

        if (commandLine.hasOption(cassandraCompressionOption.getLongOpt())) {
            cassandraCompression = true;
        } else {
            cassandraCompression = false;
        }
    }

    private void parseSQLOptionsWithDefault(CommandLine commandLine) throws NoHostAvailableException {
        if (commandLine.hasOption(sqlUserOption.getLongOpt())) {
            sqlUser = commandLine.getOptionValue(sqlUserOption.getLongOpt());
        } else {
            sqlUser = "rhqadmin";
        }

        if (commandLine.hasOption(sqlPasswordOption.getLongOpt())) {
            sqlPassword = commandLine.getOptionValue(sqlPasswordOption.getLongOpt());
        } else {
            sqlPassword = "rhqadmin";
        }

        if (commandLine.hasOption(sqlHostOption.getLongOpt())) {
            sqlHost = commandLine.getOptionValue(sqlHostOption.getLongOpt());
        } else {
            sqlHost = "localhost";
        }

        if (commandLine.hasOption(sqlPortOption.getLongOpt())) {
            sqlPort = commandLine.getOptionValue(sqlPortOption.getLongOpt());
        } else {
            sqlPort = "5432";
        }

        if (commandLine.hasOption(sqlDBOption.getLongOpt())) {
            sqlDB = commandLine.getOptionValue(sqlDBOption.getLongOpt());
        } else {
            sqlDB = "rhq_db";
        }
    }

    private void parseMigrationOptionsWithDefault(CommandLine commandLine) {
        if (commandLine.hasOption(disableRawOption.getLongOpt())) {
            disableRaw = true;
        } else {
            disableRaw = false;
        }

        if (commandLine.hasOption(disable1HOption.getLongOpt())) {
            disable1H = true;
        } else {
            disable1H = false;
        }

        if (commandLine.hasOption(disable6HOption.getLongOpt())) {
            disable6H = true;
        } else {
            disable6H = false;
        }

        if (commandLine.hasOption(disable1DOption.getLongOpt())) {
            disable1D = true;
        } else {
            disable1D = false;
        }
    }
}

