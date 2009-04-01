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

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedParameter;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.ComponentType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.MapCompositeMetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.types.CompositeMetaType;
import org.jboss.metatype.api.values.MetaValue;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.jbossas5.adapter.api.MeasurementAdapter;
import org.rhq.plugins.jbossas5.adapter.api.MeasurementAdapterFactory;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.rhq.plugins.jbossas5.ManagedComponentComponent;
import org.rhq.plugins.jbossas5.ManagedDeploymentComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

 /**
 * Utility class to convert some basic Profile Service objects to JON objects, and some basic
 * manipulation and data gathering of Profile Service objects.
 * <p/>
 * This should not include converting between Property objects and ManagedProperties. Those conversions
 * should be in the corresponding Adapter classes.
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class ConversionUtils
{
    private static final Log LOG = LogFactory.getLog(ConversionUtils.class);

    private static final Map<String, ComponentType> COMPONENT_TYPE_CACHE = new HashMap<String, ComponentType>();
    private static final Map<String, KnownDeploymentTypes> DEPLOYMENT_TYPE_CACHE = new HashMap<String, KnownDeploymentTypes>();
    private static final Map<String, Configuration> DEFAULT_PLUGIN_CONFIG_CACHE = new HashMap<String, Configuration>();

    protected static final String PLUGIN = "ProfileService";

    public static ComponentType getComponentType(@NotNull ResourceType resourceType) {
        String resourceTypeName = resourceType.getName();
        if (COMPONENT_TYPE_CACHE.containsKey(resourceTypeName))
            return COMPONENT_TYPE_CACHE.get(resourceTypeName);

        Configuration defaultPluginConfig;
        if (DEFAULT_PLUGIN_CONFIG_CACHE.containsKey(resourceTypeName))
            defaultPluginConfig = DEFAULT_PLUGIN_CONFIG_CACHE.get(resourceTypeName);
        else {
            defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
            DEFAULT_PLUGIN_CONFIG_CACHE.put(resourceTypeName, defaultPluginConfig);
        }

        String type = defaultPluginConfig.getSimpleValue(ManagedComponentComponent.COMPONENT_TYPE_PROPERTY, null);
        if (type == null || type.equals(""))
            throw new IllegalStateException("Required plugin configuration property '"
                    + ManagedComponentComponent.COMPONENT_TYPE_PROPERTY + "' is not defined in default template.");
        String subtype = defaultPluginConfig.getSimpleValue(ManagedComponentComponent.COMPONENT_SUBTYPE_PROPERTY, null);
        if (subtype == null || subtype.equals(""))
            throw new IllegalStateException("Required plugin configuration property '"
                    + ManagedComponentComponent.COMPONENT_SUBTYPE_PROPERTY + "' is not defined in default template.");
        ComponentType componentType = new ComponentType(type, subtype);
        COMPONENT_TYPE_CACHE.put(resourceTypeName, componentType);
        return componentType;
    }

    public static KnownDeploymentTypes getDeploymentType(@NotNull ResourceType resourceType) {
        String resourceTypeName = resourceType.getName();
        if (DEPLOYMENT_TYPE_CACHE.containsKey(resourceTypeName))
            return DEPLOYMENT_TYPE_CACHE.get(resourceTypeName);

        Configuration defaultPluginConfig;
        if (DEFAULT_PLUGIN_CONFIG_CACHE.containsKey(resourceTypeName))
            defaultPluginConfig = DEFAULT_PLUGIN_CONFIG_CACHE.get(resourceTypeName);
        else {
            defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
            DEFAULT_PLUGIN_CONFIG_CACHE.put(resourceTypeName, defaultPluginConfig);
        }

        String typeName = defaultPluginConfig.getSimpleValue(ManagedDeploymentComponent.DEPLOYMENT_TYPE_NAME_PROPERTY, null);
        if (typeName == null || typeName.equals(""))
            throw new IllegalStateException("Required plugin configuration property '"
                    + ManagedComponentComponent.COMPONENT_TYPE_PROPERTY + "' is not defined in default template.");
        KnownDeploymentTypes deploymentType = KnownDeploymentTypes.valueOf(typeName);
        DEPLOYMENT_TYPE_CACHE.put(resourceTypeName, deploymentType);
        return deploymentType;
    }

    private static Configuration getDefaultPluginConfiguration(ResourceType resourceType) {
        ConfigurationDefinition definition = resourceType.getPluginConfigurationDefinition();
        if (definition != null) {
            ConfigurationTemplate template = definition.getDefaultTemplate();
            if (template != null) {
                return template.getConfiguration().deepCopy();
            }
        }
        return new Configuration(); // there is no default plugin config defined - return an empty one
    }

    public static Configuration convertManagedObjectToConfiguration(Map<String, ManagedProperty> managedProperties, Map<String, PropertySimple> customProps, ResourceType resourceType)
    {
        Configuration config = new Configuration();
        ConfigurationDefinition configDef = resourceType.getResourceConfigurationDefinition();

        /* Deal with the custom properties first, then hold on to the name of these properties, so that
           the loop will skip over a property if it is in the customKeys set. */
        handleCustomPropertiesFromManagedProperties(managedProperties, customProps, config, configDef);

        Map<String, PropertyDefinition> configurationProperties = configDef.getPropertyDefinitions();

        Set<String> propNames = managedProperties.keySet();
        for (String propName : propNames)
        {
            if (customProps.containsKey(propName))
                continue;

            PropertyDefinition propertyDefinition = configurationProperties.get(propName);
            ManagedProperty managedProperty = managedProperties.get(propName);
            if (managedProperty != null && propertyDefinition != null)
            {
                MetaValue metaValue = managedProperty.getValue();
                if (metaValue != null)
                {
                    PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);
                    if (propertyAdapter == null)
                    {
                        LOG.error("Unable to get the PropertyAdapter from the factory for type: "
                                + metaValue.getMetaType().getTypeName() + " for ResourceType: " + resourceType.getName());
                        continue;
                    }
                    LOG.debug("Converting MetaValue [" + metaValue + "] to property of type [" + propertyDefinition
                            + "] using adapter [" + propertyAdapter.getClass().getSimpleName() + "]...");
                    Property customProp = customProps.get(propName);
                    if (customProp != null)
                    {
                        propertyAdapter.populatePropertyFromMetaValue(customProp, metaValue, propertyDefinition);
                    }
                    else
                    {
                        customProp = propertyAdapter.convertToProperty(metaValue, propertyDefinition);
                        customProp.setName(propertyDefinition.getName());
                    }
                    config.put(customProp);
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
                    propAdapter.populatePropertyFromMetaValue(prop, managedProperty.getValue(), propDef);
                }
                else
                {
                    prop = propAdapter.convertToProperty(managedProperty.getValue(), propDef);
                    prop.setName(customProp.getName());
                }
                config.put(prop);
            }
        }
    }

    public static void convertConfigurationToManagedProperties(Map<String, ManagedProperty> managedProperties,
                                                               Configuration configuration, ResourceType resourceType,
                                                               Map<String, PropertySimple> customProps)
    {
        ConfigurationDefinition configDefinition = resourceType.getResourceConfigurationDefinition();
        // Deal with the custom properties first, then hold on to the names of these properties so the processing
        // loop can skip over them.
        handleCustomProperties(managedProperties, configuration, configDefinition, customProps);

        for (Property property : configuration.getProperties())
        {
            String propertyName = property.getName();
            if (customProps.containsKey(propertyName))
                continue;
            ManagedProperty managedProperty = managedProperties.get(propertyName);
            PropertyDefinition propertyDefinition = configDefinition.get(propertyName);
            if (managedProperty == null) {
                // TODO: Do we want to attempt to build a ManagedProperty from scratch based on the PropertyDefinition?
                //       I don't think so - it's too difficult, since a propDef could map to multiple different types
                //       of ManagedProperties. The safest thing is for the profile service to always return templates
                //       that contain *all* ManagedProperties that are defined for the ComponentType.
                /*ManagedPropertyImpl managedPropertyImpl = new ManagedPropertyImpl(propertyName);
                managedProperty = managedPropertyImpl;
                managedProperty.setManagedObject(null);
                if (propertyDefinition instanceof PropertyDefinitionSimple) {
                    PropertyDefinitionSimple propertyDefinitionSimple = (PropertyDefinitionSimple)propertyDefinition;
                    switch (propertyDefinitionSimple.getType()) {
                        case INTEGER:
                    }
                }
                managedPropertyImpl.setMetaType(null);*/
                LOG.error("***** ManagedProperty named '" + propertyName + "' not found.");
            }
            else {
                convertPropertyToManagedProperty(property, propertyDefinition, managedProperty);
            }
        }
        return;
    }

    private static void convertPropertyToManagedProperty(Property property, PropertyDefinition propertyDefinition,
                                                         ManagedProperty managedProperty)
    {
        if (managedProperty != null)
        {
            MetaValue metaValue = managedProperty.getValue();
            if (metaValue != null)
            {
                LOG.debug("Populating existing MetaValue of type " + metaValue.getMetaType()
                        + " from RHQ property " + property + " with definition " + propertyDefinition + "...");
                PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);
                propertyAdapter.populateMetaValueFromProperty(property, metaValue, propertyDefinition);
            }
            else
            {
                MetaType metaType = managedProperty.getMetaType();
                PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaType);
                LOG.debug("Converting property " + property + " with definition " + propertyDefinition
                        + " to MetaValue of type " + metaType + "...");
                metaValue = propertyAdapter.convertToMetaValue(property, propertyDefinition, metaType);
                managedProperty.setValue(metaValue);
            }
        }
    }

    public static MetaType convertPropertyDefinitionToMetaType(PropertyDefinition propDef) {
        MetaType memberMetaType;
        if (propDef instanceof PropertyDefinitionSimple) {
            PropertySimpleType propSimpleType = ((PropertyDefinitionSimple)propDef).getType();
            memberMetaType = convertPropertySimpleTypeToSimpleMetaType(propSimpleType);
        } else if (propDef instanceof PropertyDefinitionList) {
            // TODO (very low priority, since lists of lists are not going to be at all common)
            memberMetaType = null;
        } else if (propDef instanceof PropertyDefinitionMap) {
            Map<String,PropertyDefinition> memberPropDefs = ((PropertyDefinitionMap)propDef).getPropertyDefinitions();
            if (memberPropDefs.isEmpty())
                throw new IllegalStateException("PropertyDefinitionMap doesn't contain any member PropertyDefinitions.");
            // NOTE: We assume member prop defs are all of the same type, since for MapCompositeMetaTypes, they have to be.
            PropertyDefinition mapMemberPropDef = memberPropDefs.values().iterator().next();
            MetaType mapMemberMetaType = convertPropertyDefinitionToMetaType(mapMemberPropDef);
            memberMetaType = new MapCompositeMetaType(mapMemberMetaType);
        } else {
            throw new IllegalStateException("List member PropertyDefinition has unknown type: "
                    + propDef.getClass().getName());
        }
        return memberMetaType;
    }

    private static MetaType convertPropertySimpleTypeToSimpleMetaType(PropertySimpleType memberSimpleType) {
        MetaType memberMetaType;
        Class memberClass;
        switch (memberSimpleType) {
            case BOOLEAN: memberClass = Boolean.class; break;
            case INTEGER: memberClass = Integer.class; break;
            case LONG: memberClass = Long.class; break;
            case FLOAT: memberClass = Float.class; break;
            case DOUBLE: memberClass = Double.class; break;
            default: memberClass = String.class; break;
        }
        memberMetaType = SimpleMetaType.resolve(memberClass.getName());
        return memberMetaType;
    }

    private static void handleCustomProperties(Map<String, ManagedProperty> managedProperties,
                                               Configuration resourceConfig, ConfigurationDefinition resourceConfigDef,
                                               Map<String, PropertySimple> customProps)
    {
        for (PropertySimple customProp : customProps.values())
        {
            String propName = customProp.getName();
            ManagedProperty managedProperty = managedProperties.get(propName);
            // NOTE: We assume that the custom property name refers to a top-level property.
            Property property = resourceConfig.get(propName);
            PropertyDefinition propertyDefinition = resourceConfigDef.get(propName);
            // TODO (ips): Should we handle when the property is null?
            if (managedProperty != null && property != null)
            {
                PropertyAdapter propertyAdapter = PropertyAdapterFactory.getCustomPropertyAdapter(customProp);
                MetaValue metaValue = managedProperty.getValue();
                if (metaValue != null)
                {
                    LOG.debug("Populating existing MetaValue of type " + metaValue.getMetaType()
                            + " from RHQ property " + property + " with definition " + propertyDefinition + "...");
                    propertyAdapter.populateMetaValueFromProperty(property, metaValue, propertyDefinition);
                }
                else
                {
                    MetaType metaType = managedProperty.getMetaType();
                    LOG.debug("Converting property " + property + " with definition " + propertyDefinition
                            + " to MetaValue of type " + metaType + "...");
                    metaValue = propertyAdapter.convertToMetaValue(property, propertyDefinition, metaType);
                    managedProperty.setValue(metaValue);
                }
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
    public static MetaValue[] convertOperationsParametersToMetaValues(ManagedOperation managedOperation,
                                                                      Configuration parameters,
                                                                      OperationDefinition operationDefinition)
    {
        ManagedParameter[] managedParameters = managedOperation.getParameters();
        if (operationDefinition != null)
        {
            ConfigurationDefinition configurationDefinition = operationDefinition.getParametersConfigurationDefinition();
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
                    propertyAdapter.populateMetaValueFromProperty(parameter, managedParameter.getValue(), parameterPropertyDefinition);
                }
            }
        }
        return null;
    }

    public static void convertManagedOperationResults(ManagedOperation operation, MetaValue resultMetaValue,
                                                      Configuration complexResults,
                                                      OperationDefinition operationDefinition)
    {                               
        ConfigurationDefinition resultConfigDef = operationDefinition.getResultsConfigurationDefinition();
        // Don't return any results if we have no definition with which to display them
        if (resultConfigDef == null || resultConfigDef.getPropertyDefinitions().isEmpty()) {
        	if (resultMetaValue != null) {
        		LOG.error("Plugin error: Operation [" + operationDefinition.getName()
        				+ "] is defined as returning no results, but it returned non-null results: "
        				+ resultMetaValue.toString());        		
        	}
        	return; 
        } else {
            Map<String, PropertyDefinition> resultPropDefs = resultConfigDef.getPropertyDefinitions();            
            // There should and must be only one property definition to map to the results from the Profile Service,
            // otherwise there will be a huge mismatch.
            if (resultPropDefs.size() > 1)
                LOG.error("Operation [" + operationDefinition.getName()
                        + "] is defined with multiple result properties: " + resultPropDefs.values());
            
            PropertyDefinition resultPropDef = resultPropDefs.values().iterator().next();                        
                     
            // Don't return any results, if the actual result object is null
            if (resultMetaValue == null) {
            	// lets check if result is required or not, if it is log an error   
            	if (resultPropDef.isRequired()) {
	            	LOG.error("Plugin error: Operation [" + operationDefinition.getName()
	        				+ "] is defined as returning a required result, but it returned null.");
            	}
            	return; 
        	}
            
            MetaType resultMetaType = operation.getReturnType();
            if (!instanceOf(resultMetaValue, resultMetaType))
                LOG.debug("Profile Service Error: Result type (" + resultMetaType + ") of [" + operation.getName()
                        + "] ManagedOperation does not match the type of the value returned by invoke() (" 
                        + resultMetaValue + ").");
            
            PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(resultMetaValue);
            Property resultProp = propertyAdapter.convertToProperty(resultMetaValue, resultPropDef);
            complexResults.put(resultProp);
        }
    }

    private static boolean instanceOf(MetaValue metaValue, MetaType metaType) {
        MetaType valueType = metaValue.getMetaType();
        if (valueType.isSimple() && metaType.isSimple())
            return true;
        else if (valueType.isEnum() && metaType.isEnum())
            return true;
        else if (valueType.isCollection() && metaType.isCollection())
            return true;
        else if (valueType.isArray() && metaType.isArray())
            return true;
        else if (valueType.isComposite() && metaType.isComposite()) {
            return (valueType instanceof MapCompositeMetaType && metaType instanceof MapCompositeMetaType) ||
                   (!(valueType instanceof CompositeMetaType) && !(metaType instanceof CompositeMetaType));
        } else if (valueType.isGeneric() && metaType.isGeneric())
            return true;
        else if (valueType.isTable() && metaType.isTable())
            return true;
        else
            return false;
    }

    public static void convertMetricValuesToMeasurement(MeasurementReport report, ManagedProperty metricProperty, MeasurementScheduleRequest request, ResourceType resourceType, String deploymentName)
    {
        String metricName = metricProperty.getName();
        MetaType type = metricProperty.getMetaType();
        MetaValue value = metricProperty.getValue();
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
