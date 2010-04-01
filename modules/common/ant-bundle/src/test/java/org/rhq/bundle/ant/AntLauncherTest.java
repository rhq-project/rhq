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
import java.util.Map;

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
                System.out.println("Failed to clean up test - log file did not delete");
            }
        }
        return;
    }

    public void testSimpleParseTest() throws Exception {
        AntLauncher ant = new AntLauncher();
        BundleAntProject project = ant.startAnt(getBuildXml("simple-build.xml"), "unnecessary-target", null, null,
            logFile, true, false);
        assert project != null;
        Map<String, String> bundleFiles = project.getBundleFiles();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.get("f").equals("file.txt") : bundleFiles;
        assert bundleFiles.get("pkg").equals("package.zip") : bundleFiles;

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 2;
        assert configDef.getPropertyDefinitionSimple("custom.prop1") != null;
        assert configDef.getPropertyDefinitionSimple("custom.prop2") != null;
    }

    public void testSimpleExecTest() throws Exception {
        AntLauncher ant = new AntLauncher();
        BundleAntProject project = ant.startAnt(getBuildXml("simple-build.xml"), "first-target", null, null, logFile,
            true, true);
        Map<String, String> bundleFiles = project.getBundleFiles();
        assert bundleFiles != null;
        assert bundleFiles.size() == 2 : bundleFiles;
        assert bundleFiles.get("f").equals("file.txt") : bundleFiles;
        assert bundleFiles.get("pkg").equals("package.zip") : bundleFiles;

        ConfigurationDefinition configDef = project.getConfigurationDefinition();
        assert configDef.getPropertyDefinitions().size() == 2;
        assert configDef.getPropertyDefinitionSimple("custom.prop1") != null;
        assert configDef.getPropertyDefinitionSimple("custom.prop2") != null;
    }

    private File getBuildXml(String name) throws Exception {
        File file = new File("target/test-classes", name);
        assert file.exists() : "the test ant build script doesn't exist: " + file.getAbsolutePath();
        return file;
    }
}
