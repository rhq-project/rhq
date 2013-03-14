/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.plugins.netservices;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.netservices.PortNetServiceComponent.ConfigKeys;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.UnknownHostException;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Thomas Segismont
 */
public class PortNetServiceDiscoveryComponentTest {

    private PortNetServiceDiscoveryComponent portNetServiceDiscoveryComponent;

    private Configuration configuration;

    @Mock
    private ResourceDiscoveryContext<?> resourceDiscoveryContext;

    @Mock
    private ResourceType resourceType;

    @BeforeMethod(alwaysRun = true)
    protected void setUp() throws Exception {
        portNetServiceDiscoveryComponent = new PortNetServiceDiscoveryComponent();
        configuration = new Configuration();
        configuration.setSimpleValue(ConfigKeys.ADDRESS, "127.0.0.1");
        configuration.setSimpleValue(ConfigKeys.PORT, "8080");
        MockitoAnnotations.initMocks(this);
        when(resourceDiscoveryContext.getResourceType()).thenReturn(resourceType);
    }

    @Test
    public void testDiscoverResources() throws Exception {
        // Manual add only, should always return empty set
        assertEquals(0, portNetServiceDiscoveryComponent.discoverResources(null).size());
    }

    @Test
    public void testValidComponentConfiguration() {
        try {
            DiscoveredResourceDetails resourceDetails = portNetServiceDiscoveryComponent.discoverResource(
                configuration, resourceDiscoveryContext);
            assertEquals(resourceDetails.getResourceType(), resourceType);
        } catch (InvalidPluginConfigurationException e) {
            fail("Component configuration should be valid", e);
        }
    }

    @Test
    public void testMissingAddress() {
        configuration.remove(ConfigKeys.ADDRESS);
        try {
            portNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), "Address is not defined");
        }
    }

    @Test
    public void testMissingPort() {
        configuration.remove(ConfigKeys.PORT);
        try {
            portNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), "Port is not defined");
        }
    }

    @Test
    public void testInvalidPortNumberFormat() {
        String configPort = "NaN";
        configuration.setSimpleValue(ConfigKeys.PORT, configPort);
        try {
            portNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), "Invalid port number: " + configPort);
        }
    }

    @Test
    public void testMalformedAddress() {
        String configAddress = "pipomolo";
        configuration.setSimpleValue(ConfigKeys.ADDRESS, configAddress);
        try {
            portNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getCause().getClass(), UnknownHostException.class);
        }
    }

}
