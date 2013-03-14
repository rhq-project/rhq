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

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.netservices.HTTPNetServiceComponent.ConfigKeys;
import org.rhq.plugins.netservices.HTTPNetServiceComponent.HttpMethod;

/**
 * @author Thomas Segismont
 */
public class HTTPNetServiceDiscoveryComponentTest {

    private HTTPNetServiceDiscoveryComponent httpNetServiceDiscoveryComponent;

    private Configuration configuration;

    @Mock
    private ResourceDiscoveryContext<?> resourceDiscoveryContext;

    @Mock
    private ResourceType resourceType;

    @BeforeMethod(alwaysRun = true)
    protected void setUp() throws Exception {
        httpNetServiceDiscoveryComponent = new HTTPNetServiceDiscoveryComponent();
        configuration = new Configuration();
        configuration.setSimpleValue(ConfigKeys.URL, "http://www.myhost.com/pipo/molo");
        configuration.setSimpleValue(ConfigKeys.METHOD, HttpMethod.GET.name());
        configuration.setSimpleValue(ConfigKeys.VALIDATE_RESPONSE_PATTERN, "(ok|success)");
        MockitoAnnotations.initMocks(this);
        when(resourceDiscoveryContext.getResourceType()).thenReturn(resourceType);
    }

    @Test
    public void testDiscoverResources() throws Exception {
        // Manual add only, should always return empty set
        assertEquals(0, httpNetServiceDiscoveryComponent.discoverResources(null).size());
    }

    @Test
    public void testValidComponentConfiguration() {
        try {
            DiscoveredResourceDetails resourceDetails = httpNetServiceDiscoveryComponent.discoverResource(
                configuration, resourceDiscoveryContext);
            assertEquals(resourceDetails.getResourceType(), resourceType);
        } catch (InvalidPluginConfigurationException e) {
            fail("Component configuration should be valid", e);
        }
    }

    @Test
    public void testMissingUrl() {
        try {
            configuration.remove(ConfigKeys.URL);
            httpNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), "Endpoint URL is not defined");
        }
    }

    @Test
    public void testMalformedUrl() {
        String configUrl = "pipomolo";
        try {
            configuration.setSimpleValue(ConfigKeys.URL, configUrl);
            httpNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), configUrl + " is not a valid URL");
        }
    }

    @Test
    public void testNotHttpOrHttpsUrl() {
        String configUrl = "ftp://pipo.com/molo.zipo";
        try {
            configuration.setSimpleValue(ConfigKeys.URL, configUrl);
            httpNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), configUrl + "does not point to an http(s) resource");
        }
    }

    @Test
    public void testHttpsUrl() {
        try {
            configuration.setSimpleValue(ConfigKeys.URL, "https://www.myhost.com/pipo/molo");
            DiscoveredResourceDetails resourceDetails = httpNetServiceDiscoveryComponent.discoverResource(
                configuration, resourceDiscoveryContext);
            assertEquals(resourceDetails.getResourceType(), resourceType);
        } catch (InvalidPluginConfigurationException e) {
            fail("Component configuration should be valid", e);
        }
    }

    @Test
    public void testInvalidHttpMethod() {
        String configMethod = "DELETE";
        try {
            configuration.setSimpleValue(ConfigKeys.METHOD, configMethod);
            httpNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), "Invalid http method: " + configMethod);
        }
    }

    @Test
    public void testHeadMethod() {
        try {
            configuration.setSimpleValue(ConfigKeys.METHOD, HttpMethod.HEAD.name());
            configuration.remove(ConfigKeys.VALIDATE_RESPONSE_PATTERN);
            DiscoveredResourceDetails resourceDetails = httpNetServiceDiscoveryComponent.discoverResource(
                configuration, resourceDiscoveryContext);
            assertEquals(resourceDetails.getResourceType(), resourceType);
        } catch (InvalidPluginConfigurationException e) {
            fail("Component configuration should be valid", e);
        }
    }

    @Test
    public void testUnableToValidateContentWithHeadRequest() {
        try {
            configuration.setSimpleValue(ConfigKeys.METHOD, HttpMethod.HEAD.name());
            httpNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), "Cannot validate response content with HEAD request");
        }
    }

    @Test
    public void testInvalidPatternSyntax() {
        String configValidateResponsePattern = "(pipo";
        try {
            configuration.setSimpleValue(ConfigKeys.VALIDATE_RESPONSE_PATTERN, configValidateResponsePattern);
            httpNetServiceDiscoveryComponent.discoverResource(configuration, resourceDiscoveryContext);
            fail("Component configuration should be invalid");
        } catch (InvalidPluginConfigurationException e) {
            assertEquals(e.getMessage(), "Invalid pattern: " + configValidateResponsePattern);
        }
    }

}
