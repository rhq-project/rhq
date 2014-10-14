/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.storage;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.SeedProviderDef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.ClusterInitService;
import org.rhq.cassandra.Deployer;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.cassandra.DeploymentOptionsFactory;
import org.rhq.cassandra.schema.SchemaManager;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
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
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class StorageNodeComponentITest {

    private final Log log = LogFactory.getLog(StorageNodeComponentITest.class);

    private File basedir;

    private Resource storageNode;

    private InetAddress node1Address;
    private InetAddress node2Address;

    private final static int TAKE_SNAPSHOTS = 5;
    private final static String TAKE_SNAPSHOTS_DELETE_NAME = TAKE_SNAPSHOTS + 1 + "";
    private final static String TAKE_SNAPSHOTS_MOVE_NAME = TAKE_SNAPSHOTS + 2 + "";

    @BeforeSuite
    public void deployStorageNodeAndPluginContainer() throws Exception {
        basedir = new File("target", "rhq-storage");
        node1Address = InetAddress.getByName("127.0.0.1");
        node2Address = InetAddress.getByName("127.0.0.2");
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
        deploymentOptions.setGossipPort(7200);
        deploymentOptions.setCqlPort(9142);
        deploymentOptions.setJmxPort(7399);
        deploymentOptions.setHeapSize("256M");
        deploymentOptions.setHeapNewSize("64M");

        deploymentOptions.load();
        doDeployment(deploymentOptions);

        String[] addresses = new String[] {"127.0.0.1"};
        int[] jmxPorts = new int[] {7399};

        ClusterInitService clusterInitService = new ClusterInitService();
        clusterInitService.waitForClusterToStart(addresses, jmxPorts);

        SchemaManager schemaManager = new SchemaManager("rhqadmin", "1eeb2f255e832171df8592078de921bc",
            addresses, 9142);
        try {
            schemaManager.install();
            schemaManager.updateTopology();
        } finally {
            schemaManager.shutdown();
        }
    }

    private void doDeployment(DeploymentOptions deploymentOptions) throws Exception {
        Deployer deployer = new Deployer();
        deployer.setDeploymentOptions(deploymentOptions);

        deployer.unzipDistro();
        deployer.applyConfigChanges();
        deployer.updateFilePerms();
        deployer.updateStorageAuthConf(Sets.newHashSet("127.0.0.1", "127.0.0.2"));

        File confDir = new File(deploymentOptions.getBasedir(), "conf");
        File cassandraJvmPropsFile = new File(confDir, "cassandra-jvm.properties");
        PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(cassandraJvmPropsFile.getAbsolutePath());
        Properties properties = propertiesUpdater.loadExistingProperties();

        String jvmOpts = properties.getProperty("JVM_OPTS");
        jvmOpts = jvmOpts.substring(0, jvmOpts.lastIndexOf("\""));
        jvmOpts = jvmOpts + " -Dcassandra.ring_delay_ms=2000\"";
        properties.setProperty("JVM_OPTS", jvmOpts);

        propertiesUpdater.update(properties);

        File binDir = new File(deploymentOptions.getBasedir(), "bin");
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        ProcessExecution processExecution = getProcessExecution(binDir);
        ProcessExecutionResults results = systemInfo.executeProcess(processExecution);

        assertEquals(results.getExitCode(), (Integer) 0, "Cassandra failed to start: " + results.getCapturedOutput());
    }

    private ProcessExecution getProcessExecution(File binDir) {
        ProcessExecution startScriptExe;
        if (OperatingSystem.getInstance().getName().equals(OperatingSystem.NAME_WIN32)) {
            File startScript = new File(binDir, "cassandra.bat");
            startScriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
        } else {
            File startScript = new File("./cassandra");
            startScriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
            startScriptExe.setCheckExecutableExists(false);
        }
        startScriptExe.setWorkingDirectory(binDir.getAbsolutePath());
        startScriptExe.addArguments(asList("-p", "cassandra.pid"));
        startScriptExe.setCaptureOutput(true);
        return startScriptExe;
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
        shutdownStorageNodeIfNecessary(basedir);
    }

    private void shutdownStorageNodeIfNecessary(File basedir) throws Exception {
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
            storageNode = findCassandraNode(platform.getChildResources(), "127.0.0.1");
        } else {
            storageNode = findCassandraNode(inventoryReport.getAddedRoots(), "127.0.0.1");
        }

        assertNotNull(storageNode, "Failed to discover Storage Node instance");
        assertNodeIsUp("Expected " + storageNode + " to be UP after discovery");
    }

    @Test(dependsOnMethods = "discoverStorageNode")
    public void shutdownStorageNode() throws Exception {
        OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
        OperationServicesAdapter operationsService = new OperationServicesAdapter(operationManager);

        long timeout = 1000 * 60;
        OperationContextImpl operationContext = new OperationContextImpl(storageNode.getId(), operationManager);
        OperationServicesResult result = operationsService.invokeOperation(operationContext, "shutdown",
            new Configuration(), timeout);

        assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The shutdown operation failed");

        File binDir = new File(basedir, "bin");
        File pidFile = new File(binDir, "cassandra.pid");

        assertFalse(pidFile.exists(), pidFile + " should be deleted when the storage node is shutdown.");

        assertNodeIsDown("Expected " + storageNode + " to be DOWN after shutting it down");
    }

    @Test(dependsOnMethods = "shutdownStorageNode")
    public void restartStorageNode() {
        OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
        OperationServicesAdapter operationsService = new OperationServicesAdapter(operationManager);

        long timeout = 1000 * 60;
        OperationContextImpl operationContext = new OperationContextImpl(storageNode.getId(), operationManager);
        OperationServicesResult result = operationsService.invokeOperation(operationContext, "start",
            new Configuration(), timeout);

        assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The start operation failed.");

        File binDir = new File(basedir, "bin");
        File pidFile = new File(binDir, "cassandra.pid");

        assertTrue(pidFile.exists(), pidFile + " should be created when starting the storage node.");

        assertNodeIsUp("Expected " + storageNode + " to be up after restarting it.");
    }

    @Test(dependsOnMethods = "restartStorageNode", priority = 1)
    public void takeSnaphots() throws Exception {
        Configuration params = new Configuration();
        for (int i = 0; i < TAKE_SNAPSHOTS; i++) {
            params = Configuration.builder().addSimple("snapshotName", "" + i).build();

            OperationServicesResult result = takeSnapshot(params);
            assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The takeSnapshot operation try "
                + i + " failed.");
        }
        printSnapshotDirsInfo();
        assertSnaphotCount(getSnaphostDirs(), TAKE_SNAPSHOTS);
    }

    @Test(dependsOnMethods = "takeSnaphots")
    public void takeSnapshotsKeepLastNAndDelete() {
        printSnapshotDirsInfo();

        final int keepN = 3;

        Configuration params = Configuration.builder().addSimple("retentionStrategy", "Keep Last N")
            .addSimple("count", keepN)
            .addSimple("snapshotName", TAKE_SNAPSHOTS_DELETE_NAME)
            .build();
        OperationServicesResult result = takeSnapshot(params);
        printSnapshotDirsInfo();
        assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The takeSnapshot operation failed.");

        assertSnaphotCount(getSnaphostDirs(), keepN);
        assertSnaphotsContain(getSnaphostDirs(), TAKE_SNAPSHOTS_DELETE_NAME);
    }

    @Test(dependsOnMethods = "takeSnapshotsKeepLastNAndDelete")
    public void takeSnapshotsKeepLastNAndMove() {
        printSnapshotDirsInfo();

        final int keepN = 1;

        File moveLocation = new File(basedir, TAKE_SNAPSHOTS_MOVE_NAME);

        Configuration params = Configuration.builder().addSimple("retentionStrategy", "Keep Last N")
            .addSimple("count", keepN)
            .addSimple("snapshotName", TAKE_SNAPSHOTS_MOVE_NAME)
            .addSimple("deletionStrategy", "Move")
            .addSimple("location", moveLocation.getAbsolutePath())
            .build();
        OperationServicesResult result = takeSnapshot(params);
        assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The takeSnapshot operation failed.");

        printSnapshotDirsInfo();

        assertSnaphotCount(getSnaphostDirs(), keepN);
        assertSnaphotsContain(getSnaphostDirs(), TAKE_SNAPSHOTS_MOVE_NAME);

        // assert moved snaphots
        // takeSnapshotsKeepLastNAndDelete test left 3 snaphosts, new one was generated so 3 had to be moved
        assertSnaphotCount(getMovedSnapshotDirs(moveLocation), 3);

        // snaphost that has not been moved must not be in moved location
        for (File snapDir : getMovedSnapshotDirs(moveLocation)) {
            int size = snapDir.listFiles(createDirFilter(TAKE_SNAPSHOTS_MOVE_NAME)).length;
            assertEquals(size, 0);
        }

        // but we must be able to find snapshot created by takeSnapshotsKeepLastNAndDelete test
        assertSnaphotsContain(getMovedSnapshotDirs(moveLocation), TAKE_SNAPSHOTS_DELETE_NAME);
    }

    @Test(dependsOnMethods = "takeSnapshotsKeepLastNAndMove", priority = 3)
    public void takeSnapshotsDeleteOlderThanN() {
        printSnapshotDirsInfo();

        // mark snaphosts left by takeSnapshotsKeepLastNAndMove test as 2 days old
        for (File parent : getSnaphostDirs()) {
            File snapshot = new File(parent, TAKE_SNAPSHOTS_MOVE_NAME);
            snapshot.setLastModified(System.currentTimeMillis() - (2 * 86400L * 1000L));
        }

        int delOlderThan = 3;

        Configuration params = Configuration.builder()
            .addSimple("retentionStrategy", "Delete Older Than N days")
            .addSimple("count", delOlderThan)
            .build();
        OperationServicesResult result = takeSnapshot(params);
        printSnapshotDirsInfo();
        assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The takeSnapshot operation failed.");
        // takeSnapshotsKeepLastNAndMove left 1 snapshot so now there has to be 2 of them
        assertSnaphotCount(getSnaphostDirs(), 2);

        File moveLocation = new File(basedir, "snaphosts-moved-2");
        delOlderThan = 1;
        params = Configuration.builder()
            .addSimple("retentionStrategy", "Delete Older Than N days")
            .addSimple("count", delOlderThan)
            .addSimple("deletionStrategy", "Move")
            .addSimple("location", moveLocation.getAbsolutePath())
            .build();
        result = takeSnapshot(params);
        printSnapshotDirsInfo();
        assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The takeSnapshot operation failed.");
        // 2 snapshots left 1 created, but 1 moved
        assertSnaphotCount(getSnaphostDirs(), 2);
        assertSnaphotsContain(getMovedSnapshotDirs(moveLocation), TAKE_SNAPSHOTS_MOVE_NAME);
    }

    private OperationServicesResult takeSnapshot(Configuration params) {
        // sleep a bit before taking snapshots. This is because this operation reads lastModified field of created snapshot dirs
        // and that field has precision in seconds
        try {
            Thread.currentThread().join(1500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
        OperationServicesAdapter operationsService = new OperationServicesAdapter(operationManager);

        long timeout = 1000 * 60;
        OperationContextImpl operationContext = new OperationContextImpl(storageNode.getId(), operationManager);
        return operationsService.invokeOperation(operationContext, "takeSnapshot", params, timeout);
    }

    private List<File> getMovedSnapshotDirs(File parent) {
        return Arrays.asList(
            new File(parent, "system/schema_keyspaces/snapshots"),
            new File(parent,"rhq/schema_version/snapshots")
            );
    }

    private List<File> getSnaphostDirs() {
        return Arrays.asList(
            new File(basedir, "data/system/schema_keyspaces/snapshots"),
            new File(basedir, "data/rhq/schema_version/snapshots")
            );
    }

    private void sleep() {

    }

    private void printSnapshotDirsInfo() {
        for (File snapDir : getSnaphostDirs()) {
            System.out.println("Snapshot dirs debug info [" + snapDir.getAbsolutePath() + "]");
            if (snapDir.listFiles() == null) {
                System.out.println("Snapshot directory does not exist");
                continue;
            }
            for (File snap : snapDir.listFiles()) {
                System.out.println("Snapshot [" + snap.getName() + "] lastModified=" + snap.lastModified());
            }
        }
    }

    private void assertSnaphotCount(List<File> snapshotDirs, int count) {
        System.out.println("Asserting there is " + count + " snaphosts");
        for (File snapDir : snapshotDirs) {
            System.out.println("- in [" + snapDir.getAbsolutePath() + "]");
            int size = snapDir.listFiles(createDirFilter(null)).length;
            assertEquals(size, count, "Dirs found : " + Arrays.toString(snapDir.list()));
        }
    }

    private void assertSnaphotsContain(List<File> snapshotDirs, final String snapshotName) {
        System.out.println("Asserting snapshot with name [" + snapshotName + "] exists");
        for (File snapDir : snapshotDirs) {
            System.out.println(" - exists in [" + snapDir.getAbsolutePath() + "]?");
            int size = snapDir.listFiles(createDirFilter(snapshotName)).length;
            assertEquals(size, 1, "Dirs found : " + Arrays.toString(snapDir.list()));
        }
    }

    private FileFilter createDirFilter(final String dirName) {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && dirName == null ? true : pathname.getName().equals(dirName);
            }
        };
    }

    @Test(dependsOnMethods = "restartStorageNode", priority = 100)
    public void prepareForBootstrap() throws Exception {
        File node2Basedir = new File(basedir.getParentFile(), "rhq-storage-2");

        try {
            DeploymentOptions deploymentOptions = new DeploymentOptionsFactory().newDeploymentOptions();
            deploymentOptions.setSeeds("127.0.0.1");
            deploymentOptions.setListenAddress("127.0.0.2");
            deploymentOptions.setRpcAddress("127.0.0.2");
            deploymentOptions.setBasedir(node2Basedir.getAbsolutePath());
            deploymentOptions.setCommitLogDir(new File(node2Basedir, "commit_log").getAbsolutePath());
            deploymentOptions.setDataDir(new File(node2Basedir, "data").getAbsolutePath());
            deploymentOptions.setSavedCachesDir(new File(node2Basedir, "saved_caches").getAbsolutePath());
            deploymentOptions.setCommitLogDir(new File(node2Basedir, "logs").getAbsolutePath());
            deploymentOptions.setLoggingLevel("DEBUG");
            deploymentOptions.setGossipPort(7200);
            deploymentOptions.setCqlPort(9142);
            deploymentOptions.setJmxPort(7400);
            deploymentOptions.setHeapSize("256M");
            deploymentOptions.setHeapNewSize("64M");
            deploymentOptions.load();

            doDeployment(deploymentOptions);
            ClusterInitService clusterInitService = new ClusterInitService();
            clusterInitService.waitForClusterToStart(new String [] {"127.0.0.2"}, new int[] {7400});

            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            InventoryReport inventoryReport = inventoryManager.executeServerScanImmediately();
            Resource newStorageNode = null;

            if (inventoryReport.getAddedRoots().isEmpty()) {
                // could be empty if the storage node is already in inventory from
                // a prior discovery scan.
                Resource platform = inventoryManager.getPlatform();
                newStorageNode = findCassandraNode(platform.getChildResources(), "127.0.0.2");
            } else {
                newStorageNode = findCassandraNode(inventoryReport.getAddedRoots(), "127.0.0.2");
            }

            assertNotNull(newStorageNode, "Failed to discover Storage Node instance at 127.0.0.2");
            assertNodeIsUp("Expected " + newStorageNode + " to be UP after discovery");


            Configuration params = Configuration.builder().addSimple("cqlPort", 9142).addSimple("gossipPort", 7200)
                .openList("addresses", "address").addSimples("127.0.0.1", "127.0.0.2")
                .closeList().build();

            OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
            OperationServicesAdapter operationsService = new OperationServicesAdapter(operationManager);

            long timeout = 1000 * 60;
            OperationContextImpl operationContext = new OperationContextImpl(newStorageNode.getId(), operationManager);
            OperationServicesResult result = operationsService.invokeOperation(operationContext, "prepareForBootstrap",
                params, timeout);

            log.info("Waiting for node to boostrap...");
            // When a node goes through bootstrap, StorageService sleeps for RING_DELAY ms
            // while it determines the ranges of the token ring it will own. RING_DELAY defaults
            // to 30 seconds by default but we are overriding it to be 100 ms.
            Thread.sleep(3000);

            assertEquals(result.getResultCode(), OperationServicesResultCode.SUCCESS, "The operation failed: " +
                result.getErrorStackTrace());

            assertNodeIsUp("Expected " + newStorageNode + " to be up after the prepareForBootstrap operation completes.");

            assertThatInternodeAuthConfFileMatches("127.0.0.1", "127.0.0.2");

            File confDir = new File(basedir, "conf");
            File cassandraYamlFile = new File(confDir, "cassandra.yaml");
            Config config = loadConfig(cassandraYamlFile);

            assertEquals(config.seed_provider.parameters.get("seeds"), "127.0.0.1", "Failed to update seeds " +
                "property in " + cassandraYamlFile);
        } finally {
            shutdownStorageNodeIfNecessary(node2Basedir);
        }
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
        return new Availability(storageNode, inventoryManager.getCurrentAvailability(storageNode, false).forResource(storageNode.getId()));
    }

    private void executeAvailabilityScan() {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        inventoryManager.executeAvailabilityScanImmediately(false, true);
    }

    private Resource findCassandraNode(Set<Resource> resources, String address) {
        for (Resource resource : resources) {
            if (isCassandraNode(resource.getResourceType()) &&
                resource.getResourceKey().equals("RHQ Storage Node(" + address + ")")) {
                return resource;
            }
        }
        return null;
    }

    private boolean isCassandraNode(ResourceType type) {
        return type.getPlugin().equals("RHQStorage") && type.getName().equals("RHQ Storage Node");
    }

    private void assertThatInternodeAuthConfFileMatches(String... addresses) throws Exception {
        File confDir = new File(basedir, "conf");
        File internodeAuthConfFile = new File(confDir, "rhq-storage-auth.conf");
        String contents = StreamUtil.slurp(new FileReader(internodeAuthConfFile));

        Set<String> expected = ImmutableSet.copyOf(addresses);
        Set<String> actual = ImmutableSet.copyOf(contents.split("\n"));

        assertEquals(actual, expected, "Failed to update internode authentication conf file " +
            internodeAuthConfFile + ".");
    }

    private Config loadConfig(File configFile) throws Exception {
        FileInputStream inputStream = new FileInputStream(configFile);
        org.yaml.snakeyaml.constructor.Constructor constructor =
            new org.yaml.snakeyaml.constructor.Constructor(Config.class);
        TypeDescription seedDesc = new TypeDescription(SeedProviderDef.class);
        seedDesc.putMapPropertyType("parameters", String.class, String.class);
        constructor.addTypeDescription(seedDesc);
        Yaml yaml = new Yaml(new Loader(constructor));

        return (Config) yaml.load(inputStream);
    }

}
