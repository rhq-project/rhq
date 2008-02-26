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
package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;

/**
 * This class manages the metadata for resources. Plugins are registered against this bean so that their metadata can be
 * pulled out and stored as necessary.
 *
 * <p/>// TODO GH: Should this be named PluginManager or something like that
 *
 * @author Greg Hinkle
 * @author Heiko W. Rupp
 */
@Stateless
public class ResourceMetadataManagerBean implements ResourceMetadataManagerLocal {
    private final Log log = LogFactory.getLog(ResourceMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private static PluginMetadataManager pluginMetadataManager = new PluginMetadataManager();

    @EJB
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataManager;

    /**
     * @param  name the name of a plugin
     *
     * @return the plugin with the specified name
     *
     * @throws NoResultException when no plugin with that name exists
     */
    public Plugin getPlugin(String name) {
        return (Plugin) entityManager.createNamedQuery("Plugin.findByName").setParameter("name", name)
            .getSingleResult();
    }

    public void registerPlugin(Plugin plugin, PluginDescriptor metadata) {
        // TODO GH: Consider how to remove features from plugins in updates without breaking everything
        Plugin existingPlugin = null;
        try {
            existingPlugin = (Plugin) entityManager.createNamedQuery("Plugin.findByName").setParameter("name",
                plugin.getName()).getSingleResult();
        } catch (NoResultException nre) {
            /* Expected for new plugins, so no problem */
        }

        //      if (existingPlugin == null || !existingPlugin.getMD5().equals(plugin.getMD5()))
        //      {

        // Plugin is new or has changed
        if (existingPlugin != null) {
            plugin.setId(existingPlugin.getId());
        }

        if (plugin.getDisplayName() == null) {
            plugin.setDisplayName(plugin.getName());
        }

        plugin = entityManager.merge(plugin);

        // Remove stale metadata
        updateTypes(metadata);
        // TODO GH: JBNADM-1310 - Push updated plugins to running agents and have them reboot their PCs
        // See also JBNADM-1630
        //      }
    }

    public List<Plugin> getPlugins() {
        Query q = entityManager.createNamedQuery(Plugin.QUERY_FIND_ALL);

        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    private void updateTypes(PluginDescriptor pluginDescriptor) {
        Set<ResourceType> updateTypes = pluginMetadataManager.loadPlugin(pluginDescriptor);
        if (updateTypes != null) {
            for (ResourceType resourceType : updateTypes) {
                updateType(resourceType);
            }
        }

        List<ResourceType> existingTypes = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_PLUGIN)
            .setParameter("plugin", pluginDescriptor.getName()).getResultList();

        if (existingTypes != null) {
            for (Iterator<ResourceType> iter = existingTypes.iterator(); iter.hasNext();) {
                ResourceType existingType = iter.next();
                if (pluginMetadataManager.getType(existingType.getName(), existingType.getPlugin()) == null) {
                    // This type no longer exists
                    removeFromParents(existingType);
                    entityManager.remove(existingType);
                    entityManager.flush();
                    iter.remove();
                }
            }

            // finally, its safe to remove any existing subcategories
            for (ResourceType remainingType : existingTypes) {
                ResourceType updateType = pluginMetadataManager.getType(remainingType.getName(), remainingType
                    .getPlugin());

                // if we've got a type from the descriptor which matches an existing one
                // then lets see if we need to remove any subcategories from the existing one
                if (updateType != null) {
                    removeSubCategories(updateType, remainingType);
                    entityManager.flush();
                }
            }
        }
    }

    private void removeFromParents(ResourceType typeToBeRemoved) {
        Set<ResourceType> parents = typeToBeRemoved.getParentResourceTypes();
        for (ResourceType parent : parents) {
            parent.removeChildResourceType(typeToBeRemoved);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateType(ResourceType resourceType) {
        try {
            entityManager.flush();

            // Connect the parent types if they exist which they should
            Set<ResourceType> types = new HashSet<ResourceType>(resourceType.getParentResourceTypes());
            resourceType.setParentResourceTypes(new HashSet<ResourceType>());
            for (ResourceType resourceTypeParent : types) {
                try {
                    ResourceType realParentType = (ResourceType) entityManager.createNamedQuery(
                        ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN).setParameter("name", resourceTypeParent.getName())
                        .setParameter("plugin", resourceTypeParent.getPlugin()).getSingleResult();
                    realParentType.addChildResourceType(resourceType);
                } catch (NoResultException nre) {
                    throw new RuntimeException("Couldn't persist type [" + resourceType
                        + "] because parent wasn't already persisted [" + resourceTypeParent + "]");
                }
            }

            // now see if there is already an existing type that we need to update

            if (log.isDebugEnabled()) {
                String out = "Searching existing type for name " + resourceType.getName() + " and plugin "
                    + resourceType.getPlugin();
                log.debug(out);
            }

            Query q = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN).setParameter("name",
                resourceType.getName()).setParameter("plugin", resourceType.getPlugin());
            List<ResourceType> results = q.getResultList();

            // see what we have. Default is 0 or 1. When hot deploying and moving a RT 2 can happen
            ResourceType existingType = null;
            if (results.size() == 1) {
                existingType = results.get(0);
            } else if (results.size() == 0) {
                throw new NoResultException(); // continue below TODO refactor the blocks
            } else {
                // No Unique result. List what we have and bail out if more than 2 results
                if (log.isDebugEnabled()) {
                    for (ResourceType rType : results) {
                        log.debug("updateType: found: " + rType.toString());
                    }
                }

                if (results.size() != 2) {
                    throw new IllegalArgumentException("We only expected two results here, but got " + results.size());
                }

                // two results - see if one is us, then the other must be the existing type that we are looking for
                Iterator<ResourceType> iter = results.iterator();
                ResourceType rt1 = iter.next();
                ResourceType rt2 = iter.next();
                if (rt1.equals(resourceType)) {
                    existingType = rt2;
                } else if (rt2.equals(resourceType)) {
                    existingType = rt1;
                } else {
                    throw new IllegalArgumentException("Houston we have a problem: our type is not there");
                }
            }

            // Type exists now, (if it didn't exist we'd fall through to the NSRE catch block and cascade persist everything

            // first add/update any subcategories on the parent before trying to update children
            // if we didn't do this the children may try to save themselves with subcategories which
            // wouldn't exist yet
            addAndUpdateSubCategories(resourceType, existingType);

            // update children next
            for (ResourceType childType : resourceType.getChildResourceTypes()) {
                updateType(childType);
            }

            // even though we've updated our child types to use new subcategory references, its still
            // not safe to delete the old sub categories yet, because we haven't yet deleted all of the old
            // child types which may still be referencing these sub categories

            // TODO GH: Update the rest of these related resources
            updatePluginConfiguration(resourceType, existingType);
            entityManager.flush();

            updateResourceConfiguration(resourceType, existingType);

            updateMeasurementDefinitions(resourceType, existingType);

            updateContentDefinitions(resourceType, existingType);

            updateOperationDefinitions(resourceType, existingType);

            updateProcessScans(resourceType, existingType);

            // TODO Update the type itself
            existingType.setDescription(resourceType.getDescription());
            existingType.setCreateDeletePolicy(resourceType.getCreateDeletePolicy());
            existingType.setCreationDataType(resourceType.getCreationDataType());
            existingType.setSingleton(resourceType.isSingleton());
            existingType.setSupportsManualAdd(resourceType.isSupportsManualAdd());

            existingType = entityManager.merge(existingType);
            entityManager.flush();
        } catch (NoResultException nre) {
            // If the type didn't exist then we'll persist here which will cascade through
            // all child types as well as plugin and resource configs and their delegate types and
            // metric and operation definitions and their dependent types
            log.debug("Persisting new ResourceType: " + resourceType.toString());
            entityManager.persist(resourceType);
        } catch (NonUniqueResultException nure) {
            log.debug("Found more than one existing type for " + resourceType.toString());
            throw new RuntimeException(nure);
        }
    }

    /**
     * Update the stuff below a <plugin-configuration>
     *
     * @param resourceType
     * @param existingType
     */
    private void updatePluginConfiguration(ResourceType resourceType, ResourceType existingType) {
        ConfigurationDefinition existingConfigurationDefinition = existingType.getPluginConfigurationDefinition();
        if (resourceType.getPluginConfigurationDefinition() != null) {
            // all new
            if (existingConfigurationDefinition == null) {
                entityManager.persist(resourceType.getPluginConfigurationDefinition());
                existingType.setPluginConfigurationDefinition(resourceType.getPluginConfigurationDefinition());
            } else // update the configuration
            {
                configurationMetadataManager.updateConfigurationDefinition(resourceType
                    .getPluginConfigurationDefinition(), existingConfigurationDefinition);
            }
        } else {
            // resourceType.getPlu... is null -> remove the existing config
            if (existingConfigurationDefinition != null) {
                existingType.setPluginConfigurationDefinition(null);
                entityManager.remove(existingConfigurationDefinition);
            }
        }
    }

    /**
     * Update the set of process scans for a given resource type
     *
     * @param resourceType
     * @param existingType
     */
    private void updateProcessScans(ResourceType resourceType, ResourceType existingType) {
        Set<ProcessScan> existingScans = existingType.getProcessScans();
        Set<ProcessScan> newScans = resourceType.getProcessScans();

        Set<ProcessScan> scansToPersist = missingInFirstSet(existingScans, newScans);
        Set<ProcessScan> scansToDelete = missingInFirstSet(newScans, existingScans);

        Set<ProcessScan> scansToUpdate = intersection(existingScans, newScans);

        // update scans that may have changed
        for (ProcessScan scan : scansToUpdate) {
            for (ProcessScan nScan : newScans) {
                if (scan.equals(nScan)) {
                    scan.setName(nScan.getName());
                }
            }
        }

        // persist new scans
        for (ProcessScan scan : scansToPersist) {
            existingType.addProcessScan(scan);
        }

        // remove deleted ones
        for (ProcessScan scan : scansToDelete) {
            existingScans.remove(scan);
            entityManager.remove(scan);
        }
    }

    /**
     * Update the operation definitions of existingType with the ones from resource type.
     *
     * @param resourceType New resourceType definition with operationDefinitions
     * @param existingType The existing resource type with operation Definitions
     */
    private void updateOperationDefinitions(ResourceType resourceType, ResourceType existingType) {
        Set<OperationDefinition> existingDefinitions = existingType.getOperationDefinitions();
        Set<OperationDefinition> newDefinitions = resourceType.getOperationDefinitions();

        Set<OperationDefinition> newOps = missingInFirstSet(existingDefinitions, newDefinitions);
        Set<OperationDefinition> opsToRemove = missingInFirstSet(newDefinitions, existingDefinitions);

        existingDefinitions.retainAll(newDefinitions);

        // loop over the OperationDefinitions that are neither new nor deleted
        // and update them from the resourceType
        for (OperationDefinition def : existingDefinitions) {
            for (OperationDefinition nDef : newDefinitions) {
                if (def.equals(nDef)) {
                    def.setDescription(nDef.getDescription());
                    def.setDisplayName(nDef.getDisplayName());
                    def.setParametersConfigurationDefinition(nDef.getParametersConfigurationDefinition());
                    def.setResourceVersionRange(nDef.getResourceVersionRange());
                    def.setResultsConfigurationDefinition(nDef.getResultsConfigurationDefinition());
                    def.setTimeout(nDef.getTimeout());
                }
            }
        }

        for (OperationDefinition newOp : newOps) {
            existingType.addOperationDefinition(newOp); // does the back link as well
        }

        existingDefinitions.removeAll(opsToRemove);
        for (OperationDefinition opToDelete : opsToRemove) {
            entityManager.remove(opToDelete);
        }
    }

    private void updateMeasurementDefinitions(ResourceType newType, ResourceType existingType) {
        if (newType.getMetricDefinitions() != null) {
            Set<MeasurementDefinition> existingDefinitions = existingType.getMetricDefinitions();
            if (existingDefinitions.isEmpty()) {
                // They're all new.
                for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                    existingType.addMetricDefinition(newDefinition);
                    entityManager.persist(newDefinition);
                }
            } else {
                // Update existing or add new metrics
                for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                    boolean found = false;
                    for (MeasurementDefinition existingDefinition : existingDefinitions) {
                        if (existingDefinition.getName().equals(newDefinition.getName())
                            && (existingDefinition.isPerMinute() == newDefinition.isPerMinute())) {
                            found = true;
                            existingDefinition.update(newDefinition, false);
                            entityManager.merge(existingDefinition);
                            break;
                        }
                    }

                    if (!found) {
                        // Its new, create it
                        existingType.addMetricDefinition(newDefinition);
                        entityManager.persist(newDefinition);
                    }
                }

                /*
                 * Now delete outdated measurement definitions First find them ...
                 */
                List<MeasurementDefinition> definitionsToDelete = new ArrayList<MeasurementDefinition>();
                for (MeasurementDefinition existingDefinition : existingDefinitions) {
                    if (!newType.getMetricDefinitions().contains(existingDefinition)) {
                        definitionsToDelete.add(existingDefinition);
                    }
                }

                // ... and remove them
                if (log.isDebugEnabled()) {
                    log.debug("Measurement definitions to be deleted: " + definitionsToDelete);
                }

                existingDefinitions.removeAll(definitionsToDelete);
                for (MeasurementDefinition definitionToDelete : definitionsToDelete) {
                    measurementDefinitionManager.removeMeasurementDefinition(definitionToDelete);
                }

                entityManager.flush();

                // TODO send updates to agents ?
            }
        }
        // TODO what if they are null? --> delete everything from existingType
        // not needed see JBNADM-1639
    }

    /**
     * Updates the database with new package definitions found in the new resource type. Any definitions not found in
     * the new type but were previously in the existing resource type will be removed. Any definitions common to both
     * will be merged.
     *
     * @param newType      new resource type containing updated package definitions
     * @param existingType old resource type with existing package definitions
     */
    private void updateContentDefinitions(ResourceType newType, ResourceType existingType) {
        // Easy case: If there are no package definitions in the new type, null out any in the existing and return
        if ((newType.getPackageTypes() == null) || (newType.getPackageTypes().size() == 0)) {
            existingType.setPackageTypes(null);
            return;
        }

        // The new type has package definitions

        // Easy case: If the existing type did not have any package definitions, simply use the new type defs and return
        if ((existingType.getPackageTypes() == null) || (existingType.getPackageTypes().size() == 0)) {
            for (PackageType newPackageType : newType.getPackageTypes()) {
                newPackageType.setResourceType(existingType);
                entityManager.persist(newPackageType);
            }

            existingType.setPackageTypes(newType.getPackageTypes());
            return;
        }

        // Both the new and existing types have definitions, so merge
        Set<PackageType> existingPackageTypes = existingType.getPackageTypes();
        Map<String, PackageType> newPackageTypeDefinitions = new HashMap<String, PackageType>(newType.getPackageTypes()
            .size());
        for (PackageType newPackageType : newType.getPackageTypes()) {
            newPackageTypeDefinitions.put(newPackageType.getName(), newPackageType);
        }

        // Remove all definitions that are in the existing type but not in the new type
        List<PackageType> removedPackageTypes = new ArrayList<PackageType>(existingType.getPackageTypes());
        removedPackageTypes.removeAll(newType.getPackageTypes());
        for (PackageType removedPackageType : removedPackageTypes) {
            existingType.removePackageType(removedPackageType);
            entityManager.remove(removedPackageType);
        }

        // Merge definitions that were already in the existing type and again in the new type
        List<PackageType> mergedPackageTypes = new ArrayList<PackageType>(existingType.getPackageTypes());
        mergedPackageTypes.retainAll(newType.getPackageTypes());

        for (PackageType mergedPackageType : mergedPackageTypes) {
            mergedPackageType.update(newPackageTypeDefinitions.get(mergedPackageType.getName()));
            entityManager.merge(mergedPackageType);
        }

        // Persist all new definitions
        List<PackageType> newPackageTypes = new ArrayList<PackageType>(newType.getPackageTypes());
        newPackageTypes.removeAll(existingType.getPackageTypes());

        for (PackageType newPackageType : newPackageTypes) {
            newPackageType.setResourceType(existingType);
            entityManager.persist(newPackageType);
            existingPackageTypes.add(newPackageType);
        }
    }

    /**
     * Updates the database with new subcategory definitions found in the new resource type. Any definitions common to
     * both will be merged.
     *
     * @param newType      new resource type containing updated definitions
     * @param existingType old resource type with existing definitions
     */
    private void addAndUpdateSubCategories(ResourceType newType, ResourceType existingType) {
        // we'll do the removal of all definitions that are in the existing type but not in the new type
        // once the child resource types have had a chance to stop referencing any old subcategories

        // Easy case: If the existing type did not have any definitions, simply save the new type defs and return
        if (existingType.getSubCategories() == null) {
            for (ResourceSubCategory newSubCategory : newType.getSubCategories()) {
                existingType.addSubCategory(newSubCategory);
                entityManager.persist(newSubCategory);
            }

            return;
        }

        // Merge definitions that were already in the existing type and also in the new type
        //
        // First, put the new subcategories in a map for easier access when iterating over the existing ones
        Map<String, ResourceSubCategory> subCategoriesFromNewType = new HashMap<String, ResourceSubCategory>(newType
            .getSubCategories().size());
        for (ResourceSubCategory newSubCategory : newType.getSubCategories()) {
            subCategoriesFromNewType.put(newSubCategory.getName(), newSubCategory);
        }

        // Second, loop over the sub categories that need to be merged and update and persist them
        List<ResourceSubCategory> mergedSubCategories = new ArrayList<ResourceSubCategory>(existingType
            .getSubCategories());
        mergedSubCategories.retainAll(subCategoriesFromNewType.values());
        for (ResourceSubCategory existingSubCat : mergedSubCategories) {
            updateSubCategory(existingSubCat, subCategoriesFromNewType.get(existingSubCat.getName()));
            entityManager.merge(existingSubCat);
        }

        // Persist all new definitions
        List<ResourceSubCategory> newSubCategories = new ArrayList<ResourceSubCategory>(newType.getSubCategories());
        newSubCategories.removeAll(existingType.getSubCategories());
        for (ResourceSubCategory newSubCat : newSubCategories) {
            existingType.addSubCategory(newSubCat);
            entityManager.persist(newSubCat);
        }
    }

    private void updateSubCategory(ResourceSubCategory existingSubCat, ResourceSubCategory newSubCategory) {
        // update the basic properties
        existingSubCat.update(newSubCategory);

        // we'll do the removal of all child subcategories that are in the existing subcat but not in the new once
        // once the child resource types have had a chance to stop referencing any old subcategories

        // Easy case: If the existing sub category did not have any child sub categories,
        // simply use the ones from the new type
        if ((existingSubCat.getChildSubCategories() == null) || existingSubCat.getChildSubCategories().isEmpty()) {
            for (ResourceSubCategory newChildSubCategory : newSubCategory.getChildSubCategories()) {
                existingSubCat.addChildSubCategory(newChildSubCategory);
                entityManager.persist(newChildSubCategory);
            }

            return;
        }

        // Merge definitions that were already in the existing sub cat and also in the new one
        //
        // First, put the new child sub categories in a map for easier access when iterating over the existing ones
        Map<String, ResourceSubCategory> childSubCategoriesFromNewSubCat = new HashMap<String, ResourceSubCategory>(
            newSubCategory.getChildSubCategories().size());
        for (ResourceSubCategory newChildSubCategory : newSubCategory.getChildSubCategories()) {
            childSubCategoriesFromNewSubCat.put(newChildSubCategory.getName(), newChildSubCategory);
        }

        // Second, loop over the sub categories that need to be merged and update and persist them
        List<ResourceSubCategory> mergedChildSubCategories = new ArrayList<ResourceSubCategory>(existingSubCat
            .getChildSubCategories());
        mergedChildSubCategories.retainAll(childSubCategoriesFromNewSubCat.values());
        for (ResourceSubCategory existingChildSubCategory : mergedChildSubCategories) {
            // recursively update childSubCategory
            updateSubCategory(existingChildSubCategory, childSubCategoriesFromNewSubCat.get(existingChildSubCategory
                .getName()));
            entityManager.merge(existingChildSubCategory);
        }

        // Persist all new definitions
        List<ResourceSubCategory> newChildSubCategories = new ArrayList<ResourceSubCategory>(newSubCategory
            .getChildSubCategories());
        newChildSubCategories.removeAll(existingSubCat.getChildSubCategories());
        for (ResourceSubCategory newChildSubCategory : newChildSubCategories) {
            entityManager.persist(newChildSubCategory);
            existingSubCat.addChildSubCategory(newChildSubCategory);
        }
    }

    /**
     * Remove all sub category definitions that are in the existing type but not in the new type
     *
     * @param newType      new resource type containing updated definitions
     * @param existingType old resource type with existing definitions
     */
    private void removeSubCategories(ResourceType newType, ResourceType existingType) {
        // Remove all definitions that are in the existing type but not in the new type
        List<ResourceSubCategory> removedSubCategories = new ArrayList<ResourceSubCategory>(existingType
            .getSubCategories());
        removedSubCategories.removeAll(newType.getSubCategories());
        for (ResourceSubCategory removedSubCat : removedSubCategories) {
            // remove it from the resourceType too, so we dont try to persist it again
            // when saving the type
            existingType.getSubCategories().remove(removedSubCat);
            entityManager.remove(removedSubCat);
        }

        // now need to recursively remove any child sub categories which no longer appear
        removeChildSubCategories(existingType.getSubCategories(), newType.getSubCategories());
    }

    private void removeChildSubCategories(List<ResourceSubCategory> existingSubCategories,
        List<ResourceSubCategory> newSubCategories) {
        // create a map of the new sub categories, for easier retrieval
        Map<String, ResourceSubCategory> mapOfNewSubCategories = new HashMap<String, ResourceSubCategory>(
            newSubCategories.size());
        for (ResourceSubCategory newSubCategory : newSubCategories) {
            mapOfNewSubCategories.put(newSubCategory.getName(), newSubCategory);
        }

        for (ResourceSubCategory existingSubCat : existingSubCategories) {
            // Remove all definitions that are in the existing type but not in the new type
            List<ResourceSubCategory> removedChildSubCategories = new ArrayList<ResourceSubCategory>(existingSubCat
                .getChildSubCategories());
            List<ResourceSubCategory> newChildSubCategories = mapOfNewSubCategories.get(existingSubCat.getName())
                .getChildSubCategories();
            removedChildSubCategories.removeAll(newChildSubCategories);
            for (ResourceSubCategory removedChildSubCat : removedChildSubCategories) {
                // remove  subcat and all its children, due to the CASCADE.DELETE
                existingSubCat.removeChildSubCategory(removedChildSubCat);
                entityManager.remove(removedChildSubCat);
            }

            // for any remaining children of this subCat, see if any of their children should be removed
            removeChildSubCategories(existingSubCat.getChildSubCategories(), newChildSubCategories);
        }
    }

    /**
     * deals with the content of <resource-configuration>
     */
    private void updateResourceConfiguration(ResourceType newType, ResourceType existingType) {
        ConfigurationDefinition newResourceConfigurationDefinition = newType.getResourceConfigurationDefinition();
        if (newResourceConfigurationDefinition != null) {
            if (existingType.getResourceConfigurationDefinition() == null) // everything new
            {
                entityManager.persist(newResourceConfigurationDefinition);
                existingType.setResourceConfigurationDefinition(newResourceConfigurationDefinition);
            } else {
                ConfigurationDefinition existingDefinition = existingType.getResourceConfigurationDefinition();
                configurationMetadataManager.updateConfigurationDefinition(newResourceConfigurationDefinition,
                    existingDefinition);
            }
        } else // newDefinition == null
        {
            if (existingType.getResourceConfigurationDefinition() != null) {
                existingType.setResourceConfigurationDefinition(null);
            }
        }
    }

    /**
     * Return a set containing those element that are in reference, but not in first. Both input sets are not modified
     *
     * @param  <T>
     * @param  first
     * @param  reference
     *
     * @return
     */
    private <T> Set<T> missingInFirstSet(Set<T> first, Set<T> reference) {
        Set<T> result = new HashSet<T>();

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
     * Return a new Set with elements both in first and second passed collection.
     *
     * @param  <T>    Type of set
     * @param  first  First set
     * @param  second Second set
     *
     * @return a new set (depending on input type) with elements in first and second
     */
    private <T> Set<T> intersection(Set<T> first, Set<T> second) {
        Set<T> result = new HashSet<T>();
        if ((first != null) && (second != null)) {
            result.addAll(first);
            result.retainAll(second);
        }

        return result;
    }
}