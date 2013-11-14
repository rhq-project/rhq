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

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Stop extends AbstractInstall {

    private Options options;

    public Stop() {
        options = new Options().addOption(null, "storage", false, "Stop RHQ storage node")
            .addOption(null, "server", false, "Stop RHQ server").addOption(null, "agent", false, "Stop RHQ agent");
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
    protected String getReadmeFilename() {
        return "STOP_README.txt";
    }

    @Override
    protected void exec(CommandLine commandLine) {
        try {
            // if no options specified, then stop whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (isAgentInstalled()) {
                    stopAgent();
                }

                // the server service may be installed even if the full server install fails. The files to execute
                // the remove are there after the initial unzip, so just go ahead and try to stop the service. This
                // may help clean up a failed install.
                stopRHQServer();

                if (isStorageInstalled()) {
                    stopStorage();
                }
            } else {
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        stopAgent();
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                    }
                }

                if (commandLine.hasOption(SERVER_OPTION)) {
                    // the server service may be installed even if the full server install fails. The files to execute
                    // the remove are there after the initial unzip, so just go ahead and try to stop the service. This
                    // may help clean up a failed install.
                    stopRHQServer();
                }

                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        stopStorage();
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to stop services", e);
        }
    }

    private void stopStorage() throws Exception {
        log.debug("Stopping RHQ storage node");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(getBinDir());
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        if (isWindows()) {
            commandLine = getCommandLine("rhq-storage", "stop");
            try {
                executor.execute(commandLine);

            } catch (Exception e) {
                // Ignore, service may not exist or be running, script returns 1
                log.debug("Failed to stop storage service", e);
            }
        } else {
            if (isStorageRunning()) {
                String pid = getStoragePid();

                System.out.println("Stopping RHQ storage node...");
                System.out.println("RHQ storage node (pid=" + pid + ") is stopping...");

                killPid(pid);

                waitForProcessToStop(pid);

                System.out.println("RHQ storage node has stopped");
            }

        }
    }

    private void stopRHQServer() throws Exception {
        log.debug("Stopping RHQ server");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(getBinDir());
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "stop");

        if (isWindows()) {
            try {
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to stop server service", e);
            }
        } else {
            String pid = getServerPid();

            if (pid != null) {
                executor.execute(commandLine);
            }
        }
    }

    private void stopAgent() throws Exception {
        log.debug("Stopping RHQ agent");

        File agentBinDir = new File(getAgentBasedir(), "bin");
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(agentBinDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-agent-wrapper", "stop");

        if (isWindows()) {
            try {
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to stop agent service", e);
            }
        } else {
            String pid = getAgentPid();

            if (pid != null) {
                executor.execute(commandLine);
            }
        }
    }
}
