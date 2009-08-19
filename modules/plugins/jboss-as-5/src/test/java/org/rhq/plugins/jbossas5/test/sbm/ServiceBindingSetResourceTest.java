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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceRequest;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.ResourceFactoryManager;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * 
 * @author Lukas Krejci
 */
@Test(groups = {"as5-plugin", "as5-plugin-sbm"})
public class ServiceBindingSetResourceTest extends AbstractServiceBindingTest {

    private static final String NEW_BINDING_SET_NAME = "as5-plugin-test-binding-set";
    private static final String NEW_BINDING_DEFAULT_HOST_NAME = "localhost";
    private static final int NEW_BINDING_SET_PORT_OFFSET = 333;

    private static final String NEW_BINDING_SET_NEW_OVERRIDE_SERVICE_NAME = "as5-plugin-override-service-name";
    private static final String NEW_BINDING_SET_NEW_OVERRIDE_BINDING_NAME = "as5-plugin-override-binding-name";
    private static final int NEW_BINDING_SET_NEW_OVERRIDE_PORT = 45673;
    private static final String NEW_BINDING_SET_NEW_OVERRIDE_HOSTNAME = "somewhere-over-the-rainbow";
    private static final String NEW_BINDING_SET_NEW_OVERRIDE_DESCRIPTION = "a testing override";
    
    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_SERVICE_NAME = "jboss:service=HAJNDI";
    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_BINDING_NAME = "Port";
    private static final int NEW_BINDING_SET_STANDARD_OVERRIDE_PORT = 1200;
    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_HOSTNAME = null;
    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_DESCRIPTION = "An overriden standard binding";

    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_SERVICE_NAME_FIXED = "FixedServiceTest";
    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_BINDING_NAME_FIXED = "Port";
    private static final int NEW_BINDING_SET_STANDARD_OVERRIDE_PORT_FIXED = 45879;
    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_HOSTNAME_FIXED = "localhost";
    private static final String NEW_BINDING_SET_STANDARD_OVERRIDE_DESCRIPTION_FIXED = "A fixed standard binding";

    @BeforeTest
    public void setup() throws Exception {
        saveConfiguration();
        
        //create a fixed-port, fixed-hostname standard binding that we can test overrides with
        Configuration sbmConfig = loadResourceConfiguration(getSBMResource());
        
        PropertyList bindings = sbmConfig.getList("standardBindings");
        PropertyMap newBinding = new PropertyMap("binding");
        bindings.add(newBinding);
        newBinding.put(new PropertySimple("serviceName", NEW_BINDING_SET_STANDARD_OVERRIDE_SERVICE_NAME_FIXED));
        newBinding.put(new PropertySimple("bindingName", NEW_BINDING_SET_STANDARD_OVERRIDE_BINDING_NAME_FIXED));
        newBinding.put(new PropertySimple("port", NEW_BINDING_SET_STANDARD_OVERRIDE_PORT_FIXED));
        newBinding.put(new PropertySimple("hostName", NEW_BINDING_SET_STANDARD_OVERRIDE_HOSTNAME_FIXED));
        newBinding.put(new PropertySimple("description", NEW_BINDING_SET_STANDARD_OVERRIDE_DESCRIPTION_FIXED));
        newBinding.put(new PropertySimple("fixedPort", true));
        newBinding.put(new PropertySimple("fixedHostName", true));
        
        updateResourceConfiguration(getSBMResource(), sbmConfig);
    }

    protected String getResourceTypeName() {
        return SERVICE_BINDING_SET_SERVICE_NAME;
    }

    public void testMetrics() throws Exception {
        super.testMetrics();
    }

    @Test(dependsOnMethods = "testCreateServiceBindingSet")
    public void testOperations() throws Exception {
        super.testOperations();
    }

    public void testCreateServiceBindingSet() throws Exception {
        Configuration bindingSetConfiguration = new Configuration();
        
        bindingSetConfiguration.put(new PropertySimple("name", NEW_BINDING_SET_NAME));
        bindingSetConfiguration.put(new PropertySimple("defaultHostName", NEW_BINDING_DEFAULT_HOST_NAME));
        bindingSetConfiguration.put(new PropertySimple("portOffset", NEW_BINDING_SET_PORT_OFFSET));
        
        bindingSetConfiguration.put(createOverrideBindings());
        
        Resource sbmResource = getSBMResource();
        
        ResourceFactoryManager resourceFactory = PluginContainer.getInstance().getResourceFactoryManager();
        
        CreateResourceRequest request = new CreateResourceRequest();
        request.setPluginName(getPluginName());
        request.setParentResourceId(sbmResource.getId());
        request.setResourceConfiguration(bindingSetConfiguration);
        request.setResourceName(NEW_BINDING_SET_NAME);
        request.setResourceTypeName(getResourceTypeName());
        
        CreateResourceResponse response = resourceFactory.executeCreateResourceImmediately(request);
        
        assertEquals(response.getStatus(), CreateResourceStatus.SUCCESS, "Failed to create a new binding set. Error message: " + response.getErrorMessage());
        
        //check that the binding set stays there after the restart
        AppServerUtils.restartServer();
        
        Resource bindingSetAfterRestart = getBindingSet(NEW_BINDING_SET_NAME);
        assertNotNull(bindingSetAfterRestart, "The newly created binding wasn't persisted across the server restart.");
        
        Configuration configAfterRestart = loadResourceConfiguration(bindingSetAfterRestart);
        
        //check that the config was properly persisted
        assertEquals(configAfterRestart.getSimpleValue("name", null), NEW_BINDING_SET_NAME, "Name not persisted across restart.");
        assertEquals(configAfterRestart.getSimpleValue("defaultHostName", null), NEW_BINDING_DEFAULT_HOST_NAME, "Default host name not persisted across restart.");
        assertEquals(configAfterRestart.getSimple("portOffset").getIntegerValue(), Integer.valueOf(NEW_BINDING_SET_PORT_OFFSET), "Port offset not persisted across restart.");
        assertEquals(configAfterRestart.get("overrideBindings"), getExpectedOverrideBindings(), "Override bindings not persisted across restart.");
    }

    @Test(dependsOnMethods = "testPortOffsetChange") //so that we delete the newly created binding set after we reused it
    public void testDeleteServiceBindingSet() throws Exception {
        Resource bindingSetToDelete = getBindingSet(NEW_BINDING_SET_NAME);
        
        assertNotNull(bindingSetToDelete, "Could not find a binding set to delete");
        
        ResourceFactoryManager resourceFactory = PluginContainer.getInstance().getResourceFactoryManager();
        resourceFactory.executeDeleteResourceImmediately(new DeleteResourceRequest(0, bindingSetToDelete.getId()));

        assertNull(getBindingSet(NEW_BINDING_SET_NAME), "Failed to delete the binding set.");

        //check that the delete is permanent
        AppServerUtils.restartServer();
        
        assertNull(getBindingSet(NEW_BINDING_SET_NAME), "Failed to delete the binding set - it reappeared afer restart.");
    }
    
    @Test(dependsOnMethods = "testOperations") //so that we can fiddle with the binding set after the display bindings op has been checked
    public void testPortOffsetChange() throws Exception {
        Resource bindingSet = getBindingSet(NEW_BINDING_SET_NAME);
        
        assertNotNull(bindingSet, "Could not find a binding set to change the port offset of.");
        
        Configuration config = loadResourceConfiguration(bindingSet);
        
        int newOffset = config.getSimple("portOffset").getIntegerValue() + 100;
        
        config.put(new PropertySimple("portOffset", newOffset));
        
        updateResourceConfiguration(bindingSet, config);
        
        Resource updatedBindingSet = getBindingSet(NEW_BINDING_SET_NAME);
        
        assertNotNull(updatedBindingSet, "Could not find the binding set after changing port offset.");
        
        //Check the change is persisted across the restart
        AppServerUtils.restartServer();
        
        Resource bindingSetAfterRestart = getBindingSet(NEW_BINDING_SET_NAME);
        assertNotNull(bindingSetAfterRestart, "The change of the port offset didn't survive the restart.");

        Configuration configAfterRestart = loadResourceConfiguration(bindingSetAfterRestart);
        int offsetAfterRestart = configAfterRestart.getSimple("portOffset").getIntegerValue();

        assertEquals(offsetAfterRestart, newOffset, "Unexpected port offset after server restart.");
    }

    @AfterTest
    @Override
    public void restoreConfiguration() {
        super.restoreConfiguration();
    }

    protected void validateOperationResult(String name, OperationResult result, Resource resource) {
        if ("displayBindings".equals(name) && NEW_BINDING_SET_NAME.equals(resource.getName())) {
            try {
                Configuration bindingConfiguration = loadResourceConfiguration(resource);
                Configuration results = result.getComplexResults();
                Configuration sbmConfiguration = loadResourceConfiguration(getSBMResource());
                
                int portOffset = bindingConfiguration.getSimple("portOffset").getIntegerValue();
                String defaultHostName = bindingConfiguration.getSimpleValue("defaultHostName", null);
                
                PropertyList resultingBindings = results.getList("resultingBindings");
                PropertyList overrideBindings = bindingConfiguration.getList("overrideBindings");
                
                //go through the standard bindings to see if the results conform
                for(Property p : sbmConfiguration.getList("standardBindings").getList()) {
                    PropertyMap standardBinding = (PropertyMap) p;
                    
                    String standardBindingServiceName = standardBinding.getSimpleValue("serviceName", null);
                    String standardBindingBindingName = standardBinding.getSimpleValue("bindingName", null);
                    
                    PropertyMap resultingBinding = findBinding(resultingBindings, standardBindingServiceName, standardBindingBindingName);
                    PropertyMap overrideBinding = findBinding(overrideBindings, standardBindingServiceName, standardBindingBindingName);
                    
                    //The rules are as follows:
                    //ResPort = fixedPort ? standardPort : (overrideExists ? overridePort : standardPort + portOffset)
                    //ResHostName = fixedHostName ? standardHostname : (overrideExists ? (overrideHostName == null ? defaultHostName : overrideHostName) : defaultHostName)
                    
                    PropertySimple fixedPortProp = standardBinding.getSimple("fixedPort");
                    PropertySimple fixedHostNameProp = standardBinding.getSimple("fixedHostName");
                    
                    boolean isFixedPort = fixedPortProp == null ? false : fixedPortProp.getBooleanValue();
                    boolean isFixedHostName = fixedHostNameProp == null ? false : fixedHostNameProp.getBooleanValue();
                    
                    int standardBindingPort = standardBinding.getSimple("port").getIntegerValue();
                    String standardBindingHostName = standardBinding.getSimpleValue("hostName", null);
                    
                    int resultingPort = resultingBinding.getSimple("port").getIntegerValue();
                    String resultingHostName = resultingBinding.getSimpleValue("hostName", null);
                    
                    String standardFQN = standardBinding.getSimpleValue("fullyQualifiedName", null);
                    String resultingFQN = resultingBinding.getSimpleValue("fullyQualifiedName", null);
                    
                    assertEquals(resultingFQN, standardFQN);

                    if (overrideBinding == null) {
                        int expectedPort = standardBindingPort + (isFixedPort ? 0 : portOffset);
                        String expectedHostName = isFixedHostName ? standardBindingHostName : defaultHostName;
                        
                        assertEquals(resultingPort, expectedPort);
                        assertEquals(resultingHostName, expectedHostName);
                        
                        String standardDescription = standardBinding.getSimpleValue("description", null);
                        String resultingDescription = resultingBinding.getSimpleValue("description", null);
                    
                        assertEquals(resultingDescription, standardDescription);
  
                    } else {
                        int overridePort = overrideBinding.getSimple("port").getIntegerValue();
                        String overrideHostName = overrideBinding.getSimpleValue("hostName", null);
                        
                        int expectedPort = isFixedPort ? standardBindingPort : overridePort;
                        String expectedHostName = isFixedHostName ? standardBindingHostName : (overrideHostName == null ? defaultHostName : overrideHostName);
                        
                        assertEquals(resultingPort, expectedPort);
                        assertEquals(resultingHostName, expectedHostName);
                        
                        String overrideDescription = overrideBinding.getSimpleValue("description", null);
                        String resultingDescription = resultingBinding.getSimpleValue("description", null);
                        
                        assertEquals(resultingDescription, overrideDescription);
                    }
                }
            } catch (Exception e) {
                fail("Failed to check operation results.", e);
            }
        } else {
            super.validateOperationResult(name, result, resource);
        }
    }

    private PropertyList createOverrideBindings() {
        PropertyList bindings = new PropertyList("overrideBindings");
        
        //create the new override
        PropertyMap newOverride = new PropertyMap("binding");
        bindings.add(newOverride);
        newOverride.put(new PropertySimple("serviceName", NEW_BINDING_SET_NEW_OVERRIDE_SERVICE_NAME));
        newOverride.put(new PropertySimple("bindingName", NEW_BINDING_SET_NEW_OVERRIDE_BINDING_NAME));
        newOverride.put(new PropertySimple("port", NEW_BINDING_SET_NEW_OVERRIDE_PORT));
        newOverride.put(new PropertySimple("hostName", NEW_BINDING_SET_NEW_OVERRIDE_HOSTNAME));
        newOverride.put(new PropertySimple("description", NEW_BINDING_SET_NEW_OVERRIDE_DESCRIPTION));
        
        //create the standard override
        PropertyMap standardOverride = new PropertyMap("binding");
        bindings.add(standardOverride);
        standardOverride.put(new PropertySimple("serviceName", NEW_BINDING_SET_STANDARD_OVERRIDE_SERVICE_NAME));
        standardOverride.put(new PropertySimple("bindingName", NEW_BINDING_SET_STANDARD_OVERRIDE_BINDING_NAME));
        standardOverride.put(new PropertySimple("port", NEW_BINDING_SET_STANDARD_OVERRIDE_PORT));
        standardOverride.put(new PropertySimple("hostName", NEW_BINDING_SET_STANDARD_OVERRIDE_HOSTNAME));
        standardOverride.put(new PropertySimple("description", NEW_BINDING_SET_STANDARD_OVERRIDE_DESCRIPTION));
        

        PropertyMap standardOverride2 = new PropertyMap("binding");
        bindings.add(standardOverride2);
        standardOverride2.put(new PropertySimple("serviceName", NEW_BINDING_SET_STANDARD_OVERRIDE_SERVICE_NAME_FIXED));
        standardOverride2.put(new PropertySimple("bindingName", NEW_BINDING_SET_STANDARD_OVERRIDE_BINDING_NAME_FIXED));
        standardOverride2.put(new PropertySimple("port", NEW_BINDING_SET_STANDARD_OVERRIDE_PORT_FIXED));
        standardOverride2.put(new PropertySimple("hostName", NEW_BINDING_SET_STANDARD_OVERRIDE_HOSTNAME_FIXED));
        standardOverride2.put(new PropertySimple("description", NEW_BINDING_SET_STANDARD_OVERRIDE_DESCRIPTION_FIXED));
        
        return bindings;
    }    
    
    private PropertyList getExpectedOverrideBindings() {
        PropertyList bindings = createOverrideBindings();
        
        //we must add the "fullyQualifiedName" to the bindings because
        //that's what the plugin should fill in automatically
        for(Property p : bindings.getList()) {
            PropertyMap binding = (PropertyMap) p;
            String serviceName = binding.getSimpleValue("serviceName", null);
            String bindingName = binding.getSimpleValue("bindingName", null);
            String fullyQualifiedName = serviceName;
            if (bindingName != null) {
                fullyQualifiedName += ":" + bindingName;
            }
            
            binding.put(new PropertySimple("fullyQualifiedName", fullyQualifiedName));
        }
        
        return bindings;
    }
    
    private Resource getBindingSet(String name) throws Exception {
        for(Resource r : getResources()) {
            Configuration config = loadResourceConfiguration(r);
            if (name.equals(config.getSimpleValue("name", null))) {
                return r;
            }
        }
        
        return null;
    }
}
