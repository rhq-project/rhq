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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceFactoryManager;
import org.rhq.plugins.jbossas5.test.AbstractResourceTest;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;

/**
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractServiceBindingTest extends AbstractResourceTest {

    public static final String SERVICE_BINDING_MANAGER_SERVICE_NAME = "Service Binding Manager";
    public static final String SERVICE_BINDING_SET_SERVICE_NAME = "Service Binding Set";
    
    public static final String JNP_PORT_BINDING_SERVICE_NAME = "jboss:service=Naming";
    public static final String JNP_PORT_BINDING_BINDING_NAME = "Port";
    
    private Configuration originalSBMConfiguration;
    private Configuration originalPluginConfiguration;

    private Map<String, Configuration> originalBindingSets;
    
    protected void saveConfiguration() {
        try {
            System.out.println("Saving the original SBM configuration before the SBM tests.");
            originalSBMConfiguration = loadResourceConfiguration(getSBMResource());
            originalPluginConfiguration = AppServerUtils.getASResource().getPluginConfiguration().clone();
            originalBindingSets = getAllBindingSets();
        } catch (Exception e) {
            fail("Could not save the SBM configuration before SBM tests.", e);
        }
    }
    
    protected void restoreConfiguration() {
        try {
            System.out.println("Restoring the saved SBM configuration after the SBM tests.");
            
            Resource sbm = getSBMResource();
            
            updateResourceConfiguration(sbm, originalSBMConfiguration);
            
            restoreBindingSets(sbm, originalBindingSets);
            
            AppServerUtils.shutdownServer();
            
            InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
            inventoryManager.updatePluginConfiguration(AppServerUtils.getASResource().getId(), originalPluginConfiguration);

            AppServerUtils.startServer();
        } catch (Exception e) {
            fail("Failed to restore the SBM configuration after the tests!!!", e);
        }
    }

    protected Resource getSBMResource() {
        ResourceType sbmResourceType = getResourceType(SERVICE_BINDING_MANAGER_SERVICE_NAME, getPluginName());
        
        Set<Resource> sbms = getResources(sbmResourceType);
        
        if (sbms.size() == 0 || sbms.size() > 1) {
            throw new IllegalStateException("There should be exactly 1 SBM in the inventory but there is " + sbms.size());
        }
        
        return sbms.iterator().next();
    }

    private Map<String, Configuration> getAllBindingSets() throws PluginContainerException {
        ResourceType sbsResourceType = getResourceType(SERVICE_BINDING_SET_SERVICE_NAME, getPluginName());
        
        Map<String, Configuration> configs = new HashMap<String, Configuration>();
        
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        
        for(Resource sbs : getResources(sbsResourceType)) {
            Configuration config = configurationManager.loadResourceConfiguration(sbs.getId());
            String name = config.getSimpleValue("name", null);
            configs.put(name, config);
        }
        return configs;
    }

    protected PropertyMap findBinding(PropertyList bindings, String serviceName, String bindingName) {
    
        for(Property p : bindings.getList()) {
            PropertyMap binding = (PropertyMap) p;
            
            String currentBindingName = binding.getSimpleValue("bindingName", null);
            String currentServiceName = binding.getSimpleValue("serviceName", null);
            
            if (safeEquals(currentBindingName, bindingName) &&
                safeEquals(currentServiceName, serviceName)) {
                
                return binding;
            }
        }
        
        return null;
    }

    private void restoreBindingSets(Resource sbm, Map<String, Configuration> originalBindingSets) throws Exception {
        ResourceType sbsResourceType = getResourceType(SERVICE_BINDING_SET_SERVICE_NAME, getPluginName());

        ResourceFactoryManager resourceFactory = PluginContainer.getInstance().getResourceFactoryManager();
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        
        for(Resource sbs : getResources(sbsResourceType)) {
            Configuration config = configurationManager.loadResourceConfiguration(sbs.getId());
            String name = config.getSimpleValue("name", null);
            
            Configuration originalConfiguration = originalBindingSets.get(name);
            if(originalConfiguration != null) {
                updateResourceConfiguration(sbs, originalConfiguration);
                originalBindingSets.remove(name);
            } else {
                //this binding set wasn't there originally
                resourceFactory.executeDeleteResourceImmediately(new DeleteResourceRequest(0, sbs.getId()));
            }
        }
        
        //originalBindingSets now only contain binding sets that existed originally but aren't anymore
        for(Configuration sbs : originalBindingSets.values()) {
            String name = sbs.getSimpleValue("name", null);
            
            CreateResourceRequest request = new CreateResourceRequest();
            request.setPluginName(getPluginName());
            request.setParentResourceId(sbm.getId());
            request.setResourceName(name);
            request.setResourceTypeName(SERVICE_BINDING_SET_SERVICE_NAME);
            request.setResourceConfiguration(sbs);
            
            resourceFactory.executeCreateResourceImmediately(request);
        }
    }

    private static boolean safeEquals(Object o1, Object o2) {
        if (o1 == o2) return true;
        
        if (o1 == null || o2 == null) return false;
        
        return o1.equals(o2);
    }
}
