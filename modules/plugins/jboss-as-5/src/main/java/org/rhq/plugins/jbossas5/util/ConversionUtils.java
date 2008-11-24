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
import org.jboss.managed.api.ManagedObject;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.GenericMetaType;
import org.jboss.metatype.api.types.CollectionMetaType;
import org.jboss.metatype.api.types.MapCompositeMetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.types.CompositeMetaType;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.EnumValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.metatype.api.values.EnumValueSupport;
import org.jboss.metatype.plugins.types.MutableCompositeMetaType;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
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
 * @author Ian Springer
 */
public class ConversionUtils
{
    private static final Log LOG = LogFactory.getLog(ConversionUtils.class);

    // Key is the RHQ plugin ResourceType name. Make sure if the ResourceType name changes that this map changes too.
    private static final Map<String, ComponentType> KNOWN_COMPONENT_TYPES = new HashMap<String, ComponentType>();

    static {
        KNOWN_COMPONENT_TYPES.put("No TX Datasource", KnownComponentTypes.DataSourceTypes.NoTx.getType());
        KNOWN_COMPONENT_TYPES.put("Local TX Datasource", KnownComponentTypes.DataSourceTypes.LocalTx.getType());
        KNOWN_COMPONENT_TYPES.put("XA Datasource", KnownComponentTypes.DataSourceTypes.XA.getType());
        KNOWN_COMPONENT_TYPES.put("No TX ConnectionFactory", KnownComponentTypes.ConnectionFactoryTypes.NoTx.getType());
        KNOWN_COMPONENT_TYPES.put("XA ConnectionFactory", KnownComponentTypes.ConnectionFactoryTypes.XA.getType());
        KNOWN_COMPONENT_TYPES.put("Queue", KnownComponentTypes.JMSDestination.Queue.getType());
        KNOWN_COMPONENT_TYPES.put("Topic", KnownComponentTypes.JMSDestination.Topic.getType());
    }

    // Key is the RHQ plugin ResourceType name. Make sure if the ResourceType name changes that this map changes too.
    private static final Map<String, String> KNOWN_DEPLOYMENT_TYPES = new HashMap<String, String>();

    static {
        KNOWN_DEPLOYMENT_TYPES.put("Enterprise Application (EAR)", KnownDeploymentTypes.JavaEEApplication.getType());
        KNOWN_DEPLOYMENT_TYPES.put("Client Enterprise Application", KnownDeploymentTypes.JavaEEClientApplication.getType());
        KNOWN_DEPLOYMENT_TYPES.put("EJB 2.x Application", KnownDeploymentTypes.JavaEEEnterpriseBeans2x.getType());
        KNOWN_DEPLOYMENT_TYPES.put("EJB Application (EJB-JAR)", KnownDeploymentTypes.JavaEEEnterpriseBeans3x.getType());
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

        Set<String> propNames = managedProperties.keySet();
        for (String propName : propNames)
        {
            if (customProps.containsKey(propName))
                continue;

            PropertyDefinition propertyDefinition = configurationProperties.get(propName);
            ManagedProperty managedProperty = managedProperties.get(propName);
            if (managedProperty != null && propertyDefinition != null)
            {
                MetaValue metaValue = null;
                try
                {
                    // Temporary check to see which properties are returning MetaValue object and which aren't
                    if (managedProperty.getValue() != null && !(managedProperty.getValue() instanceof MetaValue))
                    {
                        LOG.debug("convertManagedObjectToConfiguration: ManagedProperty.getValue() returned: " + managedProperty.getValue().getClass().getSimpleName()
                                + " for property: " + propName);
                    }
                    metaValue = managedProperty.getValue();
                }
                catch (ClassCastException e)
                {
                    LOG.error("convertManagedObjectToConfiguration: ManagedProperty.getValue() did not return a MetaValue, it returned: " + managedProperty.getValue().getClass().getSimpleName()
                            + " for property: " + propName, e);
                }

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

    public static void convertConfigurationToManagedProperties(Map<String, ManagedProperty> managedProperties, Configuration configuration, ResourceType resourceType, Map<String, PropertySimple> customProps)
    {
        // Deal with the custom properties first, then hold on to the names of these properties so the processing
        // loop can skip over them.
        handleCustomProperties(managedProperties, configuration, customProps);
        ConfigurationDefinition configDefinition = resourceType.getResourceConfigurationDefinition();
        for (Property property : configuration.getProperties())
        {
            String propName = property.getName();
            if (customProps.containsKey(propName))
                continue;
            ManagedProperty managedProperty = managedProperties.get(propName);
            if (managedProperty != null)
            {
                MetaValue metaValue = getValue(managedProperty);
                PropertyDefinition propertyDefinition = configDefinition.get(propName);
                if (metaValue != null)
                {
                    PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);
                    propertyAdapter.populateMetaValueFromProperty(property, metaValue, propertyDefinition);

                    // TODO: This is a workaround for https://jira.jboss.org/jira/browse/JBAS-6188.
                    if ((metaValue instanceof SimpleValueSupport && ((SimpleValue)metaValue).getValue() == null) ||
                        (metaValue instanceof EnumValueSupport && ((EnumValue)metaValue).getValue() == null))
                        managedProperty.setValue(null);

                }
                else
                {
                    MetaType metaType = managedProperty.getMetaType();
                    // TODO (ips, 11/18/08): The below if-blocks are hacks to workaround the template returning the
                    //                       wrong MetaTypes.
                    if (propName.equals("security-domain")) metaType = new GenericMetaType(ManagedObject.class.getName(),
                            ManagedObject.class.getName());
                    if (propName.equals("config-property")) metaType = new CollectionMetaType(CompositeValue.class.getName(),
                            new MutableCompositeMetaType(
                                    "org.jboss.resource.metadata.mcf.ManagedConnectionFactoryPropertyMetaData",
                                    "org.jboss.resource.metadata.mcf.ManagedConnectionFactoryPropertyMetaData"));
                    PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaType);
                    LOG.debug("Converting property " + property + " with definition " + propertyDefinition
                            + " to MetaValue with type " + metaType + "...");
                    metaValue = propertyAdapter.convertToMetaValue(property, propertyDefinition, metaType);
                    managedProperty.setValue(metaValue);
                }
            }
        }
        return;
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

    private static MetaValue getValue(ManagedProperty managedProperty) {
        MetaValue metaValue = null;
        if (managedProperty.getValue() != null)
        {
            LOG.debug("convertConfigurationToManagedProperties: ManagedProperty.getValue() returned: " + managedProperty.getValue().getClass().getSimpleName()
                    + " for property: " + managedProperty.getName());
        }
        try
        {
            // Temporary check to see which properties are returning MetaValue object and which aren't
            metaValue = managedProperty.getValue();
        }
        catch (ClassCastException e)
        {
            LOG.error("convertConfigurationToManagedProperties: ManagedProperty.getValue() did not return a MetaValue; it returned: "
                    + managedProperty.getValue().getClass().getName()
                    + " for property: " + managedProperty.getName(), e);
        }
        return metaValue;
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
                propertyAdapter.populateMetaValueFromProperty(property, managedProperty.getValue(), null);
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
                    propertyAdapter.populateMetaValueFromProperty(parameter, managedParameter.getValue(), parameterPropertyDefinition);
                }
            }
        }
        return null;
    }

    public static void convertManagedOperationResults(ManagedOperation operation, MetaValue resultMetaValue,
                                                      Configuration complexResults, ResourceType resourceType)
    {
        OperationDefinition operationDefinition = getOperationDefinition(resourceType, operation.getName());
        if (operationDefinition == null) {
            LOG.warn("ConversionUtils was not able to find the operation " + operation.getName()
                    + ", so no results can be reported.");
            return;
        }
        ConfigurationDefinition resultConfigDef = operationDefinition.getResultsConfigurationDefinition();
        if (resultConfigDef != null)
        {
            Map<String, PropertyDefinition> resultPropDefs = resultConfigDef.getPropertyDefinitions();
            if (resultPropDefs.isEmpty())
                return;
            // There should and must be only one property definition to map to the results from the Profile Service,
            // otherwise there will be a huge mismatch.
            if (resultPropDefs.size() > 1)
                LOG.error("Operation [" + operationDefinition.getName()
                        + "] is defined with multiple result properties: " + resultPropDefs.values());
            PropertyDefinition resultPropDef = resultPropDefs.values().iterator().next();
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
        else if (valueType.isSimple() && metaType.isSimple())
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
