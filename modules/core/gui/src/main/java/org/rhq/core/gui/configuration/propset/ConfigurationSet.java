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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
public class ConfigurationSet
{
    private ConfigurationDefinition configurationDefinition;
    private List<ConfigurationSetMember> members;
    private Configuration aggregateConfiguration;

    public ConfigurationSet(ConfigurationDefinition configurationDefinition, List<ConfigurationSetMember> members)
    {
        this.configurationDefinition = configurationDefinition;
        this.members = members;
        this.aggregateConfiguration = new Configuration();
        Map<String, PropertyDefinition> childPropertyDefinitions = configurationDefinition.getPropertyDefinitions();
        List<AbstractPropertyMap> sourceParentPropertyMaps = new ArrayList();
        for (ConfigurationSetMember member : this.members)
            sourceParentPropertyMaps.add(member.getConfiguration());
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
            aggregateProperty(childPropertyDefinition, sourceParentPropertyMaps, this.aggregateConfiguration);
    }

    public ConfigurationDefinition getConfigurationDefinition()
    {
        return configurationDefinition;
    }

    public List<ConfigurationSetMember> getMembers()
    {
        return members;
    }

    public Configuration getAggregateConfiguration()
    {
        return aggregateConfiguration;
    }

    private static void aggregateProperty(PropertyDefinition propertyDefinition,
                                          List<AbstractPropertyMap> sourceParentPropertyMaps,
                                          AbstractPropertyMap targetParentPropertyMap)
    {
        if (propertyDefinition instanceof PropertyDefinitionSimple)
        {
            String sampleValue = getSimpleValue(sourceParentPropertyMaps.get(0), propertyDefinition.getName());
            boolean valuesHeterogenous = false;
            for (int i = 1; i < sourceParentPropertyMaps.size(); i++)
            {
                String value = getSimpleValue(sourceParentPropertyMaps.get(i), propertyDefinition.getName());
                if ((value == null && sampleValue != null) || (value != null && !value.equals(sampleValue))) {
                    valuesHeterogenous = true;
                    break;
                }
            }
            if (!valuesHeterogenous) {
                // Only add simples with homogenous values to the aggregate config.
                PropertySimple propertySimple = new PropertySimple(propertyDefinition.getName(), sampleValue);
                // Set override to true so the renderer will know the prop is homogenous.
                propertySimple.setOverride(true);
                targetParentPropertyMap.put(propertySimple);
            }
        }
        // If the property is a Map, recurse into it and aggregate its child properties.
        else if (propertyDefinition instanceof PropertyDefinitionMap)
        {
            List<AbstractPropertyMap> nestedSourceParentPropertyMaps = new ArrayList();
            for (AbstractPropertyMap sourceParentPropertyMap : sourceParentPropertyMaps)
                nestedSourceParentPropertyMaps.add(sourceParentPropertyMap.getMap(propertyDefinition.getName()));
            PropertyMap targetPropertyMap = new PropertyMap(propertyDefinition.getName());
            targetParentPropertyMap.put(targetPropertyMap);
            aggregatePropertyMap((PropertyDefinitionMap)propertyDefinition, nestedSourceParentPropertyMaps,
                    targetPropertyMap);
        }
        else if (propertyDefinition instanceof PropertyDefinitionList)
        {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList)propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            PropertyList propertyList = new PropertyList(propertyDefinition.getName());
            targetParentPropertyMap.put(propertyList);
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap)
            {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap)listMemberPropertyDefinition;
                // TODO: How do we aggregate Lists of Maps? Not trivial...
            }
        }
    }

    private static void aggregatePropertyMap(PropertyDefinitionMap propertyDefinitionMap,
                                             List<AbstractPropertyMap> sourceParentPropertyMaps,
                                             AbstractPropertyMap targetParentPropertyMap)
    {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        if (!childPropertyDefinitions.isEmpty()) {
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
                aggregateProperty(childPropertyDefinition, sourceParentPropertyMaps, targetParentPropertyMap);
        } else {
            aggregateOpenPropertyMap(sourceParentPropertyMaps, targetParentPropertyMap);
        }
    }

    private static void aggregateOpenPropertyMap(List<AbstractPropertyMap> sourceParentPropertyMaps,
                                                 AbstractPropertyMap targetParentPropertyMap)
    {
        Map<String, Map<String, Integer>> memberNameValueFrequenciesMap =
                createMemberNameValueFrequenciesMap(sourceParentPropertyMaps);
        for (String memberName : memberNameValueFrequenciesMap.keySet()) {
            // Add each unique member to the target map, so the renderer will be able to display it on the main
            // config page.
            PropertySimple member = new PropertySimple(memberName, null);
            targetParentPropertyMap.put(member);
            // Also add it to each of the source config maps that don't already contain it, so that they can be
            // rendered as unset on the propSet page.
            for (AbstractPropertyMap map : sourceParentPropertyMaps)
            {
                if (map.get(memberName) == null)
                    map.put(new PropertySimple(memberName, null));
            }
            Map<String, Integer> valueFrequencies = memberNameValueFrequenciesMap.get(memberName);
            if (valueFrequencies.size() == 1 &&
                    valueFrequencies.values().iterator().next() == sourceParentPropertyMaps.size()) {
                // Set override to true so the renderers will know the prop is homogenous.
                member.setOverride(true);
                // And set the value, so it can be displayed on the main config page.
                member.setStringValue(valueFrequencies.keySet().iterator().next());
            }
        }
    }

    private static Map<String, Map<String, Integer>> createMemberNameValueFrequenciesMap(
            List<AbstractPropertyMap> sourceParentPropertyMaps)
    {
        Map<String, Map<String, Integer>> nameValueFrequenciesMap = new HashMap();
        for (AbstractPropertyMap map : sourceParentPropertyMaps)
        {
            for (String propertyName : map.getMap().keySet())
            {
                PropertySimple propertySimple = map.getSimple(propertyName);
                String propertyValue = (propertySimple != null) ? propertySimple.getStringValue() : null;
                Map<String, Integer> valueFrequencies = nameValueFrequenciesMap.get(propertyName);
                if (valueFrequencies == null) {
                    valueFrequencies = new HashMap();
                    valueFrequencies.put(propertyValue, 0);
                    nameValueFrequenciesMap.put(propertyName, valueFrequencies);
                }
                valueFrequencies.put(propertyValue, valueFrequencies.get(propertyValue) + 1);
            }
        }
        return nameValueFrequenciesMap;
    }

    private static String getSimpleValue(AbstractPropertyMap parentPropertyMap, String propertyName)
    {
        PropertySimple samplePropertySimple = parentPropertyMap.getSimple(propertyName);
        return (samplePropertySimple != null) ? samplePropertySimple.getStringValue() : null;
    }
}
