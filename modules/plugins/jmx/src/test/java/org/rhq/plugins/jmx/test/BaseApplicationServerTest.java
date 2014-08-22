/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.plugins.jmx.test;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.core.util.StringUtil.isNotBlank;
import static org.rhq.test.AssertUtils.timedAssertion;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXServerComponent;
import org.rhq.test.AssertUtils;

/**
 * @author Thomas Segismont
 */
public abstract class BaseApplicationServerTest extends AbstractJMXPluginTest {
    private static final Log LOG = LogFactory.getLog(BaseApplicationServerTest.class);

    private Integer jmxServerResourceId;
    private JMXServerComponent jmxServerComponent;
    private Resource jmxServerResource;

    @AfterClass
    public void uninventory() {
        if (jmxServerResourceId != null) {
            getInventoryManager().uninventoryResource(jmxServerResourceId);
        }
    }

    protected abstract String getServerTypeName();

    protected abstract String getPluginConfigTemplateName();

    protected abstract void setupTemplatedPluginConfig(Configuration pluginConfig);

    @Test(dependsOnMethods = "testPlatformFound")
    public void testManualAdd() throws Exception {
        ResourceType jmxServerType = getPluginManager().getMetadataManager().getType(SERVER_TYPE_NAME, PLUGIN_NAME);
        assertNotNull(jmxServerType);

        ConfigurationDefinition pluginConfigurationDefinition = jmxServerType.getPluginConfigurationDefinition();
        ConfigurationTemplate template = pluginConfigurationDefinition.getTemplate(getPluginConfigTemplateName());
        assertNotNull(template);

        Configuration pluginConfig = template.createConfiguration();
        setupTemplatedPluginConfig(pluginConfig);

        MergeResourceResponse response = getInventoryManager().manuallyAddResource(jmxServerType,
            getPlatform().getId(), pluginConfig, -1);

        assertNotNull(response, "Manual add response is null");

        jmxServerResourceId = response.getResourceId();

        ResourceContainer resourceContainer = getInventoryManager().getResourceContainer(jmxServerResourceId);
        @SuppressWarnings("rawtypes")
        ResourceComponent resourceComponent = resourceContainer.getResourceComponent();

        assertEquals(resourceComponent.getClass(), JMXServerComponent.class);

        jmxServerComponent = (JMXServerComponent) resourceComponent;
        jmxServerResource = resourceContainer.getResource();
    }

    @Test(dependsOnMethods = "testManualAdd")
    public void testAvailability() throws Exception {
        assertEquals(jmxServerComponent.getAvailability(), AvailabilityType.UP);
    }

    @Test(dependsOnMethods = "testManualAdd")
    public void testServiceDiscovery() throws Exception {
        timedAssertion(new AssertUtils.BooleanCondition() {
            @Override
            public boolean eval() {
                InventoryReport report = getInventoryManager().executeServiceScanImmediately();
                LOG.info("Discovery took: " + (report.getEndTime() - report.getStartTime()) + " ms");

                Set<Resource> childResources = jmxServerResource.getChildResources();
                // Each JMX Server should have exactly six singleton child Resources with the following types:
                // Operating System, Threading, VM Class Loading System, VM Compilation System, VM Memory System, and
                // java.util.logging.
                int childResourcesCount = childResources.size();
                LOG.info("childResourcesCount = " + childResourcesCount);
                return childResourcesCount == 6;

            }
        }, getServerTypeName() + " JMX Server does not have 6 child resources", 2, MINUTES, 10, SECONDS);
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testMeasurement() throws Exception {
        Set<Resource> childResources = getChildResourcesOfType(jmxServerResource, OPERATING_SYSTEM_RESOURCE_TYPE);
        assertEquals(childResources.size(), 1, String.valueOf(childResources));

        MeasurementFacet measurementFacet = getResourceComponentFacet(childResources.iterator().next(),
            MeasurementFacet.class);
        Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
        metricList.add(new MeasurementScheduleRequest(1, "CommittedVirtualMemorySize", 1000, true, MEASUREMENT));

        MeasurementReport report = new MeasurementReport();
        measurementFacet.getValues(report, metricList);
        Map<String, Object> metricsData = getMetricsData(report);

        assertTrue(getMetric(metricsData, "CommittedVirtualMemorySize") > 0);
    }

    @Test(dependsOnMethods = "testServiceDiscovery")
    public void testOperation1() throws Exception {
        Set<Resource> childResources = getChildResourcesOfType(jmxServerResource, THREADING_RESOURCE_TYPE);
        assertEquals(childResources.size(), 1, String.valueOf(childResources));

        OperationResult operationResult = invokeOperation(childResources.iterator().next(), "threadDump",
            new Configuration());
        assertNotNull(operationResult);
        Configuration complexResults = operationResult.getComplexResults();
        assertNotNull(complexResults);
        assertTrue(isNotBlank(complexResults.getSimpleValue("totalCount")));
    }
}
