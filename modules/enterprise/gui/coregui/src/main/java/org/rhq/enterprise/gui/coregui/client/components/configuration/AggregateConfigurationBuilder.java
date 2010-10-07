/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.components.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * @author Ian Springer
 */
public class AggregateConfigurationBuilder {
    public static Configuration buildAggregateConfiguration(List<Configuration> configurations,
                                                     ConfigurationDefinition configurationDefinition) {
        Configuration aggregateConfiguration = new Configuration();

        if (configurations.isEmpty()) {
            return aggregateConfiguration;
        }

        Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values()) {
            buildAggregateProperty(childPropertyDefinition, configurations, aggregateConfiguration);
        }

        return aggregateConfiguration;
    }

    private static void buildAggregateProperty(PropertyDefinition propertyDefinition,
        List<? extends AbstractPropertyMap> sourceParentPropertyMaps, AbstractPropertyMap targetParentPropertyMap) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            String sampleValue = getSimpleValue(sourceParentPropertyMaps.get(0), propertyDefinition.getName());
            boolean valuesHomogeneous = true;
            for (int i = 1; i < sourceParentPropertyMaps.size(); i++) {
                String value = getSimpleValue(sourceParentPropertyMaps.get(i), propertyDefinition.getName());
                if ((value == null && sampleValue != null) || (value != null && !value.equals(sampleValue))) {
                    valuesHomogeneous = false;
                    break;
                }
            }
            PropertySimple propertySimple = new PropertySimple(propertyDefinition.getName(),
                (valuesHomogeneous) ? sampleValue : null);
            targetParentPropertyMap.put(propertySimple);
            if (valuesHomogeneous) {
                // Set override to true so the config renderer will know the prop is homogeneous.
                propertySimple.setOverride(true);
            }
        }
        // If the property is a Map, recurse into it and group together its child properties.
        else if (propertyDefinition instanceof PropertyDefinitionMap) {
            List<AbstractPropertyMap> nestedSourceParentPropertyMaps = new ArrayList<AbstractPropertyMap>();
            for (AbstractPropertyMap sourceParentPropertyMap : sourceParentPropertyMaps) {
                PropertyMap nestedSourceParentPropertyMap = sourceParentPropertyMap
                    .getMap(propertyDefinition.getName());
                nestedSourceParentPropertyMaps
                    .add((nestedSourceParentPropertyMap != null) ? nestedSourceParentPropertyMap : new PropertyMap(
                        propertyDefinition.getName()));
            }
            PropertyMap targetPropertyMap = new PropertyMap(propertyDefinition.getName());
            targetParentPropertyMap.put(targetPropertyMap);
            buildAggregatePropertyMap((PropertyDefinitionMap) propertyDefinition, nestedSourceParentPropertyMaps,
                targetPropertyMap);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            PropertyList targetPropertyList = new PropertyList(propertyDefinition.getName());
            targetParentPropertyMap.put(targetPropertyList);
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                // TODO: How do we group together Lists of Maps? Not trivial...
            }
        }
    }

    private static void buildAggregatePropertyMap(PropertyDefinitionMap propertyDefinitionMap,
        List<AbstractPropertyMap> sourceParentPropertyMaps, AbstractPropertyMap targetParentPropertyMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        if (!childPropertyDefinitions.isEmpty()) {
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
                buildAggregateProperty(childPropertyDefinition, sourceParentPropertyMaps, targetParentPropertyMap);
        } else {
            buildAggregateOpenPropertyMap(sourceParentPropertyMaps, targetParentPropertyMap);
        }
    }

    private static void buildAggregateOpenPropertyMap(List<AbstractPropertyMap> sourceParentPropertyMaps,
        AbstractPropertyMap targetParentPropertyMap) {
        Map<String, Map<String, Integer>> memberNameValueFrequenciesMap = createMemberNameValueFrequenciesMap(sourceParentPropertyMaps);
        for (String memberName : memberNameValueFrequenciesMap.keySet()) {
            // Add each unique member to the target map, so the renderer will be able to display it on the main
            // config page.
            PropertySimple member = new PropertySimple(memberName, null);
            targetParentPropertyMap.put(member);
            // Also add it to each of the source config maps that don't already contain it, so that they can be
            // rendered as unset on the propSet page.
            for (AbstractPropertyMap map : sourceParentPropertyMaps) {
                if (map.get(memberName) == null)
                    map.put(new PropertySimple(memberName, null));
            }
            Map<String, Integer> valueFrequencies = memberNameValueFrequenciesMap.get(memberName);
            if (valueFrequencies.size() == 1
                && valueFrequencies.values().iterator().next() == sourceParentPropertyMaps.size()) {
                // Set override to true so the renderers will know the prop is homogenous.
                member.setOverride(true);
                // And set the value, so it can be displayed on the main config page.
                member.setStringValue(valueFrequencies.keySet().iterator().next());
            }
        }
    }

    private static Map<String, Map<String, Integer>> createMemberNameValueFrequenciesMap(
        List<AbstractPropertyMap> sourceParentPropertyMaps) {
        Map<String, Map<String, Integer>> nameValueFrequenciesMap = new HashMap<String, Map<String, Integer>>();
        for (AbstractPropertyMap map : sourceParentPropertyMaps) {
            for (String propertyName : map.getMap().keySet()) {
                PropertySimple propertySimple = map.getSimple(propertyName);
                String propertyValue = (propertySimple != null) ? propertySimple.getStringValue() : null;
                Map<String, Integer> valueFrequencies = nameValueFrequenciesMap.get(propertyName);
                if (valueFrequencies == null) {
                    valueFrequencies = new HashMap<String, Integer>();
                    nameValueFrequenciesMap.put(propertyName, valueFrequencies);
                }
                Integer valueFrequency = (valueFrequencies.containsKey(propertyValue)) ? (valueFrequencies
                    .get(propertyValue) + 1) : 1;
                valueFrequencies.put(propertyValue, valueFrequency);
            }
        }
        return nameValueFrequenciesMap;
    }

    private static String getSimpleValue(AbstractPropertyMap parentPropertyMap, String propertyName) {
        PropertySimple samplePropertySimple = parentPropertyMap.getSimple(propertyName);
        return (samplePropertySimple != null) ? samplePropertySimple.getStringValue() : null;
    }

    private AggregateConfigurationBuilder() {
    }
}
