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
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;
import org.rhq.server.control.util.ExecutorAssist;

/**
 * This command is registered on Windows only.  It performs Windows service removal.
 * <p/>
 * If we decide to enable this for Linux it is coded to by a synonym for stop.
 *
 * @author Jay Shaughnessy
 */
public class Remove extends ControlCommand {

    private Options options;

    public Remove() {
        options = new Options().addOption(null, "storage", false, "Remove RHQ storage node service")
            .addOption(null, "server", false, "Remove RHQ server service")
            .addOption(null, "agent", false, "Remove RHQ agent service");
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Removes RHQ services on Windows.";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected String getReadmeFilename() {
        return "REMOVE_README.txt";
    }

    @Override
    protected int exec(CommandLine commandLine) {
        int rValue = RHQControl.EXIT_CODE_OK;
        try {
            // if no options specified, then stop whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (isAgentInstalled()) {
                    rValue = Math.max(rValue, removeAgentService());
                }

                // the server service may be installed even if the full server install fails. The files to execute
                // the remove are there after the initial unzip, so just go ahead and try to remove the service. This
                // may help clean up a failed install.
                rValue = Math.max(rValue, removeServerService());

                if (isStorageInstalled()) {
                    rValue = Math.max(rValue, removeStorageService());
                }
            } else {
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        rValue = Math.max(rValue, removeAgentService());
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    // the server service may be installed even if the full server install fails. The files to execute
                    // the remove are there after the initial unzip, so just go ahead and try to remove the service. This
                    // may help clean up a failed install.
                    rValue = Math.max(rValue, removeServerService());
                }
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        rValue = Math.max(rValue, removeStorageService());
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to stop services", e);
        }
        return rValue;
    }

    private int removeStorageService() throws Exception {
        log.debug("Stopping RHQ storage node");

        org.apache.commons.exec.CommandLine commandLine;

        int rValue;

        if (isWindows()) {
            commandLine = getCommandLine("rhq-storage", "remove");
            try {
                rValue = ExecutorAssist.execute(getBinDir(), commandLine);
            } catch (Exception e) {
                log.debug("Failed to remove storage service", e);
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        } else {
            String pid = getStoragePid();
            if (pid != null) {
                System.out.println("Stopping RHQ storage node...");
                System.out.println("RHQ storage node (pid=" + pid + ") is stopping...");

                commandLine = new org.apache.commons.exec.CommandLine("kill").addArgument(pid);
                rValue = ExecutorAssist.execute(getBinDir(), commandLine);

                System.out.println("RHQ storage node has stopped");
            } else {
                rValue = RHQControl.EXIT_CODE_OK;
            }

        }
        return rValue;
    }

    private int removeServerService() throws Exception {
        log.debug("Stopping RHQ server");

        org.apache.commons.exec.CommandLine commandLine;

        int rValue;

        if (isWindows()) {
            try {
                commandLine = getCommandLine("rhq-server", "remove");
                rValue = ExecutorAssist.execute(getBinDir(), commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to remove server service", e);
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        } else {
            commandLine = getCommandLine("rhq-server", "stop");
            rValue = ExecutorAssist.execute(getBinDir(), commandLine);
        }
        return rValue;
    }

    private int removeAgentService() throws Exception {
        log.debug("Stopping RHQ agent");

        File agentBinDir = new File(getAgentBasedir(), "bin");
        org.apache.commons.exec.CommandLine commandLine;

        int rValue;

        if (isWindows()) {
            try {
                commandLine = getCommandLine("rhq-agent-wrapper", "remove");
                rValue = ExecutorAssist.execute(agentBinDir, commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist, script returns 1
                log.debug("Failed to remove agent service", e);
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        } else {
            commandLine = getCommandLine("rhq-agent-wrapper", "stop");
            rValue = ExecutorAssist.execute(agentBinDir, commandLine);
        }
        return rValue;
    }
}
