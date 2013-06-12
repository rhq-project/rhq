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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.file.FileUtil;
import org.rhq.server.control.RHQControlException;

/**
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class Upgrade extends AbstractInstall {

    static private final String FROM_AGENT_DIR_OPTION = "from-agent-dir";
    static private final String FROM_SERVER_DIR_OPTION = "from-server-dir";
    static private final String AGENT_NO_START = "agent-no-start";
    static private final String USE_REMOTE_STORAGE_NODE = "use-remote-storage-node";
    static private final String STORAGE_DATA_ROOT_DIR = "storage-data-root-dir";

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
                "If an agent is to be upgraded it will, by default, also be started. However, if this option is set to true, the agent will not be started after it gets upgraded.")
            .addOption(
                null,
                USE_REMOTE_STORAGE_NODE,
                true,
                "By default a server is co-located with a storage node. However, if this option is set to true, no local storage node will be upgraded and it is assumed a remote storage node is configured in rhq-server.properties.")
            .addOption(
                null,
                STORAGE_DATA_ROOT_DIR,
                true,
                "You can use this option to use a different base directory for all the data directories created by the storage node. This is only used if the storage node needs to be newly installed during the upgrade process; otherwise, an error will result if you specify this option.")
        ;
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

            // If using a non-default agent location then save it so it will be applied to all subsequent rhqctl
            // commands.  Then, use it to stop the agent, if running.
            boolean hasFromAgentOption = commandLine.hasOption(FROM_AGENT_DIR_OPTION);
            if (hasFromAgentOption) {
                File agentBasedir = getFromAgentDir(commandLine);
                putProperty(RHQ_AGENT_BASEDIR_PROP, agentBasedir.getPath());
                stopAgent(agentBasedir); // this is validate the path as well
            }

            // If anything appears to be installed already then don't perform an upgrade
            if (isStorageInstalled() || isServerInstalled() || (!hasFromAgentOption && isAgentInstalled())) {
                log.warn("RHQ is already installed so upgrade can not be performed.");
                return;
            }

            // Attempt to shutdown any running components. A failure to shutdown a component is not a failure as it
            // really shouldn't be running anyway. This is just an attempt to avoid upgrade problems.
            log.info("Stopping any running RHQ components...");

            // If rhqctl exists in the old version, use it to stop everything, otherwise, just try and stop the server
            // using the legacy script.
            File fromBinDir = new File(getFromServerDir(commandLine), "bin");
            String serverScriptName = getRhqServerScriptName();
            String fromScript = isRhq48OrLater(commandLine) ? "rhqctl" : serverScriptName;
            org.apache.commons.exec.CommandLine rhqctlStop = getCommandLine(false, fromScript, "stop");
            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(fromBinDir);
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
            upgradeStorage(commandLine);
            upgradeServer(commandLine);
            upgradeAgent(commandLine);

            if (Boolean.parseBoolean(commandLine.getOptionValue(AGENT_NO_START, "false"))) {
                log.info("The agent was upgraded but was told not to start automatically.");
            } else {
                File agentDir ;

                if (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
                   agentDir = new File(commandLine.getOptionValue(FROM_AGENT_DIR_OPTION));
                }  else {
                    agentDir = new File(getBaseDir(), AGENT_BASEDIR_NAME);
                }
                startAgent(agentDir, true);
            }
            printDataMigrationNotice();

        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the upgrade command", e);
        }
    }

    private String getRhqServerScriptName() {
        String rhqServerBase = "rhq-server";
        if (File.separatorChar=='/') {
            rhqServerBase = rhqServerBase + ".sh";
        }
        else {
            rhqServerBase = rhqServerBase + ".bat";
        }
        return rhqServerBase;
    }

    private void upgradeStorage(CommandLine rhqctlCommandLine) throws Exception {
        if (rhqctlCommandLine.hasOption(USE_REMOTE_STORAGE_NODE)) {
            log.info("Ignoring storage node upgrade, a remote storage node is configured.");
            return;
        }

        // If upgrading from a pre-cassandra then just install an initial storage node. Otherwise, upgrade
        if (isRhq48OrLater(rhqctlCommandLine)) {
            try {
                org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-storage-installer", "--upgrade",
                    getFromServerDir(rhqctlCommandLine).getAbsolutePath());
                Executor executor = new DefaultExecutor();
                executor.setWorkingDirectory(getBinDir());
                executor.setStreamHandler(new PumpStreamHandler());

                int exitCode = executor.execute(commandLine);
                log.info("The storage node upgrade has finished with an exit value of " + exitCode);

            } catch (IOException e) {
                log.error("An error occurred while running the storage node upgrade: " + e.getMessage());
                throw e;
            }

        } else {
            installStorageNode(getStorageBasedir(), rhqctlCommandLine);
        }
    }

    private void upgradeServer(CommandLine commandLine) throws Exception {
        // don't upgrade the server if this is a storage node only install
        File oldServerDir = getFromServerDir(commandLine);
        if (!(!isRhq48OrLater(commandLine) || isServerInstalled(oldServerDir))) {
            log.info("Ignoring server upgrade, this is a storage node only installation.");
            return;
        }

        // copy all the old settings into the new rhq-server.properties file
        upgradeServerPropertiesFile(commandLine);

        // RHQ doesn't ship the Oracle driver. If the user uses Oracle, they have their own driver so we need to copy it over.
        // Because the module.xml has the driver name in it, we need to copy the full Oracle JDBC driver module content.
        String oracleModuleRelativePath = "modules/org/rhq/oracle";
        File oldOracleModuleDir = new File(oldServerDir, oracleModuleRelativePath);
        if (oldOracleModuleDir.isDirectory()) {
            File newOracleModuleDir = new File(getBaseDir(), oracleModuleRelativePath);
            File newOracleModuleMainDir = new File(newOracleModuleDir, "main");
            // Look in the new server install and see if we do not have a real oracle JDBC driver.
            // If the new server only has our "dummy" driver, we copy over the old driver module to the new server.
            // If the new server already has a "real" driver, leave anything else in place as it may be a newer driver.
            boolean foundRealOracleDriver = false;
            for (File f : newOracleModuleMainDir.listFiles()) {
                foundRealOracleDriver = f.isFile() && f.length() > 100000L; // the actual driver is much bigger, our fake one is 1K or so
                if (foundRealOracleDriver == true) {
                    break; // we found the real driver, do not continue looking
                }
            }
            if (!foundRealOracleDriver) {
                FileUtil.purge(newOracleModuleDir, true); // clean out anything that might be in here
                FileUtil.copyDirectory(oldOracleModuleDir, newOracleModuleDir);
            }
        }

        // copy over any wrapper.inc that may have been added
        File oldWrapperIncFile = new File(oldServerDir, "bin/wrapper/rhq-server-wrapper.inc");
        if (oldWrapperIncFile.exists()) {
            File newWrapperIncFile = new File(getBaseDir(), "bin/wrapper/rhq-server-wrapper.inc");
            FileUtil.copyFile(oldWrapperIncFile, newWrapperIncFile);
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

        // copy the old key/truststore files from the old location to the new server configuration directory
        copyReferredFile(commandLine, oldServerProps, "rhq.server.tomcat.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.tomcat.security.truststore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.communications.connector.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.communications.connector.security.truststore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.client.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.client.security.truststore.file");

        String newServerPropsFilePath = new File(getBinDir(), "rhq-server.properties").getAbsolutePath();
        PropertiesFileUpdate newServerPropsFile = new PropertiesFileUpdate(newServerPropsFilePath);
        newServerPropsFile.update(oldServerProps);
    }

    /**
     * Given server properties and a property name whose value is a file path, see if we need to copy the referred
     * file to the new server location. This will set the new property value in the given properties if it needed to be
     * updated.
     *
     * @param commandLine the rhqctl command line
     * @param properties the properties
     * @param propertyName name of a property whose value refers to a file path
     */
    private void copyReferredFile(CommandLine commandLine, Properties properties, String propertyName) {
        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue == null || propertyValue.trim().length() == 0) {
            return;
        }

        File referredFile = new File(propertyValue);
        boolean originalFilePathIsAbsolute = referredFile.isAbsolute();
        if (!originalFilePathIsAbsolute) {
            // if its not absolute, we assume it is using ${jboss.server.config.dir}
            File oldServerConfigDir = new File(getFromServerDir(commandLine), "jbossas/standalone/configuration");
            if (!oldServerConfigDir.isDirectory()) {
                // the older RHQ releases had the old JBossAS 4.2.3 directory structure
                oldServerConfigDir = new File(getFromServerDir(commandLine), "jbossas/server/default/conf");
                if (!oldServerConfigDir.isDirectory()) {
                    log.warn("Cannot determine the old server's configuration directory - cannot copy over the old file: " + referredFile);
                    return;
                }
            }

            String absPath = propertyValue.replace("${jboss.server.config.dir}", oldServerConfigDir.getAbsolutePath());
            referredFile = new File(absPath);
        }

        if (!referredFile.isFile()) {
            log.info("Server property [" + propertyName + "] refers to file [" + referredFile
                + "] that does not exist. Skipping.");
            return;
        }

        File newServerConfigDir = new File(getBaseDir(), "jbossas/standalone/configuration");
        File newFile = new File(newServerConfigDir, referredFile.getName());
        try {
            FileUtil.copyFile(referredFile, newFile);
        } catch (Exception e) {
            // log a message about this problem, but we will let the upgrade continue
            log.error("Failed to copy the old file ["
                + referredFile
                + "] referred to by server property ["
                + propertyName + "] to the new location of [" + newFile
                + "]. You will need to manually copy that file to the new location."
                + "The server may not work properly until you do this.");
        }
        properties.setProperty(propertyName, "${jboss.server.config.dir}/" + newFile.getName());

        return;
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
            if (!agentBasedir.equals(oldAgentDir)) {
                FileUtil.purge(agentBasedir, true); // clear the way for the new upgraded agent
                if (!oldAgentDir.renameTo(agentBasedir)) {
                    FileUtil.copyDirectory(oldAgentDir, agentBasedir);
                }
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

        if (isRhq48OrLater(commandLine)) {
            if (commandLine.hasOption(STORAGE_DATA_ROOT_DIR)) {
                errors.add("You cannot use the option --" + STORAGE_DATA_ROOT_DIR + " for your installation.");
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

    protected boolean isRhq48OrLater(CommandLine commandLine) {
        return new File(getFromServerDir(commandLine), "bin/rhqctl").exists();
    }

    private void printDataMigrationNotice() {
        log.info("\n================\n" +
            "If this was an upgrade from a RHQ version before 4.8,\n " +
            "you need to run the data migration job to transfer stored (historic)\n" +
            "metrics data from the relational database into the new storage.\n" +
            "Until the migration has run, that historic data is not available \n" +
            "in e.g. the charting views.\n\n" +
            "To run the data migration, you can download the migration app from the\n" +
            "server and run it on the command line.\n" +
            "================\n");
    }

}
