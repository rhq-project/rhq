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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.DeploymentJBossASClient;
import org.rhq.common.jbossas.client.controller.MCCHelper;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Install extends ControlCommand {

    private final String STORAGE_CONFIG_OPTION = "storage-config";
    private final String STORAGE_DATA_ROOT_DIR = "storage-data-root-dir";

    private final String AGENT_CONFIG_OPTION = "agent-config";
    private final String AGENT_PREFERENCE = "agent-preference";
    private final String AGENT_NO_START = "agent-no-start";

    private final String SERVER_CONFIG_OPTION = "server-config";

    private final String STORAGE_CONFIG_PROP = "rhqctl.install.storage-config";

    private final File DEFAULT_STORAGE_BASEDIR = new File(getBaseDir(), STORAGE_BASEDIR_NAME);
    private final File DEFAULT_AGENT_BASEDIR = new File(getBaseDir(), AGENT_BASEDIR_NAME);

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
                "Install RHQ storage node. The install directory will be: "
                    + DEFAULT_STORAGE_BASEDIR
                    + ". Note that this option implies --agent which means an agent will also be installed, if one is not yet installed.")
            .addOption(
                null,
                "server",
                false,
                "Install RHQ server. If you have not yet installed an RHQ storage node somewhere in your network, you must specify --storage to install one.")
            .addOption(null, "agent", false,
                "Install RHQ agent. The install directory will be: " + DEFAULT_AGENT_BASEDIR)
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
                    + " storage installer. See example.storage.properties for examples.")
            .addOption(
                null,
                AGENT_PREFERENCE,
                true,
                "An agent preference setting (whose argument is in the form 'name=value') to be set in the agent. More than one of these is allowed.")
            .addOption(
                null,
                AGENT_NO_START,
                true,
                "If an agent is to be installed it will, by default, also be started. However, if this option is set to true, the agent will not be started after it gets installed.")
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
                    installStorageNode(getStorageBasedir(commandLine), commandLine);
                } else if (isWindows()) {
                    installWindowsService(getBinDir(), "rhq-storage");
                }

                if (!isServerInstalled()) {
                    startRHQServerForInstallation();
                    installRHQServer();
                    waitForRHQServerToInitialize();
                } else if (isWindows()) {
                    installWindowsService(getBinDir(), "rhq-server");
                }

                if (!isAgentInstalled()) {
                    clearAgentPreferences();
                    File agentBasedir = getAgentBasedir(commandLine);
                    installAgent(agentBasedir);
                    configureAgent(agentBasedir, commandLine);
                    if (!Boolean.parseBoolean(commandLine.getOptionValue(AGENT_NO_START, "false"))) {
                        startAgent(agentBasedir);
                    } else {
                        log.info("The agent was installed but was told not to start automatically.");
                    }
                } else if (isWindows()) {
                    installWindowsService(new File(getBaseDir(), "rhq-agent/bin"), "rhq-agent-wrapper");
                }

            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        log.info("The RHQ storage node is already installed in " + new File(getBaseDir(), "storage"));

                        if (isWindows()) {
                            installWindowsService(getBinDir(), "rhq-storage");
                        } else {
                            log.info("Skipping storage node installation.");
                        }
                    } else {
                        replaceServerPropertiesIfNecessary(commandLine);
                        installStorageNode(getStorageBasedir(commandLine), commandLine);
                    }

                    if (!isAgentInstalled()) {
                        File agentBasedir = getAgentBasedir(commandLine);
                        clearAgentPreferences();
                        installAgent(agentBasedir);
                        configureAgent(agentBasedir, commandLine);
                        if (!Boolean.parseBoolean(commandLine.getOptionValue(AGENT_NO_START, "false"))) {
                            startAgent(agentBasedir);
                        } else {
                            log.info("The agent was installed but was told not to start automatically.");
                        }
                    }
                }

                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        log.warn("The RHQ server is already installed.");

                        if (isWindows()) {
                            installWindowsService(getBinDir(), "rhq-server");
                        } else {
                            log.info("Skipping server installation.");
                        }

                    } else {
                        replaceServerPropertiesIfNecessary(commandLine);
                        startRHQServerForInstallation();
                        installRHQServer();
                        waitForRHQServerToInitialize();
                    }
                }

                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled() && !commandLine.hasOption(STORAGE_OPTION)) {
                        log.info("The RHQ agent is already installed in " + new File(getBaseDir(), "rhq-agent"));

                        if (isWindows()) {
                            installWindowsService(new File(getBaseDir(), "rhq-agent/bin"), "rhq-agent-wrapper");
                        } else {
                            log.info("Skipping agent installation.");
                        }

                    } else {
                        File agentBasedir = getAgentBasedir(commandLine);
                        clearAgentPreferences();
                        installAgent(agentBasedir);
                        configureAgent(agentBasedir, commandLine);
                        if (!Boolean.parseBoolean(commandLine.getOptionValue(AGENT_NO_START, "false"))) {
                            startAgent(agentBasedir);
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

    private void installWindowsService(File workingDir, String batFile) throws Exception {
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        commandLine = getCommandLine(batFile, "stop");
        executor.execute(commandLine);

        commandLine = getCommandLine(batFile, "remove");
        executor.execute(commandLine);

        commandLine = getCommandLine(batFile, "install");
        executor.execute(commandLine);
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

    private void validateCustomStorageDataDirectories(CommandLine commandLine, List<String> errors) {
        StorageDataDirectories customDataDirs = getCustomStorageDataDirectories(commandLine);
        if (customDataDirs != null) {
            if (customDataDirs.basedir.isAbsolute()) {
                if (!isDirectoryEmpty(customDataDirs.dataDir)) {
                    errors.add("Storage data directory [" + customDataDirs.dataDir + "] is not empty.");
                }
                if (!isDirectoryEmpty(customDataDirs.commitlogDir)) {
                    errors.add("Storage commitlog directory [" + customDataDirs.commitlogDir + "] is not empty.");
                }
                if (!isDirectoryEmpty(customDataDirs.savedcachesDir)) {
                    errors.add("Storage saved-caches directory [" + customDataDirs.savedcachesDir + "] is not empty.");
                }
            } else {
                errors.add("The storage root directory [" + customDataDirs.basedir
                    + "] must be specified with an absolute path and should be outside of the main install directory.");
            }
        }
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

    private File getStorageBasedir(CommandLine cmdLine) {
        return DEFAULT_STORAGE_BASEDIR;
    }

    private File getAgentBasedir(CommandLine cmdLine) {
        return DEFAULT_AGENT_BASEDIR;
    }

    private int installStorageNode(File storageBasedir, CommandLine rhqctlCommandLine) throws IOException {
        try {
            log.info("Preparing to install RHQ storage node.");

            putProperty(RHQ_STORAGE_BASEDIR_PROP, storageBasedir.getAbsolutePath());

            org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-storage-installer", "--dir",
                storageBasedir.getAbsolutePath());

            if (rhqctlCommandLine.hasOption(STORAGE_DATA_ROOT_DIR)) {
                StorageDataDirectories dataDirs;
                dataDirs = getCustomStorageDataDirectories(rhqctlCommandLine);
                commandLine.addArguments(new String[] { "--data", dataDirs.dataDir.getAbsolutePath() });
                commandLine.addArguments(new String[] { "--commitlog", dataDirs.commitlogDir.getAbsolutePath() });
                commandLine.addArguments(new String[] { "--saved-caches", dataDirs.savedcachesDir.getAbsolutePath() });
            }

            if (rhqctlCommandLine.hasOption(STORAGE_CONFIG_OPTION)) {
                String[] args = toArray(loadStorageProperties(rhqctlCommandLine.getOptionValue(STORAGE_CONFIG_OPTION)));
                commandLine.addArguments(args);
            } else if (hasProperty(STORAGE_CONFIG_PROP)) {
                String[] args = toArray(loadStorageProperties(getProperty(STORAGE_CONFIG_PROP)));
                commandLine.addArguments(args);
            }

            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getBinDir());
            executor.setStreamHandler(new PumpStreamHandler());

            int exitCode = executor.execute(commandLine);
            log.info("The storage node installer has finished with an exit value of " + exitCode);
            return exitCode;
        } catch (IOException e) {
            log.error("An error occurred while running the storage installer: " + e.getMessage());
            if (e.getMessage().toLowerCase().contains("exit value: 3")) {
                log.error("Try to point your root data directory via --" + STORAGE_DATA_ROOT_DIR
                    + " to a directory where you have read and write permissions.");
            }
            throw e;
        }
    }

    private class StorageDataDirectories {
        public File basedir; // the other three will be under this base directory
        public File dataDir;
        public File commitlogDir;
        public File savedcachesDir;
    }

    private StorageDataDirectories getCustomStorageDataDirectories(CommandLine commandLine) {
        StorageDataDirectories storageDataDirs = null;

        if (commandLine.hasOption(STORAGE_DATA_ROOT_DIR)) {
            storageDataDirs = new StorageDataDirectories();
            storageDataDirs.basedir = new File(commandLine.getOptionValue(STORAGE_DATA_ROOT_DIR));
            storageDataDirs.dataDir = new File(storageDataDirs.basedir, "data");
            storageDataDirs.commitlogDir = new File(storageDataDirs.basedir, "commitlog");
            storageDataDirs.savedcachesDir = new File(storageDataDirs.basedir, "saved_caches");
        }

        return storageDataDirs;
    }

    private boolean isDirectoryEmpty(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            return (files == null || files.length == 0);
        } else {
            return true;
        }
    }

    private Properties loadStorageProperties(String path) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(path)));

        return properties;
    }

    private String[] toArray(Properties properties) {
        String[] array = new String[properties.size() * 2];
        int i = 0;
        for (Object key : properties.keySet()) {
            array[i++] = "--" + (String) key;
            array[i++] = properties.getProperty((String) key);
        }
        return array;
    }

    private void startRHQServerForInstallation() throws IOException {
        try {
            log.info("The RHQ Server must be started to complete its installation. Starting the RHQ server in preparation of running the server installer...");

            // when you unzip the distro, you are getting a fresh, unadulterated, out-of-box EAP installation, which by default listens
            // to port 9999 for its native management subsystem. Make sure some other independent EAP server (or anything for that matter)
            // isn't already listening to that port.
            if (isPortInUse("127.0.0.1", 9999)) {
                throw new IOException(
                    "Something is already listening to port 9999 - shut it down before installing the server.");
            }

            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getBinDir());
            executor.setStreamHandler(new PumpStreamHandler());
            org.apache.commons.exec.CommandLine commandLine;

            if (isWindows()) {
                // For windows we will [re-]install the server as a windows service, then start the service.

                commandLine = getCommandLine("rhq-server", "stop");
                executor.execute(commandLine);

                commandLine = getCommandLine("rhq-server", "remove");
                executor.execute(commandLine);

                commandLine = getCommandLine("rhq-server", "install");
                executor.execute(commandLine);

                commandLine = getCommandLine("rhq-server", "start");
                executor.execute(commandLine);

            } else {
                // For *nix, just start the server in the background
                commandLine = getCommandLine("rhq-server", "start");
                executor.execute(commandLine, new DefaultExecuteResultHandler());
            }

            // Wait for the server to complete it's startup
            log.info("Waiting for the RHQ Server to start in preparation of running the server installer...");
            commandLine = getCommandLine("rhq-installer", "--test");

            Executor installerExecutor = new DefaultExecutor();
            installerExecutor.setWorkingDirectory(getBinDir());
            installerExecutor.setStreamHandler(new PumpStreamHandler());

            int exitCode = 0;
            int numTries = 0, maxTries = 30;
            do {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    // just keep going
                }
                if (numTries++ > maxTries) {
                    throw new IOException("Failed to detect server initialization, max tries exceeded. Aborting...");
                }
                if (numTries > 1) {
                    log.info("Still waiting to run the server installer...");
                }
                exitCode = installerExecutor.execute(commandLine);

            } while (exitCode != 0);

            log.info("The RHQ Server is ready for the server installer to run.");

        } catch (IOException e) {
            log.error("An error occurred while starting the RHQ server: " + e.getMessage());
            throw e;
        }
    }

    private void installRHQServer() throws IOException {
        try {
            log.info("Installing RHQ server");

            org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-installer");
            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getBinDir());
            executor.setStreamHandler(new PumpStreamHandler());

            executor.execute(commandLine, new DefaultExecuteResultHandler());
            log.info("The server installer is running");
        } catch (IOException e) {
            log.error("An error occurred while starting the server installer: " + e.getMessage());
        }
    }

    private void waitForRHQServerToInitialize() throws Exception {
        try {
            final long messageInterval = 30000L;
            final long problemMessageInterval = 120000L;
            long timerStart = System.currentTimeMillis();
            long intervalStart = timerStart;

            while (!isRHQServerInitialized()) {
                Long now = System.currentTimeMillis();

                if ((now - intervalStart) > messageInterval) {
                    long totalWait = (now - timerStart);

                    if (totalWait < problemMessageInterval) {
                        log.info("Still waiting for server to start...");

                    } else {
                        long minutes = totalWait / 60000;
                        log.info("It has been over ["
                            + minutes
                            + "] minutes - you may want to ensure your server startup is proceeding as expected. You can check the log at ["
                            + new File(getBaseDir(), "logs/server.log").getPath() + "].");

                        timerStart = now;
                    }

                    intervalStart = now;
                }

                Thread.sleep(5000);
            }

        } catch (IOException e) {
            log.error("An error occurred while checking to see if the server is initialized: " + e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            // Don't think we need to log any details here
            throw e;
        }
    }

    private boolean isRHQServerInitialized() throws IOException {

        BufferedReader reader = null;
        ModelControllerClient mcc = null;
        Properties props = new Properties();

        try {
            File propsFile = new File(getBaseDir(), "bin/rhq-server.properties");
            reader = new BufferedReader(new FileReader(propsFile));
            props.load(reader);

            String host = (String) props.get("jboss.bind.address.management");
            int port = Integer.valueOf((String) props.get("jboss.management.native.port")).intValue();
            mcc = MCCHelper.getModelControllerClient(host, port);
            DeploymentJBossASClient client = new DeploymentJBossASClient(mcc);
            boolean isDeployed = client.isDeployment("rhq.ear");
            return isDeployed;

        } catch (Throwable t) {
            log.debug("Falling back to logfile check due to: ", t);

            File logDir = new File(getBaseDir(), "logs");
            reader = new BufferedReader(new FileReader(new File(logDir, "server.log")));
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("Server started")) {
                    return true;
                }
                line = reader.readLine();
            }

            return false;

        } finally {
            if (null != mcc) {
                try {
                    mcc.close();
                } catch (Exception e) {
                    // best effort                    
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // best effort
                }
            }
        }
    }

    private void installAgent(File agentBasedir) throws IOException {
        try {
            log.info("Installing RHQ agent");

            File agentInstallerJar = getAgentInstaller();

            putProperty(RHQ_AGENT_BASEDIR_PROP, agentBasedir.getAbsolutePath());

            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine("java")
                .addArgument("-jar").addArgument(agentInstallerJar.getAbsolutePath())
                .addArgument("--install=" + agentBasedir.getParentFile().getAbsolutePath());

            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getBaseDir());
            executor.setStreamHandler(new PumpStreamHandler());

            int exitValue = executor.execute(commandLine);
            log.info("The agent installer finished running with exit value " + exitValue);

            new File(getBaseDir(), "rhq-agent-update.log").delete();
        } catch (IOException e) {
            log.error("An error occurred while running the agent installer: " + e.getMessage());
            throw e;
        }
    }

    private File getAgentInstaller() {
        File agentDownloadDir = new File(getBaseDir(),
            "modules/org/rhq/rhq-enterprise-server-startup-subsystem/main/deployments/rhq.ear/rhq-downloads/rhq-agent");
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

    private void startAgent(File agentBasedir) throws Exception {
        try {
            log.info("Starting RHQ agent");
            Executor executor = new DefaultExecutor();
            File agentBinDir = new File(agentBasedir, "bin");
            executor.setWorkingDirectory(agentBinDir);
            executor.setStreamHandler(new PumpStreamHandler());
            org.apache.commons.exec.CommandLine commandLine;

            if (isWindows()) {
                // For windows we will [re-]install the server as a windows service, then start the service.

                commandLine = getCommandLine("rhq-agent-wrapper", "stop");
                try {
                    executor.execute(commandLine);
                } catch (Exception e) {
                    // Ignore, service may not exist or be running, , script returns 1
                    log.debug("Failed to stop agent service", e);
                }

                commandLine = getCommandLine("rhq-agent-wrapper", "remove");
                try {
                    executor.execute(commandLine);
                } catch (Exception e) {
                    // Ignore, service may not exist, script returns 1
                    log.debug("Failed to uninstall agent service", e);
                }

                commandLine = getCommandLine("rhq-agent-wrapper", "install");
                executor.execute(commandLine);
            }

            // For *nix, just start the server in the background, for Win, now that the service is installed, start it
            commandLine = getCommandLine("rhq-agent-wrapper", "start");
            executor.execute(commandLine);

            log.info("The agent has started up");
        } catch (IOException e) {
            log.error("An error occurred while starting the agent: " + e.getMessage());
            throw e;
        }
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

    private boolean isPortInUse(String host, int port) {
        boolean inUse;

        try {
            Socket testSocket = new Socket(host, port);
            try {
                testSocket.close();
            } catch (Exception ignore) {
            }
            inUse = true;
        } catch (Exception expected) {
            inUse = false;
        }

        return inUse;
    }
}
