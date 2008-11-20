 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.CompositeValue;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.rhq.plugins.jbossas5.util.ConversionUtil;
import org.rhq.plugins.jbossas5.util.DebugUtils;

import java.util.Map;
import java.util.Set;

/**
 * Service ResourceComponent for all {@link ManagedComponent}s in a Profile.
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class JndiResourceComponent
        implements ResourceComponent, ConfigurationFacet, DeleteResourceFacet, OperationFacet, MeasurementFacet
{
    static final String COMPONENT_NAME_PROPERTY = "componentName";

    private final Log log = LogFactory.getLog(this.getClass());

    private String componentName;
    private ComponentType componentType;
    private ResourceContext resourceContext;
    private ResourceType resourceType;

    // ResourceComponent Implementation  --------------------------------------------

    public AvailabilityType getAvailability()
    {
        // TODO (ips, 11/10/08): Verify this is the correct way to check availablity.
        try {
            return (getManagedComponent() != null) ? AvailabilityType.UP : AvailabilityType.DOWN;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start(ResourceContext resourceContext)
    {
        this.resourceContext = resourceContext;
        this.resourceType = resourceContext.getResourceType();

        // Convert the resource type into the component type
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();
        this.componentName = pluginConfiguration.getSimple(COMPONENT_NAME_PROPERTY).getStringValue();
        this.componentType = ConversionUtil.getComponentType(resourceType);
    }

    public void stop()
    {
        return;
    }

    // ConfigurationComponent Implementation  --------------------------------------------

    public Configuration loadResourceConfiguration()
    {
        ManagedComponent managedComponent = getManagedComponent();
        Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();
        Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(this.resourceContext.getPluginConfiguration());
        if (log.isDebugEnabled()) log.debug("*** BEFORE LOAD:\n" + DebugUtils.convertPropertiesToString(managedComponent));
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        Configuration resourceConfig = ConversionUtil.convertManagedObjectToConfiguration(managedProperties,
                customProps, this.resourceType);
        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport)
    {
        Configuration resourceConfig = configurationUpdateReport.getConfiguration();
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        try
        {
            ManagedComponent managedComponent = getManagedComponent();
            Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(pluginConfig);
            if (log.isDebugEnabled()) log.debug("*** BEFORE UPDATE:\n" + DebugUtils.convertPropertiesToString(managedComponent));
            ConversionUtil.convertConfigurationToManagedProperties(managedProperties, resourceConfig, this.resourceType,
                    customProps);
            if (log.isDebugEnabled()) log.debug("*** AFTER UPDATE:\n" + DebugUtils.convertPropertiesToString(managedComponent));
            managementView.updateComponent(managedComponent);
            managementView.process();
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        }
        catch (Exception e)
        {
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
            configurationUpdateReport.setErrorMessageFromThrowable(e);
        }
    }

    // DeleteResourceFacet Implementation  --------------------------------------------

    public void deleteResource() throws Exception
    {
        log.debug("Deleting ManagedComponent [" + this.componentName + "]...");
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        ManagedComponent managedComponent = getManagedComponent();
        ManagedDeployment deployment = managedComponent.getDeployment();
        deployment.removeComponent(this.componentName);
        managementView.process();
    }

    // OperationFacet Implementation  --------------------------------------------

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception
    {
        OperationResult results = new OperationResult();
        ManagedComponent managedComponent = getManagedComponent();
        Set<ManagedOperation> operations = managedComponent.getOperations();
        for (ManagedOperation operation : operations)
        {
            String operationName = operation.getName();
            if (operationName.equals(name))
            {
                // Convert parameters into MetaValue array.
                MetaValue[] params = ConversionUtil.convertOperationsParametersToMetaValues(operation, parameters,
                        this.resourceType);
                if (params == null)
                    params = new MetaValue[0];
                Object result = operation.invoke(params);
                //Convert result to Correct Property type
                Configuration complexResults = results.getComplexResults();
                ConversionUtil.convertManagedOperationResults(operation, (MetaValue) result, complexResults,
                        this.resourceType);
            }
        }
        return results;
    }

    // MeasurementFacet Implementation  --------------------------------------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception
    {
        ManagedComponent managedComponent = getManagedComponent();
        for (MeasurementScheduleRequest request : metrics)
        {
            try {
                SimpleValue simpleValue = getSimpleValue(managedComponent, request);
                if (simpleValue != null)
                    addSimpleValueToMeasurementReport(report, request, simpleValue);
            }
            catch (Exception e) {
                log.error("Failed to collect metric for " + request, e);
            }
        }
    }

    // ------------------------------------------------------------------------------

    private SimpleValue getSimpleValue(ManagedComponent managedComponent, MeasurementScheduleRequest request) {
        String metricName = request.getName();
        int dotIndex = metricName.indexOf('.');
        String metricPropName = (dotIndex == -1) ? metricName : metricName.substring(0, dotIndex);
        ManagedProperty metricProp = managedComponent.getProperty(metricPropName);
        SimpleValue simpleValue;
        if (dotIndex == -1)
            simpleValue = (SimpleValue)metricProp.getValue();
        else {
            CompositeValue compositeValue = (CompositeValue)metricProp.getValue();
            String key = metricName.substring(dotIndex + 1);
            simpleValue = (SimpleValue)compositeValue.get(key);
        }
        if (simpleValue == null)
            log.debug("Profile service returned null value for metric [" + request.getName() + "].");
        return simpleValue;
    }

    private void addSimpleValueToMeasurementReport(MeasurementReport report, MeasurementScheduleRequest request, SimpleValue simpleValue) {
        DataType dataType = request.getDataType();
        switch (dataType) {
            case MEASUREMENT:
                try {
                    MeasurementDataNumeric dataNumeric = new MeasurementDataNumeric(request,
                            Double.valueOf(simpleValue.getValue().toString()));
                    report.addData(dataNumeric);
                }
                catch (NumberFormatException e) {
                    log.error("Profile service did not return a numeric value as expected for metric ["
                            + request.getName() + "] - value returned was " + simpleValue + ".", e);
                }
                break;
            case TRAIT:
                MeasurementDataTrait dataTrait = new MeasurementDataTrait(request,
                        String.valueOf(simpleValue.getValue()));
                report.addData(dataTrait);
                break;
            default:
                throw new IllegalStateException("Unsupported measurement data type: " + dataType);
        }
    }

    private ManagedComponent getManagedComponent()
    {
        try {
            ProfileServiceFactory.refreshCurrentProfileView();
            ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
            return ProfileServiceFactory.getManagedComponent(managementView, this.componentType, this.componentName);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load ManagedComponent [" + this.componentName + "].", e);
        }
    }
}
