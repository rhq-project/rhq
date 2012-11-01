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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class CassandraClusterManager implements IInvokedMethodListener {

    private final Log log = LogFactory.getLog(CassandraClusterManager.class);

    @Override
    public void beforeInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {
        Method method = invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();
        if (method.isAnnotationPresent(DeployCluster.class)) {
            try {
                deployCluster(method.getAnnotation(DeployCluster.class));
            } catch (CassandraException e) {
                log.warn("Failed to deploy cluster", e);
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {
        Method method = invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();
        if (method.isAnnotationPresent(ShutdownCluster.class)) {
            try {
                shutdownCluster();
            } catch (Exception e) {
                log.warn("An error occurred while shutting down the cluster", e);
            }
        }
    }

    private void deployCluster(DeployCluster annotation) throws CassandraException {
        File basedir = new File("target");
        File clusterDir = new File(basedir, "cassandra");

        FileUtil.purge(clusterDir, false);

        int numNodes = annotation.numNodes();

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());
        deploymentOptions.setNumNodes(numNodes);
        try {
            deploymentOptions.load();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load deployment options.", e);
        }

        BootstrapDeployer deployer = new BootstrapDeployer();
        deployer.setDeploymentOptions(deploymentOptions);
        deployer.deploy();
    }

    private void shutdownCluster() throws Exception {
        File basedir = new File("target");
        File clusterDir = new File(basedir, "cassandra");
        killNode(new File(clusterDir, "node0"));
        killNode(new File(clusterDir, "node1"));
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
