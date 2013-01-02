/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.xmlschema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationProperty;
import org.rhq.core.clientapi.descriptor.configuration.ListProperty;
import org.rhq.core.clientapi.descriptor.configuration.MapProperty;
import org.rhq.core.clientapi.descriptor.configuration.MeasurementUnitsDescriptor;
import org.rhq.core.clientapi.descriptor.configuration.Option;
import org.rhq.core.clientapi.descriptor.configuration.PropertyOptions;
import org.rhq.core.clientapi.descriptor.configuration.PropertyType;
import org.rhq.core.clientapi.descriptor.configuration.SimpleProperty;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ComplexValueDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ComplexValueListDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ComplexValueMapDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ComplexValueSimpleDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ConfigurationInstanceDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.ListPropertyInstanceDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.MapPropertyInstanceDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.PropertyValuesDescriptor;
import org.rhq.enterprise.server.xmlschema.generated.configuration.instance.SimplePropertyInstanceDescriptor;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ConfigurationInstanceDescriptorUtil {

    public static final String NS_CONFIGURATION_INSTANCE = "urn:xmlns:rhq-configuration-instance";
    public static final String NS_CONFIGURATION = "urn:xmlns:rhq-configuration";

    private static final Log LOG = LogFactory.getLog(ConfigurationInstanceDescriptorUtil.class);

    private ConfigurationInstanceDescriptorUtil() {

    }

    public static class ConfigurationAndDefinition {
        public Configuration configuration;
        public ConfigurationDefinition definition;
    }

    protected static QName getTagName(ConfigurationProperty descriptor) {
        if (descriptor instanceof SimplePropertyInstanceDescriptor) {
            return new QName(NS_CONFIGURATION_INSTANCE, "simple-property");
        } else if (descriptor instanceof ListPropertyInstanceDescriptor) {
            return new QName(NS_CONFIGURATION_INSTANCE, "list-property");
        } else if (descriptor instanceof MapPropertyInstanceDescriptor) {
            return new QName(NS_CONFIGURATION_INSTANCE, "map-property");
        } else if (descriptor instanceof SimpleProperty) {
            return new QName(NS_CONFIGURATION, "simple-property");
        } else if (descriptor instanceof ListProperty) {
            return new QName(NS_CONFIGURATION, "list-property");
        } else if (descriptor instanceof MapProperty) {
            return new QName(NS_CONFIGURATION, "map-property");
        }

        throw new IllegalArgumentException("Unknown descriptor type: " + descriptor.getClass());
    }

    private static QName getTagName(ComplexValueDescriptor value) {
        if (value instanceof ComplexValueSimpleDescriptor) {
            return new QName(NS_CONFIGURATION_INSTANCE, "simple-value");
        } else if (value instanceof ComplexValueListDescriptor) {
            return new QName(NS_CONFIGURATION_INSTANCE, "list-value");
        } else if (value instanceof ComplexValueMapDescriptor) {
            return new QName(NS_CONFIGURATION_INSTANCE, "map-value");
        }

        throw new IllegalArgumentException("Unknown value descriptor type: " + value.getClass());
    }

    private static class ToDescriptor {

        public static ConfigurationInstanceDescriptor createConfigurationInstance(ConfigurationDefinition definition,
            Configuration configuration) {

            ConfigurationInstanceDescriptor ret = new ConfigurationInstanceDescriptor();

            addAll(ret.getConfigurationProperty(), definition.getPropertyDefinitions(), configuration.getMap());

            return ret;
        }

        private static void addAll(List<JAXBElement<?>> descriptors, Map<String, PropertyDefinition> defs,
            Map<String, Property> props) {
            for (Map.Entry<String, PropertyDefinition> e : defs.entrySet()) {
                String propName = e.getKey();
                PropertyDefinition def = e.getValue();
                Property prop = props.get(propName);

                addSingle(descriptors, def, prop);
            }
        }

        private static void addSingle(List<JAXBElement<?>> descriptors, PropertyDefinition def, Property prop) {
            ConfigurationProperty descriptor = null;
            QName tagName = null;

            descriptor = createDescriptor(def, prop);
            tagName = getTagName(descriptor);

            addToJAXBElementList(descriptors, Object.class, descriptor, tagName);
        }

        private static ConfigurationProperty createDescriptor(PropertyDefinition def, Property prop) {
            ConfigurationProperty ret = null;

            if (def instanceof PropertyDefinitionSimple) {
                ret = createSimple((PropertyDefinitionSimple) def, (PropertySimple) prop);
            } else if (def instanceof PropertyDefinitionList) {
                ret = createList((PropertyDefinitionList) def, (PropertyList) prop);
            } else if (def instanceof PropertyDefinitionMap) {
                ret = createMap((PropertyDefinitionMap) def, (PropertyMap) prop);
            }

            return ret;
        }

        private static SimplePropertyInstanceDescriptor createSimple(PropertyDefinitionSimple def, PropertySimple prop) {
            SimplePropertyInstanceDescriptor ret = new SimplePropertyInstanceDescriptor();
            setCommonProps(ret, def, true);

            //these are prohibited, because they make no sense on the property instance
            //ret.setDefaultValue(def.getDefaultValue());
            //ret.setInitialValue(prop.getStringValue());
            //ret.setActivationPolicy(pds.getActivationPolicy());
            ret.setPropertyOptions(convert(def.getEnumeratedValues()));
            ret.setType(convert(def.getType()));
            ret.setUnits(convert(def.getUnits()));

            if (prop != null) {
                ret.setValue(prop.getStringValue());
            }

            return ret;
        }

        private static ListPropertyInstanceDescriptor createList(PropertyDefinitionList def, PropertyList prop) {
            ListPropertyInstanceDescriptor ret = new ListPropertyInstanceDescriptor();

            setCommonProps(ret, def, true);

            ConfigurationProperty memberDef = convertDefinition(def.getMemberDefinition());
            ret.setConfigurationProperty(new JAXBElement<ConfigurationProperty>(getTagName(memberDef),
                ConfigurationProperty.class, memberDef));

            if (prop != null) {
                PropertyValuesDescriptor values = new PropertyValuesDescriptor();

                ret.setValues(values);

                for (Property el : prop.getList()) {
                    ComplexValueDescriptor value = convertValue(el);

                    //we don't need the property-name in lists, because the list has just a single member definition
                    value.setPropertyName(null);

                    addToJAXBElementList(values.getComplexValue(), Object.class, value, getTagName(value));
                }
            }

            return ret;
        }

        private static MapPropertyInstanceDescriptor createMap(PropertyDefinitionMap def, PropertyMap prop) {
            MapPropertyInstanceDescriptor ret = new MapPropertyInstanceDescriptor();

            setCommonProps(ret, def, true);

            for (PropertyDefinition mem : def.getOrderedPropertyDefinitions()) {
                ConfigurationProperty memDef = convertDefinition(mem);

                addToJAXBElementList(ret.getConfigurationProperty(), ConfigurationProperty.class, memDef,
                    getTagName(memDef));
            }

            if (prop != null) {
                PropertyValuesDescriptor values = new PropertyValuesDescriptor();

                ret.setValues(values);

                for (Property el : prop.getMap().values()) {
                    ComplexValueDescriptor value = convertValue(el);
                    addToJAXBElementList(values.getComplexValue(), Object.class, value, getTagName(value));
                }
            }

            return ret;
        }

        private static void setCommonProps(ConfigurationProperty target, PropertyDefinition source,
            boolean creatingInstance) {
            target.setName(source.getName());
            //let's always use long description
            //target.setDescription(source.getDescription());
            target.setLongDescription(source.getDescription());
            target.setDisplayName(source.getDisplayName());

            //these are prohibited on the instance because they make no sense there
            if (!creatingInstance) {
                target.setRequired(source.isRequired());
                target.setReadOnly(source.isReadOnly());
                target.setSummary(source.isSummary());
            }
        }

        private static PropertyOptions convert(List<PropertyDefinitionEnumeration> options) {
            if (options.isEmpty()) {
                return null;
            }

            PropertyOptions ret = new PropertyOptions();

            ArrayList<PropertyDefinitionEnumeration> opts = new ArrayList<PropertyDefinitionEnumeration>(options);

            Collections.sort(opts, new Comparator<PropertyDefinitionEnumeration>() {
                public int compare(PropertyDefinitionEnumeration o1, PropertyDefinitionEnumeration o2) {
                    return o1.getOrderIndex() - o2.getOrderIndex();
                }
            });

            for (PropertyDefinitionEnumeration option : opts) {
                ret.getOption().add(convert(option));
            }

            return ret;
        }

        private static Option convert(PropertyDefinitionEnumeration option) {
            Option ret = new Option();
            ret.setName(option.getName());
            ret.setValue(option.getValue());
            return ret;
        }

        private static PropertyType convert(PropertySimpleType type) {
            if (type == null) {
                return null;
            }

            try {
                return PropertyType.valueOf(type.name());
            } catch (IllegalArgumentException e) {
                LOG.warn("Failed to convert a PropertySimpleType instance '" + type.name() + "' into a PropertyType.",
                    e);
                throw e;
            }
        }

        private static MeasurementUnitsDescriptor convert(MeasurementUnits unit) {
            //XXX there actually are some differences in the available values for these
            //two enums:
            //Missing in MeasurementUnitsDescriptor:
            //MeasurementUnits.JIFFY, MeasurementUnits.PETA_BYTES

            if (unit == null) {
                return null;
            }

            String value = unit.name();

            try {
                return MeasurementUnitsDescriptor.valueOf(value);
            } catch (IllegalArgumentException e) {
                LOG.warn("Failed to convert a MeasurementUnits instance '" + unit.getName()
                    + "' into a MeasurementUnitsDescriptor.", e);
                throw e;
            }
        }

        private static ConfigurationProperty convertDefinition(PropertyDefinition def) {
            if (def instanceof PropertyDefinitionSimple) {
                return convertSimple((PropertyDefinitionSimple) def);
            } else if (def instanceof PropertyDefinitionList) {
                return convertList((PropertyDefinitionList) def);
            } else if (def instanceof PropertyDefinitionMap) {
                return convertMap((PropertyDefinitionMap) def);
            }

            throw new IllegalArgumentException("Unsupported property definition type: " + def.getClass());
        }

        private static SimpleProperty convertSimple(PropertyDefinitionSimple def) {
            SimpleProperty ret = new SimpleProperty();
            setCommonProps(ret, def, false);
            ret.setDefaultValue(def.getDefaultValue());
            ret.setPropertyOptions(convert(def.getEnumeratedValues()));
            ret.setType(convert(def.getType()));
            ret.setUnits(convert(def.getUnits()));

            return ret;
        }

        private static ListProperty convertList(PropertyDefinitionList def) {
            ListProperty ret = new ListProperty();
            setCommonProps(ret, def, false);
            ConfigurationProperty memberDefinition = convertDefinition(def.getMemberDefinition());
            ret.setConfigurationProperty(new JAXBElement<ConfigurationProperty>(getTagName(memberDefinition),
                ConfigurationProperty.class, memberDefinition));

            return ret;
        }

        private static MapProperty convertMap(PropertyDefinitionMap def) {
            MapProperty ret = new MapProperty();
            setCommonProps(ret, def, false);

            List<JAXBElement<? extends ConfigurationProperty>> elements = ret.getConfigurationProperty();
            for (PropertyDefinition el : def.getOrderedPropertyDefinitions()) {
                ConfigurationProperty prop = convertDefinition(el);
                QName tagName = getTagName(prop);
                addToJAXBElementList(elements, ConfigurationProperty.class, prop, tagName);
            }

            return ret;
        }

        private static ComplexValueDescriptor convertValue(Property prop) {
            if (prop instanceof PropertySimple) {
                return convertSimpleValue((PropertySimple) prop);
            } else if (prop instanceof PropertyList) {
                return convertListValue((PropertyList) prop);
            } else if (prop instanceof PropertyMap) {
                return convertMapValue((PropertyMap) prop);
            }

            throw new IllegalArgumentException("Unsupported property type to convert to a value descriptor: "
                + prop.getClass());
        }

        private static ComplexValueSimpleDescriptor convertSimpleValue(PropertySimple prop) {
            ComplexValueSimpleDescriptor ret = new ComplexValueSimpleDescriptor();
            ret.setPropertyName(prop.getName());
            ret.setValue(prop.getStringValue());

            return ret;
        }

        private static ComplexValueListDescriptor convertListValue(PropertyList prop) {
            ComplexValueListDescriptor ret = new ComplexValueListDescriptor();
            ret.setPropertyName(prop.getName());

            for (Property el : prop.getList()) {
                ComplexValueDescriptor value = convertValue(el);
                addToJAXBElementList(ret.getComplexValue(), Object.class, value, getTagName(value));
            }

            return ret;
        }

        private static ComplexValueMapDescriptor convertMapValue(PropertyMap prop) {
            ComplexValueMapDescriptor ret = new ComplexValueMapDescriptor();
            ret.setPropertyName(prop.getName());

            for (Property el : prop.getMap().values()) {
                ComplexValueDescriptor value = convertValue(el);
                addToJAXBElementList(ret.getComplexValue(), Object.class, value, getTagName(value));
            }

            return ret;
        }

        private static <T> void addToJAXBElementList(List<JAXBElement<? extends T>> list, Class<T> baseClass,
            T property, QName tagName) {
            JAXBElement<? extends T> el = new JAXBElement<T>(tagName, baseClass, property);
            list.add(el);
        }
    }

    private static class ToConfigurationAndDefinition {

        public static ConfigurationAndDefinition createConfigurationAndDefinition(
            ConfigurationInstanceDescriptor descriptor) {
            ConfigurationAndDefinition ret = new ConfigurationAndDefinition();
            Configuration configuration = new Configuration();
            ConfigurationDefinition definition = new ConfigurationDefinition(null, null);

            ret.configuration = configuration;
            ret.definition = definition;

            for (JAXBElement<?> el : descriptor.getConfigurationProperty()) {
                Object childDescriptor = el.getValue();
                add(definition, configuration, null, null, childDescriptor);
            }

            return ret;
        }

        private static void add(ConfigurationDefinition configurationDefinition, Configuration configuration,
            PropertyDefinition parentDef, Property parentProp, Object propertyInstance) {
            Property prop = null;
            PropertyDefinition def = null;

            if (propertyInstance instanceof SimplePropertyInstanceDescriptor) {
                def = convert((ConfigurationProperty) propertyInstance);
                def.setConfigurationDefinition(configurationDefinition);

                SimplePropertyInstanceDescriptor simpleInstance = (SimplePropertyInstanceDescriptor) propertyInstance;

                PropertySimple simpleProp = new PropertySimple(simpleInstance.getName(), simpleInstance.getValue());
                prop = simpleProp;
            } else if (propertyInstance instanceof ListPropertyInstanceDescriptor) {
                def = convert((ConfigurationProperty) propertyInstance);
                def.setConfigurationDefinition(configurationDefinition);

                ListPropertyInstanceDescriptor listInstance = (ListPropertyInstanceDescriptor) propertyInstance;
                PropertyList listProp = new PropertyList(listInstance.getName());

                PropertyDefinition memberDefinition = ((PropertyDefinitionList) def).getMemberDefinition();

                if (listInstance.getValues() != null) {
                    for (JAXBElement<?> val : listInstance.getValues().getComplexValue()) {
                        ComplexValueDescriptor valDesc = (ComplexValueDescriptor) val.getValue();
                        Property child = convert(memberDefinition, valDesc);
                        listProp.add(child);
                    }
                }

                prop = listProp;
            } else if (propertyInstance instanceof MapPropertyInstanceDescriptor) {
                def = convert((ConfigurationProperty) propertyInstance);
                def.setConfigurationDefinition(configurationDefinition);

                MapPropertyInstanceDescriptor mapInstance = (MapPropertyInstanceDescriptor) propertyInstance;
                PropertyMap mapProp = new PropertyMap(mapInstance.getName());

                if (mapInstance.getValues() != null) {
                    for (JAXBElement<?> val : mapInstance.getValues().getComplexValue()) {
                        ComplexValueDescriptor valueDesc = (ComplexValueDescriptor) val.getValue();
                        PropertyDefinition valueDefinition = ((PropertyDefinitionMap) def).get(valueDesc
                            .getPropertyName());

                        Property child = convert(valueDefinition, valueDesc);
                        mapProp.put(child);
                    }
                }

                prop = mapProp;
            } else {
                throw new IllegalArgumentException("Unsupported property instance type: " + propertyInstance.getClass());
            }

            if (parentDef != null) {
                if (parentDef instanceof PropertyDefinitionList) {
                    def.setParentPropertyListDefinition((PropertyDefinitionList) parentDef);
                } else if (parentDef instanceof PropertyDefinitionMap) {
                    def.setParentPropertyMapDefinition((PropertyDefinitionMap) parentDef);
                }
            } else {
                configurationDefinition.put(def);
            }

            prop.setConfiguration(configuration);
            if (parentProp != null) {
                if (parentProp instanceof PropertyList) {
                    prop.setParentList((PropertyList) parentProp);
                } else if (parentProp instanceof PropertyMap) {
                    prop.setParentMap((PropertyMap) parentProp);
                }
            } else {
                configuration.put(prop);
            }
        }

        private static PropertyDefinition convert(ConfigurationProperty def) {
            try {
                ConfigurationDescriptor tmp = new ConfigurationDescriptor();
                tmp.getConfigurationProperty().add(
                    new JAXBElement<ConfigurationProperty>(getTagName(def), ConfigurationProperty.class, def));
                ConfigurationDefinition configDef = ConfigurationMetadataParser.parse(null, tmp);

                return configDef.getPropertyDefinitions().values().iterator().next();
            } catch (InvalidPluginDescriptorException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private static Property convert(PropertyDefinition definition, ComplexValueDescriptor value) {
            Property ret = null;

            if (value instanceof ComplexValueSimpleDescriptor) {
                ret = new PropertySimple(value.getPropertyName(), ((ComplexValueSimpleDescriptor) value).getValue());
            } else if (value instanceof ComplexValueListDescriptor) {
                ComplexValueListDescriptor listValue = (ComplexValueListDescriptor) value;

                PropertyDefinitionList listDefinition = (PropertyDefinitionList) definition;

                PropertyList list = new PropertyList(value.getPropertyName());

                for (JAXBElement<?> val : listValue.getComplexValue()) {
                    Property child = convert(listDefinition.getMemberDefinition(),
                        (ComplexValueDescriptor) val.getValue());

                    list.add(child);
                }
                ret = list;
            } else if (value instanceof ComplexValueMapDescriptor) {
                ComplexValueMapDescriptor mapValue = (ComplexValueMapDescriptor) value;

                PropertyMap map = new PropertyMap(value.getPropertyName());
                PropertyDefinitionMap mapDefinition = (PropertyDefinitionMap) definition;

                for (JAXBElement<?> val : mapValue.getComplexValue()) {
                    ComplexValueDescriptor childDesc = (ComplexValueDescriptor) val.getValue();

                    PropertyDefinition childDefinition = mapDefinition.get(childDesc.getPropertyName());

                    Property child = convert(childDefinition, childDesc);
                    map.put(child);
                }

                ret = map;
            }

            if (ret.getName() == null) {
                ret.setName(definition.getName());
            }

            return ret;
        }
    }

    /**
     * A configuration instance is a combination of a configuration definition and a concrete
     * configuration instance with defined values. This is used during the config synchronization
     * to output the default configuration of an importer directly in the export file so that the
     * users have an easy way of modifying that configuration.
     * 
     * @param definition
     * @param configuration
     * @return
     */
    public static ConfigurationInstanceDescriptor createConfigurationInstance(ConfigurationDefinition definition,
        Configuration configuration) {
        return ToDescriptor.createConfigurationInstance(definition, configuration);
    }

    public static ConfigurationAndDefinition createConfigurationAndDefinition(ConfigurationInstanceDescriptor descriptor) {
        return ToConfigurationAndDefinition.createConfigurationAndDefinition(descriptor);
    }
}
