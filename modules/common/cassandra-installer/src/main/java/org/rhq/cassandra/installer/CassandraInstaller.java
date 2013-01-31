/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.installer;

import java.io.File;
import java.net.InetAddress;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.UnmanagedDeployer;

/**
 * @author John Sanda
 */
public class CassandraInstaller {

    private Options options;

    public CassandraInstaller() {
        Option hostname = new Option("n", "hostname", true, "The hostname or IP address on which the node will listen for " +
            "requests. If not specified, defaults to the value returned by InetAddress.getLocalHost().getHostName().");
         hostname.setArgName("HOSTNAME");

        Option dir =  new Option("d", "dir", true, "The directory in which to install Cassandra. Defaults to the " +
            "current working directory");
        dir.setArgName("INSTALL_DIR");

        Option seeds = new Option("s", "seeds", true, "A comma-delimited list of hostnames or IP addresses that " +
            "serve as contact points. Nodes use this list to find each other and to learn the cluster topology. " +
            "It does not need to specify all nodes in the cluster. Defaults to this nodes hostname.");
        seeds.setArgName("SEEDS");

        options = new Options()
            .addOption(new Option("h", "help", false, "Show this message."))
            .addOption(hostname)
            .addOption(dir)
            .addOption(seeds);
    }

    public void run(CommandLine cmdLine) throws Exception {
        if (cmdLine.hasOption("h")) {
            printUsage();
        } else {
            DeploymentOptions options = new DeploymentOptions();

            File basedir;
            if (cmdLine.hasOption("d")) {
                basedir = new File(cmdLine.getOptionValue("d"));
            } else {
                basedir = new File(System.getProperty("user.dir"));
            }
            options.setBasedir(basedir.getAbsolutePath());

            String hostname;
            if (cmdLine.hasOption("n")) {
                hostname = cmdLine.getOptionValue("n");
            } else {
                hostname = InetAddress.getLocalHost().getHostName();
            }
            options.setListenAddress(hostname);
            options.setRpcAddress(hostname);

            String seeds;
            if (cmdLine.hasOption("s")) {
                seeds = cmdLine.getOptionValue("s");
            } else {
                seeds = hostname;
            }
            options.setSeeds(seeds);

            options.setCommitLogDir(new File(basedir, "commit_log").getAbsolutePath());
            options.setSavedCachesDir(new File(basedir, "saved_caches").getAbsolutePath());
            options.setDataDir(new File(basedir, "data").getAbsolutePath());
            options.setLogDir(new File(basedir, "logs").getAbsolutePath());
            options.load();

            UnmanagedDeployer deployer = new UnmanagedDeployer();
            deployer.unpackBundle();
            deployer.deploy(options, 1);
            System.out.println(getInstallationSummary(options));
            deployer.cleanUpBundle();
        }
    }

    public void printUsage() {
        Options options = getOptions();
        HelpFormatter helpFormatter = new HelpFormatter();
        String header = "\nInstalls RHQ metrics database.\n\n";
        String syntax = "java -jar rhq-cassandra-installer.jar [options]";

        helpFormatter.setNewLine("\n");
        helpFormatter.printHelp(syntax, header, options, null);
    }

    public Options getOptions() {
        return options;
    }

    public String getInstallationSummary(DeploymentOptions options) {
        return "\n" +
            "Installation Summary:\n" +
            "Finished installing Cassandra in " + options.getBasedir() + "\n\n" +
            "IMPORTANT - remember to update the rhq.cassandra.seeds property in rhq-server.properties with the " +
            "following:\n" +
            "\thostname: " + options.getListenAddress() + "\n" +
            "\tthrift port: " + options.getRpcPort() + "\n" +
            "\tcql port: " + options.getNativeTransportPort();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Running Cassandra installer...");
        CassandraInstaller installer = new CassandraInstaller();
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(installer.getOptions(), args);
            installer.run(cmdLine);
            return;
        } catch (ParseException e) {
            installer.printUsage();
        }
    }

}
