/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.AntXMLContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.bundle.ant.task.BundleTask;
import org.rhq.bundle.ant.type.DeploymentUnitType;
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

/**
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Test
public class AntLauncherTest {
    private static final File DEPLOY_DIR = new File("target/test-ant-bundle").getAbsoluteFile();
    private static final String ANT_BASEDIR = "target/test-classes";
    private static final File REDHAT_RELEASE_FILE = new File("/etc/redhat-release");

    private int deploymentId;

    @BeforeClass
    public void beforeClass() {
        deploymentId = 0;
    }

    @AfterClass
    public void afterClass() {
        FileUtil.purge(DEPLOY_DIR, true);
    }

    public void testParse_legacy() throws Exception {
        testParse(false, "legacy-test-bundle-v1.xml");
    }

    public void testParse() throws Exception {
        testParse(true, "test-bundle-v1.xml");
    }

    private void testParse(boolean validate, String recipeFile) throws Exception {
        // We want to test with an empty deploy dir to ensure nothing gets installed there after a parse
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher(validate);

        BundleAntProject project = ant.parseBundleDeployFile(getFileFromTestClasses(recipeFile), null);
        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 4 : bundleFiles;
        assert bundleFiles.contains("test-v1.properties") : bundleFiles;
        assert bundleFiles.contains("file.zip") : bundleFiles;
        assert bundleFiles.contains("foo-script") : bundleFiles; // from install-system-service
        assert bundleFiles.contains("foo-config") : bundleFiles; // from install-system-service

        assert project.getBundleName().equals("example.com (JBoss EAP 4.3)");
        assert project.getBundleVersion().equals("1.0");
        assert project.getBundleDescription().equals("example.com corporate website hosted on JBoss EAP 4.3");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1 : configDef.getPropertyDefinitions();
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;
        assert propDef.getDefaultValue().equals("8080");
        assert propDef.getDescription().equals("This is where the product will listen for incoming messages");
        assert propDef.isRequired();

        // all we did was parse, nothing should really have been extracted or installed
        assert !DEPLOY_DIR.exists() : "Nothing should have been installed to the deploy dir";
    }

    public void testParseWithNoDestinationComplianceCheck() throws Exception {
        // We want to test with an empty deploy dir to ensure nothing gets installed there after a parse
        FileUtil.purge(DEPLOY_DIR, true);

        //instantiate the launcher in the new validating mode (new as of RHQ 4.9.0)
        AntLauncher ant = new AntLauncher(true);

        try {
            ant.parseBundleDeployFile(getFileFromTestClasses("test-bundle-no-manage-root-dir-nor-compliance.xml"), null);
            Assert.fail("Parsing a bundle with no explicit manageRootDir should have failed.");
        } catch (InvalidBuildFileException e) {
            assert "The deployment unit must specifically declare compliance mode of the destination directory.".equals(
                e.getMessage());
        }

        BundleAntProject project = ant.parseBundleDeployFile(getFileFromTestClasses(
            "test-bundle-with-manage-root-dir.xml"), null);
        assert project != null;
        BundleTask bundleTask = findBundleTask(project);
        assert bundleTask != null;
        assert bundleTask.getDeploymentUnits() != null;
        assert bundleTask.getDeploymentUnits().size() == 1;
        DeploymentUnitType deploymentUnit = bundleTask.getDeploymentUnits().values().iterator().next();
        assert deploymentUnit != null;

        //assert the compatibility with the legacy attribute
        assert "false".equals(deploymentUnit.getManageRootDir());
        assert DestinationComplianceMode.filesAndDirectories == deploymentUnit.getCompliance();

        // all we did was parse, nothing should really have been extracted or installed
        assert !DEPLOY_DIR.exists() : "Nothing should have been installed to the deploy dir";
    }

    public void testInstall_legacy() throws Exception {
        testInstall(false, "legacy-test-bundle-v1.xml");
    }

    @Test(dependsOnMethods = "testUpgrade_legacy")
    public void testInstall() throws Exception {
        testInstall(true, "test-bundle-v1.xml");
    }

    private void testInstall(boolean validate, String recipeFile) throws Exception {

        if (skipNonRHLinux("testInstall"))
            return;

        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        // but we do want to add an unrelated file to see that it goes away - since we have manageRootDir=true
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-v1-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 4 : bundleFiles;
        assert bundleFiles.contains("test-v1.properties") : bundleFiles;
        assert bundleFiles.contains("file.zip") : bundleFiles;
        assert bundleFiles.contains("foo-script") : bundleFiles; // from install-system-service
        assert bundleFiles.contains("foo-config") : bundleFiles; // from install-system-service

        assert project.getBundleName().equals("example.com (JBoss EAP 4.3)");
        assert project.getBundleVersion().equals("1.0");
        assert project.getBundleDescription().equals("example.com corporate website hosted on JBoss EAP 4.3");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1 : configDef.getPropertyDefinitions();
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;
        assert propDef.getDefaultValue().equals("8080");
        assert propDef.getDescription().equals("This is where the product will listen for incoming messages");
        assert propDef.isRequired();

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1 : config.getProperties();
        assert "10000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assert preinstallTargetExecuted.equals("1a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assert postinstallTargetExecuted.equals("1b");

        assert new File(DEPLOY_DIR, "subdir/test.properties").exists() : "missing file";
        assert new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "missing archived bundle file";
        assert new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists() : "missing subdir archive file";
        assert !unrelatedFile.exists() : "unrelated file was not removed during the install";
        assert readPropsFile(new File(DEPLOY_DIR, "subdir/test.properties")).getProperty("junk.listener.port").equals(
            "10000");
        assert readPropsFile(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties")).getProperty(
            "templatized.variable").equals("10000");
    }

    private boolean skipNonRHLinux(String meth) {
        if (!System.getProperty("os.name").equals("Linux") || !REDHAT_RELEASE_FILE.exists()) {
            System.out.println("Skipping " + meth + "() as this only works on Red Hat Linux flavors");
            return true;
        }
        return false;
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

        if (skipNonRHLinux("testUpgrade"))
            return;

        // add an unrelated file to see that it gets deleted as part of the upgrade
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-v2-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 4 : bundleFiles;
        assert bundleFiles.contains("test-v2.properties") : bundleFiles;
        assert bundleFiles.contains("file.zip") : bundleFiles;
        assert bundleFiles.contains("foo-script") : bundleFiles; // from install-system-service
        assert bundleFiles.contains("foo-config") : bundleFiles; // from install-system-service

        assert project.getBundleName().equals("example.com (JBoss EAP 4.3)");
        assert project.getBundleVersion().equals("2.5");
        assert project.getBundleDescription().equals("updated bundle");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1 : configDef.getPropertyDefinitions();
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;
        assert propDef.getDefaultValue().equals("9090");
        assert propDef.getDescription().equals("This is where the product will listen for incoming messages");
        assert propDef.isRequired();

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1;
        assert "20000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assert preinstallTargetExecuted.equals("2a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assert postinstallTargetExecuted.equals("2b");

        assert new File(DEPLOY_DIR, "subdir/test.properties").exists() : "missing file";
        assert new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "missing archived bundle file";
        assert new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists() : "missing subdir archive file";
        assert !unrelatedFile.exists() : "we are managing root dir so unrelated file should be removed during upgrade";
        assert readPropsFile(new File(DEPLOY_DIR, "subdir/test.properties")).getProperty("junk.listener.port").equals(
            "20000");
        assert readPropsFile(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties")).getProperty(
            "templatized.variable").equals("20000");
    }

    public void testUpgradeNoManageRootDir_legacy() throws Exception {
        testUpgradeNoManageRootDir(false, "legacy-test-bundle-v2-noManageRootDir.xml");
    }

    public void testUpgradeNoManageRootDir() throws Exception {
        testUpgradeNoManageRootDir(true, "test-bundle-v2-filesAndDirectories.xml");
    }

    private void testUpgradeNoManageRootDir(boolean validate, String recipeFile) throws Exception {

        if (skipNonRHLinux("testInstall"))
            return;

        // We want to test an upgrade, so do *not* wipe out the deploy dir - let's re-invoke testInstall
        // to get us to an initial state of the v1 bundle installed
        testInstall();

        // we still want the unrelated file - we want to see that manageRootDir=false works (unrelated files should not be deleted)
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");
        assert unrelatedFile.exists() : "our initial install test method should have prepared an unmanaged file";

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-v2-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile),
            inputProps, buildListeners);
        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 4 : bundleFiles;
        assert bundleFiles.contains("test-v2.properties") : bundleFiles;
        assert bundleFiles.contains("file.zip") : bundleFiles;
        assert bundleFiles.contains("foo-script") : bundleFiles; // from install-system-service
        assert bundleFiles.contains("foo-config") : bundleFiles; // from install-system-service

        assert project.getBundleName().equals("example.com (JBoss EAP 4.3)");
        assert project.getBundleVersion().equals("2.5");
        assert project.getBundleDescription().equals("updated bundle");

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1 : configDef.getPropertyDefinitions();
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;
        assert propDef.getDefaultValue().equals("9090");
        assert propDef.getDescription().equals("This is where the product will listen for incoming messages");
        assert propDef.isRequired();

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1;
        assert "20000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assert preinstallTargetExecuted.equals("2a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assert postinstallTargetExecuted.equals("2b");

        assert new File(DEPLOY_DIR, "subdir/test.properties").exists() : "missing file";
        assert new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "missing archived bundle file";
        assert new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists() : "missing subdir archive file";
        assert unrelatedFile.exists() : "we are NOT managing root dir so unrelated file should NOT be removed during upgrade";
        assert readPropsFile(new File(DEPLOY_DIR, "subdir/test.properties")).getProperty("junk.listener.port").equals(
            "20000");
        assert readPropsFile(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties")).getProperty(
            "templatized.variable").equals("20000");
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
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-compressed-archives-input.properties", dryRun);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile),
            inputProps, buildListeners);
        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 1 : bundleFiles;
        assert bundleFiles.contains("file.zip") : bundleFiles;

        assert project.getBundleName().equals("test compressed archive files");
        assert project.getBundleVersion().equals("1.0");
        assert project.getBundleDescription() == null;

        // while we are here, let's see that we have 0 config props
        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 0 : configDef.getPropertyDefinitions();
        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 0 : config.getProperties();

        if (!dryRun) {
            assert new File(DEPLOY_DIR, "file.zip").exists() : "should be here, we told it to stay compressed";
        } else {
            assert !new File(DEPLOY_DIR, "file.zip").exists() : "dry run - should not be here";
        }

        assert !new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "should not have exploded this";
        assert !new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists() : "should not have exploded this";
        assert !new File(DEPLOY_DIR, "archived-subdir").isDirectory() : "should not still have the exploded dir";

        DeploymentsMetadata dm = new DeploymentsMetadata(DEPLOY_DIR);
        if (!dryRun) {
            FileHashcodeMap fhm = dm.getCurrentDeploymentFileHashcodes();
            assert !fhm.containsKey("archived-bundle-file.txt") : "should not have metadata - this is inside the compressed zip";
            assert !fhm.containsKey("archived-subdir/archived-file-in-subdir.properties") : "should not have metadata - this is inside the compressed zip";
            assert fhm.containsKey("file.zip") : "should have metadata for this - we didn't explode it, we just have this compressed file";

            // test that we created the zip OK. Note that our test did not do any file replacing/realization of templates
            final String[] templateVarValue = new String[] { null };
            final Integer[] entries = new Integer[] { 0 };
            ZipUtil.walkZipFile(new File(DEPLOY_DIR, "file.zip"), new ZipUtil.ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (entry.getName().equals("archived-subdir/archived-file-in-subdir.properties")) {
                        Properties props = new Properties();
                        props.load(stream);
                        templateVarValue[0] = props.getProperty("templatized.variable");
                    }
                    if (!entry.isDirectory()) {
                        entries[0] = Integer.valueOf(entries[0].intValue() + 1);
                    }
                    return true;
                }
            });
            assert templateVarValue[0] != null && templateVarValue[0].equals("@@listener.port@@") : templateVarValue[0];
            assert entries[0].intValue() == 2 : entries[0]; // we only counted the file entries
        } else {
            try {
                dm.getCurrentDeploymentFileHashcodes();
                assert false : "this was a dry run, we should not have written our metadata to the filesystem";
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

    private void testInstallCompressedZipWithTemplatizedFiles(boolean dryRun, boolean validate, String recipeFile) throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-bundle-compressed-archives-input.properties", dryRun);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(
            getFileFromTestClasses(recipeFile), inputProps, buildListeners);
        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 1 : bundleFiles;
        assert bundleFiles.contains("file.zip") : bundleFiles;

        assert project.getBundleName().equals("test compressed archive files");
        assert project.getBundleVersion().equals("1.0");
        assert project.getBundleDescription() == null;

        // we have one property that we use to realize our content
        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1 : configDef.getPropertyDefinitions();
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;
        assert propDef.getDefaultValue() == null : "recipe didn't define a default for our property";
        assert propDef.getDescription() == null : "recipe didn't define a description for our property";
        assert propDef.isRequired() : "recipe didn't make the property required, but the default should be required";

        if (!dryRun) {
            assert new File(DEPLOY_DIR, "file.zip").exists() : "should be here, we told it to stay compressed";
        } else {
            assert !new File(DEPLOY_DIR, "file.zip").exists() : "this was a dry run, should not be here";
        }
        assert !new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "should not have exploded this";
        assert !new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties").exists() : "should not have exploded this";
        assert !new File(DEPLOY_DIR, "archived-subdir").isDirectory() : "should not still have the exploded dir";

        DeploymentsMetadata dm = new DeploymentsMetadata(DEPLOY_DIR);
        if (!dryRun) {
            FileHashcodeMap fhm = dm.getCurrentDeploymentFileHashcodes();
            assert !fhm.containsKey("archived-bundle-file.txt") : "should not have metadata - this is inside the compressed zip";
            assert !fhm.containsKey("archived-subdir/archived-file-in-subdir.properties") : "should not have metadata - this is inside the compressed zip";
            assert fhm.containsKey("file.zip") : "should have metadata for this - we didn't explode it, we just have this compressed file";

            // test that the file in the zip is realized
            final String[] templateVarValue = new String[] { null };
            final Integer[] entries = new Integer[] { 0 };
            ZipUtil.walkZipFile(new File(DEPLOY_DIR, "file.zip"), new ZipUtil.ZipEntryVisitor() {
                @Override
                public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
                    if (entry.getName().equals("archived-subdir/archived-file-in-subdir.properties")) {
                        Properties props = new Properties();
                        props.load(stream);
                        templateVarValue[0] = props.getProperty("templatized.variable");
                    }
                    if (!entry.isDirectory()) {
                        entries[0] = Integer.valueOf(entries[0].intValue() + 1);
                    }
                    return true;
                }
            });
            assert templateVarValue[0] != null && templateVarValue[0].equals("12345") : templateVarValue[0];
            assert entries[0].intValue() == 2 : entries[0]; // we only counted the file entries
        } else {
            try {
                dm.getCurrentDeploymentFileHashcodes();
                assert false : "this was a dry run, we should not have written our metadata to the filesystem";
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
    //       this test should then ask the listener at the end if everything was OK and assert false if not
    private void testAuditMessages(boolean validate, String recipeFile) throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher(validate);
        Properties inputProps = createInputProperties("/test-audit-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
            buildListeners);
        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 1 : bundleFiles;
        assert bundleFiles.contains("test-audit.properties") : bundleFiles;

        // sanity check - make sure our recipe defined this property
        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1 : configDef.getPropertyDefinitions();
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;

        // make sure our test infrastruction setup the input properties correctly
        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1 : config.getProperties();
        assert "777".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assert preinstallTargetExecuted.equals("1a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assert postinstallTargetExecuted.equals("1b");

        assert new File(DEPLOY_DIR, "test-audit.properties").exists() : "missing file";
        assert readPropsFile(new File(DEPLOY_DIR, "test-audit.properties")).getProperty("my.listener.port").equals(
            "777");
    }

    public void testSubdirectoriesInRecipe_legacy() throws Exception {
        testSubdirectoriesInRecipe(false, "legacy-test-bundle-subdir.xml");
    }

    public void testSubdirectoriesInRecipe() throws Exception {
        testSubdirectoriesInRecipe(true, "test-bundle-subdir.xml");
    }

    private void testSubdirectoriesInRecipe(boolean validate, String origRecipeFile) throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

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
            assert project != null;
            Set<String> bundleFiles = project.getBundleFileNames();
            assert bundleFiles != null;
            assert bundleFiles.size() == 7 : bundleFiles;
            assert bundleFiles.contains("subdir/test0.txt") : bundleFiles;
            assert bundleFiles.contains("subdir/test1.txt") : bundleFiles;
            assert bundleFiles.contains("subdir/test2.txt") : bundleFiles;
            assert bundleFiles.contains("subdir/test.zip") : bundleFiles;
            assert bundleFiles.contains("subdir/test-explode.zip") : bundleFiles;
            assert bundleFiles.contains("subdir/test-replace.zip") : bundleFiles;
            assert bundleFiles.contains("subdir/test-replace2.zip") : bundleFiles;

            assert new File(DEPLOY_DIR, "subdir/test0.txt").exists() : "missing raw file from default destination location";
            assert new File(DEPLOY_DIR, "another/foo.txt").exists() : "missing raw file from the destinationFile";
            assert new File(DEPLOY_DIR, "second.dir/test2.txt").exists() : "missing raw file from the destinationDir";
            assert !new File(DEPLOY_DIR, "subdir/test1.txt").exists() : "should not be here because destinationFile was specified";
            assert !new File(DEPLOY_DIR, "subdir/test2.txt").exists() : "should not be here because destinationFile was specified";
            assert new File(DEPLOY_DIR, "subdir/test.zip").exists() : "missing unexploded zip file";
            assert new File(DEPLOY_DIR, "subdir/test-replace.zip").exists() : "missing unexploded zip file";
            assert new File(DEPLOY_DIR, "second.dir/test-replace2.zip").exists() : "missing unexploded second zip file";
            assert !new File(DEPLOY_DIR, "subdir/test-explode.zip").exists() : "should have been exploded";

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
            assert templateVarValue[0] != null && templateVarValue[0].equals("alpha-omega") : templateVarValue[0];

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
            assert templateVarValue2[0] != null && templateVarValue2[0].equals("alpha-omega") : templateVarValue2[0];

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

    public void testNotDeployedFiles() throws Exception {
        testNotDeployedFiles(getFileFromTestClasses("ant-properties/deploy.xml"));
    }

    public void testAntPropertiesUsedForTokenReplacement() throws Exception {
        testNotDeployedFiles();

        Properties props = readPropsFile(new File(DEPLOY_DIR, "deployed.file"));

        assert "user provided value".equals(props.getProperty("user.provided")) : "user.provided";
        assert "bundle provided value".equals(props.getProperty("bundle.provided")) : "bundle.provided";
        assert "a".equals(props.get("a.from.properties.file")) : "a.from.properties.file";
        assert "b".equals(props.get("b.from.properties.file")) : "b.from.properties.file";
    }

    public void testAntPropertiesLoadFromAbsolutePath() throws Exception {
        File tempDir = FileUtil.createTempDirectory("ant-launcher-test", null, null);

        try {
            //prepare the test bundle.. update the recipe with an absolute path to a properties file.
            File deployXml = new File(tempDir, "deploy.xml");

            FileUtil.copyFile(getFileFromTestClasses("ant-properties/deploy.xml"), deployXml);

            //copy the other file from the bundle, too, into the correct location
            FileUtil.copyFile(getFileFromTestClasses("ant-properties/deployed.file"), new File(tempDir,
                "deployed.file"));

            File absoluteLocation = new File(tempDir, "absolute-location");
            assert absoluteLocation.mkdir() : "Failed to create dir under temp";

            String deployXmlContents = StreamUtil.slurp(new InputStreamReader(new FileInputStream(deployXml)));

            File absolutePropertiesLocation = new File(absoluteLocation, "in-bundle.properties");
            FileUtil
                .copyFile(getFileFromTestClasses("ant-properties/in-bundle.properties"), absolutePropertiesLocation);

            deployXmlContents = deployXmlContents.replace("<property file=\"in-bundle.properties\"/>",
                "<property file=\"" + absolutePropertiesLocation.getAbsolutePath() + "\"/>");

            FileUtil.writeFile(new ByteArrayInputStream(deployXmlContents.getBytes()), deployXml);

            //k, now the test itself...
            testNotDeployedFiles(deployXml);
        } finally {
            FileUtil.purge(tempDir, true);
        }
    }

    private void testNotDeployedFiles(File deployXml) throws Exception {
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher(true);
        Properties inputProps = createInputProperties(null);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(deployXml, inputProps,
            buildListeners);

        assert project != null;
        Set<String> bundleFiles = project.getBundleFileNames();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.contains("deployed.file") : bundleFiles;
        assert bundleFiles.contains("in-bundle.properties") : bundleFiles;

        assert new File(DEPLOY_DIR, "deployed.file").exists() : "deployed.file missing";
        assert !new File(DEPLOY_DIR, "in-bundle.properties")
            .exists() : "in-bundle.properties deployed but shouldn't have";
    }

    private void testUrlFilesAndArchives(boolean validate, String recipeFile) throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        // we need to create our own directory structure so we can use file: URLs
        File tmpUrlLocation = FileUtil.createTempDirectory("anttest", ".url", null);
        Set<File> downloadedFiles = null;

        try {
            File subdir = new File(tmpUrlLocation, "subdir"); // must match the name in the recipe
            subdir.mkdirs();
            writeFile("file0", subdir, "test0.txt"); // filename must match recipe
            writeFile("file1", subdir, "test1.txt"); // filename must match recipe
            writeFile("X=@@X@@\n", subdir, "test2.txt"); // filename must match recipe
            createZip(new String[] { "one", "two" }, subdir, "test.zip", new String[] { "one.txt", "two.txt" });
            createZip(new String[] { "3", "4" }, subdir, "test-explode.zip", new String[] { "three.txt", "four.txt" });
            createZip(new String[] { "X=@@X@@\n" }, subdir, "test-replace.zip", new String[] { "template.txt" }); // will be exploded then recompressed

            AntLauncher ant = new AntLauncher(validate);
            Properties inputProps = createInputProperties("/test-bundle-url-input.properties");
            inputProps.setProperty("rhq.test.url.dir", tmpUrlLocation.toURI().toURL().toString()); // we use this so our recipe can use URLs
            List<BuildListener> buildListeners = createBuildListeners();

            BundleAntProject project = ant.executeBundleDeployFile(getFileFromTestClasses(recipeFile), inputProps,
                buildListeners);
            assert project != null;

            Set<String> bundleFiles = project.getBundleFileNames();
            assert bundleFiles != null;
            assert bundleFiles.size() == 0 : "we don't have any bundle files - only downloaded files from URLs: "
                + bundleFiles;

            downloadedFiles = project.getDownloadedFiles();
            assert downloadedFiles != null;
            assert downloadedFiles.size() == 6 : downloadedFiles;
            ArrayList<String> expectedDownloadedFileNames = new ArrayList<String>();
            // remember, we store url downloaded files under the names of their destination file/dir, not source location
            expectedDownloadedFileNames.add("test0.txt");
            expectedDownloadedFileNames.add("foo.txt");
            expectedDownloadedFileNames.add("test2.txt");
            expectedDownloadedFileNames.add("test.zip");
            expectedDownloadedFileNames.add("test-explode.zip");
            expectedDownloadedFileNames.add("test-replace.zip");
            for (File downloadedFile : downloadedFiles) {
                assert expectedDownloadedFileNames.contains(downloadedFile.getName()) : "We downloaded a file but its not in the project's list: "
                    + downloadedFile;
            }

            assert new File(DEPLOY_DIR, "test0.txt").exists() : "missing raw file from default destination location";
            assert new File(DEPLOY_DIR, "another/foo.txt").exists() : "missing raw file from the destinationFile";
            assert new File(DEPLOY_DIR, "second.dir/test2.txt").exists() : "missing raw file from the destinationDir";
            assert !new File(DEPLOY_DIR, "test1.txt").exists() : "should not be here because destinationFile was specified";
            assert !new File(DEPLOY_DIR, "test2.txt").exists() : "should not be here because destinationFile was specified";
            assert new File(DEPLOY_DIR, "test.zip").exists() : "missing unexploded zip file";
            assert new File(DEPLOY_DIR, "test-replace.zip").exists() : "missing unexploded zip file";
            assert !new File(DEPLOY_DIR, "test-explode.zip").exists() : "should have been exploded";

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
            assert templateVarValue[0] != null && templateVarValue[0].equals("9876") : templateVarValue[0];

            // test that our raw file was realized
            File realizedFile = new File(DEPLOY_DIR, "second.dir/test2.txt");
            Properties props = new Properties();
            FileInputStream inStream = new FileInputStream(realizedFile);
            try {
                props.load(inStream);
                assert props.getProperty("X", "<unset>").equals("9876");
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
        assert file.exists() : "The file doesn't exist: " + file.getAbsolutePath();
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

            assert content.length == entryName.length;
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
}
