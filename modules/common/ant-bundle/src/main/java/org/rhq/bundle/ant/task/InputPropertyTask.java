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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.bundle.ant.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * @author Ian Springer
 */
public class InputPropertyTask extends AbstractBundleTask {
    private String name;
    private String description;
    private boolean required = true;
    private String defaultValue;
    private String type = PropertyType.STRING.value();
    private PropertyType propertyType;

    @Override
    public void execute() throws BuildException {
        validateAttributes();

        if (this.defaultValue == null) {
            if (!this.required) {
                log("No default value was specified for optional input property '" + this.name + "'.", Project.MSG_WARN);
            }
        }
        String value = getProject().getProperty(this.name);
        if (value == null) {
            value = this.defaultValue;            
        }
        if (value == null && this.required) {
            throw new BuildException("No value was specified for required input property '" + this.name
                      + "', and no default is defined for the property.");
        }

        String valueString = (value != null) ? "'" + value + "'" : "<null>";
        log("Initializing input property '" + this.name + " with value " + valueString + "...");
        PropertySimple prop = new PropertySimple(this.name, value);

        ConfigurationDefinition configDef = getProject().getConfigurationDefinition();
        Configuration config = getProject().getConfiguration();
        // TODO: validate the config

        config.put(prop);
        return;
    }

    @Override
    public void maybeConfigure() throws BuildException {
        super.maybeConfigure();    //To change body of overridden methods use File | Settings | File Templates.
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
     * @exception org.apache.tools.ant.BuildException if an error occurs
     */
    protected void validateAttributes() throws BuildException {
        if (this.name == null) {
            throw new BuildException("The 'name' attribute is required.");
        }
        if (this.name.length() == 0) {
            throw new BuildException("The 'name' attribute must have a non-empty value.");
        }
        try {
            this.propertyType = PropertyType.fromValue(this.type);
        } catch (IllegalArgumentException e) {
            throw new BuildException("Illegal value for 'type' attribute: " + this.type);
        }

    }

    public enum PropertyType {
        STRING("string"),
        LONG_STRING("longString"),
        PASSWORD("password"),
        BOOLEAN("boolean"),
        INTEGER("integer"),
        LONG("long"),
        FLOAT("float"),
        DOUBLE("double"),
        FILE("file"),
        DIRECTORY("directory");

        private final String value;

        PropertyType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static PropertyType fromValue(String value) {
            for (PropertyType c: PropertyType.values()) {
                if (c.value.equals(value)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(value.toString());
        }
    }
}