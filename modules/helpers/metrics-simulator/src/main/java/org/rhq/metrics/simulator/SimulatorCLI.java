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

package org.rhq.metrics.simulator;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.rhq.metrics.simulator.plan.SimulationPlan;
import org.rhq.metrics.simulator.plan.SimulationPlanner;

/**
 * @author John Sanda
 */
public class SimulatorCLI {

    private Options options;

    public SimulatorCLI() {
        options = new Options();

        Option help = new Option("h", "help", false, "Display this message.");
        options.addOption(help);

        Option simulation = new Option("s", "simulation", true, "The simulation to run. Expected to be a JSON file.");
        options.addOption(simulation);
    }

    public void exec(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(options, args);

            if (cmdLine.hasOption("h")) {
                printUsage();
            } else if (cmdLine.hasOption("s")) {
                runSimulator(cmdLine.getOptionValue("s"));
            } else {
                printUsage();
            }
        } catch (ParseException e) {
            printUsage();
        }
    }

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-ms [options]";
        String header = "";

        helpFormatter.setOptPrefix("");
        helpFormatter.printHelp(syntax, header, options, null);
    }

    public void runSimulator(String file) {
        File planFile = new File(file);
        if (!planFile.exists()) {
            throw new RuntimeException("Simulation file [" + file + "] does not exist.");
        }
        if (planFile.isDirectory()) {
            throw new RuntimeException("[" + file + "] is a directory. The --simulation argument must refer to a  " +
                "file.");
        }

        SimulationPlanner planner = new SimulationPlanner();
        SimulationPlan plan = null;
        try {
            plan = planner.create(planFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create simulation: " + e.getMessage(), e);
        }

        Simulator simulator = new Simulator();
        simulator.run(plan);
    }

    public static void main(String[] args) {
        SimulatorCLI cli = new SimulatorCLI();
        try {
            cli.exec(args);
        } catch (Exception e) {
            System.exit(1);
        }
    }

}
