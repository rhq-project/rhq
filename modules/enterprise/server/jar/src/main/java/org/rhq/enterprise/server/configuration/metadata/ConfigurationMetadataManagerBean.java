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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
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
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public void updateConfigurationDefinition(ConfigurationDefinition newDefinition,
        ConfigurationDefinition existingDefinition) {
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
                    updatePropertyDefinition(existingProp, newProperty);
                } else {
                    existingDefinition.put(newProperty);
                }
            }

            // delete outdated properties
            removeNolongerUsedProperties(newDefinition, existingDefinition, existingPropertyDefinitions);
        } else {
            // TODO what if exisitingDefinitions is null?
            // we probably don't run in here, as the initial persisting is done
            // somewhere else.
        }

        entityManager.flush();

        /*
         * Now update / delete contained groups We need to be careful here, as groups are present in PropertyDefinition
         * as "backlink" from PropertyDefinition to group
         */
        List<PropertyGroupDefinition> existingGroups = existingDefinition.getGroupDefinitions();
        List<PropertyGroupDefinition> newGroups = newDefinition.getGroupDefinitions();

        List<PropertyGroupDefinition> toPersist = missingInFirstList(existingGroups, newGroups);
        List<PropertyGroupDefinition> toDelete = missingInFirstList(newGroups, existingGroups);
        List<PropertyGroupDefinition> toUpdate = intersection(newGroups, existingGroups);

        // delete groups no longer present
        for (PropertyGroupDefinition group : toDelete) {
            List<PropertyDefinition> groupedDefinitions = existingDefinition.getPropertiesInGroup(group.getName());

            // first look for contained stuff
            for (PropertyDefinition def : groupedDefinitions) {
                existingPropertyDefinitions.remove(def);
                existingDefinition.getPropertyDefinitions().remove(def.getName());
                def.setPropertyGroupDefinition(null);
                entityManager.remove(def);
            }

            // then remove the definition itself
            existingGroups.remove(group);
            entityManager.remove(group);
        }

        entityManager.flush();

        // update existing groups that stay
        for (PropertyGroupDefinition group : toUpdate) {
            String groupName = group.getName();

            //         System.out.println("Group to update: " + groupName + ", id=" + group.getId());
            List<PropertyDefinition> newGroupedDefinitions = newDefinition.getPropertiesInGroup(groupName);
            for (PropertyDefinition nDef : newGroupedDefinitions) {
                PropertyDefinition existingProperty = existingDefinition.getPropertyDefinitions().get(nDef.getName());
                if (existingProperty != null) {
                    updatePropertyDefinition(existingProperty, nDef);
                } else {
                    existingDefinition.put(nDef);
                }
            }

            // delete outdated properties of this group
            removeNolongerUsedProperties(newDefinition, existingDefinition, existingDefinition
                .getPropertiesInGroup(groupName));
        }

        entityManager.flush();

        // persist new groups
        for (PropertyGroupDefinition group : toPersist) {
            /*
             * First persist a new group definition and then link the properties to it
             */
            entityManager.persist(group);
            existingGroups.add(group); // iterating over this does not update the underlying crap

            List<PropertyDefinition> defs = newDefinition.getPropertiesInGroup(group.getName());
            Map<String, PropertyDefinition> exPDefs = existingDefinition.getPropertyDefinitions();
            for (PropertyDefinition def : defs) {
                entityManager.persist(def);
                def.setPropertyGroupDefinition(group);
                def.setConfigurationDefinition(existingDefinition);
                exPDefs.put(def.getName(), def);
            }
        }

        entityManager.flush();
    }

    /**
     * Removes PropertyDefinition items from the configuration
     *
     * @param newDefinition      new configuration to persist
     * @param existingDefinition existing persisted configuration
     * @param existingProperties list of existing properties
     */
    private void removeNolongerUsedProperties(ConfigurationDefinition newDefinition,
        ConfigurationDefinition existingDefinition, List<PropertyDefinition> existingProperties) {

        List<PropertyDefinition> definitionsToDelete = new ArrayList<PropertyDefinition>();
        for (PropertyDefinition exDef : existingProperties) {
            PropertyDefinition nDef = newDefinition.get(exDef.getName());
            if (nDef == null) {
                // not in new configuration
                definitionsToDelete.add(exDef);
            }
        }

        for (PropertyDefinition def : definitionsToDelete) {
            existingDefinition.getPropertyDefinitions().remove(def.getName());
            existingProperties.remove(def); // does not operate on original list!!
            entityManager.remove(def);
        }
        entityManager.flush();
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

        /*
         * After the general things have been set, go through the subtypes of PropertyDefinition. If the new type is the
         * same as the old, we update. Else we simply replace
         */

        if (existingProperty instanceof PropertyDefinitionMap) {
            if (newProperty instanceof PropertyDefinitionMap) {
                // remove outdated maps
                Map<String, PropertyDefinition> existingPropDefs = ((PropertyDefinitionMap) existingProperty)
                    .getPropertyDefinitions();
                Set<String> existingKeys = existingPropDefs.keySet();
                Set<String> newKeys = ((PropertyDefinitionMap) newProperty).getPropertyDefinitions().keySet();
                for (String key : existingKeys) {
                    if (!newKeys.contains(key)) {
                        entityManager.remove(existingPropDefs.get(key));
                        existingPropDefs.remove(key);
                    }
                }

                // store update new ones
                for (PropertyDefinition newChild : ((PropertyDefinitionMap) newProperty).getPropertyDefinitions()
                    .values()) {
                    PropertyDefinition existingChild = ((PropertyDefinitionMap) existingProperty).get(newChild
                        .getName());
                    if (existingChild != null) {
                        updatePropertyDefinition(existingChild, newChild);
                    } else {
                        ((PropertyDefinitionMap) existingProperty).put(newChild);
                        entityManager.persist(newChild);
                    }
                }
            } else // different type
            {
                replaceProperty(existingProperty, newProperty);
            }
        } else if (existingProperty instanceof PropertyDefinitionList) {
            PropertyDefinitionList exList = (PropertyDefinitionList) existingProperty;
            if (newProperty instanceof PropertyDefinitionList) {
                PropertyDefinitionList newList = (PropertyDefinitionList) newProperty;
                exList.setMemberDefinition(newList.getMemberDefinition());
                exList.setMax(newList.getMax());
                exList.setMin(newList.getMax());
                // what about parentPropertyListDefinition ?

                // TODO recursively update the member ?
            } else // simple property or map-property
            {
                replaceProperty(existingProperty, newProperty);
            }
        } else if (existingProperty instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple existingPDS = (PropertyDefinitionSimple) existingProperty;

            if (newProperty instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple newPDS = (PropertyDefinitionSimple) newProperty;

                // handle <property-options>?
                List<PropertyDefinitionEnumeration> existingOptions = existingPDS.getEnumeratedValues();
                List<PropertyDefinitionEnumeration> newOptions = newPDS.getEnumeratedValues();

                List<PropertyDefinitionEnumeration> toPersist = missingInFirstList(existingOptions, newOptions);
                List<PropertyDefinitionEnumeration> toDelete = missingInFirstList(newOptions, existingOptions);
                List<PropertyDefinitionEnumeration> changed = intersection(existingOptions, newOptions);

                // save new ones
                for (PropertyDefinitionEnumeration pde : toPersist) {
                    existingPDS.addEnumeratedValues(pde);
                    entityManager.persist(pde);
                }

                // delete old ones
                for (PropertyDefinitionEnumeration pde : toDelete) {
                    existingOptions.remove(pde);
                    entityManager.remove(pde);
                }

                for (PropertyDefinitionEnumeration pde : changed) {
                    for (PropertyDefinitionEnumeration nPde : newOptions) {
                        if (nPde.equals(pde)) {
                            pde.setDefault(nPde.isDefault());
                            pde.setOrderIndex(nPde.getOrderIndex());
                            pde.setValue(nPde.getValue());
                        }
                    }
                }

                // handle <constraint> [0..*]

                entityManager.flush();
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

                // handle <defaultValueDescription> [0..1]
                existingPDS.setDefaultValue(newPDS.getDefaultValue());
            } else {
                // other type
                replaceProperty(existingProperty, newProperty);
            }
        }
    }

    /**
     * Replace the existing property of a given type with a new property of a (possibly) different type
     *
     * @param existingProperty
     * @param newProperty
     */
    private void replaceProperty(PropertyDefinition existingProperty, PropertyDefinition newProperty) {
        // take id and definition from the existing one
        newProperty.setId(existingProperty.getId());
        newProperty.setConfigurationDefinition(existingProperty.getConfigurationDefinition());

        //  need to remove the old crap
        existingProperty.getConfigurationDefinition().getPropertyDefinitions().remove(existingProperty.getName());
        existingProperty.setConfigurationDefinition(null);
        entityManager.remove(existingProperty);

        // persist the new one
        entityManager.merge(newProperty);
        entityManager.flush();
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