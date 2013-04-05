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
import java.io.FileReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Stop extends ControlCommand {

    private Options options;

    public Stop() {
        options = new Options()
            .addOption(null, "storage", false, "Stop RHQ storage node")
            .addOption(null, "server", false, "Stop RHQ server")
            .addOption(null, "agent", false, "Stop RHQ agent");
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Stops RHQ services";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        boolean stopStorage;
        boolean stopServer;
        boolean stopAgent;

        if (commandLine.getOptions().length == 0) {
            stopStorage = true;
            stopServer = true;
            stopAgent = true;
        } else {
            stopStorage = commandLine.hasOption("storage");
            stopServer = commandLine.hasOption("server");
            stopAgent = commandLine.hasOption("agent");
        }

        try {
            if (stopStorage) {
                stopStorage();
            }
            if (stopServer) {
                stopRHQServer();
            }
            if (stopAgent) {
                stopAgent();
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to stop services", e);
        }
    }

    private void stopStorage() throws Exception {
        log.info("Stopping RHQ storage node");

        File storageBasedir = new File(basedir, "storage");
        File storageBinDir = new File(storageBasedir, "bin");

        File pidFile = new File(storageBinDir, "cassandra.pid");
        String pid = StreamUtil.slurp(new FileReader(pidFile));

        new ProcessBuilder("kill", pid)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

    private void stopRHQServer() throws Exception {
        log.info("Stopping RHQ server");

        new ProcessBuilder("./rhq-server.sh", "stop")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

    private void stopAgent() throws Exception {
        log.info("Stopping RHQ agent");

        File agentHomeDir = new File(basedir, "rhq-agent");
        File agentBinDir = new File(agentHomeDir, "bin");

        new ProcessBuilder("./rhq-agent-wrapper.sh", "stop")
            .directory(agentBinDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }
}
