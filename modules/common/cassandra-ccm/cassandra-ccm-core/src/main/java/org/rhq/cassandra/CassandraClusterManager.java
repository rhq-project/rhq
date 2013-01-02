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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class CassandraClusterManager {

    private final Log log = LogFactory.getLog(CassandraClusterManager.class);

    private DeploymentOptions deploymentOptions;
    private List<File> installedNodeDirs;

    public CassandraClusterManager() {
        this(new DeploymentOptions());
    }

    public CassandraClusterManager(DeploymentOptions deploymentOptions) {
        this.deploymentOptions = deploymentOptions;
        try {
            this.deploymentOptions.load();
        } catch (IOException e) {
            log.error("Failed to load deployment options", e);
            throw new IllegalStateException("An initialization error occurred.", e);
        }
    }

    public List<File> installCluster() {
        if (log.isDebugEnabled()) {
            log.debug("Installing embedded " + deploymentOptions.getNumNodes() + " node cluster to " +
                deploymentOptions.getClusterDir());
        } else {
            log.info("Installing embedded cluster");
        }

        BootstrapDeployer deployer = new BootstrapDeployer();
        deployer.setDeploymentOptions(deploymentOptions);
        try {
            installedNodeDirs = deployer.deploy();
        } catch (CassandraException e) {
            String msg = "Failed to install cluster.";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        return installedNodeDirs;
    }

    public void startCluster() {
        this.startCluster(this.installedNodeDirs);
    }

    public void startCluster(List<File> nodeDirs) {
        long start = System.currentTimeMillis();
        log.info("Starting embedded cluster");
        for (File dir : nodeDirs) {
            ProcessExecutionResults results = startNode(dir);
            if (results.getError() != null) {
                log.warn("An unexpected error occurred while starting the node at " + dir, results.getError());
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

        ProcessExecutionResults results = systemInfo.executeProcess(startScriptExe);
        if (log.isDebugEnabled()) {
            log.debug(startScript + " returned with exit code [" + results.getExitCode() + "]");
        }

        return results;
    }

    public void shutdownCluster() {
        File basedir = new File(deploymentOptions.getClusterDir());
        for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
            try {
                killNode(new File(basedir, "node" + i));
            }  catch (Exception e) {
                throw new RuntimeException("Faililed to shut down cluster", e);
            }
        }
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

    public List<String> getHostNames() {
        List<String> hosts = new ArrayList<String>(deploymentOptions.getNumNodes());
        for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
            hosts.add("127.0.0." + (i + 1));
        }
        return hosts;
    }

    public InputStream loadBundle() {
        return null;
    }

    public static void main(String[] args) {
        CassandraClusterManager ccm = new CassandraClusterManager();
        List<File> nodeDirs = ccm.installCluster();
        ccm.startCluster(nodeDirs);

        PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();
        try {
            serverPropertiesUpdater.update("rhq.cassandra.cluster.seeds",
                StringUtil.collectionToString(ccm.getHostNames()));
        }  catch (IOException e) {
            throw new RuntimeException("An error occurred while trying to update RHQ server properties", e);
        }
    }

    private static PropertiesFileUpdate getServerProperties() {
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

}
