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

package org.rhq.enterprise.server.configuration.metadata;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

import java.util.HashSet;
import java.util.Set;

public class ConfigurationDefinitionUpdateReport {

    private ConfigurationDefinition configurationDef;

    private Set<PropertyDefinition> newPropertyDefs = new HashSet<PropertyDefinition>();

    private Set<PropertyDefinition> updatedPropertyDefs = new HashSet<PropertyDefinition>();

    public ConfigurationDefinitionUpdateReport(ConfigurationDefinition configurationDefinition) {
        configurationDef = configurationDefinition;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDef;
    }

    public void addNewPropertyDefinition(PropertyDefinition propertyDef) {
        if (propertyDef instanceof PropertyDefinitionSimple) {
            newPropertyDefs.add(propertyDef);
        }
    }

    public Set<PropertyDefinition> getNewPropertyDefinitions() {
        return newPropertyDefs;
    }

    public void addUpdatedPropertyDefinition(PropertyDefinition propertyDef) {
        if (propertyDef instanceof PropertyDefinitionSimple) {
            updatedPropertyDefs.add(propertyDef);
        }
    }

    public Set<PropertyDefinition> getUpdatedPropertyDefinitions() {
        return updatedPropertyDefs;
    }

}
