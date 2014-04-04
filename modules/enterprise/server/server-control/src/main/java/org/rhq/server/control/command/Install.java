/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.server.control.command;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.core.util.file.FileReverter;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Install extends AbstractInstall {

    private Options options;

    public Install() {
        options = new Options()
            .addOption(
                null,
                "storage",
                false,
                "Install RHQ storage node. The install directory will be ["
                    + getStorageBasedir()
                    + "]. Note that this option implies --agent which means an agent will also be installed, if one is not yet installed.")
            .addOption(
                null,
                "server",
                false,
                "Install RHQ server. If you have not yet installed an RHQ storage node somewhere in your network, you must specify --storage to install one.")
            .addOption(null, "agent", false,
                "Install RHQ agent. The install directory will be [" + getAgentBasedir() + "]")
            .addOption(null, AGENT_CONFIG_OPTION, true,
                "An alternate XML file to use in place of the default agent-configuration.xml")
            .addOption(
                null,
                AGENT_PREFERENCE,
                true,
                "An agent preference setting (whose argument is in the form 'name=value') to be set in the agent. More than one of these is allowed.")
            .addOption(
                null,
                START_OPTION,
                false,
                "If specified then immediately start the services after installation.  Note that services may be started and shut down as part of the installation process, but will not be started or left running by default.")
            .addOption(null, STORAGE_DATA_ROOT_DIR, true,
                "The root directory under which all storage data directories will be placed.");
    }

    @Override
    public String getName() {
        return "install";
    }

    @Override
    public String getDescription() {
        return "Installs RHQ services.";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    protected String getReadmeFilename() {
        return "INSTALL_README.txt";
    }

    @Override
    protected int exec(CommandLine commandLine) {
        boolean start = commandLine.hasOption(START_OPTION);
        boolean startedStorage = false;
        boolean startedServer = false;
        int rValue = RHQControl.EXIT_CODE_OK;

        try {
            List<String> errors = validateOptions(commandLine);
            if (!errors.isEmpty()) {
                for (String error : errors) {
                    log.error(error);
                }
                log.error("Exiting due to the previous errors");
                return RHQControl.EXIT_CODE_NOT_INSTALLED;
            }

            // If any failures occur, we know we need to reset rhq-server.properties.
            final FileReverter serverPropFileReverter = new FileReverter(getServerPropertiesFile());
            addUndoTask(new ControlCommand.UndoTask("Reverting server properties file") {
                public void performUndoWork() throws Exception {
                    try {
                        serverPropFileReverter.revert();
                    } catch (Exception e) {
                        throw new Exception("Cannot reset rhq-server.properties - revert settings manually", e);
                    }
                }
            });

            boolean installAll = (!(commandLine.hasOption(STORAGE_OPTION) || commandLine.hasOption(SERVER_OPTION) || commandLine
                .hasOption(AGENT_OPTION)));
            boolean installStorage = installAll || commandLine.hasOption(STORAGE_OPTION);
            boolean installServer = installAll || commandLine.hasOption(SERVER_OPTION);
            boolean installAgent = installAll || commandLine.hasOption(AGENT_OPTION)
                || commandLine.hasOption(STORAGE_OPTION);
            boolean startStorage = false;

            if (installStorage) {
                if (isStorageInstalled()) {
                    log.info("The RHQ storage node is already installed in [" + new File(getBaseDir(), "rhq-storage")
                        + "]. It will not be installed.");

                    if (isWindows()) {
                        log.info("Ensuring the RHQ Storage Windows service exists. Ignore any CreateService failure.");
                        rValue = Math.max(rValue, installWindowsService(getBinDir(), "rhq-storage", false, false));
                    }
                } else {
                    rValue = Math.max(rValue, installStorageNode(getStorageBasedir(), commandLine, false));
                    startStorage = start;
                }
            }

            if (startStorage || installServer) {
                startedStorage = true;
                Start startCommand = new Start();
                rValue = Math.max(rValue, startCommand.exec(new String[] { "--storage" }));
            }

            if (installServer) {
                if (isServerInstalled()) {
                    log.info("The RHQ server is already installed. It will not be installed.");

                    if (isWindows()) {
                        log.info("Ensuring the RHQ Server Windows service exists. Ignore any CreateService failure.");
                        rValue = Math.max(rValue, installWindowsService(getBinDir(), "rhq-server", false, false));
                    }
                } else {
                    startedServer = true;
                    rValue = Math.max(rValue, startRHQServerForInstallation());
                    int installerStatusCode = runRHQServerInstaller();
                    rValue = Math.max(rValue, installerStatusCode);
                    if (installerStatusCode == RHQControl.EXIT_CODE_OK) {
                        waitForRHQServerToInitialize();
                    }
                }
            }

            if (installAgent) {
                if (isAgentInstalled()) {
                    log.info("The RHQ agent is already installed in [" + getAgentBasedir()
                        + "]. It will not be installed.");

                    if (isWindows()) {
                        try {
                            log.info("Ensuring the RHQ Agent Windows service exists. Ignore any CreateService failure.");
                            rValue = Math.max(rValue, installWindowsService(new File(getAgentBasedir(), "bin"), "rhq-agent-wrapper", false, false));
                        } catch (Exception e) {
                            // Ignore, service may already exist or be running, wrapper script returns 1
                            log.debug("Failed to stop agent service", e);
                        }
                    }
                } else {
                    File agentBasedir = getAgentBasedir();
                    installAgent(agentBasedir, commandLine);

                    rValue = Math.max(rValue, updateWindowsAgentService(agentBasedir));

                    if (start) {
                        rValue = Math.max(rValue, startAgent(agentBasedir));
                    }
                }
            }

        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the install command", e);

        } finally {
            if (!start && (startedStorage || startedServer)) {
                Stop stopCommand = new Stop();
                if (startedServer) {
                    rValue = Math.max(rValue, stopCommand.exec(new String[] { "--server" }));
                }
                if (startedStorage) {
                    rValue = Math.max(rValue, stopCommand.exec(new String[] { "--storage" }));
                }
            }
        }
        return rValue;
    }

    private List<String> validateOptions(CommandLine commandLine) {
        List<String> errors = new LinkedList<String>();

        if (!(commandLine.hasOption(STORAGE_OPTION) || commandLine.hasOption(SERVER_OPTION) || commandLine
            .hasOption(AGENT_OPTION))) {

            validateCustomStorageDataDirectories(commandLine, errors);

            if (commandLine.hasOption(AGENT_CONFIG_OPTION) && !isAgentInstalled()) {
                File agentConfig = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                validateAgentConfigOption(agentConfig, errors);
            }
        } else {
            if (commandLine.hasOption(STORAGE_OPTION)) {
                if (!isAgentInstalled() && commandLine.hasOption(AGENT_CONFIG_OPTION)) {
                    File agentConfig = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                    validateAgentConfigOption(agentConfig, errors);
                }

                validateCustomStorageDataDirectories(commandLine, errors);
            }

            if (commandLine.hasOption(AGENT_OPTION) && !isAgentInstalled()
                && commandLine.hasOption(AGENT_CONFIG_OPTION)) {
                File agentConfig = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                validateAgentConfigOption(agentConfig, errors);
            }
        }

        return errors;
    }

    private void validateAgentConfigOption(File agentConfig, List<String> errors) {
        if (!agentConfig.exists()) {
            errors.add("The --agent-config option has as its value a file that does not exist ["
                + agentConfig.getAbsolutePath() + "]");
        } else if (agentConfig.isDirectory()) {
            errors.add("The --agent-config option has as its value a path that is a directory ["
                + agentConfig.getAbsolutePath() + "]. It should be an XML file that replaces the default "
                + "agent-configuration.xml");
        }
    }
}
