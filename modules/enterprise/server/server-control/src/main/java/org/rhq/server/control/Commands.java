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

import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.rhq.server.control.command.Console;
import org.rhq.server.control.command.Install;
import org.rhq.server.control.command.Remove;
import org.rhq.server.control.command.Start;
import org.rhq.server.control.command.Status;
import org.rhq.server.control.command.Stop;
import org.rhq.server.control.command.Upgrade;

/**
 * @author John Sanda
 */
public class Commands {

    private Map<String, ControlCommand> commands = new TreeMap<String, ControlCommand>(new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            return s1.compareTo(s2);
        }
    });

    public Commands() {
        registerCommand(new Install());
        registerCommand(new Start());
        registerCommand(new Stop());
        registerCommand(new Status());
        registerCommand(new Console());
        // Add the service removal command only on windows
        if (File.separatorChar == '\\') {
            registerCommand(new Remove());
        }
        registerCommand(new Upgrade());
    }

    private void registerCommand(ControlCommand command) {
        commands.put(command.getName(), command);
    }

    public Options getOptions() {
        Options options = new Options();
        for (ControlCommand cmd : commands.values()) {
            if (cmd.getOptions().getOptions().isEmpty()) {
                options.addOption(OptionBuilder.withDescription(cmd.getDescription()).create(cmd.getName()));
            } else if (cmd.getOptions().getOptions().size() == 1) {
                options.addOption(OptionBuilder.withArgName("[options]").hasOptionalArg()
                    .withDescription(cmd.getDescription()).create(cmd.getName()));
            } else {
                options.addOption(OptionBuilder.withArgName("[options]").hasOptionalArgs()
                    .withDescription(cmd.getDescription()).create(cmd.getName()));
            }
        }
        return options;
    }

    public ControlCommand get(String name) {
        return commands.get(name);
    }

    public boolean contains(String name) {
        return commands.containsKey(name);
    }

}
