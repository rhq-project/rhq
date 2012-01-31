/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
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
package org.rhq.enterprise.client.commands;

import java.util.Properties;
import java.util.jar.Attributes;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.Version;

/**
 * Command to show the user the version information of the CLI.
 * 
 * @author John Mazzitelli
 */
public class VersionCommand implements ClientCommand {

    public String getPromptCommandString() {
        return "version";
    }

    public boolean execute(ClientMain client, String[] args) {
        VersionArgs versionArgs = parseArgs(args);
        if (versionArgs.verbose) {
            client.getPrintWriter().println(Version.getVersionPropertiesAsString());
        } else {
            Properties props = Version.getVersionProperties();
            String version = props.getProperty(Attributes.Name.IMPLEMENTATION_VERSION.toString());
            client.getPrintWriter().println(version);
        }

        return true;
    }

    public String getSyntax() {
        return getPromptCommandString() + " [-v | --verbose]";
    }

    public String getHelp() {
        return "Show CLI version information";
    }

    public String getDetailedHelp() {
        return getHelp() + ". If no arguments are specified, the CLI's version is printed. If the verbose option is "
            + "specified, the values of the main attributes from the CLI jar's MANIFEST.MF are printed.";
    }

    private VersionArgs parseArgs(String[] args) {
        String shortOpts = "-:v";
        LongOpt[] longOpts = {
            new LongOpt("verbose", LongOpt.OPTIONAL_ARGUMENT, null, 'v')
        };
        Getopt getopt = new Getopt("exec", args, shortOpts, longOpts);

        VersionArgs versionArgs = new VersionArgs();

        int code = getopt.getopt();
        while (code != -1) {
            switch (code) {
                case ':':
                    throw new IllegalArgumentException("Illegal option.");
                case 'v':
                    versionArgs.verbose = true;
                    break;
            }
            code = getopt.getopt();
        }

        return versionArgs;
    }

    private static class VersionArgs {
        boolean verbose;
    }
}