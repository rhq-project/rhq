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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;
import org.rhq.server.control.util.ExecutorAssist;

/**
 * @author John Sanda
 */
public class Status extends ControlCommand {

    private Options options;

    public Status() {
        options = new Options().addOption(null, STORAGE_OPTION, false, "Check status of RHQ storage node")
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
    protected String getReadmeFilename() {
        return "STATUS_README.txt";
    }

    @Override
    protected int exec(CommandLine commandLine) {
        int rValue = RHQControl.EXIT_CODE_OK;
        try {
            final boolean isColorSupported = Boolean.parseBoolean(System.getProperty("color"));
            // if no options specified, then check the status of whatever is installed
            if (commandLine.getOptions().length == 0) {
                boolean servicesInstalled = false;

                if (isStorageInstalled()) {
                    servicesInstalled = true;
                    rValue = Math.max(rValue, checkStorageStatus(isColorSupported));
                }
                if (isServerInstalled()) {
                    servicesInstalled = true;
                    rValue = Math.max(rValue, checkServerStatus());
                }
                if (isAgentInstalled()) {
                    servicesInstalled = true;
                    rValue = Math.max(rValue, checkAgentStatus());
                }

                if (!servicesInstalled) {
                    log.warn("No services installed. Please install the server, agent, or storage node and then re-run the command.");
                }
            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        rValue = Math.max(rValue, checkStorageStatus(isColorSupported));
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        rValue = Math.max(rValue, checkServerStatus());
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION
                            + " option will be ignored.");
                    }
                }
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        rValue = Math.max(rValue, checkAgentStatus());
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to check statuses", e);
        }
        return rValue;
    }

    private int checkStorageStatus(boolean isColorSupported) throws Exception {
        log.debug("Checking RHQ storage node status");

        int rValue = RHQControl.EXIT_CODE_OK;

        if (isWindows()) {
            org.apache.commons.exec.CommandLine commandLine;
            commandLine = getCommandLine("rhq-storage", "status");
            rValue = ExecutorAssist.execute(getBinDir(), commandLine);
        } else {
            final String ANSI_RED = "\u001B[31m";
            final String ANSI_GREEN = "\u001B[32m";
            final String ANSI_RESET = "\u001B[0m";

            PrintStream out = null;
            try {
                out = new PrintStream(System.out, true, "UTF-8");
            } catch (UnsupportedEncodingException exception) {
                out = System.out;
            }

            if (isStorageRunning()) {
                out.println(String.format("%-30s", "RHQ Storage Node") + " (pid "
                    + String.format("%-7s", getStoragePid()) + ") is " + (isColorSupported ? ANSI_GREEN : "")
                    + "\u2714running" + (isColorSupported ? ANSI_RESET : ""));
            } else {
                out.println(String.format("%-30s", "RHQ Storage Node") + " (no pid file) is "
                    + (isColorSupported ? ANSI_RED : "") + "\u2718down" + (isColorSupported ? ANSI_RESET : ""));
                rValue = RHQControl.EXIT_CODE_STATUS_NOT_RUNNING;
            }
        }
        return rValue;
    }

    private int checkServerStatus() throws Exception {
        log.debug("Checking RHQ server status");

        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "status");
        return ExecutorAssist.execute(getBinDir(), commandLine);
    }

    private int checkAgentStatus() throws Exception {
        log.debug("Checking RHQ agent status");

        File agentBinDir = new File(getAgentBasedir(), "bin");

        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-agent-wrapper", "status");
        int rValue = ExecutorAssist.execute(agentBinDir, commandLine);
        if(!isWindows() && rValue > 1 && rValue != 3) {
            // Return codes 0 and agent not running are accepted, but anything else above 0 isn't
            throw new RHQControlException("rhq-agent-wrapper exited with return value " + rValue);
        }
        return rValue;
    }
}
