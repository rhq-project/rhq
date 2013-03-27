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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.UnmanagedDeployer;
import org.rhq.cassandra.installer.RMIContextFactory;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author John Sanda
 */
public class StorageInstaller {

    static {
        SystemInfoFactory.disableNativeSystemInfo();
    }

    private final Log log = LogFactory.getLog(StorageInstaller.class);

    private Options options;

    private File rhqBaseDir;

    private File defaultDir;

    private int jmxPort = 7299;

    private int rpcPort = 9160;

    private int nativeTransportPort = 9142;

    private int storagePort = 7100;

    private int sslStoragePort = 7101;

    private boolean startNode = true;

    private boolean checkStatus = true;

    private File logDir;

    private String commitLogDir = "/var/lib/rhq/storage/commitlog";

    private String dataDir = "/var/lib/rhq/storage/data";

    private String savedCachesDir = "/var/lib/rhq/storage/saved_caches";

    public StorageInstaller() {
        String basedir = System.getProperty("rhq.server.basedir");
        rhqBaseDir = new File(basedir);
        defaultDir = new File(basedir, "storage");
        logDir = new File(rhqBaseDir, "logs");

        Option hostname = new Option("n", "hostname", true, "The hostname or IP address on which the node will listen for " +
            "requests. If not specified, defaults to the hostname for localhost.");
         hostname.setArgName("HOSTNAME");

        Option seeds = new Option("s", "seeds", true, "A comma-delimited list of hostnames or IP addresses that " +
            "serve as contact points. Nodes use this list to find each other and to learn the cluster topology. " +
            "It does not need to specify all nodes in the cluster. Defaults to this node's hostname.");
        seeds.setArgName("SEEDS");

        Option jmxPortOption = new Option("j", "jmx-port", true, "The port on which to listen for JMX connections. " +
            "Defaults to " + jmxPort + ".");
        jmxPortOption.setArgName("PORT");

        Option nativeTransportPortOption = new Option("c", "client-port", true, "The port on which to " +
            "listen for client requests. Defaults to " + nativeTransportPort);
        nativeTransportPortOption.setArgName("PORT");

        Option storagePortOption = new Option(null, "storage-port", true, "The port on which to listen for requests " +
            " from other nodes. Defaults to " + storagePort);
        storagePortOption.setArgName("PORT");

        Option sslStoragePortOption = new Option(null, "ssl-storage-port", true, "The port on which to listen for " +
            "encrypted requests from other nodes. Only used when encryption is enabled. Defaults to " + sslStoragePort);
        sslStoragePortOption.setArgName("PORT");

        Option startOption = new Option(null, "start", true, "Start the storage node after installing it on disk. " +
            "Defaults to true.");
        startOption.setArgName("true|false");

        Option checkStatus = new Option(null, "check-status", true, "Check the node status to verify that it is up " +
            "after starting it. This option is ignored if the start option is not set. Defaults to true.");
        checkStatus.setArgName("true|false");

        Option commitLogOption = new Option(null, "commitlog", true, "The directory where the storage node keeps " +
            "commit log files. Defaults to " + commitLogDir + ".");
        commitLogOption.setArgName("DIR");

        Option dataDirOption = new Option(null, "data", true, "The directory where the storage node keeps data files. " +
            "Defaults to " + dataDir + ".");
        dataDirOption.setArgName("DIR");

        Option savedCachesDirOption = new Option(null, "saved-caches", true, "The directory where the storage node " +
            "keeps saved cache files. Defaults to " + savedCachesDir + ".");
        savedCachesDirOption.setArgName("DIR");

        options = new Options()
            .addOption(new Option("h", "help", false, "Show this message."))
            .addOption(hostname)
            //.addOption(seeds)
            .addOption(jmxPortOption)
            .addOption(startOption)
            .addOption(checkStatus)
            .addOption(commitLogOption)
            .addOption(dataDirOption)
            .addOption(savedCachesDirOption)
            .addOption(nativeTransportPortOption)
            .addOption(storagePortOption)
            .addOption(sslStoragePortOption);
    }

    public void run(CommandLine cmdLine) throws Exception {
        if (cmdLine.hasOption("h")) {
            printUsage();
        } else {
            DeploymentOptions deploymentOptions = new DeploymentOptions();

            File basedir;
            if (cmdLine.hasOption("d")) {
                basedir = new File(cmdLine.getOptionValue("d"));
            } else {
                basedir = defaultDir;
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
            String seeds = hostname;
            deploymentOptions.setSeeds(seeds);

            commitLogDir = cmdLine.getOptionValue("commitlog", commitLogDir);
            dataDir = cmdLine.getOptionValue("data", dataDir);
            savedCachesDir = cmdLine.getOptionValue("saved-caches", savedCachesDir);

            deploymentOptions.setCommitLogDir(cmdLine.getOptionValue(commitLogDir));
            deploymentOptions.setDataDir(dataDir);
            deploymentOptions.setSavedCachesDir(savedCachesDir);
            deploymentOptions.setLogDir(logDir.getAbsolutePath());
            deploymentOptions.setLoggingLevel("INFO");
            deploymentOptions.setRpcPort(rpcPort);
            deploymentOptions.setJmxPort(getPort(cmdLine, "jmx-port", jmxPort));
            deploymentOptions.setNativeTransportPort(getPort(cmdLine, "client-port", nativeTransportPort));
            deploymentOptions.setStoragePort(getPort(cmdLine, "storage-port", storagePort));
            deploymentOptions.setSslStoragePort(getPort(cmdLine, "ssl-storage-port", sslStoragePort));
            deploymentOptions.load();

            List<String> errors = new ArrayList<String>();
            checkPerms(options.getOption("saved-caches"), savedCachesDir, errors);
            checkPerms(options.getOption("commitlog"), commitLogDir, errors);
            checkPerms(options.getOption("data"), dataDir, errors);

            if (!errors.isEmpty()) {
                throw new StorageInstallerException("Problems have been detected with one or more of the directories " +
                    "to which the storage node will need to store data.", errors);
            }

            UnmanagedDeployer deployer = new UnmanagedDeployer();
            deployer.unpackBundle();
            deployer.deploy(deploymentOptions, 1);
            log.info("Finished installing RHQ Storage Node. Performing post-install clean up...");
            deployer.cleanUpBundle();

            log.info("Updating rhq-server.properties...");
            PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();
            try {
                serverPropertiesUpdater.update("rhq.cassandra.seeds", getSeedsProperty(seeds));
            }  catch (IOException e) {
                throw new RuntimeException("An error occurred while trying to update RHQ server properties", e);
            }

            if (cmdLine.hasOption("start")) {
                startNode = Boolean.parseBoolean(cmdLine.getOptionValue("s"));
            }
            if (startNode) {
                log.info("Starting RHQ Storage Node");
                startNode(deploymentOptions);

                if (cmdLine.hasOption("check-status")) {
                    checkStatus = Boolean.parseBoolean(cmdLine.getOptionValue("c"));
                }
                if (checkStatus) {
                    if (verifyNodeIsUp(jmxPort, 5, 3000)) {
                        log.info("RHQ Storage Node is up and running");
                    } else {
                        log.warn("Could not verify that the node is up and running.");
                    }
                }
            }
            log.info("Installation of the storage node is complete.");
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
                errors.add("The user running this installer does not appear to have write permissions to " + parent +
                    ". Either make sure that the user running the storage node has write permissions or use the --" +
                    option.getLongOpt() + " to change this value.");
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
            list.add(host + "|" + rpcPort + "|" + nativeTransportPort);
        }
        return StringUtil.collectionToString(list);
    }

    private void startNode(DeploymentOptions deploymentOptions) {
        File basedir = new File(deploymentOptions.getBasedir());
        File binDir = new File(basedir, "bin");

        File startScript;
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        if (systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS) {
            startScript = new File(binDir, "cassandra.bat");
        } else {
            startScript = new File(binDir, "cassandra");
        }

        ProcessExecution startScriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
        startScriptExe.setWaitForCompletion(0);
        startScriptExe.setArguments(asList("-p", "cassandra.pid"));

        ProcessExecutionResults results = systemInfo.executeProcess(startScriptExe);
        if (log.isDebugEnabled()) {
            log.debug(startScript + " returned with exit code [" + results.getExitCode() + "]");
        }
    }

    private boolean verifyNodeIsUp(int jmxPort, int retries, long timeout) throws Exception {
        String url = "service:jmx:rmi:///jndi/rmi://localhost:" + jmxPort + "/jmxrmi";
        JMXServiceURL serviceURL = new JMXServiceURL(url);
        JMXConnector connector = null;
        MBeanServerConnection serverConnection = null;


        Map<String, String> env  = new HashMap<String, String>();
        env.put("java.naming.factory.initial", RMIContextFactory.class.getName());

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
                        log.info("The storage node is not up: " + rootCause.getClass().getName() + ": " +
                            rootCause.getMessage());
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
        String syntax = "rhq-storage-installer.sh [options]";
        String header = "";

//        helpFormatter.setNewLine("\n");
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
            installer.run(cmdLine);
        } catch (StorageInstallerException e) {
            installer.log.warn(e.getMessage());
            for (String error : e.getErrors()) {
                installer.log.error(error);
            }
            installer.log.error("The installer is exiting due to previous errors.");
            System.exit(1);
        } catch (ParseException e) {
            installer.printUsage();
        }
    }

}
