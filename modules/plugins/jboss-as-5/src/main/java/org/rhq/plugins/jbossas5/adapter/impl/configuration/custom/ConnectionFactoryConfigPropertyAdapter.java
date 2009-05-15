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
package org.rhq.plugins.jbossas5.adapter.impl.configuration.custom;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.values.MapCompositeValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.plugins.jbossas5.adapter.api.AbstractPropertyListAdapter;

/**
 * This class maps between a {@link PropertyList} of a {@link PropertyMap}s of
 * name, type and value {@link PropertySimple}s and a
 * {@link MapCompositeValueSupport} where the keys are the config-property names
 * and values are config-property values. We need a custom adapter for this
 * because we need to support editing the type of the config-property entries.
 * <p>
 * JBoss Profile service exposes a type of a config property in a slightly
 * "interesting" manner. A config-property defined as
 * <p>
 * <code>
 * &lt;config-property name="property" type="java.lang.String"&gt;value&lt;/config-property&gt;
 * </code>
 * <p>
 * is exposed as a pair of entries in the map:
 * <p>
 * <code>
 * [key = "property", value = "value"]
 * <br/>
 * [key = "property.type", value = "java.lang.String"]
 * </code>
 * 
 * @author Lukas Krejci
 */
public class ConnectionFactoryConfigPropertyAdapter extends
		AbstractPropertyListAdapter {

	public MetaValue convertToMetaValue(PropertyList property,
			PropertyDefinitionList propertyDefinition, MetaType metaType) {

		MapCompositeValueSupport valuesMap = new MapCompositeValueSupport(
				metaType);

		populateMetaValueFromProperty(property, valuesMap, propertyDefinition);

		return valuesMap;
	}

	public void populateMetaValueFromProperty(PropertyList property,
			MetaValue metaValue, PropertyDefinitionList propertyDefinition) {
		MapCompositeValueSupport valuesMap = (MapCompositeValueSupport) metaValue;

		for (Property p : property.getList()) {
			PropertyMap propertyMap = (PropertyMap) p;

			PropertySimple nameProperty = (PropertySimple) propertyMap
					.get("name");
			PropertySimple typeProperty = (PropertySimple) propertyMap
					.get("type");
			PropertySimple valueProperty = (PropertySimple) propertyMap
					.get("value");

			String configPropertyName = nameProperty.getStringValue();
			String configPropertyType = typeProperty.getStringValue();
			String configPropertyValue = valueProperty.getStringValue();

			if (!ConfigPropertyType.fromTypeName(configPropertyType)
					.isValueValid(configPropertyValue)) {
				String message = "The value of Config Property '"
						+ configPropertyName
						+ "' has an invalid value for its type.";

				property.setErrorMessage(message);

				throw new IllegalArgumentException(message);
			}

			valuesMap.put(configPropertyName, new SimpleValueSupport(
					SimpleMetaType.STRING, valueProperty.getStringValue()));
			valuesMap.put(configPropertyName + ".type", new SimpleValueSupport(
					SimpleMetaType.STRING, typeProperty.getStringValue()));
		}
	}

	public void populatePropertyFromMetaValue(PropertyList property,
			MetaValue metaValue, PropertyDefinitionList propertyDefinition) {
		MapCompositeValueSupport valuesMap = (MapCompositeValueSupport) metaValue;

		for (ConfigProperty configProperty : getConfigProperties(valuesMap)) {
			PropertySimple name = new PropertySimple("name", configProperty
					.getName());
			PropertySimple type = new PropertySimple("type", configProperty
					.getType().getTypeName());

			PropertySimple value = new PropertySimple("value", configProperty
					.getValue());

			property.add(new PropertyMap("config-property", name, type, value));
		}
	}

	private Collection<ConfigProperty> getConfigProperties(
			MapCompositeValueSupport valuesMap) {
		LinkedHashMap<String, ConfigProperty> results = new LinkedHashMap<String, ConfigProperty>();

		for (String key : valuesMap.getMetaType().keySet()) {
			String propertyName;
			ConfigPropertyType propertyType = null;
			Serializable propertyValue = null;

			if (key.endsWith(".type")) {
				propertyName = key.substring(0, key.length() - 5);

				SimpleValue typeName = (SimpleValue) valuesMap.get(key);

				propertyType = ConfigPropertyType.fromTypeName(typeName
						.getValue().toString());
			} else {
				propertyName = key;
				SimpleValue value = (SimpleValue) valuesMap.get(key);
				if (value != null) {
					propertyValue = value.getValue();
				}
			}

			ConfigProperty property = results.get(propertyName);
			if (property == null) {
				property = new ConfigProperty();
				property.setName(propertyName);
				results.put(propertyName, property);
			}

			if (propertyType != null) {
				property.setType(propertyType);
			}

			if (propertyValue != null) {
				property.setValue(propertyValue);
			}
		}
		return results.values();
	}

	private enum ConfigPropertyType {
		STRING(SimpleMetaType.STRING) {
			public boolean isValueValid(String value) {
				return true;
			}
		},
		CHARACTER(SimpleMetaType.CHARACTER) {
			public boolean isValueValid(String value) {
				return value == null || value.length() == 1;
			}
		},
		BOOLEAN(SimpleMetaType.BOOLEAN) {
			public boolean isValueValid(String value) {
				if (value == null)
					return false;

				return value.equalsIgnoreCase("true")
						|| value.equalsIgnoreCase("false");
			}
		},
		BYTE(SimpleMetaType.BYTE) {
			public boolean isValueValid(String value) {
				if (value == null)
					return false;

				try {
					Byte.parseByte(value);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		},
		SHORT(SimpleMetaType.SHORT) {
			public boolean isValueValid(String value) {
				if (value == null)
					return false;

				try {
					Short.parseShort(value);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		},
		INTEGER(SimpleMetaType.INTEGER) {
			public boolean isValueValid(String value) {
				if (value == null)
					return false;

				try {
					Integer.parseInt(value);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		},
		LONG(SimpleMetaType.LONG) {
			public boolean isValueValid(String value) {
				if (value == null)
					return false;

				try {
					Long.parseLong(value);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		},
		FLOAT(SimpleMetaType.FLOAT) {
			public boolean isValueValid(String value) {
				if (value == null)
					return false;

				try {
					Float.parseFloat(value);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		},
		DOUBLE(SimpleMetaType.DOUBLE) {
			public boolean isValueValid(String value) {
				if (value == null)
					return false;

				try {
					Double.parseDouble(value);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		},
		INVALID(SimpleMetaType.VOID) {
			public boolean isValueValid(String value) {
				return false;
			}
		};

		private SimpleMetaType metaType;

		private ConfigPropertyType(SimpleMetaType metaType) {
			this.metaType = metaType;
		}

		public static ConfigPropertyType fromTypeName(String typeName) {
			for (ConfigPropertyType type : values()) {
				if (type.getTypeName().equals(typeName)) {
					return type;
				}
			}
			return INVALID;
		}

		public String getTypeName() {
			return metaType.getClassName();
		}

		public SimpleMetaType getMetaType() {
			return metaType;
		}

		public abstract boolean isValueValid(String value);

		public String toString() {
			return name().toLowerCase();
		}
	}

	private static class ConfigProperty {
		private String name;
		private ConfigPropertyType type;
		private Serializable value;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ConfigPropertyType getType() {
			return type;
		}

		public void setType(ConfigPropertyType type) {
			this.type = type;
		}

		public Serializable getValue() {
			return value;
		}

		public void setValue(Serializable value) {
			this.value = value;
		}

		public String toString() {
			return "ConfigProperty[" + name + "(" + type + ") : " + value + "]";
		}

		public SimpleValue getTypeDefinitionValue() {
			return new SimpleValueSupport(SimpleMetaType.STRING, type
					.getTypeName());
		}

		public SimpleValue getValueDefinitionValue() {
			return new SimpleValueSupport(type.getMetaType(), value);
		}
	}
}
