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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.updater.DeploymentsMetadata;
import org.rhq.core.util.updater.FileHashcodeMap;

/**
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Test
public class AntLauncherTest {
    private static final File DEPLOY_DIR = new File("target/test-ant-bundle").getAbsoluteFile();
    private static final String ANT_BASEDIR = "target/test-classes";

    private int deploymentId;

    @BeforeClass
    public void beforeClass() {
        deploymentId = 0;
    }

    @AfterClass
    public void afterClass() {
        FileUtil.purge(DEPLOY_DIR, true);
    }

    public void testParse() throws Exception {
        // We want to test with an empty deploy dir to ensure nothing gets installed there after a parse
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher();

        BundleAntProject project = ant.parseBundleDeployFile(getBuildXml("test-bundle-v1.xml"));
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

    public void testInstall() throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        // but we do want to add an unrelated file to see that it remains untouched - the install just "goes around" it
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");

        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-bundle-v1-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getBuildXml("test-bundle-v1.xml"), inputProps,
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
        assert unrelatedFile.exists() : "unrelated file was removed during the install";
        assert readPropsFile(new File(DEPLOY_DIR, "subdir/test.properties")).getProperty("junk.listener.port").equals(
            "10000");
        assert readPropsFile(new File(DEPLOY_DIR, "archived-subdir/archived-file-in-subdir.properties")).getProperty(
            "templatized.variable").equals("10000");
    }

    @Test(dependsOnMethods = "testInstall")
    public void testUpgrade() throws Exception {
        // We want to test an upgrade, so do *not* wipe out the deploy dir - our test method @dependsOnMethods testInstall
        // but we do want to add an unrelated file to see that it gets deleted as part of the upgrade
        File unrelatedFile = writeFile("unrelated content", DEPLOY_DIR, "unrelated-file.txt");

        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-bundle-v2-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getBuildXml("test-bundle-v2.xml"), inputProps,
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

    public void testUpgradeNoManageRootDir() throws Exception {
        // We want to test an upgrade, so do *not* wipe out the deploy dir - let's re-invoke testInstall
        // to get us to an initial state of the v1 bundle installed
        testInstall();

        // we still want the unrelated file - we want to see that manageRootDir=false works (unrelated files should not be deleted)
        File unrelatedFile = new File(DEPLOY_DIR, "unrelated-file.txt");
        assert unrelatedFile.exists() : "our initial install test method should have prepared an unmanaged file";

        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-bundle-v2-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getBuildXml("test-bundle-v2-noManageRootDir.xml"),
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

    public void testInstallCompressedZipNoDryRun() throws Exception {
        testInstallCompressedZip(false);
    }

    public void testInstallCompressedZipDryRun() throws Exception {
        testInstallCompressedZip(true);
    }

    private void testInstallCompressedZip(boolean dryRun) throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-bundle-compressed-archives-input.properties", dryRun);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getBuildXml("test-bundle-compressed-archives.xml"),
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

    public void testInstallCompressedZipWithTemplatizedFilesNoDryRun() throws Exception {
        testInstallCompressedZipWithTemplatizedFiles(false);
    }

    public void testInstallCompressedZipWithTemplatizedFilesDryRun() throws Exception {
        testInstallCompressedZipWithTemplatizedFiles(true);
    }

    private void testInstallCompressedZipWithTemplatizedFiles(boolean dryRun) throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-bundle-compressed-archives-input.properties", dryRun);
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(
            getBuildXml("test-bundle-compressed-archives-with-replace.xml"), inputProps, buildListeners);
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

    // this doesn't verify the audit messages getting emitted are correct
    // but it does verify the audit tag getting processed correctly.
    // you have to look at the test logs to see the audit messages
    // TODO: write a ant build listener to listen for this messages, parse them and verify they are correct
    //       this test should then ask the listener at the end if everything was OK and assert false if not
    public void testAuditMessages() throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);

        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-audit-input.properties");
        List<BuildListener> buildListeners = createBuildListeners();

        BundleAntProject project = ant.executeBundleDeployFile(getBuildXml("test-bundle-audit.xml"), inputProps,
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

    private File getBuildXml(String name) throws Exception {
        File file = new File(ANT_BASEDIR, name);
        assert file.exists() : "The test Ant build script doesn't exist: " + file.getAbsolutePath();
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
}
