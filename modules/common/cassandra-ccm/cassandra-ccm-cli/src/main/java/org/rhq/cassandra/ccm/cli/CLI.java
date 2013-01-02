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

package org.rhq.cassandra.ccm.cli;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;

import org.rhq.cassandra.ccm.cli.command.CCMCommand;

/**
 * @author John Sanda
 */
public class CLI {

    private Commands commands = new Commands();

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-ccm.sh <cmd> [options]";
        String header = "\nwhere <cmd> is one of:";

        helpFormatter.setOptPrefix("");
        helpFormatter.printHelp(syntax, header, commands.getOptions(), null);
    }

    public void exec(String[] args) {
        try {
            if (args.length == 0) {
                throw new CLIException();
            }
            String commandName = findCommand(commands, args);
            CCMCommand command = commands.get(commandName);

            command.exec(getCommandLine(commandName, args));
        } catch (CLIException e) {
            printUsage();
        }
    }

    private String findCommand(Commands commands, String[] args) throws CLIException {
        List<String> commandNames = new LinkedList<String>();
        for (String arg : args) {
            if (commands.contains(arg)) {
                commandNames.add(arg);
            }
        }

        if (commandNames.size() != 1) {
            throw new CLIException();
        }

        return commandNames.get(0);
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
