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

package org.rhq.cassandra.common;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import org.rhq.cassandra.BootstrapDeployer;
import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.CassandraException;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.ShutdownCluster;

import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;

/**
 * @author John Sanda
 */
@Listeners({CassandraClusterManager.class})
public class BootstrapDeployerTest {

    @Test
    @ShutdownCluster
    public void installSchema() throws CassandraException {
        File basedir = new File("target");
        File clusterDir = new File(basedir, "cassandra");
        int numNodes = 2;

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        try {
            deploymentOptions.setClusterDir(clusterDir.getAbsolutePath());
            deploymentOptions.setRingDelay(1000L);
            deploymentOptions.load();
        } catch (IOException e) {
            throw new CassandraException(e);
        }

        BootstrapDeployer deployer = new BootstrapDeployer();
        deployer.setDeploymentOptions(deploymentOptions);
        deployer.deploy();

        // first verify that the cluster has been installed
        File installedMarker = new File(clusterDir, ".installed");
        assertTrue(installedMarker.exists(), "Cluster is not installed. The installer file marker " +
                installedMarker.getPath() + " does not exist.");

        List<CassandraHost> cassandraHosts = asList(new CassandraHost("127.0.0.1", 9160),
            new CassandraHost("127.0.0.2", 9160));

        ClusterInitService clusterInitService = new ClusterInitService();
        clusterInitService.waitForClusterToStart(cassandraHosts);
        clusterInitService.waitForSchemaAgreement("rhq", cassandraHosts);

        // now verify that the schema versions are the same on both nodes
        Cluster cluster = HFactory.getOrCreateCluster("test", "127.0.0.1");
        Map<String, List<String>> schemaVersions = cluster.describeSchemaVersions();

        // first make sure that we only have a single schema version
        assertEquals(schemaVersions.size(), 1, "There should only be one schema version.");

        // now make sure that each is on that version
        List<String> hosts = schemaVersions.values().iterator().next();
        assertEquals(hosts.size(), numNodes, "The schema has not propagated to all hosts. The latest schema version " +
            "maps to " + hosts.size() + " should map to " + numNodes + " hosts");
    }

}
