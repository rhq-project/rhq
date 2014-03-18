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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileReverter;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.file.FileVisitor;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.server.control.ControlCommand;
import org.rhq.server.control.RHQControl;
import org.rhq.server.control.RHQControlException;
import org.rhq.server.control.util.ExecutorAssist;

/**
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 * @author Heiko W. Rupp
 */
public class Upgrade extends AbstractInstall {

    private static final String FROM_AGENT_DIR_OPTION = "from-agent-dir";
    private static final String FROM_SERVER_DIR_OPTION = "from-server-dir";
    private static final String USE_REMOTE_STORAGE_NODE = "use-remote-storage-node";
    private static final String STORAGE_DATA_ROOT_DIR = "storage-data-root-dir";
    private static final String RUN_DATA_MIGRATION = "run-data-migrator";

    private Options options;

    public Upgrade() {
        options = new Options()
            .addOption(
                null,
                FROM_AGENT_DIR_OPTION,
                true,
                "Full path to install directory of the RHQ Agent to be upgraded. Required only if an existing agent "
                    + "exists and is not installed in the default location: <from-server-dir>/../rhq-agent")
            .addOption(null, FROM_SERVER_DIR_OPTION, true,
                "Full path to install directory of the RHQ Server to be upgraded. Required.")
            .addOption(
                null,
                START_OPTION,
                false,
                "If specified then immediately start the services after upgrade.  Note that services may be started and shut down as part of the upgrade process, but will not be started or left running by default.")
            .addOption(
                null,
                USE_REMOTE_STORAGE_NODE,
                true,
                "By default a server is co-located with a storage node. However, if this option is set to true, no local "
                    + "storage node will be upgraded and it is assumed a remote storage node is configured in rhq-server.properties.")
            .addOption(
                null,
                STORAGE_DATA_ROOT_DIR,
                true,
                "This option is valid only when upgrading from older systems that did not have storage nodes. Use this option to specify a non-default base "
                    + "directory for the data directories created by the storage node. For example, if the default directory is "
                    + "not writable for the current user (/var/lib on Linux) or if you simply prefer a different location. ")
            .addOption(
                null,
                RUN_DATA_MIGRATION,
                true,
                "This option is valid only when upgrading from older systems that did not have storage nodes. The existing metric data needs to migrate to "
                    + "the metric storage.  The upgrade process can trigger this or give you an estimate on the duration. If you want "
                    + "to have fine control over the process, please run the migrator on the command line. Options are none (do "
                    + "nothing), estimate (estimate the migration time only), print-command (print the command line for a manual run), "
                    + "do-it (run the migration)");
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
    protected String getReadmeFilename() {
        return "UPGRADE_README.txt";
    }

    @Override
    protected int exec(CommandLine commandLine) {
        int rValue = RHQControl.EXIT_CODE_OK;
        boolean start = commandLine.hasOption(START_OPTION);

        try {
            List<String> errors = validateOptions(commandLine);
            if (!errors.isEmpty()) {
                for (String error : errors) {
                    log.error(error);
                }
                log.error("Exiting due to the previous errors");
                return RHQControl.EXIT_CODE_OPERATION_FAILED;
            }

            // Attempt to shutdown any running components. A failure to shutdown a component is not a failure as it
            // really shouldn't be running anyway. This is just an attempt to avoid upgrade problems.
            log.info("Stopping any running RHQ components...");

            // If using non-default agent location then save it so it will be applied to all subsequent rhqctl commands.
            boolean hasFromAgentOption = commandLine.hasOption(FROM_AGENT_DIR_OPTION);
            if (hasFromAgentOption) {
                File agentBasedir = getFromAgentDir(commandLine);
                setAgentBasedir(agentBasedir);
            }

            // If storage or server appear to be installed already then don't perform an upgrade.  It's OK
            // if the agent already exists in the default location, it may be there from a prior install.
            if (isStorageInstalled() || isServerInstalled()) {
                log.warn("RHQ is already installed so upgrade can not be performed.");
                return RHQControl.EXIT_CODE_OPERATION_FAILED;
            }

            // Attempt to shutdown any running components. A failure to shutdown a component is not a failure as it
            // really shouldn't be running anyway. This is just an attempt to avoid upgrade problems.
            log.info("Stopping any running RHQ components...");

            // Stop the agent, if running.
            if (hasFromAgentOption) {
                rValue = Math.max(rValue, killAgent(getFromAgentDir(commandLine))); // this validates the path as well
            }

            // If rhqctl exists in the old version, use it to stop old components, otherwise, just try and stop the
            // server using the legacy script. If there is no rhqctl, there is no storage node anyway, so we just
            // stop server in that case.
            File fromBinDir = new File(getFromServerDir(commandLine), "bin");
            org.apache.commons.exec.CommandLine rhqctlStop = isRhq48OrLater(commandLine) ? getCommandLine(false,
                "rhqctl", "stop") : getCommandLine("rhq-server", "stop");

            int exitValue = ExecutorAssist.execute(fromBinDir, rhqctlStop);
            if (exitValue == 0) {
                log.info("The old installation components have been stopped");
            } else {
                log.error("The old installation components failed to be stopped. Please stop them manually before continuing. exit code="
                    + exitValue);
                return exitValue;
            }

            // If any failures occur during upgrade, we know we need to reset rhq-server.properties.
            final FileReverter serverPropFileReverter = new FileReverter(getServerPropertiesFile());
            addUndoTask(new ControlCommand.UndoTask("Reverting server properties file") {
                public void performUndoWork() throws Exception {
                    try {
                        serverPropFileReverter.revert();
                    } catch (Exception e) {
                        throw new Exception("Cannot reset rhq-server.properties - revert settings manually", e);
                    }
                }
            });

            // now upgrade everything
            rValue = Math.max(rValue, upgradeStorage(commandLine));
            rValue = Math.max(rValue, upgradeServer(commandLine));
            rValue = Math.max(rValue, upgradeAgent(commandLine));

            File agentDir;

            if (commandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
                agentDir = new File(commandLine.getOptionValue(FROM_AGENT_DIR_OPTION));
            } else {
                agentDir = getAgentBasedir();
            }

            updateWindowsAgentService(agentDir);

            if (start) {
                rValue = Math.max(rValue, startAgent(agentDir));
            }
        } catch (Exception e) {
            throw new RHQControlException("An error occurred while executing the upgrade command", e);
        } finally {
            try {
                if (!start) {
                    Stop stopCommand = new Stop();
                    stopCommand.exec(new String[] { "stop", "--server" });
                    if (!commandLine.hasOption(RUN_DATA_MIGRATION)) {
                        rValue = Math.max(rValue, stopCommand.exec(new String[] { "--storage" }));
                    }
                }
            } catch (Throwable t) {
                log.warn("Unable to stop services: " + t.getMessage());
                rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
            }
        }

        if (!isRhq48OrLater(commandLine) && commandLine.hasOption(RUN_DATA_MIGRATION)) {
            rValue = Math.max(rValue, runDataMigration(commandLine));
        }
        return rValue;
    }

    private int runDataMigration(CommandLine rhqctlCommandLine) {

        String migrationOption = rhqctlCommandLine.getOptionValue(RUN_DATA_MIGRATION);

        int rValue;

        if (migrationOption.equals("none")) {
            log.info("No data migration will run");
            if (!isRhq48OrLater(rhqctlCommandLine)) {
                printDataMigrationNotice();
            }
            return RHQControl.EXIT_CODE_OK;
        }

        // We deduct the database parameters from the server properties
        try {
            org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-data-migration");
            commandLine.addArgument("-X");
            if (migrationOption.equals("estimate")) {
                commandLine.addArgument("--estimate-only");
            }

            int exitValue = ExecutorAssist.execute(new File(getBaseDir(), "bin"), commandLine);
            log.info("The data migrator finished with exit value " + exitValue);
            rValue = exitValue;
        } catch (Exception e) {
            log.error("Running the data migrator failed - please try to run it from the command line: "
                + e.getMessage());
            rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
        }
        return rValue;
    }

    private int upgradeStorage(CommandLine rhqctlCommandLine) throws Exception {
        if (rhqctlCommandLine.hasOption(USE_REMOTE_STORAGE_NODE)) {
            log.info("Ignoring storage node upgrade, a remote storage node is configured.");
            return RHQControl.EXIT_CODE_OK;
        }

        int rValue;

        // If upgrading from a pre-cassandra then just install an initial storage node. Otherwise, upgrade
        if (isRhq48OrLater(rhqctlCommandLine)) {
            try {
                // We need to be sure the storage is really stopped (long enough) to not get a port conflict
                waitForProcessToStop(getStoragePid());

                addUndoTaskToStopComponent("--storage");
                // if the upgrade fails, we need to purge the new storage node basedir to allow for user to try again
                // later
                addUndoTask(new ControlCommand.UndoTask("Removing new storage node install directory") {
                    public void performUndoWork() {
                        FileUtil.purge(getStorageBasedir(), true);
                    }
                });

                org.apache.commons.exec.CommandLine commandLine = getCommandLine("rhq-storage-installer", "--upgrade",
                    getFromServerDir(rhqctlCommandLine).getAbsolutePath());

                rValue = ExecutorAssist.execute(getBinDir(), commandLine);
                log.info("The storage node upgrade has finished with an exit value of " + rValue);
            } catch (IOException e) {
                log.error("An error occurred while running the storage node upgrade: " + e.getMessage());
                throw e;
            }

        } else {
            rValue = installStorageNode(getStorageBasedir(), rhqctlCommandLine, true);
        }
        return rValue;
    }

    private int upgradeServer(CommandLine commandLine) throws Exception {
        // don't upgrade the server if this is a storage node only install
        File oldServerDir = getFromServerDir(commandLine);
        if (!(!isRhq48OrLater(commandLine) || isServerInstalled(oldServerDir))) {
            log.info("Ignoring server upgrade, this is a storage node only installation.");
            return RHQControl.EXIT_CODE_OK;
        }

        // copy all the old settings into the new rhq-server.properties file
        upgradeServerPropertiesFile(commandLine);

        int rValue = RHQControl.EXIT_CODE_OK;
        // make sure we retain the oracle driver if one exists
        try {
            copyOracleDriver(oldServerDir);
        } catch (Exception e) {
            log.error("Failed to copy the old Oracle driver to the new server. "
                + "The upgrade will continue but your server may not work if connecting to an Oracle database, "
                + "in which case you will need to manually install an Oracle driver to your server. " + "Cause: "
                + ThrowableUtil.getAllMessages(e));
            rValue = RHQControl.EXIT_CODE_OPERATION_FAILED;
        }

        // copy over any wrapper.inc that may have been added
        File oldWrapperIncFile = new File(oldServerDir, "bin/wrapper/rhq-server-wrapper.inc");
        if (oldWrapperIncFile.exists()) {
            File newWrapperIncFile = new File(getBaseDir(), "bin/wrapper/rhq-server-wrapper.inc");
            FileUtil.copyFile(oldWrapperIncFile, newWrapperIncFile);
        }

        // start the server, the invoke the installer and wait for the server to be completely installed
        rValue = Math.max(rValue, startRHQServerForInstallation());
        int installerStatusCode = runRHQServerInstaller();
        rValue = Math.max(rValue, installerStatusCode);
        if (installerStatusCode == RHQControl.EXIT_CODE_OK) {
            waitForRHQServerToInitialize();
        }

        return rValue;
    }

    public void copyOracleDriver(File oldServerDir) throws IOException {
        // RHQ doesn't ship the Oracle driver. If the user uses Oracle, they have their own driver so we need to copy it over.
        // Because the module.xml has the driver name in it, we need to copy the full Oracle JDBC driver module content.
        // Look in the new server install and see if we do not have a real oracle JDBC driver.
        // If the new server only has our "dummy" driver, we copy over the old driver module to the new server.
        // If the new server already has a "real" driver, leave anything else in place as it may be a newer driver.
        String oracleModuleRelativePath = "modules/org/rhq/oracle";
        File newOracleModuleDir = new File(getBaseDir(), oracleModuleRelativePath);
        File newOracleModuleMainDir = new File(newOracleModuleDir, "main");

        // first see if the new server was already given a real oracle driver - if so, there is nothing for us to do
        for (File f : newOracleModuleMainDir.listFiles()) {
            boolean foundRealOracleDriver = f.isFile() && f.length() > 100000L; // the actual driver is much bigger, our fake one is small
            if (foundRealOracleDriver == true) {
                log.info("Looks like the new server already has an Oracle driver: " + f);
                return; // nothing for us to do since the new server already appears to have a real oracle driver
            }
        }

        // now see if we are updating a newer JBossAS7+ based server - if so, the old oracle driver is in a module
        File oldOracleModuleDir = new File(oldServerDir, oracleModuleRelativePath);
        if (oldOracleModuleDir.isDirectory()) {
            FileUtil.purge(newOracleModuleDir, true); // clean out anything that might be in here
            FileUtil.copyDirectory(oldOracleModuleDir, newOracleModuleDir);
            log.info("Copied the old Oracle JDBC module [" + oldOracleModuleDir + "] to the new server: "
                + newOracleModuleDir);
        } else {
            // we aren't updating a newer JBossAS7+ based server, its probably an older JBossAS 4.2.3 based server
            // where the oracle jar is located in a different directory (jbossas/server/default/lib/ojdbc*.jar)
            File oldLibDir = new File(oldServerDir, "jbossas/server/default/lib");
            if (oldLibDir.isDirectory()) {
                FilenameFilter oracleDriverFilenameFilter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith("ojdbc") && name.endsWith(".jar");
                    }
                };
                // find the old ojdbc driver
                File[] oracleDriver = oldLibDir.listFiles(oracleDriverFilenameFilter);
                if (oracleDriver != null && oracleDriver.length > 0) {
                    if (oracleDriver.length > 1) {
                        log.warn("It appears that more than one oracle driver exists in the old server at ["
                            + oldLibDir + "]; this one will be reused: " + oracleDriver[0]);
                    }
                    // we need to remove the dummy oracle driver file from the new server first
                    File[] dummy = newOracleModuleMainDir.listFiles(oracleDriverFilenameFilter);
                    if (dummy != null) {
                        for (File dummyFileToDelete : dummy) { // there should only be one, but just remove all ojdbc*.jar files
                            dummyFileToDelete.delete();
                        }
                    }
                    // copy the real oracle driver to our new server's oracle module
                    File newOracleJarFile = new File(newOracleModuleMainDir, oracleDriver[0].getName());
                    FileUtil.copyFile(oracleDriver[0], newOracleJarFile);
                    log.info("Copied the old Oracle JDBC driver [" + oracleDriver[0] + "] to the new server: "
                        + newOracleJarFile);

                    // now we need to update the module.xml file so it points to the new oracle driver
                    File moduleXmlFile = new File(newOracleModuleMainDir, "module.xml");
                    String originalXml = new String(StreamUtil.slurp(new FileInputStream(moduleXmlFile)));
                    String newXml = originalXml.replaceFirst("resource-root path.*=.*\"ojdbc.*jar\"",
                        "resource-root path=\"" + oracleDriver[0].getName() + "\"");
                    FileUtil.writeFile(new ByteArrayInputStream(newXml.getBytes()), moduleXmlFile);
                    log.info("Updated module.xml [" + moduleXmlFile + "] to use the proper Oracle driver");
                }
            }
        }

        return;
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
        oldServerProps.remove("java.rmi.server.hostname");

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

        //Migrate storage node properties
        String storageUsername = oldServerProps.getProperty("rhq.cassandra.username");
        if (storageUsername != null) {
            oldServerProps.remove("rhq.cassandra.username");
            oldServerProps.setProperty("rhq.storage.username", storageUsername);
        }

        String storagePassword = oldServerProps.getProperty("rhq.cassandra.password");
        if (storagePassword != null) {
            // In RHQ 4.8 the Cassandra username/password had to be rhqadmin/rhqadmin; so,
            // we can safely set rhq.storage.password to the obfuscated version freeing the
            // user of performing the additional step of generated the obfuscated password.
            oldServerProps.remove("rhq.cassandra.password");
            oldServerProps.setProperty("rhq.storage.password", "1eeb2f255e832171df8592078de921bc");
        }

        String storageSeeds = oldServerProps.getProperty("rhq.cassandra.seeds");
        if (storageSeeds != null) {
            StringBuffer storageNodes = new StringBuffer();
            String cqlPort = "";

            String[] unparsedNodes = storageSeeds.split(",");
            for (int index = 0; index < unparsedNodes.length; index++) {
                String[] params = unparsedNodes[index].split("\\|");
                if (params.length == 3) {
                    storageNodes.append(params[0]);
                    if (index < unparsedNodes.length - 1) {
                        storageNodes.append(",");
                    }

                    cqlPort = params[2];
                }
            }

            oldServerProps.remove("rhq.cassandra.seeds");
            oldServerProps.setProperty("rhq.storage.nodes", storageNodes.toString());
            oldServerProps.setProperty("rhq.storage.cql-port", cqlPort);
        }

        String storageCompression = oldServerProps.getProperty("rhq.cassandra.client.compression-enabled");
        if (storageCompression != null) {
            oldServerProps.remove("rhq.cassandra.client.compression-enabled");
            oldServerProps.setProperty("rhq.storage.client.compression-enabled", storageCompression);
        }

        // copy the old key/truststore files from the old location to the new server configuration directory
        copyReferredFile(commandLine, oldServerProps, "rhq.server.tomcat.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.tomcat.security.truststore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.communications.connector.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.communications.connector.security.truststore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.client.security.keystore.file");
        copyReferredFile(commandLine, oldServerProps, "rhq.server.client.security.truststore.file");

        // for oracle, ensure the unused properties are set to unused otherwise prop file validation may fail
        String dbType = oldServerProps.getProperty("rhq.server.database.type-mapping");
        if (null != dbType && dbType.toLowerCase().contains("oracle")) {
            oldServerProps.setProperty("rhq.server.database.server-name", "unused");
            oldServerProps.setProperty("rhq.server.database.port", "unused");
            oldServerProps.setProperty("rhq.server.database.db-name", "unused");
        }

        // now merge the old settings in with the default properties from the new server install
        String newServerPropsFilePath = getServerPropertiesFile().getAbsolutePath();
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
                    log.warn("Cannot determine the old server's configuration directory - cannot copy over the old file: "
                        + referredFile);
                    return;
                }
            }

            String absPath = propertyValue.replace("${jboss.server.config.dir}", oldServerConfigDir.getAbsolutePath());
            absPath = absPath.replace("${jboss.server.home.dir}/conf",
                useForwardSlash(oldServerConfigDir.getAbsolutePath()));
            if (absPath.startsWith("conf/")) {
                absPath = absPath.replaceFirst("conf", useForwardSlash(oldServerConfigDir.getAbsolutePath()));
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
            log.error("Failed to copy the old file [" + referredFile + "] referred to by server property ["
                + propertyName + "] to the new location of [" + newFile
                + "]. You will need to manually copy that file to the new location."
                + "The server may not work properly until you do this.");
        }
        properties.setProperty(propertyName, "${jboss.server.config.dir}/" + newFile.getName());

        return;
    }

    private String useForwardSlash(String path) {
        return (null != path) ? path.replace('\\', '/') : null;
    }

    private int upgradeAgent(CommandLine rhqctlCommandLine) throws Exception {
        try {
            File oldAgentDir;
            if (rhqctlCommandLine.hasOption(FROM_AGENT_DIR_OPTION)) {
                oldAgentDir = new File(rhqctlCommandLine.getOptionValue(FROM_AGENT_DIR_OPTION));
                if (!oldAgentDir.isDirectory()) {
                    throw new FileNotFoundException("Missing agent to upgrade: " + oldAgentDir.getAbsolutePath());
                }
            } else {
                oldAgentDir = null;
                File fromServerDir = getFromServerDir(rhqctlCommandLine);
                if (fromServerDir != null && fromServerDir.isDirectory()) {
                    File fromServerDirParent = fromServerDir.getParentFile();
                    if (fromServerDirParent != null && fromServerDirParent.isDirectory()) {
                        oldAgentDir = new File(fromServerDirParent, "rhq-agent");
                    }
                }
                if (!oldAgentDir.isDirectory()) {
                    log.info("No " + FROM_AGENT_DIR_OPTION
                        + " option specified and no agent found in the default location ["
                        + oldAgentDir.getAbsolutePath()
                        + "]. Installing agent in the default location as part of the upgrade.");
                    return installAgent(getAgentBasedir(), rhqctlCommandLine);
                }
            }

            log.info("Upgrading RHQ agent located at: " + oldAgentDir.getAbsolutePath());

            final File agentBasedir = getAgentBasedir();
            File agentInstallerJar = getFileDownload("rhq-agent", "rhq-enterprise-agent");

            // Make sure we use the appropriate java version, don't just fall back to PATH
            String javaExeFilePath = System.getProperty("rhq.java-exe-file-path");

            org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(javaExeFilePath) //
                .addArgument("-jar").addArgument(agentInstallerJar.getAbsolutePath()) //
                .addArgument("--update=" + oldAgentDir.getAbsolutePath()) //
                .addArgument("--log=" + new File(getLogDir(), "rhq-agent-update.log")) //
                .addArgument("--launch=false"); // we can't launch this copy - we still have to move it to the new location

            int exitValue = ExecutorAssist.execute(getBaseDir(), commandLine);
            log.info("The agent installer finished upgrading with exit value " + exitValue);

            // We need to now move the new, updated agent over to the new agent location.
            // renameTo() may fail if we are crossing file system boundaries, so try a true copy as a fallback.
            if (!agentBasedir.equals(oldAgentDir)) {
                FileUtil.purge(agentBasedir, true); // clear the way for the new upgraded agent
                if (!oldAgentDir.renameTo(agentBasedir)) {
                    FileUtil.copyDirectory(oldAgentDir, agentBasedir);

                    // we need to retain the execute bits for the executable scripts and libraries
                    FileVisitor visitor = new FileVisitor() {
                        @Override
                        public void visit(File file) {
                            String filename = file.getName();
                            if (filename.contains(".so") || filename.contains(".sl")
                                || filename.contains(".dylib")) {
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

            addUndoTask(new ControlCommand.UndoTask("Removing agent install directory") {
                public void performUndoWork() {
                    FileUtil.purge(agentBasedir, true);
                }
            });

            log.info("The agent has been upgraded and placed in: " + agentBasedir);
            return exitValue;

        } catch (IOException e) {
            log.error("An error occurred while upgrading the agent: " + e.getMessage());
            throw e;
        }
    }

    private File getFileDownload(String directory, final String fileMatch) {
        File downloadDir = new File(getBaseDir(),
            "modules/org/rhq/server-startup/main/deployments/rhq.ear/rhq-downloads/" + directory);
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
                errors.add("The option --" + STORAGE_DATA_ROOT_DIR
                    + " is valid only for upgrades from older systems that did not have storage nodes.");
            }

            if (commandLine.hasOption(RUN_DATA_MIGRATION)) {
                errors.add("The option --" + RUN_DATA_MIGRATION
                    + " is valid only for upgrades from older systems that did not have storage nodes.");
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

    protected boolean isRhq410OrLater(CommandLine commandLine) {
        return new File(getFromServerDir(commandLine), "bin/internal").isDirectory();
    }

    private void printDataMigrationNotice() {
        log.info("\n================\n"
            + "If this was an upgrade from older systems that did not have storage nodes,\n "
            + "you need to run the data migration job to transfer stored (historic)\n"
            + "metrics data from the relational database into the new storage.\n"
            + "Until the migration has run, that historic data is not available \n" + "in e.g. the charting views.\n\n"
            + "To run the data migration, just run rhq-data-migration.{sh|bat}\n"
            + "script located in the server bin folder.\n" + "================\n");
    }

}
