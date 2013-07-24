package org.rhq.plugins.storage;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.net.InetAddress;
import java.util.Set;

import com.google.common.collect.Sets;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.Deployer;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pc.operation.OperationManager;
import org.rhq.core.pc.operation.OperationServicesAdapter;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pluginapi.operation.OperationServicesResult;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;

/**
 * @author John Sanda
 */
public class StorageNodeComponentITest {

    private File basedir;

    private Resource storageNode;

    @BeforeSuite
    public void deployStorageNodeAndPluginContainer() throws Exception {
        basedir = new File("target", "rhq-storage");

        deployStorageNode();

        initPluginContainer();
    }

    private void deployStorageNode() throws Exception {
        DeploymentOptionsFactory factory = new DeploymentOptionsFactory();
        DeploymentOptions deploymentOptions = factory.newDeploymentOptions();
        String address = "127.0.0.1";

        deploymentOptions.setSeeds(address);
        deploymentOptions.setListenAddress(address);
        deploymentOptions.setRpcAddress(address);
        deploymentOptions.setBasedir(basedir.getAbsolutePath());
        deploymentOptions.setCommitLogDir(new File(basedir, "commit_log").getAbsolutePath());
        deploymentOptions.setDataDir(new File(basedir, "data").getAbsolutePath());
        deploymentOptions.setSavedCachesDir(new File(basedir, "saved_caches").getAbsolutePath());
        deploymentOptions.setCommitLogDir(new File(basedir, "logs").getAbsolutePath());
        deploymentOptions.setLoggingLevel("DEBUG");
        deploymentOptions.setNativeTransportPort(9142);
        deploymentOptions.setJmxPort(7399);
        deploymentOptions.setHeapSize("256M");
        deploymentOptions.setHeapNewSize("64M");

        deploymentOptions.load();

        Deployer deployer = new Deployer();
        deployer.setDeploymentOptions(deploymentOptions);

        deployer.unzipDistro();
        deployer.applyConfigChanges();
        deployer.updateFilePerms();
        deployer.updateStorageAuthConf(Sets.newHashSet(InetAddress.getByName(address)));

        File binDir = new File(basedir, "bin");
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        File startScript = new File(binDir, "cassandra");
        ProcessExecution startScriptExe = ProcessExecutionUtility.createProcessExecution(startScript);

        startScriptExe.addArguments(asList("-p", "cassandra.pid"));
        startScriptExe.setCaptureOutput(true);
        ProcessExecutionResults results = systemInfo.executeProcess(startScriptExe);

        assertEquals(results.getExitCode(), (Integer) 0, "Cassandra failed to start: " + results.getCapturedOutput());

        StorageNode storageNode = new StorageNode();
        storageNode.parseNodeInformation("127.0.0.1|7399|9142");

        ClusterInitService clusterInitService = new ClusterInitService();
        clusterInitService.waitForClusterToStart(asList(storageNode));

        SchemaManager schemaManager = new SchemaManager("rhqadmin", "rhqadmin", "127.0.0.1|7399|9142");
        schemaManager.install();
        schemaManager.updateTopology(true);
    }

    private void initPluginContainer() {
        PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
        File pluginsDir = new File(System.getProperty("pc.plugins.dir"));
        pcConfig.setPluginDirectory(pluginsDir);
        pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginsDir));

        pcConfig.setInsideAgent(false);
        PluginContainer.getInstance().setConfiguration(pcConfig);
        PluginContainer.getInstance().initialize();
    }

    @AfterSuite
    public void ShutdownPluginContainerAndStorageNode() throws Exception {
        PluginContainer.getInstance().shutdown();
        shutdownStorageNodeIfNecessary();
    }

    private void shutdownStorageNodeIfNecessary() throws Exception {
        File binDir = new File(basedir, "bin");
        File pidFile = new File(binDir, "cassandra.pid");

        if (pidFile.exists()) {
            CassandraClusterManager ccm = new CassandraClusterManager();
            ccm.killNode(basedir);
        }
    }

    @Test
    public void discoverStorageNode() {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        InventoryReport inventoryReport = inventoryManager.executeServerScanImmediately();

        if (inventoryReport.getAddedRoots().isEmpty()) {
            // could be empty if the storage node is already in inventory from
            // a prior discovery scan.
            Resource platform = inventoryManager.getPlatform();
            storageNode = findCassandraNode(platform.getChildResources());
        } else {
            storageNode = findCassandraNode(inventoryReport.getAddedRoots());
        }

        assertNotNull(storageNode, "Failed to discover Storage Node instance");
        assertNodeIsUp("Expected " + storageNode + " to be UP after discovery");
    }

    @Test(dependsOnMethods = "discoverStorageNode")
    public void shutdownStorageNode() throws Exception {
        OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
        OperationServicesAdapter operationsService = new OperationServicesAdapter(operationManager);

        long timeout = 1000 * 60;
        OperationContextImpl operationContext = new OperationContextImpl(storageNode.getId());
        OperationServicesResult result = operationsService.invokeOperation(operationContext, "shutdown",
            new Configuration(), timeout);

        assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The shutdown operation failed");
        // TODO why is this failing?
        assertNodeIsDown("Expected " + storageNode + " to be DOWN after shutting it down");
    }

    private void assertNodeIsUp(String msg) {
        executeAvailabilityScan();

        Availability availability = getAvailability();

        assertNotNull(availability, "Unable to determine availability for " + storageNode);
        assertEquals(availability.getAvailabilityType(), AvailabilityType.UP, msg);
    }

    private void assertNodeIsDown(String msg) {
        executeAvailabilityScan();

        Availability availability = getAvailability();

        assertNotNull(availability, "Unable to determine availability for " + storageNode);
        assertEquals(availability.getAvailabilityType(), AvailabilityType.DOWN, msg);
    }

    private Availability getAvailability() {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        return inventoryManager.getAvailabilityIfKnown(storageNode);
    }

    private void executeAvailabilityScan() {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        inventoryManager.executeAvailabilityScanImmediately(false, true);
    }

    private Resource findCassandraNode(Set<Resource> resources) {
        for (Resource resource : resources) {
            if (isCassandraNode(resource.getResourceType())) {
                return resource;
            }
        }
        return null;
    }

    private boolean isCassandraNode(ResourceType type) {
        return type.getPlugin().equals("RHQStorage") && type.getName().equals("RHQ Storage Node");
    }

}
