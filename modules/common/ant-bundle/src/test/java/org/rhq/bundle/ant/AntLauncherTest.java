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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.file.FileUtil;

/**
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Test
public class AntLauncherTest {
    private static final File DEPLOY_DIR = new File("target/test-ant-bundle").getAbsoluteFile();

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

        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1 : config.getProperties();
        assert "10000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assert preinstallTargetExecuted.equals("1a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assert postinstallTargetExecuted.equals("1b");

        assert new File(DEPLOY_DIR, "subdir/test.properties").exists() : "missing file";
        assert new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "missing archived bundle file";
        assert unrelatedFile.exists() : "unrelated file was removed during the install";
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

        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1;
        assert "20000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assert preinstallTargetExecuted.equals("2a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assert postinstallTargetExecuted.equals("2b");

        assert new File(DEPLOY_DIR, "subdir/test.properties").exists() : "missing file";
        assert new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "missing archived bundle file";
        assert !unrelatedFile.exists() : "we are managing root dir so unrelated file should be removed during upgrade";
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

        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1;
        assert "20000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();

        String preinstallTargetExecuted = (String) project.getProperties().get("preinstallTargetExecuted");
        assert preinstallTargetExecuted.equals("2a");
        String postinstallTargetExecuted = (String) project.getProperties().get("postinstallTargetExecuted");
        assert postinstallTargetExecuted.equals("2b");

        assert new File(DEPLOY_DIR, "subdir/test.properties").exists() : "missing file";
        assert new File(DEPLOY_DIR, "archived-bundle-file.txt").exists() : "missing archived bundle file";
        assert unrelatedFile.exists() : "we are NOT managing root dir so unrelated file should NOT be removed during upgrade";
    }

    private Properties createInputProperties(String resourcePath) throws IOException {
        Properties inputProps = new Properties();
        inputProps.setProperty(DeployPropertyNames.DEPLOY_DIR, DEPLOY_DIR.getPath());
        inputProps.setProperty(DeployPropertyNames.DEPLOY_ID, "100");
        inputProps.setProperty(DeployPropertyNames.DEPLOY_PHASE, DeploymentPhase.INSTALL.name());
        InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
        try {
            inputProps.load(inputStream);
        } finally {
            inputStream.close();
        }
        return inputProps;
    }

    private File getBuildXml(String name) throws Exception {
        File file = new File("target/test-classes", name);
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
}
