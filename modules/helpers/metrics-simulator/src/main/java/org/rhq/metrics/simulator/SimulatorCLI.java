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

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

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
    }

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-ms [options]";
        String header = "";

        helpFormatter.setOptPrefix("");
        helpFormatter.printHelp(syntax, header, options, null);
    }

    public static void main(String[] args) {
        SimulatorCLI cli = new SimulatorCLI();
        cli.exec(args);
    }

}
