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

    public void testSimpleParseTest() throws Exception {
        AntLauncher ant = new AntLauncher();
        
        Properties inputProps = createInputProperties();
        BundleAntProject project = ant.startAnt(getBuildXml("simple-build.xml"), "unnecessary-target", null, inputProps,
            this.logFile, true, false);
        assert project != null;
        /*Map<String, String> bundleFiles = project.getBundleFiles();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.get("f").equals("file.properties") : bundleFiles;
        assert bundleFiles.get("pkg").equals("package.zip") : bundleFiles;*/

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1;
        assert configDef.getPropertyDefinitionSimple("listener.port") != null;
    }

    public void testSimpleExecTest() throws Exception {
        // We want to test a fresh install, so make sure the deploy dir doesn't pre-exist.
        FileUtil.purge(new File("/home/ips/jboss"), true);
        
        AntLauncher ant = new AntLauncher();
        Properties inputProps = createInputProperties();
        BundleAntProject project = ant.startAnt(getBuildXml("simple-build.xml"), "first-target", null, inputProps,
                this.logFile, true, true);
        /*Map<String, String> bundleFiles = project.getBundleFiles();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.get("f").equals("file.properties") : bundleFiles;
        assert bundleFiles.get("pkg").equals("package.zip") : bundleFiles;*/

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 1;
        PropertyDefinitionSimple propDef = configDef.getPropertyDefinitionSimple("listener.port");
        assert propDef != null;
        assert propDef.getType() == PropertySimpleType.INTEGER;

        Configuration config = project.getConfiguration();
        assert config.getProperties().size() == 1;
        assert "7080".equals(config.getSimpleValue("listener.port", null));
    }

    private Properties createInputProperties() throws IOException {
        Properties inputProps = new Properties();
        InputStream inputStream = this.getClass().getResourceAsStream("/input.properties");
        try {
            inputProps.load(inputStream);
        } finally {
            inputStream.close();
        }
        return inputProps;
    }

    private File getBuildXml(String name) throws Exception {
        File file = new File("target/test-classes", name);
        assert file.exists() : "the test ant build script doesn't exist: " + file.getAbsolutePath();
        return file;
    }
}
