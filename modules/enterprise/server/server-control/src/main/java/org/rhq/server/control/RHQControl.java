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

package org.rhq.server.control;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;

import org.apache.commons.cli.HelpFormatter;

import org.rhq.core.util.TokenReplacingReader;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class RHQControl {

    private Commands commands = new Commands();

    private File basedir;

    private File binDir;

    public RHQControl() {
        basedir = new File(System.getProperty("rhq.server.basedir"));
        binDir = new File(basedir, "bin");
    }

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhqctl.sh <cmd> [options]";
        String header = "\nwhere <cmd> is one of:";

        helpFormatter.setOptPrefix("");
        helpFormatter.printHelp(syntax, header, commands.getOptions(), null);
    }

    public void exec(String[] args) {
        try {
            if (args.length == 0) {
                throw new RHQControlException();
            }
            String commandName = findCommand(commands, args);
            ControlCommand command = commands.get(commandName);

            command.exec(getCommandLine(commandName, args));
        } catch (RHQControlException e) {
            printUsage();
        }
    }

    private String findCommand(Commands commands, String[] args) throws RHQControlException {
        List<String> commandNames = new LinkedList<String>();
        for (String arg : args) {
            if (commands.contains(arg)) {
                commandNames.add(arg);
            }
        }

        if (commandNames.size() != 1) {
            throw new RHQControlException();
        }

        return commandNames.get(0);
    }

    private String[] getCommandLine(String cmd, String[] args) {
        String[] cmdLine = new String[args.length - 1];
        int i = 0;
        for (String arg : args) {
            if (arg.equals(cmd)) {
                continue;
            }
            cmdLine[i++] = arg;
        }
        return cmdLine;
    }

    public void install() throws Exception {
        installStorageNode();
        startRHQServerForInstallation();
        installRHQServer();
        waitForRHQServerToInitialize();
        clearAgentPreferences();
        installAgent();
        configureAgent();
        startAgent();
    }

    private int installStorageNode() throws Exception {
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

        InputStream inputStream = RHQControl.class.getResourceAsStream("/agent-configuration.xml");
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

    public static void main(String[] args) throws Exception {
//        File basedir = new File(System.getProperty("rhq.server.basedir"));
//        File binDir = new File(basedir, "bin");
//
//        // --commitlog=../storage/commit_log --data=../storage/data --saved-caches=../storage/saved_cache
//        StorageInstaller.main(new String[] {"--commitlog", "../commit_log", "--data", "../data",
//            "--saved-caches", "../saved_caches"});
//
//        Process process = new ProcessBuilder("./rhq-server.sh", "start")
//            .directory(binDir)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
//            .start();
//        Thread.sleep(5000);
//
//        initSysPropsForRHQInstaller(basedir, binDir);
//
//        InstallerService installer = new InstallerServiceImpl(new InstallerConfiguration());
//        while (true) {
//            try {
//                installer.test();
//                break;
//            } catch (Exception e) {
//                Thread.sleep(1000);
//            }
//        }
//        final HashMap<String, String> serverProperties = installer.preInstall();
//        installer.install(serverProperties, null, null);
//
//        while (!isRHQServerInitialized(basedir)) {
//            Thread.sleep(5000);
//        }
//        System.out.println("RHQ server is initialized");
//
//        File agentDownloadDir = new File(basedir,
//            "modules/org/rhq/rhq-enterprise-server-startup-subsystem/main/deployments/rhq.ear/rhq-downloads/rhq-agent");
//        File agentUpdateFile = agentDownloadDir.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File file) {
//                return file.getName().contains("rhq-enterprise-agent");
//            }
//        })[0];
//        File agentJarFile = new File(basedir, "rhq-agent.jar");
//        FileUtil.copyFile(agentUpdateFile, agentJarFile);
//
//        new ProcessBuilder("java", "-jar", agentJarFile.getPath(), "--install")
//            .directory(basedir)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
//            .start()
//            .waitFor();
//
//        new File(basedir, "rhq-agent-update.log").delete();
//        agentJarFile.delete();
//
//        File agentHomeDir = new File(basedir, "rhq-agent");
//        File agentBinDir = new File(agentHomeDir, "bin");
//        File agentConfDir = new File(agentHomeDir, "conf");
//        File agentConfigFile = new File(agentConfDir, "agent-configuration.xml");
//        agentConfigFile.delete();
//
//        Preferences agentPrefs = Preferences.userRoot().node("/rhq-agent");
//        agentPrefs.removeNode();
//        agentPrefs.flush();
//
//        // update agent config
//        Map<String, String> tokens = new TreeMap<String, String>();
//        tokens.put("rhq.agent.server.bind-address", InetAddress.getLocalHost().getHostName());
//
//        InputStream inputStream = RHQControl.class.getResourceAsStream("/agent-configuration.xml");
//        TokenReplacingReader reader = new TokenReplacingReader(new InputStreamReader(inputStream), tokens);
//        BufferedWriter writer = new BufferedWriter(new FileWriter(agentConfigFile));
//
//        StreamUtil.copy(reader, writer);
//
//        new ProcessBuilder("./rhq-agent-wrapper.sh", "start")
//            .directory(agentBinDir)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
//            .start();

        RHQControl control = new RHQControl();
//        control.install();
        control.exec(args);

        System.exit(0);
    }

    private static void initSysPropsForRHQInstaller(File baseDir, File binDir) {
        File logDir = new File(baseDir, "logs");

        System.setProperty("i18nlog.logger-type", "commons");
        System.setProperty("rhq.server.properties-file", new File(binDir, "rhq-server.properties").getPath());
        System.setProperty("rhq.server.installer.logdir", logDir.getPath());
        System.setProperty("rhq.server.installer.loglevel", "INFO");
    }

}
