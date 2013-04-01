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

import static java.util.Arrays.asList;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.util.StringUtil;
import org.rhq.core.util.stream.StreamUtil;

import difflib.DiffUtils;
import difflib.Patch;

/**
 * @author John Sanda
 */
public class DeployerITest {

    private File deployDir;

    private File confDir;

    private Deployer deployer;

    @BeforeClass
    public void initDeployer() throws Exception {
        deployDir = new File(System.getProperty("rhq.storage.deploy-dir", System.getProperty("java.io.tmpdir")));
        confDir = new File(deployDir, "conf");

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setBasedir(deployDir.getAbsolutePath());
        deploymentOptions.setCommitLogDir("/var/lib/rhq/storage/commit_log");
        deploymentOptions.setDataDir("/var/lib/rhq/storage/data");
        deploymentOptions.setSavedCachesDir("/var/lib/rhq/storage/saved_caches");
        deploymentOptions.setLogFileName("/var/lib/rhq/storage/logs/rhq-storage.log");
        deploymentOptions.setPasswordPropertiesFile("conf/passwd.properties");
        deploymentOptions.setAccessPropertiesFile("conf/access.properties");
        deploymentOptions.load();

        deployer = new Deployer();
        deployer.setDeploymentOptions(deploymentOptions);
    }

    @Test
    public void deploy() throws Exception {
        deployer.unzipDistro();

        // Just do some minimal tests to verify that things are where we expect. This is
        // not intended to verify every single directory/path.
        File binDir = new File(deployDir, "bin");
        assertTrue(binDir.exists(), binDir + " does not exist");

        File confDir = new File(deployDir, "conf");
        assertTrue(confDir.exists(), confDir +  " does not exist");

        File libDir = new File(deployDir, "lib");
        assertTrue(libDir.exists(), libDir + " does not exist");
    }

    @Test(dependsOnMethods = "deploy")
    public void applyConfigChanges() throws Exception {
        deployer.applyConfigChanges();
    }

    @Test(dependsOnMethods = "applyConfigChanges")
    public void verifyConfigChangesToCassandraYaml() throws Exception {
        assertFileDeployedAndUpdated("cassandra.yaml");
    }

    @Test(dependsOnMethods = "applyConfigChanges")
    public void verifyConfigChangesToLog4J() throws Exception {
        assertFileDeployedAndUpdated("log4j-server.properties");
    }

    @Test(dependsOnMethods = "applyConfigChanges")
    public void verifyConfigChangesToCassandraEnv() throws Exception {
        assertFileDeployedAndUpdated("cassandra-env.sh");
    }

    private void assertFileDeployedAndUpdated(String fileName) throws Exception {
        File rhqFile = new File(confDir, "rhq." + fileName);
        File file = new File(confDir, fileName);
        assertTrue(file.exists(), file  + " does not exist");
        assertFalse(rhqFile.exists(), "Failed to delete " + rhqFile);
        assertFileUpdated(file);
    }

    private void assertFileUpdated(File actualFile) throws Exception {
        File expectedFile = new File(getClass().getResource("/expected." + actualFile.getName()).toURI());
        assertTrue(expectedFile.exists(), "Cannot verify that " + actualFile.getName() + " has been updated. There " +
            "should be a file named expected." + actualFile.getName() + " in the root of the test classpath.");

        String actualContents = StreamUtil.slurp(new FileReader(actualFile));
        List<String> actualList = asList(actualContents.split("\\n"));

        String expectedContents = StreamUtil.slurp(new FileReader(expectedFile));
        List<String> expectedList = asList(expectedContents.split("\\n"));

        Patch patch = DiffUtils.diff(actualList, expectedList);
        List<String> diffs = DiffUtils.generateUnifiedDiff(actualFile.getName(), "expected.cassandra.yaml",
            actualList, patch, 5);
        assertTrue(patch.getDeltas().isEmpty(), actualFile.getName() + " was not configured correctly. The " +
            "following differences were found:\n" + StringUtil.listToString(diffs, "\n"));
    }

}
