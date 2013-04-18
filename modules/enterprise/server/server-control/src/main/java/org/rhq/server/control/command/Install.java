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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.core.util.TokenReplacingReader;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Install extends ControlCommand {

    private final String STORAGE_CONFIG_OPTION = "storage-config";

    private final String AGENT_CONFIG_OPTION = "agent-config";

    private final String SERVER_CONFIG_OPTION = "server-config";

    private final String STORAGE_BASEDIR_NAME = "rhq-storage";

    private final String AGENT_BASEDIR_NAME = "rhq-agent";

    private final File DEFAULT_STORAGE_BASEDIR = new File(basedir, STORAGE_BASEDIR_NAME);

    private final File DEFAULT_AGENT_BASEDIR = new File(basedir, AGENT_BASEDIR_NAME);

    private Options options;

    public Install() {
        options = new Options()
            .addOption(null, "storage", false, "Install RHQ storage node. The default install directory will be " +
                DEFAULT_STORAGE_BASEDIR + ". Use the --storage-dir option to choose an alternate directory.")
            .addOption(null, "server", false, "Install RHQ server")
            .addOption(null, "agent", false, "Install RHQ agent. The default install directory will be " +
                DEFAULT_AGENT_BASEDIR + ". Use the --agent-dir option to choose an alternate directory.")
            .addOption(null, "storage-dir", true, "The directory where the storage node will be installed.")
            .addOption(null, "agent-dir", true, "The directory where the agent will be installed.")
            .addOption(null, SERVER_CONFIG_OPTION, true, "An alternate properties file to use in place of the default " +
                "rhq-server.properties")
            .addOption(null, AGENT_CONFIG_OPTION, true, "An alternate XML file to use in place of the default " +
                "agent-configuration.xml")
            .addOption(null, STORAGE_CONFIG_OPTION, true, "A properties file with keys that correspond to option names " +
                "of the storage installer. Each property will be translated into an option that is passed to the " +
                " storage installer. See example.storage.properties for examples.");
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

            // if no options specified, then install whatever is installed
            if (!(commandLine.hasOption(STORAGE_OPTION) || commandLine.hasOption(SERVER_OPTION) ||
                commandLine.hasOption(AGENT_OPTION))) {

                replaceServerPropertiesIfNecessary(commandLine);

                if (!isStorageInstalled()) {
                    installStorageNode(getStorageBasedir(commandLine), commandLine);
                }
                if (!isServerInstalled()) {
                    startRHQServerForInstallation();
                    installRHQServer();
                    waitForRHQServerToInitialize();
                }
                if (!isAgentInstalled()) {
                    clearAgentPreferences();
                    File agentBasedir = getAgentBasedir(commandLine);
                    installAgent(agentBasedir);
                    configureAgent(agentBasedir, commandLine);
                    startAgent(agentBasedir);
                }
            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        log.warn("The RHQ storage node is already installed in " + new File(basedir, "storage"));
                        log.warn("Skipping storage node installation.");
                    } else {
                        replaceServerPropertiesIfNecessary(commandLine);
                        installStorageNode(getStorageBasedir(commandLine), commandLine);
                    }

                    if (!isAgentInstalled()) {
                        File agentBasedir = getAgentBasedir(commandLine);
                        clearAgentPreferences();
                        installAgent(agentBasedir);
                        replaceAgentConfigIfNecessary(commandLine);
                        configureAgent(agentBasedir, commandLine);
                        startAgent(agentBasedir);
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        log.warn("The RHQ server is already installed.");
                        log.warn("Skipping server installation.");
                    } else {
                        replaceServerPropertiesIfNecessary(commandLine);
                        startRHQServerForInstallation();
                        installRHQServer();
                        waitForRHQServerToInitialize();
                    }
                }
                if (commandLine.hasOption(AGENT_OPTION)) {
                    if (isAgentInstalled() && !commandLine.hasOption(STORAGE_OPTION)) {
                        log.warn("The RHQ agent is already installed in " + new File(basedir, "rhq-agent"));
                        log.warn("Skipping agent installation");
                    } else {
                        File agentBasedir = getAgentBasedir(commandLine);
                        clearAgentPreferences();
                        installAgent(agentBasedir);
                        replaceAgentConfigIfNecessary(commandLine);
                        configureAgent(agentBasedir, commandLine);
                        startAgent(agentBasedir);
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to install services", e);
        }
    }

    private List<String> validateOptions(CommandLine commandLine) {
        List<String> errors = new LinkedList<String>();

        if (!(commandLine.hasOption(STORAGE_OPTION) || commandLine.hasOption(SERVER_OPTION) ||
            commandLine.hasOption(AGENT_OPTION))) {
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
            }

            if (commandLine.hasOption(SERVER_OPTION) && !isStorageInstalled() &&
                commandLine.hasOption(SERVER_CONFIG_OPTION)) {
                File serverConfig = new File(commandLine.getOptionValue(SERVER_CONFIG_OPTION));
                validateServerConfigOption(serverConfig, errors);
            }

            if (commandLine.hasOption(AGENT_OPTION) && !isAgentInstalled() &&
                commandLine.hasOption(AGENT_CONFIG_OPTION)) {
                File agentConfig = new File(commandLine.getOptionValue(AGENT_CONFIG_OPTION));
                validateAgentConfigOption(agentConfig, errors);
            }
        }

        return errors;
    }

    private void validateServerConfigOption(File serverConfig, List<String> errors) {
        if (!serverConfig.exists()) {
            errors.add("The --server-config option has as its value a file that does not exist [" +
                serverConfig.getAbsolutePath() + "]");
        } else if (serverConfig.isDirectory()) {
            errors.add("The --server-config option has as its value a path that is a directory [" +
                serverConfig.getAbsolutePath() + "]. It should be a properties file that replaces the " +
                "default rhq-server.properties");
        }
    }

    private void validateAgentConfigOption(File agentConfig, List<String> errors) {
        if (!agentConfig.exists()) {
            errors.add("The --agent-config option has as its value a file that does not exist [" +
                agentConfig.getAbsolutePath() + "]");
        } else if (agentConfig.isDirectory()) {
            errors.add("The --agent-config option has as its value a path that is a directory [" +
                agentConfig.getAbsolutePath() + "]. It should be an XML file that replaces the default " +
                "agent-configuration.xml");
        }
    }

    private void validateStorageConfigOption(File storageConfig, List<String> errors) {
        if (!storageConfig.exists()) {
            errors.add("The --storage-config option has as its value a file that does not exist [" +
                storageConfig.getAbsolutePath() + "]");
        } else if (storageConfig.isDirectory()) {
            errors.add("The --storage-config option has as its value a path that is a directory [" +
                storageConfig.getAbsolutePath() + "]. It should be a properties file with keys that " +
                "correspond to options for the storage installer.");
        }
    }

    private File getStorageBasedir(CommandLine cmdLine) {
        if (cmdLine.hasOption("storage-dir")) {
            File installDir = new File(cmdLine.getOptionValue("storage-dir"));
            return new File(installDir, STORAGE_BASEDIR_NAME);
        }
        return DEFAULT_STORAGE_BASEDIR;
    }

    private File getAgentBasedir(CommandLine cmdLine) {
        if (cmdLine.hasOption("agent-dir")) {
            File installDir = new File(cmdLine.getOptionValue("agent-dir"));
            return new File(installDir, AGENT_BASEDIR_NAME);
        }
        return DEFAULT_AGENT_BASEDIR;
    }

    private int installStorageNode(File storageBasedir, CommandLine rhqctlCommandLine) throws Exception {
        log.debug("Installing RHQ storage node");

        putProperty("rhq.storage.basedir", storageBasedir.getAbsolutePath());

        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(
            "./rhq-storage-installer." + getExtension());

        if (rhqctlCommandLine.hasOption(STORAGE_CONFIG_OPTION)) {
            String[] args = toArray(loadStorageProperties(rhqctlCommandLine.getOptionValue(STORAGE_CONFIG_OPTION)));
            commandLine.addArguments(args);
        }


//            .addArgument("--dir")
//            .addArgument(storageBasedir.getAbsolutePath())
//            .addArgument("--commitlog")
//            .addArgument("./storage/commit_log")
//            .addArgument("--data")
//            .addArgument("./storage/data")
//            .addArgument("--saved-caches")
//            .addArgument("./storage/saved_caches");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(binDir);
        executor.setStreamHandler(new PumpStreamHandler());

        return executor.execute(commandLine);
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

    private void startRHQServerForInstallation() throws Exception {
        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(
            "./rhq-server." + getExtension()).addArgument("start");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(binDir);
        executor.setStreamHandler(new PumpStreamHandler());

        executor.execute(commandLine, new DefaultExecuteResultHandler());

        commandLine = new org.apache.commons.exec.CommandLine("./rhq-installer." + getExtension())
            .addArgument("--test");
        executor = new DefaultExecutor();
        executor.setWorkingDirectory(binDir);

        int exitCode = executor.execute(commandLine);
        while (exitCode != 0) {
            exitCode = executor.execute(commandLine);
        }
    }

    private void installRHQServer() throws Exception {
        log.debug("Installing RHQ server");

        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(
            "./rhq-installer." + getExtension());
        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(binDir);
        executor.setStreamHandler(new PumpStreamHandler());

        executor.execute(commandLine, new DefaultExecuteResultHandler());
    }

    private void waitForRHQServerToInitialize() throws Exception {
        while (!isRHQServerInitialized()) {
            Thread.sleep(5000);
        }
    }

    private boolean isRHQServerInitialized() throws Exception {
        File logDir = new File(basedir, "logs");
        BufferedReader reader = new BufferedReader(new FileReader(new File(logDir, "server.log")));
        String line = reader.readLine();
        while (line != null) {
            if (line.contains("Server started")) {
                return true;
            }
            line = reader.readLine();
        }
        return false;
    }

    private void installAgent(File agentBasedir) throws Exception {
        log.debug("Installing RHQ agent");

        File agentInstallerJar = getAgentInstaller();

        putProperty(RHQ_AGENT_BASEDIR_PROP, agentBasedir.getAbsolutePath());

        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine("java")
            .addArgument("-jar")
            .addArgument(agentInstallerJar.getAbsolutePath())
            .addArgument("--install=" + agentBasedir.getParentFile().getAbsolutePath());

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(basedir);
        executor.setStreamHandler(new PumpStreamHandler());

        executor.execute(commandLine);

        new File(basedir, "rhq-agent-update.log").delete();
    }

    private File getAgentInstaller() {
        File agentDownloadDir = new File(basedir,
            "modules/org/rhq/rhq-enterprise-server-startup-subsystem/main/deployments/rhq.ear/rhq-downloads/rhq-agent");
        return agentDownloadDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains("rhq-enterprise-agent");
            }
        })[0];
    }

    private void configureAgent(File agentBasedir, CommandLine commandLine) throws Exception {
        if (commandLine.hasOption(AGENT_CONFIG_OPTION)) {
            replaceAgentConfigIfNecessary(commandLine);
        } else {
            File agentConfDir = new File(agentBasedir, "conf");
            File agentConfigFile = new File(agentConfDir, "agent-configuration.xml");
            agentConfigFile.delete();

            Map<String, String> tokens = new TreeMap<String, String>();
            tokens.put("rhq.agent.server.bind-address", InetAddress.getLocalHost().getHostName());

            InputStream inputStream = getClass().getResourceAsStream("/agent-configuration.xml");
            TokenReplacingReader reader = new TokenReplacingReader(new InputStreamReader(inputStream), tokens);
            BufferedWriter writer = new BufferedWriter(new FileWriter(agentConfigFile));

            StreamUtil.copy(reader, writer);
        }
    }

    private void clearAgentPreferences() throws Exception {
        Preferences agentPrefs = Preferences.userRoot().node("/rhq-agent");
        agentPrefs.removeNode();
        agentPrefs.flush();
        Preferences.userRoot().sync();
    }

    private void startAgent(File agentBasedir) throws Exception {
        File agentBinDir = new File(agentBasedir, "bin");

        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(
            "./rhq-agent-wrapper." + getExtension()).addArgument("start");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(agentBinDir);
        executor.setStreamHandler(new PumpStreamHandler());
        executor.execute(commandLine);
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
            throw new RHQControlException("Failed to replace " + defaultServerProps + " with " + newServerProperties,
                e);
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
