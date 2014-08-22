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

package org.rhq.bundle.ant;

import static org.rhq.core.util.stream.StreamUtil.slurp;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.AntXMLContext;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.bundle.ant.task.BundleTask;
import org.rhq.bundle.ant.type.DeploymentUnitType;
import org.rhq.bundle.ant.type.HandoverInfo;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.updater.DeploymentsMetadata;
import org.rhq.core.util.updater.DestinationComplianceMode;
import org.rhq.core.util.updater.FileHashcodeMap;
import org.rhq.test.PortScout;

/**
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Test
public class AntLauncherTest {
    private static final Log LOG = LogFactory.getLog(AntLauncherTest.class);

    private static final File DEPLOY_DIR = new File("target/test-ant-bundle").getAbsoluteFile();
    private static final File WORKING_DIR = new File("target/test-ant-working-dir").getAbsoluteFile();
    private static final String ANT_BASEDIR = "target/test-classes";
    private static final File REDHAT_RELEASE_FILE = new File("/etc/redhat-release");

    private int deploymentId;

    @BeforeClass
    public void beforeClass() {
        deploymentId = 0;
    }

    @BeforeMethod
    public void beforeTest() {
        FileUtil.purge(DEPLOY_DIR, true);
    }

    @AfterClass
    public void afterClass() {
        FileUtil.purge(new File(DEPLOY_DIR.getParentFile(), "test-ant-bundle-sibling"), true);
        FileUtil.purge(DEPLOY_DIR, true);
        FileUtil.purge(WORKING_DIR, true);
    }

    public void testParse_legacy() throws Exception {
        testParse(false, "legacy-test-bundle-v1.xml");
    }

    public void testParse() throws Exception {
        testParse(true, "test-bundle-v1.xml");
    }

    private void testParse(boolean validate, String recipeFile) throws Exception {
        AntLauncher ant = new AntLauncher(validate);

        BundleAntProject project = ant.parseBundleDeployFile(getFileFromTestClasses(recipeFile), null);
        assertNotNull(project);
        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 5, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("prepareDatasource.cli"), String.valueOf(bundleFiles)); // handed over file
        assertTrue(bundleFiles.contains("test-v1.properties"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("file.zip"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("foo-script"), String.valueOf(bundleFiles)); // from install-system-service
        assertTrue(bundleFiles.contains("foo-config"), String.valueOf(bundleFiles)); // from install-system-service

        assertEquals(project.getBundleName(), "example.com (JBoss EAP 4.3)");
        assertEquals(project.getBundleVersion(), "1.0");
        assertEquals(project.getBundleDescription(), "example.com corporate website hosted on JBoss EAP 4.3");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assertEquals(configDef.getPropertyDefinitions().size(), 1, String.valueOf(configDef.getPropertyDefinitions()));
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.INTEGER);
        assertEquals(propDef.getDefaultValue(), "8080");
        assertEquals(propDef.getDescription(), "This is where the product will listen for incoming messages");
        assertTrue(propDef.isRequired());

        // all we did was parse, nothing should really have been extracted or installed
        assertFalse(DEPLOY_DIR.exists(), "Nothing should have been installed to the deploy dir");
    }

    public void testParseWithNoDestinationComplianceCheck() throws Exception {
        //instantiate the launcher in the new validating mode (new as of RHQ 4.9.0)
        AntLauncher ant = new AntLauncher(true);

        try {
            ant.parseBundleDeployFile(getFileFromTestClasses("test-bundle-no-manage-root-dir-nor-compliance.xml"), null);
            fail("Parsing a bundle with no explicit manageRootDir should have failed.");
        } catch (InvalidBuildFileException e) {
            assertEquals(e.getMessage(),
                "The deployment unit must specifically declare compliance mode of the destination directory.");
        }

        BundleAntProject project = ant.parseBundleDeployFile(
            getFileFromTestClasses("test-bundle-with-manage-root-dir.xml"), null);
        assertNotNull(project);
        BundleTask bundleTask = findBundleTask(project);
        assertNotNull(bundleTask);
        assertNotNull(bundleTask.getDeploymentUnits());
        assertEquals(bundleTask.getDeploymentUnits().size(), 1);
        DeploymentUnitType deploymentUnit = bundleTask.getDeploymentUnits().values().iterator().next();
        assertNotNull(deploymentUnit);

        //assert the compatibility with the legacy attribute
        //noinspection deprecation
        assertEquals(deploymentUnit.getManageRootDir(), "false");
        assertEquals(DestinationComplianceMode.filesAndDirectories, deploymentUnit.getCompliance());

        // all we did was parse, nothing should really have been extracted or installed
        assertFalse(DEPLOY_DIR.exists(), "Nothing should have been installed to the deploy dir");
    }

    public void testInstall_legacy() throws Exception {
        testInstall(false, "legacy-test-bundle-v1.xml");
    }

    @Test(dependsOnMethods = "testUpgrade_legacy")
    public void testInstall() throws Exception {
        testInstall(true, "test-bundle-v1.xml");
    }

    private void testInstall(boolean validate, String recipeFile) throws Exception {
        skipNonRHLinux();

        // but we do want to add an unrelated file to see that it goes away - since we have manageRootDir=true
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-v1-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assertNotNull(project);
        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 5, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("prepareDatasource.cli"), String.valueOf(bundleFiles)); // handed over file
        assertTrue(bundleFiles.contains("test-v1.properties"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("file.zip"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("foo-script"), String.valueOf(bundleFiles)); // from install-system-service
        assertTrue(bundleFiles.contains("foo-config"), String.valueOf(bundleFiles)); // from install-system-service

        assertEquals(project.getBundleName(), "example.com (JBoss EAP 4.3)");
        assertEquals(project.getBundleVersion(), "1.0");
        assertEquals(project.getBundleDescription(), "example.com corporate website hosted on JBoss EAP 4.3");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assertEquals(configDef.getPropertyDefinitions().size(), 1, String.valueOf(configDef.getPropertyDefinitions()));
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.INTEGER);
        assertEquals(propDef.getDefaultValue(), "8080");
        assertEquals(propDef.getDescription(), "This is where the product will listen for incoming messages");
        assertTrue(propDef.isRequired());

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assertEquals(config.getProperties().size(), 1, String.valueOf(config.getProperties()));
        assertEquals(config.getSimpleValue("listener.port", null), "10000", String.valueOf(config.getProperties()));

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assertEquals(preinstallTargetExecuted, "1a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assertEquals(postinstallTargetExecuted, "1b");

        assertTrue(new File(DEPLOY_DIR, "subdir/test.properties").exists(), "missing file");
        assertTrue(new File(DEPLOY_DIR, "archived-bundle-file.txt").exists(), "missing archived bundle file");
        assertTrue(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists(),
            "missing subdir archive file");
        assertFalse(unrelatedFile.exists(), "unrelated file was not removed during the install");
        assertEquals(readPropsFile(new File(DEPLOY_DIR, "subdir/test.properties")).getProperty("junk.listener.port"),
            "10000");
        assertEquals(readPropsFile(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties"))
            .getProperty("templatized.variable"), "10000");
    }

    private void skipNonRHLinux() {
        if (!System.getProperty("os.name").equals("Linux") || !REDHAT_RELEASE_FILE.exists()) {
            throw new SkipException("This test only works on Red Hat Linux flavors");
        }
    }

    @Test(dependsOnMethods = "testInstall_legacy")
    public void testUpgrade_legacy() throws Exception {
        testUpgrade(false, "legacy-test-bundle-v2.xml");
    }

    @Test(dependsOnMethods = "testInstall")
    public void testUpgrade() throws Exception {
        testUpgrade(true, "test-bundle-v2.xml");
    }

    private void testUpgrade(boolean validate, String recipeFile) throws Exception {

        skipNonRHLinux();

        // add an unrelated file to see that it gets deleted as part of the upgrade
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-v2-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assertNotNull(project);
        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 5, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("fileToHandover.zip"), String.valueOf(bundleFiles)); // handed over file
        assertTrue(bundleFiles.contains("test-v2.properties"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("file.zip"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("foo-script"), String.valueOf(bundleFiles)); // from install-system-service
        assertTrue(bundleFiles.contains("foo-config"), String.valueOf(bundleFiles)); // from install-system-service

        assertEquals(project.getBundleName(), "example.com (JBoss EAP 4.3)");
        assertEquals(project.getBundleVersion(), "2.5");
        assertEquals(project.getBundleDescription(), "updated bundle");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assertEquals(configDef.getPropertyDefinitions().size(), 1, String.valueOf(configDef.getPropertyDefinitions()));
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.INTEGER);
        assertEquals(propDef.getDefaultValue(), "9090");
        assertEquals(propDef.getDescription(), "This is where the product will listen for incoming messages");
        assertTrue(propDef.isRequired());

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assertEquals(config.getProperties().size(), 1);
        assertEquals(config.getSimpleValue("listener.port", null), "20000", String.valueOf(config.getProperties()));

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assertEquals(preinstallTargetExecuted, "2a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assertEquals(postinstallTargetExecuted, "2b");

        assertTrue(new File(DEPLOY_DIR, "subdir/test.properties").exists(), "missing file");
        assertTrue(new File(DEPLOY_DIR, "archived-bundle-file.txt").exists(), "missing archived bundle file");
        assertTrue(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists(),
            "missing subdir archive file");
        assertFalse(unrelatedFile.exists(),
            "we are managing root dir so unrelated file should be removed during upgrade");
        assertEquals(readPropsFile(new File(DEPLOY_DIR, "subdir/test.properties")).getProperty("junk.listener.port"),
            "20000");
        assertEquals(readPropsFile(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties"))
            .getProperty("templatized.variable"), "20000");
    }

    public void testUpgradeNoManageRootDir_legacy() throws Exception {
        testUpgradeNoManageRootDir(false, "legacy-test-bundle-v2-noManageRootDir.xml");
    }

    public void testUpgradeNoManageRootDir() throws Exception {
        testUpgradeNoManageRootDir(true, "test-bundle-v2-filesAndDirectories.xml");
    }

    private void testUpgradeNoManageRootDir(boolean validate, String recipeFile) throws Exception {

        skipNonRHLinux();

        // We want to test an upgrade, so do *not* wipe out the deploy dir - let's re-invoke testInstall
        // to get us to an initial state of the v1 bundle installed
        testInstall();

        // we still want the unrelated file - we want to see that manageRootDir=false works (unrelated files should not be deleted)
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");
        assertTrue(unrelatedFile.exists(), "our initial install test method should have prepared an unmanaged file");

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-v2-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assertNotNull(project);
        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 4, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("test-v2.properties"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("file.zip"), String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("foo-script"), String.valueOf(bundleFiles)); // from install-system-service
        assertTrue(bundleFiles.contains("foo-config"), String.valueOf(bundleFiles)); // from install-system-service

        assertEquals(project.getBundleName(), "example.com (JBoss EAP 4.3)");
        assertEquals(project.getBundleVersion(), "2.5");
        assertEquals(project.getBundleDescription(), "updated bundle");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assertEquals(configDef.getPropertyDefinitions().size(), 1, String.valueOf(configDef.getPropertyDefinitions()));
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.INTEGER);
        assertEquals(propDef.getDefaultValue(), "9090");
        assertEquals(propDef.getDescription(), "This is where the product will listen for incoming messages");
        assertTrue(propDef.isRequired());

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assertEquals(config.getProperties().size(), 1);
        assertEquals(config.getSimpleValue("listener.port", null), "20000", String.valueOf(config.getProperties()));

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assertEquals(preinstallTargetExecuted, "2a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assertEquals(postinstallTargetExecuted, "2b");

        assertTrue(new File(DEPLOY_DIR, "subdir/test.properties").exists(), "missing file");
        assertTrue(new File(DEPLOY_DIR, "archived-bundle-file.txt").exists(), "missing archived bundle file");
        assertTrue(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists(),
            "missing subdir archive file");
        assertTrue(unrelatedFile.exists(),
            "we are NOT managing root dir so unrelated file should NOT be removed during upgrade");
        assertEquals(readPropsFile(new File(DEPLOY_DIR, "subdir/test.properties")).getProperty("junk.listener.port"),
            "20000");
        assertEquals(readPropsFile(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties"))
            .getProperty("templatized.variable"), "20000");
    }

    public void testInstallCompressedZipNoDryRun_legacy() throws Exception {
        testInstallCompressedZip(false, false, "legacy-test-bundle-compressed-archives.xml");
    }

    public void testInstallCompressedZipNoDryRun() throws Exception {
        testInstallCompressedZip(false, true, "test-bundle-compressed-archives.xml");
    }

    public void testInstallCompressedZipDryRun_legacy() throws Exception {
        testInstallCompressedZip(true, false, "legacy-test-bundle-compressed-archives.xml");
    }

    public void testInstallCompressedZipDryRun() throws Exception {
        testInstallCompressedZip(true, true, "test-bundle-compressed-archives.xml");
    }

    private void testInstallCompressedZip(boolean dryRun, boolean validate, String recipeFile) throws Exception {
        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-compressed-archives-input.properties", dryRun);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assertNotNull(project);
        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 1, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("file.zip"), String.valueOf(bundleFiles));

        assertEquals(project.getBundleName(), "test compressed archive files");
        assertEquals(project.getBundleVersion(), "1.0");
        assertNull(project.getBundleDescription());

        // while we are here, let's see that we have 0 config props
        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assertEquals(configDef.getPropertyDefinitions().size(), 0, String.valueOf(configDef.getPropertyDefinitions()));
        Configuration config = project.getConfiguration();
        assertEquals(config.getProperties().size(), 0, String.valueOf(config.getProperties()));

        if (!dryRun) {
            assertTrue(new File(DEPLOY_DIR, "file.zip").exists(), "should be here, we told it to stay compressed");
        } else {
            assertFalse(new File(DEPLOY_DIR, "file.zip").exists(), "dry run - should not be here");
        }

        assertFalse(new File(DEPLOY_DIR, "archived-bundle-file.txt").exists(), "should not have exploded this");
        assertFalse(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists(),
            "should not have exploded this");
        assertFalse(new File(DEPLOY_DIR, "archived-subdir").isDirectory(), "should not still have the exploded dir");

        DeploymentsMetadata dm = new DeploymentsMetadata(DEPLOY_DIR);
        if (!dryRun) {
            FileHashcodeMap fhm = dm.getCurrentDeploymentFileHashcodes();
            assertFalse(fhm.containsKey("archived-bundle-file.txt"),
                "should not have metadata - this is inside the compressed zip");
            assertFalse(fhm.containsKey("archived-subdir/archived-file-in-subdir.properties"),
                "should not have metadata - this is inside the compressed zip");
            assertTrue(fhm.containsKey("file.zip"),
                String
                    .valueOf("should have metadata for this - we didn't explode it, we just have this compressed file"));

            // test that we created the zip OK. Note that our test did not do any file replacing/realization of templates
            final String[] templateVarValue = new String[1];
            final int[] entries = new int[1];
            ZipUtil.walkZipFile(new File(DEPLOY_DIR, "file.zip"), new ZipUtil.ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (entry.getName().equals("archived-subdir/archived-file-in-subdir.properties")) {
                        Properties props = new Properties();
                        props.load(stream);
                        templateVarValue[0] = props.getProperty("templatized.variable");
                    }
                    if (!entry.isDirectory()) {
                        entries[0] = entries[0] + 1;
                    }
                    return true;
                }
            });
            assertNotNull(templateVarValue[0]);
            assertEquals(templateVarValue[0], "@@listener.port@@");
            assertEquals(entries[0], 2, String.valueOf(entries[0])); // we only counted the file entries
        } else {
            try {
                dm.getCurrentDeploymentFileHashcodes();
                fail("this was a dry run, we should not have written our metadata to the filesystem");
            } catch (Exception e) {
                // expected
            }
        }
    }

    public void testInstallCompressedZipWithTemplatizedFilesNoDryRun_legacy() throws Exception {
        testInstallCompressedZipWithTemplatizedFiles(false, false,
            "legacy-test-bundle-compressed-archives-with-replace.xml");
    }

    public void testInstallCompressedZipWithTemplatizedFilesNoDryRun() throws Exception {
        testInstallCompressedZipWithTemplatizedFiles(false, true, "test-bundle-compressed-archives-with-replace.xml");
    }

    public void testInstallCompressedZipWithTemplatizedFilesDryRun_legacy() throws Exception {
        testInstallCompressedZipWithTemplatizedFiles(true, false,
            "legacy-test-bundle-compressed-archives-with-replace.xml");
    }

    public void testInstallCompressedZipWithTemplatizedFilesDryRun() throws Exception {
        testInstallCompressedZipWithTemplatizedFiles(true, true, "test-bundle-compressed-archives-with-replace.xml");
    }

    private void testInstallCompressedZipWithTemplatizedFiles(boolean dryRun, boolean validate, String recipeFile)
        throws Exception {
        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-compressed-archives-input.properties", dryRun);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assertNotNull(project);
        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 1, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("file.zip"), String.valueOf(bundleFiles));

        assertEquals(project.getBundleName(), "test compressed archive files");
        assertEquals(project.getBundleVersion(), "1.0");
        assertNull(project.getBundleDescription());

        // we have one property that we use to realize our content
        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assertEquals(configDef.getPropertyDefinitions().size(), 1, String.valueOf(configDef.getPropertyDefinitions()));
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.INTEGER);
        assertNull(propDef.getDefaultValue(), "recipe didn't define a default for our property");
        assertNull(propDef.getDescription(), "recipe didn't define a description for our property");
        assertTrue(propDef.isRequired(), "recipe didn't make the property required, but the default should be required");

        if (!dryRun) {
            assertTrue(new File(DEPLOY_DIR, "file.zip").exists(), "should be here, we told it to stay compressed");
        } else {
            assertFalse(new File(DEPLOY_DIR, "file.zip").exists(), "this was a dry run, should not be here");
        }
        assertFalse(new File(DEPLOY_DIR, "archived-bundle-file.txt").exists(), "should not have exploded this");
        assertFalse(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists(),
            "should not have exploded this");
        assertFalse(new File(DEPLOY_DIR, "archived-subdir").isDirectory(), "should not still have the exploded dir");

        DeploymentsMetadata dm = new DeploymentsMetadata(DEPLOY_DIR);
        if (!dryRun) {
            FileHashcodeMap fhm = dm.getCurrentDeploymentFileHashcodes();
            assertFalse(fhm.containsKey("archived-bundle-file.txt"),
                "should not have metadata - this is inside the compressed zip");
            assertFalse(fhm.containsKey("archived-subdir/archived-file-in-subdir.properties"),
                "should not have metadata - this is inside the compressed zip");
            assertTrue(fhm.containsKey("file.zip"),
                String
                    .valueOf("should have metadata for this - we didn't explode it, we just have this compressed file"));

            // test that the file in the zip is realized
            final String[] templateVarValue = new String[1];
            final int[] entries = new int[1];
            ZipUtil.walkZipFile(new File(DEPLOY_DIR, "file.zip"), new ZipUtil.ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (entry.getName().equals("archived-subdir/archived-file-in-subdir.properties")) {
                        Properties props = new Properties();
                        props.load(stream);
                        templateVarValue[0] = props.getProperty("templatized.variable");
                    }
                    if (!entry.isDirectory()) {
                        entries[0] = entries[0] + 1;
                    }
                    return true;
                }
            });
            assertNotNull(templateVarValue[0]);
            assertEquals(templateVarValue[0], "12345");
            assertEquals(entries[0], 2, String.valueOf(entries[0])); // we only counted the file entries
        } else {
            try {
                dm.getCurrentDeploymentFileHashcodes();
                fail("this was a dry run, we should not have written our metadata to the filesystem");
            } catch (Exception e) {
                // expected
            }
        }
    }

    public void testAuditMessages_legacy() throws Exception {
        testAuditMessages(false, "legacy-test-bundle-audit.xml");
    }

    public void testAuditMessages() throws Exception {
        testAuditMessages(true, "test-bundle-audit.xml");
    }

    // this doesn't verify the audit messages getting emitted are correct
    // but it does verify the audit tag getting processed correctly.
    // you have to look at the test logs to see the audit messages
    // TODO: write a ant build listener to listen for this messages, parse them and verify they are correct
    //       this test should then ask the listener at the end if everything was OK and assertTrue(false if not
    private void testAuditMessages(boolean validate, String recipeFile) throws Exception {
        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-audit-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assertNotNull(project);
        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 1, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("test-audit.properties"), String.valueOf(bundleFiles));

        // sanity check - make sure our recipe defined this property
        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assertEquals(configDef.getPropertyDefinitions().size(), 1, String.valueOf(configDef.getPropertyDefinitions()));
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assertNotNull(propDef);

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assertEquals(config.getProperties().size(), 1, String.valueOf(config.getProperties()));
        assertEquals(config.getSimpleValue("listener.port", null), "777", String.valueOf(config.getProperties()));

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assertEquals(preinstallTargetExecuted, "1a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assertEquals(postinstallTargetExecuted, "1b");

        assertTrue(new File(DEPLOY_DIR, "test-audit.properties").exists(), "missing file");
        assertEquals(readPropsFile(new File(DEPLOY_DIR, "test-audit.properties")).getProperty("my.listener.port"),
            "777");
    }

    public void testSubdirectoriesInRecipe_legacy() throws Exception {
        testSubdirectoriesInRecipe(false, "legacy-test-bundle-subdir.xml");
    }

    public void testSubdirectoriesInRecipe() throws Exception {
        testSubdirectoriesInRecipe(true, "test-bundle-subdir.xml");
    }

    private void testSubdirectoriesInRecipe(boolean validate, String origRecipeFile) throws Exception {
        // we need to create our own directory structure - let's build a temporary ant basedir
        // and put our recipe in there as well as a subdirectory with a test raw file and test zip file
        File antBasedir = FileUtil.createTempDirectory("anttest", ".test", null);
        try {
            File subdir = new File(antBasedir, "subdir"); // must match the name in the recipe
            subdir.mkdirs();
            writeFile("file0", subdir, "test0.txt"); // filename must match recipe
            writeFile("file1", subdir, "test1.txt"); // filename must match recipe
            writeFile("file2", subdir, "test2.txt"); // filename must match recipe
            createZip(new String[] { "one", "two" }, subdir, "test.zip", new String[] { "one.txt", "two.txt" });
            createZip(new String[] { "3", "4" }, subdir, "test-explode.zip", new String[] { "three.txt", "four.txt" });
            createZip(new String[] { "X=@@X@@\n" }, subdir, "test-replace.zip", new String[] { "template.txt" }); // will be exploded then recompressed
            createZip(new String[] { "X=@@X@@\n" }, subdir, "test-replace2.zip", new String[] { "template.txt" }); // will be exploded then recompressed
            File recipeFile = new File(antBasedir, "deploy.xml");
            FileUtil.copyFile(new File(ANT_BASEDIR, origRecipeFile), recipeFile);

            AntLauncher ant = new AntLauncher(validate);
            Properties inputProps = new Properties();
            inputProps.setProperty(DeployPropertyNames.DEPLOY_DIR, DEPLOY_DIR.getPath());
            inputProps.setProperty(DeployPropertyNames.DEPLOY_ID, String.valueOf(++this.deploymentId));
            inputProps.setProperty(DeployPropertyNames.DEPLOY_PHASE, DeploymentPhase.INSTALL.name());
            inputProps.setProperty("X", "alpha-omega");
            List<BuildListener> buildListeners = createBuildListeners();

            BundleAntProject project = ant.executeBundleDeployFile(recipeFile, inputProps, buildListeners);
            assertNotNull(project);
            Set<String> bundleFiles = project.getBundleFileNames();
            assertNotNull(bundleFiles);
            assertEquals(bundleFiles.size(), 7, String.valueOf(bundleFiles));
            assertTrue(bundleFiles.contains("subdir/test0.txt"), String.valueOf(bundleFiles));
            assertTrue(bundleFiles.contains("subdir/test1.txt"), String.valueOf(bundleFiles));
            assertTrue(bundleFiles.contains("subdir/test2.txt"), String.valueOf(bundleFiles));
            assertTrue(bundleFiles.contains("subdir/test.zip"), String.valueOf(bundleFiles));
            assertTrue(bundleFiles.contains("subdir/test-explode.zip"), String.valueOf(bundleFiles));
            assertTrue(bundleFiles.contains("subdir/test-replace.zip"), String.valueOf(bundleFiles));
            assertTrue(bundleFiles.contains("subdir/test-replace2.zip"), String.valueOf(bundleFiles));

            assertTrue(new File(DEPLOY_DIR, "subdir/test0.txt").exists(),
                "missing raw file from default destination location");
            assertTrue(new File(DEPLOY_DIR, "another/foo.txt").exists(), "missing raw file from the destinationFile");
            assertTrue(new File(DEPLOY_DIR, "second.dir/test2.txt").exists(),
                "missing raw file from the destinationDir");
            assertFalse(new File(DEPLOY_DIR, "subdir/test1.txt").exists(),
                "should not be here because destinationFile was specified");
            assertFalse(new File(DEPLOY_DIR, "subdir/test2.txt").exists(),
                "should not be here because destinationFile was specified");
            assertTrue(new File(DEPLOY_DIR, "subdir/test.zip").exists(), "missing unexploded zip file");
            assertTrue(new File(DEPLOY_DIR, "subdir/test-replace.zip").exists(), "missing unexploded zip file");
            assertTrue(new File(DEPLOY_DIR, "second.dir/test-replace2.zip").exists(),
                "missing unexploded second zip file");
            assertFalse(new File(DEPLOY_DIR, "subdir/test-explode.zip").exists(), "should have been exploded");

            // test that the file in the zip is realized
            final String[] templateVarValue = new String[] { null };
            ZipUtil.walkZipFile(new File(DEPLOY_DIR, "subdir/test-replace.zip"), new ZipUtil.ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (entry.getName().equals("template.txt")) {
                        Properties props = new Properties();
                        props.load(stream);
                        templateVarValue[0] = props.getProperty("X");
                    }
                    return true;
                }
            });
            assertNotNull(templateVarValue[0]);
            assertEquals(templateVarValue[0], "alpha-omega");

            // test that the file in the second zip is realized
            final String[] templateVarValue2 = new String[] { null };
            ZipUtil.walkZipFile(new File(DEPLOY_DIR, "second.dir/test-replace2.zip"), new ZipUtil.ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (entry.getName().equals("template.txt")) {
                        Properties props = new Properties();
                        props.load(stream);
                        templateVarValue2[0] = props.getProperty("X");
                    }
                    return true;
                }
            });
            assertNotNull(templateVarValue2[0]);
            assertEquals(templateVarValue2[0], "alpha-omega");

        } finally {
            FileUtil.purge(antBasedir, true);
        }
    }

    public void testUrlFilesAndArchives_legacy() throws Exception {
        testUrlFilesAndArchives(false, "legacy-test-bundle-url.xml");
    }

    public void testUrlFilesAndArchives() throws Exception {
        testUrlFilesAndArchives(true, "test-bundle-url.xml");
    }

    public void testNotDeployedPropertyFile() throws Exception {
        testNotDeployedFiles(getFileFromTestClasses("ant-properties/deploy.xml.properties-in-bundle"), true, false);
    }

    public void testNotDeployedRhqPropertyFile() throws Exception {
        testNotDeployedFiles(getFileFromTestClasses("ant-properties/deploy.xml.rhq-property-tag-in-bundle"), true,
            false);
    }

    public void testDeployedPropertyFile() throws Exception {
        testNotDeployedFiles(getFileFromTestClasses("ant-properties/deploy.xml.properties-in-bundle-props-deployed"),
            true, true);
    }

    public void testAntPropertiesUsedForTokenReplacement() throws Exception {
        testNotDeployedPropertyFile();
        checkPropertiesFromExternalFileReplaced();
    }

    public void testAntPropertiesLoadFromAbsolutePath() throws Exception {
        File tempDir = FileUtil.createTempDirectory("ant-launcher-test", null, null);

        try {
            //prepare the test bundle.. update the recipe with an absolute path to a properties file.
            File deployXml = new File(tempDir, "deploy.xml");

            FileUtil.copyFile(getFileFromTestClasses("ant-properties/deploy.xml.properties-out-of-bundle"), deployXml);

            //copy the other file from the bundle, too, into the correct location
            FileUtil.copyFile(getFileFromTestClasses("ant-properties/deployed.file"),
                new File(tempDir, "deployed.file"));

            File absoluteLocation = new File(tempDir, "absolute-location");
            assert absoluteLocation.mkdir() : "Failed to create dir under temp";

            String deployXmlContents = StreamUtil.slurp(new InputStreamReader(new FileInputStream(deployXml)));

            File absolutePropertiesLocation = new File(absoluteLocation, "in-bundle.properties");
            FileUtil
                .copyFile(getFileFromTestClasses("ant-properties/in-bundle.properties"), absolutePropertiesLocation);

            deployXmlContents = deployXmlContents.replace("%%REPLACE_ME%%",
                "file=\"" + absolutePropertiesLocation.getAbsolutePath() + "\"");

            FileUtil.writeFile(new ByteArrayInputStream(deployXmlContents.getBytes()), deployXml);

            //k, now the test itself...
            testNotDeployedFiles(deployXml, false, false);
            checkPropertiesFromExternalFileReplaced();
        } finally {
            FileUtil.purge(tempDir, true);
        }
    }

    public void testAntPropertiesLoadFromURL() throws Exception {
        File tempDir = FileUtil.createTempDirectory("ant-launcher-test", null, null);
        Undertow undertow = null;

        try {
            //prepare the test bundle.. update the recipe with an absolute path to a properties file.
            File deployXml = new File(tempDir, "deploy.xml");

            FileUtil.copyFile(getFileFromTestClasses("ant-properties/deploy.xml.properties-out-of-bundle"), deployXml);

            //copy the other file from the bundle, too, into the correct location
            FileUtil.copyFile(getFileFromTestClasses("ant-properties/deployed.file"),
                new File(tempDir, "deployed.file"));

            //fire up minimal server
            PortScout portScout = new PortScout();
            int port = 0;
            try {
                port = portScout.getNextFreePort();
            } finally {
                portScout.close();
            }

            undertow = Undertow.builder().addHttpListener(port, "localhost").setHandler(new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                    httpServerExchange.startBlocking();
                    FileInputStream in = new FileInputStream(
                        getFileFromTestClasses("ant-properties/in-bundle.properties"));
                    try {
                        StreamUtil.copy(in, httpServerExchange.getOutputStream(), false);
                    } catch (Exception e) {
                        LOG.error("Failed to handle the HTTP request for loading properties.", e);
                        throw e;
                    } finally {
                        StreamUtil.safeClose(in);
                    }
                }
            }).build();

            undertow.start();

            String deployXmlContents = StreamUtil.slurp(new InputStreamReader(new FileInputStream(deployXml)));

            deployXmlContents = deployXmlContents.replace("%%REPLACE_ME%%", "url=\"http://localhost:" + port + "\"");

            FileUtil.writeFile(new ByteArrayInputStream(deployXmlContents.getBytes()), deployXml);

            //k, now the test itself...
            testNotDeployedFiles(deployXml, false, false);
            checkPropertiesFromExternalFileReplaced();
        } finally {
            FileUtil.purge(tempDir, true);
            if (undertow != null) {
                undertow.stop();
            }
        }
    }

    public void testRhqPropertiesLoadFromDestination() throws Exception {
        File sideDir = new File(DEPLOY_DIR.getParentFile(), "test-ant-bundle-sibling");
        assert sideDir.mkdir() : "Failed to create a side directory";

        FileUtil.copyFile(getFileFromTestClasses("ant-properties/in-bundle.properties"), new File(sideDir,
            "in-bundle.properties"));

        testNotDeployedFiles(getFileFromTestClasses("ant-properties/deploy.xml.rhq-property-tag-in-destination"),
            false, false);
    }

    private void testNotDeployedFiles(File deployXml, boolean expectPropertiesFileInBundle,
        boolean expectPropertiesFileInDestination) throws Exception {
        FileUtil.purge(DEPLOY_DIR, true);
        FileUtil.purge(WORKING_DIR, true);

        FileUtil.copyDirectory(deployXml.getParentFile(), WORKING_DIR);

        deployXml = new File(WORKING_DIR, deployXml.getName());

        AntLauncher ant = new AntLauncher(true);
        Properties inputProps = createInputProperties(null);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(deployXml, inputProps, buildListeners);

        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == (expectPropertiesFileInBundle ? 2 : 1) : bundleFiles;
        assert bundleFiles.contains("deployed.file") : bundleFiles;
        assert !expectPropertiesFileInBundle || bundleFiles.contains("in-bundle.properties") : bundleFiles;

        assert new File(DEPLOY_DIR, "deployed.file").exists() : "deployed.file missing";
        assert expectPropertiesFileInDestination == new File(DEPLOY_DIR, "in-bundle.properties").exists() : "in-bundle.properties "
            + (expectPropertiesFileInDestination ? "not deployed but should have" : "deployed but shouldn't have");
    }

    private void checkPropertiesFromExternalFileReplaced() throws Exception {
        Properties props = readPropsFile(new File(DEPLOY_DIR, "deployed.file"));

        assert "user provided value".equals(props.getProperty("user.provided")) : "user.provided";
        assert "bundle provided value".equals(props.getProperty("bundle.provided")) : "bundle.provided";
        assert "a".equals(props.get("a.from.properties.file")) : "a.from.properties.file";
        assert "b".equals(props.get("b.from.properties.file")) : "b.from.properties.file";
    }

    private void testUrlFilesAndArchives(boolean validate, String recipeFile) throws Exception {
        // we need to create our own directory structure so we can use file: URLs
        File tmpUrlLocation = FileUtil.createTempDirectory("anttest", ".url", null);
        Set<File> downloadedFiles = null;

        try {
            File subdir = new File(tmpUrlLocation, "subdir"); // must match the name in the recipe
            subdir.mkdirs();
            writeFile("file0", subdir, "test0.txt"); // filename must match recipe
            writeFile("file1", subdir, "test1.txt"); // filename must match recipe
            writeFile("X=@@X@@\n", subdir, "test2.txt"); // filename must match recipe
            writeFile("pipo", subdir, "prepareDatasource.cli"); // filename must match recipe
            createZip(new String[] { "one", "two" }, subdir, "test.zip", new String[] { "one.txt", "two.txt" });
            createZip(new String[] { "3", "4" }, subdir, "test-explode.zip", new String[] { "three.txt", "four.txt" });
            createZip(new String[] { "X=@@X@@\n" }, subdir, "test-replace.zip", new String[] { "template.txt" }); // will be exploded then recompressed
            createZip(new String[] { "one", "two" }, subdir, "fileToHandover.zip",
                new String[] { "one.txt", "two.txt" });

            AntLauncher ant = new AntLauncher(validate);
            Properties inputProps = createInputProperties("/test-bundle-url-input.properties");
            inputProps.setProperty("rhq.test.url.dir", tmpUrlLocation.toURI().toURL().toString()); // we use this so our recipe can use URLs
            List<BuildListener> buildListeners = createBuildListeners();

            BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
                buildListeners);
            assertNotNull(project);

            Set<String> bundleFiles = project.getBundleFileNames();
            assertNotNull(bundleFiles);
            assertEquals(bundleFiles.size(), 0, "we don't have any bundle files - only downloaded files from URLs: "
                + bundleFiles);

            downloadedFiles = project.getDownloadedFiles();
            assertNotNull(downloadedFiles);
            assertEquals(downloadedFiles.size(), 8, String.valueOf(downloadedFiles));
            ArrayList<String> expectedDownloadedFileNames = new ArrayList<String>();
            // remember, we store url downloaded files under the names of their destination file/dir, not source location
            expectedDownloadedFileNames.add("test0.txt");
            expectedDownloadedFileNames.add("foo.txt");
            expectedDownloadedFileNames.add("test2.txt");
            expectedDownloadedFileNames.add("prepareDatasource.cli");
            expectedDownloadedFileNames.add("test.zip");
            expectedDownloadedFileNames.add("test-explode.zip");
            expectedDownloadedFileNames.add("test-replace.zip");
            expectedDownloadedFileNames.add("fileToHandover.zip");
            for (File downloadedFile : downloadedFiles) {
                assertTrue(expectedDownloadedFileNames.contains(downloadedFile.getName()),
                    "We downloaded a file but its not in the project's list: " + downloadedFile);
            }

            assertTrue(new File(DEPLOY_DIR, "test0.txt").exists(), "missing raw file from default destination location");
            assertTrue(new File(DEPLOY_DIR, "another/foo.txt").exists(), "missing raw file from the destinationFile");
            assertTrue(new File(DEPLOY_DIR, "second.dir/test2.txt").exists(),
                "missing raw file from the destinationDir");
            assertFalse(new File(DEPLOY_DIR, "test1.txt").exists(),
                "should not be here because destinationFile was specified");
            assertFalse(new File(DEPLOY_DIR, "test2.txt").exists(),
                "should not be here because destinationFile was specified");
            assertTrue(new File(DEPLOY_DIR, "test.zip").exists(), "missing unexploded zip file");
            assertTrue(new File(DEPLOY_DIR, "test-replace.zip").exists(), "missing unexploded zip file");
            assertFalse(new File(DEPLOY_DIR, "test-explode.zip").exists(), "should have been exploded");

            // test that the file in the zip is realized
            final String[] templateVarValue = new String[] { null };
            ZipUtil.walkZipFile(new File(DEPLOY_DIR, "test-replace.zip"), new ZipUtil.ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (entry.getName().equals("template.txt")) {
                        Properties props = new Properties();
                        props.load(stream);
                        templateVarValue[0] = props.getProperty("X");
                    }
                    return true;
                }
            });
            assertNotNull(templateVarValue[0]);
            assertEquals(templateVarValue[0], "9876");

            // test that our raw file was realized
            File realizedFile = new File(DEPLOY_DIR, "second.dir/test2.txt");
            Properties props = new Properties();
            FileInputStream inStream = new FileInputStream(realizedFile);
            try {
                props.load(inStream);
                assertEquals(props.getProperty("X", "<unset>"), "9876");
            } finally {
                inStream.close();
            }
        } finally {
            FileUtil.purge(tmpUrlLocation, true);
            if (downloadedFiles != null) {
                for (File doomed : downloadedFiles) {
                    doomed.delete();
                }
            }
        }
    }

    public void testHandover() throws Exception {
        AntLauncher ant = new AntLauncher(true);

        final List<HandoverInfoArgument> handoverInfoArguments = new ArrayList<HandoverInfoArgument>();
        HandoverTarget handoverTarget = new HandoverTarget() {
            @Override
            public boolean handoverContent(HandoverInfo handoverInfo) {
                HandoverInfoArgument handoverInfoArgument;
                try {
                    handoverInfoArgument = new HandoverInfoArgument(handoverInfo);
                } catch (IOException e) {
                    return false;
                }
                handoverInfoArguments.add(handoverInfoArgument);
                try {
                    FileUtil.writeFile(handoverInfo.getContent(), handoverInfoArgument.handoverInfoTestContentFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        };
        ant.setHandoverTarget(handoverTarget);

        List<BuildListener> buildListeners = createBuildListeners();
        Properties inputProps = createInputProperties("/handover-test-bundle-input.properties");

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses("handover-test-bundle.xml"),
            inputProps, buildListeners);
        assertNotNull(project);
        assertEquals(project.getBundleName(), "example.com (EAP 6)");
        assertEquals(project.getBundleVersion(), "1.0");
        assertEquals(project.getBundleDescription(), "example.com corporate website hosted on EAP 6");

        Set<String> bundleFiles = project.getBundleFileNames();
        assertNotNull(bundleFiles);
        assertEquals(bundleFiles.size(), 2, String.valueOf(bundleFiles));
        assertTrue(bundleFiles.contains("prepareDatasource.cli"), String.valueOf(bundleFiles)); // handed over file
        assertTrue(bundleFiles.contains("fileToHandover.zip"), String.valueOf(bundleFiles)); // handed over archive

        ConfigurationDefinition projectConfigDef = project.getConfigurationDefinition();
        assertEquals(projectConfigDef.getPropertyDefinitions().size(), 3,
            String.valueOf(projectConfigDef.getPropertyDefinitions()));

        PropertyDefinitionSimple propDef = projectConfigDef.getPropertyDefinitionSimple("myapp.datasource.property");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.INTEGER);
        assertTrue(propDef.isRequired());

        propDef = projectConfigDef.getPropertyDefinitionSimple("myapp.listener.port");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.INTEGER);
        assertTrue(propDef.isRequired());

        propDef = projectConfigDef.getPropertyDefinitionSimple("myapp.runtime.name");
        assertNotNull(propDef);
        assertEquals(propDef.getType(), PropertySimpleType.STRING);
        assertTrue(propDef.isRequired());

        Configuration projectConfig = project.getConfiguration();
        assertNotNull(projectConfig);
        assertEquals(projectConfig.getProperties().size(), 3, String.valueOf(projectConfig.getProperties()));
        assertEquals(projectConfig.getSimpleValue("myapp.datasource.property"), "10",
            String.valueOf(projectConfig.getProperties()));
        assertEquals(projectConfig.getSimpleValue("myapp.listener.port"), "9777",
            String.valueOf(projectConfig.getProperties()));
        assertEquals(projectConfig.getSimpleValue("myapp.runtime.name"), "site.war",
            String.valueOf(projectConfig.getProperties()));

        assertEquals(handoverInfoArguments.size(), 2, String.valueOf(handoverInfoArguments));
        Iterator<HandoverInfoArgument> handoverInfoIterator = handoverInfoArguments.iterator();

        HandoverInfoArgument handoverInfoArgument = handoverInfoIterator.next();
        HandoverInfo handoverInfo = handoverInfoArgument.handoverInfo;
        InputStream cliScriptContent = getClass().getClassLoader().getResourceAsStream("prepareDatasource.cli");
        assertNotNull(cliScriptContent);
        FileInputStream actualContent = new FileInputStream(handoverInfoArgument.handoverInfoTestContentFile);
        assertEquals(slurp(actualContent), slurp(cliScriptContent));
        assertEquals(handoverInfo.getFilename(), "prepareDatasource.cli");
        assertEquals(handoverInfo.getAction(), "execute-script");
        assertEquals(handoverInfo.getParams(), Collections.emptyMap());
        assertEquals(handoverInfo.isRevert(), false);

        handoverInfoArgument = handoverInfoIterator.next();
        handoverInfo = handoverInfoArgument.handoverInfo;
        final Properties[] propertiesHolder = new Properties[1];
        ZipUtil.walkZipFile(handoverInfoArgument.handoverInfoTestContentFile, new ZipUtil.ZipEntryVisitor() {
            @Override
            public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                String entryName = entry.getName();
                if (entryName.equals("archived-subdir/archived-file-in-subdir.properties")) {
                    Properties properties = new Properties();
                    properties.load(stream);
                    propertiesHolder[0] = properties;
                }
                return true;
            }
        });
        Properties properties = propertiesHolder[0];
        assertNotNull(properties);
        assertEquals(properties.size(), 3, String.valueOf(properties));
        assertEquals(properties.getProperty("templatized.variable"), "9777", String.valueOf(properties));
        assertEquals(handoverInfo.getFilename(), "fileToHandover.zip");
        assertEquals(handoverInfo.getAction(), "deployment");
        assertEquals(handoverInfo.getParams(), new HashMap<String, String>() {
            {
                put("runtimeName", "site.war");
            }
        });
        assertEquals(handoverInfo.isRevert(), false);
    }

    public void testHandoverFailure() throws Exception {
        AntLauncher ant = new AntLauncher(true);

        final List<HandoverInfoArgument> handoverInfoArguments = new ArrayList<HandoverInfoArgument>();
        HandoverTarget handoverTarget = new HandoverTarget() {
            @Override
            public boolean handoverContent(HandoverInfo handoverInfo) {
                HandoverInfoArgument handoverInfoArgument;
                try {
                    handoverInfoArgument = new HandoverInfoArgument(handoverInfo);
                } catch (IOException e) {
                    return false;
                }
                handoverInfoArguments.add(handoverInfoArgument);
                return false;
            }
        };
        ant.setHandoverTarget(handoverTarget);

        List<BuildListener> buildListeners = createBuildListeners();
        Properties inputProps = createInputProperties("/handover-test-bundle-input.properties");

        try {
            ant.executeBundleDeployFile(getFileFromTestClasses("handover-test-bundle.xml"), inputProps, buildListeners);
            fail("Expected RuntimeException because of failed handover");
        } catch (RuntimeException expected) {
        }

        // We still expect the callback to be called twice, as the first handover has a failonerror="false" attribute
        assertEquals(handoverInfoArguments.size(), 2, String.valueOf(handoverInfoArguments));
    }

    public void testHandoverFailonerrorAttribute() throws Exception {
        AntLauncher ant = new AntLauncher(true);

        final List<HandoverInfoArgument> handoverInfoArguments = new ArrayList<HandoverInfoArgument>();
        HandoverTarget handoverTarget = new HandoverTarget() {
            @Override
            public boolean handoverContent(HandoverInfo handoverInfo) {
                HandoverInfoArgument handoverInfoArgument;
                try {
                    handoverInfoArgument = new HandoverInfoArgument(handoverInfo);
                } catch (IOException e) {
                    return false;
                }
                handoverInfoArguments.add(handoverInfoArgument);
                return !handoverInfo.getFilename().equals("prepareDatasource.cli");
            }
        };
        ant.setHandoverTarget(handoverTarget);

        List<BuildListener> buildListeners = createBuildListeners();
        Properties inputProps = createInputProperties("/handover-test-bundle-input.properties");

        ant.executeBundleDeployFile(getFileFromTestClasses("handover-test-bundle.xml"), inputProps, buildListeners);

        assertEquals(handoverInfoArguments.size(), 2, String.valueOf(handoverInfoArguments));
    }

    private List<BuildListener> createBuildListeners() {
        List<BuildListener> buildListeners = new ArrayList<BuildListener>();
        DefaultLogger logger = new DefaultLogger();
        logger.setMessageOutputLevel(Project.MSG_DEBUG);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        buildListeners.add(logger);
        return buildListeners;
    }

    private Properties createInputProperties(String resourcePath) throws IOException {
        return createInputProperties(resourcePath, false);
    }

    private Properties createInputProperties(String resourcePath, boolean dryRun) throws IOException {
        Properties inputProps = new Properties();
        inputProps.setProperty(DeployPropertyNames.DEPLOY_DIR, DEPLOY_DIR.getPath());
        inputProps.setProperty(DeployPropertyNames.DEPLOY_ID, String.valueOf(++this.deploymentId));
        inputProps.setProperty(DeployPropertyNames.DEPLOY_PHASE, DeploymentPhase.INSTALL.name());
        if (dryRun) {
            inputProps.setProperty(DeployPropertyNames.DEPLOY_DRY_RUN, Boolean.TRUE.toString());
        }
        if (resourcePath != null) {
            InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
            try {
                inputProps.load(inputStream);
            } finally {
                inputStream.close();
            }
        }
        return inputProps;
    }

    private Properties readPropsFile(File propFile) throws Exception {
        Properties props = new Properties();
        InputStream inputStream = new FileInputStream(propFile);
        try {
            props.load(inputStream);
        } finally {
            inputStream.close();
        }
        return props;
    }

    private File getFileFromTestClasses(String name) throws Exception {
        File file = new File(ANT_BASEDIR, name);
        assertTrue(file.exists(), "The file doesn't exist: " + file.getAbsolutePath());
        return file;
    }

    private File writeFile(String content, File fileToOverwrite) throws Exception {
        FileOutputStream out = null;

        try {
            fileToOverwrite.getParentFile().mkdirs();
            out = new FileOutputStream(fileToOverwrite);
            out.write(content.getBytes());
            return fileToOverwrite;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File writeFile(String content, File destDir, String fileName) throws Exception {
        File destFile = new File(destDir, fileName);
        return writeFile(content, destFile);
    }

    private File createZip(String[] content, File destDir, String zipName, String[] entryName) throws Exception {
        FileOutputStream stream = null;
        ZipOutputStream out = null;

        try {
            destDir.mkdirs();
            File zipFile = new File(destDir, zipName);
            stream = new FileOutputStream(zipFile);
            out = new ZipOutputStream(stream);

            assertEquals(content.length, entryName.length);
            for (int i = 0; i < content.length; i++) {
                ZipEntry zipAdd = new ZipEntry(entryName[i]);
                zipAdd.setTime(System.currentTimeMillis());
                out.putNextEntry(zipAdd);
                out.write(content[i].getBytes());
            }
            return zipFile;
        } finally {
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }

    private BundleTask findBundleTask(BundleAntProject project) {
        AntXMLContext antParsingContext = (AntXMLContext) project.getReference("ant.parsing.context");
        Vector targets = antParsingContext.getTargets();
        for (Object targetObj : targets) {
            Target target = (Target) targetObj;
            Task[] tasks = target.getTasks();
            for (Task task : tasks) {
                if ("rhq:bundle".equals(task.getTaskName())) {
                    return (BundleTask) preconfigureTask(task);
                }
            }
        }

        return null;
    }

    private static Task preconfigureTask(Task task) {
        if (task instanceof UnknownElement) {
            task.maybeConfigure();
            Task resolvedTask = ((UnknownElement) task).getTask();
            return (resolvedTask != null) ? resolvedTask : task;
        } else {
            return task;
        }
    }

    private static class HandoverInfoArgument {
        final HandoverInfo handoverInfo;
        final File handoverInfoTestContentFile;

        private HandoverInfoArgument(HandoverInfo handoverInfo) throws IOException {
            this.handoverInfo = handoverInfo;
            handoverInfoTestContentFile = File.createTempFile(HandoverInfoArgument.class.getSimpleName() + "-", ".tmp");
            handoverInfoTestContentFile.deleteOnExit();
        }

        @Override
        public String toString() {
            return "HandoverInfoArgument[" + "handoverInfo=" + handoverInfo + ", handoverInfoTestContentFile="
                + handoverInfoTestContentFile + ']';
        }
    }
}
