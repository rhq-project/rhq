/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;

/**
 * @author Ian Springer
 */
public class ConfigurationSet {
    private ConfigurationDefinition configurationDefinition;
    private List<ConfigurationSetMember> members;
    private Configuration groupConfiguration;

    public ConfigurationSet(ConfigurationDefinition configurationDefinition, List<ConfigurationSetMember> members) {
        if (configurationDefinition == null)
            throw new IllegalArgumentException("configurationDefinition parameter is null.");
        this.configurationDefinition = configurationDefinition;
        if (members == null)
            throw new IllegalArgumentException("members parameter is null.");
        this.members = members;
        this.groupConfiguration = new Configuration();
        calculateGroupConfiguration();
    }

    public void calculateGroupConfiguration() {
        this.groupConfiguration.getMap().clear();
        if (this.members.isEmpty())
            return;
        Map<String, PropertyDefinition> childPropertyDefinitions = this.configurationDefinition
            .getPropertyDefinitions();
        List<AbstractPropertyMap> sourceParentPropertyMaps = new ArrayList();
        for (ConfigurationSetMember member : this.members)
            sourceParentPropertyMaps.add(member.getConfiguration());
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
            calculateGroupProperty(childPropertyDefinition, sourceParentPropertyMaps, this.groupConfiguration);
    }

    public void applyGroupConfigurationForView() {
        applyGroupConfiguration(true);
    }

    public void applyGroupConfigurationForUpdate() {
        applyGroupConfiguration(false);
    }
    
    private void applyGroupConfiguration(boolean forView) {
        if (this.members.isEmpty())
            return;
        Map<String, PropertyDefinition> childPropertyDefinitions = this.configurationDefinition
            .getPropertyDefinitions();
        List<AbstractPropertyMap> sourceParentPropertyMaps = new ArrayList<AbstractPropertyMap>();
        for (ConfigurationSetMember member : this.members)
            sourceParentPropertyMaps.add(member.getConfiguration());
        for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
            mergeProperty(childPropertyDefinition, sourceParentPropertyMaps, this.groupConfiguration, forView);
    }
    
    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public List<ConfigurationSetMember> getMembers() {
        return members;
    }

    public Configuration getGroupConfiguration() {
        return groupConfiguration;
    }

    public void mask() {
        for (ConfigurationSetMember member : this.members) {
            ConfigurationMaskingUtility.maskConfiguration(member.getConfiguration(), this.configurationDefinition);
        }
        ConfigurationMaskingUtility.maskConfiguration(this.groupConfiguration, this.configurationDefinition);
    }

    public void unmask() {
        for (ConfigurationSetMember member : this.members) {
            ConfigurationMaskingUtility.unmaskConfiguration(member.getConfiguration(), this.configurationDefinition);
        }
        ConfigurationMaskingUtility.unmaskConfiguration(this.groupConfiguration, this.configurationDefinition);
    }

    private static void calculateGroupProperty(PropertyDefinition propertyDefinition,
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
        // If the property is a Map, recurse into it and group together its child properties.
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
            calculateGroupPropertyMap((PropertyDefinitionMap) propertyDefinition, nestedSourceParentPropertyMaps,
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

    private static void calculateGroupPropertyMap(PropertyDefinitionMap propertyDefinitionMap,
        List<AbstractPropertyMap> sourceParentPropertyMaps, AbstractPropertyMap targetParentPropertyMap) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        if (!childPropertyDefinitions.isEmpty()) {
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
                calculateGroupProperty(childPropertyDefinition, sourceParentPropertyMaps, targetParentPropertyMap);
        } else {
            calculateGroupOpenPropertyMap(sourceParentPropertyMaps, targetParentPropertyMap);
        }
    }

    private static void calculateGroupOpenPropertyMap(List<AbstractPropertyMap> sourceParentPropertyMaps,
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
                if (map.get(memberName) == null) {
                    PropertySimple sourceProperty = new PropertySimple(memberName, null);
                    //ok, this is highly confusing but we have no other way of "marking"
                    //this kind of property.
                    //We need to mark the property as not present in the original source map.
                    //This is different from having an unset value of course.
                    
                    //There is no other property on the Property class that we could use for this
                    //than the override. The confusion here is that the override property is used
                    //for the exact opposite reason on the target (i.e. the calculated group configuration),
                    //where it marks the fact that all the properties in the different resource configuration
                    //are set and have the same value.
                    //This works, because a member property is never also a calculated one, it can just confuse
                    //the casual reader of this code ;)
                    sourceProperty.setOverride(true);
                    map.put(sourceProperty);
                }
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
        List<AbstractPropertyMap> memberParentPropertyMaps, AbstractPropertyMap groupParentPropertyMap, boolean forView) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            PropertySimple propertySimple = groupParentPropertyMap.getSimple(propertyDefinition.getName());
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
            PropertyMap groupPropertyMap = groupParentPropertyMap.getMap(propertyDefinition.getName());
            groupParentPropertyMap.put(groupPropertyMap);
            mergePropertyMap((PropertyDefinitionMap) propertyDefinition, nestedSourceParentPropertyMaps,
                groupPropertyMap, forView);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition listMemberPropertyDefinition = propertyDefinitionList.getMemberDefinition();
            PropertyList groupPropertyList = groupParentPropertyMap.getList(propertyDefinition.getName());
            if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) listMemberPropertyDefinition;
                // TODO: How do we merge Lists of Maps? Not trivial...
            }
        }
    }

    private static void mergePropertyMap(PropertyDefinitionMap propertyDefinitionMap,
        List<AbstractPropertyMap> memberParentPropertyMaps, AbstractPropertyMap groupParentPropertyMap, boolean forView) {
        Map<String, PropertyDefinition> childPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        if (!childPropertyDefinitions.isEmpty()) {
            for (PropertyDefinition childPropertyDefinition : childPropertyDefinitions.values())
                mergeProperty(childPropertyDefinition, memberParentPropertyMaps, groupParentPropertyMap, forView);
        } else {
            mergeOpenPropertyMap(memberParentPropertyMaps, groupParentPropertyMap, forView);
        }
    }

    private static void mergeOpenPropertyMap(List<AbstractPropertyMap> memberParentPropertyMaps,
        AbstractPropertyMap groupParentPropertyMap, boolean forView) {
        for (String groupMemberPropertyName : groupParentPropertyMap.getMap().keySet()) {
            PropertySimple groupMemberProperty = groupParentPropertyMap.getSimple(groupMemberPropertyName);
            if (groupMemberProperty != null && groupMemberProperty.getOverride() != null
                && groupMemberProperty.getOverride()) {
                for (AbstractPropertyMap sourceParentPropertyMap : memberParentPropertyMaps) {
                    PropertySimple sourcePropertySimple = sourceParentPropertyMap.getSimple(groupMemberPropertyName);
                    if (sourcePropertySimple == null) {
                        sourcePropertySimple = new PropertySimple(groupMemberPropertyName, groupMemberProperty
                            .getStringValue());
                        sourceParentPropertyMap.put(sourcePropertySimple);
                    } else {
                        sourcePropertySimple.setStringValue(groupMemberProperty.getStringValue());
                    }
                }
            } else if (!forView) {
                //we have a non-homogenic property here and want to update the underlying configurations
                //for update. This means that we want to erradicate all the open map properties from the 
                //underlying configs that are not assigned a value in the calculated group config.
                //this to basically undo the appearance of there being all open map properties defined
                //in all configurations in each of them. That is needed for the view purposes but leaving
                //the configs like that will cause new properties with "empty" values introduced in all the
                //resource configs in the group after update.
                for (AbstractPropertyMap sourceParentPropertyMap : memberParentPropertyMaps) {
                    PropertySimple sourcePropertySimple = sourceParentPropertyMap.getSimple(groupMemberPropertyName);
                    if (sourcePropertySimple == null) {
                        //there was no such property there - just add it
                        sourcePropertySimple = new PropertySimple(groupMemberPropertyName, groupMemberProperty
                            .getStringValue());
                        sourceParentPropertyMap.put(sourcePropertySimple);
                    } else {
                        //the members of open map are marked with override to signify that they have
                        //not been originally present in the member configuration.
                        //If this is true and the group property has null value, it means
                        //that the user has set no value for this property and thus it should
                        //be removed again from the member config.
                        if (groupMemberProperty.getStringValue() == null 
                            && sourcePropertySimple.getOverride() != null && sourcePropertySimple.getOverride()) {
                            
                            sourceParentPropertyMap.getMap().remove(sourcePropertySimple.getName());
                        }
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
