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

package org.rhq.server.control.command;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Start extends ControlCommand {

    private Options options;

    public Start() {
        options = new Options()
            .addOption(null, "storage", false, "Start RHQ storage node")
            .addOption(null, "server", false, "Start RHQ server")
            .addOption(null, "agent", false, "Start RHQ agent");
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Starts RHQ services.";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        boolean startStorage;
        boolean startServer;
        boolean startAgent;

        if (commandLine.getOptions().length == 0) {
            startStorage = true;
            startServer = true;
            startAgent = true;
        } else {
            startStorage = commandLine.hasOption("storage");
            startServer = commandLine.hasOption("server");
            startAgent = commandLine.hasOption("agent");
        }

        try {
            if (startStorage) {
                startStorage();
            }
            if (startServer) {
                startRHQServer();
            }
            if (startAgent) {
                startAgent();
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to stop services", e);
        }
    }

    private void startStorage() throws Exception {
        log.info("Starting RHQ storage node");

        File storageBasedir = new File(basedir, "storage");
        File storageBinDir = new File(storageBasedir, "bin");

        new ProcessBuilder("./cassandra", "-p", "cassandra.pid")
            .directory(storageBinDir)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    }

    private void startRHQServer() throws Exception {
        log.info("Starting RHQ server");

        new ProcessBuilder("./rhq-server.sh", "start")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

    private void startAgent() throws Exception {
        log.info("Starting RHQ agent");

        File agentHomeDir = new File(basedir, "rhq-agent");
        File agentBinDir = new File(agentHomeDir, "bin");

        new ProcessBuilder("./rhq-agent-wrapper.sh", "start")
            .directory(agentBinDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

}
