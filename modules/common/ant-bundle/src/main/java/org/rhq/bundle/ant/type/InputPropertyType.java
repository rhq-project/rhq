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
 * An Ant task that defines a basic property that the user provides as input for deployment of a bundle. Typically, the
 * property will be substituted into configuration files from the bundle during deployment - see ReplaceTask.
 *
 * If the deployment script is invoked from the GUI, the user will be prompted for values for any input properties that
 * are defined via this task. If the script is invoked from the command line, the properties must be passed using the -D
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

    public void init() {
        validateAttributes();

        ConfigurationDefinition configDef = getProject().getConfigurationDefinition();
        PropertySimpleType propSimpleType = PropertySimpleType.fromXmlName(this.type);
        PropertyDefinitionSimple propDef = new PropertyDefinitionSimple(this.name, this.description, this.required,
                propSimpleType);
        configDef.put(propDef);
    }
    
    public void execute() throws BuildException {
        String value = getProject().getProperty(this.name);
        if (value == null) {
            value = this.defaultValue;
        }
        if (value == null && this.required) {
            throw new BuildException("No value was specified for required input property '" + this.name
                      + "', and no default is defined for the property.");
        }
        String valueString = (value != null) ? "'" + value + "'" : "<null>";
        log("Initializing input property '" + this.name + "' with value " + valueString + "...");

        PropertySimple prop = new PropertySimple(this.name, value);
        ConfigurationDefinition configDef = getProject().getConfigurationDefinition();
        Configuration config = getProject().getConfiguration();
        // TODO: validate the config
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
        try {
            PropertySimpleType.fromXmlName(this.type);
        } catch (IllegalArgumentException e) {
            throw new BuildException("Illegal value for 'type' attribute: " + this.type);
        }
        if (this.defaultValue == null) {
            if (!this.required) {
                log("No default value was specified for optional input property '" + this.name + "'.", Project.MSG_WARN);
            }
        }
    }
}