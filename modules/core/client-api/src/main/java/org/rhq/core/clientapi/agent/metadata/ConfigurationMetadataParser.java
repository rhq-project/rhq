 /*
  * RHQ Management Platform
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
package org.rhq.core.clientapi.agent.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationProperty;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationTemplateDescriptor;
import org.rhq.core.clientapi.descriptor.configuration.ConstraintType;
import org.rhq.core.clientapi.descriptor.configuration.FloatConstraintType;
import org.rhq.core.clientapi.descriptor.configuration.IntegerConstraintType;
import org.rhq.core.clientapi.descriptor.configuration.ListProperty;
import org.rhq.core.clientapi.descriptor.configuration.MapProperty;
import org.rhq.core.clientapi.descriptor.configuration.Option;
import org.rhq.core.clientapi.descriptor.configuration.PropertyGroup;
import org.rhq.core.clientapi.descriptor.configuration.PropertyOptions;
import org.rhq.core.clientapi.descriptor.configuration.PropertyType;
import org.rhq.core.clientapi.descriptor.configuration.RegexConstraintType;
import org.rhq.core.clientapi.descriptor.configuration.SimpleProperty;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.ActivationPolicy;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;
import org.rhq.core.domain.util.StringUtils;

/**
 * @author Jason Dobies
 * @author Ian Springer
 */
public class ConfigurationMetadataParser {
    public static ConfigurationDefinition parse(String configurationName, ConfigurationDescriptor descriptor)
        throws InvalidPluginDescriptorException {
        if (descriptor == null) {
            return null;
        }

        ConfigurationDefinition configurationDefinition = new ConfigurationDefinition(configurationName,
                descriptor.getNotes());

        for (ConfigurationTemplateDescriptor templateDescriptor : descriptor.getTemplate()) {
            configurationDefinition.putTemplate(parseTemplate(templateDescriptor));
        }

        ConfigurationTemplate defaultTemplate = initDefaultTemplate(configurationDefinition);
        configurationDefinition.putTemplate(defaultTemplate);

        Configuration defaultConfiguration = defaultTemplate.getConfiguration();
        parseProperties(descriptor, defaultConfiguration, configurationDefinition);
        ConfigurationUtility.normalizeConfiguration(defaultConfiguration, configurationDefinition);

        return configurationDefinition;
    }

    private static ConfigurationTemplate initDefaultTemplate(ConfigurationDefinition configurationDefinition) {
        ConfigurationTemplate defaultTemplate = configurationDefinition.getDefaultTemplate();
        if (defaultTemplate == null) {
            // TODO: Not everything should have a default template... only stuff that has default values
            defaultTemplate = new ConfigurationTemplate(ConfigurationTemplate.DEFAULT_TEMPLATE_NAME,
                "the default template");
            Configuration defaultConfiguration = new Configuration();
            defaultTemplate.setConfiguration(defaultConfiguration);
        }

        defaultTemplate.setDefault(true);
        return defaultTemplate;
    }

    private static void parseProperties(ConfigurationDescriptor descriptor, Configuration defaultConfiguration,
        ConfigurationDefinition configurationDefinition) throws InvalidPluginDescriptorException {
        List<PropertyGroup> groups = descriptor.getGroup();
        List<JAXBElement<? extends ConfigurationProperty>> properties = descriptor.getConfigurationProperty();
        if ((groups.size() == 0) && (properties.size() == 0)) {
            throw new InvalidPluginDescriptorException(
                "Configuration properties are missing. Resource configurations must have at least one group or one config-property as a child.");
        }

        int groupOrderIndex = 0;
        for (PropertyGroup group : descriptor.getGroup()) {
            org.rhq.core.domain.configuration.definition.PropertyGroupDefinition groupDef = new PropertyGroupDefinition(
                group.getName());
            groupDef.setDisplayName((group.getDisplayName() != null) ? group.getDisplayName() : StringUtils
                .deCamelCase(group.getName()));
            groupDef.setDescription(group.getDescription());
            groupDef.setDefaultHidden(group.isHiddenByDefault());
            groupDef.setOrder(groupOrderIndex++);
            List<JAXBElement<? extends ConfigurationProperty>> groupProperties = group.getConfigurationProperty();
            int propertyOrderIndex = 0;
            for (JAXBElement<? extends ConfigurationProperty> jaxbProperty : groupProperties) {
                ConfigurationProperty uncastedProperty = jaxbProperty.getValue();
                PropertyDefinition propertyDefinition = parseProperty(uncastedProperty, propertyOrderIndex, defaultConfiguration
                );
                if (configurationDefinition != null) {
                    propertyDefinition.setPropertyGroupDefinition(groupDef);
                    configurationDefinition.put(propertyDefinition);
                    propertyOrderIndex++;
                }
            }
        }

        int propertyOrderIndex = 0;
        for (JAXBElement<? extends ConfigurationProperty> jaxbProperty : properties) {
            ConfigurationProperty uncastedProperty = jaxbProperty.getValue();
            PropertyDefinition propertyDefinition = parseProperty(uncastedProperty, propertyOrderIndex, defaultConfiguration
            );
            if (configurationDefinition != null) {
                configurationDefinition.put(propertyDefinition);
                propertyOrderIndex++;
            }
        }
    }

    private static ConfigurationTemplate parseTemplate(ConfigurationTemplateDescriptor templateDescripter)
        throws InvalidPluginDescriptorException {
        ConfigurationTemplate template = new ConfigurationTemplate(templateDescripter.getName(), templateDescripter
            .getDescription());
        Configuration templateConfiguration = new Configuration();
        template.setConfiguration(templateConfiguration);

        parseProperties(templateDescripter, templateConfiguration, null);
        return template;
    }

    private static PropertyDefinition parseProperty(ConfigurationProperty uncastedProperty, int orderIndex)
        throws InvalidPluginDescriptorException {
        return parseProperty(uncastedProperty, orderIndex, null);
    }

    private static PropertyDefinition parseProperty(ConfigurationProperty uncastedProperty,
                                                    int orderIndex, AbstractPropertyMap defaultConfigurationParentMap)
            throws InvalidPluginDescriptorException {
        PropertyDefinition property = null;
        if (uncastedProperty instanceof SimpleProperty) {
            property = parseSimpleProperty((SimpleProperty) uncastedProperty, defaultConfigurationParentMap);
        } else if (uncastedProperty instanceof ListProperty) {
            property = parseListProperty((ListProperty) uncastedProperty);
        } else if (uncastedProperty instanceof MapProperty) {
            property = parseMapProperty((MapProperty) uncastedProperty, defaultConfigurationParentMap);
        }

        if (property != null) {
            property.setOrder(orderIndex);
        }

        return property;
    }

    private static PropertyDefinitionSimple parseSimpleProperty(SimpleProperty simpleProperty,
        AbstractPropertyMap defaultConfigurationParentMap) throws InvalidPluginDescriptorException {
        String description = parseMultiValue(simpleProperty.getDescription(), simpleProperty.getLongDescription());
        String displayName = (simpleProperty.getDisplayName() != null) ? simpleProperty.getDisplayName() : StringUtils
            .deCamelCase(simpleProperty.getName());

        PropertyDefinitionSimple property = new PropertyDefinitionSimple(simpleProperty.getName(), description,
            simpleProperty.isRequired(), translatePropertyType(simpleProperty.getType()));

        property.setReadOnly(simpleProperty.isReadOnly());
        property.setSummary(simpleProperty.isSummary());
        property.setActivationPolicy(translateActivationPolicy(simpleProperty.getActivationPolicy()));
        property.setConstraints(translateContraints(simpleProperty.getConstraint()));
        property.setDisplayName(displayName);
        property.setDefaultValue(simpleProperty.getDefaultValue());
        property.setUnits(MetricsMetadataParser.getMeasurementUnits(simpleProperty.getUnits(), null));

        String initialValue = simpleProperty.getInitialValue();
        if ((defaultConfigurationParentMap != null) && (initialValue != null)) {
            defaultConfigurationParentMap.put(new PropertySimple(simpleProperty.getName(), initialValue));
        }

        // Load the enumeration of options
        if (simpleProperty.getPropertyOptions() != null) {
            parsePropertyOptions(property, simpleProperty.getPropertyOptions());
        }

        return property;
    }

    private static List<PropertyDefinitionEnumeration> parsePropertyOptions(PropertyDefinitionSimple parentProperty,
        PropertyOptions options) {
        List<PropertyDefinitionEnumeration> results = new ArrayList<PropertyDefinitionEnumeration>();
        for (Option option : options.getOption()) {
            String name = option.getName();
            if (name == null) {
                name = option.getValue();
            }

            parentProperty.addEnumeratedValues(new PropertyDefinitionEnumeration(name, option.getValue(), option
                .isDefault()));
        }

        parentProperty.setAllowCustomEnumeratedValue(options.isAllowCustomValue());

        return results;
    }

    private static PropertyDefinitionList parseListProperty(ListProperty listProperty)
        throws InvalidPluginDescriptorException {
        String description = parseMultiValue(listProperty.getDescription(), listProperty.getLongDescription());
        JAXBElement<? extends ConfigurationProperty> memberProperty = listProperty.getConfigurationProperty();
        PropertyDefinition memberDefinition = (memberProperty != null) ? parseProperty(memberProperty.getValue(), 0)
            : null;

        PropertyDefinitionList list = new PropertyDefinitionList(listProperty.getName(), description, listProperty
            .isRequired(), memberDefinition);

        String displayName = (listProperty.getDisplayName() != null) ? listProperty.getDisplayName() : StringUtils
            .deCamelCase(listProperty.getName());
        list.setDisplayName(displayName);
        list.setReadOnly(listProperty.isReadOnly());
        list.setSummary(listProperty.isSummary());

        list.setMin(listProperty.getMin().intValue());
        if (listProperty.getMax().equals("unbounded")) {
            list.setMax(Integer.MAX_VALUE);
        } else {
            list.setMax(Integer.parseInt(listProperty.getMax()));
        }

        return list;
    }

    private static PropertyDefinitionMap parseMapProperty(MapProperty mapProperty,
                                                          AbstractPropertyMap defaultConfigurationParentMap)
        throws InvalidPluginDescriptorException {
        String description = parseMultiValue(mapProperty.getDescription(), mapProperty.getLongDescription());

        PropertyDefinitionMap propDefMap = new PropertyDefinitionMap(mapProperty.getName(), description, mapProperty
            .isRequired());

        String displayName = (mapProperty.getDisplayName() != null) ? mapProperty.getDisplayName() : StringUtils
            .deCamelCase(mapProperty.getName());
        propDefMap.setDisplayName(displayName);
        propDefMap.setReadOnly(mapProperty.isReadOnly());
        propDefMap.setSummary(mapProperty.isSummary());

        // Add an instance of the map to the default config, if appropriate.
        PropertyMap propMap;
        if (defaultConfigurationParentMap != null) {
            propMap = new PropertyMap(propDefMap.getName());
            defaultConfigurationParentMap.put(propMap);
        } else {
            propMap = null;
        }

        // Process the map's nested properties.
        List<JAXBElement<? extends ConfigurationProperty>> nestedProperties = mapProperty.getConfigurationProperty();
        int propertyOrderIndex = 0;
        for (JAXBElement<? extends ConfigurationProperty> jaxbProperty : nestedProperties) {
            ConfigurationProperty uncastedProperty = jaxbProperty.getValue();
            PropertyDefinition propertyDefinition = parseProperty(uncastedProperty, propertyOrderIndex, propMap);
            propDefMap.put(propertyDefinition);
        }
        return propDefMap;
    }

    private static PropertySimpleType translatePropertyType(PropertyType fromType)
        throws InvalidPluginDescriptorException {
        PropertySimpleType toType;

        switch (fromType) {
        case BOOLEAN: {
            toType = PropertySimpleType.BOOLEAN;
            break;
        }

        case DIRECTORY: {
            toType = PropertySimpleType.DIRECTORY;
            break;
        }

        case FILE: {
            toType = PropertySimpleType.FILE;
            break;
        }

        case FLOAT: {
            toType = PropertySimpleType.FLOAT;
            break;
        }

        case DOUBLE: {
            toType = PropertySimpleType.DOUBLE;
            break;
        }

        case INTEGER: {
            toType = PropertySimpleType.INTEGER;
            break;
        }

        case LONG: {
            toType = PropertySimpleType.LONG;
            break;
        }

        case LONG_STRING: {
            toType = PropertySimpleType.LONG_STRING;
            break;
        }

        case PASSWORD: {
            toType = PropertySimpleType.PASSWORD;
            break;
        }

        case STRING: {
            toType = PropertySimpleType.STRING;
            break;
        }

        default: {
            throw new InvalidPluginDescriptorException(
                "Property type specified does not have a corresponding domain property type. Property type: "
                    + fromType);
        }
        }

        return toType;
    }

    private static ActivationPolicy translateActivationPolicy(
        org.rhq.core.clientapi.descriptor.configuration.ActivationPolicy fromPolicy)
        throws InvalidPluginDescriptorException {
        ActivationPolicy toPolicy;

        switch (fromPolicy) {
        case IMMEDIATE: {
            toPolicy = ActivationPolicy.IMMEDIATE;
            break;
        }

        case RESTART: {
            toPolicy = ActivationPolicy.RESTART;
            break;
        }

        case SHUTDOWN: {
            toPolicy = ActivationPolicy.SHUTDOWN;
            break;
        }

        default: {
            throw new InvalidPluginDescriptorException(
                "Activation policy specified does not have a corresponding domain activation policy type. Activation Policy type: "
                    + fromPolicy);
        }
        }

        return toPolicy;
    }

    private static Set<Constraint> translateContraints(List<ConstraintType> fromConstraints)
        throws InvalidPluginDescriptorException {
        Set<Constraint> toConstraints = new HashSet<Constraint>();

        if (fromConstraints == null) {
            return toConstraints;
        }

        for (ConstraintType fromC : fromConstraints) {
            List<Object> constraints = fromC.getIntegerConstraintOrFloatConstraintOrRegexConstraint();

            for (Object constraint : constraints) {
                if (constraint instanceof FloatConstraintType) {
                    FloatConstraintType floatDetails = (FloatConstraintType) constraint;

                    Double floatMin = (floatDetails.getMinimum() != null) ? new Double(floatDetails.getMinimum()
                        .toString()) : null;
                    Double floatMax = (floatDetails.getMaximum() != null) ? new Double(floatDetails.getMaximum()
                        .toString()) : null;

                    FloatRangeConstraint fc = new FloatRangeConstraint(floatMin, floatMax);
                    toConstraints.add(fc);
                } else if (constraint instanceof IntegerConstraintType) {
                    IntegerConstraintType intDetails = (IntegerConstraintType) constraint;

                    Long longMin = (intDetails.getMinimum() != null) ? new Long(intDetails.getMinimum().toString())
                        : null;
                    Long longMax = (intDetails.getMaximum() != null) ? new Long(intDetails.getMaximum().toString())
                        : null;

                    IntegerRangeConstraint ic = new IntegerRangeConstraint(longMin, longMax);
                    toConstraints.add(ic);
                } else if (constraint instanceof RegexConstraintType) {
                    RegexConstraintType regexDetails = (RegexConstraintType) constraint;

                    RegexConstraint rc = new RegexConstraint();
                    rc.setDetails(regexDetails.getExpression());

                    toConstraints.add(rc);
                } else {
                    // this will only occur if we change the .xsd schema and add a type but forget to add to the code above
                    throw new InvalidPluginDescriptorException("Unknown constraint type: " + fromC);
                }
            }
        }

        return toConstraints;
    }

    /**
     * Iterates over each value specified. For the first non-null value found, the value is trimmed and returned. If no
     * non-null values are found, <code>null</code> is returned.
     *
     * @param  values values, in order of priority, to be checked.
     *
     * @return trimmed value if one is found; <code>null</code> if none are found.
     */
    private static String parseMultiValue(String... values) {
        String value = null;
        for (String s : values) {
            if (s == null) {
                continue;
            }

            value = s;
            break;
        }

        return (value != null) ? value.trim() : null;
    }
}