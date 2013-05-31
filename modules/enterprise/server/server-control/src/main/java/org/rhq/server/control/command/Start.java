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
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Start extends ControlCommand {

    private Options options;

    public Start() {
        options = new Options().addOption(null, STORAGE_OPTION, false, "Start RHQ storage node")
            .addOption(null, SERVER_OPTION, false, "Start RHQ server")
            .addOption(null, AGENT_OPTION, false, "Start RHQ agent");
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
        try {
            // if no options specified, then start whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (isStorageInstalled()) {
                    startStorage();
                }
                if (isServerInstalled()) {
                    startRHQServer();
                }
                if (isAgentInstalled()) {
                    startAgent();
                }
            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        startStorage();
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        startRHQServer();
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        startAgent();
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to stop services", e);
        }
    }

    private void startStorage() throws Exception {
        log.debug("Starting RHQ storage node");

        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        if (isWindows()) {
            executor.setWorkingDirectory(binDir);
            commandLine = getCommandLine("rhq-storage", "start");
            try {
                executor.execute(commandLine);

            } catch (Exception e) {
                // Ignore, service may not exist or may already be running, script returns 1
                log.debug("Failed to start storage service", e);
            }
        } else {

            File storageBinDir = new File(getStorageBasedir(), "bin");
            File pidFile = new File(storageBinDir, "cassandra.pid");

            // For now we are duplicating logic in the status command. This code will be
            // replaced when we implement a rhq-storage.sh script.
            if (pidFile.exists()) {
                String pid = StreamUtil.slurp(new FileReader(pidFile));
                System.out.println("RHQ storage node (pid " + pid + ") is running");
            } else {
                commandLine = getCommandLine(false, "cassandra", "-p", pidFile.getAbsolutePath());
                executor.setWorkingDirectory(storageBinDir);

                executor.execute(commandLine);
            }
        }
    }

    private void startRHQServer() throws Exception {
        log.debug("Starting RHQ server");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(binDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "start");

        if (isWindows()) {
            try {
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to start server service", e);
            }
        } else {
            executor.execute(commandLine);
        }
    }

    private void startAgent() throws Exception {
        log.debug("Starting RHQ agent");

        File agentBinDir = new File(getAgentBasedir(), "bin");
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(agentBinDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-agent-wrapper", "start");

        if (isWindows()) {
            try {
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to start agent service", e);
            }
        } else {
            executor.execute(commandLine);
        }
    }

}
