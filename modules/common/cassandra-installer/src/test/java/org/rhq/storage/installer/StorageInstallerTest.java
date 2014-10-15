package org.rhq.storage.installer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.Deployer;
import org.rhq.cassandra.DeploymentException;
import org.rhq.cassandra.util.ConfigEditor;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class StorageInstallerTest {

    private File basedir;

    private File serverDir;

    private File storageDir;

    private StorageInstaller installer;

    @BeforeMethod
    public void initDirs(Method test) throws Exception {
        File dir = new File(getClass().getResource(".").toURI());
        basedir = new File(dir, getClass().getSimpleName() + "/" + test.getName());
        FileUtil.purge(basedir, true);
        basedir.mkdirs();

        serverDir = new File(basedir, "rhq-server");

        System.setProperty("rhq.server.basedir", serverDir.getAbsolutePath());

        File serverPropsFile = new File(serverDir, "rhq-server.properties");
        FileUtils.touch(serverPropsFile);
        System.setProperty("rhq.server.properties-file", serverPropsFile.getAbsolutePath());

        storageDir = new File(serverDir, "rhq-storage");

        installer = new SafeStorageInstaller();
    }

    @AfterMethod(alwaysRun = true)
    public void shutdownStorageNode() throws Exception {
        if (FileUtils.getFile(storageDir, "bin", "cassandra.pid").exists()) {
            CassandraClusterManager ccm = new CassandraClusterManager();
            ccm.killNode(storageDir);
            Thread.sleep(1000);
        }
    }

    @Test
    public void performDefaultInstall() throws Exception {
        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = parser.parse(installer.getOptions(), new String[] {});

        int status = installer.run(cmdLine);

        String address = InetAddress.getLocalHost().getHostName();

        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful default install");
        assertNodeIsRunning();
        assertRhqServerPropsUpdated(address);

        File binDir = new File(storageDir, "bin");
        assertTrue(binDir.exists(), "Expected to find bin directory at " + binDir);

        File confDir = new File(storageDir, "conf");
        assertTrue(confDir.exists(), "Expected to find conf directory at " + confDir);

        File libDir = new File(storageDir, "lib");
        assertTrue(libDir.exists(), "Expected to find lib directory at " + libDir);

        File baseDataDir = new File(basedir, "rhq-data");

        File commitLogDir = new File(baseDataDir, "commit_log");
        assertTrue(commitLogDir.exists(), "Expected to find commit_log directory at " + commitLogDir);

        File dataDir = new File(baseDataDir, "data");
        assertTrue(dataDir.exists(), "Expected to find data directory at " + dataDir);

        File savedCachesDir = new File(baseDataDir, "saved_caches");
        assertTrue(savedCachesDir.exists(), "Expected to find saved_caches directory at " + savedCachesDir);

        File log4jFile = new File(confDir, "log4j-server.properties");
        assertTrue(log4jFile.exists(), log4jFile + " does not exist");

        Properties log4jProps = new Properties();
        log4jProps.load(new FileInputStream(log4jFile));
        assertEquals(log4jProps.getProperty("log4j.appender.R.File"), StorageInstaller.STORAGE_LOG_FILE_PATH,
            "The log file is wrong");

        File yamlFile = new File(confDir, "cassandra.yaml");
        ConfigEditor yamlEditor = new ConfigEditor(yamlFile);
        yamlEditor.load();

        assertEquals(yamlEditor.getInternodeAuthenticator(), "org.rhq.cassandra.auth.RhqInternodeAuthenticator",
            "Failed to set the internode_authenticator property in " + yamlFile);
        assertEquals(yamlEditor.getAuthenticator(), "org.apache.cassandra.auth.PasswordAuthenticator",
            "The authenticator property is wrong");
        assertEquals(yamlEditor.getListenAddress(), address, "The listen_address property is wrong");
        assertEquals(yamlEditor.getNativeTransportPort(), (Integer) 9142, "The native_transport_port property is wrong");
        assertEquals(yamlEditor.getRpcAddress(), address, "The rpc_address property is wrong");
        assertEquals(yamlEditor.getStoragePort(), (Integer) 7100, "The storage_port property is wrong");

        File cassandraJvmPropsFile = new File(confDir, "cassandra-jvm.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(cassandraJvmPropsFile));

        assertEquals(properties.getProperty("jmx_port"), "7299", "The jmx_port property is wrong");
        assertEquals(properties.getProperty("heap_min"), "-Xms512M", "The heap_min property is wrong");
        assertEquals(properties.getProperty("heap_max"), "-Xmx512M", "The heap_max property is wrong");
        assertEquals(properties.getProperty("heap_new"), "-Xmn128M", "The heap_new property is wrong");
        assertEquals(properties.getProperty("thread_stack_size"), "-Xss256k", "The thread_stack_size property is wrong");
    }

    @Test(dependsOnMethods = "performDefaultInstall")
    public void upgradeFromDefaultInstall() throws Exception {
        CommandLineParser parser = new PosixParser();
        File defaultInstallDir = new File(basedir.getParentFile(), "performDefaultInstall");
        File upgradeFromServerDir = new File(defaultInstallDir, "rhq-server");
        // add --no-version-stamp because the test has no support for interacting with a DB
        String[] args = { "--upgrade", upgradeFromServerDir.getAbsolutePath(), "--no-version-stamp" };
        CommandLine cmdLine = parser.parse(installer.getOptions(), args);

        int status = installer.run(cmdLine);
        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful default upgrade");

        String address = InetAddress.getLocalHost().getHostName();
        assertNodeIsRunning();
        assertRhqServerPropsUpdated(address);

        File binDir = new File(storageDir, "bin");
        assertTrue(binDir.exists(), "Expected to find bin directory at " + binDir);

        File confDir = new File(storageDir, "conf");
        assertTrue(confDir.exists(), "Expected to find conf directory at " + confDir);

        File libDir = new File(storageDir, "lib");
        assertTrue(libDir.exists(), "Expected to find lib directory at " + libDir);

        File baseDataDir = new File(defaultInstallDir, "rhq-data");

        File commitLogDir = new File(baseDataDir, "commit_log");
        assertTrue(commitLogDir.exists(), "Expected to find commit_log directory at " + commitLogDir);

        File dataDir = new File(baseDataDir, "data");
        assertTrue(dataDir.exists(), "Expected to find data directory at " + dataDir);

        File savedCachesDir = new File(baseDataDir, "saved_caches");
        assertTrue(savedCachesDir.exists(), "Expected to find saved_caches directory at " + savedCachesDir);

        File log4jFile = new File(confDir, "log4j-server.properties");
        assertTrue(log4jFile.exists(), log4jFile + " does not exist");

        File logsDir = new File(serverDir, "logs");
        File logFile = new File(logsDir, "rhq-storage.log");

        Properties log4jProps = new Properties();
        log4jProps.load(new FileInputStream(log4jFile));
        assertEquals(log4jProps.getProperty("log4j.appender.R.File"), StorageInstaller.STORAGE_LOG_FILE_PATH,
            "The log file is wrong");

        File yamlFile = new File(confDir, "cassandra.yaml");
        ConfigEditor yamlEditor = new ConfigEditor(yamlFile);
        yamlEditor.load();

        assertEquals(yamlEditor.getInternodeAuthenticator(), "org.rhq.cassandra.auth.RhqInternodeAuthenticator",
            "Failed to set the internode_authenticator property in " + yamlFile);
        assertEquals(yamlEditor.getAuthenticator(), "org.apache.cassandra.auth.PasswordAuthenticator",
            "The authenticator property is wrong");
        assertEquals(yamlEditor.getListenAddress(), address, "The listen_address property is wrong");
        assertEquals(yamlEditor.getNativeTransportPort(), (Integer) 9142, "The native_transport_port property is wrong");
        assertEquals(yamlEditor.getRpcAddress(), address, "The rpc_address property is wrong");
        assertEquals(yamlEditor.getStoragePort(), (Integer) 7100, "The storage_port property is wrong");

        File cassandraJvmPropsFile = new File(confDir, "cassandra-jvm.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(cassandraJvmPropsFile));

        assertEquals(properties.getProperty("jmx_port"), "7299", "The jmx_port property is wrong");
        assertEquals(properties.getProperty("heap_min"), "-Xms512M", "The heap_min property is wrong");
        assertEquals(properties.getProperty("heap_max"), "-Xmx512M", "The heap_max property is wrong");
        assertEquals(properties.getProperty("heap_new"), "-Xmn128M", "The heap_new property is wrong");
        assertEquals(properties.getProperty("thread_stack_size"), "-Xss256k", "The thread_stack_size property is wrong");
    }

    @Test
    public void performValidInstallWithOutputToStderr() throws Exception {
        installer = new SafeStorageInstaller() {
            @Override
            protected void exec(Executor executor, org.apache.commons.exec.CommandLine cmdLine) throws IOException {
                executor.execute(cmdLine, ImmutableMap.of("JAVA_TOOL_OPTIONS", "-Dfile.encoding=UTF8"));
            }
        };

        System.setProperty("-Dfile.encoding", "UTF8");

        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = parser.parse(installer.getOptions(), new String[] {});

        int status = installer.run(cmdLine);

        assertEquals(status, 0, "A zero status code should be returned even when the storage node writes to stderr.");
    }

    @Test
    public void installWithJMXPortConflict() throws Exception {
        ServerSocket serverSocket = null;
        try {
            String address = InetAddress.getLocalHost().getHostAddress();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(address, 7799));

            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(installer.getOptions(),
                new String[] { "--rhq.storage.jmx-port", "7799" });

            int status = installer.run(cmdLine);

            assertEquals(status, StorageInstaller.STATUS_JMX_PORT_CONFLICT, "The status code is wrong");
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    @Test
    public void installWithCQLPortConflict() throws Exception {
        ServerSocket serverSocket = null;
        try {
            String address = InetAddress.getLocalHost().getHostAddress();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(address, 9342));

            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(installer.getOptions(),
                new String[] { "--rhq.storage.cql-port", "9342" });

            int status = installer.run(cmdLine);

            assertEquals(status, StorageInstaller.STATUS_CQL_PORT_CONFLICT, "The status code is wrong");
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    @Test
    public void performValidInstall() throws Exception {
        CommandLineParser parser = new PosixParser();

        String[] args = { "--dir", storageDir.getAbsolutePath(), "--rhq.storage.commitlog",
            new File(storageDir, "commit_log").getAbsolutePath(), "--rhq.storage.data",
            new File(storageDir, "data").getAbsolutePath(), "--rhq.storage.saved-caches",
            new File(storageDir, "saved_caches").getAbsolutePath(), "--rhq.storage.heap-size", "256M",
            "--rhq.storage.heap-new-size", "64M", "--rhq.storage.hostname", "127.0.0.1" };

        CommandLine cmdLine = parser.parse(installer.getOptions(), args);
        int status = installer.run(cmdLine);

        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful install");
        assertNodeIsRunning();
        assertRhqServerPropsUpdated("127.0.0.1");

        File binDir = new File(storageDir, "bin");
        assertTrue(binDir.exists(), "Expected to find bin directory at " + binDir);

        File confDir = new File(storageDir, "conf");
        assertTrue(confDir.exists(), "Expected to find conf directory at " + confDir);

        File libDir = new File(storageDir, "lib");
        assertTrue(libDir.exists(), "Expected to find lib directory at " + libDir);

        File commitLogDir = new File(storageDir, "commit_log");
        assertTrue(commitLogDir.exists(), "Expected to find commit_log directory at " + commitLogDir);

        File dataDir = new File(storageDir, "data");
        assertTrue(dataDir.exists(), "Expected to find data directory at " + dataDir);

        File savedCachesDir = new File(storageDir, "saved_caches");
        assertTrue(savedCachesDir.exists(), "Expected to find saved_caches directory at " + savedCachesDir);
    }

    @Test
    public void upgradeFromRHQ48Install() throws Exception {
        File rhq48ServerDir = new File(basedir, "rhq48-server");
        File rhq48StorageDir = new File(rhq48ServerDir, "rhq-storage");
        File rhq48StorageConfDir = new File(rhq48StorageDir, "conf");

        File oldCassandraYamlFile = new File(rhq48StorageConfDir, "cassandra.yaml");
        File oldCassandraEnvFile = new File(rhq48StorageConfDir, "cassandra-env.sh");
        File oldLog4JFile = new File(rhq48StorageConfDir, "log4j-server.properties");

        rhq48StorageConfDir.mkdirs();
        StreamUtil.copy(getClass().getResourceAsStream("/rhq48/storage/conf/cassandra.yaml"), new FileOutputStream(
            oldCassandraYamlFile), true);
        StreamUtil.copy(getClass().getResourceAsStream("/rhq48/storage/conf/cassandra-env.sh"), new FileOutputStream(
            oldCassandraEnvFile));
        StreamUtil.copy(getClass().getResourceAsStream("/rhq48/storage/conf/log4j-server.properties"),
            new FileOutputStream(oldLog4JFile));

        CommandLineParser parser = new PosixParser();

        // add --no-version-stamp because the test has no support for interacting with a DB
        String[] args = { "--upgrade", rhq48ServerDir.getAbsolutePath(), "--dir", storageDir.getAbsolutePath(),
            "--no-version-stamp" };

        CommandLine cmdLine = parser.parse(installer.getOptions(), args);
        int status = installer.run(cmdLine);

        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful upgrade");
        assertNodeIsRunning();

        File binDir = new File(storageDir, "bin");
        assertTrue(binDir.exists(), "Expected to find bin directory at " + binDir);

        File libDir = new File(storageDir, "lib");
        assertTrue(libDir.exists(), "Expected to find lib directory at " + libDir);

        File confDir = new File(storageDir, "conf");
        assertTrue(confDir.exists(), "Expected to find conf directory at " + confDir);

        File newCassandraYamlFile = new File(confDir, "cassandra.yaml");
        assertTrue(newCassandraYamlFile.exists(), newCassandraYamlFile + " does not exist");

        File newLog4JFile = new File(confDir, "log4j-server.properties");
        assertTrue(newLog4JFile.exists(), newLog4JFile + " does not exist");

        File logsDir = new File(serverDir, "logs");
        File logFile = new File(logsDir, "rhq-storage.log");

        Properties log4jProps = new Properties();
        log4jProps.load(new FileInputStream(newLog4JFile));
        assertEquals(log4jProps.getProperty("log4j.appender.R.File"), StorageInstaller.STORAGE_LOG_FILE_PATH,
            "The log file is wrong");

        assertFalse(new File(confDir, "cassandra-env.sh").exists(),
            "cassandra-env.sh should not be used after RHQ 4.8.0");

        File cassandraJvmPropsFile = new File(confDir, "cassandra-jvm.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(cassandraJvmPropsFile));

        // If this check fails, make sure that the expected value matches the value in
        // src/test/resources/rhq48/storage/conf/cassandra-env.sh
        assertEquals(properties.getProperty("jmx_port"), "7399", "Failed to update the JMX port in "
            + cassandraJvmPropsFile);

        File yamlFile = new File(confDir, "cassandra.yaml");
        ConfigEditor newYamlEditor = new ConfigEditor(yamlFile);
        newYamlEditor.load();

        ConfigEditor oldYamlEditor = new ConfigEditor(oldCassandraYamlFile);
        oldYamlEditor.load();

        assertEquals(newYamlEditor.getInternodeAuthenticator(), "org.rhq.cassandra.auth.RhqInternodeAuthenticator",
            "Failed to set the internode_authenticator property in " + yamlFile);
        assertEquals(newYamlEditor.getAuthenticator(), oldYamlEditor.getAuthenticator(), "The authenticator property "
            + "is wrong");
        assertEquals(newYamlEditor.getCommitLogDirectory(), oldYamlEditor.getCommitLogDirectory(),
            "The commit_log property is wrong");
        assertEquals(newYamlEditor.getDataFileDirectories(), oldYamlEditor.getDataFileDirectories(),
            "The data_files property is wrong");
        assertEquals(newYamlEditor.getListenAddress(), oldYamlEditor.getListenAddress(),
            "The listen_address property is wrong");
        assertEquals(newYamlEditor.getNativeTransportPort(), oldYamlEditor.getNativeTransportPort(),
            "The native_transport_port property is wrong");
        assertEquals(newYamlEditor.getRpcAddress(), oldYamlEditor.getRpcAddress(), "The rpc_address property is wrong");
        assertEquals(newYamlEditor.getSavedCachesDirectory(), oldYamlEditor.getSavedCachesDirectory(),
            "The saved_caches_directory property is wrong");
        assertEquals(newYamlEditor.getStoragePort(), oldYamlEditor.getStoragePort(),
            "The storage_port property is wrong");
    }

    private void assertNodeIsRunning() {
        try {
            installer.verifyNodeIsUp("127.0.0.1", 7299, 3, 1000);
        } catch (Exception e) {
            fail("Failed to verify that node is up", e);
        }
    }

    private void assertRhqServerPropsUpdated() {
        assertRhqServerPropsUpdated("localhost");
    }

    private void assertRhqServerPropsUpdated(String address) {
        File serverPropsFile = new File(serverDir, "rhq-server.properties");
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(serverPropsFile));
        } catch (IOException e) {
            fail("Failed to verify that " + serverPropsFile + " was updated", e);
        }

        assertEquals(properties.getProperty("rhq.storage.nodes"), address);
        assertEquals(properties.getProperty("rhq.storage.cql-port"), "9142");
    }

    private static class SafeStorageInstaller extends StorageInstaller {

        @Override
        protected Deployer getDeployer() {
            return new SafeDeployer();
        }
    }

    private static class SafeDeployer extends Deployer {

        @Override
        public void applyChangesToWindowsServiceWrapper(File deployDir) throws DeploymentException {
            // Not every test env will have this file set up.  If it doesn't exist just skip this.
            File wrapperDir = new File(deployDir, "../bin/wrapper");
            File wrapperEnvFile = new File(wrapperDir, "rhq-storage-wrapper.env");
            if (wrapperEnvFile.isFile()) {
                super.applyChangesToWindowsServiceWrapper(deployDir);
            }
        }
    }
}
