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
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.file.FileReverter;
import org.rhq.core.util.file.FileUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Install extends AbstractInstall {

    private static final String FROM_AGENT_DIR_OPTION = "from-agent-dir";

    private Options options;

    public Install() {
        options = new Options()
            .addOption(
                null,
                FROM_AGENT_DIR_OPTION,
                true,
                "Full path to the install directory of the existing RHQ Agent. This is used when installing a "
                    + "new RHQ HA Server, or an RHQ Storage Node, to a machine with an existing RHQ Agent installed. "
                    + "The existing agent will be updated as needed and relocated into the default location: "
                    + "<server-dir>/../rhq-agent.")
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
            final File serverPropsFile = getServerPropertiesFile();
            final FileReverter serverPropFileReverter = new FileReverter(serverPropsFile);
            addUndoTask(new ControlCommand.UndoTask("Reverting server properties file") {
                public void performUndoWork() throws Exception {
                    try {
                        serverPropFileReverter.revert();
                    } catch (Exception e) {
                        throw new Exception("Cannot reset rhq-server.properties - revert settings manually", e);
                    }
                }
            });

            // If using non-default agent location then save it so it will be applied to all subsequent rhqctl commands.
            boolean hasFromAgentOption = commandLine.hasOption(FROM_AGENT_DIR_OPTION);
            File fromAgentDir = null;
            if (hasFromAgentOption) {
                // stop the existing agent  if it is running, this validates the path as well
                log.info("Stopping the existing agent, if running...");
                fromAgentDir = getFromAgentDir(commandLine);
                rValue = Math.max(rValue, killAgent(fromAgentDir));
            }

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

            if ((startStorage || installServer) && rValue == RHQControl.EXIT_CODE_OK) {
                startedStorage = true;
                Start startCommand = new Start();
                rValue = Math.max(rValue, startCommand.exec(new String[] { "--storage" }));

                // More recent versions of Cassandra are taking a little longer to lay down the initial
                // files on the first startup.  Pause for 10s to ensure that Cassandra is ready for the
                // Server install to connect with the default 'cassandra' user and update the schema.
                if (installServer) {
                    log.info("Pausing to ensure RHQ Storage is initialized prior to RHQ Server installation.");
                    Thread.sleep(10000L);
                }
            }

            if (installServer && rValue == RHQControl.EXIT_CODE_OK) {
                if (isServerInstalled()) {
                    log.info("The RHQ server is already installed. It will not be installed.");

                    if (isWindows()) {
                        log.info("Ensuring the RHQ Server Windows service exists. Ignore any CreateService failure.");
                        rValue = Math.max(rValue, installWindowsService(getBinDir(), "rhq-server", false, false));
                    }
                } else {
                    startedServer = true;
                    rValue = Math.max(rValue, startRHQServerForInstallation());
                    Future<Integer> integerFuture = runRHQServerInstaller();
                    waitForRHQServerToInitialize(integerFuture);
                    rValue = Math.max(rValue, integerFuture.get());
                }
            }

            if (installAgent && rValue == RHQControl.EXIT_CODE_OK) {
                final Properties serverProps = new PropertiesFileUpdate(serverPropsFile).loadExistingProperties();
                final String embeddedAgentProp = serverProps.getProperty("rhq.server.embedded-agent.enabled");
                if (!Boolean.parseBoolean(embeddedAgentProp)) {
                    if (isAgentInstalled()) {
                        log.info("The RHQ agent is already installed in [" + getAgentBasedir()
                            + "]. It will not be installed.");

                        if (isWindows()) {
                            try {
                                log.info("Ensuring the RHQ Agent Windows service exists. Ignore any CreateService failure.");
                                rValue = Math.max(
                                    rValue,
                                    installWindowsService(new File(getAgentBasedir(), "bin"), "rhq-agent-wrapper", false,
                                        false));
                            } catch (Exception e) {
                                // Ignore, service may already exist or be running, wrapper script returns 1
                                log.debug("Failed to stop agent service", e);
                            }
                        }
                    } else {
                        final File agentBasedir = getAgentBasedir();

                        if (!hasFromAgentOption) {
                            installAgent(agentBasedir, commandLine);

                        } else {
                            // update the existing agent, it may be out of date, and then move it to the proper location
                            File agentInstallerJar = getFileDownload("rhq-agent", "rhq-enterprise-agent");

                            rValue = Math.max(rValue, updateAndMoveExistingAgent(agentBasedir, fromAgentDir, agentInstallerJar));

                            addUndoTask(new ControlCommand.UndoTask("Removing agent install directory") {
                                public void performUndoWork() {
                                    FileUtil.purge(agentBasedir, true);
                                }
                            });

                            log.info("The agent has been upgraded and placed in: " + agentBasedir);
                        }

                        rValue = Math.max(rValue, updateWindowsAgentService(agentBasedir));

                        if (start) {
                            rValue = Math.max(rValue, startAgent(agentBasedir));
                        }
                    }
                } else {
                    log.info("The embedded agent has been enabled in the server configuration, so the standalone agent will not be installed");
                }
            }

        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the install command", e);

        } finally {
            if (!start && (startedStorage || startedServer)) {
                Stop stopCommand = new Stop();
                if (startedServer) {
                    try {
                        rValue = Math.max(rValue, stopCommand.exec(new String[] { "--server" }));
                    } catch (Exception e) {
                        log.warn("Could not stop the server - it may still be running.");
                    }
                }
                if (startedStorage) {
                    try {
                        rValue = Math.max(rValue, stopCommand.exec(new String[] { "--storage" }));
                    } catch (Exception e) {
                        log.warn("Could not stop the storage node - it may still be running.");
                    }
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

        if (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
            if (isAgentInstalled()) {
                errors.add("An Agent is already installed. The " + FROM_AGENT_DIR_OPTION + " option is not allowed.");
            } else {
                File fromAgentDir = new File(commandLine.getOptionValue(FROM_AGENT_DIR_OPTION));
                if (!fromAgentDir.isDirectory()) {
                    errors.add("The " + FROM_AGENT_DIR_OPTION + " directory does not exist: [" + fromAgentDir.getPath()
                        + "]");
                } else {
                    String agentEnvFileName = (File.separatorChar == '/') ? "bin/rhq-agent-env.sh"
                        : "bin/rhq-agent-env.bat";
                    File agentEnvFile = new File(fromAgentDir, agentEnvFileName);
                    if (!agentEnvFile.isFile()) {
                        errors.add("The " + FROM_AGENT_DIR_OPTION
                            + " directory does not appear to be an RHQ Agent installation. Missing expected file: ["
                            + agentEnvFile.getPath() + "]");
                    }
                }
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
