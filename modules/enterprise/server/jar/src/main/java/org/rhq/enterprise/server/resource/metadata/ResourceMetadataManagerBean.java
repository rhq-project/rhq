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
package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.ejb3.annotation.TransactionTimeout;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;
import org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.inventory.InventoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.scheduler.jobs.AsyncResourceDeleteJob;
import org.rhq.enterprise.server.scheduler.jobs.PurgeResourceTypesJob;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

/**
 * This class manages the metadata for resources. Plugins are registered against this bean so that their metadata can be
 * pulled out and stored as necessary.
 *
 * @author Greg Hinkle
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 * @author Ian Springer
 */
@Stateless
public class ResourceMetadataManagerBean implements ResourceMetadataManagerLocal {
    private final Log log = LogFactory.getLog(ResourceMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private InventoryManagerLocal inventoryManager;

    @EJB
    private ResourceMetadataManagerLocal resourceMetadataManager; // self

    @EJB
    private ContentMetadataManagerLocal contentMetadataMgr;

    @EJB
    private OperationMetadataManagerLocal operationMetadataMgr;

    @EJB
    private EventMetdataManagerLocal eventMetadataMgr;

    @EJB
    private MeasurementMetadataManagerLocal measurementMetadataMgr;

    @EJB
    private AlertMetadataManagerLocal alertMetadataMgr;

    @EJB
    private ResourceConfigurationMetadataManagerLocal resourceConfigMetadataMgr;

    @EJB
    private PluginConfigurationMetadataManagerLocal pluginConfigMetadataMgr;

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void updateTypes(Set<ResourceType> resourceTypes) throws Exception {
        // Only process the type if it is a non-runs-inside type (i.e. not a child of some other type X at this same
        // level in the type hierarchy). runs-inside types which we skip here will get processed at the next level down
        // when we recursively process type X's children.
        Set<ResourceType> allChildren = new HashSet<ResourceType>();

        Queue<ResourceType> unvisitedChildren = new LinkedList<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            unvisitedChildren.addAll(resourceType.getChildResourceTypes());
        }

        while (!unvisitedChildren.isEmpty()) {
            ResourceType childResourceType = unvisitedChildren.poll();
            if (!allChildren.contains(childResourceType)) {
                allChildren.add(childResourceType);
                unvisitedChildren.addAll(childResourceType.getChildResourceTypes());
            }
        }

        Set<ResourceType> nonRunsInsideResourceTypes = new LinkedHashSet<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            if (!allChildren.contains(resourceType)) {
                nonRunsInsideResourceTypes.add(resourceType);
            }
        }

        // Iterate the resource types breadth-first, so all platform types get added before any server types or platform
        // service types. This way, we'll be able to set all of the platform types as parents of the server types and
        // platform service types. It's also helpful for other types with multiple "runs-inside" parent types (e.g
        // Hibernate Entities), since it ensures the parent types will get persisted prior to the child types.
        if (log.isDebugEnabled()) {
            log.debug("Processing types: " + nonRunsInsideResourceTypes + "...");
        }
        Set<ResourceType> legitimateChildren = new HashSet<ResourceType>();
        for (ResourceType resourceType : nonRunsInsideResourceTypes) {
            long startTime = System.currentTimeMillis();
            resourceType = resourceMetadataManager.updateTypeInNewTx(resourceType);
            long endTime = System.currentTimeMillis();
            log.debug("Updated resource type [" + toConciseString(resourceType) + "] in " + (endTime - startTime)
                + " ms");

            legitimateChildren.addAll(resourceType.getChildResourceTypes());
        }
        // Only recurse if there are actually children - this prevents infinite recursion.
        if (!legitimateChildren.isEmpty()) {
            updateTypes(legitimateChildren);
        }
    }

    // NO TRANSACTION SHOULD BE ACTIVE ON ENTRY
    // Start with no transaction so we can control the transactional boundaries. Obsolete type removal removes
    // resources of the obsolete type. We need to avoid an umbrella transaction for the type removal because large
    // inventories of obsolete resources will generate very large transactions. Potentially resulting in timeouts
    // or other issues.
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void removeObsoleteTypes(Subject subject, String pluginName, PluginMetadataManager metadataCache) {

        Set<ResourceType> obsoleteTypes = new HashSet<ResourceType>();
        Set<ResourceType> legitTypes = new HashSet<ResourceType>();

        try {
            resourceMetadataManager.getPluginTypes(subject, pluginName, legitTypes, obsoleteTypes, metadataCache);

            if (!obsoleteTypes.isEmpty()) {
                log.info("Removing " + obsoleteTypes.size() + " obsolete types: " + obsoleteTypes + "...");

                // removeResourceTypes(subject, obsoleteTypes, new HashSet<ResourceType>(obsoleteTypes));

                // 1) Mark the obsolete types for deletion and uninventory the doomed resources
                List<Integer> obsoleteTypeIds = new ArrayList<Integer>(obsoleteTypes.size());
                for (ResourceType rt : obsoleteTypes) {
                    obsoleteTypeIds.add(rt.getId());
                }
                inventoryManager.markTypesDeleted(obsoleteTypeIds, true);

                // 2) Immediately remove the uninventoried resources by forcing the normally async work to run in-band
                new AsyncResourceDeleteJob().execute(null);

                // 3) Immediately finish removing the deleted types by forcing the normally async work to run in-band
                new PurgeResourceTypesJob().executeJobCode(null);
            }
        } catch (Throwable t) {
            // Catch all exceptions, so a failure here does not cause the outer tx to rollback.
            log.error("Failure during removal of obsolete ResourceTypes and Subcategories.", t);
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    @SuppressWarnings("unchecked")
    public void getPluginTypes(Subject subject, String pluginName, Set<ResourceType> legitTypes,
        Set<ResourceType> obsoleteTypes, PluginMetadataManager metadataCache) {
        try {
            Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_PLUGIN);
            query.setParameter("plugin", pluginName);
            List<ResourceType> existingTypes = query.getResultList();

            if (existingTypes != null) {

                for (ResourceType existingType : existingTypes) {
                    if (metadataCache.getType(existingType.getName(), existingType.getPlugin()) == null) {
                        // The type is obsolete - (i.e. it's no longer defined by the plugin).
                        obsoleteTypes.add(existingType);
                    } else {
                        legitTypes.add(existingType);
                    }
                }
            }
        } catch (Exception e) {
            // Catch all exceptions, so a failure here does not cause the outer tx to rollback.
            log.error("Failure during removal of obsolete ResourceTypes and Subcategories.", e);
        }
    }

    @RequiredPermission(Permission.MANAGE_SETTINGS)
    public void completeRemoveResourceType(Subject subject, ResourceType existingType) {
        existingType = entityManager.find(ResourceType.class, existingType.getId());

        if (entityManager.contains(existingType)) {
            entityManager.refresh(existingType);
        }

        // Completely remove the type from the type hierarchy.
        removeFromParents(existingType);
        removeFromChildren(existingType);
        entityManager.merge(existingType);

        contentMetadataMgr.deleteMetadata(subject, existingType);

        entityManager.flush();
        existingType = entityManager.find(existingType.getClass(), existingType.getId());

        try {
            alertMetadataMgr.deleteAlertTemplates(subject, existingType);
        } catch (Exception e) {
            throw new RuntimeException("Alert template deletion failed. Cannot finish deleting " + existingType, e);
        }

        entityManager.flush();
        existingType = entityManager.find(existingType.getClass(), existingType.getId());

        // Remove all compatible groups that are of the type.
        List<ResourceGroup> compatGroups = existingType.getResourceGroups();
        if (compatGroups != null) {
            Iterator<ResourceGroup> compatGroupIterator = compatGroups.iterator();
            while (compatGroupIterator.hasNext()) {
                ResourceGroup compatGroup = compatGroupIterator.next();
                try {
                    resourceGroupManager.deleteResourceGroup(subject, compatGroup.getId());
                } catch (ResourceGroupDeleteException e) {
                    throw new RuntimeException(e);
                }
                compatGroupIterator.remove();
            }
        }
        entityManager.flush();

        measurementMetadataMgr.deleteMetadata(existingType);
        entityManager.flush();

        // TODO: Clean out event definitions?

        // Finally, remove the type itself.
        // Refresh it first to make sure any newly discovered Resources of the type get added to the persistence
        // context and hopefully get removed via cascade when we remove the type.
        entityManager.refresh(existingType);
        entityManager.remove(existingType);
        entityManager.flush();
    }

    private void removeFromParents(ResourceType typeToBeRemoved) {
        // Wrap in new HashSet to avoid ConcurrentModificationExceptions.
        Set<ResourceType> parentTypes = new HashSet<ResourceType>(typeToBeRemoved.getParentResourceTypes());
        for (ResourceType parentType : parentTypes) {
            parentType.removeChildResourceType(typeToBeRemoved);
            entityManager.merge(parentType);
        }
    }

    private void removeFromChildren(ResourceType typeToBeRemoved) {
        // Wrap in new HashSet to avoid ConcurrentModificationExceptions.
        Set<ResourceType> childTypes = new HashSet<ResourceType>(typeToBeRemoved.getChildResourceTypes());
        for (ResourceType childType : childTypes) {
            childType.removeParentResourceType(typeToBeRemoved);
            entityManager.merge(childType);
        }
    }

    @TransactionTimeout(1800)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResourceType updateTypeInNewTx(ResourceType resourceType) {

        // see if there is already an existing type that we need to update
        log.info("Updating resource type [" + toConciseString(resourceType) + "]...");

        ResourceType existingType;
        try {
            existingType = resourceTypeManager.getResourceTypeByNameAndPlugin(resourceType.getName(),
                resourceType.getPlugin());

        } catch (NonUniqueResultException nure) {
            log.info("Found more than one existing ResourceType for " + resourceType);
            // TODO: Delete the redundant ResourceTypes to get the DB into a valid state.
            throw new IllegalStateException(nure);
        }

        // Connect the parent types if they exist, which they should.
        // We'll do this no matter if the resourceType exists or not - but we use existing vs. resourceType appropriately
        // This is to support the case when an existing type gets a new parent resource type in <runs-inside>
        updateParentResourceTypes(resourceType, existingType);

        if (existingType == null) {
            persistNewType(resourceType);
        } else {
            mergeExistingType(resourceType, existingType);
        }

        return resourceType;
    }

    /**
     * This impl needs to take into consideration that a large resource population may already exist for the type.
     */
    private void mergeExistingType(ResourceType resourceType, ResourceType existingType) {
        log.debug("Merging type [" + resourceType + "] + into existing type [" + existingType + "]...");

        // even though we've updated our child types to use new subcategory references, its still
        // not safe to delete the old sub categories yet, because we haven't yet deleted all of the old
        // child types which may still be referencing these sub categories

        // Update the rest of these related resources
        long startTime = System.currentTimeMillis();
        pluginConfigMetadataMgr.updatePluginConfigurationDefinition(existingType, resourceType);
        long endTime = System.currentTimeMillis();
        log.debug("Updated plugin configuration definition for ResourceType[" + toConciseString(existingType) + "] in "
            + (endTime - startTime) + " ms");

        resourceConfigMetadataMgr.updateResourceConfigurationDefinition(existingType, resourceType);

        measurementMetadataMgr.updateMetadata(existingType, resourceType);
        contentMetadataMgr.updateMetadata(existingType, resourceType);
        operationMetadataMgr.updateMetadata(existingType, resourceType);

        resourceMetadataManager.updateDriftMetadata(existingType, resourceType);

        updateProcessScans(resourceType, existingType);

        eventMetadataMgr.updateMetadata(existingType, resourceType);

        // Update the type itself
        if (resourceType.getCategory() != existingType.getCategory()) {
            log.info("Changing category of Resource type [" + resourceType + "] from " + existingType.getCategory()
                + " to " + resourceType.getCategory() + "...");
            existingType.setCategory(resourceType.getCategory());
        }

        existingType.setCreateDeletePolicy(resourceType.getCreateDeletePolicy());
        existingType.setCreationDataType(resourceType.getCreationDataType());
        existingType.setDescription(resourceType.getDescription());
        existingType.setDisplayName(resourceType.getDisplayName());
        existingType.setSingleton(resourceType.isSingleton());
        existingType.setSupportsManualAdd(resourceType.isSupportsManualAdd());
        existingType.setSupportsMissingAvailabilityType(resourceType.isSupportsMissingAvailabilityType());
        existingType.setSubCategory(resourceType.getSubCategory());

        existingType = entityManager.merge(existingType);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDriftMetadata(ResourceType existingType, ResourceType resourceType) {
        existingType = entityManager.find(ResourceType.class, existingType.getId());

        //
        // Only if one or more drift definitions are different do we have to do anything to the persisted metadata.
        //

        Set<DriftDefinitionTemplate> existingDriftTemplates = existingType.getDriftDefinitionTemplates();

        // We are only concerned with the plugin defined templates, user defined templates are not affected.

        Set<DriftDefinitionTemplate> existingPluginDriftTemplates = new HashSet<DriftDefinitionTemplate>(
            existingDriftTemplates.size());

        for (DriftDefinitionTemplate existingTemplate : existingDriftTemplates) {
            if (!existingTemplate.isUserDefined()) {
                existingPluginDriftTemplates.add(existingTemplate);
            }
        }

        Set<DriftDefinitionTemplate> newPluginDriftTemplates = resourceType.getDriftDefinitionTemplates();
        // note: the size of the sets are typically really small (usually between 1 and 3),
        // so iterating through them is fast.

        // look at all the configs to ensure we detect any changes to individual settings on the templates
        Set<String> existingNames = new HashSet<String>(existingPluginDriftTemplates.size());
        DriftDefinitionComparator dirComp = new DriftDefinitionComparator(CompareMode.ONLY_DIRECTORY_SPECIFICATIONS);

        for (Iterator<DriftDefinitionTemplate> i = existingDriftTemplates.iterator(); i.hasNext();) {
            DriftDefinitionTemplate existingTemplate = i.next();

            String existingName = existingTemplate.getName();
            DriftDefinition existingDef = existingTemplate.getTemplateDefinition();
            Set<DriftDefinition> attachedDefs = existingTemplate.getDriftDefinitions();
            boolean noAttachedDefs = (null == attachedDefs || attachedDefs.isEmpty());
            boolean notPinned = !existingTemplate.isPinned();
            boolean stillDefined = false;

            // for later to determine if any existing templates are no longer defined in the plugin
            existingNames.add(existingName);

            for (DriftDefinitionTemplate newTemplate : newPluginDriftTemplates) {
                String newName = newTemplate.getName();

                // The new template existed previously. See if it has changed and if so, in what way:
                //
                // IF      the existingTemplate
                //         has no attached defs AND
                //         is not pinned
                // THEN    we can update it with impunity
                // ELSE IF the directories have not changed
                // THEN    we can update the base info fields only
                //    Note that in the latter case we update the template but we will not push the
                //    changes down to attached defs.  This is a little odd because the template and defs can
                //    get out of sync, but we don't want a plugin change to affect existing defs in case
                //    the user has made manual changes, or wants it the way it is.
                if (newName.equals(existingName)) {
                    stillDefined = true;

                    DriftDefinition newDef = newTemplate.getTemplateDefinition();
                    boolean noDirChanges = (0 == dirComp.compare(existingDef, newDef));

                    if ((noAttachedDefs && notPinned) || noDirChanges) {
                        existingTemplate.setTemplateDefinition(newDef);

                    } else {
                        // can't update directories for an existing template if pinned and/or having attached defs
                        log.error("Failed to update drift definition [" + newName + "] on type ["
                            + resourceType.getName()
                            + "]. It is not allowed to update directories on an existing template that is pinned "
                            + "or has attached definitions. It would invalidate pinned snapshots as the fileset "
                            + "would no longer map from template to definition.");
                    }

                    break;
                }
            }

            // If the template is no longer defined then what we do depends on whether it has attached
            // definitions. If not it can be deleted, otherwise we keep it around so the user doesn't lose
            // anything, but set it to user-defined, in essence removing it from the plugin.
            if (!stillDefined) {
                if (noAttachedDefs) {
                    entityManager.remove(existingTemplate);
                    i.remove();

                } else {
                    existingTemplate.setUserDefined(true);
                    log.warn("Plugin no longer defines drift template [" + existingTemplate.getName() + "] on type ["
                        + resourceType.getName()
                        + "]. This template has attached definitions.  To preserve the existing definitions the "
                        + " template will not be removed but is instead being set as user-defined.  The user will "
                        + " be responsible for further maintenance of this template.");
                }
            }
        }

        // Now add new templates, not previously defined
        for (DriftDefinitionTemplate newTemplate : newPluginDriftTemplates) {
            String newName = newTemplate.getName();

            if (existingNames.contains(newName)) {
                continue;
            }

            newTemplate.setResourceType(existingType);
            entityManager.persist(newTemplate);
            existingDriftTemplates.add(newTemplate);
        }
    }

    private void persistNewType(ResourceType resourceType) {
        log.info("Persisting new ResourceType [" + toConciseString(resourceType) + "]...");
        // If the type didn't exist then we'll persist here which will cascade through
        // all child types as well as plugin and resource configs and their delegate types and
        // metric and operation definitions and their dependent types,
        // but first do some validity checking.

        // Ensure that the new type has any built-in metrics (like Availability Type)
        MeasurementMetadataManagerBean.getMetricDefinitions(resourceType);

        //Ensure any explicitly targeted resource types of a bundle type are refreshed with their
        //persisted counterparts (which should have been persisted before persisting the type
        //due to the dependency graph ordering)
        if (resourceType.getBundleType() != null &&
            !resourceType.getBundleType().getExplicitlyTargetedResourceTypes().isEmpty()) {

            Set<ResourceType> existingTypes = new HashSet<ResourceType>(
                resourceType.getBundleType().getExplicitlyTargetedResourceTypes().size());

            for (ResourceType targetedType : resourceType.getBundleType().getExplicitlyTargetedResourceTypes()) {
                ResourceType existingType = resourceTypeManager.getResourceTypeByNameAndPlugin(targetedType.getName(),
                    targetedType.getPlugin());

                existingTypes.add(existingType);
            }

            resourceType.getBundleType().getExplicitlyTargetedResourceTypes().clear();
            resourceType.getBundleType().getExplicitlyTargetedResourceTypes().addAll(existingTypes);
        }

        entityManager.merge(resourceType);
    }

    private void updateParentResourceTypes(ResourceType newType, ResourceType existingType) {
        if (log.isDebugEnabled()) {
            if (existingType != null) {
                log.debug("Setting parent types on existing type: " + existingType + " to ["
                    + newType.getParentResourceTypes() + "] - current parent types are ["
                    + existingType.getParentResourceTypes() + "]...");
            } else {
                log.debug("Setting parent types on new type: " + newType + " to [" + newType.getParentResourceTypes()
                    + "]...");
            }
        }

        Set<ResourceType> newParentTypes = newType.getParentResourceTypes();
        newType.setParentResourceTypes(new HashSet<ResourceType>());
        Set<ResourceType> originalExistingParentTypes = new HashSet<ResourceType>();
        if (existingType != null) {
            originalExistingParentTypes.addAll(existingType.getParentResourceTypes());
        }
        for (ResourceType newParentType : newParentTypes) {
            try {
                boolean isExistingParent = originalExistingParentTypes.remove(newParentType);
                if (existingType == null || !isExistingParent) {
                    ResourceType realParentType = (ResourceType) entityManager
                        .createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN)
                        .setParameter("name", newParentType.getName())
                        .setParameter("plugin", newParentType.getPlugin()).getSingleResult();
                    ResourceType type = (existingType != null) ? existingType : newType;
                    if (existingType != null) {
                        log.info("Adding ResourceType [" + toConciseString(type) + "] as child of ResourceType ["
                            + toConciseString(realParentType) + "]...");
                    }
                    realParentType.addChildResourceType(type);
                }
            } catch (NoResultException nre) {
                throw new RuntimeException("Couldn't persist type [" + newType + "] because parent [" + newParentType
                    + "] wasn't already persisted.");
            }
        }

        for (ResourceType obsoleteParentType : originalExistingParentTypes) {
            log.info("Removing type [" + toConciseString(existingType) + "] from parent type ["
                + toConciseString(obsoleteParentType) + "]...");
            obsoleteParentType.removeChildResourceType(existingType);
            moveResourcesToNewParent(existingType, obsoleteParentType, newParentTypes);
        }
    }

    private static String toConciseString(ResourceType type) {
        return (type != null) ? (type.getPlugin() + ":" + type.getName() + "(id=" + type.getId() + ")") : "null";
    }

    private void moveResourcesToNewParent(ResourceType existingType, ResourceType obsoleteParentType,
        Set<ResourceType> newParentTypes) {
        final Subject overlord = subjectManager.getOverlord();
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceTypeId(existingType.getId());
        criteria.addFilterParentResourceTypeId(obsoleteParentType.getId());

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
            @Override
            public PageList<Resource> execute(ResourceCriteria criteria) {
                return resourceManager.findResourcesByCriteria(overlord, criteria);
            }
        };

        CriteriaQuery<Resource, ResourceCriteria> resources = new CriteriaQuery<Resource, ResourceCriteria>(criteria,
            queryExecutor);

        for (Resource resource : resources) {
            Resource newParent = null;
            newParentTypes: for (ResourceType newParentType : newParentTypes) {
                Resource ancestorResource = resource.getParentResource();
                while (ancestorResource != null) {
                    if (ancestorResource.getResourceType().equals(newParentType)) {
                        // We found an ancestor to be the new parent of our orphaned Resource.
                        newParent = ancestorResource;
                        break newParentTypes;
                    }
                    ancestorResource = ancestorResource.getParentResource();
                }
                for (Resource childResource : resource.getChildResources()) {
                    if (childResource.getResourceType().equals(newParentType)) {
                        // We found a child to be the new parent of our orphaned Resource.
                        // TODO: Check if there are are multiple children of the new parent type. If so,
                        //       log an error and don't move the resource.
                        newParent = childResource;
                        break newParentTypes;
                    }
                }
            }
            if (newParent != null) {
                if (resource.getParentResource() != null) {
                    resource.getParentResource().removeChildResource(resource);
                }
                newParent.addChildResource(resource);
                // Assigning a new parent changes the ancestry for the resource and its children. Since the
                // children are not handled in this method, update their ancestry now.
                resourceManager.updateAncestry(subjectManager.getOverlord(), resource.getId());
            } else {
                log.info("We were unable to move " + resource + " from invalid parent " + resource.getParentResource()
                    + " to a new valid parent with one of the following types: " + newParentTypes);
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

        Set<ProcessScan> scansToPersist = CollectionsUtil.missingInFirstSet(existingScans, newScans);
        Set<ProcessScan> scansToDelete = CollectionsUtil.missingInFirstSet(newScans, existingScans);

        Set<ProcessScan> scansToUpdate = CollectionsUtil.intersection(existingScans, newScans);

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

    @Override
    public void removeObsoleteSubCategories(Subject subject, ResourceType newType, ResourceType existingType) {
        // TODO Auto-generated method stub

    }
}
