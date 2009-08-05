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

import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.plugins.jbossas5.test.AbstractResourceTest;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = {"as5-plugin", "as5-plugin-sbm"})
public class ServiceBindingManagerResourceTest extends AbstractResourceTest {

    private Configuration originalSBMConfiguration;
    private Configuration originalPluginConfiguration;
    
    protected String getResourceTypeName() {
        return "Service Binding Manager";
    }

    @BeforeTest
    public void rememberSBMConfiguration() {
        try {
        System.out.println("Saving the original SBM configuration before the SBM tests.");
        originalSBMConfiguration = loadResourceConfiguration(getSBMResource());
        originalPluginConfiguration = AppServerUtils.getASResource().getPluginConfiguration().clone();
        } catch (Exception e) {
            fail("Could not save the SBM configuration before SBM tests.", e);
        }
    }
    
    public void testMetrics() throws Exception {
        super.testMetrics();
    }

    public void testActiveBindingSetNameChange() {
        try {
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
            
            AppServerUtils.startServer();
            
            Configuration configurationAfterRestart = loadResourceConfiguration(getSBMResource());
            
            assertEquals(configurationAfterRestart, configuration, "The active binding set change didn't survive a server restart.");
        } catch (Exception e) {
            fail("Failed to change binding set name.", e);
        }
    }
    
    public void testStandardBindingsChangeTest() {
        //TODO implement
    }
    
    protected void validateTraitMetricValue(String metricName, String value, Resource resource) {
        //TODO do we need to do anything here?
        super.validateTraitMetricValue(metricName, value, resource);
    }

    @AfterTest
    public void restoreSBMConfiguration() {
        try {
            System.out.println("Restoring the saved SBM configuration after the SBM tests.");
            
            updateResourceConfiguration(getSBMResource(), originalSBMConfiguration);
            
            AppServerUtils.shutdownServer();
            
            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            inventoryManager.updatePluginConfiguration(AppServerUtils.getASResource().getId(), originalPluginConfiguration);

            AppServerUtils.startServer();
        } catch (Exception e) {
            fail("Failed to restore the SBM configuration after the tests!!!", e);
        }
    }
    
    private Resource getSBMResource() {
        Set<Resource> sbms = getResources();
        
        if (sbms.size() == 0 || sbms.size() > 1) {
            throw new IllegalStateException("There should be exactly 1 SBM in the inventory but there is " + sbms.size());
        }
        
        return sbms.iterator().next();
    }
}
