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
package org.rhq.plugins.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class AntBundlePluginComponentTest {

    private AntBundlePluginComponent plugin;
    private File tmpDir;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        tmpDir = new File("target/antbundletest");
        tmpDir.mkdirs();
        plugin = new AntBundlePluginComponent();
        ResourceType type = new ResourceType("antBundleTestType", "antBundleTestPlugin", ResourceCategory.SERVER, null);
        Resource resource = new Resource("antBundleTestKey", "antBundleTestName", type);
        @SuppressWarnings("unchecked")
        ResourceContext<?> context = new ResourceContext(resource, null, null,
            SystemInfoFactory.createJavaSystemInfo(), tmpDir, null, "antBundleTestPC", null, null, null, null, null);
        plugin.start(context);
    }

    @BeforeClass
    @AfterMethod
    public void cleanTmpDir() {
        if (tmpDir != null) {
            File[] files = tmpDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    public void testSimpleBundle() throws Exception {
        ResourceType resourceType = new ResourceType("testSimpleBundle", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle", resourceType);
        Repo repo = new Repo("testSimpleBundle");
        PackageType packageType = new PackageType("testSimpleBundle", resourceType);
        Bundle bundle = new Bundle("testSimpleBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testSimpleBundle", "1.0", bundle,
            getRecipeFromFile("simple-build.xml"));

        BundleDeployDefinition deployDef = new BundleDeployDefinition();
        deployDef.setBundleVersion(bundleVersion);
        deployDef.setConfiguration(null);

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(tmpDir);
        request.setResourceDeployment(new BundleResourceDeployment(deployDef, null));

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // our ant script wrote some output that we should verify to make sure we ran it
        File outputFile = new File(tmpDir, "output.1");
        String output = new String(StreamUtil.slurp(new FileInputStream(outputFile)));
        assert output.equals("HELLO WORLD") : output;

    }

    public void testAntBundle() throws Exception {
        ResourceType resourceType = new ResourceType("testSimpleBundle", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle", resourceType);
        Repo repo = new Repo("testSimpleBundle");
        PackageType packageType = new PackageType("testSimpleBundle", resourceType);
        Bundle bundle = new Bundle("testSimpleBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testSimpleBundle", "1.0", bundle,
            getRecipeFromFile("test-build.xml"));

        Configuration config = new Configuration();
        config.put(new PropertySimple("custom.prop1", "custom property 1"));
        config.put(new PropertySimple("custom.prop2", "custom property 2"));

        BundleDeployDefinition deployDef = new BundleDeployDefinition();
        deployDef.setBundleVersion(bundleVersion);
        deployDef.setConfiguration(config);

        File file1 = new File(tmpDir, "file.txt");
        File file2 = new File(tmpDir, "package.zip");
        assert file1.createNewFile() : "could not create our mock bundle file";
        assert file2.createNewFile() : "could not create our mock bundle file";

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(tmpDir);
        request.setResourceDeployment(new BundleResourceDeployment(deployDef, null));

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);

        // our ant script wrote some output that we should verify to make sure we ran it
        File outputFile = new File(tmpDir, "output.2");
        Properties props = new Properties();
        props.load(new FileInputStream(outputFile));
        assert props.getProperty("prop1").equals("custom property 1") : props;
        assert props.getProperty("prop2").equals("custom property 2") : props;
        assert props.getProperty("f.exists").equals("true") : props;
        assert props.getProperty("pkg.exists").equals("true") : props;
        assert props.getProperty("hostname").equals(SystemInfoFactory.createSystemInfo().getHostname()) : props;
        String javaIoTmpDir = System.getProperty("java.io.tmpdir");
        String val = props.getProperty("tmpdir");
        assert val.equals(javaIoTmpDir) : props;
    }

    private void assertResultsSuccess(BundleDeployResult results) {
        if (results.getErrorMessage() != null) {
            assert false : "Failed to process bundle: [" + results.getErrorMessage() + "]";
        }
    }

    private String getRecipeFromFile(String filename) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
        byte[] contents = StreamUtil.slurp(stream);
        return new String(contents);
    }
}
