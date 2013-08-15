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
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.yaml.snakeyaml.Yaml;

import org.rhq.cassandra.Deployer;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;

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

    private final String STORAGE_BASEDIR = "rhq-storage";

    private final Log log = LogFactory.getLog(StorageInstaller.class);

    private final String VERIFY_DATA_DIRS_EMPTY = "verify-data-dirs-empty";

    private Options options;

    private File serverBasedir;

    private File storageBasedir;

    private int defaultJmxPort = 7299;

    private int rpcPort = 9160;

    private int defaultNativeTransportPort = 9142;

    private int storagePort = 7100;

    private int sslStoragePort = 7101;

    private File logDir;

    private String dirPrefix = (isWindows()) ? "" : "/var/lib";

    private String commitLogDir = dirPrefix + "/rhq/storage/commitlog";

    private String dataDir = dirPrefix + "/rhq/storage/data";

    private String savedCachesDir = dirPrefix + "/rhq/storage/saved_caches";

    private String defaultHeapSize = "512M";

    private String defaultHeapNewSize = "128M";

    public StorageInstaller() {
        String basedir = System.getProperty("rhq.server.basedir");
        serverBasedir = new File(basedir);
        storageBasedir = new File(basedir, STORAGE_BASEDIR);
        logDir = new File(serverBasedir, "logs");

        Option hostname = new Option("n", "hostname", true,
            "The hostname or IP address on which the node will listen for "
                + "requests. Note that if a hostname is specified, the IP address is used. Defaults to the IP " +
            "address of the local host (which depending on hostname configuration may not be localhost).");
        hostname.setArgName("HOSTNAME");

        Option seeds = new Option("s", "seeds", true, "A comma-delimited list of hostnames or IP addresses that "
            + "serve as contact points. Nodes use this list to find each other and to learn the cluster topology. "
            + "It does not need to specify all nodes in the cluster. Defaults to this node's hostname.");
        seeds.setArgName("SEEDS");

        Option jmxPortOption = new Option("j", "jmx-port", true, "The port on which to listen for JMX connections. "
            + "Defaults to " + defaultJmxPort + ".");
        jmxPortOption.setArgName("PORT");

        Option nativeTransportPortOption = new Option("c", "client-port", true, "The port on which to "
            + "listen for client requests. Defaults to " + defaultNativeTransportPort);
        nativeTransportPortOption.setArgName("PORT");

        Option storagePortOption = new Option(null, "storage-port", true, "The port on which to listen for requests "
            + " from other nodes. Defaults to " + storagePort);
        storagePortOption.setArgName("PORT");

        Option sslStoragePortOption = new Option(null, "ssl-storage-port", true, "The port on which to listen for "
            + "encrypted requests from other nodes. Only used when encryption is enabled. Defaults to "
            + sslStoragePort);
        sslStoragePortOption.setArgName("PORT");

        Option startOption = new Option(null, "start", true, "Start the storage node after installing it on disk. "
            + "Defaults to true.");
        startOption.setArgName("true|false");

        Option checkStatus = new Option(null, "check-status", true, "Check the node status to verify that it is up "
            + "after starting it. This option is ignored if the start option is not set. Defaults to true.");
        checkStatus.setArgName("true|false");

        Option commitLogOption = new Option(null, "commitlog", true, "The directory where the storage node keeps "
            + "commit log files. Defaults to " + commitLogDir + ".");
        commitLogOption.setArgName("DIR");

        Option dataDirOption = new Option(null, "data", true, "The directory where the storage node keeps data files. "
            + "Defaults to " + dataDir + ".");
        dataDirOption.setArgName("DIR");

        Option savedCachesDirOption = new Option(null, "saved-caches", true, "The directory where the storage node "
            + "keeps saved cache files. Defaults to " + savedCachesDir + ".");
        savedCachesDirOption.setArgName("DIR");

        Option basedirOption = new Option(null, "dir", true, "The directory where the storage node will be installed "
            + "The default directory will be " + storageBasedir);

        Option heapSizeOption = new Option(null, "heap-size", true, "The value to use for both the min and max heap. "
            + "This value is passed directly to the -Xms and -Xmx options of the Java executable. Defaults to "
            + defaultHeapSize);

        Option heapNewSizeOption = new Option(null, "heap-new-size", true, "The value to use for the new generation "
            + "of the heap. This value is passed directly to the -Xmn option of the Java executable. Defaults to "
            + defaultHeapNewSize);

        Option stackSizeOption = new Option(null, "stack-size", true, "The value to use for the thread stack size. "
            + "This value is passed directly to the -Xss option of the Java executable.");

        Option upgradeOption = new Option(null, "upgrade", true, "Upgrades an existing storage node. The directory "
            + "where the existing RHQ server is installed.");
        upgradeOption.setArgName("RHQ_SERVER_DIR");

        Option verifyDataDirsEmptyOption = new Option(null, VERIFY_DATA_DIRS_EMPTY, true, "Will cause the installer " +
            "to abort if any of the data directories is not empty. Defaults to true.");

        options = new Options().addOption(new Option("h", "help", false, "Show this message.")).addOption(hostname)
            .addOption(seeds).addOption(jmxPortOption).addOption(startOption).addOption(checkStatus)
            .addOption(commitLogOption).addOption(dataDirOption).addOption(savedCachesDirOption)
            .addOption(nativeTransportPortOption).addOption(storagePortOption).addOption(sslStoragePortOption)
            .addOption(basedirOption).addOption(heapSizeOption).addOption(heapNewSizeOption).addOption(stackSizeOption)
            .addOption(upgradeOption).addOption(verifyDataDirsEmptyOption);
    }

    public int run(CommandLine cmdLine) throws Exception {
        if (cmdLine.hasOption("h")) {
            printUsage();
            return STATUS_SHOW_USAGE;
        } else {
            DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
            DeploymentOptions deploymentOptions = factory.newDeploymentOptions();

            String hostname;
            int jmxPort;
            File logFile = new File(logDir, "rhq-storage.log");

            if (cmdLine.hasOption("upgrade")) {
                File existingStorageDir = null;
                File upgradeFromDir = new File(cmdLine.getOptionValue("upgrade", ""));

                if (!upgradeFromDir.isDirectory()) {
                    log.error("The value passed to the upgrade option is not a directory. The value must be a valid "
                        + "path that points to the base directory of an existing RHQ server installation.");
                    return STATUS_INVALID_UPGRADE;
                }
                existingStorageDir = new File(upgradeFromDir, "rhq-storage");
                if (!(existingStorageDir.exists() && existingStorageDir.isDirectory())) {
                    log.error(existingStorageDir + " does not appear to be an existing RHQ storage node installtion. "
                        + "Check the value that was passed to the upgrade option and make sure it specifies the base "
                        + "directory of an existing RHQ server installation.");
                    return STATUS_INVALID_UPGRADE;
                }

                deploymentOptions.setBasedir(storageBasedir.getAbsolutePath());

                Deployer deployer = new Deployer();
                deployer.setDeploymentOptions(deploymentOptions);
                storageBasedir.mkdirs();
                deployer.unzipDistro();
                deployer.updateFilePerms();

                // For upgrades we will copy the existing cassandra.yaml,
                // log4j-server.properties, and cassandra-jvm.properties from the existing
                // storage node installation. Going forward though we need to add support
                // for merging in the existing cassandra.yaml with the new one.
                File oldConfDir = new File(existingStorageDir, "conf");
                File newConfDir = new File(storageBasedir, "conf");

                File cassandraEnvFile = new File(oldConfDir, "cassandra-env.sh");

                String cassandraYaml = "cassandra.yaml";
                String cassandraJvmProps = "cassandra-jvm.properties";
                File cassandraJvmPropsFile = new File(newConfDir, cassandraJvmProps);
                String log4j = "log4j-server.properties";

                replaceFile(new File(oldConfDir, cassandraYaml), new File(newConfDir, cassandraYaml));
                replaceFile(new File(oldConfDir, log4j), new File(newConfDir, log4j));

                if (cassandraEnvFile.exists()) {
                    // Then this is an RHQ 4.8 install
                    jmxPort = parseJmxPortFromCassandrEnv(cassandraEnvFile);
                    Properties jvmProps = new Properties();
                    jvmProps.load(new FileInputStream(cassandraJvmPropsFile));
                    PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(
                        cassandraJvmPropsFile.getAbsolutePath());
                    jvmProps.setProperty("jmx_port", Integer.toString(jmxPort));

                    propertiesUpdater.update(jvmProps);

                } else {
                    jmxPort = parseJmxPort(cassandraJvmPropsFile);
                    replaceFile(new File(oldConfDir, cassandraJvmProps), cassandraJvmPropsFile);
                }

                log.info("Finished installing RHQ Storage Node.");

                log.info("Updating rhq-server.properties...");
                File yamlFile = new File(newConfDir, "cassandra.yaml");

                Yaml yaml = new Yaml();
                Map<String, Object> config = (Map<String, Object>) yaml.load(new FileInputStream(yamlFile));

                hostname = (String) config.get("listen_address");
            } else {
                if (cmdLine.hasOption("dir")) {
                    File basedir = new File(cmdLine.getOptionValue("dir"));
                    deploymentOptions.setBasedir(basedir.getAbsolutePath());
                }

                if (cmdLine.hasOption("n")) {
                    hostname = InetAddress.getByName(cmdLine.getOptionValue("n")).getHostAddress();
                } else {
                    hostname = InetAddress.getLocalHost().getHostAddress();
                }
                deploymentOptions.setListenAddress(hostname);
                deploymentOptions.setRpcAddress(hostname);

                // TODO add support for getting updated seeds list
                // Rather than have the user specify the seeds for each node, the installer can
                // obtain the list either from the RHQ server or directly from querying the
                // database.
                String seeds = cmdLine.getOptionValue("seeds", hostname);
                deploymentOptions.setSeeds(seeds);

                commitLogDir = cmdLine.getOptionValue("commitlog", commitLogDir);
                dataDir = cmdLine.getOptionValue("data", dataDir);
                savedCachesDir = cmdLine.getOptionValue("saved-caches", savedCachesDir);
                File commitLogDirFile = new File(commitLogDir);
                File dataDirFile = new File(dataDir);
                File savedCachesDirFile = new File(savedCachesDir);

                boolean verifyDataDirsEmpty = Boolean.valueOf(cmdLine.getOptionValue(VERIFY_DATA_DIRS_EMPTY, "true"));
                if (verifyDataDirsEmpty) {
                    // validate the three data directories are empty - if they are not, we are probably stepping on
                    // another storage node
                    if (!isDirectoryEmpty(commitLogDirFile)) {
                        log.error("Commitlog directory is not empty. It should not exist for a new Storage Node ["
                            + commitLogDirFile.getAbsolutePath() + "]");
                        return STATUS_DATA_DIR_NOT_EMPTY;
                    }
                    if (!isDirectoryEmpty(dataDirFile)) {
                        log.error("Data directory is not empty. It should not exist for a new Storage Node ["
                            + dataDirFile.getAbsolutePath() + "]");
                        return STATUS_DATA_DIR_NOT_EMPTY;
                    }
                    if (!isDirectoryEmpty(savedCachesDirFile)) {
                        log.error("Saved caches directory is not empty. It should not exist for a new Storage Node ["
                            + savedCachesDirFile.getAbsolutePath() + "]");
                        return STATUS_DATA_DIR_NOT_EMPTY;
                    }
                }

                jmxPort = getPort(cmdLine, "jmx-port", defaultJmxPort);
                int nativeTransportPort = getPort(cmdLine, "client-port", defaultNativeTransportPort);

                deploymentOptions.setCommitLogDir(commitLogDir);
                deploymentOptions.setDataDir(dataDir);
                deploymentOptions.setSavedCachesDir(savedCachesDir);
                deploymentOptions.setLogFileName(logFile.getPath());
                deploymentOptions.setLoggingLevel("INFO");
                deploymentOptions.setRpcPort(rpcPort);
                deploymentOptions.setJmxPort(jmxPort);
                deploymentOptions.setNativeTransportPort(nativeTransportPort);
                deploymentOptions.setStoragePort(getPort(cmdLine, "storage-port", storagePort));
                deploymentOptions.setSslStoragePort(getPort(cmdLine, "ssl-storage-port", sslStoragePort));

                // The out of box default for native_transport_max_threads is 128. We default
                // to 64 for dev/test environments so we need to update it here.
                deploymentOptions.setNativeTransportMaxThreads(128);

                // TODO set defaults for read/write/range timeouts

                deploymentOptions.setHeapSize(cmdLine.getOptionValue("heap-size", defaultHeapSize));
                deploymentOptions.setHeapNewSize(cmdLine.getOptionValue("heap-new-size", defaultHeapNewSize));

                if (cmdLine.hasOption("stack-size")) {
                    deploymentOptions.setStackSize(cmdLine.getOptionValue("stack-size"));
                }

                deploymentOptions.load();

                List<String> errors = new ArrayList<String>();
                checkPerms(options.getOption("saved-caches"), savedCachesDir, errors);
                checkPerms(options.getOption("commitlog"), commitLogDir, errors);
                checkPerms(options.getOption("data"), dataDir, errors);

                if (!errors.isEmpty()) {
                    log.error("Problems have been detected with one or more of the directories in which the storage "
                        + "node will need to store data");
                    for (String error : errors) {
                        log.error(error);
                    }
                    log.error("The storage installer will now exit due to previous errors.");
                    return STATUS_INVALID_FILE_PERMISSIONS;
                }

                Deployer deployer = new Deployer();
                deployer.setDeploymentOptions(deploymentOptions);
                storageBasedir.mkdirs();
                deployer.unzipDistro();
                deployer.applyConfigChanges();
                deployer.updateFilePerms();
                deployer.updateStorageAuthConf(getAddresses(hostname, seeds));

                log.info("Finished installing RHQ Storage Node.");

                PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();
                log.info("Updating rhq-server.properties...");
                serverPropertiesUpdater.update("rhq.cassandra.seeds",
                    getSeedsProperty(hostname, jmxPort, nativeTransportPort));
            }

            boolean startNode = Boolean.parseBoolean(cmdLine.getOptionValue("start", "true"));
            if (startNode) {
                log.info("Starting RHQ Storage Node");
                String startupErrors = startNode(deploymentOptions);
                if (startupErrors == null) {
                    boolean checkStatus = Boolean.parseBoolean(cmdLine.getOptionValue("check-status", "true"));
                    if (checkStatus || isWindows()) { // no reliable pid file on windows
                        if (verifyNodeIsUp(hostname, jmxPort, 5, 3000)) {
                            log.info("RHQ Storage Node is up and running and ready to service client requests");
                            log.info("Installation of the storage node has completed successfully.");
                            return STATUS_NO_ERRORS;
                        } else {
                            log.error("Could not verify that the node is up and running.");
                            log.error("Check the log file at " + logFile + " for errors.");
                            log.error("The storage installer will now exit");
                            return STATUS_FAILED_TO_VERIFY_NODE_UP;
                        }
                    } else {
                        if (isRunning()) {
                            log.info("Installation of the storage node is complete. The node should be up and "
                                + "running");
                            return STATUS_NO_ERRORS;
                        } else {
                            log.warn("Installation of the storage node is complete, but the node does not appear to "
                                + "be running. No start up errors were reported.  Check the log file at " + logFile
                                + " for any other possible errors.");
                            return STATUS_STORAGE_NOT_RUNNING;
                        }
                    }
                } else {
                    // There are platforms where snappy can not be found (OS/X or IBM JRE)
                    // We get a message back from the cassandra node, which is in reality just a
                    // warning. So we can swallow it.
                    boolean foundLinkError = false;
                    boolean harmless = true;
                    if (startupErrors.contains("UnsatisfiedLinkError: no snappyjava")) {
                        log.info("Could not find snappyjava in library path. Will not compress system tables.");
                        log.info("Installation of the storage node is complete");
                        foundLinkError = true;
                    }
                    if (!foundLinkError) {
                        log.error("The storage node reported the following errors while trying to start:\n\n"
                            + startupErrors + "\n\n");
                        harmless = false;
                    }
                    if (startupErrors.contains("java.net.BindException: Address already in use")) {
                        log.error("This error may indicate a conflict for the JMX port.");
                    }
                    if (!harmless) {
                        log.error("Please review your configuration for possible sources of errors such as port "
                            + "conflicts or invalid arguments/options passed to the java executable.");
                        log.error("The storage installer will now exit.");
                        return STATUS_STORAGE_NOT_RUNNING;
                    }
                    else {
                        return STATUS_NO_ERRORS;
                    }
                }
            } else {
                log.info("Installation of the storage node is complete");
                return STATUS_NO_ERRORS;
            }
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

    private int getPort(CommandLine cmdLine, String option, int defaultValue) {
        return Integer.parseInt(cmdLine.getOptionValue(option, Integer.toString(defaultValue)));
    }

    private void checkPerms(Option option, String path, List<String> errors) {
        File dir = new File(path);

        if (dir.exists()) {
            if (dir.isFile()) {
                errors.add(path + " is not a directory. Use the --" + option.getLongOpt() + " to change this value.");
            }
        } else {
            File parent = findParentDir(new File(path));
            if (!parent.canWrite()) {
                errors.add("The user running this installer does not appear to have write permissions to " + parent
                    + ". Either make sure that the user running the storage node has write permissions or use the --"
                    + option.getLongOpt() + " to change this value.");
            }
        }
    }

    private File findParentDir(File path) {
        File dir = path;
        while (!dir.exists()) {
            dir = dir.getParentFile();
        }
        return dir;
    }

    private Set<InetAddress> getAddresses(String hostname, String seeds) throws IOException {
        Set<InetAddress> addresses = new HashSet<InetAddress>();
        addresses.add(InetAddress.getByName(hostname));

        if (!StringUtil.isEmpty(seeds)) {
            for (String seed : seeds.split(",")) {
                addresses.add(InetAddress.getByName(seed));
            }
        }

        return addresses;
    }

    private PropertiesFileUpdate getServerProperties() {
        String sysprop = System.getProperty("rhq.server.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhq.server.properties-file] is not defined.");
        }

        File file = new File(sysprop);
        if (!(file.exists() && file.isFile())) {
            throw new RuntimeException("System property [" + sysprop + "] points to in invalid file.");
        }

        return new PropertiesFileUpdate(file.getAbsolutePath());
    }

    private String getSeedsProperty(String hostname, int jmxPort, int nativeTransportPort) {
        return hostname + "|" + jmxPort + "|" + nativeTransportPort;
    }

    private String startNode(DeploymentOptions deploymentOptions) throws Exception {
        org.apache.commons.exec.CommandLine cmdLine;
        String errOutput;

        if (isWindows()) {
            File basedir = new File(System.getProperty("rhq.server.basedir"));
            basedir = (null == basedir) ? new File(deploymentOptions.getBasedir()).getParentFile() : basedir;
            File binDir = new File(basedir, "bin");

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

            // Third installer the service
            cmdLine = new org.apache.commons.exec.CommandLine("cmd.exe");
            cmdLine.addArgument("/C");
            cmdLine.addArgument("rhq-storage.bat");
            cmdLine.addArgument("install");
            errOutput = exec(binDir, cmdLine);

            if (!errOutput.isEmpty()) {
                return errOutput;
            }

            // Fourth, start the service
            cmdLine = new org.apache.commons.exec.CommandLine("cmd.exe");
            cmdLine.addArgument("/C");
            cmdLine.addArgument("rhq-storage.bat");
            cmdLine.addArgument("start");
            errOutput = exec(binDir, cmdLine);

            if (!errOutput.isEmpty()) {
                return errOutput;
            }

        } else {
            File basedir = new File(deploymentOptions.getBasedir());
            File binDir = new File(basedir, "bin");

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
        PumpStreamHandler streamHandler = new PumpStreamHandler(new NullOutputStream(), buffer);
        executor.setWorkingDirectory(workingDir);
        executor.setStreamHandler(streamHandler);
        String result = "";

        try {
            executor.execute(cmdLine);
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

                return Integer.parseInt(jmxPort);
            } catch (IOException e) {
                log.error("Failed to parse JMX port. There was an unexpected IO error", e);
                throw new RuntimeException("Failed to parse JMX port due to IO error: " + e.getMessage());
            }
        }
    }

    public void printUsage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-storage-installer.sh|bat [options]";
        String header = "";

        helpFormatter.printHelp(syntax, header, getHelpOptions(), null);
    }

    public Options getHelpOptions() {
        Options helpOptions = new Options();
        for (Option option : (Collection<Option>)options.getOptions()) {
            if (option.getLongOpt().equals(VERIFY_DATA_DIRS_EMPTY)) {
                continue;
            }
            helpOptions.addOption(option);
        }
        return helpOptions;
    }

    public Options getOptions() {
        return options;
    }

    public static void main(String[] args) throws Exception {
        StorageInstaller installer = new StorageInstaller();
        installer.log.info("Running RHQ Storage Node installer...");
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(installer.getOptions(), args);
            int status = installer.run(cmdLine);
            System.exit(status);
        } catch (StorageInstallerException e) {
            installer.log.warn(e.getMessage());
            for (String error : e.getErrors()) {
                installer.log.error(error);
            }
            installer.log.error("The storage installer is exiting due to previous errors.");
            System.exit(1);
        } catch (ParseException e) {
            installer.printUsage();
            System.exit(STATUS_SHOW_USAGE);
        }
    }

}
