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
import java.lang.reflect.Method;
import java.net.InetAddress;
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
 * @author Heiko W. Rupp
 */
public class Upgrade extends AbstractInstall {

    static private final String FROM_AGENT_DIR_OPTION = "from-agent-dir";
    static private final String FROM_SERVER_DIR_OPTION = "from-server-dir";
    static private final String AGENT_AUTOSTART_OPTION = "agent-auto-start";
    static private final String USE_REMOTE_STORAGE_NODE = "use-remote-storage-node";
    static private final String STORAGE_DATA_ROOT_DIR = "storage-data-root-dir";
    private static final String RUN_DATA_MIGRATION = "run-data-migrator";
    private static final long STORAGE_INSTALL_SLEEP_TIME = 10 * 1000L; // Wait 10s so that ports get recycled

    private Options options;

    public Upgrade() {
        options = new Options()
            .addOption(
                null,
                FROM_AGENT_DIR_OPTION,
                true,
                "Full path to install directory of the RHQ Agent to be upgraded. Required only if an existing agent " +
                    "exists and is not installed in the default location: <from-server-dir>/rhq-agent")
            .addOption(null, FROM_SERVER_DIR_OPTION, true,
                "Full path to install directory of the RHQ Server to be upgraded. Required.")
            .addOption(
                null,
                AGENT_AUTOSTART_OPTION,
                true,
                "If an agent is to be upgraded it will, by default, also be started. However, if this option is set to " +
                    "false, the agent will not be started after it gets upgraded.")
            .addOption(
                null,
                USE_REMOTE_STORAGE_NODE,
                true,
                "By default a server is co-located with a storage node. However, if this option is set to true, no local " +
                    "storage node will be upgraded and it is assumed a remote storage node is configured in rhq-server.properties.")
            .addOption(
                null,
                STORAGE_DATA_ROOT_DIR,
                true,
                "You can use this option to use a different base directory for all the data directories created by the storage node e.g. "
                    + "if the default directory is not writable for the current user (which is under /var/lib on Linux). "
                    + "This is only used if the storage node needs to be newly installed during the upgrade process; otherwise, "
                    + "an error will result if you specify this option. ")
            .addOption(
                null,
                RUN_DATA_MIGRATION,
                true,
                "By default you ned to migrate metrics from a pre RHQ 4.8 system. The upgrade process can trigger this or " +
                    "give you an estimate on the duration. If you want to have fine control over the process, please run the " +
                    "migrator on the command line. Options are none (do nothing), estimate (estimate the migration time only), " +
                    "print-command (print the command line for a manual run) , do-it (run the migration)")
        ;

        options.getOption(AGENT_AUTOSTART_OPTION).setOptionalArg(true);
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

            // Attempt to shutdown any running components. A failure to shutdown a component is not a failure as it
            // really shouldn't be running anyway. This is just an attempt to avoid upgrade problems.
            log.info("Stopping any running RHQ components...");

            // If using non-default agent location then save it so it will be applied to all subsequent rhqctl commands.
            boolean hasFromAgentOption = commandLine.hasOption(FROM_AGENT_DIR_OPTION);
            if (hasFromAgentOption) {
                File agentBasedir = getFromAgentDir(commandLine);
                putProperty(RHQ_AGENT_BASEDIR_PROP, agentBasedir.getPath());
            }

            // If anything appears to be installed already then don't perform an upgrade
            if (isStorageInstalled() || isServerInstalled() || (!hasFromAgentOption && isAgentInstalled())) {
                log.warn("RHQ is already installed so upgrade can not be performed.");
                return;
            }

            // Stop the agent, if running.
            if (hasFromAgentOption) {
                stopAgent(getFromAgentDir(commandLine)); // this is validate the path as well
            }

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

            if (!Boolean.parseBoolean(commandLine.getOptionValue(AGENT_AUTOSTART_OPTION, "true"))) {
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
        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the upgrade command", e);
        }
        if (!isRhq48OrLater(commandLine) && commandLine.hasOption(RUN_DATA_MIGRATION)) {
            runDataMigration(commandLine);
        }

    }

    private void runDataMigration(CommandLine rhqCtlCommandLine) {

        String migrationOption = rhqCtlCommandLine.getOptionValue(RUN_DATA_MIGRATION);

        if (migrationOption.equals("none")) {
            log.info("No data migration will run");
            if (!isRhq48OrLater(rhqCtlCommandLine)) {
                printDataMigrationNotice();
            }
            return;
        }


        // We deduct the database parameters from the server properties
        try {
            File propertiesFile = new File(getBinDir(), "rhq-server.properties");
            Properties serverProperties = new Properties();
            FileInputStream is = new FileInputStream(propertiesFile);
            serverProperties.load(is);

            String dbName = serverProperties.getProperty("rhq.server.database.db-name");
            String dbUser = serverProperties.getProperty("rhq.server.database.user-name");
            String dbType = serverProperties.getProperty("rhq.server.database.type-mapping");
            String dbServerName = serverProperties.getProperty("rhq.server.database.server-name");
            String dbServerPort = serverProperties.getProperty("rhq.server.database.port");
            String dbPasswordProperty = serverProperties.getProperty("rhq.server.database.password");

            if (dbType.toLowerCase().contains("postgres")) {
                dbType = "postgres";
            } else if (dbType.toLowerCase().contains("oracle")) {
                dbType = "oracle";
                throw new RHQControlException("Can not migrate oracle databases yet");
            } else {
                throw new RHQControlException("Unknown database type " + dbType + " can not migrate data");
            }

            // Password in the properties file is obfuscated
            String dbPassword = deobfuscatePassword(dbPasswordProperty);

            File dataMigratorJar = getFileDownload("data-migrator", "rhq-data-migrator");

            String cassandraHost = InetAddress.getLocalHost().getCanonicalHostName();
            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine("java") //
                .addArgument("-jar").addArgument(dataMigratorJar.getAbsolutePath()) //

                .addArgument("--sql-user").addArgument(dbUser)
                .addArgument("--sql-db").addArgument(dbName)
                .addArgument("--sql-host").addArgument(dbServerName)
                .addArgument("--sql-port").addArgument(dbServerPort)
                .addArgument("--sql-server-type").addArgument(dbType)
                .addArgument("--cassandra-hosts").addArgument(cassandraHost);

            String commandLineString = commandLine.toString();
            if (migrationOption.equals("print-command")) {
                log.info(commandLineString);
                return;
            }

            // Add the password after generating commandLineString to
            // not print the password on stdout
            commandLine.addArgument("--sql-password ").addArgument(dbPassword);

            if (migrationOption.equals("estimate")) {
                commandLine.addArgument("--estimate-only");
            }

            Executor executor = new DefaultExecutor();
            executor.setWorkingDirectory(getBaseDir());
            executor.setStreamHandler(new PumpStreamHandler());

            int exitValue = executor.execute(commandLine);
            log.info("The data migrator finished with exit value " + exitValue);

            if (migrationOption.equals("estimate")) {
                log.info("You can use this command line as a start to later run the data migrator\n\n" + commandLineString);
            }


        } catch (Exception e) {
            log.error("Running the data migrator failed - please try to run it from the command line: " + e.getMessage());
        }

    }

    private String deobfuscatePassword(String dbPassword) {

        // We need to do some mumbo jumbo, as the interesting method is private
        // in SecureIdentityLoginModule

        try {
            String className = "org.picketbox.datasource.security.SecureIdentityLoginModule";
            Class<?> clazz = Class.forName(className);
            Object object = clazz.newInstance();
            Method method = clazz.getDeclaredMethod("decode", String.class);
            method.setAccessible(true);
            char[] result = (char[]) method.invoke(object, dbPassword);
            return new String(result);
        } catch (Exception e) {
            throw new RuntimeException("de-obfuscating db password failed: ", e);
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
                // We need to be sure the storage is really stopped (long enough)
                // to not get a port conflict
                // TODO find a better way
                Thread.sleep(STORAGE_INSTALL_SLEEP_TIME);

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

        // remove some old, obsolete settings no longer needed or used
        oldServerProps.remove("rhq.server.embedded-agent.name");
        oldServerProps.remove("rhq.server.embedded-agent.reset-configuration");
        oldServerProps.remove("rhq.server.embedded-agent.disable-native-system");
        oldServerProps.remove("rhq.server.embedded-agent.enabled");
        oldServerProps.remove("rhq.server.startup.jrmpinvoker.rmiport");
        oldServerProps.remove("rhq.server.startup.webservice.port");
        oldServerProps.remove("rhq.server.startup.unifiedinvoker.port");
        oldServerProps.remove("rhq.server.startup.namingservice.rmiport");
        oldServerProps.remove("rhq.server.startup.pooledinvoker.rmiport");
        oldServerProps.remove("rhq.server.startup.ajp.port");
        oldServerProps.remove("rhq.server.startup.namingservice.port");
        oldServerProps.remove("rhq.server.startup.aspectdeployer.bind-port");
        oldServerProps.remove("rhq.server.plugin-deployer-threads");
        oldServerProps.remove("rhq.server.database.xa-datasource-class");
        oldServerProps.remove("rhq.server.database.driver-class");

        // do not set the keystore/truststore algorithms if they are the defaults to allow for runtime defaults to take effect
        String[] algPropNames = new String[] { "rhq.communications.connector.security.truststore.algorithm", //
            "rhq.communications.connector.security.keystore.algorithm", //
            "rhq.server.client.security.keystore.algorithm", //
            "rhq.server.client.security.truststore.algorithm", //
            "rhq.server.tomcat.security.algorithm" };
        for (String algPropName : algPropNames) {
            String algValue = oldServerProps.getProperty(algPropName, "SunX509");
            if (algValue.equals("SunX509") || algValue.equals("IbmX509")) {
                oldServerProps.remove(algPropName); // let the default take effect at runtime - which will depend on the JVM
            }
        }

        // the older servers stored the HTTP and HTTPS ports under different names - make sure we reuse those ports with the new properties
        String httpPort = oldServerProps.getProperty("rhq.server.startup.web.http.port");
        if (httpPort != null) {
            oldServerProps.remove("rhq.server.startup.web.http.port");
            oldServerProps.setProperty("rhq.server.socket.binding.port.http", httpPort);
        }
        String httpsPort = oldServerProps.getProperty("rhq.server.startup.web.https.port");
        if (httpsPort != null) {
            oldServerProps.remove("rhq.server.startup.web.https.port");
            oldServerProps.setProperty("rhq.server.socket.binding.port.https", httpsPort);
        }

        // copy the old key/truststore files from the old location to the new server configuration directory
        copyReferredFile(commandLine, oldServerProps, "rhq.server.tomcat.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.tomcat.security.truststore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.communications.connector.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.communications.connector.security.truststore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.client.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.client.security.truststore.file");

        // now merge the old settings in with the default properties from the new server install
        String newServerPropsFilePath = new File(getBinDir(), "rhq-server.properties").getAbsolutePath();
        PropertiesFileUpdate newServerPropsFile = new PropertiesFileUpdate(newServerPropsFilePath);
        newServerPropsFile.update(oldServerProps);

        return;
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
            // If its not absolute, we assume it is using some default syntax used in earlier versions
            // and we will know this if the value starts with one of the following:
            //    ${jboss.server.config.dir}
            //    ${jboss.server.home.dir}/conf
            //    conf/
            // All of which refer to the old server's configuration directory.
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
            absPath = absPath.replace("${jboss.server.home.dir}/conf", oldServerConfigDir.getAbsolutePath());
            if (absPath.startsWith("conf/")) {
                absPath = absPath.replaceFirst("conf", oldServerConfigDir.getAbsolutePath());
            }
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
            File agentInstallerJar = getFileDownload("rhq-agent", "rhq-enterprise-agent");

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

    private File getFileDownload(String directory, final String fileMatch) {
        File downloadDir = new File(getBaseDir(),
            "modules/org/rhq/rhq-enterprise-server-startup-subsystem/main/deployments/rhq.ear/rhq-downloads/" + directory);
        return downloadDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(fileMatch);
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
