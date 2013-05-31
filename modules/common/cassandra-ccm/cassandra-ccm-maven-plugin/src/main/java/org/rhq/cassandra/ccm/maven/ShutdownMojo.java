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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;

/**
 * @author John Sanda
 */
@Mojo(name = "shutdown")
public class ShutdownMojo extends AbstractMojo {

    @Parameter(property = "cluster.dir", defaultValue = "${project.build.directory}/cassandra")
    private File clusterDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Shutting down cluster in " + clusterDir);
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());

        long start = System.currentTimeMillis();
        CassandraClusterManager ccm = new CassandraClusterManager(deploymentOptions);
        ccm.shutdownCluster();
        long end = System.currentTimeMillis();
        getLog().info("Finished cluster shutdown in " + (end - start) + " ms");
    }

}
