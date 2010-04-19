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
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.file.FileUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

@Test
public class AntLauncherTest {
    private static final File DEPLOY_DIR = new File("target/test-ant-bundle").getAbsoluteFile();

    private File logFile;

    @BeforeMethod
    public void beforeMethod() {
        logFile = new File("target/test-ant-log.txt");
        if (logFile.exists()) {
            if (!logFile.delete()) {
                System.out.println("Failed to delete log file [" + this.logFile + "] prior to executing test method.");
            }
        }
        return;
    }

    public void testParse() throws Exception {
        AntLauncher ant = new AntLauncher();
        
        Properties inputProps = createInputProperties("/test-bundle-v1-input.properties");
        BundleAntProject project = ant.startAnt(getBuildXml("test-bundle-v1.xml"), "unnecessary-target",
                null, inputProps, this.logFile, true, false);
        assert project != null;
        /*Map<String, String> bundleFiles = project.getBundleFiles();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.get("f").equals("test-v2.properties") : bundleFiles;
        assert bundleFiles.get("pkg").equals("package.zip") : bundleFiles;*/

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1;
        assert configDef.getPropertyDefinitionSimple("listener.port") != null;
    }

    public void testInstall() throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(DEPLOY_DIR, true);
        
        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-bundle-v1-input.properties");
        BundleAntProject project = ant.startAnt(getBuildXml("test-bundle-v1.xml"), "deploy", null, inputProps,
                this.logFile, true, true);
        /*Map<String, String> bundleFiles = project.getBundleFiles();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.get("f").equals("test-v2.properties") : bundleFiles;
        assert bundleFiles.get("pkg").equals("package.zip") : bundleFiles;*/

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1;
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;

        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1;
        assert "10000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();
    }

    @Test(dependsOnMethods = "testInstall")
    public void testUpgrade() throws Exception {
        // We want to test an upgrade, so do *not* wipe out the deploy dir.

        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties("/test-bundle-v2-input.properties");
        BundleAntProject project = ant.startAnt(getBuildXml("test-bundle-v2.xml"), "deploy", null, inputProps,
                this.logFile, true, true);
        /*Map<String, String> bundleFiles = project.getBundleFiles();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.get("f").equals("test-v2.properties") : bundleFiles;
        assert bundleFiles.get("pkg").equals("package.zip") : bundleFiles;*/

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1;
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;

        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1;
        assert "20000".equals(config.getSimpleValue("listener.port", null)) : config.getProperties();
    }

    private Properties createInputProperties(String resourcePath) throws IOException {
        Properties inputProps = new Properties();
        inputProps.setProperty(AntLauncher.DEPLOY_DIR_PROP, DEPLOY_DIR.getPath());
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
}
