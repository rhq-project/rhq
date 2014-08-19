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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.DeploymentJBossASClient;
import org.rhq.common.jbossas.client.controller.MCCHelper;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.file.FileReverter;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.file.FileVisitor;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;
import org.rhq.server.control.RHQPosixParser;
import org.rhq.server.control.util.ExecutorAssist;

/**
 * Common code for commands that perform installs. Basically shared code for Install and Upgrade commands.
 *
 * @author Jay Shaughnessy
 */
public abstract class AbstractInstall extends ControlCommand {

    protected static final String AGENT_CONFIG_OPTION = "agent-config";
    protected static final String FROM_AGENT_DIR_OPTION = "from-agent-dir";
    protected static final String START_OPTION = "start";

    protected static final String AGENT_PREFERENCE = "agent-preference";
    protected static final String STORAGE_DATA_ROOT_DIR = "storage-data-root-dir";

    // some known agent preference setting names
    private static final String PREF_RHQ_AGENT_CONFIGURATION_SETUP_FLAG = "rhq.agent.configuration-setup-flag";
    private static final String PREF_RHQ_AGENT_AUTO_UPDATE_FLAG = "rhq.agent.agent-update.enabled";
    private static final String PREF_RHQ_AGENT_SECURITY_TOKEN = "rhq.agent.security-token";
    private static final String PREF_RHQ_AGENT_SERVER_TRANSPORT = "rhq.agent.server.transport";
    private static final String PREF_RHQ_AGENT_SERVER_BINDADDRESS = "rhq.agent.server.bind-address";
    private static final String PREF_RHQ_AGENT_SERVER_BINDPORT = "rhq.agent.server.bind-port";
    private static final String PREF_RHQ_AGENT_SERVER_TRANSPORTPARAMS = "rhq.agent.server.transport-params";

    protected int installWindowsService(File workingDir, String batFile, boolean replaceExistingService, boolean start)
        throws Exception {
        org.apache.commons.exec.CommandLine commandLine;

        int rValue = RHQControl.EXIT_CODE_OK;

        if (replaceExistingService) {
            commandLine = getCommandLine(batFile, "stop");
            rValue = Math.max(rValue, ExecutorAssist.execute(workingDir, commandLine));

            commandLine = getCommandLine(batFile, "remove");
            rValue = Math.max(rValue, ExecutorAssist.execute(workingDir, commandLine));
        }

        commandLine = getCommandLine(batFile, "install");
        rValue = Math.max(rValue, ExecutorAssist.execute(workingDir, commandLine));

        if (start) {
            commandLine = getCommandLine(batFile, "start");
            rValue = Math.max(rValue, ExecutorAssist.execute(workingDir, commandLine));
        }
        return rValue;
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
            File propsFile = getServerPropertiesFile();
            reader = new BufferedReader(new FileReader(propsFile));
            props.load(reader);

            String host = (String) props.get("jboss.bind.address.management");
            int port = Integer.valueOf((String) props.get("jboss.management.native.port")).intValue();
            mcc = MCCHelper.createModelControllerClient(host, port);
            DeploymentJBossASClient client = new DeploymentJBossASClient(mcc);
            boolean isDeployed = client.isDeployment("rhq.ear");
            return isDeployed;

        } catch (Throwable t) {
            log.debug("Falling back to logfile check due to: ", t);

            File logDir = new File(getBaseDir(), "logs");
            reader = new BufferedReader(new FileReader(new File(logDir, "server.log")));
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("Server initialized")) {
                    return true;
                }
                line = reader.readLine();
            }

            return false;

        } finally {
            MCCHelper.safeClose(mcc);
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // best effort
                }
            }
        }
    }

    protected int updateWindowsAgentService(final File agentBasedir) throws Exception {
        if (!isWindows()) {
            return RHQControl.EXIT_CODE_OK;
        }

        int rValue = RHQControl.EXIT_CODE_OK;

        try {
            File agentBinDir = new File(agentBasedir, "bin");
            if (!agentBinDir.exists()) {
                throw new IllegalArgumentException("No Agent found for base directory [" + agentBasedir.getPath() + "]");
            }

            log.info("Updating RHQ Agent Service...");
            org.apache.commons.exec.CommandLine commandLine;

            // Ensure the windows service is up to date. [re-]install the windows service.

            commandLine = getCommandLine("rhq-agent-wrapper", "stop");
            try {
                rValue = Math.max(rValue, ExecutorAssist.execute(agentBinDir, commandLine));
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to stop agent service", e);
            }

            commandLine = getCommandLine("rhq-agent-wrapper", "remove");
            try {
                rValue = Math.max(rValue, ExecutorAssist.execute(agentBinDir, commandLine));
            } catch (Exception e) {
                // Ignore, service may not exist, script returns 1
                log.debug("Failed to uninstall agent service", e);
            }

            commandLine = getCommandLine("rhq-agent-wrapper", "install");
            rValue = Math.max(rValue, ExecutorAssist.execute(agentBinDir, commandLine));
        } catch (IOException e) {
            log.error("An error occurred while updating the agent service: " + e.getMessage());
            throw e;
        }

        return rValue;
    }

    protected int startAgent(final File agentBasedir) throws Exception {
        int rValue = RHQControl.EXIT_CODE_OK;

        try {
            File agentBinDir = new File(agentBasedir, "bin");
            if (!agentBinDir.exists()) {
                throw new IllegalArgumentException("No Agent found for base directory [" + agentBasedir.getPath() + "]");
            }

            log.info("Starting RHQ agent...");
            org.apache.commons.exec.CommandLine commandLine;

            // For *nix, just start the server in the background, for Win, now that the service is installed, start it
            commandLine = getCommandLine("rhq-agent-wrapper", "start");
            rValue = Math.max(rValue, ExecutorAssist.execute(agentBinDir, commandLine));

            // if any errors occur after now, we need to stop the agent
            addUndoTask(new ControlCommand.UndoTask("Stopping agent") {
                public void performUndoWork() throws Exception {
                    killAgent(agentBasedir);
                }
            });

            log.info("The agent has started up");
        } catch (IOException e) {
            log.error("An error occurred while starting the agent: " + e.getMessage());
            throw e;
        }
        return rValue;
    }

    protected int killAgent(File agentBasedir) throws Exception {

        File agentBinDir = new File(agentBasedir, "bin");
        if (!agentBinDir.exists()) {
            throw new IllegalArgumentException("No Agent found for base directory [" + agentBasedir.getPath() + "]");
        }

        log.debug("Stopping RHQ agent...");
        org.apache.commons.exec.CommandLine commandLine;

        int rValue = RHQControl.EXIT_CODE_OK;

        if (isWindows()) {
            try {
                commandLine = getCommandLine("rhq-agent-wrapper", "stop");
                rValue = Math.max(rValue, ExecutorAssist.execute(agentBinDir, commandLine));
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to stop agent service", e);
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        } else {
            String pid = getAgentPid();
            if (pid != null) {
                commandLine = getCommandLine("rhq-agent-wrapper", "kill");
                rValue = Math.max(rValue, ExecutorAssist.execute(agentBinDir, commandLine));
            }
        }
        return rValue;
    }

    protected int stopServer() throws Exception {

        File serverBinDir = getBinDir();
        if (!serverBinDir.exists()) {
            throw new IllegalArgumentException("No Server found for base directory [" + getBaseDir().getPath() + "]");
        }

        log.debug("Stopping RHQ server...");
        org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-server", "stop");

        int rValue = RHQControl.EXIT_CODE_OK;

        if (isWindows()) {
            try {
                rValue = Math.max(rValue, ExecutorAssist.execute(serverBinDir, commandLine));
            } catch (Exception e) {
                // Ignore, service may not exist or be running, , script returns 1
                log.debug("Failed to stop server service", e);
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        } else {
            rValue = Math.max(rValue, ExecutorAssist.execute(serverBinDir, commandLine));
        }
        return rValue;
    }

    protected int startRHQServerForInstallation() throws IOException {
        int rValue = RHQControl.EXIT_CODE_OK;

        try {
            validateServerPropertiesFile();
            log.info("The RHQ Server must be started to complete its installation. Starting the RHQ server in preparation of running the server installer...");

            // when you unzip the distro, you are getting a fresh, unadulterated, out-of-box EAP installation, which by default listens
            // to port 9999 for its native management subsystem. Make sure some other independent EAP server (or anything for that matter)
            // isn't already listening to that port.
            if (isPortInUse("127.0.0.1", 9999)) {
                throw new IOException(
                    "Something is already listening to port 9999 - shut it down before installing the server.");
            }

            org.apache.commons.exec.CommandLine commandLine;

            if (isWindows()) {
                // For windows we will [re-]install the server as a windows service, then start the service.

                commandLine = getCommandLine("rhq-server", "stop");
                rValue = Math.max(rValue, ExecutorAssist.execute(getBinDir(), commandLine));

                commandLine = getCommandLine("rhq-server", "remove");
                rValue = Math.max(rValue, ExecutorAssist.execute(getBinDir(), commandLine));

                commandLine = getCommandLine("rhq-server", "install");
                rValue = Math.max(rValue, ExecutorAssist.execute(getBinDir(), commandLine));

                commandLine = getCommandLine("rhq-server", "start");
                rValue = Math.max(rValue, ExecutorAssist.execute(getBinDir(), commandLine));

            } else {
                // For *nix, just start the server in the background
                commandLine = getCommandLine("rhq-server", "start");
                rValue = Math.max(rValue, ExecutorAssist.execute(getBinDir(), commandLine, true));
            }

            addUndoTaskToStopComponent("--server"); // if any errors occur after now, we need to stop the server

            // Wait for the server to complete it's startup
            log.info("Waiting for the RHQ Server to start in preparation of running the server installer...");
            commandLine = getCommandLine("rhq-installer", "--test");

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
                exitCode = ExecutorAssist.execute(getBinDir(), commandLine);
            } while (exitCode != 0);

            log.info("The RHQ Server is ready to be upgraded by the server installer.");

        } catch (IOException e) {
            log.error("An error occurred while starting the RHQ server: " + e.getMessage());
            throw e;
        }

        return rValue;
    }

    protected int runRHQServerInstaller() throws IOException {
        try {
            log.info("Installing RHQ server");

            // If the install fails, we will remove the install marker file allowing the installer to be able to run again.
            // We also need to revert mgmt-users.properties
            File mgmtUserPropertiesFile = new File(getBaseDir(),
                "jbossas/standalone/configuration/mgmt-users.properties");
            final FileReverter mgmtUserPropertiesReverter = new FileReverter(mgmtUserPropertiesFile);
            addUndoTask(new ControlCommand.UndoTask("Removing server-installed marker file and management user") {
                public void performUndoWork() throws Exception {
                    getServerInstalledMarkerFile(getBaseDir()).delete();
                    mgmtUserPropertiesReverter.revert();
                }
            });

            /**
             * @TODO There's no way this could catch the resultValue..
             */

            org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-installer");
            ExecutorAssist.execute(getBinDir(), commandLine, true);
            log.info("The server installer is running");
            return RHQControl.EXIT_CODE_OK; // the installer really didn't exit yet, so we don't know the result
        } catch (Exception e) {
            log.error("An error occurred while starting the server installer: " + e.getMessage());
            return RHQControl.EXIT_CODE_NOT_INSTALLED;
        }
    }

    private class StorageDataDirectories {
        public File basedir; // the other three will typically be under this base directory
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
            storageDataDirs.commitlogDir = new File(storageDataDirs.basedir, "commit_log");
            storageDataDirs.savedcachesDir = new File(storageDataDirs.basedir, "saved_caches");
        }

        return storageDataDirs;
    }

    private StorageDataDirectories getStorageDataDirectoriesFromProperties(Properties storageProperties) {
        File basedirForData = new File(getBaseDir().getParentFile(), "rhq-data");

        // check that the data directories are set and convert to absolute dirs
        File dataDirProp = new File(storageProperties.getProperty("rhq.storage.data", "data"));
        File commitlogDirProp = new File(storageProperties.getProperty("rhq.storage.commitlog", "commit_log"));
        File savedcachesDirProp = new File(storageProperties.getProperty("rhq.storage.saved-caches", "saved_caches"));

        if (!dataDirProp.isAbsolute()) {
            dataDirProp = new File(basedirForData, dataDirProp.getPath());
        }
        if (!commitlogDirProp.isAbsolute()) {
            commitlogDirProp = new File(basedirForData, commitlogDirProp.getPath());
        }
        if (!savedcachesDirProp.isAbsolute()) {
            savedcachesDirProp = new File(basedirForData, savedcachesDirProp.getPath());
        }

        StorageDataDirectories storageDataDirs = new StorageDataDirectories();
        storageDataDirs.basedir = basedirForData;
        storageDataDirs.dataDir = dataDirProp;
        storageDataDirs.commitlogDir = commitlogDirProp;
        storageDataDirs.savedcachesDir = savedcachesDirProp;
        return storageDataDirs;
    }

    protected int installStorageNode(final File storageBasedir, CommandLine rhqctlCommandLine, boolean start)
        throws Exception {
        try {
            log.info("Preparing to install RHQ storage node.");

            final Properties storageProperties = loadStorageProperties();

            org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-storage-installer", "--dir",
                storageBasedir.getAbsolutePath(), "--start", Boolean.valueOf(start).toString());

            if (rhqctlCommandLine.hasOption(STORAGE_DATA_ROOT_DIR)) {
                StorageDataDirectories dataDirs;
                dataDirs = getCustomStorageDataDirectories(rhqctlCommandLine);
                storageProperties.setProperty("rhq.storage.data", dataDirs.dataDir.getAbsolutePath());
                storageProperties.setProperty("rhq.storage.commitlog", dataDirs.commitlogDir.getAbsolutePath());
                storageProperties.setProperty("rhq.storage.saved-caches", dataDirs.savedcachesDir.getAbsolutePath());
            }

            // add the properties set in rhq-storage.properties to the command line
            String[] args = toArray(storageProperties);
            commandLine.addArguments(args);

            // if the install fails, we need to delete the data directories that were created and
            // purge the rhq-storage install directory that might have a "half" installed storage node in it.
            addUndoTask(new ControlCommand.UndoTask("Removing storage node data and install directories") {
                public void performUndoWork() {
                    StorageDataDirectories dataDirs = getStorageDataDirectoriesFromProperties(storageProperties);
                    FileUtil.purge(dataDirs.dataDir, true);
                    FileUtil.purge(dataDirs.commitlogDir, true);
                    FileUtil.purge(dataDirs.savedcachesDir, true);

                    FileUtil.purge(storageBasedir, true);
                }
            });

            // execute the storage installer now
            int exitCode = ExecutorAssist.execute(getBinDir(), commandLine);
            log.info("The storage node installer has finished with an exit value of " + exitCode);

            // the storage node is installed AND running now so, if we fail later, we need to shut the storage node down
            addUndoTaskToStopComponent("--storage");

            return exitCode;
        } catch (Exception e) {
            log.error("An error occurred while running the storage installer: " + e.getMessage());
            if (e.getMessage().toLowerCase().contains("exit value: 3")) {
                log.error("Try to point your root data directory via --" + STORAGE_DATA_ROOT_DIR
                    + " to a directory where you have read and write permissions.");
            }
            throw e;
        }
    }

    protected int installAgent(final File agentBasedir, final CommandLine commandLine) throws Exception {
        clearAgentPreferences();
        int rValue = installAgent(agentBasedir);
        configureAgent(agentBasedir, commandLine);
        return rValue;
    }

    private int installAgent(final File agentBasedir) throws Exception {
        try {
            log.info("Installing RHQ agent");

            File agentInstallerJar = getAgentInstaller();

            setAgentBasedir(agentBasedir);

            // if the install fails, we will completely delete any agent that might have been "half" installed
            addUndoTask(new ControlCommand.UndoTask("Removing agent install directory") {
                public void performUndoWork() {
                    FileUtil.purge(agentBasedir, true);
                }
            });

            // Make sure we use the appropriate java version, don't just fall back to PATH
            String javaExeFilePath = System.getProperty("rhq.java-exe-file-path");

            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(javaExeFilePath)
                .addArgument("-jar").addArgument(agentInstallerJar.getAbsolutePath())
                .addArgument("--install=" + agentBasedir.getParentFile().getAbsolutePath())
                .addArgument("--log=" + new File(getLogDir(), "rhq-agent-update.log"));

            int exitValue = ExecutorAssist.execute(getBaseDir(), commandLine);
            log.info("The agent installer finished running with exit value " + exitValue);
            return exitValue;
        } catch (Exception e) {
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

    protected int updateAndMoveExistingAgent(final File agentBasedir, final File oldAgentDir,
        final File agentInstallerJar) throws Exception {

        // Make sure we use the appropriate java version, don't just fall back to PATH
        String javaExeFilePath = System.getProperty("rhq.java-exe-file-path");

        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(javaExeFilePath) //
            .addArgument("-jar").addArgument(agentInstallerJar.getAbsolutePath()) //
            .addArgument("--update=" + oldAgentDir.getAbsolutePath()) //
            .addArgument("--log=" + new File(getLogDir(), "rhq-agent-update.log")) //
            .addArgument("--launch=false"); // we can't launch this copy - we still have to move it to the new location

        int exitValue = ExecutorAssist.execute(getBaseDir(), commandLine);
        log.info("The agent installer finished updating with exit value " + exitValue);

        // We need to now move the new, updated agent over to the new agent location.
        // renameTo() may fail if we are crossing file system boundaries, so try a true copy as a fallback.
        if (!agentBasedir.equals(oldAgentDir)) {
            // BZ 1118906 - we need to guard against the possibility that one or both of these are symlinks which aren't
            // "equal" to each other but yet still point to the same location. If they point to the same location
            // it is as if they are "equal" and we should do nothing.
            if (!agentBasedir.getCanonicalPath().equals(oldAgentDir.getCanonicalPath())) {
                FileUtil.purge(agentBasedir, true); // clear the way for the new upgraded agent
                if (!oldAgentDir.renameTo(agentBasedir)) {
                    FileUtil.copyDirectory(oldAgentDir, agentBasedir);

                    // we need to retain the execute bits for the executable scripts and libraries
                    FileVisitor visitor = new FileVisitor() {
                        @Override
                        public void visit(File file) {
                            String filename = file.getName();
                            if (filename.contains(".so") || filename.contains(".sl") || filename.contains(".dylib")) {
                                file.setExecutable(true);
                            } else if (filename.endsWith(".sh")) {
                                file.setExecutable(true);
                            }
                        }
                    };

                    FileUtil.forEachFile(new File(agentBasedir, "bin"), visitor);
                    FileUtil.forEachFile(new File(agentBasedir, "lib"), visitor);
                }
            }
        }

        return exitValue;
    }

    static protected File getFromAgentDir(CommandLine commandLine) {
        return (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) ? new File(
            commandLine.getOptionValue(FROM_AGENT_DIR_OPTION)) : null;
    }

    protected File getFileDownload(String directory, final String fileMatch) {
        File downloadDir = new File(getBaseDir(),
            "modules/org/rhq/server-startup/main/deployments/rhq.ear/rhq-downloads/" + directory);
        return downloadDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(fileMatch);
            }
        })[0];
    }

    private Preferences getAgentPreferences() {
        Preferences agentPrefs = Preferences.userRoot().node("rhq-agent/default");
        return agentPrefs;
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

            // get the configured server endpoint information and tell the agent so it knows where the server is.
            Properties serverEndpoint = getAgentServerEndpoint();
            String endpointTransport = serverEndpoint.getProperty(PREF_RHQ_AGENT_SERVER_TRANSPORT);
            String endpointAddress = serverEndpoint.getProperty(PREF_RHQ_AGENT_SERVER_BINDADDRESS);
            String endpointPort = serverEndpoint.getProperty(PREF_RHQ_AGENT_SERVER_BINDPORT);
            String endpointParams = serverEndpoint.getProperty(PREF_RHQ_AGENT_SERVER_TRANSPORTPARAMS);
            if (endpointTransport != null) {
                preferencesNode.put(PREF_RHQ_AGENT_SERVER_TRANSPORT, endpointTransport);
            }
            if (endpointAddress != null) {
                preferencesNode.put(PREF_RHQ_AGENT_SERVER_BINDADDRESS, endpointAddress);
            }
            if (endpointPort != null) {
                preferencesNode.put(PREF_RHQ_AGENT_SERVER_BINDPORT, endpointPort);
            }
            if (endpointParams != null) {
                preferencesNode.put(PREF_RHQ_AGENT_SERVER_TRANSPORTPARAMS, endpointParams);
            }

            // if the user provided any overrides to the agent config, use them.
            overrideAgentPreferences(commandLine, preferencesNode);

            // set some prefs that must be a specific value
            // - do not tell this agent to auto-update itself - this agent must be managed by rhqctl only
            // - set the config setup flag to true to prohibit the agent from asking setup questions at startup
            String agentUpdateEnabledPref = PREF_RHQ_AGENT_AUTO_UPDATE_FLAG;
            preferencesNode.putBoolean(agentUpdateEnabledPref, false);
            String setupPref = PREF_RHQ_AGENT_CONFIGURATION_SETUP_FLAG;
            preferencesNode.putBoolean(setupPref, true);

            try {
                preferencesNode.flush();
                preferencesNode.sync();
            } catch (BackingStoreException bse) {
                log.error("Failed to store agent preferences, for Linux systems we require writable user.home ["
                    + System.getProperty("user.home")
                    + "]. You can also set different location for agent preferences by setting \"-Djava.util.prefs.userRoot=/some/path/\""
                    + " java system property. You may need to put this property to RHQ_CONTROL_ADDIDIONAL_JAVA_OPTS and RHQ_AGENT_ADDIDIONAL_JAVA_OPTS env variables.");
                throw bse;
            }

            log.info("Finished configuring the agent");
        } catch (Exception e) {
            log.error("An error occurred while configuring the agent: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns the server endpoint that the agent should use when connecting to the server.
     *
     * @return properties with the values - only those values that could be determined will be in here
     *
     * @throws Exception if could not determine the values due to a runtime error
     */
    private Properties getAgentServerEndpoint() throws Exception {
        Properties endpointData = new Properties();

        // load in the server properties file to find out how the server will be listening for agent messages
        File serverPropsFile = getServerPropertiesFile();
        Properties serverProps = new PropertiesFileUpdate(serverPropsFile).loadExistingProperties();

        // transport and transport params
        String transport = serverProps.getProperty("rhq.communications.connector.transport", "servlet");
        endpointData.put(PREF_RHQ_AGENT_SERVER_TRANSPORT, transport);

        String params = serverProps.getProperty("rhq.communications.connector.transport-params");
        if (params != null && params.trim().length() > 0) {
            endpointData.setProperty(PREF_RHQ_AGENT_SERVER_TRANSPORTPARAMS, params.trim());
        }

        //  address and port depends on the transport
        if (transport.contains("servlet")) {
            String port;
            if (transport.contains("ssl")) {
                port = serverProps.getProperty("rhq.server.socket.binding.port.https");
            } else {
                port = serverProps.getProperty("rhq.server.socket.binding.port.http");
            }
            if (port != null && port.trim().length() > 0) {
                endpointData.setProperty(PREF_RHQ_AGENT_SERVER_BINDPORT, port.trim());
            }

            String address = serverProps.getProperty("jboss.bind.address");
            if (address != null && address.trim().length() > 0) {
                endpointData.setProperty(PREF_RHQ_AGENT_SERVER_BINDADDRESS, address.trim());
            }
        } else {
            String port = serverProps.getProperty("rhq.communications.connector.bind-port");
            if (port != null && port.trim().length() > 0) {
                endpointData.setProperty(PREF_RHQ_AGENT_SERVER_BINDPORT, port.trim());
            }
            String address = serverProps.getProperty("rhq.communications.connector.bind-address");
            if (address != null && address.trim().length() > 0) {
                endpointData.setProperty(PREF_RHQ_AGENT_SERVER_BINDADDRESS, address.trim());
            }
        }

        // the public endpoint will override anything that was done above
        String publicEndpoint = serverProps.getProperty("rhq.autoinstall.public-endpoint-address");
        if (publicEndpoint != null && publicEndpoint.trim().length() > 0) {
            endpointData.setProperty(PREF_RHQ_AGENT_SERVER_BINDADDRESS, publicEndpoint.trim());
        }

        return endpointData;
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

    protected void addUndoTaskToStopComponent(final String componentArgument) {
        // component argument must be one of --storage, --server, --agent (a valid argument to the Stop command)
        addUndoTask(new ControlCommand.UndoTask("Stopping component: " + componentArgument) {
            public void performUndoWork() throws Exception {
                Stop stopCommand = new Stop();
                CommandLineParser parser = new RHQPosixParser(true);
                CommandLine cmdLine = parser.parse(stopCommand.getOptions(), new String[] { componentArgument });
                stopCommand.exec(cmdLine);
            }
        });
    }

    private Properties loadStorageProperties() throws IOException {
        Properties properties = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File("bin/rhq-storage.properties"));
            properties.load(fis);

            // Ignore empty values
            Iterator<Map.Entry<Object, Object>> iterator = properties.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, Object> entry = iterator.next();
                String value = (String) entry.getValue();
                if (value == null || value.length() < 1) {
                    iterator.remove();
                }
            }
        } finally {
            if (null != fis) {
                fis.close();
            }
        }

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
