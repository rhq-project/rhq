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
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

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
        try {
            // if no options specified, then stop whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (isStorageInstalled()) {
                    stopStorage();
                }
                if (isServerInstalled()) {
                    stopRHQServer();
                }
                if (isAgentInstalled()) {
                    stopAgent();
                }
            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        stopStorage();
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION +
                            " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        stopRHQServer();
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION +
                            " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        stopAgent();
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION +
                            " option will be ignored.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to stop services", e);
        }
    }

    private void stopStorage() throws Exception {
        log.debug("Stopping RHQ storage node");

        String pid = getStoragePid();
        if (pid != null) {
            System.out.println("Stopping RHQ storage node...");
            System.out.println("RHQ storage node (pid=" + pid + ") is stopping...");

            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine("kill")
                .addArgument(pid);
            Executor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler());
            executor.execute(commandLine);

            System.out.println("RHQ storage node has stopped");
        }
    }

    private void stopRHQServer() throws Exception {
        log.debug("Stopping RHQ server");

        String pid = getServerPid();
        if (pid != null) {
            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(
                "./rhq-server.sh").addArgument("stop");
            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(binDir);
            executor.setStreamHandler(new PumpStreamHandler());
            executor.execute(commandLine);
        }
    }

    private void stopAgent() throws Exception {
        log.debug("Stopping RHQ agent");

        File agentHomeDir = new File(basedir, "rhq-agent");
        File agentBinDir = new File(agentHomeDir, "bin");
        String pid = getAgentPid();

        if (pid != null) {
            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(
                "./rhq-agent-wrapper.sh").addArgument("stop");
            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(agentBinDir);
            executor.setStreamHandler(new PumpStreamHandler());
            executor.execute(commandLine);
        }
    }
}
