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
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import org.rhq.cassandra.schema.SchemaManager;

/**
 * @author John Sanda
 */
public class CCMTestNGListener implements IInvokedMethodListener {

    private final Log log = LogFactory.getLog(CCMTestNGListener.class);

    private CassandraClusterManager ccm;

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
                Boolean skipShutdown = Boolean.valueOf(
                    System.getProperty("rhq.cassandra.cluster.skip-shutdown", "false"));
                if (!skipShutdown) {
                    shutdownCluster();
                }
            } catch (Exception e) {
                log.warn("An error occurred while shutting down the cluster", e);
            }
        }
    }

    private void deployCluster(DeployCluster annotation) throws CassandraException {
        String clusterDir = System.getProperty("rhq.cassandra.cluster.dir");
        if (clusterDir == null || clusterDir.isEmpty()) {
            File basedir = new File("target");
            clusterDir = new File(basedir, "cassandra").getAbsolutePath();
        }

        int numNodes = annotation.numNodes();
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir);
        deploymentOptions.setNumNodes(numNodes);
        deploymentOptions.setUsername(annotation.username());
        deploymentOptions.setPassword(annotation.password());

        // TODO Figure where/when to initialize ccm
        // Ideally I would like to support multiple test/configuration methods using
        // @DeployCluster to facilitate testing different scenarios for around
        // consistency and failover. If we start doing that at some point, then
        // we cannot initialize ccm here.
        ccm = new CassandraClusterManager(deploymentOptions);
        List<CassandraNode> nodes = ccm.createCluster();

        if (System.getProperty("rhq.cassandra.cluster.skip-shutdown") == null) {
            for (CassandraNode node : nodes) {
                if (node.isThrifPortOpen()) {
                    throw new RuntimeException("A cluster is already running on the same ports.");
                }
            }
        }
        ccm.startCluster(false);

        ClusterInitService clusterInitService = new ClusterInitService();
        clusterInitService.waitForClusterToStart(nodes, nodes.size(), 3000, 20);

        SchemaManager schemaManager = new SchemaManager(annotation.username(), annotation.password(), nodes);
        if (!schemaManager.schemaExists()) {
            schemaManager.createSchema();
            schemaManager.updateSchema();
        }
        schemaManager.shutdown();

        if (annotation.waitForSchemaAgreement()) {
            // TODO do not hard code cluster name
            // I am ok with hard coding the cluster name for now as it is only required
            // by the Hector API, and it is to be determined whether or not we will continue
            // using Hector. If we wind up directly using the underlying Thrift API, there
            // is no cluster name argument.
            //
            // jsanda
            clusterInitService.waitForSchemaAgreement(nodes);
        }
    }

    private void shutdownCluster() throws Exception {
        ccm.shutdownCluster();
    }

}
