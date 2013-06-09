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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.DeploymentJBossASClient;
import org.rhq.common.jbossas.client.controller.MCCHelper;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.file.FileUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControlException;

/**
 * @author Jay Shaughnessy
 * @author John Mazzitelli 
 */
public class Upgrade extends ControlCommand {

    static private final String FROM_AGENT_DIR_OPTION = "from-agent-dir";
    static private final String FROM_SERVER_DIR_OPTION = "from-server-dir";
    static private final String AGENT_NO_START = "agent-no-start";

    private Options options;

    public Upgrade() {
        options = new Options()
            .addOption(
                null,
                FROM_AGENT_DIR_OPTION,
                true,
                "Full path to install directory of the RHQ Agent to be upgraded. Required only if an existing agent exists and is not installed in the default location: <from-server-dir>/rhq-agent")
            .addOption(null, FROM_SERVER_DIR_OPTION, true,
                "Full path to install directory of the RHQ Server to be upgraded. Required.")
            .addOption(
                null,
                AGENT_NO_START,
                true,
                "If an agent is to be upgraded it will, by default, also be started. However, if this option is set to true, the agent will not be started after it gets upgraded.");

    }

    @Override
    public String getName() {
        return "upgrade";
    }

    @Override
    public String getDescription() {
        return "Upgrades RHQ services from an earlier installed version";
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

            if (isStorageInstalled() || isServerInstalled() || isAgentInstalled()) {
                log.warn("RHQ is already installed so upgrade can not be performed.");
                return;
            }

            // shutdown the server/agent/storage nodes from the old install
            log.info("Stopping old installation components");

            if (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
                stopOldAgent(commandLine);
            }

            org.apache.commons.exec.CommandLine rhqctlStop = getCommandLine(false, "rhqctl", "stop");
            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(new File(getFromServerDir(commandLine), "bin"));
            executor.setStreamHandler(new PumpStreamHandler());
            int exitValue = executor.execute(rhqctlStop);
            if (exitValue == 0) {
                log.info("The old installation components have been stopped");
            } else {
                log.error("The old installation components failed to be stopped. Please stop them manually before continuing. exit code="
                    + exitValue);
                return;
            }

            // now upgrade everything and start them up again
            upgradeServer(commandLine);
            upgradeAgent(commandLine);

            if (!Boolean.parseBoolean(commandLine.getOptionValue(AGENT_NO_START, "false"))) {
                startAgent(new File(getBaseDir(), AGENT_BASEDIR_NAME));
            } else {
                log.info("The agent was upgraded but was told not to start automatically.");
            }

        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the upgrade command", e);
        }
    }

    private void upgradeServer(CommandLine commandLine) throws Exception {
        // copy all the old settings into the new rhq-server.properties file
        upgradeServerPropertiesFile(commandLine);

        // RHQ doesn't ship the Oracle driver. If the user uses Oracle, they have their own driver so we need to copy it over.
        // Because the module.xml has the driver name in it, we need to copy the full Oracle JDBC driver module content.
        String oracleModuleRelativePath = "modules/org/rhq/oracle";
        File oldServerDir = getFromServerDir(commandLine);
        File oldOracleModuleDir = new File(oldServerDir, oracleModuleRelativePath);
        if (oldOracleModuleDir.isDirectory()) {
            File newOracleModuleDir = new File(getBaseDir(), oracleModuleRelativePath);
            FileUtil.purge(newOracleModuleDir, true); // clean out anything that might be in here
            FileUtil.copyDirectory(oldOracleModuleDir, newOracleModuleDir);
        }

        startRHQServerForInstallation();
        runRHQServerInstaller();
        waitForRHQServerToInitialize();
    }

    private void upgradeServerPropertiesFile(CommandLine commandLine) throws Exception {
        File oldServerDir = getFromServerDir(commandLine);
        File oldServerPropsFile = new File(oldServerDir, "bin/rhq-server.properties");
        Properties oldServerProps = new Properties();
        FileInputStream oldServerPropsFileInputStream = new FileInputStream(oldServerPropsFile);
        try {
            oldServerProps.load(oldServerPropsFileInputStream);
        } finally {
            oldServerPropsFileInputStream.close();
        }

        oldServerProps.setProperty("rhq.autoinstall.enabled", "true"); // ensure that we always enable the installer
        oldServerProps.setProperty("rhq.autoinstall.database", "auto"); // the old value could have been "overwrite" - NOT what we want when upgrading

        String newServerPropsFilePath = new File(getBinDir(), "rhq-server.properties").getAbsolutePath();
        PropertiesFileUpdate newServerPropsFile = new PropertiesFileUpdate(newServerPropsFilePath);
        newServerPropsFile.update(oldServerProps);
    }

    private void upgradeAgent(CommandLine rhqctlCommandLine) throws IOException {
        try {
            File oldAgentDir;
            if (rhqctlCommandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
                oldAgentDir = new File(rhqctlCommandLine.getOptionValue(FROM_AGENT_DIR_OPTION));
                if (!oldAgentDir.isDirectory()) {
                    throw new FileNotFoundException("Missing agent to upgrade: " + oldAgentDir.getAbsolutePath());
                }
            } else {
                oldAgentDir = new File(rhqctlCommandLine.getOptionValue(FROM_SERVER_DIR_OPTION), AGENT_BASEDIR_NAME);
                if (!oldAgentDir.isDirectory()) {
                    log.info("No agent found in the old server location... skipping agent upgrade");
                    return;
                }
            }

            log.info("Upgrading RHQ agent located at: " + oldAgentDir.getAbsolutePath());

            File agentBasedir = getAgentBasedir();
            File agentInstallerJar = getAgentInstaller();

            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine("java") //
                .addArgument("-jar").addArgument(agentInstallerJar.getAbsolutePath()) //
                .addArgument("--update=" + oldAgentDir.getAbsolutePath()) //
                .addArgument("--log=" + new File(getLogDir(), "rhq-agent-update.log")) //
                .addArgument("--launch=false"); // we can't launch this copy - we still have to move it to the new location

            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getBaseDir());
            executor.setStreamHandler(new PumpStreamHandler());

            int exitValue = executor.execute(commandLine);
            log.info("The agent installer finished upgrading with exit value " + exitValue);

            // We need to now move the new, updated agent over to the new agent location.
            // renameTo() may fail if we are crossing file system boundaries, so try a true copy as a fallback.
            FileUtil.purge(agentBasedir, true); // clear the way for the new upgraded agent
            if (!oldAgentDir.renameTo(agentBasedir)) {
                FileUtil.copyDirectory(oldAgentDir, agentBasedir);
            }

            log.info("The agent has been upgraded and placed in: " + agentBasedir);

        } catch (IOException e) {
            log.error("An error occurred while upgrading the agent: " + e.getMessage());
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

    private List<String> validateOptions(CommandLine commandLine) {
        List<String> errors = new LinkedList<String>();

        if (!commandLine.hasOption(FROM_SERVER_DIR_OPTION)) {
            errors.add("Missing required option: " + FROM_SERVER_DIR_OPTION);
        } else {
            File fromServerDir = new File(commandLine.getOptionValue(FROM_SERVER_DIR_OPTION));
            if (!fromServerDir.isDirectory()) {
                errors.add("The " + FROM_SERVER_DIR_OPTION + " directory does not exist: [" + fromServerDir.getPath()
                    + "]");
            } else {
                File serverPropsFile = new File(fromServerDir, "bin/rhq-server.properties");
                if (!serverPropsFile.isFile()) {
                    errors.add("The " + FROM_SERVER_DIR_OPTION
                        + " directory does not appear to be an RHQ installation. Missing expected file: ["
                        + serverPropsFile.getPath() + "]");
                }
            }
        }

        if (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
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

        return errors;
    }

    static public File getFromAgentDir(CommandLine commandLine) {
        return (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) ? new File(
            commandLine.getOptionValue(FROM_AGENT_DIR_OPTION)) : null;
    }

    static public File getFromServerDir(CommandLine commandLine) {
        return (commandLine.hasOption(FROM_SERVER_DIR_OPTION)) ? new File(
            commandLine.getOptionValue(FROM_SERVER_DIR_OPTION)) : null;
    }

    private void startRHQServerForInstallation() throws IOException {
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

    private void runRHQServerInstaller() throws IOException {
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

    private void stopOldAgent(CommandLine rhqctlCommandLine) throws Exception {
        log.debug("Stopping old RHQ agent");

        File agentBinDir = new File(getFromAgentDir(rhqctlCommandLine), "bin");
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
}
