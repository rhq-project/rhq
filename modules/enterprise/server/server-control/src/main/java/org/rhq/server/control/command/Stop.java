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
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;
import org.rhq.server.control.util.ExecutorAssist;

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
    protected int exec(CommandLine commandLine) {

        int rValue = RHQControl.EXIT_CODE_OK;

        try {
            // if no options specified, then stop whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (isAgentInstalled()) {
                    rValue = Math.max(rValue, stopAgent());
                }

                // the server service may be installed even if the full server install fails. The files to execute
                // the remove are there after the initial unzip, so just go ahead and try to stop the service. This
                // may help clean up a failed install.
                rValue = Math.max(rValue, stopRHQServer());

                if (isStorageInstalled()) {
                    rValue = Math.max(rValue, stopStorage());
                }
            } else {
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        rValue = Math.max(rValue, stopAgent());
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_INVALID_ARGUMENT;
                    }
                }

                if (commandLine.hasOption(SERVER_OPTION)) {
                    // the server service may be installed even if the full server install fails. The files to execute
                    // the remove are there after the initial unzip, so just go ahead and try to stop the service. This
                    // may help clean up a failed install.
                    rValue = Math.max(rValue, stopRHQServer());
                }

                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        rValue = Math.max(rValue, stopStorage());
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

    private int stopStorage() throws Exception {
        log.debug("Stopping RHQ storage node");

        org.apache.commons.exec.CommandLine commandLine;

        int rValue;

        if (isWindows()) {
            commandLine = getCommandLine("rhq-storage", "stop");
            rValue = ExecutorAssist.execute(getBinDir(), commandLine);
            if(rValue != RHQControl.EXIT_CODE_OK) {
                log.debug("Failed to stop storage service, return code " + rValue);                
            } else {
                System.out.println("RHQ storage node has stopped");                
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
            rValue = RHQControl.EXIT_CODE_OK; // If process isn't running, stopping it is considered OK.

        }
        return rValue;
    }

    private int stopRHQServer() throws Exception {
        log.debug("Stopping RHQ server");

        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "stop");

        int rValue;

        if (isWindows()) {
            rValue = ExecutorAssist.execute(getBinDir(), commandLine);
            if(rValue != RHQControl.EXIT_CODE_OK) {
                log.debug("Failed to stop server service");                    
            }
        } else {
            String pid = getServerPid();

            if (pid != null) {
                rValue = ExecutorAssist.execute(getBinDir(), commandLine);
            } else {
                rValue = RHQControl.EXIT_CODE_OK;
            }
        }
        return rValue;
    }

    private int stopAgent() throws Exception {
        log.debug("Stopping RHQ agent");

        File agentBinDir = new File(getAgentBasedir(), "bin");
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-agent-wrapper", "stop");

        int rValue;

        if (isWindows()) {
            rValue = ExecutorAssist.execute(agentBinDir, commandLine);
            if(rValue != RHQControl.EXIT_CODE_OK) {
                log.debug("Failed to stop agent service, return value" + rValue);                
            }
        } else {
            String pid = getAgentPid();

            if (pid != null) {
                rValue = ExecutorAssist.execute(agentBinDir, commandLine);
            } else {
                rValue = RHQControl.EXIT_CODE_OK;
            }
        }
        return rValue;
    }
}
