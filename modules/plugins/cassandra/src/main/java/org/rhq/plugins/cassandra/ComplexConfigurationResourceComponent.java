/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.cassandra;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
public class ComplexConfigurationResourceComponent extends MBeanResourceComponent<JMXComponent<?>> {

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public Configuration loadResourceConfiguration() {
        Configuration configuration =  super.loadResourceConfiguration();

        ConfigurationDefinition resourceConfigurationDefinition = this.resourceContext.getResourceType()
            .getResourceConfigurationDefinition();

        for (PropertyDefinition propertyDefinition : resourceConfigurationDefinition.getPropertyDefinitions().values()) {
            if (propertyDefinition instanceof PropertyDefinitionList) {

                EmsAttribute attribute = getEmsBean().getAttribute(propertyDefinition.getName());

                if (attribute != null) {
                    Object result = attribute.refresh();

                    PropertyList propertyList = new PropertyList(propertyDefinition.getName());

                    if (result instanceof Map) {
                        PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) ((PropertyDefinitionList) propertyDefinition)
                            .getMemberDefinition();
                        List<PropertyDefinition> subPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
                        String mapName = propertyDefinitionMap.getName();
                        String keyName = ((PropertyDefinitionSimple) subPropertyDefinitions.get(0)).getName();
                        String valueName = ((PropertyDefinitionSimple) subPropertyDefinitions.get(1)).getName();

                        Map<Object, Object> mapValue = (Map<Object, Object>) result;

                        PropertyMap propertyMap;
                        for (Entry<Object, Object> entry : mapValue.entrySet()) {
                            propertyMap = new PropertyMap(mapName);
                            propertyMap.put(new PropertySimple(keyName, entry.getKey().toString()));
                            propertyMap.put(new PropertySimple(valueName, entry.getValue().toString()));

                            propertyList.add(propertyMap);
                        }
                    } else if (result instanceof Set) {
                        String entryName = ((PropertyDefinitionSimple) ((PropertyDefinitionList) propertyDefinition)
                            .getMemberDefinition()).getName();

                        Set<Object> setValue = (Set<Object>) result;

                        for (Object entry : setValue) {
                            propertyList.add(new PropertySimple(entryName, entry.toString()));
                        }
                    } else if (result instanceof Object[]) {
                        String entryName = ((PropertyDefinitionSimple) ((PropertyDefinitionList) propertyDefinition)
                            .getMemberDefinition()).getName();

                        Object[] arrayValue = (Object[]) result;

                        for (Object entry : arrayValue) {
                            propertyList.add(new PropertySimple(entryName, entry.toString()));
                        }
                    }

                    if (propertyList.getList().size() != 0) {
                        configuration.put(propertyList);
                    }
                }
            }
        }

        return configuration;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // don't try to update the read only properties, it will fail
        super.updateResourceConfiguration(report, true);
    }
}
