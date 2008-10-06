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
package org.rhq.core.gui.configuration;

import java.util.Collection;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;

/**
 * A factory that generates a tree of JSF components that depicts a given collection of JON {@link Property}s from a
 * specified top-level map.
 *
 * @author Ian Springer
 */
public class MapInConfigurationUIComponentTreeFactory extends AbstractPropertyBagUIComponentTreeFactory {
    public MapInConfigurationUIComponentTreeFactory(ConfigUIComponent config, String mapName) {
        super(config, getPropertyDefinitions(config.getConfigurationDefinition(), mapName), config.getConfiguration()
            .getMap(mapName), false, createValueExpressionFormat(config.getConfigurationExpressionString(), mapName));
    }

    static String createValueExpressionFormat(String configurationExpressionString, String mapName) {
        StringBuilder expression = new StringBuilder();
        expression.append("#{");
        expression.append(unwrapExpressionString(configurationExpressionString));
        expression.append(".");
        expression.append(PROPERTY_MAP_VALUE_ACCESSOR_SUFFIX);
        expression.append("['").append(mapName).append("'].");
        expression.append(PROPERTY_MAP_VALUE_ACCESSOR_SUFFIX);
        expression.append("['%s']."); // property name
        expression.append(PROPERTY_SIMPLE_VALUE_ACCESSOR_SUFFIX);
        expression.append("}");
        return expression.toString();
    }

    private static Collection<PropertyDefinition> getPropertyDefinitions(
        ConfigurationDefinition configurationDefinition, String mapName) {
        return configurationDefinition.getPropertyDefinitionMap(mapName).getPropertyDefinitions().values();
    }
}