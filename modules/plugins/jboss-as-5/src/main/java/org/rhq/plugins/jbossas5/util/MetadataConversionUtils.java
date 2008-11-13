/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.jbossas5.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedParameter;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.metatype.api.types.CompositeMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.MetaValue;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.StringUtils;

/**
 * Utility class for converting a JBAS5 ManagedComponent to an RHQ ResourceType.
 *
 * @author Ian Springer
 */
public class MetadataConversionUtils {
    private static final Log LOG = LogFactory.getLog(MetadataConversionUtils.class);

    private static final String PLUGIN_NAME = "ProfileService";

    private static Map<String, PropertySimpleType> TYPE_MAP = new HashMap();
    static {
        TYPE_MAP.put(Boolean.class.getName(), PropertySimpleType.BOOLEAN);
        TYPE_MAP.put(Integer.class.getName(), PropertySimpleType.INTEGER);
        TYPE_MAP.put(Long.class.getName(), PropertySimpleType.LONG);
        TYPE_MAP.put(Float.class.getName(), PropertySimpleType.FLOAT);
        TYPE_MAP.put(Double.class.getName(), PropertySimpleType.DOUBLE);
    }

    public static ResourceType convertComponentToResourceType(ManagedComponent component)
    {
        ComponentType componentType = component.getType();
        String name = componentType.getSubtype() + " " + componentType.getType();
        ResourceType resourceType = new ResourceType(name, PLUGIN_NAME, ResourceCategory.SERVICE, null);
        Set<OperationDefinition> opDefs = convertManagedOperationsToOperationDefinitions(component);
        for (OperationDefinition opDef : opDefs)
            resourceType.addOperationDefinition(opDef);
        Set<MeasurementDefinition> measurementDefs = convertMetricPropertiesToMeasurementDefinitions(component);
        resourceType.setMetricDefinitions(measurementDefs);
        ConfigurationDefinition resourceConfigDef = convertConfigurationPropertiesToConfigurationDefinition(component);
        resourceType.setResourceConfigurationDefinition(resourceConfigDef);
        return resourceType;
    }

    private static Set<OperationDefinition> convertManagedOperationsToOperationDefinitions(ManagedComponent component) {
        Set<OperationDefinition> opDefs = new TreeSet(new OperationDefinitionComparator());
        for (ManagedOperation operation : component.getOperations()) {
            OperationDefinition opDef = convertOperationToOperationDefinition(operation);
            opDefs.add(opDef);
        }
        return opDefs;
    }

    public static OperationDefinition convertOperationToOperationDefinition(ManagedOperation operation) {
        String desc = (!operation.getName().equals(operation.getDescription())) ? operation.getDescription() : null;
        OperationDefinition opDef = new OperationDefinition(operation.getName(), null, desc);
        opDef.setDisplayName(StringUtils.deCamelCase(operation.getName()));
        ConfigurationDefinition paramsConfigDef = convertParametersToConfigurationDefinition(operation);
        opDef.setParametersConfigurationDefinition(paramsConfigDef);
        ConfigurationDefinition resultsConfigDef = convertResultToConfigurationDefinition(operation);
        opDef.setResultsConfigurationDefinition(resultsConfigDef);
        return opDef;
    }

    public static ConfigurationDefinition convertParametersToConfigurationDefinition(ManagedOperation operation)
    {
        if (operation.getParameters() == null || operation.getParameters().length == 0)
            return null;
        ConfigurationDefinition configDef = new ConfigurationDefinition(operation.getName(), null);
        for (ManagedParameter parameter : operation.getParameters()) {
            PropertyDefinition propDef = convertParameterToPropertyDefinition(parameter);
            configDef.put(propDef);
        }
        return configDef;
    }

    private static PropertyDefinition convertParameterToPropertyDefinition(ManagedParameter parameter) {
        PropertyDefinition propDef;
        String desc = (!parameter.getName().equals(parameter.getDescription())) ? parameter.getDescription() : null;
        if (parameter.getMetaType().isArray() || parameter.getMetaType().isCollection()) {
            propDef = createPropertyDefinitionList(parameter.getName(), desc);
        } else {
            propDef = convertSimpleParameterToPropertyDefinitionSimple(parameter, desc);
        }
        return propDef;
    }

    private static PropertyDefinitionList createPropertyDefinitionList(String name, String desc) {
        PropertySimpleType propType = convertClassToPropertySimpleType(Object.class.getName());
        PropertyDefinitionSimple propDefSimple = new PropertyDefinitionSimple("item", null, false, propType);
        propDefSimple.setDisplayName(StringUtils.deCamelCase(propDefSimple.getName()));
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        PropertyDefinitionList propDefList = new PropertyDefinitionList(name, desc, true, propDefSimple);
        return propDefList;
    }

    private static PropertyDefinitionSimple convertSimpleParameterToPropertyDefinitionSimple(ManagedParameter parameter,
                                                                                             String desc) {
        PropertySimpleType propType = convertClassToPropertySimpleType(parameter.getMetaType().getClassName());
        PropertyDefinitionSimple propDefSimple = new PropertyDefinitionSimple(parameter.getName(), desc,
                true, propType);
        propDefSimple.setDisplayName(StringUtils.deCamelCase(parameter.getName()));
        addConstraints(propDefSimple, parameter.getMinimumValue(), parameter.getMaximumValue());
        // TODO: Convert parameter.getLegalValues() to enum defs.
        return propDefSimple;
    }

    private static ConfigurationDefinition convertResultToConfigurationDefinition(ManagedOperation operation)
    {
        MetaType returnType = operation.getReturnType();
        if (returnType.getClassName().equals(Void.class.getName())) {
            return null;
        }
        PropertyDefinition propDef;
        if (returnType.isArray() || returnType.isCollection()) {
            propDef = createPropertyDefinitionList("result", null);
        } else {
            PropertySimpleType propType = convertClassToPropertySimpleType(returnType.getClassName());
            propDef = new PropertyDefinitionSimple("result", null, true, propType);
        }
        propDef.setDisplayName(StringUtils.deCamelCase(propDef.getName()));
        ConfigurationDefinition configDef = new ConfigurationDefinition(operation.getName(), null);
        configDef.put(propDef);
        return configDef;
    }

    private static Set<MeasurementDefinition> convertMetricPropertiesToMeasurementDefinitions(ManagedComponent component)
    {
        Set<MeasurementDefinition> measurementDefs = new TreeSet(new MeasurementDefinitionComparator());
        for(ManagedProperty property : component.getProperties().values()) {
            if (!property.hasViewUse(ViewUse.STATISTIC) && !property.hasViewUse(ViewUse.RUNTIME))
                continue;
            if (property.getMetaType().isSimple()) {
                DataType dataType = (property.hasViewUse(ViewUse.STATISTIC)) ? DataType.MEASUREMENT : DataType.TRAIT;
                MeasurementDefinition measurementDef = new MeasurementDefinition(property.getName(),
                            MeasurementCategory.PERFORMANCE, MeasurementUnits.NONE, dataType, true, 60000,
                            DisplayType.DETAIL);
                measurementDefs.add(measurementDef);
                measurementDef.setDisplayName(StringUtils.deCamelCase(property.getName()));
                String desc = (!property.getName().equals(property.getDescription())) ? property.getDescription() : null;
                measurementDef.setDescription(desc);
            } else if (property.getMetaType().isComposite()) {
                addMeasurementDefinitionsForCompositeManagedProperty(property, measurementDefs);
            }
        }
        return measurementDefs;
    }

    private static void addMeasurementDefinitionsForCompositeManagedProperty(ManagedProperty property, Set<MeasurementDefinition> measurementDefs) {
        CompositeValue compositeValue = (CompositeValue)property.getValue();
        CompositeMetaType compositeMetaType = compositeValue.getMetaType();
        Set<String> itemNames = compositeMetaType.keySet();
        for (String itemName : itemNames) {
            MetaType itemType = compositeMetaType.getType(itemName);
            if (itemType.isSimple()) {
                String metricName = property.getName() + "." + itemName;
                DataType dataType = (itemType.getClassName().equals(String.class.getName())) ?
                        DataType.TRAIT : DataType.MEASUREMENT;
                MeasurementDefinition measurementDef = new MeasurementDefinition(metricName,
                    MeasurementCategory.PERFORMANCE, MeasurementUnits.NONE, dataType, true, 60000,
                    DisplayType.DETAIL);
                measurementDefs.add(measurementDef);
                measurementDef.setDisplayName(StringUtils.deCamelCase(itemName));
            } else {
                MetaValue itemValue = compositeValue.get(itemName);
                LOG.warn("Composite stat property [" + compositeValue + "] contains non-simple item [" + itemValue +
                        "] - skipping..." );
            }
         }
    }

    public static ConfigurationDefinition convertConfigurationPropertiesToConfigurationDefinition(ManagedComponent component)
    {
        Set<PropertyDefinition> propDefs = new TreeSet(new PropertyDefinitionComparator());
        for(ManagedProperty prop : component.getProperties().values()) {
            if (!prop.hasViewUse(ViewUse.CONFIGURATION))
                continue;
            String desc = (!prop.getName().equals(prop.getDescription())) ? prop.getDescription() : null;
            PropertySimpleType propType = convertClassToPropertySimpleType(prop.getMetaType().getClassName());
            PropertyDefinitionSimple propDefSimple = new PropertyDefinitionSimple(prop.getName(),
                    desc, prop.isMandatory(), propType);
            propDefs.add(propDefSimple);
            propDefSimple.setDisplayName(StringUtils.deCamelCase(prop.getName()));
            addConstraints(propDefSimple, prop.getMinimumValue(), prop.getMaximumValue());
            // TODO: Convert parameter.getLegalValues() to enum defs.
        }
        if (propDefs.isEmpty())
            return null;
        ConfigurationDefinition configDef = new ConfigurationDefinition(component.getName(), null);
        for (PropertyDefinition propDef : propDefs)
           configDef.getPropertyDefinitions().put(propDef.getName(), propDef);
        return configDef;
    }

    private static PropertySimpleType convertClassToPropertySimpleType(String className) {
        PropertySimpleType simpleType = TYPE_MAP.get(className);
        return (simpleType != null) ? simpleType : PropertySimpleType.STRING;
    }

    private static void addConstraints(PropertyDefinitionSimple propDefSimple, Comparable<?> minValue,
                                       Comparable<?> maxValue) {
        if (minValue != null || maxValue != null) {
            if (propDefSimple.getType() == PropertySimpleType.INTEGER ||
                    propDefSimple.getType() == PropertySimpleType.LONG) {
                Long min = (minValue != null) ? ((Number)minValue).longValue() : null;
                Long max = (maxValue != null) ? ((Number)maxValue).longValue() : null;
                IntegerRangeConstraint constraint = new IntegerRangeConstraint(min, max);
                propDefSimple.addConstraints(constraint);
            } else if (propDefSimple.getType() == PropertySimpleType.FLOAT ||
                    propDefSimple.getType() == PropertySimpleType.DOUBLE) {
                Double min = (minValue != null) ? ((Number)minValue).doubleValue() : null;
                Double max = (maxValue != null) ? ((Number)maxValue).doubleValue() : null;
                FloatRangeConstraint constraint = new FloatRangeConstraint(min, max);
                propDefSimple.addConstraints(constraint);
            }
        }
    }

    private static class OperationDefinitionComparator implements Comparator<OperationDefinition> {
        public int compare(OperationDefinition opDef1, OperationDefinition opDef2) {
            return opDef1.getName().compareTo(opDef2.getName());
        }
    }

    private static class MeasurementDefinitionComparator implements Comparator<MeasurementDefinition> {
        public int compare(MeasurementDefinition metricDef1, MeasurementDefinition metricDef2) {
            return metricDef1.getName().compareTo(metricDef2.getName());
        }
    }

    private static class PropertyDefinitionComparator implements Comparator<PropertyDefinition> {
        public int compare(PropertyDefinition propDef1, PropertyDefinition propDef2) {
            return propDef1.getName().compareTo(propDef2.getName());
        }
    }
}
