package org.rhq.storage.installer;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.core.util.file.FileUtil;

/**
 * @author John Sanda
 */
public class StorageInstallerTest {

    private File basedir;

    private File storageDir;

    private StorageInstaller installer;

    @BeforeMethod
    public void initDirs(Method test) throws Exception {
        File dir = new File(getClass().getResource(".").toURI());
        basedir = new File(dir, getClass().getSimpleName() + "/" + test.getName());
        FileUtil.purge(basedir, true);
        basedir.mkdirs();

        System.setProperty("rhq.server.basedir", basedir.getAbsolutePath());

        File serverPropsFile = new File(basedir, "rhq-server.properties");
        FileUtils.touch(serverPropsFile);
        System.setProperty("rhq.server.properties-file", serverPropsFile.getAbsolutePath());

        storageDir = new File(basedir, "rhq-storage");

        installer = new StorageInstaller();
    }

    @AfterMethod
    public void shutdownStorageNode() throws Exception {
        CassandraClusterManager ccm = new CassandraClusterManager();
        ccm.killNode(storageDir);
    }

    @Test
    public void performValidInstall() throws Exception {
        CommandLineParser parser = new PosixParser();

        String[] args = {
            "--dir", storageDir.getAbsolutePath(),
            "--commitlog", new File(storageDir, "commit_log").getAbsolutePath(),
            "--data", new File(storageDir, "data").getAbsolutePath(),
            "--saved-caches", new File(storageDir, "saved_caches").getAbsolutePath(),
            "--heap-size", "256M",
            "--heap-new-size", "64M",
            "--hostname", "127.0.0.1"
        };

        CommandLine cmdLine = parser.parse(installer.getOptions(), args);
        int status = installer.run(cmdLine);

        assertEquals(status, 0, "Expected to get back a status code of 0 for a successful install");
        assertNodeIsRunning();
        assertRhqServerPropsUpdated();

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

    private void assertNodeIsRunning() {
        try {
            installer.verifyNodeIsUp("127.0.0.1", 7299, 3, 1000);
        } catch (Exception e) {
            fail("Failed to verify that node is up", e);
        }
    }

    private void assertRhqServerPropsUpdated() {
        File serverPropsFile = new File(basedir, "rhq-server.properties");
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(serverPropsFile));
        } catch (IOException e) {
            fail("Failed to verify that " + serverPropsFile + " was updated", e);
        }

        String seeds = properties.getProperty("rhq.cassandra.seeds");

        assertEquals(seeds, "127.0.0.1|7299|9142");
    }

}
