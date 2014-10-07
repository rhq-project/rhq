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

package org.rhq.storage.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.Deployer;
import org.rhq.cassandra.DeploymentException;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.util.ConfigEditor;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.DbUtil;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.obfuscation.PicketBoxObfuscator;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class StorageInstaller {

    public static final int STATUS_NO_ERRORS = 0;

    public static final int STATUS_STORAGE_NOT_RUNNING = 1;

    public static final int STATUS_FAILED_TO_VERIFY_NODE_UP = 2;

    public static final int STATUS_INVALID_FILE_PERMISSIONS = 3;

    public static final int STATUS_DATA_DIR_NOT_EMPTY = 4;

    public static final int STATUS_SHOW_USAGE = 100;

    public static final int STATUS_INVALID_UPGRADE = 5;

    public static final int STATUS_DEPLOYMENT_ERROR = 6;

    public static final int STATUS_IO_ERROR = 7;

    public static final int STATUS_JMX_PORT_CONFLICT = 8;

    public static final int STATUS_CQL_PORT_CONFLICT = 9;

    public static final int STATUS_GOSSIP_PORT_CONFLICT = 10;

    public static final int STATUS_UNKNOWN_HOST = 11;

    private final String STORAGE_BASEDIR = "rhq-storage";

    static final String STORAGE_LOG_FILE_PATH = "../../logs/rhq-storage.log";

    static final String DEFAULT_COMMIT_LOG_DIR = "../../../rhq-data/commit_log";

    static final String DEFAULT_DATA_DIR = "../../../rhq-data/data";

    static final String DEFAULT_SAVED_CACHES_DIR = "../../../rhq-data/saved_caches";

    static private final Log log = LogFactory.getLog(StorageInstaller.class);

    private Options options;

    private File serverBasedir;

    private File storageBasedir;

    private int defaultJmxPort = 7299;

    private int rpcPort = 9160;

    private int defaultCqlPort = 9142;

    private int defaultGossipPort = 7100;

    private String defaultHeapSize = "512M";

    private String defaultHeapNewSize = "128M";

    public StorageInstaller() {
        String basedir = System.getProperty("rhq.server.basedir");
        serverBasedir = new File(basedir);
        storageBasedir = new File(basedir, STORAGE_BASEDIR);

        Option hostname = new Option("n", StorageProperty.HOSTNAME.property(), true,
            "The hostname or IP address on which the node will listen for "
                + "requests. Note that if a hostname is specified, the IP address is used. Defaults to the IP "
                + "address of the local host (which depending on hostname configuration may not be localhost).");
        hostname.setArgName("HOSTNAME");

        Option seeds = new Option("s", StorageProperty.SEEDS.property(), true,
            "A comma-delimited list of hostnames or IP addresses that "
                + "serve as contact points. Nodes use this list to find each other and to learn the cluster topology. "
                + "It does not need to specify all nodes in the cluster. Defaults to this node's hostname.");
        seeds.setArgName("SEEDS");

        Option jmxPortOption = new Option("j", StorageProperty.JMX_PORT.property(), true,
            "The port on which to listen for JMX connections. " + "Defaults to " + defaultJmxPort + ".");
        jmxPortOption.setArgName("PORT");

        Option cqlPortOption = new Option("c", StorageProperty.CQL_PORT.property(), true, "The port on which to "
            + "listen for client requests. Defaults to " + defaultCqlPort);
        cqlPortOption.setArgName("PORT");

        Option gossipPortOption = new Option(null, StorageProperty.GOSSIP_PORT.property(), true,
            "The port on which to listen for requests " + " from other nodes. Defaults to " + defaultGossipPort);
        gossipPortOption.setArgName("PORT");

        Option startOption = new Option(null, "start", true, "Start the storage node after installing it on disk. "
            + "Defaults to true.");
        startOption.setArgName("true|false");

        Option checkStatus = new Option(null, "check-status", true, "Check the node status to verify that it is up "
            + "after starting it. This option is ignored if the start option is not set. Defaults to true.");
        checkStatus.setArgName("true|false");

        Option commitLogOption = new Option(null, StorageProperty.COMMITLOG.property(), true,
            "The directory where the storage node keeps " + "commit log files. Defaults to " + getDefaultCommitLogDir()
                + ".");
        commitLogOption.setArgName("DIR");

        Option dataDirOption = new Option(null, StorageProperty.DATA.property(), true,
            "The directory where the storage node keeps data files. " + "Defaults to " + getDefaultDataDir() + ".");
        dataDirOption.setArgName("DIR");

        Option savedCachesDirOption = new Option(null, StorageProperty.SAVED_CACHES.property(), true,
            "The directory where the storage node " + "keeps saved cache files. Defaults to "
                + getDefaultSavedCachesDir() + ".");
        savedCachesDirOption.setArgName("DIR");

        Option basedirOption = new Option(null, "dir", true, "The directory where the storage node will be installed "
            + "The default directory will be " + storageBasedir);

        Option heapSizeOption = new Option(null, StorageProperty.HEAP_SIZE.property(), true,
            "The value to use for both the min and max heap. "
                + "This value is passed directly to the -Xms and -Xmx options of the Java executable. Defaults to "
                + defaultHeapSize);

        Option heapNewSizeOption = new Option(null, StorageProperty.HEAP_NEW_SIZE.property(), true,
            "The value to use for the new generation "
                + "of the heap. This value is passed directly to the -Xmn option of the Java executable. Defaults to "
                + defaultHeapNewSize);

        Option stackSizeOption = new Option(null, StorageProperty.STACK_SIZE.property(), true,
            "The value to use for the thread stack size. "
                + "This value is passed directly to the -Xss option of the Java executable.");

        Option upgradeOption = new Option(null, "upgrade", true, "Upgrades an existing storage node. The directory "
            + "where the existing RHQ server is installed.");
        upgradeOption.setArgName("RHQ_SERVER_DIR");

        Option verifyDataDirsEmptyOption = new Option(null, StorageProperty.VERIFY_DATA_DIRS_EMPTY.property(), true,
            "Will cause the installer " + "to abort if any of the data directories is not empty. Defaults to true.");

        options = new Options().addOption(new Option("h", "help", false, "Show this message.")).addOption(hostname)
            .addOption(seeds).addOption(jmxPortOption).addOption(startOption).addOption(checkStatus)
            .addOption(commitLogOption).addOption(dataDirOption).addOption(savedCachesDirOption)
            .addOption(cqlPortOption).addOption(gossipPortOption).addOption(basedirOption).addOption(heapSizeOption)
            .addOption(heapNewSizeOption).addOption(stackSizeOption).addOption(upgradeOption)
            .addOption(verifyDataDirsEmptyOption);
    }

    public int run(CommandLine cmdLine) throws Exception {
        if (cmdLine.hasOption("h")) {
            printUsage();
            return STATUS_SHOW_USAGE;
        }

        InstallerInfo installerInfo = null;
        boolean isUpgrade = cmdLine.hasOption("upgrade");
        File upgradeFromDir = null;

        try {
            if (isUpgrade) {
                upgradeFromDir = new File(cmdLine.getOptionValue("upgrade", ""));
                installerInfo = upgrade(upgradeFromDir);
            } else {
                installerInfo = install(cmdLine);
            }
        } catch (StorageInstallerError e) {
            log.error("An unexpected error occurred", e);
            log.error("The storage installer will exit due to previous errors");
            return e.getErrorCode();
        } catch (StorageInstallerException e) {
            log.warn(e.getMessage());
            log.warn("The storage installer will exit due to previous errors");
            return e.getErrorCode();
        }

        log.info("Updating rhq-server.properties...");
        PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();
        Properties properties = new Properties();

        properties.setProperty("rhq.storage.nodes", installerInfo.hostname);
        properties.setProperty(StorageProperty.CQL_PORT.property(), Integer.toString(installerInfo.cqlPort));
        properties.setProperty(StorageProperty.GOSSIP_PORT.property(), Integer.toString(installerInfo.gossipPort));

        serverPropertiesUpdater.update(properties);

        Properties dbProperties = null;
        if (isUpgrade) {
            File oldServerPropsFile = new File(upgradeFromDir, "bin/rhq-server.properties");
            dbProperties = new Properties();
            FileInputStream oldServerPropsFileInputStream = new FileInputStream(oldServerPropsFile);
            try {
                dbProperties.load(oldServerPropsFileInputStream);
            } finally {
                oldServerPropsFileInputStream.close();
            }
        } else {
            dbProperties = serverPropertiesUpdater.loadExistingProperties();
        }
        stampStorageNodeVersion(dbProperties);

        // start node (and install windows service) if necessary
        File binDir = null;
        if (isWindows()) {
            File basedir = new File(System.getProperty("rhq.server.basedir"));
            basedir = (null == basedir) ? installerInfo.basedir.getParentFile() : basedir;
            binDir = new File(basedir, "bin/internal");
        } else {
            binDir = new File(installerInfo.basedir, "bin");
        }

        boolean startNode = Boolean.parseBoolean(cmdLine.getOptionValue("start", "true"));
        String startupErrors = startNodeIfNecessary(binDir, startNode);

        if (startupErrors != null) {
            log.warn("The storage node reported the following errors while trying to start:\n\n" + startupErrors + "\n");

            if (startupErrors.contains("Port already in use: " + installerInfo.jmxPort)) {
                log.warn("There is a conflict with the JMX port that prevented the storage node JVM "
                    + "from starting.");
                File confDir = new File(storageBasedir, "conf");
                File confFile = new File(confDir, "cassandra-jvm.properties");
                log.info("Change the jmx_port property in " + confFile + " to have the storage node listen "
                    + "on a different port for JMX connections.");

                return STATUS_JMX_PORT_CONFLICT;
            }

            if (startupErrors.contains("java.net.UnknownHostException")) {
                int from = startupErrors.indexOf("java.net.UnknownHostException:")
                    + "java.net.UnknownHostException:".length();
                String hostname = startupErrors.substring(from, startupErrors.indexOf(':', from));
                log.error("Failed to resolve requested binding address. Please check the installation "
                    + "instructions and host DNS settings"
                    + (isWindows() ? "." : " also make sure the hostname alias is set in /etc/hosts.")
                    + " Unknown host: " + hostname);
                log.error("The storage installer will exit due to previous errors");
                return STATUS_UNKNOWN_HOST;
            }

            log.warn("Please review your configuration for possible sources of errors such as port "
                + "conflicts or invalid arguments/options passed to the java executable.");
        }

        if (startNode) {
            boolean checkStatus = Boolean.parseBoolean(cmdLine.getOptionValue("check-status", "true"));
            if (checkStatus || isWindows()) { // no reliable pid file on windows
                if (verifyNodeIsUp(installerInfo.hostname, installerInfo.jmxPort, 5, 3000)) {
                    log.info("RHQ Storage Node is up and running and ready to service client requests");
                    log.info("Installation of the storage node has completed successfully.");
                    return STATUS_NO_ERRORS;

                } else {
                    log.warn("Could not verify that the node is up and running.");
                    log.warn("Check the log file at " + installerInfo.logFile + " for errors.");
                    log.warn("The storage installer will now exit");
                    return STATUS_FAILED_TO_VERIFY_NODE_UP;
                }
            } else {
                if (isRunning()) {
                    log.info("Installation of the storage node is complete. The node should be up and " + "running");
                    return STATUS_NO_ERRORS;

                } else {
                    log.warn("Installation of the storage node is complete, but the node does not appear to "
                        + "be running. No start up errors were reported.  Check the log file at "
                        + installerInfo.logFile + " for any other possible errors.");
                    return STATUS_STORAGE_NOT_RUNNING;
                }
            }
        } else {
            log.info("Installation of the storage node is complete");
            return STATUS_NO_ERRORS;
        }
    }

    private InstallerInfo install(CommandLine cmdLine) throws StorageInstallerException {
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        InstallerInfo installerInfo = new InstallerInfo();

        if (cmdLine.hasOption("dir")) {
            installerInfo.basedir = new File(cmdLine.getOptionValue("dir"));
            deploymentOptions.setBasedir(installerInfo.basedir.getAbsolutePath());
        } else {
            installerInfo.basedir = new File(serverBasedir, "rhq-storage");
            deploymentOptions.setBasedir(installerInfo.basedir.getAbsolutePath());
        }

        try {
            if (cmdLine.hasOption("n")) {
                installerInfo.hostname = cmdLine.getOptionValue("n");
                // Make sure it is a reachable address
                InetAddress.getByName(installerInfo.hostname);
            } else {
                installerInfo.hostname = InetAddress.getLocalHost().getHostName();
            }

            if (InetAddress.getByName(installerInfo.hostname).isLoopbackAddress()) {
                log.warn("This Storage Node is bound to the loopback address " + installerInfo.hostname + " . "
                    + "It will not be able to communicate with Storage Nodes on other machines,"
                    + " and it can only receive client requests from this machine.");
            }

            deploymentOptions.setListenAddress(installerInfo.hostname);
            deploymentOptions.setRpcAddress(installerInfo.hostname);

            String seeds = cmdLine.getOptionValue(StorageProperty.SEEDS.property(), installerInfo.hostname);
            deploymentOptions.setSeeds(seeds);

            String commitlogDir = cmdLine
                .getOptionValue(StorageProperty.COMMITLOG.property(), getDefaultCommitLogDir());
            String dataDir = cmdLine.getOptionValue(StorageProperty.DATA.property(), getDefaultDataDir());
            String savedCachesDir = cmdLine.getOptionValue(StorageProperty.SAVED_CACHES.property(),
                getDefaultSavedCachesDir());

            File commitLogDirFile = new File(commitlogDir);
            File dataDirFile = new File(dataDir);
            File savedCachesDirFile = new File(savedCachesDir);

            boolean verifyDataDirsEmpty = Boolean.valueOf(cmdLine.getOptionValue(
                StorageProperty.VERIFY_DATA_DIRS_EMPTY.property(), "true"));
            if (verifyDataDirsEmpty) {
                // validate the three data directories are empty - if they are not, we are probably stepping on
                // another storage node
                if (!isDirectoryEmpty(commitLogDirFile)) {
                    log.error("Commitlog directory is not empty. It should not exist for a new Storage Node ["
                        + commitLogDirFile.getAbsolutePath() + "]");
                    throw new StorageInstallerException("Installation cannot proceed. The commit log directory "
                        + commitLogDirFile + " is not empty", STATUS_DATA_DIR_NOT_EMPTY);
                }
                if (!isDirectoryEmpty(dataDirFile)) {
                    log.error("Data directory is not empty. It should not exist for a new Storage Node ["
                        + dataDirFile.getAbsolutePath() + "]");
                    throw new StorageInstallerException("Installation cannot proceed. The data directory "
                        + dataDirFile + " is not empty", STATUS_DATA_DIR_NOT_EMPTY);
                }
                if (!isDirectoryEmpty(savedCachesDirFile)) {
                    log.error("Saved caches directory is not empty. It should not exist for a new Storage Node ["
                        + savedCachesDirFile.getAbsolutePath() + "]");
                    throw new StorageInstallerException("Installation cannot proceed. The saved caches directory "
                        + savedCachesDirFile + " is not empty", STATUS_DATA_DIR_NOT_EMPTY);
                }
            }

            verifyPortStatus(cmdLine, installerInfo);

            deploymentOptions.setCommitLogDir(commitlogDir);
            // TODO add support for specifying multiple dirs
            deploymentOptions.setDataDir(dataDirFile.getPath());
            deploymentOptions.setSavedCachesDir(savedCachesDir);

            deploymentOptions.setLogFileName(installerInfo.logFile);
            deploymentOptions.setLoggingLevel("INFO");

            deploymentOptions.setRpcPort(rpcPort);
            deploymentOptions.setCqlPort(installerInfo.cqlPort);
            deploymentOptions.setGossipPort(installerInfo.gossipPort);
            deploymentOptions.setJmxPort(installerInfo.jmxPort);

            deploymentOptions
                .setHeapSize(cmdLine.getOptionValue(StorageProperty.HEAP_SIZE.property(), defaultHeapSize));
            deploymentOptions.setHeapNewSize(cmdLine.getOptionValue(StorageProperty.HEAP_NEW_SIZE.property(),
                defaultHeapNewSize));
            if (cmdLine.hasOption(StorageProperty.STACK_SIZE.property())) {
                deploymentOptions.setStackSize(cmdLine.getOptionValue(StorageProperty.STACK_SIZE.property()));
            }

            // The out of box default for native_transport_max_threads is 128. We default
            // to 64 for dev/test environments so we need to update it here.
            deploymentOptions.setNativeTransportMaxThreads(128);

            deploymentOptions.load();

            List<String> errors = new ArrayList<String>();
            checkPerms(options.getOption(StorageProperty.SAVED_CACHES.property()), savedCachesDir, errors);
            checkPerms(options.getOption(StorageProperty.COMMITLOG.property()), commitlogDir, errors);
            checkPerms(options.getOption(StorageProperty.DATA.property()), dataDir, errors);

            if (!errors.isEmpty()) {
                log.error("Problems have been detected with one or more of the directories in which the storage "
                    + "node will need to store data");
                for (String error : errors) {
                    log.error(error);
                }
                throw new StorageInstallerException(
                    "Installation cannot proceed. There are problems with one or more of "
                        + "the storage data directories.", STATUS_INVALID_FILE_PERMISSIONS);
            }

            Deployer deployer = getDeployer();
            deployer.setDeploymentOptions(deploymentOptions);
            storageBasedir.mkdirs();
            deployer.unzipDistro();
            deployer.applyConfigChanges();
            deployer.updateFilePerms();

            deployer.updateStorageAuthConf(asSet(installerInfo.hostname));

            return installerInfo;
        } catch (UnknownHostException unknownHostException) {
            throw new StorageInstallerException(
                "Failed to resolve requested binding address. Please check the installation instructions and host DNS settings"
                    + (isWindows() ? "." : " also make sure the hostname alias is set in /etc/hosts.")
                    + " Unknown host " + unknownHostException.getMessage(), unknownHostException, STATUS_UNKNOWN_HOST);
        } catch (IOException e) {
            throw new StorageInstallerError("The upgrade cannot proceed. An unexpected I/O error occurred", e,
                STATUS_IO_ERROR);
        } catch (DeploymentException e) {
            throw new StorageInstallerException("The installation cannot proceed. An error occurred during storage "
                + "node deployment.", e, STATUS_DEPLOYMENT_ERROR);
        }
    }

    private void verifyPortStatus(CommandLine cmdLine, InstallerInfo installerInfo) throws StorageInstallerException {
        installerInfo.jmxPort = getPort(cmdLine, StorageProperty.JMX_PORT.property(), defaultJmxPort);
        isPortBound(installerInfo.hostname, installerInfo.jmxPort, StorageProperty.JMX_PORT.property(),
            STATUS_JMX_PORT_CONFLICT);

        installerInfo.cqlPort = getPort(cmdLine, StorageProperty.CQL_PORT.property(), defaultCqlPort);
        isPortBound(installerInfo.hostname, installerInfo.cqlPort, StorageProperty.CQL_PORT.property(),
            STATUS_CQL_PORT_CONFLICT);

        installerInfo.gossipPort = getPort(cmdLine, StorageProperty.GOSSIP_PORT.property(), defaultGossipPort);
        isPortBound(installerInfo.hostname, installerInfo.gossipPort, StorageProperty.GOSSIP_PORT.property(),
            STATUS_GOSSIP_PORT_CONFLICT);
    }

    /**
     * This can be overridden to allow for custom deploy behavior.
     * @return a Deployer
     */
    protected Deployer getDeployer() {
        return new Deployer();
    }

    private InstallerInfo upgrade(File upgradeFromDir) throws StorageInstallerException {
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        InstallerInfo installerInfo = new InstallerInfo();

        File existingStorageDir = null;

        if (!upgradeFromDir.isDirectory()) {
            log.error("The value passed to the upgrade option is not a directory. The value must be a valid "
                + "path that points to the base directory of an existing RHQ server installation.");
            throw new StorageInstallerException("The upgrade cannot proceed. The value passed to the upgrade option "
                + "is invalid.", STATUS_INVALID_UPGRADE);
        }
        existingStorageDir = new File(upgradeFromDir, "rhq-storage");
        if (!(existingStorageDir.exists() && existingStorageDir.isDirectory())) {
            log.error(existingStorageDir + " does not appear to be an existing RHQ storage node installation. "
                + "Check the value that was passed to the upgrade option and make sure it specifies the base "
                + "directory of an existing RHQ server installation.");
            throw new StorageInstallerException("The upgrade cannot proceed. " + existingStorageDir + " is not an "
                + "existing RHQ storage node installation", STATUS_INVALID_UPGRADE);
        }

        try {
            File oldConfDir = new File(existingStorageDir, "conf");
            File oldYamlFile = new File(oldConfDir, "cassandra.yaml");
            File newConfDir = new File(storageBasedir, "conf");
            File newYamlFile = new File(newConfDir, "cassandra.yaml");
            File cassandraEnvFile = new File(oldConfDir, "cassandra-env.sh");
            File cassandraJvmPropsFile = new File(newConfDir, "cassandra-jvm.properties");

            installerInfo.basedir = storageBasedir;

            boolean isRHQ48Install;
            if (cassandraEnvFile.exists()) {
                isRHQ48Install = true;
                installerInfo.jmxPort = parseJmxPortFromCassandrEnv(cassandraEnvFile);
            } else {
                isRHQ48Install = false;
                installerInfo.jmxPort = parseJmxPort(new File(oldConfDir, "cassandra-jvm.properties"));
            }

            deploymentOptions.setBasedir(storageBasedir.getAbsolutePath());
            deploymentOptions.setLogFileName(installerInfo.logFile);
            deploymentOptions.setLoggingLevel("INFO");
            deploymentOptions.setJmxPort(installerInfo.jmxPort);
            deploymentOptions.setHeapSize(defaultHeapSize);
            deploymentOptions.setHeapNewSize(defaultHeapNewSize);

            deploymentOptions.load();

            Deployer deployer = new Deployer();
            deployer.setDeploymentOptions(deploymentOptions);
            storageBasedir.mkdirs();
            deployer.unzipDistro();
            deployer.applyConfigChanges();
            deployer.updateFilePerms();

            ConfigEditor oldYamlEditor = new ConfigEditor(oldYamlFile);
            oldYamlEditor.load();
            ConfigEditor newYamlEditor = new ConfigEditor(newYamlFile);
            newYamlEditor.load();

            installerInfo.hostname = oldYamlEditor.getListenAddress();
            newYamlEditor.setListenAddress(installerInfo.hostname);
            newYamlEditor.setRpcAddress(installerInfo.hostname);

            installerInfo.cqlPort = oldYamlEditor.getNativeTransportPort();
            newYamlEditor.setNativeTransportPort(installerInfo.cqlPort);

            installerInfo.gossipPort = oldYamlEditor.getStoragePort();
            newYamlEditor.setStoragePort(installerInfo.gossipPort);

            newYamlEditor.setCommitLogDirectory(oldYamlEditor.getCommitLogDirectory());
            newYamlEditor.setSavedCachesDirectory(oldYamlEditor.getSavedCachesDirectory());
            newYamlEditor.setDataFileDirectories(oldYamlEditor.getDataFileDirectories());
            newYamlEditor.setSeeds(installerInfo.hostname);

            newYamlEditor.save();

            if (isRHQ48Install) {
                Properties jvmProps = new Properties();
                jvmProps.load(new FileInputStream(cassandraJvmPropsFile));
                PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(
                    cassandraJvmPropsFile.getAbsolutePath());
                jvmProps.setProperty("jmx_port", Integer.toString(installerInfo.jmxPort));

                propertiesUpdater.update(jvmProps);

                deployer.updateStorageAuthConf(asSet(installerInfo.hostname));
            } else {
                File oldStorageAuthConfFile = new File(oldConfDir, "rhq-storage-auth.conf");
                File newStorageAuthConfFile = new File(newConfDir, "rhq-storage-auth.conf");
                StreamUtil.copy(new FileInputStream(oldStorageAuthConfFile), new FileOutputStream(
                    newStorageAuthConfFile));
            }

            return installerInfo;

        } catch (UnknownHostException unknownHostException) {
            throw new StorageInstallerException(
                "Failed to resolve requested binding address. Please check the installation instructions and host DNS settings"
                    + (isWindows() ? "." : " also make sure the hostname alias is set in /etc/hosts.")
                    + " Unknown host " + unknownHostException.getMessage(), unknownHostException, STATUS_UNKNOWN_HOST);
        } catch (IOException e) {
            throw new StorageInstallerError("The upgrade cannot proceed. An unexpected I/O error occurred", e,
                STATUS_IO_ERROR);
        } catch (DeploymentException e) {
            throw new StorageInstallerException("THe upgrade cannot proceed. An error occurred during the storage "
                + "node deployment", e, STATUS_DEPLOYMENT_ERROR);
        }
    }

    private boolean isDirectoryEmpty(File dir) {
        // TODO need to check subdirectories
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            return (files == null || files.length == 0);
        } else {
            return true;
        }
    }

    private Set<String> asSet(String string) {
        TreeSet<String> set = new TreeSet<String>();
        set.add(string);
        return set;
    }

    private int getPort(CommandLine cmdLine, String option, int defaultValue) {
        return Integer.parseInt(cmdLine.getOptionValue(option, Integer.toString(defaultValue)));
    }

    private void checkPerms(Option option, String path, List<String> errors) {
        try {
            log.info("Checking perms for " + path);
            File dir = new File(path);
            if (!dir.isAbsolute()) {
                dir = new File(new File(storageBasedir, "bin"), path);
            }
            dir = dir.getCanonicalFile();

            if (dir.exists()) {
                if (dir.isFile()) {
                    errors.add(path + " is not a directory. Use the --" + option.getLongOpt()
                        + " to change this value.");
                }
            } else {
                File parentDir = dir.getParentFile();
                while (!parentDir.exists()) {
                    parentDir = parentDir.getParentFile();
                }

                if (!parentDir.canWrite()) {
                    errors
                        .add("The user running this installer does not appear to have write permissions to "
                            + parentDir
                            + ". Either make sure that the user running the storage node has write permissions or use the --"
                            + option.getLongOpt() + " to change this value.");
                }
            }
        } catch (Exception e) {
            errors
                .add("The request path cannot be constructed (path: "
                    + path
                    + "). "
                    + "Please use a valid and also make sure the user running the storage node has write permissions for the path.");
        }
    }

    private void isPortBound(String address, int port, String portName, int potentialErrorCode)
        throws StorageInstallerException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(address, port));
        } catch (BindException e) {
            throw new StorageInstallerException("The " + portName + " (" + address + ":" + port
                + ") is already in use. " + "Installation cannot proceed.", potentialErrorCode);
        } catch (IOException e) {
            // We only log a warning here and let the installation proceed in case the
            // exception is something that can be ignored.
            log.warn("An unexpected error occurred while checking the " + portName + " port", e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    log.error("An error occurred trying to close the connection to the " + portName, e);
                }
            }
        }
    }

    private PropertiesFileUpdate getServerProperties() {
        String sysprop = System.getProperty("rhq.server.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhq.server.properties-file] is not defined.");
        }

        File file = new File(sysprop);
        if (!(file.exists() && file.isFile())) {
            throw new RuntimeException("System property [" + sysprop + "] points to an invalid file.");
        }

        return new PropertiesFileUpdate(file.getAbsolutePath());
    }

    private String startNodeIfNecessary(File binDir, boolean startNode) throws Exception {
        org.apache.commons.exec.CommandLine cmdLine;
        String errOutput;

        if (isWindows()) {
            // First, stop the service if it exists
            cmdLine = new org.apache.commons.exec.CommandLine("cmd.exe");
            cmdLine.addArgument("/C");
            cmdLine.addArgument("rhq-storage.bat");
            cmdLine.addArgument("stop");
            errOutput = exec(binDir, cmdLine);

            if (!errOutput.isEmpty()) {
                return errOutput;
            }

            // Second, remove it if it exists
            cmdLine = new org.apache.commons.exec.CommandLine("cmd.exe");
            cmdLine.addArgument("/C");
            cmdLine.addArgument("rhq-storage.bat");
            cmdLine.addArgument("remove");
            errOutput = exec(binDir, cmdLine);

            if (!errOutput.isEmpty()) {
                return errOutput;
            }

            // Third install the service
            cmdLine = new org.apache.commons.exec.CommandLine("cmd.exe");
            cmdLine.addArgument("/C");
            cmdLine.addArgument("rhq-storage.bat");
            cmdLine.addArgument("install");
            errOutput = exec(binDir, cmdLine);

            if (!errOutput.isEmpty()) {
                return errOutput;
            }

            // Fourth, start the service if necessary
            if (startNode) {
                log.info("Starting RHQ Storage Node");

                cmdLine = new org.apache.commons.exec.CommandLine("cmd.exe");
                cmdLine.addArgument("/C");
                cmdLine.addArgument("rhq-storage.bat");
                cmdLine.addArgument("start");
                errOutput = exec(binDir, cmdLine);

                if (!errOutput.isEmpty()) {
                    return errOutput;
                }
            }

        } else if (startNode) {
            log.info("Starting RHQ Storage Node");

            cmdLine = new org.apache.commons.exec.CommandLine("./cassandra");
            cmdLine.addArgument("-p");
            cmdLine.addArgument(new File(binDir, "cassandra.pid").getAbsolutePath());
            errOutput = exec(binDir, cmdLine);

            if (!errOutput.isEmpty()) {
                return errOutput;
            }
        }

        return null;
    }

    private String exec(File workingDir, org.apache.commons.exec.CommandLine cmdLine) throws Exception {
        Executor executor = new DefaultExecutor();
        org.apache.commons.io.output.ByteArrayOutputStream buffer = new org.apache.commons.io.output.ByteArrayOutputStream();
        NullOutputStream nullOs = new NullOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(nullOs, buffer);
        executor.setWorkingDirectory(workingDir);
        executor.setStreamHandler(streamHandler);
        String result = "";

        try {
            exec(executor, cmdLine);
            result = buffer.toString();

        } finally {
            try {
                buffer.close();
                nullOs.close();
            } catch (Exception e) {
                // best effort
            }
        }

        return result;
    }

    // This is just a test hook
    protected void exec(Executor executor, org.apache.commons.exec.CommandLine cmdLine) throws IOException {
        executor.execute(cmdLine);
    }

    private boolean isWindows() {
        String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.US);
        return operatingSystem.contains("windows");
    }

    private boolean isRunning() {
        File binDir = new File(storageBasedir, "bin");
        return new File(binDir, "cassandra.pid").exists();
    }

    boolean verifyNodeIsUp(String address, int jmxPort, int retries, long timeout) throws Exception {
        String url = "service:jmx:rmi:///jndi/rmi://" + address + ":" + jmxPort + "/jmxrmi";
        JMXServiceURL serviceURL = new JMXServiceURL(url);
        JMXConnector connector = null;
        MBeanServerConnection serverConnection = null;

        // Sleep a few seconds to work around https://issues.apache.org/jira/browse/CASSANDRA-5467
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        Map<String, String> env = new HashMap<String, String>();
        for (int i = 0; i < retries; ++i) {
            try {
                connector = JMXConnectorFactory.connect(serviceURL, env);
                serverConnection = connector.getMBeanServerConnection();
                ObjectName storageService = new ObjectName("org.apache.cassandra.db:type=StorageService");
                Boolean nativeTransportRunning = (Boolean) serverConnection.getAttribute(storageService,
                    "NativeTransportRunning");

                return nativeTransportRunning;
            } catch (Exception e) {
                if (i < retries) {
                    if (log.isDebugEnabled()) {
                        log.debug("The storage node is not up.", e);
                    } else {
                        Throwable rootCause = ThrowableUtil.getRootCause(e);
                        log.info("The storage node is not up: " + rootCause.getClass().getName() + ": "
                            + rootCause.getMessage());
                    }
                    log.info("Checking storage node status again in " + (timeout * (i + 1)) + " ms...");
                }
                Thread.sleep(timeout * (i + 1));
            }
        }
        return false;
    }

    private void replaceFile(File oldFile, File newFile) throws IOException {
        log.info("Copying " + oldFile + " to " + newFile);
        if (!oldFile.exists()) {
            log.warn(oldFile + " does not exist. " + newFile.getName() + " will be created.");
        } else {
            newFile.delete();
            try {
                FileUtil.copyFile(oldFile, newFile);
            } catch (IOException e) {
                log.error("There was an error while copying " + oldFile + " to " + " " + newFile, e);
                throw e;
            }
        }
    }

    private int parseJmxPortFromCassandrEnv(File cassandraEnvFile) {
        Integer port = null;
        if (isWindows()) {
            // TODO
            return defaultJmxPort;
        } else {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(cassandraEnvFile));
                String line = reader.readLine();

                while (line != null) {
                    if (line.startsWith("JMX_PORT")) {
                        int startIndex = "JMX_PORT=\"".length();
                        int endIndex = line.lastIndexOf("\"");

                        if (startIndex == -1 || endIndex == -1) {
                            log.error("Failed to parse the JMX port. Make sure that you have the JMX port defined on its "
                                + "own line as follows, JMX_PORT=\"<jmx-port>\"");
                            throw new RuntimeException("Cannot determine JMX port");
                        }
                        try {
                            port = Integer.parseInt(line.substring(startIndex, endIndex));
                        } catch (NumberFormatException e) {
                            log.error("The JMX port must be an integer. [" + port + "] is an invalid value");
                            throw new RuntimeException("The JMX port has an invalid value");
                        }
                        return port;
                    }
                    line = reader.readLine();
                }
                log.error("Failed to parse the JMX port. Make sure that you have the JMX port defined on its "
                    + "own line as follows, JMX_PORT=\"<jmx-port>\"");
                throw new RuntimeException("Cannot determine JMX port");
            } catch (IOException e) {
                log.error("Failed to parse JMX port. There was an unexpected IO error", e);
                throw new RuntimeException("Failed to parse JMX port due to IO error: " + e.getMessage());
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("An error occurred closing the " + BufferedReader.class.getName() + " used to "
                            + "parse the JMX port", e);
                    } else {
                        log.warn("There was error closing the reader used to parse the JMX port: " + e.getMessage());
                    }
                }
            }
        }
    }

    private int parseJmxPort(File cassandraJvmOptsFile) {
        Integer port = null;
        if (isWindows()) {
            // TODO
            return defaultJmxPort;
        } else {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(cassandraJvmOptsFile));

                String jmxPort = properties.getProperty("jmx_port");
                if (StringUtil.isEmpty(jmxPort)) {
                    log.error("The property [jmx_port] is undefined.");
                    throw new RuntimeException("Cannot determine JMX port");
                }

                jmxPort = jmxPort.replaceAll("\"", "");

                return Integer.parseInt(jmxPort);
            } catch (IOException e) {
                log.error("Failed to parse JMX port. There was an unexpected IO error", e);
                throw new RuntimeException("Failed to parse JMX port due to IO error: " + e.getMessage());
            }
        }
    }

    /**
     * @return The parent directory of the server
     */
    private File getInstallationDir() {
        return serverBasedir.getParentFile();
    }

    private File getDefaultBaseDataDir() {
        return new File(getInstallationDir(), "rhq-data");
    }

    private String getDefaultCommitLogDir() {
        return DEFAULT_COMMIT_LOG_DIR;
    }

    private String getDefaultDataDir() {
        return DEFAULT_DATA_DIR;
    }

    private String getDefaultSavedCachesDir() {
        return DEFAULT_SAVED_CACHES_DIR;
    }

    private static boolean stampStorageNodeVersion(Properties serverProperties) throws Exception {
        final String dbUrl = serverProperties.getProperty("rhq.server.database.connection-url");
        final String dbUsername = serverProperties.getProperty("rhq.server.database.user-name");
        String obfuscatedDbPassword = serverProperties.getProperty("rhq.server.database.password");
        String clearTextDbPassword = PicketBoxObfuscator.decode(obfuscatedDbPassword);

        boolean result = updateStorageNodeVersion(dbUrl, dbUsername, clearTextDbPassword,
            serverProperties.getProperty("rhq.storage.nodes"));

        return result;
    }

    /**
     * Update server version stamp with the install version.
     *
     * @param connectionUrl
     * @param username
     * @param password
     * @param storageNodeAddress
     * @return true if updated, false otherwise
     *
     * @throws Exception if failed to communicate with the database
     */
    private static boolean updateStorageNodeVersion(String connectionUrl, String username, String password,
        String storageNodeAddress) throws Exception {
        DatabaseType db = null;
        Connection conn = null;
        PreparedStatement stm = null;
        boolean result = false;
        String version = StorageInstaller.class.getPackage().getImplementationVersion();

        try {
            conn = DbUtil.getConnection(connectionUrl, username, password);
            db = DatabaseTypeFactory.getDatabaseType(conn);

            // For two reasons we add the column here, as opposed to db-upgrade.xml. First, a SN upgrade may
            // happen before a Server upgrade, so db-upgrade may not have yet run.  Second, we can limit the
            // setting of the version to the row in question, db-upgrade would not know which row to set.
            boolean columnExists = db.checkColumnExists(conn, "rhq_storage_node", "version");
            if (!columnExists) {
                db.addColumn(conn, "RHQ_STORAGE_NODE", "VERSION", "VARCHAR2", "255");
                stm = conn.prepareStatement("UPDATE rhq_storage_node SET version = ?");
                stm.setString(1, "PRE-" + version);
                stm.executeUpdate();
                db.closeStatement(stm);
            }

            stm = conn.prepareStatement("UPDATE rhq_storage_node SET version = ? WHERE address = ?");
            stm.setString(1, version);
            stm.setString(2, storageNodeAddress);
            result = (1 == stm.executeUpdate());

            // set column not null after it's been set
            if (!columnExists) {
                db.alterColumn(conn, "RHQ_STORAGE_NODE", "VERSION", "VARCHAR2", null, "255", false, false);
            }

        } catch (IllegalStateException e) {
            log.info("Unable to update storage node [" + storageNodeAddress + "] to version [" + version
                + "], column does not exist.");
        } catch (SQLException e) {
            log.info("Unable to update storage node [" + storageNodeAddress + "] to version [" + version + "] "
                + e.getMessage());
        } finally {
            if (null != db) {
                db.closeJDBCObjects(conn, stm, null);
            }
        }

        return result;
    }

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-storage-installer.sh|bat [options]";
        String header = "";

        helpFormatter.printHelp(syntax, header, getHelpOptions(), null);
    }

    public Options getHelpOptions() {
        Options helpOptions = new Options();
        for (Option option : (Collection<Option>) options.getOptions()) {
            if (option.getLongOpt().equals(StorageProperty.VERIFY_DATA_DIRS_EMPTY)) {
                continue;
            }
            helpOptions.addOption(option);
        }
        return helpOptions;
    }

    public Options getOptions() {
        return options;
    }

    private static class InstallerInfo {
        File basedir;
        String logFile = STORAGE_LOG_FILE_PATH;
        int jmxPort;
        int cqlPort;
        int gossipPort;
        String hostname;
    }

    public static void main(String[] args) throws Exception {
        StorageInstaller installer = new StorageInstaller();
        installer.log.info("Running RHQ Storage Node installer...");
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(installer.getOptions(), args);
            int status = installer.run(cmdLine);
            System.exit(status);
        } catch (ParseException parseException) {
            installer.printUsage();
            System.exit(STATUS_SHOW_USAGE);
        }
    }

}
