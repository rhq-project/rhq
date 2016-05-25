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
package org.rhq.enterprise.server.configuration.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.enterprise.server.RHQConstants;

/**
 * Used to work with metadata defining generic configurations.
 */
@Stateless
public class ConfigurationMetadataManagerBean implements ConfigurationMetadataManagerLocal {
    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public ConfigurationDefinitionUpdateReport updateConfigurationDefinition(ConfigurationDefinition newDefinition,
        ConfigurationDefinition existingDefinition) {

        ConfigurationDefinitionUpdateReport updateReport = new ConfigurationDefinitionUpdateReport(existingDefinition);

        /*
         * handle grouped and ungrouped properties separately. for ungrouped, we don't need to care about the group, but
         * for the grouped ones we need to start at group level and then look at the properties. This is done below.
         *
         * First look at the ungrouped ones.
         */

        List<PropertyDefinition> existingPropertyDefinitions = existingDefinition.getNonGroupedProperties();
        List<PropertyDefinition> newPropertyDefinitions = newDefinition.getNonGroupedProperties();
        if (existingPropertyDefinitions != null) {
            for (PropertyDefinition newProperty : newPropertyDefinitions) {
                PropertyDefinition existingProp = existingDefinition.get(newProperty.getName());
                if (existingProp != null) {
                    log.debug("Updating nonGrouped property [" + existingProp + "]");

                    updatePropertyDefinition(existingProp, newProperty);
                    updateReport.addUpdatedPropertyDefinition(newProperty);
                } else {
                    log.debug("Adding nonGrouped property [" + newProperty + "]");

                    existingDefinition.put(newProperty);
                    updateReport.addNewPropertyDefinition(newProperty);
                }
            }

            existingDefinition = removeNoLongerUsedProperties(newDefinition, existingDefinition,
                existingPropertyDefinitions);

        } else {
            // TODO what if existingDefinitions is null?
            // we probably don't run in here, as the initial persisting is done
            // somewhere else.
        }

        /*
         * Now update / delete contained groups We need to be careful here, as groups are present in PropertyDefinition
         * as "backlink" from PropertyDefinition to group
         */
        List<PropertyGroupDefinition> existingGroups = existingDefinition.getGroupDefinitions();
        List<PropertyGroupDefinition> newGroups = newDefinition.getGroupDefinitions();

        List<PropertyGroupDefinition> toPersist = missingInFirstList(existingGroups, newGroups);
        List<PropertyGroupDefinition> toDelete = missingInFirstList(newGroups, existingGroups);
        List<PropertyGroupDefinition> toUpdate = intersection(existingGroups, newGroups);

        // delete groups no longer present
        for (PropertyGroupDefinition group : toDelete) {
            List<PropertyDefinition> groupedDefinitions = existingDefinition.getPropertiesInGroup(group.getName());

            // first look for contained stuff
            for (PropertyDefinition def : groupedDefinitions) {
                log.debug("Removing property [" + def + "] from group [" + group + "]");

                existingPropertyDefinitions.remove(def);
                existingDefinition.getPropertyDefinitions().remove(def.getName());
                def.setPropertyGroupDefinition(null);
                entityManager.remove(def);
            }

            // then remove the definition itself
            log.debug("Removing group [" + group + "]");

            existingGroups.remove(group);
            entityManager.remove(group);
        }

        // update existing groups that stay
        for (PropertyGroupDefinition group : toUpdate) {
            String groupName = group.getName();

            List<PropertyDefinition> newGroupedDefinitions = newDefinition.getPropertiesInGroup(groupName);
            for (PropertyDefinition nDef : newGroupedDefinitions) {
                PropertyDefinition existingProperty = existingDefinition.getPropertyDefinitions().get(nDef.getName());
                if (existingProperty != null) {
                    log.debug("Updating property [" + nDef + "] in group [" + group + "]");

                    updatePropertyDefinition(existingProperty, nDef);
                    updateReport.addUpdatedPropertyDefinition(nDef);

                } else {
                    log.debug("Adding property [" + nDef + "] to group [" + group + "]");

                    existingDefinition.put(nDef);
                    updateReport.addNewPropertyDefinition(nDef);
                }
            }

            // delete outdated properties of this group
            existingDefinition = removeNoLongerUsedProperties(newDefinition, existingDefinition,
                existingDefinition.getPropertiesInGroup(groupName));
        }

        // persist new groups
        for (PropertyGroupDefinition group : toPersist) {

            // First persist a new group definition and then link the properties to it
            log.debug("Persisting new group [" + group + "]");

            entityManager.persist(group);
            existingGroups.add(group); // iterating over this does not update the underlying crap

            List<PropertyDefinition> defs = newDefinition.getPropertiesInGroup(group.getName());
            Map<String, PropertyDefinition> exPDefs = existingDefinition.getPropertyDefinitions();
            for (PropertyDefinition def : defs) {
                entityManager.persist(def);
                def.setPropertyGroupDefinition(group);
                def.setConfigurationDefinition(existingDefinition);

                if (!exPDefs.containsKey(def.getName())) {
                    updateReport.addNewPropertyDefinition(def);
                }

                exPDefs.put(def.getName(), def);
            }
        }

        /*
         * Now work on the templates.
         */
        Map<String, ConfigurationTemplate> existingTemplates = existingDefinition.getTemplates();
        Map<String, ConfigurationTemplate> newTemplates = newDefinition.getTemplates();
        List<String> toRemove = new ArrayList<String>();
        List<String> templatesToUpdate = new ArrayList<String>();

        for (String name : existingTemplates.keySet()) {
            if (newTemplates.containsKey(name)) {
                templatesToUpdate.add(name);
            } else {
                toRemove.add(name);
            }
        }

        for (String name : toRemove) {
            log.debug("Removing template [" + name + "]");

            ConfigurationTemplate template = existingTemplates.remove(name);
            entityManager.remove(template);
        }

        for (String name : templatesToUpdate) {
            log.debug("Updating template [" + name + "]");

            updateTemplate(existingDefinition.getTemplate(name), newTemplates.get(name));
        }

        for (String name : newTemplates.keySet()) {
            // add completely new templates
            if (!existingTemplates.containsKey(name)) {
                log.debug("Adding template [" + name + "]");

                ConfigurationTemplate newTemplate = newTemplates.get(name);

                // we need to set a valid configurationDefinition, where we will live on.
                newTemplate.setConfigurationDefinition(existingDefinition);

                entityManager.persist(newTemplate);
                existingTemplates.put(name, newTemplate);
            }
        }

        return updateReport;
    }

    private void updateTemplate(ConfigurationTemplate existingDT, ConfigurationTemplate newDT) {

        try {
            Configuration existConf = existingDT.getConfiguration();
            Configuration newConf = newDT.getConfiguration();
            Collection<String> exNames = existConf.getNames();
            Collection<String> newNames = newConf.getNames();
            List<String> toRemove = new ArrayList<String>();
            for (String name : exNames) {
                Property prop = newConf.get(name);
                if (prop instanceof PropertySimple) {
                    PropertySimple ps = newConf.getSimple(name);
                    if (ps != null) {
                        Property eprop = existConf.get(name);
                        if (eprop instanceof PropertySimple) {
                            PropertySimple exps = existConf.getSimple(name);
                            if (ps.getStringValue() != null) {
                                exps.setStringValue(ps.getStringValue());
                                //                                System.out.println("  updated " + name + " to value " + ps.getStringValue());
                            }
                        } else {
                            if (eprop != null) {
                                //                                System.out.println("Can't yet handle target prop: " + eprop);
                            }
                        }
                    } else { // property not in new template -> deleted
                        toRemove.add(name);
                    }
                } else {
                    if (prop != null) {
                        //                        System.out.println("Can't yet handle source prop: " + prop);
                    }
                }
            }
            for (String name : toRemove)
                existConf.remove(name);

            // now check for new names and add them
            for (String name : newNames) {
                if (!exNames.contains(name)) {
                    Property prop = newConf.get(name);
                    if (prop instanceof PropertySimple) {
                        PropertySimple ps = newConf.getSimple(name);
                        if (ps.getStringValue() != null) {
                            // TODO add a new property
                            //                            Collection<Property> properties = existConf.getProperties();
                            //                            properties = new ArrayList<Property>(properties);
                            //                            properties.add(ps);
                            //                            existConf.setProperties(properties);
                            Property property = ps.deepCopy(false);
                            existConf.put(property);
                        }
                    }
                }
            }

            entityManager.flush();
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /**
     * Removes PropertyDefinition items from the configuration
     *
     * @param newConfigDef       new configuration being merged into the existing one
     * @param existingConfigDef  existing persisted configuration
     * @param existingProperties list of existing properties to inspect for potential removal
     */
    private ConfigurationDefinition removeNoLongerUsedProperties(ConfigurationDefinition newConfigDef,
        ConfigurationDefinition existingConfigDef, List<PropertyDefinition> existingProperties) {

        List<PropertyDefinition> propDefsToDelete = new ArrayList<PropertyDefinition>();
        for (PropertyDefinition existingPropDef : existingProperties) {
            PropertyDefinition newPropDef = newConfigDef.get(existingPropDef.getName());
            if (newPropDef == null) {
                // not in new configuration
                propDefsToDelete.add(existingPropDef);
            }
        }

        if (!propDefsToDelete.isEmpty()) {
            log.debug("Deleting obsolete props from configDef [" + existingConfigDef + "]: " + propDefsToDelete);
            for (PropertyDefinition propDef : propDefsToDelete) {
                existingConfigDef.getPropertyDefinitions().remove(propDef.getName());
                existingProperties.remove(propDef); // does not operate on original list!!
            }
            existingConfigDef = entityManager.merge(existingConfigDef);
        }

        return existingConfigDef;
    }

    /**
     * Update objects of type on:property (simple-property, map-property, list-property
     *
     * @param existingProperty
     * @param newProperty
     */
    private void updatePropertyDefinition(PropertyDefinition existingProperty, PropertyDefinition newProperty) {
        existingProperty.setDescription(newProperty.getDescription());
        existingProperty.setDisplayName(newProperty.getDisplayName());
        existingProperty.setActivationPolicy(newProperty.getActivationPolicy());
        existingProperty.setVersion(newProperty.getVersion());
        existingProperty.setRequired(newProperty.isRequired());
        existingProperty.setReadOnly(newProperty.isReadOnly());
        existingProperty.setSummary(newProperty.isSummary());
        existingProperty.setPropertyGroupDefinition(newProperty.getPropertyGroupDefinition());

        /*
         * After the general things have been set, go through the subtypes of PropertyDefinition. If the new type is the
         * same as the old, we update. Else we simply replace
         */

        if (existingProperty instanceof PropertyDefinitionMap) {
            if (newProperty instanceof PropertyDefinitionMap) {

                // alter existingPropDefs to reflect newPropDefs
                Map<String, PropertyDefinition> existingPropDefs = ((PropertyDefinitionMap) existingProperty)
                    .getMap();

                Map<String, PropertyDefinition> newPropDefs = ((PropertyDefinitionMap) newProperty)
                    .getMap();
                Set<String> newKeys = newPropDefs.keySet();

                // remove obsolete propDefs
                List<String> doomedKeys = new ArrayList<String>();
                for (String existingKey : existingPropDefs.keySet()) {
                    if (!newKeys.contains(existingKey)) {
                        doomedKeys.add(existingKey);
                    }
                }
                for (String doomedKey : doomedKeys) {
                    PropertyDefinition doomed = existingPropDefs.get(doomedKey);
                    existingPropDefs.remove(doomedKey);
                    entityManager.remove(entityManager.find(PropertyDefinition.class, doomed.getId()));
                }

                int order = 0;
                for (String key : newKeys) {
                    PropertyDefinition existingPropDef = existingPropDefs.get(key);
                    PropertyDefinition newPropDef = newPropDefs.get(key);
                    if (null == existingPropDef) {
                        newPropDef.setOrder(order++);
                        newPropDef.setParentPropertyMapDefinition((PropertyDefinitionMap) existingProperty);
                        entityManager.persist(newPropDef);
                        existingPropDefs.put(key, newPropDef);
                    } else {
                        existingPropDef.setOrder(order++);
                        updatePropertyDefinition(existingPropDef, newPropDef);
                    }
                }

                existingProperty = entityManager.merge(existingProperty);

            } else { // different type

                replaceProperty(existingProperty, newProperty);
            }
        } else if (existingProperty instanceof PropertyDefinitionList) {
            PropertyDefinitionList exList = (PropertyDefinitionList) existingProperty;
            if (newProperty instanceof PropertyDefinitionList) {
                PropertyDefinitionList newList = (PropertyDefinitionList) newProperty;
                replaceListProperty(exList, newList);
            } else { // simple property or map-property
                replaceProperty(existingProperty, newProperty);
            }
        } else if (existingProperty instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple existingPDS = (PropertyDefinitionSimple) existingProperty;

            if (newProperty instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple newPDS = (PropertyDefinitionSimple) newProperty;

                existingPDS.setType(newPDS.getType());

                // handle <property-options>?
                List<PropertyDefinitionEnumeration> existingOptions = existingPDS.getEnumeratedValues();
                List<PropertyDefinitionEnumeration> newOptions = newPDS.getEnumeratedValues();

                List<PropertyDefinitionEnumeration> toPersist = missingInFirstList(existingOptions, newOptions);
                List<PropertyDefinitionEnumeration> toDelete = missingInFirstList(newOptions, existingOptions);
                List<PropertyDefinitionEnumeration> changed = intersection(existingOptions, newOptions);

                // sync the enumerated values and then merge the changes into the PDS, I think this
                // solves previous issues with orderIndex values.
                // First remove obsolete values
                for (PropertyDefinitionEnumeration pde : toDelete) {
                    existingPDS.removeEnumeratedValues(pde);
                }

                // save new ones
                for (PropertyDefinitionEnumeration pde : toPersist) {
                    existingPDS.addEnumeratedValues(pde);
                    entityManager.persist(pde);
                }

                // update others
                for (PropertyDefinitionEnumeration pde : changed) {
                    for (PropertyDefinitionEnumeration nPde : newOptions) {
                        if (nPde.equals(pde)) {
                            pde.setValue(nPde.getValue());
                            pde.setName(nPde.getName());
                        }
                    }
                }
                existingPDS = entityManager.merge(existingPDS);

                // handle <constraint> [0..*]

                Set<Constraint> exCon = existingPDS.getConstraints();
                if (exCon.size() > 0) {
                    for (Constraint con : exCon) {
                        con.setPropertyDefinitionSimple(null);
                        entityManager.remove(con);
                    }

                    existingPDS.getConstraints().clear(); // clear out existing
                }

                for (Constraint con : newPDS.getConstraints()) {
                    existingPDS.addConstraints(con);
                }

                // handle <defaultValue> [0..1]
                existingPDS.setDefaultValue(newPDS.getDefaultValue());

                // handle <c:source>
                existingPDS.setOptionsSource(newPDS.getOptionsSource());
            } else {
                // other type
                replaceProperty(existingProperty, newProperty);
            }
        }
    }

    /**
     * Replace the existing property of a given type with a new property of a (possibly) different type
     *
     * @param existingProperty the existing prop
     * @param newProperty the new prop that should replace the existing prop
     */
    private void replaceProperty(PropertyDefinition existingProperty, PropertyDefinition newProperty) {
        ConfigurationDefinition configDef = existingProperty.getConfigurationDefinition();

        // First take id from existing prop, and replace existing prop in the config def.
        newProperty.setId(existingProperty.getId());
        configDef.put(newProperty);

        entityManager.remove(existingProperty);
        entityManager.merge(configDef);
    }

    /**
     * This replaces an existing list property def with a new list property definition. Primarily it replaces
     * the member prop def for the list.  If the member prop def is a nested structure the whole thing
     * is replaced from the top.
     *
     * @param exList the existing prop def list
     * @param newList the new prop def list
     */
    private void replaceListProperty(PropertyDefinitionList exList, PropertyDefinitionList newList) {
        PropertyDefinition doomedMemberDef = null;

        if (newList.getMemberDefinition() == null) {
            log.error("\n\n!! Member definition for new list property [" + newList.getName()
                + "] is null - check and fix the plugin descriptor\n");
            return;
        }

        // We did not have a member definition before (which is wrong )
        // we need to add it now
        // only remove the existing member if it is a different entity
        PropertyDefinition exListMemberDefinition = exList.getMemberDefinition();
        if (exListMemberDefinition != null && exListMemberDefinition.getId() != newList.getMemberDefinition().getId()) {
            doomedMemberDef = exListMemberDefinition;
        }

        exList.setMemberDefinition(newList.getMemberDefinition());
        exList.setMax(newList.getMax());
        exList.setMin(newList.getMin());

        // BZ 594706
        // Don't clean this up here because it's causing deadlocks in Oracle.  Instead we'll just leave
        // garbage in the db.  Although annoying, and confusing for db queries, it's not a lot of data,
        // just some extra prop defs and prop_def_enums.
        if (null != doomedMemberDef) {
            // entityManager.remove(doomedMemberDef);
            if (log.isDebugEnabled()) {
                log.debug("Ignoring cleanup of [" + doomedMemberDef + "] due to BZ 594706");
            }
        }

        entityManager.merge(exList);
    }

    /**
     * Return a list containing those element that are in reference, but not in first. Both input lists are not modified
     *
     * @param  first
     * @param  reference
     *
     * @return list containing those element that are in reference, but not in first
     */
    private <T> List<T> missingInFirstList(List<T> first, List<T> reference) {
        List<T> result = new ArrayList<T>();

        if (reference != null) {
            // First collection is null -> everything is missing
            if (first == null) {
                result.addAll(reference);
                return result;
            }

            // else loop over the set and sort out the right items.
            for (T item : reference) {
                if (!first.contains(item)) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    /**
     * Return a new List with elements both in first and second passed collection.
     *
     * @param  first  First list
     * @param  second Second list
     *
     * @return a new set (depending on input type) with elements in first and second
     */
    private <T> List<T> intersection(List<T> first, List<T> second) {
        List<T> result = new ArrayList<T>();

        if ((first != null) && (second != null)) {
            result.addAll(first);
            result.retainAll(second);
        }

        return result;
    }
}