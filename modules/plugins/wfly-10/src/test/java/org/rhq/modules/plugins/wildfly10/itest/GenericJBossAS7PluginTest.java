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

package org.rhq.modules.plugins.wildfly10.itest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.Nullable;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.MeasurementDefinitionFilter;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.plugin.testutil.AbstractAgentPluginTest;
import org.rhq.modules.plugins.wildfly10.itest.domain.ManagedServerTest;
import org.rhq.modules.plugins.wildfly10.test.util.Constants;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * AS7 plugin tests that are not specific to particular Resource types. The tests delegate to methods in
 * {@link AbstractAgentPluginTest}, which provides generic impls of such tests.
 *
 * @author Ian Springer
 */
@Test(groups = { "integration", "pc" }, singleThreaded = true)
public class GenericJBossAS7PluginTest extends AbstractJBossAS7PluginTest {

    // ****************************** LIFECYCLE ****************************** //
    @Test(priority = 1)
    @RunDiscovery
    public void testAllResourceComponentsStarted() throws Exception {
        // first, wait for entire discovery to stabilize
        Resource platform = validatePlatform();
        validateDiscovery();

        // (jshaughn) The idea of this test, i think, is to ensure all component start methods work, so
        // next, proactively try and start all components. Some may already be started, that's OK, the
        // activate call will just be a no-op.
        System.out.println("Starting all resources...");
        startComponent(this.pluginContainer.getInventoryManager(), platform);

        // now, check that they are started
        System.out.println("Validating all resources have started...");
        assertAllResourceComponentsStarted(this.pluginContainer.getInventoryManager(), platform);
    }

    private void startComponent(InventoryManager im, Resource resource) {
        ResourceContainer container = im.getResourceContainer(resource);
        if (null == container) {
            throw new IllegalStateException("No container found for resource " + resource);
        }

        try {
            im.activateResource(resource, container, false);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to activate resource " + resource);
        }

        for (Resource child : resource.getChildResources()) {
            startComponent(im, child);
        }
    }

    private void assertAllResourceComponentsStarted(InventoryManager im, Resource platform) throws Exception {

        Map<ResourceType, ResourceContainer> nonStartedResourceContainersByType = new LinkedHashMap<ResourceType, ResourceContainer>();
        findNonStartedResourceComponentsRecursively(im, platform, nonStartedResourceContainersByType);
        assertTrue(nonStartedResourceContainersByType.isEmpty(),
            "Resource containers with non-started Resource components by type: " + nonStartedResourceContainersByType);
    }

    private void findNonStartedResourceComponentsRecursively(InventoryManager im, Resource resource,
        Map<ResourceType, ResourceContainer> nonStartedResourceContainersByType) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        if (!nonStartedResourceContainersByType.containsKey(resourceType)) {
            ResourceContainer resourceContainer = im.getResourceContainer(resource);
            if (resourceContainer.getResourceComponentState() == ResourceContainer.ResourceComponentState.STARTING) {
                // give it 5s to finish starting
                try {
                    System.out.println("Resource is STARTING, Waiting 5s before checking for STARTED " + resource);
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    // keep going
                }
            }
            if (resourceContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
                nonStartedResourceContainersByType.put(resourceType, resourceContainer);

            } else if (resourceContainer.getResourceComponent() == null) {
                System.err.println("****** Resource container " + resourceContainer
                    + " says its Resource component is started, but the component is null. ******");
                nonStartedResourceContainersByType.put(resourceType, resourceContainer);
            }
        }

        // Recurse.
        for (Resource childResource : resource.getChildResources()) {
            findNonStartedResourceComponentsRecursively(im, childResource, nonStartedResourceContainersByType);
        }
    }

    // ******************************* METRICS ******************************* //
    @Test(priority = 2)
    public void testAllMetricsHaveNonNullValues() throws Exception {
        Map<ResourceType, String[]> excludedMetricNamesByType = new HashMap<ResourceType, String[]>();
        // It's normal for the "startTime" trait to be null for a Managed Server that is down/disabled.
        // It's normal for the "multicastAddress" trait to be null for a Managed Server that is not configured for JGroups HA.
        excludedMetricNamesByType
            .put(ManagedServerTest.RESOURCE_TYPE, new String[] { "startTime", "multicastAddress" });
        // Some memory pools do not expose those statistics (by default), so in case they
        // are not exposed, it is normal that the server returns 'undefined' for the value
        // Note that those
        excludedMetricNamesByType.put(new ResourceType("Memory Pool Resource", Constants.PLUGIN_NAME, ResourceCategory.SERVICE,
            null), new String[] { "collection-usage-threshold-count", "collection-usage-threshold", "collection-usage",
            "collection-usage-threshold-exceeded", "collection-usage:committed", "collection-usage:init",
            "collection-usage:max", "collection-usage:used", "usage-threshold-count", "usage-threshold-exceeded" });

        //the max-connections will be 'undefined' if no specific value is set. This is AS's way of saying the value
        //is connector specific
        excludedMetricNamesByType.put(new ResourceType("Connector (Managed Server)", Constants.PLUGIN_NAME, ResourceCategory.SERVICE, null),
            new String[] {"_expr:max-connections"});
        excludedMetricNamesByType.put(new ResourceType("Connector", Constants.PLUGIN_NAME, ResourceCategory.SERVICE, null),
            new String[] {"_expr:max-connections"});

        // It's normal for the "active-patches" to have null value on servers that do not support patching.
        excludedMetricNamesByType.put(new ResourceType("JBossAS7 Host Controller", Constants.PLUGIN_NAME, ResourceCategory.SERVER,
                null), new String[]{"active-patches"});
        excludedMetricNamesByType.put(new ResourceType("JBossAS7 Standalone Server", Constants.PLUGIN_NAME, ResourceCategory.SERVER,
            null), new String[]{"active-patches"});

        assertAllNumericMetricsAndTraitsHaveNonNullValues(excludedMetricNamesByType);
    }

    protected void assertAllNumericMetricsAndTraitsHaveNonNullValues(
        Map<ResourceType, String[]> excludedMetricNamesByType) throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        LinkedHashMap<ResourceType, Set<String>> metricsWithNullValuesByType = new LinkedHashMap<ResourceType, Set<String>>();
        findNumericMetricsAndTraitsWithNullValuesRecursively(platform, metricsWithNullValuesByType);
        removeExcludedMetricNames(metricsWithNullValuesByType, excludedMetricNamesByType);
        assertTrue(metricsWithNullValuesByType.isEmpty(), "Metrics with null values by type: "
            + metricsWithNullValuesByType);
    }

    private void removeExcludedMetricNames(LinkedHashMap<ResourceType, Set<String>> metricsWithNullValuesByType,
        Map<ResourceType, String[]> excludedMetricNamesByType) {
        for (Iterator<ResourceType> mapIterator = metricsWithNullValuesByType.keySet().iterator(); mapIterator
            .hasNext();) {
            ResourceType resourceType = mapIterator.next();
            if (excludedMetricNamesByType.get(resourceType) == null) {
                continue;
            }

            Set<String> namesOfMetricsWithNullValues = metricsWithNullValuesByType.get(resourceType);
            List<String> excludedMetricNames = Arrays.asList(excludedMetricNamesByType.get(resourceType));
            for (Iterator<String> setIterator = namesOfMetricsWithNullValues.iterator(); setIterator.hasNext();) {
                String nameOfMetricWithNullValue = setIterator.next();
                if (excludedMetricNames.contains(nameOfMetricWithNullValue)) {
                    setIterator.remove();
                }
            }
            if (namesOfMetricsWithNullValues.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private void findNumericMetricsAndTraitsWithNullValuesRecursively(Resource resource,
        Map<ResourceType, Set<String>> metricsWithNullValuesByType) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        // Only check metrics on types of Resources from the plugin under test.
        if (resourceType.getPlugin().equals(getPluginName())) {
            ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(
                resource);
            if (resourceContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
                return;
            }

            Set<String> metricsWithNullValues = getNumericMetricsAndTraitsWithNullValues(resource);
            if (!metricsWithNullValues.isEmpty()) {
                Set<String> metricsWithNullValuesForType = metricsWithNullValuesByType.get(resourceType);
                if (metricsWithNullValuesForType != null) {
                    metricsWithNullValuesForType.addAll(metricsWithNullValues);
                } else {
                    metricsWithNullValuesByType.put(resourceType, metricsWithNullValues);
                }
            }
        }

        // Recurse.
        for (Resource childResource : resource.getChildResources()) {
            findNumericMetricsAndTraitsWithNullValuesRecursively(childResource, metricsWithNullValuesByType);
        }
    }

    protected Set<String> getNumericMetricsAndTraitsWithNullValues(Resource resource) throws Exception {
        ResourceType type = resource.getResourceType();
        Set<MeasurementDefinition> numericMetricAndTraitDefs = ResourceTypeUtility.getMeasurementDefinitions(type,
            new MeasurementDefinitionFilter() {
                private final Set<DataType> acceptableDataTypes = EnumSet.of(DataType.MEASUREMENT, DataType.TRAIT);

                @Override
                public boolean accept(MeasurementDefinition metricDef) {
                    return acceptableDataTypes.contains(metricDef.getDataType());
                }
            });
        Set<String> metricsWithNullValues = getMetricsWithNullValues(resource, numericMetricAndTraitDefs);
        return metricsWithNullValues;
    }

    protected Set<String> getMetricsWithNullValues(Resource resource, Set<MeasurementDefinition> metricDefs)
        throws Exception {
        Set<String> metricsWithNullValues = new TreeSet<String>();
        for (MeasurementDefinition metricDef : metricDefs) {
            if (!metricDef.getResourceType().equals(resource.getResourceType())) {
                throw new IllegalArgumentException(metricDef + " is not defined by " + resource.getResourceType());
            }
            Object value;
            switch (metricDef.getDataType()) {
            case MEASUREMENT:
                value = collectNumericMetric(resource, metricDef.getName());
                break;
            case TRAIT:
                value = collectTrait(resource, metricDef.getName());
                break;
            default:
                throw new IllegalArgumentException("Unsupported metric type: " + metricDef.getDataType());
            }
            if (value == null) {
                metricsWithNullValues.add(metricDef.getName());
            }
        }
        return metricsWithNullValues;
    }

    @Nullable
    protected Double collectNumericMetric(Resource resource, String metricName) throws Exception {
        System.out.println("=== Collecting numeric metric [" + metricName + "] for " + resource + "...");
        MeasurementReport report = collectMetric(resource, metricName);

        Double value;
        if (report.getNumericData().isEmpty()) {
            assertEquals(
                report.getTraitData().size(),
                0,
                "Metric [" + metricName + "] for Resource type " + resource.getResourceType()
                    + " is defined as a numeric metric, but the plugin returned one or more traits!: "
                    + report.getTraitData());
            assertEquals(report.getCallTimeData().size(), 0,
                "Metric [" + metricName + "] for Resource type " + resource.getResourceType()
                    + " is defined as a numeric metric, but the plugin returned one or more call-time metrics!: "
                    + report.getCallTimeData());
            value = null;
        } else {
            assertEquals(report.getNumericData().size(), 1,
                "Requested a single metric, but plugin returned more than one datum: " + report.getNumericData());
            MeasurementDataNumeric datum = report.getNumericData().iterator().next();
            assertEquals(datum.getName(), metricName, "Numeric metric [" + metricName + "] for Resource type "
                + resource.getResourceType() + " was requested, but the plugin returned a numeric metric with name ["
                + datum.getName() + "] and value [" + datum.getValue() + "]!");
            // Normalize NaN or infinite to null, as the PC does.
            value = (datum.getValue().isNaN() || datum.getValue().isInfinite()) ? null : datum.getValue();
        }
        System.out.println("====== Collected numeric metric [" + metricName + "] with value of [" + value + "] for "
            + resource + ".");

        return value;
    }

    // **************************** RESOURCE CONFIG ************************** //
    @Test(priority = 3)
    public void testAllResourceConfigsLoad() throws Exception {
        assertAllResourceConfigsLoad();
    }

    protected void assertAllResourceConfigsLoad() throws Exception {
        Resource platform = this.pluginContainer.getInventoryManager().getPlatform();
        Map<ResourceType, Exception> resourceConfigLoadExceptionsByType = new LinkedHashMap<ResourceType, Exception>();
        findResourceConfigsThatFailToLoadRecursively(platform, resourceConfigLoadExceptionsByType);
        assertTrue(resourceConfigLoadExceptionsByType.isEmpty(), "Resource configs that failed to load by type: "
            + resourceConfigLoadExceptionsByType);
    }

    private void findResourceConfigsThatFailToLoadRecursively(Resource resource,
        Map<ResourceType, Exception> resourceConfigLoadExceptionsByType) throws Exception {
        ResourceType resourceType = resource.getResourceType();
        // Only check resource configs on types of Resources from the plugin under test.
        if (resourceType.getPlugin().equals(getPluginName())
            && (resourceType.getResourceConfigurationDefinition() != null)
            && !resourceConfigLoadExceptionsByType.containsKey(resourceType)) {
            ResourceContainer resourceContainer = this.pluginContainer.getInventoryManager().getResourceContainer(
                resource);
            if (resourceContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
                return;
            }

            Exception exception = null;
            try {
                Configuration resourceConfig = loadResourceConfiguration(resource);
                List<String> validationErrors = ConfigurationUtility.validateConfiguration(resourceConfig,
                    resourceType.getResourceConfigurationDefinition());
                if (!validationErrors.isEmpty()) {
                    exception = new Exception("Resource config is not valid: " + validationErrors.toString());
                }
            } catch (Exception e) {
                exception = e;
            }
            if (exception != null) {
                resourceConfigLoadExceptionsByType.put(resourceType, exception);
            }
        }

        // Recurse.
        for (Resource childResource : resource.getChildResources()) {
            findResourceConfigsThatFailToLoadRecursively(childResource, resourceConfigLoadExceptionsByType);
        }
    }

}
