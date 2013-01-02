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

/**
 * @author John Sanda
 */
public class CLI {

    private Set<Option> supportedArgs = new HashSet<Option>();

    private Option deployCommand;

    private Option shutdownCommand;

    private String deployDescription = "Creates an embedded cluster and then starts each node";

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
                ccm.installCluster();
                ccm.startCluster();
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

    public void shutdown() {

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
//        OptionGroup ccmArgs = new OptionGroup();
//
//        Option deploy = OptionBuilder
//            .withArgName("[options]")
//            .hasOptionalArgs()
//            .withDescription("Creates an embedded cluster and then starts each node")
//            .create("deploy");
//
//        Option shutdown = OptionBuilder
//            .withArgName("[options]")
//            .hasOptionalArg()
//            .withDescription("Shuts down all of the cluster nodes.")
//            .create("shutdown");
//
//        ccmArgs.addOption(deploy).addOption(shutdown);
//        //ccmArgs.setRequired(true);
//
//        CommandLineParser parser = new PosixParser();
//        Options options = new Options();
//        options.addOptionGroup(ccmArgs);
//
//        try {
//            CommandLine cmdLine = parser.parse(options, args);
//        }  catch (ParseException e) {
//            e.printStackTrace();
//        }
        CLI cli = new CLI();
        cli.exec(args);
    }

}
