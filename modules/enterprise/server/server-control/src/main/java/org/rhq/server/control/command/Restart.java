/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.server.control.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.server.control.ControlCommand;

/**
 * @author Stefan Negrea
 */
public class Restart extends ControlCommand {

    private Options options;

    public Restart() {
        options = new Options().addOption(null, STORAGE_OPTION, false, "Restart RHQ storage node")
            .addOption(null, SERVER_OPTION, false, "Restart RHQ server")
            .addOption(null, AGENT_OPTION, false, "Restart RHQ agent");
    }

    @Override
    public String getName() {
        return "restart";
    }

    @Override
    public String getDescription() {
        return "Restarts RHQ services.";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected String getReadmeFilename() {
        return "RESTART_README.txt";
    }

    @Override
    protected int exec(CommandLine commandLine) {
        Stop stop = new Stop();
        stop.exec(commandLine);

        // If the server isn't stopped.. restart had some LSB rules.. check 'em.

        Start start = new Start();
        return start.exec(commandLine);
    }
}
