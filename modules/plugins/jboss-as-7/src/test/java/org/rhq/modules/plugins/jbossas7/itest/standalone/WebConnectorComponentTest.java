/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.itest.standalone;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.domain.resource.ResourceCategory.SERVICE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.modules.plugins.jbossas7.itest.AbstractJBossAS7PluginTest;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * @author Thomas Segismont
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class WebConnectorComponentTest extends AbstractJBossAS7PluginTest {
    private static final String RESOURCE_TYPE_NAME = "Connector";
    private static final String MAX_CONNECTIONS_METRIC_NAME = "_expr:max-connections";
    private static final String MAX_CONNECTIONS_CONFIG_ATTRIBUTE = "max-connections:expr";

    @Test(groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void firstDiscovery() throws Exception {
        Resource platform = pluginContainer.getInventoryManager().getPlatform();
        assertNotNull(platform);
        assertEquals(platform.getInventoryStatus(), InventoryStatus.COMMITTED);
        setMaxDiscoveryDepthOverride(10);
    }

    @Test(groups = "discovery")
    @RunDiscovery(discoverServices = true, discoverServers = true)
    public void secondDiscovery() throws Exception {
        Set<Resource> webConnectorResources = getWebConnectorResources();
        assertTrue(webConnectorResources != null && !webConnectorResources.isEmpty(), "Found no resources of type ["
            + RESOURCE_TYPE_NAME + "]");
        for (Resource webConnectorResource : webConnectorResources) {
            assertEquals(webConnectorResource.getInventoryStatus(), InventoryStatus.COMMITTED);
        }
    }

    private Set<Resource> getWebConnectorResources() {
        ResourceType resourceType = new ResourceType(RESOURCE_TYPE_NAME, getPluginName(), SERVICE, null);
        return pluginContainer.getInventoryManager().getResourcesWithType(resourceType);
    }

    @Test(dependsOnMethods = { "secondDiscovery" })
    public void testGetMaxConnectionMetricHasPositiveValue() throws Exception {
        Set<Resource> webConnectorResources = getWebConnectorResources();
        for (Resource webConnectorResource : webConnectorResources) {
            Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>();
            requests.add(newMeasurementRequest(MAX_CONNECTIONS_METRIC_NAME));
            MeasurementReport report = new MeasurementReport();
            ComponentUtil.getComponent(webConnectorResource.getId(), MeasurementFacet.class, FacetLockType.READ,
                SECONDS.toMillis(30), true, false).getValues(report, requests);
            Set<MeasurementDataNumeric> numericMetrics = report.getNumericData();
            assertEquals(numericMetrics, 1);
            MeasurementData measurementData = numericMetrics.iterator().next();
            assertEquals(measurementData.getName(), MAX_CONNECTIONS_METRIC_NAME);
            assertTrue(measurementData instanceof MeasurementDataNumeric);
            MeasurementDataNumeric metric = (MeasurementDataNumeric) measurementData;
            assertNotNull(metric.getValue());
            assertTrue(metric.getValue() > 0);
        }
    }

    @Test(dependsOnMethods = { "testGetMaxConnectionMetricHasPositiveValue" })
    public void testMaxConnectionConfigPropertyHasValue() throws Exception {
        Set<Resource> webConnectorResources = getWebConnectorResources();
        for (Resource webConnectorResource : webConnectorResources) {
            Configuration configuration = pluginContainer.getConfigurationManager().loadResourceConfiguration(
                webConnectorResource.getId());
            PropertySimple maxConnectionProperty = configuration.getSimple(MAX_CONNECTIONS_CONFIG_ATTRIBUTE);
            assertNotNull(maxConnectionProperty);
            assertNotNull(maxConnectionProperty.getStringValue());
        }
    }

    private static MeasurementScheduleRequest newMeasurementRequest(String metricName) {
        return new MeasurementScheduleRequest(-1, metricName, -1, true, DataType.MEASUREMENT);
    }
}
