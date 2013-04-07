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
public class Status extends ControlCommand {

    private Options options;

    public Status() {
        options = new Options()
            .addOption(null, STORAGE_OPTION, false, "Check status of RHQ storage node")
            .addOption(null, SERVER_OPTION, false, "Check status of RHQ server")
            .addOption(null, AGENT_OPTION, false, "Check status of RHQ agent");
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public String getDescription() {
        return "Check status of RHQ services";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        boolean checkStorage;
        boolean checkServer;
        boolean checkAgent;

        if (commandLine.getOptions().length == 0) {
            checkStorage = true;
            checkServer = true;
            checkAgent = true;
        } else {
            checkStorage = commandLine.hasOption(STORAGE_OPTION);
            checkServer = commandLine.hasOption(SERVER_OPTION);
            checkAgent = commandLine.hasOption(AGENT_OPTION);
        }

        try {
            if (checkStorage) {
                if (commandLine.hasOption(STORAGE_OPTION) && !isStorageInstalled()) {
                    log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION +
                        " option will be ignored.");
                } else {
                    checkStorageStatus();
                }
            }

            if (checkServer) {
                if (commandLine.hasOption(SERVER_OPTION) && !isServerInstalled()) {
                    log.warn("It appears that the server is not installed. The --" + SERVER_OPTION +
                        " option will be ignored.");
                } else {
                    checkServerStatus();
                }
            }

            if (checkAgent) {
                if (commandLine.hasOption(AGENT_OPTION) && !isAgentInstalled()) {
                    log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION +
                        " option will be ignored.");
                } else {
                    checkAgentStatus();
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to check statuses", e);
        }
    }

    private void checkStorageStatus() throws Exception {
        log.debug("Checking RHQ storage node status");

        File storageBasedir = new File(basedir, "storage");
        File storageBinDir = new File(storageBasedir, "bin");
        File pidFile = new File(storageBinDir, "cassandra.pid");

        if (pidFile.exists()) {
            String pid = StreamUtil.slurp(new FileReader(pidFile));
            System.out.println("RHQ storage node (pid " + pid + ") is running");
        } else {
            System.out.println("RHQ storage node (no pid file) is NOT running");
        }
    }

    private void checkServerStatus() throws Exception {
        log.debug("Checking RHQ server status");

        new ProcessBuilder("./rhq-server.sh", "status")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

    private void checkAgentStatus() throws Exception {
        log.debug("Checking RHQ agent status");

        File agentHomeDir = new File(basedir, "rhq-agent");
        File agentBinDir = new File(agentHomeDir, "bin");

        new ProcessBuilder("./rhq-agent-wrapper.sh", "status")
            .directory(agentBinDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }
}
