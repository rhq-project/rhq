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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Install extends AbstractInstall {

    private final String AGENT_CONFIG_OPTION = "agent-config";
    private final String AGENT_PREFERENCE = "agent-preference";
    private final String AGENT_AUTOSTART_OPTION = "agent-auto-start";

    private final String SERVER_CONFIG_OPTION = "server-config";

    private Options options;

    // some known agent preference setting names
    private static final String PREF_RHQ_AGENT_SECURITY_TOKEN = "rhq.agent.security-token";
    private static final String PREF_RHQ_AGENT_CONFIGURATION_SETUP_FLAG = "rhq.agent.configuration-setup-flag";
    private static final String PREF_RHQ_AGENT_AUTO_UPDATE_FLAG = "rhq.agent.agent-update.enabled";

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
            .addOption(null, SERVER_CONFIG_OPTION, true,
                "An alternate properties file to use in place of the default rhq-server.properties")
            .addOption(null, AGENT_CONFIG_OPTION, true,
                "An alternate XML file to use in place of the default agent-configuration.xml")
            .addOption(
                null,
                STORAGE_CONFIG_OPTION,
                true,
                "A properties file with keys that correspond to option names "
                    + "of the storage installer. Each property will be translated into an option that is passed to the "
                    + "storage installer.")
            .addOption(
                null,
                AGENT_PREFERENCE,
                true,
                "An agent preference setting (whose argument is in the form 'name=value') to be set in the agent. More than one of these is allowed.")
            .addOption(
                null,
                AGENT_AUTOSTART_OPTION,
                true,
                "If an agent is to be installed it will, by default, also be started. However, if this option is set to false, the agent will not be started after it gets installed.")
            .addOption(null, STORAGE_DATA_ROOT_DIR, true,
                "The root directory under which all storage data directories will be placed.");

        options.getOption(AGENT_AUTOSTART_OPTION).setOptionalArg(true);
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
    protected void exec(CommandLine commandLine) {
        try {
            List<String> errors = validateOptions(commandLine);
            if (!errors.isEmpty()) {
                for (String error : errors) {
                    log.error(error);
                }
                log.error("Exiting due to the previous errors");
                return;
            }

            // if no options specified, then install whatever is not installed yet
            if (!(commandLine.hasOption(STORAGE_OPTION) || commandLine.hasOption(SERVER_OPTION) || commandLine
                .hasOption(AGENT_OPTION))) {

                replaceServerPropertiesIfNecessary(commandLine);

                if (!isStorageInstalled()) {
                    installStorageNode(getStorageBasedir(), commandLine);
                } else if (isWindows()) {
                    installWindowsService(getBinDir(), "rhq-storage", true);
                }

                if (!isServerInstalled()) {
                    startRHQServerForInstallation();
                    runRHQServerInstaller();
                    waitForRHQServerToInitialize();
                } else if (isWindows()) {
                    installWindowsService(getBinDir(), "rhq-server", true);
                }

                if (!isAgentInstalled()) {
                    clearAgentPreferences();
                    File agentBasedir = getAgentBasedir();
                    installAgent(agentBasedir);
                    configureAgent(agentBasedir, commandLine);
                    boolean start = Boolean.parseBoolean(commandLine.getOptionValue(AGENT_AUTOSTART_OPTION, "true"));
                    if (start) {
                        startAgent(agentBasedir, true);
                    } else {
                        log.info("The agent was installed but was told not to start automatically.");
                    }
                } else if (isWindows()) {
                    boolean start = Boolean.parseBoolean(commandLine.getOptionValue(AGENT_AUTOSTART_OPTION, "true"));
                    installWindowsService(new File(getAgentBasedir(), "bin"), "rhq-agent-wrapper", start);
                }

            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        log.info("The RHQ storage node is already installed in " + new File(getBaseDir(), "storage"));

                        if (isWindows()) {
                            installWindowsService(getBinDir(), "rhq-storage", true);
                        } else {
                            log.info("Skipping storage node installation.");
                        }
                    } else {
                        replaceServerPropertiesIfNecessary(commandLine);
                        installStorageNode(getStorageBasedir(), commandLine);
                    }

                    if (!isAgentInstalled()) {
                        File agentBasedir = getAgentBasedir();
                        clearAgentPreferences();
                        installAgent(agentBasedir);
                        configureAgent(agentBasedir, commandLine);
                        if (Boolean.parseBoolean(commandLine.getOptionValue(AGENT_AUTOSTART_OPTION, "true"))) {
                            startAgent(agentBasedir, true);
                        } else {
                            log.info("The agent was installed but was told not to start automatically.");
                        }
                    }
                }

                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        log.warn("The RHQ server is already installed.");

                        if (isWindows()) {
                            installWindowsService(getBinDir(), "rhq-server", true);
                        } else {
                            log.info("Skipping server installation.");
                        }

                    } else {
                        replaceServerPropertiesIfNecessary(commandLine);
                        startRHQServerForInstallation();
                        runRHQServerInstaller();
                        waitForRHQServerToInitialize();
                    }
                }

                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled() && !commandLine.hasOption(STORAGE_OPTION)) {
                        log.info("The RHQ agent is already installed in [" + getAgentBasedir() + "]");

                        boolean start = Boolean
                            .parseBoolean(commandLine.getOptionValue(AGENT_AUTOSTART_OPTION, "true"));
                        if (isWindows()) {
                            installWindowsService(new File(getAgentBasedir(), "bin"), "rhq-agent-wrapper", start);
                        } else {
                            log.info("Skipping agent installation.");
                        }

                    } else {
                        File agentBasedir = getAgentBasedir();
                        clearAgentPreferences();
                        installAgent(agentBasedir);
                        configureAgent(agentBasedir, commandLine);
                        if (Boolean.parseBoolean(commandLine.getOptionValue(AGENT_AUTOSTART_OPTION, "true"))) {
                            startAgent(agentBasedir, true);
                        } else {
                            log.info("The agent was installed but was told not to start automatically.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the install command", e);
        }
    }

    private List<String> validateOptions(CommandLine commandLine) {
        List<String> errors = new LinkedList<String>();

        if (!(commandLine.hasOption(STORAGE_OPTION) || commandLine.hasOption(SERVER_OPTION) || commandLine
            .hasOption(AGENT_OPTION))) {

            validateCustomStorageDataDirectories(commandLine, errors);

            if (commandLine.hasOption(SERVER_CONFIG_OPTION) && !isServerInstalled()) {
                File serverConfig = new File(commandLine.getOptionValue(SERVER_CONFIG_OPTION));
                validateServerConfigOption(serverConfig, errors);
            }

            if (commandLine.hasOption(AGENT_CONFIG_OPTION) && !isAgentInstalled()) {
                File agentConfig = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                validateAgentConfigOption(agentConfig, errors);
            }

            if (commandLine.hasOption(STORAGE_CONFIG_OPTION) && !isStorageInstalled()) {
                File storageConfig = new File(commandLine.getOptionValue(STORAGE_CONFIG_OPTION));
                validateStorageConfigOption(storageConfig, errors);
            }
        } else {
            if (commandLine.hasOption(STORAGE_OPTION)) {
                if (!isStorageInstalled() && commandLine.hasOption(STORAGE_CONFIG_OPTION)) {
                    File storageConfig = new File(commandLine.getOptionValue(STORAGE_CONFIG_OPTION));
                    validateStorageConfigOption(storageConfig, errors);
                }

                if (!isAgentInstalled() && commandLine.hasOption(AGENT_CONFIG_OPTION)) {
                    File agentConfig = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                    validateAgentConfigOption(agentConfig, errors);
                }

                validateCustomStorageDataDirectories(commandLine, errors);
            }

            if (commandLine.hasOption(SERVER_OPTION) && !isStorageInstalled()
                && commandLine.hasOption(SERVER_CONFIG_OPTION)) {
                File serverConfig = new File(commandLine.getOptionValue(SERVER_CONFIG_OPTION));
                validateServerConfigOption(serverConfig, errors);
            }

            if (commandLine.hasOption(AGENT_OPTION) && !isAgentInstalled()
                && commandLine.hasOption(AGENT_CONFIG_OPTION)) {
                File agentConfig = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                validateAgentConfigOption(agentConfig, errors);
            }
        }

        return errors;
    }

    private void validateServerConfigOption(File serverConfig, List<String> errors) {
        if (!serverConfig.exists()) {
            errors.add("The --server-config option has as its value a file that does not exist ["
                + serverConfig.getAbsolutePath() + "]");
        } else if (serverConfig.isDirectory()) {
            errors.add("The --server-config option has as its value a path that is a directory ["
                + serverConfig.getAbsolutePath() + "]. It should be a properties file that replaces the "
                + "default rhq-server.properties");
        }
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

    private void validateStorageConfigOption(File storageConfig, List<String> errors) {
        if (!storageConfig.exists()) {
            errors.add("The --storage-config option has as its value a file that does not exist ["
                + storageConfig.getAbsolutePath() + "]");
        } else if (storageConfig.isDirectory()) {
            errors.add("The --storage-config option has as its value a path that is a directory ["
                + storageConfig.getAbsolutePath() + "]. It should be a properties file with keys that "
                + "correspond to options for the storage installer.");
        }
    }

    private void installAgent(File agentBasedir) throws IOException {
        try {
            log.info("Installing RHQ agent");

            File agentInstallerJar = getAgentInstaller();

            putProperty(RHQ_AGENT_BASEDIR_PROP, agentBasedir.getAbsolutePath());

            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine("java")
                .addArgument("-jar").addArgument(agentInstallerJar.getAbsolutePath())
                .addArgument("--install=" + agentBasedir.getParentFile().getAbsolutePath())
                .addArgument("--log=" + new File(getLogDir(), "rhq-agent-update.log"));

            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getBaseDir());
            executor.setStreamHandler(new PumpStreamHandler());

            int exitValue = executor.execute(commandLine);
            log.info("The agent installer finished running with exit value " + exitValue);
        } catch (IOException e) {
            log.error("An error occurred while running the agent installer: " + e.getMessage());
            throw e;
        }
    }

    private File getAgentInstaller() {
        File agentDownloadDir = new File(getBaseDir(),
            "modules/org/rhq/server-startup/main/deployments/rhq.ear/rhq-downloads/rhq-agent");
        return agentDownloadDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains("rhq-enterprise-agent");
            }
        })[0];
    }

    private void configureAgent(File agentBasedir, CommandLine commandLine) throws Exception {
        // If the user provided us with an agent config file, we will use it.
        // Otherwise, we are going to use the out-of-box agent config file.
        //
        // Because we want to accept all defaults and consider the agent fully configured, we need to set
        //    rhq.agent.configuration-setup-flag=true
        // This tells the agent not to ask any setup questions at startup.
        // We do this whether using a custom config file or the default config file - this is because
        // we cannot allow the agent to ask the setup questions (rhqctl doesn't support that).
        //
        // Note that agent preferences found in the config file can be overridden with
        // the AGENT_PREFERENCE settings (you can set more than one).
        try {
            File agentConfDir = new File(agentBasedir, "conf");
            File agentConfigFile = new File(agentConfDir, "agent-configuration.xml");

            if (commandLine.hasOption(AGENT_CONFIG_OPTION)) {
                log.info("Configuring the RHQ agent with custom configuration file: "
                    + commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                replaceAgentConfigIfNecessary(commandLine);
            } else {
                log.info("Configuring the RHQ agent with default configuration file: " + agentConfigFile);
            }

            // we require our agent preference node to be the user node called "default"
            Preferences preferencesNode = getAgentPreferences();

            // read the comments in AgentMain.loadConfigurationFile(String) to know why we do all of this
            String securityToken = preferencesNode.get(PREF_RHQ_AGENT_SECURITY_TOKEN, null);
            ByteArrayOutputStream rawConfigFileData = new ByteArrayOutputStream();
            StreamUtil.copy(new FileInputStream(agentConfigFile), rawConfigFileData, true);
            String newConfig = rawConfigFileData.toString().replace("${rhq.agent.preferences-node}", "default");
            ByteArrayInputStream newConfigInputStream = new ByteArrayInputStream(newConfig.getBytes());
            Preferences.importPreferences(newConfigInputStream);
            if (securityToken != null) {
                preferencesNode.put(PREF_RHQ_AGENT_SECURITY_TOKEN, securityToken);
            }

            overrideAgentPreferences(commandLine, preferencesNode);

            // set some prefs that must be a specific value
            // - do not tell this agent to auto-update itself - this agent must be managed by rhqctl only
            // - set the config setup flag to true to prohibit the agent from asking setup questions at startup
            String agentUpdateEnabledPref = PREF_RHQ_AGENT_AUTO_UPDATE_FLAG;
            preferencesNode.putBoolean(agentUpdateEnabledPref, false);
            String setupPref = PREF_RHQ_AGENT_CONFIGURATION_SETUP_FLAG;
            preferencesNode.putBoolean(setupPref, true);

            preferencesNode.flush();
            preferencesNode.sync();

            log.info("Finished configuring the agent");
        } catch (Exception e) {
            log.error("An error occurred while configuring the agent: " + e.getMessage());
            throw e;
        }
    }

    private void overrideAgentPreferences(CommandLine commandLine, Preferences preferencesNode) {
        // override the out of box config with user custom agent preference values
        String[] customPrefs = commandLine.getOptionValues(AGENT_PREFERENCE);
        if (customPrefs != null && customPrefs.length > 0) {
            for (String nameValuePairString : customPrefs) {
                String[] nameValuePairArray = nameValuePairString.split("=", 2);
                String prefName = nameValuePairArray[0];
                String prefValue = nameValuePairArray.length == 1 ? "true" : nameValuePairArray[1];
                log.info("Overriding agent preference: " + prefName + "=" + prefValue);
                preferencesNode.put(prefName, prefValue);
            }
        }
        return;
    }

    private void clearAgentPreferences() throws Exception {
        log.info("Removing any existing agent preferences from default preference node");

        // remove everything EXCEPT the security token
        Preferences agentPrefs = getAgentPreferences();
        String[] prefKeys = null;

        try {
            prefKeys = agentPrefs.keys();
        } catch (Exception e) {
            log.warn("Failed to get agent preferences - cannot clear them: " + e);
        }

        if (prefKeys != null && prefKeys.length > 0) {
            for (String prefKey : prefKeys) {
                if (!prefKey.equals(PREF_RHQ_AGENT_SECURITY_TOKEN)) {
                    agentPrefs.remove(prefKey);
                }
            }
            agentPrefs.flush();
            Preferences.userRoot().sync();
        }
    }

    private Preferences getAgentPreferences() {
        Preferences agentPrefs = Preferences.userRoot().node("rhq-agent/default");
        return agentPrefs;
    }

    private void replaceServerPropertiesIfNecessary(CommandLine commandLine) {
        if (commandLine.hasOption(SERVER_CONFIG_OPTION) && !isServerInstalled()) {
            replaceServerProperties(new File(commandLine.getOptionValue(SERVER_CONFIG_OPTION)));
        }
    }

    private void replaceServerProperties(File newServerProperties) {
        File defaultServerProps = new File(System.getProperty("rhq.server.properties-file"));
        defaultServerProps.delete();
        try {
            StreamUtil.copy(new FileReader(newServerProperties), new FileWriter(defaultServerProps));
        } catch (IOException e) {
            throw new RHQControlException("Failed to replace " + defaultServerProps + " with " + newServerProperties, e);
        }
    }

    private void replaceAgentConfigIfNecessary(CommandLine commandLine) {
        if (!commandLine.hasOption(AGENT_CONFIG_OPTION)) {
            return;
        }
        File newConfigFile = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));

        File confDir = new File(getAgentBasedir(), "conf");
        File defaultConfigFile = new File(confDir, "agent-configuration.xml");
        defaultConfigFile.delete();
        try {
            StreamUtil.copy(new FileReader(newConfigFile), new FileWriter(defaultConfigFile));
        } catch (IOException e) {
            throw new RHQControlException(("Failed to replace " + defaultConfigFile + " with " + newConfigFile));
        }
    }
}
