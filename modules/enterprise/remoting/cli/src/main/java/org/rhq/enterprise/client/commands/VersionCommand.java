/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
        if (args.length <= 1) {
            client.getPrintWriter().println(Version.getVersionPropertiesAsString());
        } else {
            Properties props = Version.getVersionProperties();
            for (int i = 1; i < args.length; i++) {
                client.getPrintWriter().println(args[i] + "=" + props.getProperty(args[i], "<unknown>"));
            }
        }
        return true;
    }

    public String getSyntax() {
        return "version [prop name]...";
    }

    public String getHelp() {
        return "Show version information and properties";
    }

    public String getDetailedHelp() {
        return getHelp();
    }
}