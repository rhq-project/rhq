/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
 * * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.bundle.ant.type;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

/**
 * An Ant type that defines a basic property that the user provides as input for deployment of a bundle. Typically, the
 * property will be substituted into configuration files from the bundle during deployment - see ReplaceTask.
 *
 * If the deployment script is invoked from the GUI, the user will be prompted for values for any input properties that
 * are defined via this type. If the script is invoked from the command line, the properties must be passed using the -D
 * and/or -propertyfile options.
 *
 * @author Ian Springer
 */
public class InputPropertyType extends AbstractBundleType {
    private String name;
    private String description;
    private boolean required = true;
    private String defaultValue;
    private String type = PropertySimpleType.STRING.xmlName();

    public void init() throws BuildException {
        validateAttributes();

        ConfigurationDefinition configDef = getProject().getConfigurationDefinition();
        PropertySimpleType propType = PropertySimpleType.fromXmlName(this.type);
        PropertyDefinitionSimple propDef = new PropertyDefinitionSimple(this.name, this.description, this.required,
                propType);
        configDef.put(propDef);

        String value = getProject().getProperty(this.name);
        if (value == null) {
            value = this.defaultValue;
            getProject().setProperty(this.name, value);
        }

        boolean parseOnly = getProject().isParseOnly();
        if (!parseOnly) {
            if (value == null && this.required) {
                throw new BuildException("No value was specified for required input property '" + this.name
                          + "', and no default is defined for the property.");
            }
            validateValue(value, propType);
            String valueString = (value != null) ? "'" + value + "'" : "<null>";
            log("Initializing input property '" + this.name + "' with value " + valueString + "...");
        }

        Configuration config = getProject().getConfiguration();
        PropertySimple prop = new PropertySimple(this.name, value);
        config.put(prop);

        return;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     *
     * @throws org.apache.tools.ant.BuildException if an error occurs
     */
    protected void validateAttributes() throws BuildException {
        if (this.name == null) {
            throw new BuildException("The 'name' attribute is required.");
        }
        if (this.name.length() == 0) {
            throw new BuildException("The 'name' attribute must have a non-empty value.");
        }
        PropertySimpleType propType;
        try {
            propType = PropertySimpleType.fromXmlName(this.type);
        } catch (IllegalArgumentException e) {
            throw new BuildException("Illegal value for 'type' attribute: " + this.type);
        }
        if (this.defaultValue == null) {
            if (!this.required) {
                log("No default value was specified for optional input property '" + this.name + "'.", Project.MSG_WARN);
            }
        } else {
            // Make sure the default value is valid according to the property's type.
            try {
                validateValue(this.defaultValue, propType);
            } catch (RuntimeException e) {
                throw new BuildException("Default value '" + this.defaultValue
                        + "' is not valid according to 'type' attribute: " + this.type, e);
            }
        }
    }

    private void validateValue(String value, PropertySimpleType propType) {
        if (value != null) {
            try {
                switch (propType) {
                    case BOOLEAN:
                        if (!value.equals(Boolean.TRUE.toString()) && !value.equals(Boolean.FALSE.toString())) {
                            throw new RuntimeException("Illegal value for boolean property - value must be 'true' or 'false'." 
                                    + value);
                        }
                        break;
                    case DOUBLE:
                        Double.valueOf(value);
                        break;
                    case FLOAT:
                        Float.valueOf(value);
                        break;
                    case INTEGER:
                        Integer.valueOf(value);
                        break;
                    case LONG:
                        Long.valueOf(value);
                        break;
                }
            } catch (RuntimeException e) {
                throw new BuildException("'" + value + "' is not a legal value for input property '" + this.name
                        + "', which has type '" + this.type + "'.", e);
            }
        }
    }
}