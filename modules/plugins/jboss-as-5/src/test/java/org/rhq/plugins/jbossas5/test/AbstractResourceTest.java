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
package org.rhq.plugins.jbossas5.test;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.configuration.ConfigurationManager;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.agent.PluginContainerException;

/**
 * @author Ian Springer
 */
public abstract class AbstractResourceTest extends AbstractPluginTest {
    private static final long MEASUREMENT_FACET_METHOD_TIMEOUT = 3000; // 3 seconds

    protected abstract ResourceType getResourceType();

    protected ResourceType getServerResourceType() {
        return new ResourceType("JBossAS Server", getPluginName(), ResourceCategory.SERVER, null);
    }

    protected Set<Resource> getResources() {
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        return inventoryManager.getResourcesWithType(getResourceType());
    }

    @Test
    public void testDiscovery() throws Exception {
        Set<Resource> resources = getResources();
        System.out.println("Found " + resources.size() + " " + getResourceType().getName() + " Resources.");
        assert !resources.isEmpty();
    }

    @Test
    public void testMetrics() throws Exception {
        Set<MeasurementDefinition> metricDefinitions = getResourceType().getMetricDefinitions();
        Set<Resource> resources = getResources();
        for (Resource resource : resources) {
            System.out.println("Validating metrics for " + resource + "...");
            MeasurementFacet measurementFacet = ComponentUtil.getComponent(resource.getId(), MeasurementFacet.class, FacetLockType.READ,
                    MEASUREMENT_FACET_METHOD_TIMEOUT, true, true);
            for (MeasurementDefinition metricDefinition : metricDefinitions) {
                String name = metricDefinition.getName();
                DataType dataType = metricDefinition.getDataType();
                if (dataType == DataType.MEASUREMENT || metricDefinition.getDataType() == DataType.TRAIT) {
                    MeasurementReport report = new MeasurementReport();
                    Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>();
                    MeasurementScheduleRequest request = new MeasurementScheduleRequest(1, metricDefinition.getName(),
                            0, true, metricDefinition.getDataType());
                    requests.add(request);
                    measurementFacet.getValues(report, requests);
                    if (dataType == DataType.MEASUREMENT) {
                        assert report.getNumericData().isEmpty() || report.getNumericData().size() == 1;
                        assert report.getTraitData().isEmpty();
                        assert report.getCallTimeData().isEmpty();
                        MeasurementDataNumeric dataNumeric = (report.getNumericData().isEmpty()) ?
                                null : report.getNumericData().iterator().next();
                        Double value = (dataNumeric != null) ? dataNumeric.getValue() : null;
                        System.out.println("Validating numeric metric '" + name + "' value (" + value + ")...");
                        validateNumericMetricValue(metricDefinition.getName(), value);
                    }
                    else if (metricDefinition.getDataType() == DataType.TRAIT) {
                        assert report.getTraitData().isEmpty() || report.getTraitData().size() == 1;
                        assert report.getNumericData().isEmpty();
                        assert report.getCallTimeData().isEmpty();
                        MeasurementDataTrait dataTrait = (report.getTraitData().size() == 1) ?
                                report.getTraitData().iterator().next() : null;
                        String value = (dataTrait != null) ? dataTrait.getValue() : null;
                        System.out.println("Validating trait '" + name + "' value (" + value + ")...");
                        validateTraitMetricValue(metricDefinition.getName(), value);
                    }
                }
            }
        }
        return;
    }

    @Test
    public void testResourceConfigLoad() throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        Set<Resource> resources = getResources();
        for (Resource resource : resources) {
            try {
                Configuration resourceConfig = configurationManager.loadResourceConfiguration(resource.getId());
                // TODO: Validate the properties?
            }
            catch (Exception e) {
                throw new Exception("Failed to load Resource config for " + resource + ".", e);
            }
        }
        return;
    }

    @Test
    public void testResourceConfigUpdate() throws Exception {
        ConfigurationManager configurationManager = PluginContainer.getInstance().getConfigurationManager();
        Set<Resource> resources = getResources();
        for (Resource resource : resources) {
            try {
                Configuration testResourceConfig = getTestResourceConfiguration();
                ConfigurationUpdateRequest configurationUpdateRequest = new ConfigurationUpdateRequest(0,
                        testResourceConfig, resource.getId());
                configurationManager.updateResourceConfiguration(configurationUpdateRequest);
                Configuration resourceConfig = configurationManager.loadResourceConfiguration(resource.getId());
                assert resourceConfig.equals(testResourceConfig);
            }
            catch (Exception e) {
                throw new Exception("Failed to update Resource config for " + resource + ".", e);
            }
        }
        return;
    }

    protected abstract Configuration getTestResourceConfiguration();

    protected void validateNumericMetricValue(String metricName, Double value) {
        assert value != null;
    }

    protected void validateTraitMetricValue(String metricName, String value) {
        assert value != null;
    }
}
