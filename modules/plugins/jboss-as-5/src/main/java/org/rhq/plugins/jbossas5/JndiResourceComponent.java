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
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.rhq.plugins.jbossas5.util.ConversionUtil;

import java.util.Map;
import java.util.Set;

/**
 * Service ResourceComponent for all services in the Profile Service
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 */
public class JndiResourceComponent
        implements ResourceComponent, ConfigurationFacet, DeleteResourceFacet, OperationFacet, MeasurementFacet
{

    private final Log LOG = LogFactory.getLog(JndiResourceComponent.class);
    protected final String DEPLOYMENT_PROPERTY_NAME = "deploymentName";

    // Attributes  --------------------------------------------
    private String deploymentName;
    private ComponentType componentType;
    private ResourceContext resourceContext;
    private ResourceType resourceType;

    // ResourceComponent Implementation  --------------------------------------------

    public AvailabilityType getAvailability()
    {
        return null;
    }


    public void start(ResourceContext resourceContext)
    {
        this.resourceContext = resourceContext;
        resourceType = resourceContext.getResourceType();

        // Convert the resource type into the component type
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();
        deploymentName = pluginConfiguration.getSimple(DEPLOYMENT_PROPERTY_NAME).getStringValue();
        componentType = ConversionUtil.getComponentType(resourceType);
    }

    public void stop()
    {

    }

    // ConfigurationComponent Implementation  --------------------------------------------

    public Configuration loadResourceConfiguration()
    {
        Configuration configuration = resourceContext.getPluginConfiguration();
        ManagementView profileView = ProfileServiceFactory.getCurrentProfileView();
        try
        {
            //resourceContext.getResourceKey();
            ManagedComponent managedComponent =
                    ProfileServiceFactory.getManagedComponent(profileView,
                            componentType, deploymentName);

            Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();

            ConversionUtil.convertManagedObjectToConfiguration(managedProperties, configuration, resourceType);

        }
        catch (Exception e)
        {
            LOG.error("Unable to load Resource " + deploymentName, e);
        }

        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport)
    {
        Configuration configuration = configurationUpdateReport.getConfiguration();
        ManagementView mgtView = ProfileServiceFactory.getCurrentProfileView();
        try
        {
            String componentName = configuration.getSimple(DEPLOYMENT_PROPERTY_NAME).getStringValue();
            ManagedComponent managedComponent = ProfileServiceFactory.getManagedComponent(mgtView, componentType, componentName);
            Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();

            ConversionUtil.convertConfigurationToManagedProperties(managedProperties, configuration, resourceType);

            mgtView.updateComponent(managedComponent);
            mgtView.process();

            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        }
        catch (Exception e)
        {
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
        }

    }

    // DeleteResourceFacet Implementation  --------------------------------------------

    public void deleteResource() throws Exception
    {
        ManagementView mgtView = ProfileServiceFactory.getCurrentProfileView();
        ManagedComponent component = getManagedComponent();
        ManagedDeployment deployment = component.getDeployment();
        mgtView.removeDeployment(deployment.getName(), ManagedDeployment.DeploymentPhase.APPLICATION);
        mgtView.process();
    }

    // OperationFacet Implementation  --------------------------------------------

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception
    {
        OperationResult results = new OperationResult();
        ManagedComponent component = getManagedComponent();
        Set<ManagedOperation> operations = component.getOperations();
        for (ManagedOperation operation : operations)
        {
            String operationName = operation.getName();
            if (operationName.equals(name))
            {
                //Convert parameters into MetaValue[]

                MetaValue[] params = ConversionUtil.convertOperationsParametersToMetaValues(operation, parameters, resourceType);
                if (params == null)
                {
                    params = new MetaValue[0];
                }
                Object result = operation.invoke(params);
                //Convert result to Correct Property type
                Configuration complexResults = results.getComplexResults();
                ConversionUtil.convertManagedOperationResults(operation, (MetaValue) result, complexResults, resourceType);
            }
        }
        return results;
    }

    public void startOperationFacet(OperationContext context)
    {
    }

    // MeasurementFacet Implementation  --------------------------------------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception
    {
        ManagedComponent component = getManagedComponent();
        for (MeasurementScheduleRequest request : metrics)
        {
            String metricName = request.getName();
            ManagedProperty metricProperty = component.getProperty(metricName);
            ConversionUtil.convertMetricValuesToMeasurement(report, metricProperty, request, resourceType, deploymentName);
        }
    }


    private ManagedComponent getManagedComponent() throws Exception
    {
        ManagementView mgtView = ProfileServiceFactory.getCurrentProfileView();
        ComponentType type = ConversionUtil.getComponentType(resourceType);
        return ProfileServiceFactory.getManagedComponent(mgtView, type, deploymentName);
    }

}
