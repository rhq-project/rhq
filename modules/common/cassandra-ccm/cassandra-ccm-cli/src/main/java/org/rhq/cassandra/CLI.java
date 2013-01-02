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

package org.rhq.cassandra;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class CLI {

    private Set<Option> supportedArgs = new HashSet<Option>();

    private Option deployCommand;

    private Option shutdownCommand;

    private String deployDescription = "Creates an embedded cluster and then starts each node";

    private String shutdownDescription = "Shutdown an embedded cluster. Note that if a cassandra.pid file is not " +
        "found, no attempt will be made to shutdown the node.";

    public CLI() {
        deployCommand = OptionBuilder
            .withArgName("[options]")
            .hasOptionalArgs()
            .withDescription(deployDescription)
            .create("deploy");

        shutdownCommand = OptionBuilder
            .withArgName("[options]")
            .hasOptionalArg()
            .withDescription("Shuts down all of the cluster nodes.")
            .create("shutdown");
    }

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-ccm.sh <cmd> [options]";
        String header = "\nwhere <cmd> is one of:";

        Options options = new Options().addOption(deployCommand).addOption(shutdownCommand);

        helpFormatter.setOptPrefix("");
        helpFormatter.printHelp(syntax, header, options, null);
    }

    public void exec(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        List<String> commands = new LinkedList<String>();
        for (String arg : args) {
            if (arg.equals(deployCommand.getOpt()) || arg.equals(shutdownCommand.getOpt())) {
                commands.add(arg);
            }
        }

        if (commands.size() != 1) {
            printUsage();
            return;
        }

        String cmd = commands.get(0);

        if (cmd.equals(deployCommand.getOpt())) {
            deploy(getCommandLine(cmd, args));
        } else if (cmd.equals(shutdownCommand.getOpt())) {
            shutdown(getCommandLine(cmd, args));
        }
    }

    public void deploy(String [] args) {
        Options options = new Options()
            .addOption("h", "help", false, "Show this message.")
            .addOption("n", "num-nodes", true, "The number of nodes to install and configure. The top level or base " +
                "directory for each node will be nodeN where N is the node number.");

        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(options, args);

            if (cmdLine.hasOption("h")) {
                printDeployUsage(options);
            } else {
                DeploymentOptions deploymentOptions = new DeploymentOptions();
                if (cmdLine.hasOption("n")) {
                    int numNodes = Integer.parseInt(cmdLine.getOptionValue("n"));
                    deploymentOptions.setNumNodes(numNodes);
                }

                CassandraClusterManager ccm = new CassandraClusterManager(deploymentOptions);
                List<File> nodeDirs = ccm.installCluster();
                ccm.startCluster(nodeDirs);

                PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();
                try {
                    serverPropertiesUpdater.update("rhq.cassandra.cluster.seeds",
                        StringUtil.collectionToString(ccm.getHostNames()));
                }  catch (IOException e) {
                    throw new RuntimeException("An error occurred while trying to update RHQ server properties", e);
                }
            }
        }  catch (ParseException e) {
            printDeployUsage(options);
        }
    }

    private void printDeployUsage(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-ccm.sh deploy [options]";
        String header = "\n" + deployDescription + "\n\n";

        helpFormatter.setNewLine("\n");
        helpFormatter.printHelp(syntax, header, options, null);
    }

    private static PropertiesFileUpdate getServerProperties() {
        String sysprop = System.getProperty("rhq.server.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhq.server.properties] is not defined.");
        }

        File file = new File(sysprop);
        if (!(file.exists() && file.isFile())) {
            throw new RuntimeException("System property [" + sysprop + "] points to in invalid file.");
        }

        return new PropertiesFileUpdate(file.getAbsolutePath());
    }

    public void shutdown(String[] args) {
        Options options = new Options()
            .addOption("h", "help", false, "Show this message")
            .addOption("n", "node", true, "A comma-delimited list of node ids that specifies nodes to shut down.");

        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(options, args);

            if (cmdLine.hasOption("h")) {
                printShutdownUsage(options);
            } else {
                DeploymentOptions deploymentOptions = new DeploymentOptions();
                if (cmdLine.hasOption("n")) {
                    // TODO
                }
                CassandraClusterManager ccm = new CassandraClusterManager(deploymentOptions);
                ccm.shutdownCluster();
            }
        }  catch (ParseException e) {
            printShutdownUsage(options);
        }
    }

    private void printShutdownUsage(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-ccm.sh shutdown [options]";
        String header = "\n" + shutdownDescription + "\n\n";

        helpFormatter.setNewLine("\n");
        helpFormatter.printHelp(syntax, header, options, null);
    }

    private String[] getCommandLine(String cmd, String[] args) {
        String[] cmdLine = new String[args.length - 1];
        int i = 0;
        for (String arg : args) {
            if (arg.equals(cmd)) {
                continue;
            }
            cmdLine[i++] = arg;
        }
        return cmdLine;
    }

    public static void main(String[] args) {
        CLI cli = new CLI();
        cli.exec(args);
    }

}
