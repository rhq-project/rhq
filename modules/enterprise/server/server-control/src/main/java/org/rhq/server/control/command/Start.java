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

import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;
import org.rhq.server.control.util.ExecutorAssist;

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
    protected int exec(CommandLine commandLine) {
        int rValue = RHQControl.EXIT_CODE_OK;
        try {
            // if no options specified, then start whatever is installed
            if (commandLine.getOptions().length == 0) {
                boolean storageInstalled = isStorageInstalled();
                boolean serverInstalled = isServerInstalled();
                boolean agentInstalled = isAgentInstalled();

                if (!(storageInstalled || serverInstalled || agentInstalled)) {
                    log.warn("Nothing to start. No RHQ services are installed.");
                    rValue = RHQControl.EXIT_CODE_NOT_INSTALLED;
                } else {
                    if (storageInstalled) {
                        rValue = Math.max(rValue, startStorage());
                    }
                    if (serverInstalled) {
                        rValue = Math.max(rValue, startRHQServer());
                    }
                    if (agentInstalled) {
                        rValue = Math.max(rValue, startAgent());
                    }
                }
            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        rValue = Math.max(rValue, startStorage());
                    } else {
                        log.warn("It appears that the storage node is not installed. The --" + STORAGE_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_NOT_INSTALLED;
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        rValue = Math.max(rValue, startRHQServer());
                    } else {
                        log.warn("It appears that the server is not installed. The --" + SERVER_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_NOT_INSTALLED;
                    }
                }
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled()) {
                        rValue = Math.max(rValue, startAgent());
                    } else {
                        log.warn("It appears that the agent is not installed. The --" + AGENT_OPTION
                            + " option will be ignored.");
                        rValue = RHQControl.EXIT_CODE_NOT_INSTALLED;
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to start services", e);
        }
        return rValue;
    }

    private int startStorage() throws Exception {
        log.debug("Starting RHQ storage node");

        org.apache.commons.exec.CommandLine commandLine;

        int rValue;

        // Cassandra looks for JAVA_HOME or then defaults to PATH.  We want it to use the Java
        // defined for RHQ, so make sure JAVA_HOME is set, and set to the RHQ Java for the executor
        // environment.
        String javaExeFilePath = System.getProperty("rhq.java-exe-file-path");
        String javaHome = javaExeFilePath.replace('\\', '/').replace("/bin/java", "");

        Map<String, String> env = new HashMap<String, String>(System.getenv());
        env.put("JAVA_HOME", javaHome);

        if (isWindows()) {
            // Force CASSANDRA_HOME to prevent starting wrong instance of Cassandra (BZ 1069855)
            env.put("CASSANDRA_HOME", getStorageBasedir().getAbsolutePath());
            commandLine = getCommandLine("rhq-storage", "start");
            rValue = ExecutorAssist.execute(getBinDir(), commandLine, env);
            if(rValue != RHQControl.EXIT_CODE_OK) {
                log.debug("Failed to start storage service, return value" + rValue);                    
            }
        } else {
            File storageBinDir = new File(getStorageBasedir(), "bin");
            File pidFile = getStoragePidFile();

            // For now we are duplicating logic in the status command. This code will be
            // replaced when we implement a rhq-storage.sh script.
            if (isStorageRunning()) {
                String pid = getStoragePid();
                System.out.println("RHQ storage node (pid " + pid + ") is running");
                rValue = RHQControl.EXIT_CODE_OK;
            } else {
                // Force CASSANDRA_INCLUDE to prevent starting wrong instance of Cassandra (BZ 1069855)
                env.put("CASSANDRA_INCLUDE", storageBinDir + File.separator + "cassandra.in.sh");
                commandLine = getCommandLine(false, "cassandra", "-p", pidFile.getAbsolutePath());
                rValue = ExecutorAssist.execute(storageBinDir, commandLine, env);
            }
        }
        return rValue;
    }

    private int startRHQServer() throws Exception {
        log.debug("Starting RHQ server");
        validateServerPropertiesFile();
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "start");

        int rValue;

        if (isWindows()) {
            try {
                rValue = ExecutorAssist.execute(getBinDir(), commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to start server service", e);
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        } else {
            rValue = ExecutorAssist.execute(getBinDir(), commandLine);
        }
        return rValue;
    }

    private int startAgent() throws Exception {
        log.debug("Starting RHQ agent");

        File agentBinDir = new File(getAgentBasedir(), "bin");
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-agent-wrapper", "start");

        int rValue;

        if (isWindows()) {
            try {
                rValue = ExecutorAssist.execute(agentBinDir, commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to start agent service", e);
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        } else {
            rValue = ExecutorAssist.execute(agentBinDir, commandLine);
        }

        return rValue;
    }

}
