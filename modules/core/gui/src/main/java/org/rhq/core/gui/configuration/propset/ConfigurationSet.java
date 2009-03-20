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
package org.rhq.core.gui.configuration.propset;

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
public class ConfigurationSet {
    private ConfigurationDefinition configurationDefinition;
    private List<ConfigurationSetMember> members;
    private Configuration aggregateConfiguration;

    public ConfigurationSet(ConfigurationDefinition configurationDefinition, List<ConfigurationSetMember> members) {
        if (configurationDefinition == null)
            throw new IllegalArgumentException("configurationDefinition parameter is null.");
        this.configurationDefinition = configurationDefinition;
        if (members == null)
            throw new IllegalArgumentException("members parameter is null.");
        this.members = members;
        this.aggregateConfiguration = new Configuration();
        calculateAggregateConfiguration();
    }

    public void calculateAggregateConfiguration() {
        this.aggregateConfiguration.getMap().clear();
        if (this.members.isEmpty())
            return;
        Map<String, PropertyDefinition> childPropertyDefinitions = this.configurationDefinition
            .getPropertyDefinitions();
        List<AbstractPropertyMap> sourceParentPropertyMaps = new ArrayList();
        for (ConfigurationSetMember member : this.members)
            sourceParentPropertyMaps.add(member.getConfiguration());
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
            aggregateProperty(childPropertyDefinition, sourceParentPropertyMaps, this.aggregateConfiguration);
    }

    public void applyAggregateConfiguration() {
        if (this.members.isEmpty())
            return;
        Map<String, PropertyDefinition> childPropertyDefinitions = this.configurationDefinition
            .getPropertyDefinitions();
        List<AbstractPropertyMap> sourceParentPropertyMaps = new ArrayList();
        for (ConfigurationSetMember member : this.members)
            sourceParentPropertyMaps.add(member.getConfiguration());
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
            mergeProperty(childPropertyDefinition, sourceParentPropertyMaps, this.aggregateConfiguration);
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public List<ConfigurationSetMember> getMembers() {
        return members;
    }

    public Configuration getAggregateConfiguration() {
        return aggregateConfiguration;
    }

    private static void aggregateProperty(PropertyDefinition propertyDefinition,
        List<AbstractPropertyMap> sourceParentPropertyMaps, AbstractPropertyMap targetParentPropertyMap) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            String sampleValue = getSimpleValue(sourceParentPropertyMaps.get(0), propertyDefinition.getName());
            boolean valuesHomogenous = true;
            for (int i = 1; i < sourceParentPropertyMaps.size(); i++) {
                String value = getSimpleValue(sourceParentPropertyMaps.get(i), propertyDefinition.getName());
                if ((value == null && sampleValue != null) || (value != null && !value.equals(sampleValue))) {
                    valuesHomogenous = false;
                    break;
                }
            }
            PropertySimple propertySimple = new PropertySimple(propertyDefinition.getName(),
                (valuesHomogenous) ? sampleValue : null);
            targetParentPropertyMap.put(propertySimple);
            if (valuesHomogenous)
                // Set override to true so the config renderer will know the prop is homogenous.
                propertySimple.setOverride(true);
        }
        // If the property is a Map, recurse into it and aggregate its child properties.
        else if (propertyDefinition instanceof PropertyDefinitionMap) {
            List<AbstractPropertyMap> nestedSourceParentPropertyMaps = new ArrayList();
            for (AbstractPropertyMap sourceParentPropertyMap : sourceParentPropertyMaps) {
                PropertyMap nestedSourceParentPropertyMap = sourceParentPropertyMap
                    .getMap(propertyDefinition.getName());
                nestedSourceParentPropertyMaps
                    .add((nestedSourceParentPropertyMap != null) ? nestedSourceParentPropertyMap : new PropertyMap(
                        propertyDefinition.getName()));
            }
            PropertyMap targetPropertyMap = new PropertyMap(propertyDefinition.getName());
            targetParentPropertyMap.put(targetPropertyMap);
            aggregatePropertyMap((PropertyDefinitionMap) propertyDefinition, nestedSourceParentPropertyMaps,
                targetPropertyMap);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            PropertyList targetPropertyList = new PropertyList(propertyDefinition.getName());
            targetParentPropertyMap.put(targetPropertyList);
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                // TODO: How do we aggregate Lists of Maps? Not trivial...
            }
        }
    }

    private static void aggregatePropertyMap(PropertyDefinitionMap propertyDefinitionMap,
        List<AbstractPropertyMap> sourceParentPropertyMaps, AbstractPropertyMap targetParentPropertyMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        if (!childPropertyDefinitions.isEmpty()) {
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
                aggregateProperty(childPropertyDefinition, sourceParentPropertyMaps, targetParentPropertyMap);
        } else {
            aggregateOpenPropertyMap(sourceParentPropertyMaps, targetParentPropertyMap);
        }
    }

    private static void aggregateOpenPropertyMap(List<AbstractPropertyMap> sourceParentPropertyMaps,
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

    private static void mergeProperty(PropertyDefinition propertyDefinition,
        List<AbstractPropertyMap> memberParentPropertyMaps, AbstractPropertyMap aggregateParentPropertyMap) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            PropertySimple propertySimple = aggregateParentPropertyMap.getSimple(propertyDefinition.getName());
            if (propertySimple != null && propertySimple.getOverride() != null && propertySimple.getOverride()) {
                for (AbstractPropertyMap sourceParentPropertyMap : memberParentPropertyMaps) {
                    PropertySimple sourcePropertySimple = sourceParentPropertyMap.getSimple(propertyDefinition
                        .getName());
                    if (sourcePropertySimple == null) {
                        sourcePropertySimple = new PropertySimple(propertyDefinition.getName(), propertySimple
                            .getStringValue());
                        sourceParentPropertyMap.put(sourcePropertySimple);
                    } else {
                        sourcePropertySimple.setStringValue(propertySimple.getStringValue());
                    }
                }
            }
        }
        // If the property is a Map, recurse into it and merge its child properties.
        else if (propertyDefinition instanceof PropertyDefinitionMap) {
            List<AbstractPropertyMap> nestedSourceParentPropertyMaps = new ArrayList();
            for (AbstractPropertyMap sourceParentPropertyMap : memberParentPropertyMaps)
                nestedSourceParentPropertyMaps.add(sourceParentPropertyMap.getMap(propertyDefinition.getName()));
            PropertyMap aggregatePropertyMap = aggregateParentPropertyMap.getMap(propertyDefinition.getName());
            aggregateParentPropertyMap.put(aggregatePropertyMap);
            mergePropertyMap((PropertyDefinitionMap) propertyDefinition, nestedSourceParentPropertyMaps,
                aggregatePropertyMap);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            PropertyList aggregatePropertyList = aggregateParentPropertyMap.getList(propertyDefinition.getName());
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                // TODO: How do we merge Lists of Maps? Not trivial...
            }
        }
    }

    private static void mergePropertyMap(PropertyDefinitionMap propertyDefinitionMap,
        List<AbstractPropertyMap> memberParentPropertyMaps, AbstractPropertyMap aggregateParentPropertyMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        if (!childPropertyDefinitions.isEmpty()) {
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
                mergeProperty(childPropertyDefinition, memberParentPropertyMaps, aggregateParentPropertyMap);
        } else {
            mergeOpenPropertyMap(memberParentPropertyMaps, aggregateParentPropertyMap);
        }
    }

    private static void mergeOpenPropertyMap(List<AbstractPropertyMap> memberParentPropertyMaps,
        AbstractPropertyMap aggregateParentPropertyMap) {
        for (String aggregateMemberPropertyName : aggregateParentPropertyMap.getMap().keySet()) {
            PropertySimple aggregateMemberProperty = aggregateParentPropertyMap.getSimple(aggregateMemberPropertyName);
            if (aggregateMemberProperty != null && aggregateMemberProperty.getOverride() != null
                && aggregateMemberProperty.getOverride()) {
                for (AbstractPropertyMap sourceParentPropertyMap : memberParentPropertyMaps) {
                    PropertySimple sourcePropertySimple = sourceParentPropertyMap
                        .getSimple(aggregateMemberPropertyName);
                    if (sourcePropertySimple == null) {
                        sourcePropertySimple = new PropertySimple(aggregateMemberPropertyName, aggregateMemberProperty
                            .getStringValue());
                        sourceParentPropertyMap.put(sourcePropertySimple);
                    } else {
                        sourcePropertySimple.setStringValue(aggregateMemberProperty.getStringValue());
                    }
                }
            }
        }
    }

    private static Map<String, Map<String, Integer>> createMemberNameValueFrequenciesMap(
        List<AbstractPropertyMap> sourceParentPropertyMaps) {
        Map<String, Map<String, Integer>> nameValueFrequenciesMap = new HashMap();
        for (AbstractPropertyMap map : sourceParentPropertyMaps) {
            for (String propertyName : map.getMap().keySet()) {
                PropertySimple propertySimple = map.getSimple(propertyName);
                String propertyValue = (propertySimple != null) ? propertySimple.getStringValue() : null;
                Map<String, Integer> valueFrequencies = nameValueFrequenciesMap.get(propertyName);
                if (valueFrequencies == null) {
                    valueFrequencies = new HashMap();
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
}
