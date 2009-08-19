/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5.test.sbm;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = {"as5-plugin", "as5-plugin-sbm"})
public class ServiceBindingManagerResourceTest extends AbstractServiceBindingTest {

    protected String getResourceTypeName() {
        return SERVICE_BINDING_MANAGER_SERVICE_NAME;
    }

    @BeforeTest
    @Override
    public void saveConfiguration() {
        super.saveConfiguration();
    }
    
    @Test(dependsOnMethods = "testActiveBindingSetNameChange")
    public void testMetrics() throws Exception {
        super.testMetrics();
    }

    public void testActiveBindingSetNameChange() {
        try {
            System.out.println("Running SBM active binding set name change test...");
            
            Resource sbmResource = getSBMResource();
            Configuration configuration = loadResourceConfiguration(sbmResource);
            
            //we know that there are ports-default, ports-01, ports-02 and ports-03
            //binding sets by default in the server
            PropertySimple activeBindingSetProperty = configuration.getSimple("activeBindingSetName");
            activeBindingSetProperty.setStringValue("ports-01");
            
            Configuration updatedConfiguration = updateResourceConfiguration(sbmResource, configuration);
            
            assertEquals(updatedConfiguration, configuration, "Active binding set update failed.");
            
            //now try to restart and reconfigure the server
            
            AppServerUtils.shutdownServer();
            
            Resource asResource = AppServerUtils.getASResource();
            Configuration newServerConfig = asResource.getPluginConfiguration();
            newServerConfig.put(new PropertySimple("namingURL", "jnp://localhost:1199"));

            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            inventoryManager.updatePluginConfiguration(asResource.getId(), newServerConfig);
            
            System.out.println("Expecting the server to come up with JNP url jnp://localhost:1199");
            
            AppServerUtils.startServer();
            
            Configuration configurationAfterRestart = loadResourceConfiguration(getSBMResource());
            
            assertEquals(configurationAfterRestart, configuration, "The active binding set change didn't survive a server restart.");
        } catch (Exception e) {
            fail("Failed to change binding set name.", e);
        }
    }
    
    public void testInvalidActiveBindingSetNameChangeFailure() {
        try {
            System.out.println("Running SBM invalid active binding set name change test...");
            
            Resource sbmResource = getSBMResource();
            Configuration configuration;
            configuration = loadResourceConfiguration(sbmResource);

            PropertySimple activeBindingSetProperty = configuration.getSimple("activeBindingSetName");
            String origActiveBindingSetName = activeBindingSetProperty.getStringValue();
            //well - technically we should check if there is a binding set with this name, but hey...
            activeBindingSetProperty.setStringValue("*(^$*)(@&_#(*@#@^$&(*^%@)*&#@)#_&_&(#&^#(@%#@^%");
            
            Configuration updatedConfiguration = updateResourceConfiguration(sbmResource, configuration);
            PropertySimple updatedActiveBindingSetProperty = updatedConfiguration.getSimple("activeBindingSetName");
            String updatedActiveBindingSetName = updatedActiveBindingSetProperty.getStringValue();
            
            assertEquals(updatedActiveBindingSetName, origActiveBindingSetName);
        } catch (Exception e) {
            fail("Failed to check that active binding name cannot be set to an invalid name.");
        }        
    }
    
    @Test(dependsOnMethods = "testActiveBindingSetNameChange")
    public void testStandardBindingsChangeTest() {
        try {
            System.out.println("Running SBM standard bindings change test...");
            
            Resource sbmResource = getSBMResource();
            Configuration configuration = loadResourceConfiguration(sbmResource);
            
            PropertyMap jnpURLBinding = findBinding(configuration.getList("standardBindings"), JNP_PORT_BINDING_SERVICE_NAME, JNP_PORT_BINDING_BINDING_NAME);
            
            assertNotNull(jnpURLBinding, "Could not find jnp URL binding in the SBM. This should not happen.");
            
            PropertySimple jnpPortProperty = jnpURLBinding.getSimple("port");
            int jnpPort = jnpPortProperty.getIntegerValue();
            
            jnpPort += 100;
            
            jnpPortProperty.setIntegerValue(jnpPort);
            
            updateResourceConfiguration(sbmResource, configuration);
            
            Configuration updatedConfiguration = loadResourceConfiguration(sbmResource);
            
            assertEquals(updatedConfiguration, configuration, "A change to JNP URL port standard binding wasn't persisted.");
            
            AppServerUtils.shutdownServer();
            
            Resource asResource = AppServerUtils.getASResource();
            Configuration newServerConfig = asResource.getPluginConfiguration();
            
            //this test depends on the active binding set name change test that set the active binding set to
            //ports-01 that adds 100 to all standard bindings. We changed the standard binding above
            //so now we have to add that 100 to the expected port.
            newServerConfig.put(new PropertySimple("namingURL", "jnp://localhost:" + (jnpPort + 100))); 

            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            inventoryManager.updatePluginConfiguration(asResource.getId(), newServerConfig);
            
            System.out.println("Expecting the server to come up with JNP URL jnp://localhost:" + (jnpPort + 100));
            
            AppServerUtils.startServer();
            
            Configuration configurationAfterRestart = loadResourceConfiguration(getSBMResource());
            
            assertEquals(configurationAfterRestart, configuration, "Change to JNP URL port standard binding didn't survive a restart.");
        } catch (Exception e) {
            fail("Failed to test changing the standard bindings.", e);
        }
    }
    
    protected void validateTraitMetricValue(String metricName, String value, Resource resource) {
        if ("activeBindingSetName".equals(metricName) && getSBMResource().equals(resource)) {
            assertEquals(value, "ports-01");
        } else {
            super.validateTraitMetricValue(metricName, value, resource);
        }
    }

    @AfterTest
    @Override
    public void restoreConfiguration() {
        super.restoreConfiguration();
    }
}
