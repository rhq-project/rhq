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
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
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
    private final Log log = LogFactory.getLog(this.getClass());
    static final String COMPONENT_NAME_PROPERTY = "componentName";

    // Attributes  --------------------------------------------
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
        ManagedComponent managedComponent;
        try {
            managedComponent = getManagedComponent();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load ManagedComponent.", e);
        }
        Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();
        Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(this.resourceContext.getPluginConfiguration());
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

            if (log.isDebugEnabled()) log.debug("BEFORE:\n" + DebugUtils.convertPropertiesToString(managedComponent));
            ConversionUtil.convertConfigurationToManagedProperties(managedProperties, resourceConfig, this.resourceType,
                    customProps);
            if (log.isDebugEnabled()) log.debug("AFTER:\n" + DebugUtils.convertPropertiesToString(managedComponent));

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
            String metricName = request.getName();
            ManagedProperty metricProperty = managedComponent.getProperty(metricName);
            ConversionUtil.convertMetricValuesToMeasurement(report, metricProperty, request, this.resourceType,
                    this.componentName);
        }
    }

    private ManagedComponent getManagedComponent() throws Exception
    {
        ProfileServiceFactory.refreshCurrentProfileView();
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        return ProfileServiceFactory.getManagedComponent(managementView, this.componentType, this.componentName);
    }
}
