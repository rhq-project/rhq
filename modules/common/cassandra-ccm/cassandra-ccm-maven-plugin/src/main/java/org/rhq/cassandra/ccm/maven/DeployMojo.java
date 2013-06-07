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

package org.rhq.cassandra.ccm.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.core.domain.cloud.StorageNode;

/**
 * @author John Sanda
 */
@Mojo(name= "deploy")
public class DeployMojo extends AbstractMojo {

    @Parameter(property = "clusterDir", defaultValue = "${project.build.directory}/cassandra")
    private File clusterDir;

    @Parameter(property = "numNodes", defaultValue = "2")
    private Integer numNodes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());
        deploymentOptions.setNumNodes(numNodes);

        CassandraClusterManager ccm = new CassandraClusterManager(deploymentOptions);

        long start = System.currentTimeMillis();
        getLog().info("Creating " + numNodes + " cluster in " + clusterDir);
        List<StorageNode> nodes = ccm.createCluster();

        getLog().info("Starting cluster nodes");
        ccm.startCluster();

        getLog().info("Installing RHQ schema");
        SchemaManager schemaManager = new SchemaManager(deploymentOptions.getUsername(),
            deploymentOptions.getPassword(), nodes);

        try {
            schemaManager.install();
            schemaManager.updateTopology();
        } catch (Exception e) {
            throw new MojoExecutionException("Schema installation failed.", e);
        }

        long end = System.currentTimeMillis();
        getLog().info("Finished cluster deployment in " + (end - start) + " ms");
    }
}
