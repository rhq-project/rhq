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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

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
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.installer.RMIContextFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author John Sanda
 */
public class StorageInstaller {

    public static final int STATUS_NO_ERRORS = 0;

    public static final int STATUS_STORAGE_NOT_RUNNING = 1;

    public static final int STATUS_FAILED_TO_VERIFY_NODE_UP = 2;

    public static final int STATUS_INVALID_FILE_PERMISSIONS = 3;

    public static final int STATUS_SHOW_USAGE = 4;

    private final String STORAGE_BASEDIR = "rhq-storage";

    private final Log log = LogFactory.getLog(StorageInstaller.class);

    private Options options;

    private File serverBasedir;

    private File storageBasedir;

    private int jmxPort = 7299;

    private int rpcPort = 9160;

    private int nativeTransportPort = 9142;

    private int storagePort = 7100;

    private int sslStoragePort = 7101;

    private File logDir;

    private String dirPrefix = (isWindows()) ? "" : "/var/lib";

    private String commitLogDir = dirPrefix + "/rhq/storage/commitlog";

    private String dataDir = dirPrefix + "/rhq/storage/data";

    private String savedCachesDir = dirPrefix + "/rhq/storage/saved_caches";

    public StorageInstaller() {
        String basedir = System.getProperty("rhq.server.basedir");
        serverBasedir = new File(basedir);
        storageBasedir = new File(basedir, "rhq-storage");
        logDir = new File(serverBasedir, "logs");

        Option hostname = new Option("n", "hostname", true,
            "The hostname or IP address on which the node will listen for "
                + "requests. If not specified, defaults to the hostname for localhost.");
        hostname.setArgName("HOSTNAME");

        Option seeds = new Option("s", "seeds", true, "A comma-delimited list of hostnames or IP addresses that "
            + "serve as contact points. Nodes use this list to find each other and to learn the cluster topology. "
            + "It does not need to specify all nodes in the cluster. Defaults to this node's hostname.");
        seeds.setArgName("SEEDS");

        Option jmxPortOption = new Option("j", "jmx-port", true, "The port on which to listen for JMX connections. "
            + "Defaults to " + jmxPort + ".");
        jmxPortOption.setArgName("PORT");

        Option nativeTransportPortOption = new Option("c", "client-port", true, "The port on which to "
            + "listen for client requests. Defaults to " + nativeTransportPort);
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

        Option heapSizeOption = new Option(null, "heap-size", true, "The value to use for both the min and max heap. " +
            "This value is passed directly to the -Xms and -Xmx options of the Java executable.");

        Option heapNewSizeOption = new Option(null, "heap-new-size", true, "The value to use for the new generation " +
            "of the heap. This value is passed directly to the -Xmn option of the Java executable");

        Option stackSizeOption = new Option(null, "stack-size", true, "The value to use for the thread stack size. " +
            "This value is passed directly to the -Xss option of the Java executable.");

        options = new Options().addOption(new Option("h", "help", false, "Show this message."))
            .addOption(hostname)
            .addOption(seeds)
            .addOption(jmxPortOption)
            .addOption(startOption)
            .addOption(checkStatus)
            .addOption(commitLogOption)
            .addOption(dataDirOption)
            .addOption(savedCachesDirOption)
            .addOption(nativeTransportPortOption)
            .addOption(storagePortOption)
            .addOption(sslStoragePortOption)
            .addOption(basedirOption)
            .addOption(heapSizeOption)
            .addOption(heapNewSizeOption)
            .addOption(stackSizeOption);
    }

    public int run(CommandLine cmdLine) throws Exception {
        if (cmdLine.hasOption("h")) {
            printUsage();
            return STATUS_SHOW_USAGE;
        } else {
            DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
            DeploymentOptions deploymentOptions = factory.newDeploymentOptions();

            File basedir;
            if (cmdLine.hasOption("dir")) {
                basedir = new File(cmdLine.getOptionValue("dir"));
            } else {
                basedir = storageBasedir;
            }
            deploymentOptions.setBasedir(basedir.getAbsolutePath());

            String hostname;
            if (cmdLine.hasOption("n")) {
                hostname = cmdLine.getOptionValue("n");
            } else {
                hostname = InetAddress.getLocalHost().getHostName();
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
            File logFile = new File(logDir, "rhq-storage.log");

            deploymentOptions.setBasedir(basedir.getAbsolutePath());
            deploymentOptions.setCommitLogDir(commitLogDir);
            deploymentOptions.setDataDir(dataDir);
            deploymentOptions.setSavedCachesDir(savedCachesDir);
            deploymentOptions.setLogFileName(logFile.getPath());
            deploymentOptions.setLoggingLevel("INFO");
            deploymentOptions.setRpcPort(rpcPort);
            deploymentOptions.setJmxPort(getPort(cmdLine, "jmx-port", jmxPort));
            deploymentOptions.setNativeTransportPort(getPort(cmdLine, "client-port", nativeTransportPort));
            deploymentOptions.setStoragePort(getPort(cmdLine, "storage-port", storagePort));
            deploymentOptions.setSslStoragePort(getPort(cmdLine, "ssl-storage-port", sslStoragePort));

            if (cmdLine.hasOption("heap-size")) {
                deploymentOptions.setHeapSize(cmdLine.getOptionValue("heap-size"));
            }

            if (cmdLine.hasOption("heap-new-size")) {
                deploymentOptions.setHeapNewSize(cmdLine.getOptionValue("heap-new-size"));
            }

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
                log.error("The installer will now exit due to previous errors.");
                return STATUS_INVALID_FILE_PERMISSIONS;
            }

            Deployer deployer = new Deployer();
            deployer.setDeploymentOptions(deploymentOptions);
            storageBasedir.mkdirs();
            deployer.unzipDistro();
            deployer.applyConfigChanges();
            deployer.updateFilePerms();
            log.info("Finished installing RHQ Storage Node.");

            log.info("Updating rhq-server.properties...");
            PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();
            try {
                serverPropertiesUpdater.update("rhq.cassandra.seeds", getSeedsProperty(seeds));
            } catch (IOException e) {
                throw new RuntimeException("An error occurred while trying to update RHQ server properties", e);
            }

            boolean startNode = Boolean.parseBoolean(cmdLine.getOptionValue("start", "true"));
            if (startNode) {
                log.info("Starting RHQ Storage Node");
                String startupErrors = startNode(deploymentOptions);
                if (startupErrors == null) {
                    boolean checkStatus = Boolean.parseBoolean(cmdLine.getOptionValue("check-status", "true"));
                    if (checkStatus || isWindows()) { // no reliable pid file on windows
                        if (verifyNodeIsUp(jmxPort, 5, 3000)) {
                            log.info("RHQ Storage Node is up and running and ready to service client requests");
                            log.info("Installation of the storage node has completed successfully.");
                            return STATUS_NO_ERRORS;
                        } else {
                            log.error("Could not verify that the node is up and running.");
                            log.error("Check the log file at " + logFile + " for errors.");
                            log.error("The installer will now exit");
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
                    log.error("The storage node reported the following errors while trying to start:\n\n"
                        + startupErrors + "\n\n");
                    if (startupErrors.contains("java.net.BindException: Address already in use")) {
                        log.error("This error may indicate a conflict for the JMX port.");
                    }
                    log.error("Please review your configuration for possible sources of errors such as port "
                        + "conflicts or invalid arguments/options passed to the java executable.");
                    log.error("The installer will now exit.");
                    return STATUS_STORAGE_NOT_RUNNING;
                }
            } else {
                log.info("Installation of the storage node is complete");
                return STATUS_NO_ERRORS;
            }
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

    private PropertiesFileUpdate getServerProperties() {
        String sysprop = System.getProperty("rhq.server.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhq.server.properties] is not defined.");
        }

        File file = new File(sysprop);
        if (!(file.exists() && file.isFile())) {
            throw new RuntimeException("System property [" + sysprop + "] points to in invalid file.");
        }

        return new PropertiesFileUpdate(file.getAbsolutePath());
    }

    private String getSeedsProperty(String seeds) {
        String[] hosts = seeds.split(",");
        List<String> list = new ArrayList<String>(hosts.length);
        for (String host : hosts) {
            list.add(host + "|" + jmxPort + "|" + nativeTransportPort);
        }
        return StringUtil.collectionToString(list);
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

            cmdLine = new org.apache.commons.exec.CommandLine(new File(binDir, "cassandra"));
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

    private boolean verifyNodeIsUp(int jmxPort, int retries, long timeout) throws Exception {
        String address;
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            address = "localhost";
        }
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
        env.put(Context.INITIAL_CONTEXT_FACTORY, RMIContextFactory.class.getName());

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

    public void printUsage() {
        Options options = getOptions();
        HelpFormatter helpFormatter = new HelpFormatter();
        String syntax = "rhq-storage-installer.sh|bat [options]";
        String header = "";

        helpFormatter.printHelp(syntax, header, options, null);
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
            installer.log.error("The installer is exiting due to previous errors.");
            System.exit(1);
        } catch (ParseException e) {
            installer.printUsage();
            System.exit(STATUS_SHOW_USAGE);
        }
    }

}
