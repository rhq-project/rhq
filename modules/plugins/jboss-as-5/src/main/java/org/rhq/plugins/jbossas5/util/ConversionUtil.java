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
package org.rhq.plugins.jbossas5.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.KnownComponentTypes;
import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedParameter;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.MetaValue;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.jbossas5.adapter.api.MeasurementAdapter;
import org.rhq.plugins.jbossas5.adapter.api.MeasurementAdapterFactory;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;

 /**
 * Utility class to convert some basic Profile Service objects to JON objects, and some basic
 * manipulation and data gathering of Profile Service objects.
 * <p/>
 * This should not include converting between Property objects and ManagedProperties. Those conversions
 * should be in the corresponding Adapter classes.
 *
 * @author Mark Spritzler
 */
public class ConversionUtil
{
    private static final Log LOG = LogFactory.getLog(ConversionUtil.class);

    // Key is the RHQ plugin ResourceType name. Make sure if the ResourceType name changes that this map changes too.
    private static final Map<String, ComponentType> KNOWN_COMPONENT_TYPES = new HashMap<String, ComponentType>();

    static {
        KNOWN_COMPONENT_TYPES.put("No Transaction", KnownComponentTypes.DataSourceTypes.NoTx.getType());
        KNOWN_COMPONENT_TYPES.put("Local Transaction", KnownComponentTypes.DataSourceTypes.LocalTx.getType());
        KNOWN_COMPONENT_TYPES.put("XA Transaction", KnownComponentTypes.DataSourceTypes.XA.getType());
        KNOWN_COMPONENT_TYPES.put("No TX ConnectionFactory", KnownComponentTypes.ConnectionFactoryTypes.NoTx.getType());
        KNOWN_COMPONENT_TYPES.put("Transaction ConnectionFactory", KnownComponentTypes.ConnectionFactoryTypes.XA.getType());
        KNOWN_COMPONENT_TYPES.put("Queue", KnownComponentTypes.JMSDestination.Queue.getType());
        KNOWN_COMPONENT_TYPES.put("Topic", KnownComponentTypes.JMSDestination.Topic.getType());
    }

    // Key is the RHQ plugin ResourceType name. Make sure if the ResourceType name changes that this map changes too.
    private static final Map<String, String> KNOWN_DEPLOYMENT_TYPES = new HashMap<String, String>();

    static {
        KNOWN_DEPLOYMENT_TYPES.put("Enterprise Application (EAR)", KnownDeploymentTypes.JavaEEApplication.getType());
        KNOWN_DEPLOYMENT_TYPES.put("Client Enterprise Application", KnownDeploymentTypes.JavaEEClientApplication.getType());
        KNOWN_DEPLOYMENT_TYPES.put("EJB 2.x Application", KnownDeploymentTypes.JavaEEEnterpriseBeans2x.getType());
        KNOWN_DEPLOYMENT_TYPES.put("EJB Application (JAR)", KnownDeploymentTypes.JavaEEEnterpriseBeans3x.getType());
        KNOWN_DEPLOYMENT_TYPES.put("Perisistence XML (PAR)", KnownDeploymentTypes.JavaEEPersistenceUnit.getType());
        KNOWN_DEPLOYMENT_TYPES.put("Resource Adaptor (RAR)", KnownDeploymentTypes.JavaEEResourceAdaptor.getType());
        KNOWN_DEPLOYMENT_TYPES.put("Web Application (WAR)", KnownDeploymentTypes.JavaEEWebApplication.getType());
        KNOWN_DEPLOYMENT_TYPES.put("JBoss Service (SAR)", KnownDeploymentTypes.JBossServices.getType());
        KNOWN_DEPLOYMENT_TYPES.put("Microcontainter Beans (.beans)", KnownDeploymentTypes.MCBeans.getType());
        KNOWN_DEPLOYMENT_TYPES.put("OSGI Bundle", KnownDeploymentTypes.OSGIBundle.getType());
        KNOWN_DEPLOYMENT_TYPES.put("Spring Application", KnownDeploymentTypes.SpringApplication.getType());
    }

    protected static final String PLUGIN = "ProfileService";

    public static final String COMPONENT_SUB_TYPE_PROPERTY = "componentSubType";
    public static final String COMPONENT_PARENT_TYPE_PROPERTY = "componentParentType";

    /**
     * Converts the ResourceType into a ComponentType
     *
     * @param resourceType resourceType to convert
     * @return ComponentType the componentType object
     */
    public static ComponentType getComponentType(ResourceType resourceType)
    {
        return getComponentType(resourceType.getName());
    }

    public static ComponentType getComponentType(String resourceType)
    {
        return KNOWN_COMPONENT_TYPES.get(resourceType);
    }

    public static String getDeploymentTypeString(ResourceType resourceType)
    {
        return getDeploymentTypeString(resourceType.getName());
    }

    public static String getDeploymentTypeString(String resourceType)
    {
        return KNOWN_DEPLOYMENT_TYPES.get(resourceType);
    }

    public static Configuration convertManagedObjectToConfiguration(Map<String, ManagedProperty> managedProperties, Map<String, PropertySimple> customProps, ResourceType resourceType)
    {
        Configuration config = new Configuration();
        ConfigurationDefinition configDef = resourceType.getResourceConfigurationDefinition();

        /* Deal with the custom properties first, then hold on to the name of these properties, so that
           the loop will skip over a property if it is in the customKeys set. */
        handleCustomPropertiesFromManagedProperties(managedProperties, customProps, config, configDef);

        Map<String, PropertyDefinition> configurationProperties = configDef.getPropertyDefinitions();

        Set<String> keys = managedProperties.keySet();
        for (String key : keys)
        {
            if (customProps.containsKey(key))
            {
                continue;
            }

            PropertyDefinition definition = configurationProperties.get(key);
            ManagedProperty managedProperty = managedProperties.get(key);
            if (managedProperty != null && definition != null)
            {
                MetaValue metaValue = null;
                try
                {
                    // Temporary check to see which properties are returning MetaValue object and which aren't
                    if (managedProperty.getValue() != null && !(managedProperty.getValue() instanceof MetaValue))
                    {
                        LOG.debug("convertManagedObjectToConfiguration: ManagedProperty.getValue() returned: " + managedProperty.getValue().getClass().getSimpleName()
                                + " for property: " + key);
                    }
                    metaValue = (MetaValue) managedProperty.getValue();
                }
                catch (ClassCastException e)
                {
                    LOG.error("convertManagedObjectToConfiguration: ManagedProperty.getValue() did not return a MetaValue, it returned: " + managedProperty.getValue().getClass().getSimpleName()
                            + " for property: " + key, e);
                }

                if (metaValue != null)
                {
                    PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);

                    if (propertyAdapter == null)
                    {
                        LOG.error("Unable to get the PropertyAdapter from the factory for type: " + metaValue.getMetaType().getTypeName() + " for ResourceType: " + resourceType.getName());
                    }

                    Property property = customProps.get(key);
                    if (property != null)
                    {
                        propertyAdapter.setPropertyValues(property, metaValue, definition);
                    }
                    else
                    {
                        property = propertyAdapter.getProperty(metaValue, definition);
                        property.setName(definition.getName());
                    }

                    config.put(property);
                }
            }
        }
        return config;
    }

    private static void handleCustomPropertiesFromManagedProperties(Map<String, ManagedProperty> managedProperties,
                                                                    Map<String, PropertySimple> customProps,
                                                                    Configuration config,
                                                                    ConfigurationDefinition configDef)
    {
        for (PropertySimple customProp : customProps.values())
        {
            ManagedProperty managedProperty = managedProperties.get(customProp.getName());
            if (managedProperty != null)
            {
                PropertyAdapter propAdapter = PropertyAdapterFactory.getCustomPropertyAdapter(customProp);
                Property prop = config.get(customProp.getName());
                PropertyDefinition propDef = configDef.getPropertyDefinitions().get(customProp.getName());
                if (prop != null)
                {
                    propAdapter.setPropertyValues(prop, (MetaValue) managedProperty.getValue(), propDef);
                }
                else
                {
                    prop = propAdapter.getProperty((MetaValue) managedProperty.getValue(), propDef);
                    prop.setName(customProp.getName());
                }
                config.put(prop);
            }
        }
    }

    public static void convertConfigurationToManagedProperties(Map<String, ManagedProperty> managedProperties, Configuration configuration, ResourceType resourceType, Map<String, PropertySimple> customProps)
    {
        /* Deal with the custom properties first, then hold on to the name of these properties so that
           the loop will skip over a property if it is the customKeys set */
        handleCustomProperties(managedProperties, configuration, customProps);

        ConfigurationDefinition configDefinition = resourceType.getResourceConfigurationDefinition();
        Map<String, PropertyDefinition> configurationProperties = configDefinition.getPropertyDefinitions();
        for (Property property : configuration.getProperties())
        {
            String key = property.getName();
            if (customProps.containsKey(key))
                continue;

            ManagedProperty managedProperty = managedProperties.get(key);

            if (managedProperty != null)
            {
                MetaValue metaValue = null;
                try
                {
                    // Temporary check to see which properties are returning MetaValue object and which aren't
                    if (managedProperty.getValue() != null)
                    {
                        LOG.debug("convertConfigurationToManagedProperties: ManagedProperty.getValue() returned: " + managedProperty.getValue().getClass().getSimpleName()
                                + " for property: " + key);
                    }
                    metaValue = (MetaValue) managedProperty.getValue();
                }
                catch (ClassCastException e)
                {
                    LOG.error("convertConfigurationToManagedProperties: ManagedProperty.getValue() did not return a MetaValue, it returned: " + managedProperty.getValue().getClass().getSimpleName()
                            + " for property: " + key, e);
                }

                PropertyDefinition definition = configurationProperties.get(key);
                PropertyAdapter propertyAdapter;
                if (metaValue != null)
                {
                    propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);
                    propertyAdapter.setMetaValues(property, metaValue, definition);
                }
                else
                {
                    MetaType metaType = managedProperty.getMetaType();
                    propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaType);
                    metaValue = propertyAdapter.getMetaValue(property, definition, metaType);
                    if (metaValue != null)
                    {
                        managedProperty.setValue(metaValue);
                    }
                }
            }
        }
        return;
    }

    private static void handleCustomProperties(Map<String, ManagedProperty> managedProperties, Configuration resourceConfig, Map<String, PropertySimple> customProps)
    {
        for (PropertySimple customProp : customProps.values())
        {
            ManagedProperty managedProperty = managedProperties.get(customProp.getName());
            // NOTE: For now, we assume that the custom property name refers to a top-level property.
            Property property = resourceConfig.get(customProp.getName());
            if (managedProperty != null && property != null)
            {
                PropertyAdapter propertyAdapter = PropertyAdapterFactory.getCustomPropertyAdapter(customProp);
                propertyAdapter.setMetaValues(property, (MetaValue) managedProperty.getValue(), null);
            }
        }
    }

    /**
     * Takes the Configuration parameter object and converts it into a MetaValue array, which can them be passed
     * in with the invoke method to the ProfileService to fire the operation of a resource.
     *
     * @param managedOperation Operation that will be fired and stores the parameter types for the operation
     * @param parameters       set of Parameter Values that the OperationFacet sent to the Component
     * @param resourceType     resourceType to get the Operation definitions from
     * @return MetaValue[] array of MetaValues representing the parameters
     */
    public static MetaValue[] convertOperationsParametersToMetaValues(ManagedOperation managedOperation, Configuration parameters, ResourceType resourceType)
    {
        ManagedParameter[] managedParameters = managedOperation.getParameters();
        OperationDefinition operation = null;
        String operationName = managedOperation.getName();

        for (OperationDefinition operationDefinition : resourceType.getOperationDefinitions())
        {
            if (operationDefinition.getName().equals(operationName))
            {
                operation = operationDefinition;
                break;
            }
        }
        if (operation != null)
        {
            ConfigurationDefinition configurationDefinition = operation.getParametersConfigurationDefinition();
            if (configurationDefinition != null)
            {
                Map<String, PropertyDefinition> resultPropertyDefinitions = configurationDefinition.getPropertyDefinitions();

                for (ManagedParameter managedParameter : managedParameters)
                {
                    String name = managedParameter.getName();

                    PropertyDefinition parameterPropertyDefinition = resultPropertyDefinitions.get(name);
                    Property parameter = parameters.get(name);
                    MetaType type = managedParameter.getMetaType();
                    //ManagedParameter should have a MetaValue object returned
                    PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(type);
                    propertyAdapter.setMetaValues(parameter, (MetaValue) managedParameter.getValue(), parameterPropertyDefinition);
                }
            }
        }
        return null;
    }

    public static void convertManagedOperationResults(ManagedOperation operation, MetaValue result, Configuration complexResults, ResourceType resourceType)
    {
        OperationDefinition operationDefinition = getOperationDefinition(resourceType, operation.getName());
        if (operationDefinition == null) {
            LOG.warn("ConversionUtil was not able to find the operation " + operation.getName()
                    + ", so no results can be reported.");
            return;
        }
        ConfigurationDefinition resultDefinition = operationDefinition.getResultsConfigurationDefinition();
        if (resultDefinition != null)
        {
            Map<String, PropertyDefinition> resultPropertyDefinitions = resultDefinition.getPropertyDefinitions();
            PropertyDefinition propertyDefinition = null;
            // There should and must be only one definition to map to the results from the Profile Service,
            // otherwise there will be a huge mismatch
            for (PropertyDefinition definition : resultPropertyDefinitions.values())
            {
                propertyDefinition = definition;
            }

            MetaType type = operation.getReturnType();
            PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(type);

            if (propertyDefinition != null)
            {
                Property property = propertyAdapter.getProperty(result, propertyDefinition);
                complexResults.put(property);
            }
        }
    }

    private static OperationDefinition getOperationDefinition(ResourceType resourceType, String operationName) {
        Set<OperationDefinition> operationDefinitions = resourceType.getOperationDefinitions();
        OperationDefinition operationDefinition = null;
        for (OperationDefinition definition : operationDefinitions)
        {
            if (definition.getName().equals(operationName))
            {
                operationDefinition = definition;
                break;
            }
        }
        return operationDefinition;
    }

    public static void convertMetricValuesToMeasurement(MeasurementReport report, ManagedProperty metricProperty, MeasurementScheduleRequest request, ResourceType resourceType, String deploymentName)
    {
        String metricName = metricProperty.getName();
        MetaType type = metricProperty.getMetaType();
        MetaValue value = (MetaValue) metricProperty.getValue();
        if (value != null)
        {
            MeasurementAdapter measurementAdapter = MeasurementAdapterFactory.getMeasurementPropertyAdapter(type);
            MeasurementDefinition measurementDefinition = getMeasurementDefinition(resourceType, metricName);
            if (measurementDefinition != null)
            {
                measurementAdapter.setMeasurementData(report, value, request, measurementDefinition);
            }
        }
        else
        {
            LOG.debug("Unable to obtain metric data for resource: " + deploymentName + " metric: " + metricName);
        }
    }

    private static MeasurementDefinition getMeasurementDefinition(ResourceType resourceType, String metricName) {
        MeasurementDefinition measurementDefinition = null;
        for (MeasurementDefinition definition : resourceType.getMetricDefinitions())
        {
            if (definition.getName().equals(metricName))
            {
                measurementDefinition = definition;
                break;
            }
        }
        return measurementDefinition;
    }
}
