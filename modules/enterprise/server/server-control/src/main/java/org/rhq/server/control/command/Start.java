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
import java.util.HashMap;
import java.util.Map;

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
    protected String getReadmeFilename() {
        return "START_README.txt";
    }

    @Override
    protected void exec(CommandLine commandLine) {
        try {
            // if no options specified, then start whatever is installed
            if (commandLine.getOptions().length == 0) {
                boolean storageInstalled = isStorageInstalled();
                boolean serverInstalled = isServerInstalled();
                boolean agentInstalled = isAgentInstalled();

                if (!(storageInstalled || serverInstalled || agentInstalled)) {
                    log.warn("Nothing to start. No RHQ services are installed.");
                } else {
                    if (storageInstalled) {
                        startStorage();
                    }
                    if (serverInstalled) {
                        startRHQServer();
                    }
                    if (agentInstalled) {
                        startAgent();
                    }
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
            throw new RHQControlException("Failed to start services", e);
        }
    }

    private void startStorage() throws Exception {
        log.debug("Starting RHQ storage node");

        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        // Cassandra looks for JAVA_HOME or then defaults to PATH.  We want it to use the Java
        // defined for RHQ, so make sure JAVA_HOME is set, and set to the RHQ Java for the executor
        // environment.
        String javaExeFilePath = System.getProperty("rhq.java-exe-file-path");
        String javaHome = javaExeFilePath.replace('\\', '/').replace("/bin/java", "");

        Map<String, String> env = new HashMap<String, String>(System.getenv());
        env.put("JAVA_HOME", javaHome);

        if (isWindows()) {
            executor.setWorkingDirectory(getBinDir());
            commandLine = getCommandLine("rhq-storage", "start");
            try {
                executor.execute(commandLine, env);

            } catch (Exception e) {
                // Ignore, service may not exist or may already be running, script returns 1
                log.debug("Failed to start storage service", e);
            }
        } else {
            File storageBinDir = new File(getStorageBasedir(), "bin");
            File pidFile = getStoragePidFile();

            // For now we are duplicating logic in the status command. This code will be
            // replaced when we implement a rhq-storage.sh script.
            if (isStorageRunning()) {
            	String pid = getStoragePid();
                System.out.println("RHQ storage node (pid " + pid + ") is running");
            } else {
                commandLine = getCommandLine(false, "cassandra", "-p", pidFile.getAbsolutePath());
                executor.setWorkingDirectory(storageBinDir);

                executor.execute(commandLine, env);
            }
        }
    }

    private void startRHQServer() throws Exception {
        log.debug("Starting RHQ server");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(getBinDir());
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
