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
package org.rhq.core.clientapi.agent.configuration;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * Utility methods for working with {@link Configuration}s.
 *
 * @author Ian Springer
 * 
 * @deprecated use the utility found in the domain module -org.rhq.core.domain.configuration.ConfigurationUtility
 */
public abstract class ConfigurationUtility {

    /**
     * This will populate the given configuration definition with a default template.
     * A default template will only be created if one or more properties are required
     * or have default values. If no property definition is required or has a default value,
     * the default template will remain <code>null</code> in the given config definition.
     * 
     * Note that if the given configuration definition already has a default template defined
     * for it, this method is a no-op and will return immediately.
     * 
     * @param configDef the configuration definition whose default template is to be created and set
     *
     * @deprecated use the utility found in the domain module -org.rhq.core.domain.configuration.ConfigurationUtility
     */
    public static void initializeDefaultTemplate(ConfigurationDefinition configDef) {
        org.rhq.core.domain.configuration.ConfigurationUtility.initializeDefaultTemplate(configDef);
    }

    /**
     * Given a configuration definition, this will build and return a "default configuration" that
     * can be validated with the definition. All required properties are set and all properties
     * that define a default value are also set. If a required property does not have a default
     * value defined in the definition, the property value will be set to <code>null</code>.
     *
     * Use this to help create the definition's default template.
     * 
     * @param configurationDefinition the configuration definition whose default configuration is to be created
     * @return configuration the default configuration
     *
     * @deprecated use the utility found in the domain module -org.rhq.core.domain.configuration.ConfigurationUtility
     */
    public static Configuration createDefaultConfiguration(ConfigurationDefinition configurationDefinition) {
        return org.rhq.core.domain.configuration.ConfigurationUtility
            .createDefaultConfiguration(configurationDefinition);
    }

    /**
     * "Normalize" the given configuration according to the given configuration definition. That is, for any optional
     * properties that are not defined in the top-level configuration Map or any sub-Maps, set them. Simple properties
     * are set with a null value, Map properties as an empty Map, and List properties as an empty List.
     *
     * @param configuration           the configuration to be normalized
     * @param configurationDefinition the configuration definition to normalize the configuration against
     *
     * @deprecated use the utility found in the domain module -org.rhq.core.domain.configuration.ConfigurationUtility
     */
    public static void normalizeConfiguration(@NotNull Configuration configuration,
        @Nullable ConfigurationDefinition configurationDefinition) {
        org.rhq.core.domain.configuration.ConfigurationUtility.normalizeConfiguration(configuration,
            configurationDefinition, false, false);
    }

    /**
     * Validate the given configuration according to the given configuration definition. That is, check that any
     * required properties in the top-level configuration Map or any sub-Maps, are defined and, in the case of simple
     * properties, check that they have a non-null value. A list of messages describing any errors that were found is
     * returned. Additionally, any undefined or null simple properties will be assigned a value of "".
     *
     * @param  configuration           the configuration to be validated
     * @param  configurationDefinition the configuration definition to validate the configuration against
     *
     * @return a list of messages describing any errors that were found
     *
     * @deprecated use the utility found in the domain module -org.rhq.core.domain.configuration.ConfigurationUtility
     */
    @NotNull
    public static List<String> validateConfiguration(@NotNull Configuration configuration,
        @Nullable ConfigurationDefinition configurationDefinition) {
        return org.rhq.core.domain.configuration.ConfigurationUtility.validateConfiguration(configuration,
            configurationDefinition);
    }
}