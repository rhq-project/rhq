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

package org.rhq.server.control;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class RHQControl {

    private final Log log = LogFactory.getLog(RHQControl.class);

    private Commands commands = new Commands();

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhqctl.sh <cmd> [options]";
        String header = "\nwhere <cmd> is one of:";

        helpFormatter.setOptPrefix("");
        helpFormatter.printHelp(syntax, header, commands.getOptions(), null);
    }

    public void exec(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
            } else {
                String commandName = findCommand(commands, args);
                ControlCommand command = commands.get(commandName);

                command.exec(getCommandLine(commandName, args));
            }
        } catch (RHQControlException e) {
            log.error(e);
        }
    }

    private String findCommand(Commands commands, String[] args) throws RHQControlException {
        List<String> commandNames = new LinkedList<String>();
        for (String arg : args) {
            if (commands.contains(arg)) {
                commandNames.add(arg);
            }
        }

        if (commandNames.size() != 1) {
            throw new RHQControlException();
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

    public static void main(String[] args) throws Exception {
        RHQControl control = new RHQControl();
        try {
            control.exec(args);
            System.exit(0);
        } catch (RHQControlException e) {
            control.log.error("There was an unxpected error", e);
            System.exit(1);
        }
    }

}
