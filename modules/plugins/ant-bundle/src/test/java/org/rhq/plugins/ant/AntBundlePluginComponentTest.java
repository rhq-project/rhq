/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.ant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.pluginapi.bundle.BundlePurgeRequest;
import org.rhq.core.pluginapi.bundle.BundlePurgeResult;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.updater.DeploymentProperties;
import org.rhq.core.util.updater.DeploymentsMetadata;

@Test
public class AntBundlePluginComponentTest {
    private static final boolean ENABLE_TESTS = true;

    private static final String DEST_BASE_DIR_NAME = "Root File System"; // just mimics the real platform plugin types' name

    private AntBundlePluginComponent plugin;
    private File testFilesBaseDir; // under here will go all our test files: tmpDir, bundleFilesDir and destDir
    private File tmpDir;
    private File bundleFilesDir;
    private File destDir;

    @BeforeClass
    public void initDirs() throws Exception {
        this.testFilesBaseDir = new File("target/antbundletest");

        this.tmpDir = new File(this.testFilesBaseDir, "tmp");
        FileUtil.purge(this.tmpDir, true);
        this.bundleFilesDir = new File(this.testFilesBaseDir, "bundlefiles");
        FileUtil.purge(this.bundleFilesDir, true);
        this.destDir = new File(this.testFilesBaseDir, "destination");
        FileUtil.purge(this.destDir, true);
    }

    @BeforeMethod
    public void prepareBeforeTestMethod() throws Exception {
        if (!this.tmpDir.mkdirs()) {
            throw new IllegalStateException("Failed to create temp dir '" + this.tmpDir + "'.");
        }
        if (!this.bundleFilesDir.mkdirs()) {
            throw new IllegalStateException("Failed to create bundle files dir '" + this.bundleFilesDir + "'.");
        }
        this.plugin = new AntBundlePluginComponent();
        ResourceType type = new ResourceType("antBundleTestType", "antBundleTestPlugin", ResourceCategory.SERVER, null);
        Resource resource = new Resource("antBundleTestKey", "antBundleTestName", type);
        resource.setUuid(UUID.randomUUID().toString());
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ResourceContext<?> context = new ResourceContext(resource, null, null, null,
            SystemInfoFactory.createJavaSystemInfo(), tmpDir, null, "antBundleTestPC", null, null, null, null, null,
            null);
        this.plugin.start(context);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanPluginDirs() {
        FileUtil.purge(this.tmpDir, true);
        FileUtil.purge(this.bundleFilesDir, true);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanDestDir() {
        FileUtil.purge(this.destDir, true);
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAntBundleRevert() throws Exception {
        // install then upgrade a bundle first
        testAntBundleUpgrade();
        cleanPluginDirs(); // clean everything but the dest dir - we want to keep the metadata
        prepareBeforeTestMethod(); // prepare for our new test

        // we installed version 2.5 then upgraded to 3.0
        // now we want to revert back to 2.5
        ResourceType resourceType = new ResourceType("testSimpleBundle2Type", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle2BType", resourceType);
        Repo repo = new Repo("test-bundle-two");
        PackageType packageType = new PackageType("test-bundle-two", resourceType);
        Bundle bundle = new Bundle("test-bundle-two", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("test-bundle-two", "2.5", bundle,
            getRecipeFromFile("test-bundle-two.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testSimpleBundle2Dest", new ResourceGroup(
            "testSimpleBundle2Group"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());

        Configuration config = new Configuration();
        String customPropName = "custom.prop";
        String customPropValue = "ABC-revert";
        String onePropName = "one.prop";
        String onePropValue = "111-revert";
        config.put(new PropertySimple(customPropName, customPropValue));
        config.put(new PropertySimple(onePropName, onePropValue));

        BundleDeployment deployment = new BundleDeployment();
        deployment.setId(789);
        deployment.setName("test bundle 2 deployment name - REVERT");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // copy the test archive file to the bundle files dir
        FileUtil.copyFile(new File("src/test/resources/test-bundle-two-archive.zip"), new File(this.bundleFilesDir,
            "test-bundle-two-archive.zip"));

        // create test.properties file in the bundle files dir
        File file1 = new File(this.bundleFilesDir, "test.properties");
        Properties props = new Properties();
        props.setProperty(customPropName, "@@" + customPropName + "@@");
        FileOutputStream outputStream = new FileOutputStream(file1);
        props.store(outputStream, "test.properties comment");
        outputStream.close();

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);
        request.setRevert(true);

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // test that the prop was replaced in raw file test.properties
        Properties realizedProps = new Properties();
        loadProperties(realizedProps, new FileInputStream(new File(this.destDir, "config/test.properties")));
        assert customPropValue.equals(realizedProps.getProperty(customPropName)) : "didn't replace prop";

        // test that the archive was extracted properly. These are the files in the archive:
        // zero-file.txt (content: "zero")
        // one/one-file.txt (content: "@@one.prop@@") <-- recipe says this is to be replaced
        // two/two-file.txt (content: "@@two.prop@@") <-- recipe does not say to replace this
        // REMOVED: three/three-file.txt <-- this existed in the upgrade, but not the original
        // ----- the following was backed up and should be reverted
        // extra/extra-file.txt

        File zeroFile = new File(this.destDir, "zero-file.txt");
        File oneFile = new File(this.destDir, "one/one-file.txt");
        File twoFile = new File(this.destDir, "two/two-file.txt");
        File threeFile = new File(this.destDir, "three/three-file.txt");
        assert zeroFile.exists() : "zero file should have been restored during revert";
        assert oneFile.exists() : "one file missing";
        assert twoFile.exists() : "two file missing";
        assert !threeFile.exists() : "three file should have been deleted during revert";

        assert readFile(zeroFile).startsWith("zero") : "bad restore of zero file";
        assert readFile(oneFile).startsWith(onePropValue);
        assert readFile(twoFile).startsWith("@@two.prop@@");

        // make sure the revert restored the backed up files
        File extraFile = new File(this.destDir, "extra/extra-file.txt");
        assert extraFile.exists() : "extra file should have been restored due to revert deployment request";
        assert readFile(extraFile).startsWith("extra") : "bad restore of extra file";

        DeploymentsMetadata metadata = new DeploymentsMetadata(this.destDir);
        DeploymentProperties deploymentProps = metadata.getDeploymentProperties(deployment.getId());
        assert deploymentProps.getDeploymentId() == deployment.getId();
        assert deploymentProps.getBundleName().equals(bundle.getName());
        assert deploymentProps.getBundleVersion().equals(bundleVersion.getVersion());
        assert deploymentProps.getManageRootDir() == true;

        DeploymentProperties currentProps = metadata.getCurrentDeploymentProperties();
        assert deploymentProps.equals(currentProps);

        // check the backup directory - note, clean flag is irrelevent when determining what should be backed up
        File backupDir = metadata.getDeploymentBackupDirectory(deployment.getId());
        File ignoredBackupFile = new File(backupDir, "ignore/ignore-file.txt");
        assert ignoredBackupFile.isFile() : "old recipe didn't ignore these, should be backed up";

        DeploymentProperties previousProps = metadata.getPreviousDeploymentProperties(789);
        assert previousProps != null : "There should be previous deployment metadata";
        assert previousProps.getDeploymentId() == 456 : "bad previous deployment metadata"; // testAntBundleUpgrade used 456
        assert previousProps.getBundleName().equals(deploymentProps.getBundleName());
        assert previousProps.getBundleVersion().equals("3.0"); // testAntBundleUpgrade deployed version 3.0
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAntBundleUpgrade() throws Exception {
        upgrade(false);
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAntBundleCleanUpgrade() throws Exception {
        upgrade(true);
    }

    @Test(enabled = ENABLE_TESTS)
    public void testAntBundleInitialInstall() throws Exception {
        doAntBundleInitialInstall(true);
    }

    /**
     * Test deployment of an RHQ bundle recipe with archive file and raw file
     * @param startClean if true, the destination directory will be non-existent and thus clean
     *                   if false, this will put some junk files in the dest directory
     */
    private void doAntBundleInitialInstall(boolean startClean) throws Exception {
        ResourceType resourceType = new ResourceType("testSimpleBundle2Type", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle2BType", resourceType);
        Repo repo = new Repo("test-bundle-two");
        PackageType packageType = new PackageType("test-bundle-two", resourceType);
        Bundle bundle = new Bundle("test-bundle-two", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("test-bundle-two", "2.5", bundle,
            getRecipeFromFile("test-bundle-two.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testSimpleBundle2Dest", new ResourceGroup(
            "testSimpleBundle2Group"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());

        Configuration config = new Configuration();
        String customPropName = "custom.prop";
        String customPropValue = "ABC";
        String onePropName = "one.prop";
        String onePropValue = "111";
        config.put(new PropertySimple(customPropName, customPropValue));
        config.put(new PropertySimple(onePropName, onePropValue));

        BundleDeployment deployment = new BundleDeployment();
        deployment.setId(123);
        deployment.setName("test bundle 2 deployment name");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // copy the test archive file to the bundle files dir
        FileUtil.copyFile(new File("src/test/resources/test-bundle-two-archive.zip"), new File(this.bundleFilesDir,
            "test-bundle-two-archive.zip"));

        // create test.properties file in the bundle files dir
        File file1 = new File(this.bundleFilesDir, "test.properties");
        Properties props = new Properties();
        props.setProperty(customPropName, "@@" + customPropName + "@@");
        FileOutputStream outputStream = new FileOutputStream(file1);
        props.store(outputStream, "test.properties comment");
        outputStream.close();

        // if we are not to start clean, create some junk files that will need to be backed up and moved away
        if (startClean == false) {
            this.destDir.mkdirs();
            File junk1 = new File(this.destDir, "junk1.properties");
            Properties junkProps = new Properties();
            junkProps.setProperty("junk1", "wot gorilla?");
            FileOutputStream os = new FileOutputStream(junk1);
            junkProps.store(os, "junk1.properties comment");
            os.close();

            File junk2 = new File(this.destDir, "junksubdir" + File.separatorChar + "junk2.properties");
            junk2.getParentFile().mkdirs();
            junkProps = new Properties();
            junkProps.setProperty("junk2", "more junk");
            os = new FileOutputStream(junk2);
            junkProps.store(os, "junk2.properties comment");
            os.close();
        }

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // test that the prop was replaced in raw file test.properties
        Properties realizedProps = new Properties();
        loadProperties(realizedProps, new FileInputStream(new File(this.destDir, "config/test.properties")));
        assert customPropValue.equals(realizedProps.getProperty(customPropName)) : "didn't replace prop";

        // test that the archive was extracted properly. These are the files in the archive:
        // zero-file.txt (content: "zero")
        // one/one-file.txt (content: "@@one.prop@@") <-- recipe says this is to be replaced
        // two/two-file.txt (content: "@@two.prop@@") <-- recipe does not say to replace this
        File zeroFile = new File(this.destDir, "zero-file.txt");
        File oneFile = new File(this.destDir, "one/one-file.txt");
        File twoFile = new File(this.destDir, "two/two-file.txt");
        assert zeroFile.exists() : "zero file missing";
        assert oneFile.exists() : "one file missing";
        assert twoFile.exists() : "two file missing";
        assert readFile(zeroFile).startsWith("zero");
        assert readFile(oneFile).startsWith(onePropValue);
        assert readFile(twoFile).startsWith("@@two.prop@@");

        DeploymentsMetadata metadata = new DeploymentsMetadata(this.destDir);
        DeploymentProperties deploymentProps = metadata.getDeploymentProperties(deployment.getId());
        assert deploymentProps.getDeploymentId() == deployment.getId();
        assert deploymentProps.getBundleName().equals(bundle.getName());
        assert deploymentProps.getBundleVersion().equals(bundleVersion.getVersion());
        assert deploymentProps.getManageRootDir() == true;
        DeploymentProperties currentProps = metadata.getCurrentDeploymentProperties();
        assert deploymentProps.equals(currentProps);
        DeploymentProperties previousProps = metadata.getPreviousDeploymentProperties(deployment.getId());
        assert previousProps == null : "There should not be any previous deployment metadata";
    }

    /**
     * Test deployment of an RHQ bundle recipe.
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAntBundle() throws Exception {
        ResourceType resourceType = new ResourceType("testSimpleBundle", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle", resourceType);
        Repo repo = new Repo("testSimpleBundle");
        PackageType packageType = new PackageType("testSimpleBundle", resourceType);
        Bundle bundle = new Bundle("testSimpleBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testSimpleBundle", "1.0", bundle,
            getRecipeFromFile("test-bundle.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testSimpleBundle", new ResourceGroup(
            "testSimpleBundle"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());

        Configuration config = new Configuration();
        String realPropValue = "ABC123";
        config.put(new PropertySimple("custom.prop1", realPropValue));

        BundleDeployment deployment = new BundleDeployment();
        deployment.setName("test bundle deployment name");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // create test file
        File file1 = new File(this.bundleFilesDir, "test.properties");
        Properties props = new Properties();
        props.setProperty("custom.prop1", "@@custom.prop1@@");
        FileOutputStream outputStream = new FileOutputStream(file1);
        props.store(outputStream, "replace");
        outputStream.close();

        // create noreplace test file
        File noreplacefile = new File(this.bundleFilesDir, "noreplace.properties");
        outputStream = new FileOutputStream(noreplacefile);
        props.store(outputStream, "noreplace");
        outputStream.close();

        // create foo test file
        File foofile = new File(this.bundleFilesDir, "foo.properties");
        outputStream = new FileOutputStream(foofile);
        props.store(outputStream, "foo");
        outputStream.close();

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // test that the prop was replaced in test.properties
        Properties realizedProps = new Properties();
        loadProperties(realizedProps, new FileInputStream(new File(this.destDir, "config/test.properties")));
        assert realPropValue.equals(realizedProps.getProperty("custom.prop1")) : "didn't replace prop";

        // test that the prop was not replaced in noreplace.properties
        Properties notrealizedProps = new Properties();
        loadProperties(notrealizedProps, new FileInputStream(new File(this.destDir, "config/noreplace.properties")));
        assert "@@custom.prop1@@".equals(notrealizedProps.getProperty("custom.prop1")) : "replaced prop when it shouldn't";
    }

    /**
     * Test raw files whose destination locations have ".." in their paths.
     */
    @Test(enabled = ENABLE_TESTS)
    public void testRawFilesWithDotDotPaths() throws Exception {
        // our test bundle's relative raw file paths that resolve above dest dir will resolve to here
        final File externalDir = new File(this.testFilesBaseDir, "ext");

        ResourceType resourceType = new ResourceType("testSimpleBundle", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle", resourceType);
        Repo repo = new Repo("testSimpleBundle");
        PackageType packageType = new PackageType("testSimpleBundle", resourceType);
        Bundle bundle = new Bundle("testSimpleBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testSimpleBundle", "1.0", bundle,
            getRecipeFromFile("test-bundle-dotdot.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testSimpleBundle", new ResourceGroup(
            "testSimpleBundle"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());
        Configuration config = new Configuration();

        BundleDeployment deployment = new BundleDeployment();
        deployment.setId(0);
        deployment.setName("test bundle deployment name");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // create test files (see bundle recipe for why these files are created)
        final String TEST1 = "test1.txt";
        File file1 = new File(this.bundleFilesDir, TEST1);
        writeFile(TEST1, file1);

        final String TEST2 = "test2.txt";
        File file2 = new File(this.bundleFilesDir, TEST2);
        writeFile(TEST2, file2);

        final String TEST3 = "test3.txt";
        File file3 = new File(this.bundleFilesDir, TEST3);
        writeFile(TEST3, file3);

        final String TEST4 = "test4.txt";
        File file4 = new File(this.bundleFilesDir, TEST4);
        writeFile(TEST4, file4);

        // ----- initial deployment -----
        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);
        BundleDeployResult results = plugin.deployBundle(request);
        assertResultsSuccess(results);

        // test that the files were put where we expected them to be
        File file1Dest = new File(this.destDir, "subdir/" + TEST1);
        File file2Dest = new File(this.destDir, TEST2);
        File file3Dest = new File(externalDir, TEST3);
        File file4Dest = new File(externalDir, TEST4);
        assert TEST1.equals(readFile(file1Dest)); // inside dest dir
        assert TEST2.equals(readFile(file2Dest)); // inside dest dir
        assert TEST3.equals(readFile(file3Dest)); // outside dest dir
        assert TEST4.equals(readFile(file4Dest)); // outside dest dir

        // ----- prepare to update the bundle ----
        cleanPluginDirs(); // clean everything but the dest dir - we want to keep the metadata (this should not purge ext/ dir)
        prepareBeforeTestMethod(); // prepare for our new test
        // our src files will have different content for this bundle deployment compared to the initial deployment
        writeFile(TEST1 + "update", file1);
        writeFile(TEST2 + "update", file2);
        writeFile(TEST3 + "update", file3);
        writeFile(TEST4 + "update", file4);
        // change our initial deployment files, RHQ should see the changes and back these files up
        writeFile(TEST1 + "modified", file1Dest);
        writeFile(TEST2 + "modified", file2Dest);
        writeFile(TEST3 + "modified", file3Dest);
        writeFile(TEST4 + "modified", file4Dest);

        // ----- update deployment -----
        deployment.setId(1);
        request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);
        results = plugin.deployBundle(request);
        assertResultsSuccess(results);

        // test that all files were updated
        assert (TEST1 + "update").equals(readFile(file1Dest)); // inside dest dir
        assert (TEST2 + "update").equals(readFile(file2Dest)); // inside dest dir
        assert (TEST3 + "update").equals(readFile(file3Dest)); // outside dest dir
        assert (TEST4 + "update").equals(readFile(file4Dest)); // outside dest dir

        // test that our changed files that were under dest dir were properly backed up
        DeploymentsMetadata metadata = new DeploymentsMetadata(this.destDir);
        File backupDir = metadata.getDeploymentBackupDirectory(deployment.getId());
        File file1Backup = new File(backupDir, "subdir/" + TEST1);
        File file2Backup = new File(backupDir, TEST2);
        assert file1Backup.isFile() : "should have been backed up: " + file1Backup;
        assert file2Backup.isFile() : "should have been backed up: " + file2Backup;
        assert (TEST1 + "modified").equals(readFile(file1Backup)) : "bad backup file: " + file1Backup;
        assert (TEST2 + "modified").equals(readFile(file2Backup)) : "bad backup file: " + file2Backup;

        // test that our changed files that were above dest dir were properly backed up
        Map<String, File> winDirs = metadata.getDeploymentExternalBackupDirectoriesForWindows(deployment.getId());
        if (winDirs == null) {
            // we are running on non-windows platform
            backupDir = metadata.getDeploymentExternalBackupDirectory(deployment.getId());
        } else {
            // we are on windows, our test only uses a single drive root, so we can grab the only item in the map
            assert winDirs.size() == 1 : "should only have 1 ext backup dir on windows: " + winDirs;
            backupDir = winDirs.values().iterator().next().getAbsoluteFile();
        }

        File file3Backup;
        File file4Backup;
        boolean isWindows = (File.separatorChar == '\\');
        if (isWindows) {
            StringBuilder file3AbsPath = new StringBuilder(file3Dest.getAbsolutePath());
            StringBuilder file4AbsPath = new StringBuilder(file4Dest.getAbsolutePath());
            FileUtil.stripDriveLetter(file3AbsPath);
            FileUtil.stripDriveLetter(file4AbsPath);
            file3Backup = new File(backupDir, file3AbsPath.toString());
            file4Backup = new File(backupDir, file4AbsPath.toString());
        } else {
            file3Backup = new File(backupDir, file3Dest.getAbsolutePath());
            file4Backup = new File(backupDir, file4Dest.getAbsolutePath());
        }
        assert file3Backup.isFile() : "should have been backed up: " + file3Backup;
        assert file4Backup.isFile() : "should have been backed up: " + file4Backup;
        assert (TEST3 + "modified").equals(readFile(file3Backup)) : "bad backup file: " + file3Backup;
        assert (TEST4 + "modified").equals(readFile(file4Backup)) : "bad backup file: " + file4Backup;

        // ----- revert to last deployment, restoring backed up files
        deployment.setId(2);
        request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);
        request.setRevert(true);
        results = plugin.deployBundle(request);
        assertResultsSuccess(results);

        // make sure our files were reverted, giving us back the files that were backed up
        assert readFile(file1Backup).equals(readFile(file1Dest)); // inside dest dir
        assert readFile(file2Backup).equals(readFile(file2Dest)); // inside dest dir
        assert readFile(file3Backup).equals(readFile(file3Dest)); // outside dest dir
        assert readFile(file4Backup).equals(readFile(file4Dest)); // outside dest dir

        // ----- clean up our test -----
        FileUtil.purge(externalDir, true);
    }

    /**
     * Test realizing of replacement tokens of resource tags.
     */
    @Test(enabled = ENABLE_TESTS)
    public void testTags() throws Exception {
        ResourceType resourceType = new ResourceType("testSimpleBundle", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle", resourceType);
        Repo repo = new Repo("testSimpleBundle");
        PackageType packageType = new PackageType("testSimpleBundle", resourceType);
        Bundle bundle = new Bundle("testSimpleBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testSimpleBundle", "1.0", bundle,
            getRecipeFromFile("test-bundle.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testSimpleBundle", new ResourceGroup(
            "testSimpleBundle"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());

        BundleDeployment deployment = new BundleDeployment();
        deployment.setName("test bundle deployment name");
        deployment.setBundleVersion(bundleVersion);
        deployment.setDestination(destination);

        // create test file that will have @@ tokens for all our tags
        // note that only tags that have semantics specified will be replaced
        // our createNewBundleDeployment creates our test tags
        File file1 = new File(this.bundleFilesDir, "test.properties");
        Properties props = new Properties();
        props.setProperty("rhq.tag.ns1.sem1", "@@rhq.tag.ns1.sem1@@"); // tag is "ns1:sem1=tag1"
        props.setProperty("rhq.tag.sem2", "@@rhq.tag.sem2@@"); // tag is "sem2=tag2"
        props.setProperty("rhq.tag.ns3.null", "@@rhq.tag.ns3.null@@"); // tag is "ns3:tag3"
        props.setProperty("rhq.tag.null", "@@rhq.tag.null@@"); // tag is "tag4"
        FileOutputStream outputStream = new FileOutputStream(file1);
        props.store(outputStream, "replace");
        outputStream.close();

        // create noreplace test file
        File noreplacefile = new File(this.bundleFilesDir, "noreplace.properties");
        outputStream = new FileOutputStream(noreplacefile);
        props.store(outputStream, "noreplace");
        outputStream.close();

        // create foo test file
        File foofile = new File(this.bundleFilesDir, "foo.properties");
        outputStream = new FileOutputStream(foofile);
        props.store(outputStream, "foo");
        outputStream.close();

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // test that the prop was replaced in test.properties
        Properties realizedProps = new Properties();
        loadProperties(realizedProps, new FileInputStream(new File(this.destDir, "config/test.properties")));
        assert "tag1".equals(realizedProps.getProperty("rhq.tag.ns1.sem1")) : "didn't replace prop 1";
        assert "tag2".equals(realizedProps.getProperty("rhq.tag.sem2")) : "didn't replace prop 2";
        assert "@@rhq.tag.ns3.null@@".equals(realizedProps.getProperty("rhq.tag.ns3.null")) : "this tag should have been ignored 3";
        assert "@@rhq.tag.null@@".equals(realizedProps.getProperty("rhq.tag.null")) : "this tag should have been ignored 4";

        // test that the prop was not replaced in noreplace.properties
        Properties notrealizedProps = new Properties();
        loadProperties(notrealizedProps, new FileInputStream(new File(this.destDir, "config/noreplace.properties")));
        assert "@@rhq.tag.ns1.sem1@@".equals(notrealizedProps.getProperty("rhq.tag.ns1.sem1")) : "replaced prop 1 when it shouldn't";
        assert "@@rhq.tag.sem2@@".equals(notrealizedProps.getProperty("rhq.tag.sem2")) : "replaced prop 2 when it shouldn't";
    }

    /**
     * Test deployment of an RHQ bundle recipe where the deploy directory is not to be fully managed.
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAntBundleNoManageRootDir() throws Exception {
        ResourceType resourceType = new ResourceType("testNoManageRootDirBundle", "plugin", ResourceCategory.SERVER,
            null);
        BundleType bundleType = new BundleType("testNoManageRootDirBundle", resourceType);
        Repo repo = new Repo("testNoManageRootDirBundle");
        PackageType packageType = new PackageType("testNoManageRootDirBundle", resourceType);
        Bundle bundle = new Bundle("testNoManageRootDirBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testNoManageRootDirBundle", "1.0", bundle,
            getRecipeFromFile("test-bundle-no-manage-root-dir.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testNoManageRootDirBundle", new ResourceGroup(
            "testNoManageRootDirBundle"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());
        Configuration config = new Configuration();

        BundleDeployment deployment = new BundleDeployment();
        deployment.setName("test bundle deployment name");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // create bundle test files
        File file0 = new File(this.bundleFilesDir, "zero.properties");
        Properties props = new Properties();
        props.setProperty("zero", "0");
        FileOutputStream outputStream = new FileOutputStream(file0);
        props.store(outputStream, "zero file");
        outputStream.close();

        File file1 = new File(this.bundleFilesDir, "one.properties");
        props.clear();
        props.setProperty("one", "1");
        outputStream = new FileOutputStream(file1);
        props.store(outputStream, "one file");
        outputStream.close();

        File file2 = new File(this.bundleFilesDir, "two.properties");
        props.clear();
        props.setProperty("two", "2");
        outputStream = new FileOutputStream(file2);
        props.store(outputStream, "two file");
        outputStream.close();

        // create some external test files that don't belong to the bundle but are in the dest dir (which is not fully managed by the bundle)
        this.destDir.mkdirs();
        File external1 = new File(this.destDir, "external1.properties");
        props.clear();
        props.setProperty("external1", "1");
        outputStream = new FileOutputStream(external1);
        props.store(outputStream, "external1 file");
        outputStream.close();

        File external2 = new File(this.destDir, "extdir/external2.properties");
        external2.getParentFile().mkdirs();
        props.clear();
        props.setProperty("external2", "2");
        outputStream = new FileOutputStream(external2);
        props.store(outputStream, "external2 file");
        outputStream.close();

        // this extra file is already in the subdir1 directory when the deployment happens - it should be removed
        File extraSubdirFile = new File(this.destDir, "subdir1/extra.properties");
        extraSubdirFile.getParentFile().mkdirs();
        props.clear();
        props.setProperty("extra", "3");
        outputStream = new FileOutputStream(extraSubdirFile);
        props.store(outputStream, "extra subdir1 file");
        outputStream.close();

        // deploy the bundle
        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // test that files were deployed in the proper place
        props.clear();
        loadProperties(props, new FileInputStream(new File(this.destDir, "zero.properties")));
        assert "0".equals(props.getProperty("zero")) : "did not deploy bundle correctly 0";
        loadProperties(props, new FileInputStream(new File(this.destDir, "subdir1/one.properties")));
        assert "1".equals(props.getProperty("one")) : "did not deploy bundle correctly 1";
        loadProperties(props, new FileInputStream(new File(this.destDir, "subdir2/two.properties")));
        assert "2".equals(props.getProperty("two")) : "did not deploy bundle correctly 2";

        DeploymentsMetadata metadata = new DeploymentsMetadata(this.destDir);
        assert metadata.isManaged() == true : "missing metadata directory";
        assert metadata.getCurrentDeploymentProperties().getManageRootDir() == false : "should not be managing root dir";

        // make sure our unmanaged files/directories weren't removed
        props.clear();
        loadProperties(props, new FileInputStream(new File(this.destDir, "external1.properties")));
        assert "1".equals(props.getProperty("external1")) : "bundle deployment removed our unmanaged file 1";
        loadProperties(props, new FileInputStream(new File(this.destDir, "extdir/external2.properties")));
        assert "2".equals(props.getProperty("external2")) : "bundle deployment removed our unmanaged file 2";

        // make sure that extra file that was underneath the dest dir was removed
        assert !extraSubdirFile.exists() : "the extra file in subdir1 was not removed during initial deploy";

        // now purge the bundle - this should only purge those files that were laid down by the bundle plus the metadata directory
        BundlePurgeRequest purgeRequest = new BundlePurgeRequest();
        purgeRequest.setLiveResourceDeployment(createNewBundleDeployment(deployment));
        purgeRequest.setBundleManagerProvider(new MockBundleManagerProvider());
        purgeRequest.setAbsoluteDestinationDirectory(this.destDir);

        BundlePurgeResult purgeResults = plugin.purgeBundle(purgeRequest);
        assertResultsSuccess(purgeResults);

        // make sure our bundle files have been completely purged; the metadata directory should have been purged too
        assert new File(this.destDir, "zero.properties").exists() == false;
        assert new File(this.destDir, "subdir1/one.properties").exists() == false;
        assert new File(this.destDir, "subdir2/two.properties").exists() == false;
        assert new File(this.destDir, "subdir1").exists() == false;
        assert new File(this.destDir, "subdir2").exists() == false;
        assert this.destDir.exists() == true : "deploy dir should still exist, we were told not to fully manage it";

        metadata = new DeploymentsMetadata(this.destDir);
        assert metadata.getMetadataDirectory().exists() == false : "metadata directory should not exist";

        // make sure our external, unmanaged files still exist - the purge should not have deleted these
        props.clear();
        loadProperties(props, new FileInputStream(new File(this.destDir, "external1.properties")));
        assert "1".equals(props.getProperty("external1")) : "bundle purge removed our unmanaged file 1";
        loadProperties(props, new FileInputStream(new File(this.destDir, "extdir/external2.properties")));
        assert "2".equals(props.getProperty("external2")) : "bundle purge removed our unmanaged file 2";
    }

    /**
     * Test deployment of an RHQ bundle recipe where the deploy directory is to be fully managed.
     * This is the typical use-case and the default behavior.
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAntBundleManageRootDir() throws Exception {
        ResourceType resourceType = new ResourceType("testManageRootDirBundle", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testManageRootDirBundle", resourceType);
        Repo repo = new Repo("testManageRootDirBundle");
        PackageType packageType = new PackageType("testManageRootDirBundle", resourceType);
        Bundle bundle = new Bundle("testManageRootDirBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testManageRootDirBundle", "1.0", bundle,
            getRecipeFromFile("test-bundle-manage-root-dir.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testManageRootDirBundle", new ResourceGroup(
            "testManageRootDirBundle"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());
        Configuration config = new Configuration();

        BundleDeployment deployment = new BundleDeployment();
        deployment.setName("test bundle deployment name");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // create bundle test files
        File file0 = new File(this.bundleFilesDir, "zero.properties");
        Properties props = new Properties();
        props.setProperty("zero", "0");
        FileOutputStream outputStream = new FileOutputStream(file0);
        props.store(outputStream, "zero file");
        outputStream.close();

        File file1 = new File(this.bundleFilesDir, "one.properties");
        props.clear();
        props.setProperty("one", "1");
        outputStream = new FileOutputStream(file1);
        props.store(outputStream, "one file");
        outputStream.close();

        File file2 = new File(this.bundleFilesDir, "two.properties");
        props.clear();
        props.setProperty("two", "2");
        outputStream = new FileOutputStream(file2);
        props.store(outputStream, "two file");
        outputStream.close();

        // create some external test files that don't belong to the bundle but are in the dest dir (which is to be managed by the bundle)
        this.destDir.mkdirs();
        File external1 = new File(this.destDir, "external1.properties");
        props.clear();
        props.setProperty("external1", "1");
        outputStream = new FileOutputStream(external1);
        props.store(outputStream, "external1 file");
        outputStream.close();

        File external2 = new File(this.destDir, "extdir/external2.properties");
        external2.getParentFile().mkdirs();
        props.clear();
        props.setProperty("external2", "2");
        outputStream = new FileOutputStream(external2);
        props.store(outputStream, "external2 file");
        outputStream.close();

        // deploy the bundle
        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // test that files were deployed in the proper place
        props.clear();
        loadProperties(props, new FileInputStream(new File(this.destDir, "zero.properties")));
        assert "0".equals(props.getProperty("zero")) : "did not deploy bundle correctly 0";
        loadProperties(props, new FileInputStream(new File(this.destDir, "subdir1/one.properties")));
        assert "1".equals(props.getProperty("one")) : "did not deploy bundle correctly 1";
        loadProperties(props, new FileInputStream(new File(this.destDir, "subdir2/two.properties")));
        assert "2".equals(props.getProperty("two")) : "did not deploy bundle correctly 2";

        DeploymentsMetadata metadata = new DeploymentsMetadata(this.destDir);
        assert metadata.isManaged() == true : "missing metadata directory";
        assert metadata.getCurrentDeploymentProperties().getManageRootDir() == true : "should be managing root dir";

        // make sure our external files/directories were removed because
        // they aren't part of the bundle and we are fully managing the dest dir
        props.clear();
        try {
            loadProperties(props, new FileInputStream(new File(this.destDir, "external1.properties")));
            assert false : "bundle deployment did not remove our managed file 1";
        } catch (Exception ok) {
        }
        try {
            loadProperties(props, new FileInputStream(new File(this.destDir, "extdir/external2.properties")));
            assert false : "bundle deployment did not remove our managed file 2";
        } catch (Exception ok) {
        }

        // now purge the bundle - this should purge everything in the deploy dir because we are fully managing it
        BundlePurgeRequest purgeRequest = new BundlePurgeRequest();
        purgeRequest.setLiveResourceDeployment(createNewBundleDeployment(deployment));
        purgeRequest.setBundleManagerProvider(new MockBundleManagerProvider());
        purgeRequest.setAbsoluteDestinationDirectory(this.destDir);

        BundlePurgeResult purgeResults = plugin.purgeBundle(purgeRequest);
        assertResultsSuccess(purgeResults);

        // make sure our bundle files have been completely purged; the metadata directory should have been purged too
        assert new File(this.destDir, "zero.properties").exists() == false;
        assert new File(this.destDir, "subdir1/one.properties").exists() == false;
        assert new File(this.destDir, "subdir2/two.properties").exists() == false;
        assert new File(this.destDir, "subdir1").exists() == false;
        assert new File(this.destDir, "subdir2").exists() == false;
        assert this.destDir.exists() == false : "deploy dir should not exist, we were told to fully manage it";

        metadata = new DeploymentsMetadata(this.destDir);
        assert metadata.getMetadataDirectory().exists() == false : "metadata directory should not exist";
    }

    private void upgrade(boolean clean) throws Exception {
        doAntBundleInitialInstall(clean); // install a bundle first
        cleanPluginDirs(); // clean everything but the dest dir - we want to upgrade the destination
        prepareBeforeTestMethod(); // prepare for our new test

        // deploy upgrade and test it
        ResourceType resourceType = new ResourceType("testSimpleBundle2Type", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle2BType", resourceType);
        Repo repo = new Repo("test-bundle-two");
        PackageType packageType = new PackageType("test-bundle-two", resourceType);
        Bundle bundle = new Bundle("test-bundle-two", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("test-bundle-two", "3.0", bundle,
            getRecipeFromFile("test-bundle-three.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testSimpleBundle2Dest", new ResourceGroup(
            "testSimpleBundle2Group"), DEST_BASE_DIR_NAME, this.destDir.getAbsolutePath());

        Configuration config = new Configuration();
        String customPropName = "custom.prop";
        String customPropValue = "DEF";
        String onePropName = "one.prop";
        String onePropValue = "one-one-one";
        String threePropName = "three.prop";
        String threePropValue = "333";
        config.put(new PropertySimple(customPropName, customPropValue));
        config.put(new PropertySimple(onePropName, onePropValue));
        config.put(new PropertySimple(threePropName, threePropValue));

        BundleDeployment deployment = new BundleDeployment();
        deployment.setId(456);
        deployment.setName("test bundle 3 deployment name - upgrades test bundle 2");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // copy the test archive file to the bundle files dir
        FileUtil.copyFile(new File("src/test/resources/test-bundle-three-archive.zip"), new File(this.bundleFilesDir,
            "test-bundle-three-archive.zip"));

        // create test.properties file in the bundle files dir
        File file1 = new File(this.bundleFilesDir, "test.properties");
        Properties props = new Properties();
        props.setProperty(customPropName, "@@" + customPropName + "@@");
        FileOutputStream outputStream = new FileOutputStream(file1);
        props.store(outputStream, "test.properties comment");
        outputStream.close();

        // create some additional files - note: recipe says to ignore "ignore/**"
        File ignoreDir = new File(this.destDir, "ignore");
        File extraDir = new File(this.destDir, "extra");
        ignoreDir.mkdirs();
        extraDir.mkdirs();
        File ignoredFile = new File(ignoreDir, "ignore-file.txt");
        File extraFile = new File(extraDir, "extra-file.txt");
        FileUtil.writeFile(new ByteArrayInputStream("ignore".getBytes()), ignoredFile);
        FileUtil.writeFile(new ByteArrayInputStream("extra".getBytes()), extraFile);

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(this.bundleFilesDir);
        request.setResourceDeployment(createNewBundleDeployment(deployment));
        request.setBundleManagerProvider(new MockBundleManagerProvider());
        request.setAbsoluteDestinationDirectory(this.destDir);
        request.setCleanDeployment(clean);

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // test that the prop was replaced in raw file test.properties
        Properties realizedProps = new Properties();
        loadProperties(realizedProps, new FileInputStream(new File(this.destDir, "config/test.properties")));
        assert customPropValue.equals(realizedProps.getProperty(customPropName)) : "didn't replace prop";

        // test that the archive was extracted properly. These are the files in the archive or removed from original:
        // REMOVED: zero-file.txt
        // one/one-file.txt (content: "@@one.prop@@") <-- recipe says this is to be replaced
        // two/two-file.txt (content: "@@two.prop@@") <-- recipe does not say to replace this
        // three/three-file.txt (content: "@@three.prop@@") <-- recipe says this is to be replaced
        File zeroFile = new File(this.destDir, "zero-file.txt");
        File oneFile = new File(this.destDir, "one/one-file.txt");
        File twoFile = new File(this.destDir, "two/two-file.txt");
        File threeFile = new File(this.destDir, "three/three-file.txt");
        assert !zeroFile.exists() : "zero file should have been removed during upgrade";
        assert oneFile.exists() : "one file missing";
        assert twoFile.exists() : "two file missing";
        assert threeFile.exists() : "three file missing";
        if (clean) {
            assert !ignoredFile.exists() : "ignored file should have been deleted due to clean deployment request";
            assert !extraFile.exists() : "extra file should have been deleted due to clean deployment request";
        } else {
            assert ignoredFile.exists() : "ignored file wasn't ignored, it was deleted";
            assert !extraFile.exists() : "extra file ignored, but it should have been deleted/backed up";
        }
        assert readFile(oneFile).startsWith(onePropValue);
        assert readFile(twoFile).startsWith("@@two.prop@@");
        assert readFile(threeFile).startsWith(threePropValue);

        DeploymentsMetadata metadata = new DeploymentsMetadata(this.destDir);
        DeploymentProperties deploymentProps = metadata.getDeploymentProperties(deployment.getId());
        assert deploymentProps.getDeploymentId() == deployment.getId();
        assert deploymentProps.getBundleName().equals(bundle.getName());
        assert deploymentProps.getBundleVersion().equals(bundleVersion.getVersion());
        assert deploymentProps.getManageRootDir() == true;

        DeploymentProperties currentProps = metadata.getCurrentDeploymentProperties();
        assert deploymentProps.equals(currentProps);

        // check the backup directory - note, clean flag is irrelevent when determining what should be backed up
        File backupDir = metadata.getDeploymentBackupDirectory(deployment.getId());
        File extraBackupFile = new File(backupDir, extraDir.getName() + File.separatorChar + extraFile.getName());
        File ignoredBackupFile = new File(backupDir, ignoreDir.getName() + File.separatorChar + ignoredFile.getName());
        assert !ignoredBackupFile.exists() : "ignored file was backed up but it should not have been";
        assert extraBackupFile.exists() : "extra file was not backed up";
        assert "extra".equals(new String(StreamUtil.slurp(new FileInputStream(extraBackupFile)))) : "bad backup of extra";

        DeploymentProperties previousProps = metadata.getPreviousDeploymentProperties(456);
        assert previousProps != null : "There should be previous deployment metadata";
        assert previousProps.getDeploymentId() == 123 : "bad previous deployment metadata"; // testAntBundleInitialInstall used 123
        assert previousProps.getBundleName().equals(deploymentProps.getBundleName());
        assert previousProps.getBundleVersion().equals("2.5"); // testAntBundleInitialInstall deployed version 2.5
        assert previousProps.getManageRootDir() == true;
    }

    private void assertResultsSuccess(BundleDeployResult results) {
        assert (results.getErrorMessage() == null) : "Failed to process bundle: [" + results.getErrorMessage() + "]";
        assert results.isSuccess() : "Failed to process bundle!: [" + results.getErrorMessage() + "]";
    }

    private void assertResultsSuccess(BundlePurgeResult results) {
        assert (results.getErrorMessage() == null) : "Failed to purge bundle: [" + results.getErrorMessage() + "]";
        assert results.isSuccess() : "Failed to purge bundle!: [" + results.getErrorMessage() + "]";
    }

    private String getRecipeFromFile(String filename) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);

        byte[] contents = StreamUtil.slurp(stream);
        return new String(contents);
    }

    private String readFile(File file) throws Exception {
        return new String(StreamUtil.slurp(new FileInputStream(file)));
    }

    private void writeFile(final String content, final File file) throws IOException {
        FileUtil.writeFile(new ByteArrayInputStream(content.getBytes()), file);
    }

    private void loadProperties(Properties realizedProps, FileInputStream fileInputStream) throws Exception {
        try {
            realizedProps.load(fileInputStream);
        } finally {
            fileInputStream.close();
        }
    }

    private BundleResourceDeployment createNewBundleDeployment(BundleDeployment deployment) {
        Resource resource = new Resource(1);
        Set<Tag> tags = new HashSet<Tag>();
        tags.add(new Tag("ns1:sem1=tag1")); // we can use @@rhq.tag.ns1.sem1@@
        tags.add(new Tag("sem2=tag2")); // we can use @@rhq.tag.sem2@@
        tags.add(new Tag("ns3:tag3")); // no semantic is specified, bundle deployments will not see this
        tags.add(new Tag("tag4")); // no semantic is specified, bundle deployments will not see this
        resource.setTags(tags);
        return new BundleResourceDeployment(deployment, resource);
    }

    private class MockBundleManagerProvider implements BundleManagerProvider {
        public void auditDeployment(BundleResourceDeployment deployment, String action, String info,
            BundleResourceDeploymentHistory.Category category, BundleResourceDeploymentHistory.Status status,
            String message, String attachment) throws Exception {
            System.out.println("Auditing deployment step [" + message + "]...");
        }

        public List<PackageVersion> getAllBundleVersionPackageVersions(BundleVersion bundleVersion) throws Exception {
            return null;
        }

        public long getFileContent(PackageVersion packageVersion, OutputStream outputStream) throws Exception {
            return 0;
        }

        @Override
        public BundleHandoverResponse handoverContent(Resource bundleTarget, BundleHandoverRequest handoverRequest) {
            return null;
        }
    }
}
