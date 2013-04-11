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
public class Console extends ControlCommand {

    private Options options;

    public Console() {
        options = new Options()
            .addOption(null, "storage", false, "Start the RHQ storage node in the foreground")
            .addOption(null, "server", false, "Start the RHQ server in the foreground")
            .addOption(null, "agent", false, "Start the RHQ agent in the foreground");
    }

    @Override
    public String getName() {
        return "console";
    }

    @Override
    public String getDescription() {
        return "Starts an RHQ service in the foreground. Only one of the following options should be specified.";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        if (commandLine.getOptions().length != 1) {
            printUsage();
        } else {
            String option = commandLine.getOptions()[0].getLongOpt();
            try {
                if (option.equals(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        startStorageInForeground();
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION +
                            " option will be ignored.");
                    }
                } else if (option.equals(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        startServerInForeground();
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION +
                            " option will be ignored.");
                    }
                } else if (option.equals(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        startAgentInForeground();
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION +
                            " option will be ignored.");
                    }
                } else {
                    throw new IllegalArgumentException(option + " is not a supported option");
                }
            } catch (Exception e) {
                throw new RHQControlException("Failed to execute console command", e);
            }
        }
    }

    private void startStorageInForeground() throws Exception {
        log.debug("Starting RHQ storage node in foreground");

        File storageBasedir = new File(basedir, "storage");
        File storageBinDir = new File(storageBasedir, "bin");

        new ProcessBuilder("./cassandra", "-f")
            .directory(storageBinDir)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

    private void startServerInForeground() throws Exception {
        log.debug("Starting RHQ server in foreground");

        new ProcessBuilder("./rhq-server.sh", "console")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

    private void startAgentInForeground() throws Exception {
        log.info("Starting RHQ agent in foreground");

        File agentHomeDir = new File(basedir, "rhq-agent");
        File agentBinDir = new File(agentHomeDir, "bin");

        new ProcessBuilder("./rhq-agent.sh")
            .directory(agentBinDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }
}
