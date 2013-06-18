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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.DeploymentJBossASClient;
import org.rhq.common.jbossas.client.controller.MCCHelper;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * Common code for commands that perform installs. Basically shared code for Install and Upgrade commands.
 *
 * @author Jay Shaughnessy
 */
public abstract class AbstractInstall extends ControlCommand {

    protected final String STORAGE_CONFIG_OPTION = "storage-config";
    protected final String STORAGE_DATA_ROOT_DIR = "storage-data-root-dir";

    protected final String STORAGE_CONFIG_PROP = "rhqctl.install.storage-config";

    protected void installWindowsService(File workingDir, String batFile) throws Exception {
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

    protected void validateCustomStorageDataDirectories(CommandLine commandLine, List<String> errors) {
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

    private boolean isDirectoryEmpty(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            return (files == null || files.length == 0);
        } else {
            return true;
        }
    }

    protected void waitForProcessToStop(String pid) throws Exception {

        if (isWindows() || pid==null) {
            // For the moment we have no better way to just wait some time
            Thread.sleep(10*1000L);
        } else {
            int tries = 5;
            while (tries > 0) {
                log.debug(".");
                if (!isUnixPidRunning(pid)) {
                    break;
                }
                Thread.sleep(2*1000L);
                tries--;
            }
            if (tries==0) {
                throw new RHQControlException("Process [" + pid + "] did not finish yet. Terminate it manually and retry.");
            }
        }

    }

    protected boolean isUnixPidRunning(String pid) {

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(getBinDir());
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine;

        commandLine = new org.apache.commons.exec.CommandLine("/bin/kill")
            .addArgument("-0")
            .addArgument(pid);

        try {
            int code = executor.execute(commandLine);
            if (code!=0) {
                return false;
            }
        } catch (ExecuteException ee ) {
            if (ee.getExitValue()==1) {
                // return code 1 means process does not exist
                return false;
            }
        } catch (IOException e) {
            log.error("Checking for running process failed: " + e.getMessage());
        }
        return true;
    }

    protected void waitForRHQServerToInitialize() throws Exception {
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

    @SuppressWarnings("resource")
    protected boolean isRHQServerInitialized() throws IOException {

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

    /**
     * Same as <pre>startAgent(agentBasedir, false);</pre>
     * @param agentBasedir
     * @throws Exception
     */
    protected void startAgent(File agentBasedir) throws Exception {
        startAgent(agentBasedir, false);
    }

    protected void startAgent(File agentBasedir, boolean updateWindowsService) throws Exception {
        try {
            File agentBinDir = new File(agentBasedir, "bin");
            if (!agentBinDir.exists()) {
                throw new IllegalArgumentException("No Agent found for base directory [" + agentBasedir.getPath() + "]");
            }

            log.info("Starting RHQ agent...");
            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(agentBinDir);
            executor.setStreamHandler(new PumpStreamHandler());
            org.apache.commons.exec.CommandLine commandLine;

            if (isWindows() && updateWindowsService) {
                // Ensure the windows service is up to date beFore starting. [re-]install the windows service.

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

    protected void stopAgent(File agentBasedir) throws Exception {

        File agentBinDir = new File(agentBasedir, "bin");
        if (!agentBinDir.exists()) {
            throw new IllegalArgumentException("No Agent found for base directory [" + agentBasedir.getPath() + "]");
        }

        log.debug("Stopping RHQ agent...");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(agentBinDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-agent-wrapper", "stop");

        if (isWindows()) {
            try {
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to stop agent service", e);
            }
        } else {
            String pid = getAgentPid();
            if (pid != null) {
                executor.execute(commandLine);
            }
        }
    }

    protected void stopServer(File serverBasedir) throws Exception {

        File serverBinDir = new File(serverBasedir, "bin");
        if (!serverBinDir.exists()) {
            throw new IllegalArgumentException("No Server found for base directory [" + serverBasedir.getPath() + "]");
        }

        log.debug("Stopping RHQ server...");


        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(serverBinDir);
        executor.setStreamHandler(new PumpStreamHandler());
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "stop");

        if (isWindows()) {
            try {
                executor.execute(commandLine);
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to stop server service", e);
            }
        } else {
            executor.execute(commandLine);
        }
    }

    protected void startRHQServerForInstallation() throws IOException {
        try {
            log.info("The RHQ Server must be started to complete its upgrade. Starting the RHQ server in preparation of running the server installer...");

            // when you unzip the distro, you are getting a fresh, unadulterated, out-of-box EAP installation, which by default listens
            // to port 9999 for its native management subsystem. Make sure some other independent EAP server (or anything for that matter)
            // isn't already listening to that port.
            if (isPortInUse("127.0.0.1", 9999)) {
                throw new IOException(
                    "Something is already listening to port 9999 - shut it down before upgrading the server.");
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
            log.info("Waiting for the RHQ Server to start in preparation of running the server installer for upgrade...");
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

            log.info("The RHQ Server is ready to be upgraded by the server installer.");

        } catch (IOException e) {
            log.error("An error occurred while starting the RHQ server: " + e.getMessage());
            throw e;
        }
    }

    protected void runRHQServerInstaller() throws IOException {
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

    protected int installStorageNode(File storageBasedir, CommandLine rhqctlCommandLine) throws IOException {
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

}
