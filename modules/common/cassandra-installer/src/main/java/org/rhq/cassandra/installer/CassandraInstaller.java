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

package org.rhq.cassandra.installer;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class CassandraInstaller {

    static {
        SystemInfoFactory.disableNativeSystemInfo();
    }

    private final Log log = LogFactory.getLog(CassandraInstaller.class);

    private Options options;

    private File rhqBaseDir;

    private File defaultDir;

    private int rpcPort = 9160;

    private int nativeTransportPort = 9042;

    public CassandraInstaller() {
        String basedir = System.getProperty("rhq.server.basedir");
        rhqBaseDir = new File(basedir);
        defaultDir = new File(basedir, "storage");

        Option hostname = new Option("n", "hostname", true, "The hostname or IP address on which the node will listen for " +
            "requests. If not specified, defaults to the value returned by InetAddress.getLocalHost().getHostName().");
         hostname.setArgName("HOSTNAME");

        Option dir =  new Option("d", "dir", true, "The directory in which to install the RHQ Storage Node. Defaults " +
            "to " + defaultDir);
        dir.setArgName("INSTALL_DIR");

        Option seeds = new Option("s", "seeds", true, "A comma-delimited list of hostnames or IP addresses that " +
            "serve as contact points. Nodes use this list to find each other and to learn the cluster topology. " +
            "It does not need to specify all nodes in the cluster. Defaults to this node's hostname.");
        seeds.setArgName("SEEDS");

        Option jmxPort = new Option("j", "jmx-port", true, "The port on which to listen for JMX connections. " +
            "Defaults to 7200");
        jmxPort.setArgName("JMX_PORT");

        options = new Options()
            .addOption(new Option("h", "help", false, "Show this message."))
            .addOption(hostname)
            .addOption(dir)
            .addOption(seeds)
            .addOption(jmxPort);
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

            String seeds;
            if (cmdLine.hasOption("s")) {
                seeds = cmdLine.getOptionValue("s");
            } else {
                seeds = hostname;
            }
            deploymentOptions.setSeeds(seeds);

            Integer jmxPort;
            if (cmdLine.hasOption("j")) {
                jmxPort = Integer.parseInt(cmdLine.getOptionValue("j"));
            } else {
                jmxPort = 7200;
            }
            deploymentOptions.setJmxPort(jmxPort);

            deploymentOptions.setCommitLogDir(new File(basedir, "commit_log").getAbsolutePath());
            deploymentOptions.setSavedCachesDir(new File(basedir, "saved_caches").getAbsolutePath());
            deploymentOptions.setDataDir(new File(basedir, "data").getAbsolutePath());
            deploymentOptions.setLogDir(new File(basedir, "logs").getAbsolutePath());
            deploymentOptions.setRpcPort(rpcPort);
            deploymentOptions.setNativeTransportPort(nativeTransportPort);
            deploymentOptions.load();

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

            log.info("Starting RHQ Storage Node");
            startNode(deploymentOptions);
        }
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
        startScriptExe.setArguments(asList("-p", "cassandra.pid"));

        ProcessExecutionResults results = systemInfo.executeProcess(startScriptExe);
        if (log.isDebugEnabled()) {
            log.debug(startScript + " returned with exit code [" + results.getExitCode() + "]");
        }
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
        CassandraInstaller installer = new CassandraInstaller();
        installer.log.info("Running RHQ Storage Node installer...");
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(installer.getOptions(), args);
            installer.run(cmdLine);
            return;
        } catch (ParseException e) {
            installer.printUsage();
        }
    }

}
