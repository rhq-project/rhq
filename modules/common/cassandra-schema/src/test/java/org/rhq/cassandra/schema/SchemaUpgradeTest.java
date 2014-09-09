package org.rhq.cassandra.schema;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;

/**
 * @author John Sanda
 */
public class SchemaUpgradeTest {

    private CassandraClusterManager ccm;

    @BeforeSuite
    public void deployStorageCluster() {
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        deploymentOptions.setClusterDir("target/cassandra");
        deploymentOptions.setNumNodes(1);
        deploymentOptions.setUsername("rhqadmin");
        deploymentOptions.setPassword("rhqadmin");
        deploymentOptions.setStartRpc(true);
        deploymentOptions.setHeapSize("256M");
        deploymentOptions.setHeapNewSize("64M");
        deploymentOptions.setJmxPort(8399);

        ccm = new CassandraClusterManager(deploymentOptions);

        if (!Boolean.valueOf(System.getProperty("rhq.storage.deploy", "true"))) {
            return;
        }

        ccm.createCluster();
        ccm.startCluster(false);

        ClusterInitService clusterInitService = new ClusterInitService();
        clusterInitService.waitForClusterToStart(new String[] {"127.0.0.1"}, new int[] {8399}, 1, 2000, 20, 10);
    }

    @AfterSuite(alwaysRun = true)
    public void shutdownStorageCluster() {
        if (Boolean.valueOf(System.getProperty("rhq.storage.shutdown", "true"))) {
            ccm.shutdownCluster();
        }
    }

}
