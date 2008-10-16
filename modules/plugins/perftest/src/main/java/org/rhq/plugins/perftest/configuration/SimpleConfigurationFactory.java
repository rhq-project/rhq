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
package org.rhq.plugins.perftest.configuration;

import java.util.Collection;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * Basic implementation of the configuration factory that fills in an arbitrary value for each property.
 *
 * @author Jason Dobies
 */
public class SimpleConfigurationFactory implements ConfigurationFactory {
    // ConfigurationFactory Implementation  --------------------------------------------

    public Configuration generateConfiguration(ConfigurationDefinition definition) {
        Collection<PropertyDefinition> allDefinitions = definition.getPropertyDefinitions().values();

        Configuration configuration = new Configuration();

        for (PropertyDefinition propertyDefinition : allDefinitions) {
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple simple = (PropertyDefinitionSimple) propertyDefinition;
                String name = simple.getName();
                PropertySimple property = new PropertySimple(name, "value");
                configuration.put(property);
            }
        }

        return configuration;
    }
}