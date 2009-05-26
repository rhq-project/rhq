/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.jboss.on.common.jbossas.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jboss.jbossnetwork.product.jbpm.handlers.test.MockContentContext;
import com.jboss.jbossnetwork.product.jbpm.handlers.test.MockContentServices;
import com.jboss.jbossnetwork.product.jbpm.handlers.test.MockControlActionFacade;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.jboss.on.common.jbossas.JBPMWorkflowManager;
import org.jboss.on.common.jbossas.JBossASPaths;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.operation.OperationServicesResultCode;

/**
 * @author Jason Dobies
 */
public class JBPMWorkflowManagerTest {

    private static final boolean ENABLE_TESTS = true;
    private static final String JBOSS_HOME_DIR_CONFIG_PROP = "jbossHomeDir";
    private static final String CONFIGURATION_PATH_CONFIG_PROP = "configurationPath";

    private ResourcePackageDetails packageDetails;
    private Configuration jbossPluginConfiguration;

    @BeforeMethod
    public void initPackageDetails() throws Exception {
        PackageDetailsKey key = new PackageDetailsKey("TestPackage", "1.0", "AS Patch", "noarch");
        packageDetails = new ResourcePackageDetails(key);

        Configuration extraProperties = new Configuration();
        extraProperties.put(new PropertySimple("instructionCompatibilityVersion", "1.4"));
        packageDetails.setExtraProperties(extraProperties);

        InputStream processStream = this.getClass().getClassLoader().getResourceAsStream(
            "handlers/JBPMWorkflowManagerTest-1.xml");
        byte[] process = new byte[processStream.available()];
        processStream.read(process);
        packageDetails.setMetadata(process);

        packageDetails.setFileName("test-patch.zip");
        packageDetails.setMD5("0b3b49b059ade16690dd206d4be505c7");
    }

    @BeforeMethod
    public void initTestJBossServer() throws Exception {
        jbossPluginConfiguration = new Configuration();

        // Copy the test jboss server directory structure over to target to be worked on
        File jbossServerZip = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator
            + "jboss-server.zip");
        File targetDir = new File("target");
        File jbossTestServerDir = new File("target" + File.separator + "jboss-server");
        File jbossServerDir = new File(jbossTestServerDir.getAbsolutePath() + File.separator + "server"
            + File.separator + "default");

        unzip(jbossServerZip, targetDir);

        jbossPluginConfiguration.put(new PropertySimple(JBOSS_HOME_DIR_CONFIG_PROP, jbossTestServerDir
            .getAbsolutePath()));
        jbossPluginConfiguration.put(new PropertySimple(CONFIGURATION_PATH_CONFIG_PROP, jbossServerDir
            .getAbsolutePath()));
    }

    @Test(enabled = ENABLE_TESTS)
    public void successfulWorkflow() throws Exception {
        // Setup
        MockControlActionFacade mockFacade = new MockControlActionFacade();
        mockFacade.setResultCode(OperationServicesResultCode.SUCCESS);

        MockContentContext mockContentContext = new MockContentContext();
        MockContentServices mockContentServices = (MockContentServices) mockContentContext.getContentServices();
        mockContentServices.setFilename("test-patch.zip");

        // Test
        JBPMWorkflowManager manager = new JBPMWorkflowManager(mockContentContext, mockFacade,
            getJBossPaths(jbossPluginConfiguration));
        DeployIndividualPackageResponse response = manager.run(packageDetails);

        assert response.getResult() == ContentResponseResult.SUCCESS : "Incorrect response status. Expected: Success, Found: "
            + response.getResult();

        List<DeployPackageStep> steps = response.getDeploymentSteps();

        assert steps != null : "Null steps found in response";
        assert steps.size() == 8 : "Incorrect number of steps. Expected: 8, Found: " + steps.size();

        for (DeployPackageStep step : steps) {
            assert step.getStepResult() == ContentResponseResult.SUCCESS : "Step failed: " + step;
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void failedWorkflow() throws Exception {
        // Setup
        MockControlActionFacade mockFacade = new MockControlActionFacade();
        mockFacade.setResultCode(OperationServicesResultCode.SUCCESS);

        MockContentContext mockContentContext = new MockContentContext();
        MockContentServices mockContentServices = (MockContentServices) mockContentContext.getContentServices();
        mockContentServices.setFilename("nonexistent-file");

        // Test
        JBPMWorkflowManager manager = new JBPMWorkflowManager(mockContentContext, mockFacade,
            getJBossPaths(jbossPluginConfiguration));
        DeployIndividualPackageResponse response = manager.run(packageDetails);

        assert response.getResult() == ContentResponseResult.FAILURE : "Incorrect response status. Expected: Failed, Found: "
            + response.getResult();

        List<DeployPackageStep> steps = response.getDeploymentSteps();

        assert steps != null : "Null steps found in response";
        assert steps.size() == 8 : "Incorrect number of steps. Expected: 8, Found: " + steps.size();

        int counter = 0;
        for (DeployPackageStep step : steps) {
            if (counter++ == 0) {
                assert step.getStepResult() == ContentResponseResult.FAILURE : "First step was not reported as failed. Expected: Failed, Found: "
                    + step.getStepResult();
            } else {
                assert step.getStepResult() == ContentResponseResult.NOT_PERFORMED : "Subsequent steps not reported as not executed. Step number: "
                    + counter + ", Expected: Not Performed, Found: " + step.getStepResult();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void successfulTranslateSteps() throws Exception {
        // Setup
        MockControlActionFacade mockFacade = new MockControlActionFacade();
        mockFacade.setResultCode(OperationServicesResultCode.SUCCESS);

        MockContentContext mockContentContext = new MockContentContext();
        MockContentServices mockContentServices = (MockContentServices) mockContentContext.getContentServices();
        mockContentServices.setFilename("test-patch.zip");

        // Test
        JBPMWorkflowManager manager = new JBPMWorkflowManager(mockContentContext, mockFacade,
            getJBossPaths(jbossPluginConfiguration));
        List<DeployPackageStep> steps = manager.translateSteps(packageDetails);

        assert steps != null : "Null steps received from call to translate steps";
        assert steps.size() == 8 : "Incorrect number of steps translated. Expected: 8, Found: " + steps.size();

        for (DeployPackageStep step : steps) {
            assert step.getDescription() != null : "Null description for step: " + step;
            assert step.getStepResult() == ContentResponseResult.NOT_PERFORMED : "Incorrect step result for step: "
                + step;
        }
    }

    private void unzip(File zipFile, File toDir) throws Exception {
        FileInputStream fis = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry e;

        while ((e = zis.getNextEntry()) != null) {
            String entryFileName = e.getName();
            File entryFile = new File(toDir + File.separator + entryFileName);

            if (e.isDirectory()) {
                entryFile.mkdir();
            } else {
                FileOutputStream fos = new FileOutputStream(entryFile);
                BufferedOutputStream buf = new BufferedOutputStream(fos);

                byte[] data = new byte[4096];
                int len;
                while ((len = zis.read(data)) > 0) {
                    buf.write(data, 0, len);
                }

                buf.flush();
                fos.close();
            }
        }
    }

    private JBossASPaths getJBossPaths(Configuration jbossPluginConfiguration) {
        JBossASPaths paths = new JBossASPaths();

        String homeDir = jbossPluginConfiguration.getSimpleValue(JBOSS_HOME_DIR_CONFIG_PROP, null);
        String serverDir = jbossPluginConfiguration.getSimpleValue(CONFIGURATION_PATH_CONFIG_PROP, null);

        paths.setHomeDir(homeDir);
        paths.setServerDir(serverDir);

        return paths;
    }
}