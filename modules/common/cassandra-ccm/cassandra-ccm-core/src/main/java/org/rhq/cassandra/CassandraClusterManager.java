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

package org.rhq.cassandra;

import static org.rhq.core.util.StringUtil.collectionToString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class CassandraClusterManager {

    private final Log log = LogFactory.getLog(CassandraClusterManager.class);

    private DeploymentOptions deploymentOptions;
    private List<File> installedNodeDirs = new ArrayList<File>();
    private Map<Integer, Process> nodeProcessMap = new HashMap<Integer, Process>();

    private String[] nodes;
    private int[] jmxPorts;
    private int cqlPort;


    public CassandraClusterManager() {
        this(new DeploymentOptionsFactory().newDeploymentOptions());
    }

    public CassandraClusterManager(DeploymentOptions deploymentOptions) {
        // Disabling native layer because according to
        // https://docs.jboss.org/author/display/MODULES/Native+Libraries more work than I
        // prefer is needed in order to properly deploy sigar's native libraries. We do not
        // need the native layer as we are only using the rhq-core-native-system apis for
        // starting cassandra nodes.
        //
        // jsanda
        SystemInfoFactory.disableNativeSystemInfo();
        this.deploymentOptions = deploymentOptions;
        try {
            this.deploymentOptions.load();
        } catch (IOException e) {
            log.error("Failed to load deployment options", e);
            throw new IllegalStateException("An initialization error occurred.", e);
        }
    }

    /**
     * @return addresses of storage cluster nodes
     */
    public String[] getNodes() {
        return nodes;
    }

    /**
     * @return the JMX ports
     */
    public int[] getJmxPorts() {
        return jmxPorts;
    }

    /**
     * @return the CQL Port
     */
    public int getCqlPort() {
        return cqlPort;
    }

    public void createCluster() {
        if (log.isDebugEnabled()) {
            log.debug("Installing embedded " + deploymentOptions.getNumNodes() + " node cluster to "
                + deploymentOptions.getClusterDir());
        } else {
            log.info("Installing embedded cluster");
        }

        File clusterDir = new File(deploymentOptions.getClusterDir());
        File installedMarker = new File(clusterDir, ".installed");

        if (installedMarker.exists()) {
            log.info("It appears that the cluster already exists in " + clusterDir);
            log.info("Skipping cluster creation.");
            getStorageClusterConfiguration();
        }
        FileUtil.purge(clusterDir, false);

        String seeds = collectionToString(calculateLocalIPAddresses(deploymentOptions.getNumNodes()));

        this.nodes = new String[deploymentOptions.getNumNodes()];
        this.jmxPorts = new int[deploymentOptions.getNumNodes()];
        this.cqlPort = deploymentOptions.getCqlPort();

        boolean useRemoteJMX = this.nodes.length > 1;

        for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
            File basedir = new File(deploymentOptions.getClusterDir(), "node" + i);
            String address = getLocalIPAddress(i + 1);

            DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
            DeploymentOptions nodeOptions = factory.newDeploymentOptions();
            nodeOptions.setSeeds(seeds);
            nodeOptions.setJmxPort(deploymentOptions.getJmxPort() + i);
            nodeOptions.setBasedir(basedir.getAbsolutePath());
            nodeOptions.setListenAddress(address);
            nodeOptions.setRpcAddress(address);
            nodeOptions.setCommitLogDir(new File(basedir, "commit_log").getAbsolutePath());
            nodeOptions.setDataDir(new File(basedir, "data").getAbsolutePath());
            nodeOptions.setSavedCachesDir(new File(basedir, "saved_caches").getAbsolutePath());

            nodeOptions.merge(deploymentOptions);
            try {
                nodeOptions.load();
                Deployer deployer = new Deployer();
                deployer.setDeploymentOptions(nodeOptions);

                deployer.unzipDistro();
                deployer.applyConfigChanges();
                deployer.updateFilePerms();
                deployer.updateStorageAuthConf(calculateLocalIPAddresses(deploymentOptions.getNumNodes()));

                if (useRemoteJMX) {
                    File confDir = new File(nodeOptions.getBasedir(), "conf");
                    File cassandraJvmPropsFile = new File(confDir, "cassandra-jvm.properties");
                    PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(
                        cassandraJvmPropsFile.getAbsolutePath());
                    Properties properties = propertiesUpdater.loadExistingProperties();
                    String jmxOpts =
                        "\"-Djava.rmi.server.hostname=" + nodeOptions.getRpcAddress() +
                        " -Dcom.sun.management.jmxremote.port=" + nodeOptions.getJmxPort() +
                        " -Dcom.sun.management.jmxremote.rmi.port=" + nodeOptions.getJmxPort() +
                        " -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false \"";

                    properties.setProperty("JMX_OPTS", jmxOpts);

                    propertiesUpdater.update(properties);
                }

                this.nodes[i] = address;
                this.jmxPorts[i] = deploymentOptions.getJmxPort() + i;

                installedNodeDirs.add(basedir);
            } catch (Exception e) {
                log.error("Failed to install node at " + basedir);
                throw new RuntimeException("Failed to install node at " + basedir, e);
            }
        }
        try {
            FileUtil.writeFile(new ByteArrayInputStream(new byte[] { 0 }), installedMarker);
        } catch (IOException e) {
            log.warn("Failed to write installed file marker to " + installedMarker, e);
        }
    }

    private void updateStorageAuthConf(File basedir) {
        File confDir = new File(basedir, "conf");
        File authFile = new File(confDir, "rhq-storage-auth.conf");
        authFile.delete();

        Set<String> addresses = calculateLocalIPAddresses(deploymentOptions.getNumNodes());

        try {
            StreamUtil.copy(new StringReader(StringUtil.collectionToString(addresses, "\n")),
                new FileWriter(authFile), true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update " + authFile);
        }
    }

    private Set<String> calculateLocalIPAddresses(int numNodes) {
        Set<String> addresses = new HashSet<String>();

        for (int i = 1; i <= numNodes; ++i) {
            addresses.add(getLocalIPAddress(i));
        }

        return addresses;
    }

    private String getLocalIPAddress(int i) {
        String seeds = deploymentOptions.getSeeds();

        if (null == seeds || seeds.isEmpty() || "localhost".equals(seeds)) {
            return "127.0.0." + i;
        }

        String[] seedsArray = seeds.split(",");
        return i <= seedsArray.length ? seedsArray[i - 1] : ("127.0.0." + i);
    }

    private void getStorageClusterConfiguration() {
        this.nodes = new String[deploymentOptions.getNumNodes()];
        this.jmxPorts = new int[deploymentOptions.getNumNodes()];
        for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
            this.nodes[i] = getLocalIPAddress(i + 1);
            this.jmxPorts[i] = deploymentOptions.getJmxPort() + i;
        }

        this.cqlPort = deploymentOptions.getCqlPort();
    }

    public void startCluster() {
        startCluster(true);
    }

    public void startCluster(boolean waitForClusterToStart) {
        startCluster(getNodeIds());

        if (waitForClusterToStart) {
            getStorageClusterConfiguration();
            ClusterInitService clusterInitService = new ClusterInitService();
            clusterInitService.waitForClusterToStart(this.nodes, this.jmxPorts, this.nodes.length, 20);
        }
    }

    public void startCluster(List<Integer> nodeIds) {
        if (log.isDebugEnabled()) {
            log.debug("Starting embedded cluster for nodes " + collectionToString(nodeIds));
        } else {
            log.info("Starting embedded cluster");
        }
        long start = System.currentTimeMillis();
        File basedir = new File(deploymentOptions.getClusterDir());
        for (Integer nodeId : nodeIds) {
            File nodeDir = new File(basedir, "node" + nodeId);
            ProcessExecutionResults results = startNode(nodeDir);
            if (results.getError() != null) {
                log.warn("An unexpected error occurred while starting the node at " + nodeDir, results.getError());
            } else {
                nodeProcessMap.put(nodeId, results.getProcess());
            }
        }
        long end = System.currentTimeMillis();
        log.info("Started embedded cluster in " + (end - start) + " ms");
    }

    private ProcessExecutionResults startNode(File basedir) {
        if (log.isDebugEnabled()) {
            log.debug("Starting node at " + basedir);
        }
        File binDir = new File(basedir, "bin");
        File startScript;
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        ProcessExecution startScriptExe;

        if (systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS) {
            startScript = new File(binDir, "cassandra.bat");
            startScriptExe = createProcessExecution(null, startScript);
        } else {
            startScript = new File(binDir, "cassandra");
            startScriptExe = createProcessExecution(null, startScript);
            startScriptExe.addArguments(Arrays.asList("-p", "cassandra.pid"));
        }

        startScriptExe.setWaitForCompletion(0);

        ProcessExecutionResults results = systemInfo.executeProcess(startScriptExe);
        if (log.isDebugEnabled()) {
            log.debug(startScript + " returned with exit code [" + results.getExitCode() + "]");
        }

        return results;
    }

    public void shutdownCluster() {
        shutdown(getNodeIds());
    }

    public void shutdown(List<Integer> nodeIds) {
        if (log.isDebugEnabled()) {
            log.debug("Preparing to shutdown cluster nodes " + collectionToString(nodeIds));
        } else {
            log.info("Preparing to shutdown cluster nodes.");
        }
        File basedir = new File(deploymentOptions.getClusterDir());

        for (Integer nodeId : nodeIds) {
            File nodeDir = new File(basedir, "node" + nodeId);
            log.debug("Shutting down node at " + nodeDir);
            try {
                if (!nodeDir.exists()) {
                    log.warn("No shutdown to perform. " + nodeDir + " does not exist.");
                    continue;
                }

                try {
                    killNode(nodeDir);
                } catch (Throwable t) {
                    log.warn("Unable to kill nodeDir [" + nodeDir + "]", t);
                }

                // This nodeProcess stuff is unlikely to be useful. I added it for Windows
                // support but we don't actually use this code anymore for Windows, we
                // use an external storage node.  I'll leave it on the very off chance that
                // it kills some hanging around process.  On Linux it will not kill
                // the cassandra process (see killNode above) because this is the launching
                // process, not the cassandra process itself.
                Process nodeProcess = nodeProcessMap.get(nodeId);
                if (null != nodeProcess) {
                    try {
                        nodeProcess.destroy();
                    } catch (Throwable t) {
                        log.warn("Failed to kill Cassandra node " + nodeDir, t);
                    }
                }
            } catch (Exception e) {
                log.warn("An error occurred trying to shutdown node at " + nodeDir);
            }
        }
    }

    private List<Integer> getNodeIds() {
        List<Integer> nodeIds = new ArrayList<Integer>();
        for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
            nodeIds.add(i);
        }
        return nodeIds;
    }

    public void killNode(File nodeDir) throws Exception {
        long pid = getPid(nodeDir);
        CLibrary.kill((int) pid, 9);
    }

    private long getPid(File nodeDir) throws IOException {
        File binDir = new File(nodeDir, "bin");
        StringWriter writer = new StringWriter();
        StreamUtil.copy(new FileReader(new File(binDir, "cassandra.pid")), writer);

        return Long.parseLong(writer.getBuffer().toString());
    }

    /**
     * Creates a ProcessExecution for the specified file for the current platform. If the current platform is Windows
     * and the file name ends with ".bat" or ".cmd", the file will be assumed to be a Windows batch file, and the
     * process execution will be initialized accordingly. Note, if the file is a UNIX script, its first line must be a
     * valid #! reference to a script interpreter (e.g. #!/bin/sh), otherwise it will fail to execute. The returned
     * ProcessExecution will have a non-null arguments list, an environment map that is a copy of the current process's
     * environment, and a working directory set to its executable's parent directory.
     *
     * @param  prefix a prefix command line that should be prepended to the executable's command line
     *                (e.g. "/usr/bin/nohup /usr/bin/sudo -u jboss -g jboss"). any files on the
     *                command line should be absolute paths. if null, no prefix command line will be
     *                prepended
     * @param  file an executable or a batch file
     *
     * @return a process execution
     */
    public static ProcessExecution createProcessExecution(String prefix, File file) {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        ProcessExecution processExecution;

        List<String> prefixArgs;
        if (prefix != null) {
            // TODO (ips, 04/27/10): Ideally, the prefix should be a String[], not a String.
            prefixArgs = Arrays.asList(prefix.split("[ \t]+"));
        } else {
            prefixArgs = Collections.emptyList();
        }
        String executable;
        List<String> args = new ArrayList<String>();
        if (systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS && isBatchFile(file)) {
            // Windows batch files cannot be executed directly - they must be passed as arguments to cmd.exe, e.g.
            // "C:\Windows\System32\cmd.exe /c C:\opt\jboss-as\bin\run.bat".
            executable = getCmdExeFile().getPath();
            args.add("/c");
            args.addAll(prefixArgs);
            args.add(file.getPath());
        } else {
            // UNIX
            if (prefixArgs.isEmpty()) {
                executable = file.getPath();
            } else {
                executable = prefixArgs.get(0);
                if (prefixArgs.size() > 1) {
                    args.addAll(prefixArgs.subList(1, prefixArgs.size()));
                }
                args.add(file.getPath());
            }
        }

        processExecution = new ProcessExecution(executable);
        processExecution.setArguments(args);

        // Start out with a copy of our own environment, since Windows needs
        // certain system environment variables to find DLLs, etc., and even
        // on UNIX, many scripts will require certain environment variables
        // (PATH, JAVA_HOME, etc.).
        // TODO (ips, 04/27/12): We probably should not just do this by default.
        Map<String, String> envVars = new LinkedHashMap<String, String>(System.getenv());
        processExecution.setEnvironmentVariables(envVars);

        // Many scripts (e.g. JBossAS scripts) assume their working directory is the directory containing the script.
        processExecution.setWorkingDirectory(file.getParent());

        return processExecution;
    }

    private static boolean isBatchFile(File file) {
        return file.getName().matches(".*\\.((bat)|(cmd))$(?i)");
    }

    private static File getCmdExeFile() {
        String cmdExe = System.getenv("COMSPEC");
        if (cmdExe == null) {
            throw new RuntimeException("COMSPEC environment variable is not defined.");
            // TODO: Try to find cmd.exe by checking the various usual locations.
        }

        File cmdExeFile = new File(cmdExe);
        if (!cmdExeFile.exists()) {
            throw new RuntimeException("COMSPEC environment variable specifies a non-existent path: " + cmdExe);
        }

        return cmdExeFile;
    }
}
