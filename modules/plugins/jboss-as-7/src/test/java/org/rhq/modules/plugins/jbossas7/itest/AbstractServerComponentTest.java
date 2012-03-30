/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
 * All rights reserved.
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
package org.rhq.modules.plugins.jbossas7.itest;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

import static org.testng.Assert.*;

/**
 * The base class for the integration tests for the two AS7 server types.
 *
 * @author Ian Springer
 */
public abstract class AbstractServerComponentTest extends AbstractJBossAS7PluginTest {

    private static final Map<String, String> EAP6_VERSION_TO_AS7_VERSION_MAP = new HashMap<String, String>();
    static {
        EAP6_VERSION_TO_AS7_VERSION_MAP.put("6.0.0.Beta1", "7.1.0.Final-redhat-1");
        EAP6_VERSION_TO_AS7_VERSION_MAP.put("6.0.0.ER4", "7.1.1.Final-redhat-1");
        EAP6_VERSION_TO_AS7_VERSION_MAP.put("6.0.0.CR1", "7.1.1.Final-redhat-1");
    }

    private static final String RELEASE_VERSION_TRAIT_NAME = "_skm:release-version";

    private static final String SHUTDOWN_OPERATION_NAME = "shutdown";
    private static final String START_OPERATION_NAME = "start";

    protected abstract ResourceType getServerResourceType();

    protected abstract String getServerResourceKey();

    protected Resource getServerResource() {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        return getResourceByTypeAndKey(platform, getServerResourceType(), getServerResourceKey());
    }

    public void testAutoDiscovery() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        assertNotNull(platform);
        assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);

        assertNotNull(getServerResource(),
                getServerResourceType() + " Resource with key [" + getServerResourceKey() + "] was not discovered.");
        System.out.println("===== Discovered: " + getServerResource());
        Configuration pluginConfig = getServerResource().getPluginConfiguration();
        System.out.println("---------- " + pluginConfig.toString(true));
        validatePluginConfiguration(pluginConfig);
    }

    private void validatePluginConfiguration(Configuration pluginConfig) {
        String hostname = pluginConfig.getSimpleValue("hostname", null);
        String expectedHostname = System.getProperty(getBindAddressSystemPropertyName());
        assertEquals(hostname, expectedHostname, "Plugin config prop [hostname].");

        String portString = pluginConfig.getSimpleValue("port", null);
        Integer port = (portString != null) ? Integer.valueOf(portString) : null;
        String portOffsetString = System.getProperty(getPortOffsetSystemPropertyName());
        int portOffset = (portOffsetString != null) ? Integer.valueOf(portOffsetString) : 0;
        Integer expectedPort = portOffset + 9990;        
        assertEquals(port, expectedPort, "Plugin config prop [port].");
    }

    protected abstract String getBindAddressSystemPropertyName();
    
    protected abstract String getPortOffsetSystemPropertyName();

    public void testMetricsHaveNonNullValues() throws Exception {
        assertAllNumericMetricsAndTraitsHaveNonNullValues(getServerResource());
    }

    public void testReleaseVersionTrait() throws Exception {
        String releaseVersion = collectTrait(getServerResource(), RELEASE_VERSION_TRAIT_NAME);
        String as7Version = System.getProperty( "as7.version" );
        String expectedReleaseVersion;
        if (as7Version.startsWith("6.")) {
            // EAP6
            expectedReleaseVersion = EAP6_VERSION_TO_AS7_VERSION_MAP.get(as7Version);
            if (expectedReleaseVersion == null) {
                throw new Exception("No AS7 version mapping is defined for EAP6 version [" + as7Version + "].");
            }
        } else {
            // AS7
            expectedReleaseVersion = as7Version;
        }
        assertEquals(releaseVersion, expectedReleaseVersion,
                "Unexpected value for trait [" + RELEASE_VERSION_TRAIT_NAME + "].");
    }

    public void testShutdownAndStartOperations() throws Exception {
        AvailabilityType avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);
        invokeOperationAndAssertSuccess(getServerResource(), SHUTDOWN_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.DOWN);
        // Restart the server, so the rest of the tests don't fail.
        invokeOperationAndAssertSuccess(getServerResource(), START_OPERATION_NAME, null);
        avail = getAvailability(getServerResource());
        assertEquals(avail, AvailabilityType.UP);
    }

}
