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

import static java.util.Arrays.asList;
import static org.rhq.core.util.StringUtil.collectionToString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class CassandraClusterManager {

    private final Log log = LogFactory.getLog(CassandraClusterManager.class);

    private DeploymentOptions deploymentOptions;
    private List<File> installedNodeDirs = new ArrayList<File>();

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

    public List<CassandraNode> createCluster() {
        if (log.isDebugEnabled()) {
            log.debug("Installing embedded " + deploymentOptions.getNumNodes() + " node cluster to " +
                deploymentOptions.getClusterDir());
        } else {
            log.info("Installing embedded cluster");
        }

        File clusterDir = new File(deploymentOptions.getClusterDir());
        File installedMarker = new File(clusterDir, ".installed");

        if (installedMarker.exists()) {
            log.info("It appears that the cluster already exists in " + clusterDir);
            log.info("Skipping cluster creation.");
            return calculateNodes();
        }
        FileUtil.purge(clusterDir, false);

        List<CassandraNode> nodes = new ArrayList<CassandraNode>(deploymentOptions.getNumNodes());
        String seeds = collectionToString(calculateLocalIPAddresses(deploymentOptions.getNumNodes()));

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

                nodes.add(new CassandraNode(address, deploymentOptions.getJmxPort() + i,
                    nodeOptions.getNativeTransportPort()));
                installedNodeDirs.add(basedir);
            } catch (Exception e) {
                log.error("Failed to install node at " + basedir);
                throw new RuntimeException("Failed to install node at " + basedir, e);
            }
        }
        try {
            FileUtil.writeFile(new ByteArrayInputStream(new byte[]{0}), installedMarker);
        } catch (IOException e) {
            log.warn("Failed to write installed file marker to " + installedMarker, e);
        }
        return nodes;
    }

    private Set<String> calculateLocalIPAddresses(int numNodes) {
        Set<String> addresses = new HashSet<String>();
        for (int i = 1; i <= numNodes; ++i) {
            addresses.add(getLocalIPAddress(i));
        }
        return addresses;
    }

    private String getLocalIPAddress(int i) {
        return "127.0.0." + i;
    }

    private List<CassandraNode> calculateNodes() {
        List<CassandraNode> nodes = new ArrayList<CassandraNode>(deploymentOptions.getNumNodes());
        for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
            nodes.add(new CassandraNode(getLocalIPAddress(i + 1), deploymentOptions.getJmxPort() + i,
                deploymentOptions.getNativeTransportPort()));
        }
        return nodes;
    }

    public void startCluster() {
        startCluster(true);
    }

    public void startCluster(boolean waitForClusterToStart) {
        startCluster(getNodeIds());

        if (waitForClusterToStart) {
            List<CassandraNode> nodes = calculateNodes();
            ClusterInitService clusterInitService = new ClusterInitService();
            clusterInitService.waitForClusterToStart(nodes, nodes.size(), 20);
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

        if (systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS) {
            startScript = new File(binDir, "cassandra.bat");
        } else {
            startScript = new File(binDir, "cassandra");
        }

        ProcessExecution startScriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
        startScriptExe.setArguments(asList("-p", "cassandra.pid"));
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
                killNode(nodeDir);
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

    private void killNode(File nodeDir) throws Exception {
        long pid = getPid(nodeDir);
        CLibrary.kill((int) pid, 9);
    }

    private long getPid(File nodeDir) throws IOException {
        File binDir = new File(nodeDir, "bin");
        StringWriter writer = new StringWriter();
        StreamUtil.copy(new FileReader(new File(binDir, "cassandra.pid")), writer);

        return Long.parseLong(writer.getBuffer().toString());
    }

}
