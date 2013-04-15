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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.core.util.TokenReplacingReader;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author John Sanda
 */
public class Install extends ControlCommand {

    private Options options;

    public Install() {
        options = new Options()
            .addOption(null, "storage", false, "Install RHQ storage node")
            .addOption(null, "server", false, "Install RHQ server")
            .addOption(null, "agent", false, "Install RHQ agent");
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
            // if no options specified, then stop whatever is installed
            if (commandLine.getOptions().length == 0) {
                if (!isStorageInstalled()) {
                    installStorageNode();
                }
                if (!isServerInstalled()) {
                    startRHQServerForInstallation();
                    installRHQServer();
                    waitForRHQServerToInitialize();
                }
                if (!isAgentInstalled()) {
                    clearAgentPreferences();
                    installAgent();
                    configureAgent();
                    startAgent();
                }
            } else {
                if (commandLine.hasOption(STORAGE_OPTION)) {
                    if (isStorageInstalled()) {
                        log.warn("The RHQ storage node is already installed in " + new File(basedir, "storage"));
                        log.warn("Skipping storage node installation.");
                    } else {
                        installStorageNode();
                    }

                    if (!isAgentInstalled()) {
                        clearAgentPreferences();
                        installAgent();
                        configureAgent();
                        startAgent();
                    }
                }
                if (commandLine.hasOption(SERVER_OPTION)) {
                    if (isServerInstalled()) {
                        log.warn("The RHQ server is already installed.");
                        log.warn("Skipping server installation.");
                    } else {
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
                        clearAgentPreferences();
                        installAgent();
                        configureAgent();
                        startAgent();
                    }
                }
            }
        } catch (Exception e) {
            throw new RHQControlException("Failed to install services", e);
        }
    }

    private int installStorageNode() throws Exception {
        log.debug("Installing RHQ storage node");

        return new ProcessBuilder("./rhq-storage-installer.sh", "--commitlog", "../storage/commit_log", "--data",
            "../storage/data", "--saved-caches", "../storage/saved_caches")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();
    }

    private void startRHQServerForInstallation() throws Exception {
        new ProcessBuilder("./rhq-server.sh", "start")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();

        ProcessBuilder pb = new ProcessBuilder("./rhq-installer.sh", "--test")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = pb.start();
        while (process.waitFor() != 0) {
            process = pb.start();
        }
    }

    private void installRHQServer() throws Exception {
        log.debug("Installing RHQ server");

        new ProcessBuilder("./rhq-installer.sh")
            .directory(binDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
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

    private void installAgent() throws Exception {
        log.debug("Installing RHQ agent");

        File agentInstallerJar = getAgentInstaller();
        new ProcessBuilder("java", "-jar", agentInstallerJar.getPath(), "--install")
            .directory(basedir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor();

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

    private void configureAgent() throws Exception {
        File agentHomeDir = new File(basedir, "rhq-agent");
        File agentBinDir = new File(agentHomeDir, "bin");
        File agentConfDir = new File(agentHomeDir, "conf");
        File agentConfigFile = new File(agentConfDir, "agent-configuration.xml");
        agentConfigFile.delete();

        Map<String, String> tokens = new TreeMap<String, String>();
        tokens.put("rhq.agent.server.bind-address", InetAddress.getLocalHost().getHostName());

        InputStream inputStream = getClass().getResourceAsStream("/agent-configuration.xml");
        TokenReplacingReader reader = new TokenReplacingReader(new InputStreamReader(inputStream), tokens);
        BufferedWriter writer = new BufferedWriter(new FileWriter(agentConfigFile));

        StreamUtil.copy(reader, writer);
    }

    private void clearAgentPreferences() throws Exception {
        Preferences agentPrefs = Preferences.userRoot().node("/rhq-agent");
        agentPrefs.removeNode();
        agentPrefs.flush();
        Preferences.userRoot().sync();
    }

    private void startAgent() throws Exception {
        File agentHomeDir = new File(basedir, "rhq-agent");
        File agentBinDir = new File(agentHomeDir, "bin");

        new ProcessBuilder("./rhq-agent-wrapper.sh", "start")
            .directory(agentBinDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    }

}
