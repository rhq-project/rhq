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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

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
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class AntBundlePluginComponentTest {    
    private AntBundlePluginComponent plugin;
    private File tmpDir;

    @BeforeClass
    public void initTempDir() throws Exception {
        this.tmpDir = new File("target/antbundletest");
        FileUtil.purge(this.tmpDir, true);
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        if (!this.tmpDir.mkdirs()) {
            throw new IllegalStateException("Failed to create temp dir '" + this.tmpDir + "'.");
        }
        this.plugin = new AntBundlePluginComponent();
        ResourceType type = new ResourceType("antBundleTestType", "antBundleTestPlugin", ResourceCategory.SERVER, null);
        Resource resource = new Resource("antBundleTestKey", "antBundleTestName", type);
        @SuppressWarnings("unchecked")
        ResourceContext<?> context = new ResourceContext(resource, null, null,
            SystemInfoFactory.createJavaSystemInfo(), tmpDir, null, "antBundleTestPC", null, null, null, null, null);
        this.plugin.start(context);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanTmpDir() {
        FileUtil.purge(this.tmpDir, true);
    }

    /**
     * Test deployment of an RHQ bundle recipe.
     */
    @Test
    public void testAntBundle() throws Exception {
        ResourceType resourceType = new ResourceType("testSimpleBundle", "plugin", ResourceCategory.SERVER, null);
        BundleType bundleType = new BundleType("testSimpleBundle", resourceType);
        Repo repo = new Repo("testSimpleBundle");
        PackageType packageType = new PackageType("testSimpleBundle", resourceType);
        Bundle bundle = new Bundle("testSimpleBundle", bundleType, repo, packageType);
        BundleVersion bundleVersion = new BundleVersion("testSimpleBundle", "1.0", bundle,
            getRecipeFromFile("test-bundle.xml"));
        BundleDestination destination = new BundleDestination(bundle, "testSimpleBundle", new ResourceGroup(
            "testSimpleBundle"), "/tmp/rhq-testAntBundle");

        Configuration config = new Configuration();
        config.put(new PropertySimple("custom.prop1", "ABC123"));

        BundleDeployment deployment = new BundleDeployment();
        deployment.setName("test bundle deployment name");
        deployment.setBundleVersion(bundleVersion);
        deployment.setConfiguration(config);
        deployment.setDestination(destination);

        // create test file
        File file1 = new File(tmpDir, "test.properties");
        Properties props = new Properties();
        props.setProperty("custom.prop1", "@@custom.prop1@@");
        FileOutputStream outputStream = new FileOutputStream(file1);
        props.store(outputStream, "blah");
        outputStream.close();

        BundleDeployRequest request = new BundleDeployRequest();
        request.setBundleFilesLocation(tmpDir);
        request.setResourceDeployment(new BundleResourceDeployment(deployment, null));
        request.setBundleManagerProvider(new MockBundleManagerProvider());

        BundleDeployResult results = plugin.deployBundle(request);

        assertResultsSuccess(results);
        // TODO: Check that custom.prop1 got replaced.
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
    }
}
