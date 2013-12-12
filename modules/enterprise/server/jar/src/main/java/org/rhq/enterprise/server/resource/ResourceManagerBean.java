/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.resource;

import static org.rhq.core.domain.criteria.Criteria.Restriction.COLLECTION_ONLY;
import static org.rhq.core.domain.criteria.Criteria.Restriction.COUNT_ONLY;
import static org.rhq.enterprise.server.util.CriteriaQueryGenerator.getPageControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.ResourceRepo;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.drift.JPADrift;
import org.rhq.core.domain.drift.JPADriftChangeSet;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.measurement.calltime.CallTimeDataKey;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationScheduleEntity;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.resource.composite.ResourceIdFlyWeight;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.flyweight.FlyweightCache;
import org.rhq.core.domain.resource.flyweight.ResourceFlyweight;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.IntExtractor;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.discovery.DiscoveryServerServiceImpl;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.disambiguation.DisambiguationUpdateStrategy;
import org.rhq.enterprise.server.resource.disambiguation.Disambiguator;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.ResourceHandlerBean;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.QueryUtility;

/**
 * A manager that provides methods for creating, updating, deleting, and querying {@link Resource}s.
 *
 * @author Ian Springer
 * @author Joseph Marques
 * @author Jay Shaughnessy (delete operations)
 */
@Stateless
public class ResourceManagerBean implements ResourceManagerLocal, ResourceManagerRemote {
    private final Log log = LogFactory.getLog(ResourceManagerBean.class);

    private final static String BOUNDED_MAX_RESOURCES = "1000";
    private final static String BOUNDED_MAX_RESOURCES_BY_TYPE = "200";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private ResourceGroupManagerLocal groupManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private ResourceManagerLocal resourceManager; // ourself, for xactional semantic consistency
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private ResourceTypeManagerLocal typeManager;
    @EJB
    //@IgnoreDependency
    private MeasurementScheduleManagerLocal measurementScheduleManager;
    @EJB
    private AvailabilityManagerLocal availabilityManager;

    public void createResource(Subject user, Resource resource, int parentId) throws ResourceAlreadyExistsException {
        Resource parent = null;
        if (parentId != Resource.ROOT_ID) {
            // not trying to create a root resource, so lookup the parent
            parent = entityManager.find(Resource.class, parentId);

            // uh-oh if the parent could not be found
            if (parent == null) {
                throw new ResourceNotFoundException("Intended parent for new resource does not exist.");
            }

            if (!authorizationManager.hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, parent.getId())) {
                throw new PermissionException("You do not have permission to add this resource as a child.");
            }

            if (getResourceByParentAndKey(user, parent, resource.getResourceKey(), resource.getResourceType()
                .getPlugin(), resource.getResourceType().getName()) != null) {
                throw new ResourceAlreadyExistsException("Resource with key '" + resource.getResourceKey()
                    + "' already exists.");
            }
        }
        if (parent != Resource.ROOT) {
            // Only set the relationships if this is not a root resource.
            // The cardinal rule is to add the relationship in both directions,
            // so the parent will have direct access to this child after the method returns.
            resource.setParentResource(parent);
            parent.addChildResource(resource);
        }

        entityManager.persist(resource);
        log.debug("********* resource persisted ************");
        // Execute sub-methods as overlord to bypass additional security checks.
        Subject overlord = this.subjectManager.getOverlord();
        updateImplicitMembership(overlord, resource);

        // Because this resource is in the process of creation it has no measurement schedules
        // defined. These are needed before applying alert templates for the resource type.
        // This call will create the schedules as necessary and, as a side effect, apply the templates.
        // TODO: jshaughn - This fails for resource types without metric definitions
        int[] resourceIds = new int[] { resource.getId() };
        measurementScheduleManager.findSchedulesForResourceAndItsDescendants(resourceIds, false);
    }

    private void updateImplicitMembership(Subject subject, Resource resource) {
        /*
         * if this is a new root resource, it could not have been a member of some group - explicitly or implicitly
         */
        if (resource.getParentResource() == null) {
            return;
        }

        groupManager.updateImplicitGroupMembership(subject, resource);
    }

    public Resource updateResource(Subject user, Resource resource) {
        Resource persistedResource = entityManager.find(Resource.class, resource.getId());
        if (persistedResource == null) {
            throw new ResourceNotFoundException(resource.getId());
        }

        if (!authorizationManager.hasResourcePermission(user, Permission.MODIFY_RESOURCE, resource.getId())) {
            throw new PermissionException("You do not have permission to modify Resource with id " + resource.getId()
                + ".");
        }

        /*if (getResourceByParentAndKey(user, resource.getParentResource(), resource.getResourceKey()) != null)
         * { throw new ResourceAlreadyExistsException("Resource with key '" + resource.getName() + "' already
         * exists");}*/

        // On name change make sure we update the ancestry as the name is part of the ancestry string
        if (!persistedResource.getName().equals(resource.getName())) {
            persistedResource.setName(resource.getName());
            updateAncestry(persistedResource);
        }
        persistedResource.setLocation(resource.getLocation());
        persistedResource.setDescription(resource.getDescription());

        persistedResource.setAgentSynchronizationNeeded();
        persistedResource.setModifiedBy(user.getName());

        return entityManager.merge(persistedResource);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void uninventoryResourcesOfResourceType(Subject subject, int resourceTypeId) {
        List<Integer> typeIds = new ArrayList<Integer>(1);
        typeIds.add(resourceTypeId);

        List<Integer> resourceIds = resourceManager.findIdsByTypeIds(typeIds);

        if (resourceIds != null && !resourceIds.isEmpty()) {
            log.info("Uninventorying all [" + resourceIds.size() + "] resources with resource type ID of ["
                + resourceTypeId + "]");

            for (Integer resourceId : resourceIds) {
                resourceManager.uninventoryResourceInNewTransaction(resourceId);
            }
        }

        return;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void uninventoryAllResourcesByAgent(Subject user, Agent doomedAgent) {
        Resource platform = resourceManager.getPlatform(doomedAgent);
        if (platform == null) {
            // there is no platform resource - just delete the agent itself
            agentManager.deleteAgent(doomedAgent);
        } else {
            resourceManager.uninventoryResources(user, new int[] { platform.getId() });
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Integer> uninventoryResources(Subject user, int[] resourceIds) {

        List<Integer> result = new ArrayList<Integer>();
        boolean isInventoryManager = authorizationManager.isInventoryManager(user);

        for (Integer resourceId : resourceIds) {
            if (!result.contains(resourceId)) {
                if (!isInventoryManager) {
                    result.addAll(resourceManager.uninventoryResource(user, resourceId));

                } else {
                    result.addAll(resourceManager.uninventoryResourceInNewTransaction(resourceId));
                }
            }
        }

        return result;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Integer> uninventoryResource(Subject user, int resourceId) {

        // make sure the user is authorized to delete this resource (which implies you can delete all its children)
        // TODO: There is a pretty good argument for this being replaced with MANAGE_INVENTORY.  It takes an
        // inventory manager to import resources, so why not to remove them?  But, since no one has complained
        // we're timid about making a change that may hamstring existing setups.

        if (!authorizationManager.hasResourcePermission(user, Permission.DELETE_RESOURCE, resourceId)) {
            throw new PermissionException("You do not have permission to uninventory resource [" + resourceId + "]");
        }

        return resourceManager.uninventoryResourceInNewTransaction(resourceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Integer> uninventoryResourceInNewTransaction(int resourceId) {

        Resource resource = entityManager.find(Resource.class, resourceId);

        if (resource == null) {
            log.info("Delete resource not possible, as resource with id [" + resourceId + "] was not found");
            return Collections.emptyList(); // Resource not found. TODO give a nice message to the user
        }

        // if the resource has no parent, its a platform resource and its agent should be purged too
        Agent doomedAgent = null;
        boolean isDebugEnabled = log.isDebugEnabled();

        try {
            Subject overlord = subjectManager.getOverlord();

            if (resource.getParentResource() == null) {
                // note, this needs to be done before the marking because the agent reference is going to be set to null
                doomedAgent = agentManager.getAgentByResourceId(overlord, resourceId);

                // note - test code does not always provide the agent, if not found, just warn
                if (doomedAgent == null) {
                    log.warn("Platform resource had no associated agent. This warning should occur in TEST code only!");
                }
            }

            AgentClient agentClient = null;
            try {
                // The test code does not always generate agents for the resources. Catch and log any problem but continue
                agentClient = agentManager.getAgentClient(overlord, resourceId);
            } catch (Throwable t) {
                log.warn("No AgentClient found for resource [" + resource
                    + "]. Unable to inform agent of inventory removal (this may be ok): " + t);
            }

            // since we delete the resource asynchronously, make sure we remove things that would cause system
            // side effects after markForDeletion completed but before the resource is actually removed from the DB

            // delete the resource and all its children
            if (isDebugEnabled) {
                log.debug("Marking resource [" + resource + "] for asynchronous uninventory");
            }

            // set agent references null
            // foobar the resourceKeys
            // update the inventory status to UNINVENTORY
            Query toBeDeletedQuery = entityManager.createNamedQuery(Resource.QUERY_FIND_DESCENDANTS);
            toBeDeletedQuery.setParameter("resourceId", resourceId);
            List<Integer> toBeDeletedResourceIds = toBeDeletedQuery.getResultList();

            int i = 0;
            if (isDebugEnabled) {
                log.debug("== total size : " + toBeDeletedResourceIds.size());
            }

            while (i < toBeDeletedResourceIds.size()) {
                int j = i + 1000;
                if (j > toBeDeletedResourceIds.size())
                    j = toBeDeletedResourceIds.size();
                List<Integer> idsToDelete = toBeDeletedResourceIds.subList(i, j);
                if (isDebugEnabled) {
                    log.debug("== Bounds " + i + ", " + j);
                }

                // refresh overlord session for each batch to avoid session timeout
                overlord = subjectManager.getOverlord();
                boolean hasErrors = uninventoryResourcesBulkDelete(overlord, idsToDelete);
                if (hasErrors) {
                    throw new IllegalArgumentException("Could not remove resources from their containing groups");
                }
                i = j;
            }

            // QUERY_MARK_RESOURCES_FOR_ASYNC_DELETION is an expensive recursive query
            // But luckily we have already (through such a recursive query above) determined the doomed resources
            //        Query markDeletedQuery = entityManager.createNamedQuery(Resource.QUERY_MARK_RESOURCES_FOR_ASYNC_DELETION);
            //        markDeletedQuery.setParameter("resourceId", resourceId);
            //        markDeletedQuery.setParameter("status", InventoryStatus.UNINVENTORIED);
            //        int resourcesDeleted = markDeletedQuery.executeUpdate();

            i = 0;
            int resourcesDeleted = 0;
            while (i < toBeDeletedResourceIds.size()) {
                int j = i + 1000;
                if (j > toBeDeletedResourceIds.size())
                    j = toBeDeletedResourceIds.size();
                List<Integer> idsToDelete = toBeDeletedResourceIds.subList(i, j);

                Query markDeletedQuery = entityManager
                    .createNamedQuery(Resource.QUERY_MARK_RESOURCES_FOR_ASYNC_DELETION_QUICK);
                markDeletedQuery.setParameter("resourceIds", idsToDelete);
                markDeletedQuery.setParameter("status", InventoryStatus.UNINVENTORIED);
                resourcesDeleted += markDeletedQuery.executeUpdate();
                i = j;
            }

            if (resourcesDeleted != toBeDeletedResourceIds.size()) {
                log.error("Tried to uninventory " + toBeDeletedResourceIds.size()
                    + " resources, but actually uninventoried " + resourcesDeleted);
            }

            // still need to tell the agent about the removed resources so it stops avail reports
            // but not if this is a synthetic agent that was created in the REST-api
            // See org.rhq.enterprise.server.rest.ResourceHandlerBean.createPlatformInternal()
            // See also https://docs.jboss.org/author/display/RHQ/Virtual+platforms+and+synthetic+agents
            if (agentClient != null) {
                if (agentClient.getAgent() == null || agentClient.getAgent().getName() == null
                    || !agentClient.getAgent().getName().startsWith(ResourceHandlerBean.DUMMY_AGENT_NAME_PREFIX)) { // don't do that on "REST-agents"
                    try {
                        agentClient.getDiscoveryAgentService().uninventoryResource(resourceId);
                    } catch (Exception e) {
                        log.warn(" Unable to inform agent of inventory removal for resource [" + resourceId + "]", e);
                    }
                }
            }

            // now remove the doomed agent. Call flush() to force out any problems with agent removal
            // so that we can catch them and report a better exception.
            if (doomedAgent != null) {
                agentManager.deleteAgent(doomedAgent);
                entityManager.flush();
            }

            return toBeDeletedResourceIds;

        } catch (RuntimeException e) {
            if (doomedAgent != null) {
                // The most likely reason for a failure, although unlikely in itself, is that newly discovered resources
                // are currently being merged into the platform, and associated with the doomed agent.  In this case
                // the user must wait until the merge is complete.  Make sure the caller knows about this possibility.
                String msg = "Failed to uninventory platform. This can happen if new resources were actively being imported. Please wait and try again shortly.";
                throw new IllegalStateException(msg, (isDebugEnabled ? e : null));
            }

            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getResourceDescendantsByTypeAndName(Subject user, int resourceId, Integer resourceTypeId,
        String name) {
        Query descendantQuery = entityManager.createNamedQuery(Resource.QUERY_FIND_DESCENDANTS_BY_TYPE_AND_NAME);
        descendantQuery.setParameter("resourceId", resourceId);
        descendantQuery.setParameter("resourceTypeId", resourceTypeId);
        name = QueryUtility.formatSearchParameter(name);
        descendantQuery.setParameter("name", name);
        List<Integer> descendants = descendantQuery.getResultList();
        return descendants;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void uninventoryResourceAsyncWork(Subject user, int resourceId) {
        if (!authorizationManager.isOverlord(user)) {
            throw new IllegalArgumentException("Only the overlord can execute out-of-band async resource delete method");
        }

        /*
         * even though the group removal occurs in the in-band work, there can be some group definitions that just
         * happens to perform its recalculation (either manually or schedules) in the period after the in-band work
         * completes but before the async job triggers. since the ExpressionEvaluator that underlies the bulk of the
         * dynagroup query generations automatically adds a filter to only manipulate COMMITTED resource, this work
         * should be a no-op most of the time.  however, in rare circumstances it's possible for an InventoryReport to
         * come across the wire and flip the status of resources from UNINVENTORIED back to COMMITTED.  in this case,
         * this group removal logic needs to be executed again just prior to removing the rest of the reosurce history.
         */
        boolean hasErrors = uninventoryResourcesBulkDelete(user, Arrays.asList(resourceId));
        if (hasErrors) {
            return; // return early if there were any errors, because we can't remove the resource yet
        }

        hasErrors = uninventoryResourceBulkDeleteAsyncWork(user, resourceId);
        if (hasErrors) {
            return; // return early if there were any errors, because we can't remove the resource yet
        }

        Resource attachedResource = entityManager.find(Resource.class, resourceId);
        if (log.isDebugEnabled()) {
            log.debug("Overlord is asynchronously deleting resource [" + attachedResource + "]");
        }

        if (attachedResource != null) {
            // our unidirectional one-to-many mapping of drift definition makes it not possible to easily bulk delete drift definition
            // so remove them here and let cascading of delete_orphan do the work
            if (attachedResource.getDriftDefinitions() != null) {
                attachedResource.getDriftDefinitions().clear();
            }

            // one more thing, delete any autogroup backing groups
            List<ResourceGroup> backingGroups = attachedResource.getAutoGroupBackingGroups();
            if (null != backingGroups && !backingGroups.isEmpty()) {
                int size = backingGroups.size();
                int[] backingGroupIds = new int[size];
                for (int i = 0; (i < size); ++i) {
                    backingGroupIds[i] = backingGroups.get(i).getId();
                }
                try {
                    resourceGroupManager.deleteResourceGroups(user, backingGroupIds);
                } catch (Throwable t) {
                    if (log.isDebugEnabled()) {
                        log.error("Bulk delete error for autogroup backing group deletion for " + backingGroupIds, t);
                    } else {
                        log.error("Bulk delete error for autogroup backing group deletion for " + backingGroupIds
                            + ": " + t.getMessage());
                    }
                }
            }

            // now we can purge the resource, let cascading do the rest
            entityManager.remove(attachedResource);
        }
        return;
    }

    private boolean uninventoryResourcesBulkDelete(Subject overlord, List<Integer> resourceIds) {
        // Obtain group ids of affected groups, i.e. groups where resources act as the explicit resources.
        // The RHQ_RESOURCE_GROUP_RES_EXP_MAP table is used, because the resource type of a group is set to a
        // non null value iff the number of unique resource types of the member _EXPLICIT_ resources is equal to 1.
        // In other words, the implicit resources of a group have no impact on the group type (compatible/mixed).
        Query nativeQuery = entityManager.createNativeQuery(ResourceGroup.QUERY_GET_GROUP_IDS_BY_RESOURCE_IDS);
        nativeQuery.setParameter("resourceIds", resourceIds);
        // Note that different DB vendors return different types for IDs because the representation
        // is different at the storage layer. This is an em native query so we need to handle the differences.
        // Postgres will return an Integer, but Oracle returns a BigDecimal, etc.
        List<?> rs = nativeQuery.getResultList();

        String[] nativeQueriesToExecute = new String[] { //
        ResourceGroup.QUERY_DELETE_EXPLICIT_BY_RESOURCE_IDS, // unmap from explicit groups
            ResourceGroup.QUERY_DELETE_IMPLICIT_BY_RESOURCE_IDS // unmap from implicit groups
        };

        boolean hasErrors = false;
        for (String nativeQueryToExecute : nativeQueriesToExecute) {
            // execute all in new transactions, continuing on error, but recording whether errors occurred
            hasErrors |= resourceManager.bulkNativeQueryDeleteInNewTransaction(overlord, nativeQueryToExecute,
                resourceIds);
        }

        // update the resource type of affected groups by calling setResouceType()
        DatabaseType dbType = DatabaseTypeFactory.getDefaultDatabaseType();
        Integer groupId = null;
        for (int i = 0, size = rs.size(); i < size; ++i) {
            try {
                groupId = dbType.getInteger(rs.get(i));
                resourceGroupManager.setResourceType(groupId);

            } catch (ResourceGroupDeleteException rgde) {
                log.warn("Unable to change resource type for group with id [" + groupId + "]", rgde);
            }
        }

        return hasErrors;
    }

    private boolean uninventoryResourceBulkDeleteAsyncWork(Subject overlord, int resourceId) {
        String[] namedQueriesToExecute = new String[] { //
        StorageNode.QUERY_UPDATE_REMOVE_LINKED_RESOURCES, //remove storage node resource links
            ResourceRepo.DELETE_BY_RESOURCES, //
            MeasurementBaseline.QUERY_DELETE_BY_RESOURCES, // baseline BEFORE schedules
            MeasurementDataTrait.QUERY_DELETE_BY_RESOURCES, // traits BEFORE schedules
            CallTimeDataValue.QUERY_DELETE_BY_RESOURCES, // call time data values BEFORE schedules & call time data keys
            CallTimeDataKey.QUERY_DELETE_BY_RESOURCES, // call time data keys BEFORE schedules
            MeasurementOOB.DELETE_FOR_RESOURCES, //
            MeasurementSchedule.DELETE_BY_RESOURCES, // schedules AFTER baselines, traits, and calltime data
            Availability.QUERY_DELETE_BY_RESOURCES, //
            ResourceError.QUERY_DELETE_BY_RESOURCES, //
            Event.DELETE_BY_RESOURCES, //
            EventSource.QUERY_DELETE_BY_RESOURCES, //
            BundleResourceDeploymentHistory.QUERY_DELETE_BY_RESOURCES, // resource deployment history BEFORE resource deployment
            BundleResourceDeployment.QUERY_DELETE_BY_RESOURCES, //
            PackageInstallationStep.QUERY_DELETE_BY_RESOURCES, // steps BEFORE installed package history
            InstalledPackageHistory.QUERY_DELETE_BY_RESOURCES, // history BEFORE installed packages & content service requests
            InstalledPackage.QUERY_DELETE_BY_RESOURCES, //
            ContentServiceRequest.QUERY_DELETE_BY_RESOURCES, //
            ResourceOperationScheduleEntity.QUERY_DELETE_BY_RESOURCES, //
            ResourceOperationHistory.QUERY_DELETE_BY_RESOURCES, //
            DeleteResourceHistory.QUERY_DELETE_BY_RESOURCES, //
            CreateResourceHistory.QUERY_DELETE_BY_RESOURCES, //
            ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_0, // orphan parent list or maps (execute only on non selfRefCascade dbs)
            ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_1, // first, delete the raw configs for the config
            ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_2, // then delete the config objects
            ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_3, // then the history objects wrapping those configs
            PluginConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_0, // orphan parent list or maps (execute only on non selfRefCascade dbs)
            PluginConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_1, // first, delete the raw configs for the config
            PluginConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_2, // then delete the config objects
            PluginConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_3, // then the history objects wrapping those configs
            AlertConditionLog.QUERY_DELETE_BY_RESOURCES, //             Don't
            AlertConditionLog.QUERY_DELETE_BY_RESOURCES_BULK_DELETE, // alter
            AlertNotificationLog.QUERY_DELETE_BY_RESOURCES, //          the
            Alert.QUERY_DELETE_BY_RESOURCES, //                         order
            AlertCondition.QUERY_DELETE_BY_RESOURCES, //                of
            AlertDampeningEvent.QUERY_DELETE_BY_RESOURCES, //           alert-
            AlertNotification.QUERY_DELETE_BY_RESOURCES, //             related
            AlertDefinition.QUERY_DELETE_BY_RESOURCES, //               deletes
            JPADrift.QUERY_DELETE_BY_RESOURCES, //       drift before changeset
            JPADriftChangeSet.QUERY_DELETE_BY_RESOURCES };

        List<Integer> resourceIds = new ArrayList<Integer>();
        resourceIds.add(resourceId);
        boolean supportsCascade = DatabaseTypeFactory.getDefaultDatabaseType().supportsSelfReferringCascade();

        boolean hasErrors = false;
        boolean debugEnabled = log.isDebugEnabled();
        for (String namedQueryToExecute : namedQueriesToExecute) {
            // execute all in new transactions, continuing on error, but recording whether errors occurred

            // If the db vendor can not support our self-referring cascade delete data model then we may have
            // to leave some config prop rows orphaned. Only execute the selected queries if you *do*
            // want to avoid self-referring cascade delete (and leave orphans)
            if (supportsCascade && ( //
                namedQueryToExecute.equals(ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_0) || //
                namedQueryToExecute.equals(PluginConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_0))) {
                continue;
            }

            if (debugEnabled) {
                log.debug("uninv, running query: " + namedQueryToExecute);
            }
            hasErrors |= resourceManager.bulkNamedQueryDeleteInNewTransaction(overlord, namedQueryToExecute,
                resourceIds);
        }

        return hasErrors;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean bulkNativeQueryDeleteInNewTransaction(Subject subject, String nativeQueryString,
        List<Integer> resourceIds) {
        if (!authorizationManager.isOverlord(subject)) {
            throw new IllegalArgumentException("Only the overlord can execute arbitrary native query strings");
        }

        try {
            Query nativeQuery = entityManager.createNativeQuery(nativeQueryString);
            nativeQuery.setParameter("resourceIds", resourceIds);
            nativeQuery.executeUpdate();
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.error("Bulk native query delete error for '" + nativeQueryString + "' for " + resourceIds, t);
            } else {
                log.error("Bulk native query delete error for '" + nativeQueryString + "' for " + resourceIds + ": "
                    + t.getMessage());
            }
            return true; // had errors
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean bulkNamedQueryDeleteInNewTransaction(Subject subject, String namedQuery, List<Integer> resourceIds) {
        if (!authorizationManager.isOverlord(subject)) {
            throw new IllegalArgumentException("Only the overlord can execute arbitrary named query strings");
        }

        try {
            Query nativeQuery = entityManager.createNamedQuery(namedQuery);
            nativeQuery.setParameter("resourceIds", resourceIds);
            nativeQuery.executeUpdate();
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.error("Bulk named query delete error for '" + namedQuery + "' for " + resourceIds, t);
            } else {
                log.error("Bulk named query delete error for '" + namedQuery + "' for " + resourceIds + ": "
                    + t.getMessage());
            }
            return true; // had errors
        }
        return false;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Resource setResourceStatus(Subject user, Resource resource, InventoryStatus newStatus, boolean setDescendents) {
        // do special processing if we are being asked to commit the resource to inventory
        if (newStatus == InventoryStatus.COMMITTED) {
            if ((resource.getParentResource() != null)
                && (resource.getParentResource().getInventoryStatus() != InventoryStatus.COMMITTED)) {
                throw new IllegalStateException("Cannot commit resource [" + resource
                    + "] to inventory, because its parent resource [" + resource.getParentResource()
                    + "] has not yet been committed.");
            }

            if ((resource.getResourceType() == null) || (resource.getResourceType().isIgnored())) {
                log.debug("Not commiting resource [" + resource + "] to inventory because its type is ignored");
                return resource;
            }
        }

        long now = System.currentTimeMillis();
        updateInventoryStatus(resource, newStatus, now);
        resource = entityManager.merge(resource);

        if (setDescendents) {
            for (Resource childResource : resource.getChildResources()) {
                updateInventoryStatus(childResource, newStatus, now);
                childResource = entityManager.merge(childResource);
                setResourceStatus(user, childResource, newStatus, setDescendents);
            }
        } else if (resource.getResourceType().getCategory() == ResourceCategory.PLATFORM) {
            // Commit platform services when the platform is committed.
            for (Resource childResource : resource.getChildResources()) {
                if (childResource.getResourceType().getCategory() == ResourceCategory.SERVICE) {
                    updateInventoryStatus(childResource, newStatus, now);
                    childResource = entityManager.merge(childResource);
                    setResourceStatus(user, childResource, newStatus, setDescendents);
                }
            }
        }

        return resource;
    }

    private void updateInventoryStatus(Resource resource, InventoryStatus newStatus, long now) {
        resource.setInventoryStatus(newStatus);
        resource.setItime(now);
        resource.setAgentSynchronizationNeeded();
    }

    @SuppressWarnings("unchecked")
    public Resource getResourceById(Subject user, int resourceId) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_ID);
        query.setParameter("resourceId", resourceId);
        List<Resource> resources = query.getResultList();

        if (resources.size() != 1) {
            throw new ResourceNotFoundException(resourceId);
        }

        if (!authorizationManager.canViewResource(user, resourceId)) {
            throw new PermissionException("User [" + user + "] does not have permission to view resource ["
                + resourceId + "]");
        }

        return resources.get(0);
    }

    @Nullable
    public Resource getResourceByParentAndKey(Subject user, Resource parent, String key, String plugin, String typeName) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_PARENT_AND_KEY);
        query.setParameter("parent", parent);
        query.setParameter("key", key);
        query.setParameter("plugin", plugin);
        query.setParameter("typeName", typeName);
        Resource resource;
        try {
            resource = (Resource) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }

        if (!authorizationManager.canViewResource(user, resource.getId())) {
            throw new PermissionException("You do not have permission to get this resource by parent and key.");
        }

        return resource;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceWithAvailability> findResourcesByParentAndType(Subject user, Resource parent, ResourceType type) {

        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_PARENT_AND_TYPE_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_PARENT_AND_TYPE);
            query.setParameter("subject", user);
        }

        query.setParameter("parent", parent);
        query.setParameter("type", type);
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        // We are not doing a query with constructor here, as this would fire a select per
        // resource and row. So we need to construct the ResourceWithAvailability objects ourselves.
        List<Object[]> objs = query.getResultList();
        List<ResourceWithAvailability> results = new ArrayList<ResourceWithAvailability>(objs.size());
        for (Object[] ob : objs) {
            Resource r = (Resource) ob[0];
            AvailabilityType at = (AvailabilityType) ob[1];
            ResourceWithAvailability rwa = new ResourceWithAvailability(r, at);
            results.add(rwa);
        }

        return results;
    }

    @Nullable
    public Resource getParentResource(int resourceId) {

        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException(resourceId);
        }

        Resource parent = resource.getParentResource();
        if (parent != null) {
            parent.getId(); // Important: This ensures Hibernate actually populates parent's fields.
        }

        return parent;
    }

    @Nullable
    private Integer getParentResourceId(int resourceId) {

        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_PARENT_ID);
        query.setParameter("id", resourceId);

        try {
            return (Integer) query.getSingleResult();
        } catch (NoResultException nre) {
            // this is OK, no parent means this is a platform
            return null;
        }
    }

    // lineage is a getXXX (not findXXX) because it logically returns a single object, but modeled as a list here
    public List<Integer> getResourceIdLineage(int resourceId) {
        List<Integer> lineage = new ArrayList<Integer>();

        Integer child = resourceId;
        Integer parent = null;
        while ((parent = getParentResourceId(child)) != null) {
            lineage.add(parent);
            child = parent;
        }

        return lineage;
    }

    // lineage is a getXXX (not findXXX) because it logically returns a single object, but modeled as a list here
    public List<Resource> getResourceLineage(int resourceId) {
        LinkedList<Resource> resourceLineage = new LinkedList<Resource>();
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException(resourceId);
        }

        resourceLineage.add(resource);
        int childResourceId = resourceId;
        Resource parent;
        while ((parent = getParentResource(childResourceId)) != null) {
            resourceLineage.addFirst(parent);
            childResourceId = parent.getId(); // This also ensures Hibernate actually populates parent's fields.
        }

        return resourceLineage;
    }

    public List<ResourceLineageComposite> getResourceLineageAndSiblings(Subject subject, int resourceId) {
        // Get the raw resource lineage up to the platform. We'll check the auth below.
        List<Resource> rawResourceLineage = getResourceLineage(resourceId);
        int depth = rawResourceLineage.size();
        Resource parent = (depth > 1) ? rawResourceLineage.get(depth - 2) : null;

        // Build up a list of composite Resources for the ancestry that includes which ancestors, if any, should be
        // locked from view.
        boolean isInventoryManager = authorizationManager.isInventoryManager(subject);
        List<ResourceLineageComposite> resourceLineage = new ArrayList<ResourceLineageComposite>(
            rawResourceLineage.size());
        for (Resource resource : rawResourceLineage) {
            boolean isLocked = !(isInventoryManager || authorizationManager.canViewResource(subject, resource.getId()));
            ResourceLineageComposite composite = new ResourceLineageComposite(resource, isLocked);
            resourceLineage.add(composite);
        }

        // Build up the result list, including only the direct ancestors of the Resource and all viewable siblings of
        // the ancestors. The list will represent a tree, rooted at the platform, in depth-first order.
        List<ResourceLineageComposite> result = new LinkedList<ResourceLineageComposite>();

        for (ResourceLineageComposite ancestor : resourceLineage) {
            // Always include a direct ancestor.
            result.add(ancestor);

            // If the ancestor is not locked, include viewable children.
            if (!ancestor.isLocked() || ancestor.getResource() == parent) {
                // Get viewable committed children, but bounded to ensure it's not an overwhelming return set
                ResourceCriteria criteria = new ResourceCriteria();
                criteria.addFilterParentResourceId(ancestor.getResource().getId());
                criteria.addSortName(PageOrdering.ASC);
                criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

                List<Resource> children = findResourcesByCriteriaBounded(subject, criteria, 0, 0);
                // Remove any that are in the lineage to avoid repeated handling.
                children.removeAll(rawResourceLineage);
                for (Resource child : children) {
                    // Ensure the parentResource field is fetched. (do this here and not via criteria.fetchParentResource
                    // because that option would require inventory manager perm)
                    child.getParentResource().getId();
                    // The query only returned viewable children, so the composite should not be locked.
                    boolean isLocked = false;
                    ResourceLineageComposite composite = new ResourceLineageComposite(child, isLocked);
                    result.add(composite);
                }
            }
        }

        return result;
    }

    public List<ResourceLineageComposite> getResourceLineage(Subject subject, int resourceId) {
        boolean isInventoryManager = authorizationManager.isInventoryManager(subject);

        // get the raw resource lineage up to the platform. We'll check the auth below
        List<Resource> rawLineage = getResourceLineage(resourceId);

        // record which of the raw ancestry is locked from view
        List<ResourceLineageComposite> resourceLineage = new ArrayList<ResourceLineageComposite>(rawLineage.size());
        for (Resource resource : rawLineage) {
            boolean isLocked = false;
            if (!isInventoryManager) {
                isLocked = !authorizationManager.canViewResource(subject, resource.getId());
            }
            resourceLineage.add(new ResourceLineageComposite(resource, isLocked));
        }

        return resourceLineage;
    }

    public Map<Integer, String> getResourcesAncestry(Subject subject, Integer[] resourceIds,
        ResourceAncestryFormat format) {
        Map<Integer, String> result = new HashMap<Integer, String>(resourceIds.length);

        if (resourceIds.length == 0) {
            return result;
        }

        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterIds(resourceIds);
        resourceCriteria.fetchResourceType(true);
        resourceCriteria.clearPaging();//disable paging as the code assumes all the results will be returned.
        List<Resource> resources = findResourcesByCriteria(subject, resourceCriteria);

        if (ResourceAncestryFormat.RAW == format) {
            for (Resource resource : resources) {
                result.put(resource.getId(), resource.getAncestry());
            }
            return result;
        }

        HashSet<Integer> typesSet = new HashSet<Integer>();
        HashSet<String> ancestries = new HashSet<String>();
        for (Resource resource : resources) {
            ResourceType type = resource.getResourceType();
            if (type != null) {
                typesSet.add(type.getId());
            }
            ancestries.add(resource.getAncestry());
        }

        // In addition to the types of the result resources, get the types of their ancestry
        typesSet.addAll(getAncestryTypeIds(ancestries));

        ResourceTypeCriteria resourceTypeCriteria = new ResourceTypeCriteria();
        resourceTypeCriteria.addFilterIds(typesSet.toArray(new Integer[typesSet.size()]));
        resourceTypeCriteria.addFilterIgnored(null); // don't worry if they are ignored or not, get the ancestry anyway
        List<ResourceType> types = typeManager.findResourceTypesByCriteria(subject, resourceTypeCriteria);

        for (Resource resource : resources) {
            String decodedAncestry = getDecodedAncestry(resource, types, format);
            result.put(resource.getId(), decodedAncestry);
        }
        return result;
    }

    /**
     * Get the complete set of resource type Ids in the ancestries provided. This is useful for
     * being able to load all the types in advance of generating decoded values.
     *
     * @return
     */
    private HashSet<Integer> getAncestryTypeIds(Collection<String> ancestries) {
        HashSet<Integer> result = new HashSet<Integer>();

        for (String ancestry : ancestries) {
            if (null == ancestry) {
                continue;
            }
            String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
            for (int i = 0; i < ancestryEntries.length; ++i) {
                String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
                int rtId = Integer.valueOf(entryTokens[0]);
                result.add(rtId);
            }
        }

        return result;
    }

    private String getDecodedAncestry(Resource resource, List<ResourceType> typeList, ResourceAncestryFormat format) {

        String ancestry = resource.getAncestry();
        StringBuilder sb = new StringBuilder("");

        if (ResourceAncestryFormat.VERBOSE != format) {

            if (ResourceAncestryFormat.EXTENDED == format) {
                sb.append(resource.getName());
            }

            if (null != ancestry) {
                if (ResourceAncestryFormat.EXTENDED == format) {
                    sb.append(" < ");
                }

                String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
                for (int i = 0; i < ancestryEntries.length; ++i) {
                    String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
                    String ancestorName = entryTokens[2];

                    sb.append((i > 0) ? " < " : "");
                    sb.append(ancestorName);
                }
            }
        } else {

            Map<Integer, ResourceType> types = new HashMap<Integer, ResourceType>(typeList.size());
            for (ResourceType type : typeList) {
                types.put(type.getId(), type);
            }

            ResourceType type = types.get(resource.getResourceType().getId());
            String resourceLongName = getResourceLongName(resource.getName(), type);

            // decode ancestry
            if (null != ancestry) {
                String[] ancestryEntries = ancestry.split(Resource.ANCESTRY_DELIM);
                for (int i = ancestryEntries.length - 1, j = 0; i >= 0; --i, ++j) {
                    String[] entryTokens = ancestryEntries[i].split(Resource.ANCESTRY_ENTRY_DELIM);
                    int ancestorTypeId = Integer.valueOf(entryTokens[0]);
                    String ancestorName = entryTokens[2];

                    // indent with spaces
                    if (j > 0) {
                        sb.append("\n");
                        for (int k = 0; k < j; ++k) {
                            sb.append("  ");
                        }
                    }
                    type = types.get(ancestorTypeId);
                    sb.append(getResourceLongName(ancestorName, type));
                }

                // add target resource, indent with spaces
                sb.append("\n");
                for (int k = 0; k <= ancestryEntries.length; ++k) {
                    sb.append("  ");
                }
                sb.append(resourceLongName);

            } else {
                // just show the resource name/type info
                sb.append(resourceLongName);
            }
        }

        return sb.toString();
    }

    private String getResourceLongName(String resourceName, ResourceType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(resourceName);
        sb.append(" [");
        sb.append(type.getPlugin());
        sb.append(", ");
        sb.append(type.getName());
        sb.append("]");

        return sb.toString();
    }

    @NotNull
    public Resource getRootResourceForResource(int resourceId) {
        Query q = entityManager.createNamedQuery(Resource.QUERY_FIND_ROOT_PLATFORM_OF_RESOURCE);
        q.setParameter("resourceId", resourceId);

        return (Resource) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findResourceByParentAndInventoryStatus(Subject user, Resource parent,
        InventoryStatus inventoryStatus, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS);
            queryCount.setParameter("subject", user);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_PARENT_AND_INVENTORY_STATUS, pageControl);
            query.setParameter("subject", user);
        }

        queryCount.setParameter("parent", parent);
        queryCount.setParameter("inventoryStatus", inventoryStatus);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("parent", parent);
        query.setParameter("inventoryStatus", inventoryStatus);
        List<Resource> results = query.getResultList();
        return new PageList<Resource>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findChildResources(Subject user, Resource parent, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_CHILDREN_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_CHILDREN_ADMIN,
                pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_CHILDREN);
            queryCount.setParameter("subject", user);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_CHILDREN, pageControl);
            query.setParameter("subject", user);
        }

        queryCount.setParameter("parent", parent);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("parent", parent);
        List<Resource> results = query.getResultList();
        return new PageList<Resource>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findChildrenResourceIds(int parentResourceId, InventoryStatus status) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_IDS_ADMIN);
        query.setParameter("parentResourceId", parentResourceId);
        query.setParameter("inventoryStatus", status);

        List<Integer> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findChildResourcesByCategoryAndInventoryStatus(Subject user, Resource parent, @Nullable
    ResourceCategory category, InventoryStatus status, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS);
            queryCount.setParameter("subject", user);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_CHILDREN_BY_CATEGORY_AND_INVENTORY_STATUS, pageControl);
            query.setParameter("subject", user);
        }

        queryCount.setParameter("parent", parent);
        queryCount.setParameter("category", category);
        queryCount.setParameter("status", status);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("parent", parent);
        query.setParameter("category", category);
        query.setParameter("status", status);
        List<Resource> results = query.getResultList();
        return new PageList<Resource>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findResourcesByCategory(Subject user, ResourceCategory category,
        InventoryStatus inventoryStatus, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS, pageControl);

            queryCount.setParameter("subject", user);
            query.setParameter("subject", user);
        }

        queryCount.setParameter("category", category);
        query.setParameter("category", category);

        queryCount.setParameter("inventoryStatus", inventoryStatus);
        query.setParameter("inventoryStatus", inventoryStatus);

        long count = (Long) queryCount.getSingleResult();

        List<Resource> results = query.getResultList();
        return new PageList<Resource>(results, (int) count, pageControl);
    }

    /**
     * This finder query can be used to find resources with various combinations of attributes in their composite form.
     * Except for the user parameter, the other parameters can be left null so that the query will not filter by that
     * attribute.
     *
     * @param  user
     * @param  category       Limit the search to a given {@link ResourceCategory}
     * @param  typeName       Limit the search to to {@link ResourceType}(s) with the given name
     * @param  pluginName     Limit the search to the plugin with the given name
     * @param  parentResource Limit the search to children of a given parent resource
     * @param  searchString
     * @param  pageControl
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public PageList<ResourceComposite> findResourceComposites(Subject user, ResourceCategory category, String typeName,
        String pluginName, Resource parentResource, String searchString, boolean attachParentResource,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");
        pageControl.addDefaultOrderingField("res.id");

        String queryName;
        String queryCountName;

        if (authorizationManager.isInventoryManager(user)) {
            if (attachParentResource) {
                queryName = Resource.QUERY_FIND_COMPOSITE_WITH_PARENT_ADMIN;
            } else {
                queryName = Resource.QUERY_FIND_COMPOSITE_ADMIN;
            }
            queryCountName = Resource.QUERY_FIND_COMPOSITE_COUNT_ADMIN;
        } else {
            if (attachParentResource) {
                queryName = Resource.QUERY_FIND_COMPOSITE_WITH_PARENT;
            } else {
                queryName = Resource.QUERY_FIND_COMPOSITE;
            }
            queryCountName = Resource.QUERY_FIND_COMPOSITE_COUNT;
        }

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryCountName);

        if (authorizationManager.isInventoryManager(user) == false) {
            queryCount.setParameter("subject", user);
            query.setParameter("subject", user);
        }

        searchString = QueryUtility.formatSearchParameter(searchString);

        query.setParameter("category", category);
        queryCount.setParameter("category", category);
        query.setParameter("resourceTypeName", typeName);
        queryCount.setParameter("resourceTypeName", typeName);
        query.setParameter("pluginName", pluginName);
        queryCount.setParameter("pluginName", pluginName);
        query.setParameter("search", searchString);
        queryCount.setParameter("search", searchString);
        query.setParameter("escapeChar", QueryUtility.getEscapeCharacter());
        queryCount.setParameter("escapeChar", QueryUtility.getEscapeCharacter());
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        queryCount.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        query.setParameter("parentResource", parentResource);
        queryCount.setParameter("parentResource", parentResource);

        long count = (Long) queryCount.getSingleResult();

        return new PageList<ResourceComposite>(query.getResultList(), (int) count, pageControl);
    }

    /**
     * Get a Resource Composite for Resources limited by the given parameters
     *
     * @param  user           User executing the query
     * @param  category       Category this query should be limited to
     * @param  resourceTypeId the PK of the desired resource type or -1 if no limit
     * @param  parentResource the desired parent resource or null if no limit
     * @param  pageControl
     *
     * @return
     */
    public PageList<ResourceComposite> findResourceCompositeForParentAndTypeAndCategory(Subject user,
        ResourceCategory category, int resourceTypeId, Resource parentResource, PageControl pageControl) {
        // pageControl.initDefaultOrderingField(); // not needed since findResourceComposites will set it

        ResourceType resourceType = null;
        if (resourceTypeId != -1) {
            try {
                resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            } catch (NoResultException nre) {
                // No problem
            }
        }

        String typeNameFilter = (resourceType == null) ? null : resourceType.getName();
        String pluginNameFilter = (resourceType == null) ? null : resourceType.getPlugin();

        return findResourceComposites(user, category, typeNameFilter, pluginNameFilter, parentResource, null, false,
            pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findResourcesByType(Subject user, ResourceType resourceType, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_TYPE_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_BY_TYPE_ADMIN,
                pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_TYPE);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_BY_TYPE, pageControl);
            queryCount.setParameter("subject", user);
            query.setParameter("subject", user);
        }

        queryCount.setParameter("type", resourceType);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("type", resourceType);
        List<Resource> results = query.getResultList();
        return new PageList<Resource>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public int[] getResourceCountSummary(Subject user, InventoryStatus status) {
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_RESOURCE_SUMMARY_BY_INVENTORY_STATUS_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_RESOURCE_SUMMARY_BY_INVENTORY_STATUS);
            query.setParameter("subject", user);
        }

        query.setParameter("inventoryStatus", status);

        int[] counts = new int[3];
        List<Object[]> resultList = query.getResultList();

        for (Object[] row : resultList) {
            switch ((ResourceCategory) row[0]) {
            case PLATFORM:
                counts[0] = ((Long) row[1]).intValue();
                break;
            case SERVER:
                counts[1] = ((Long) row[1]).intValue();
                break;
            case SERVICE:
                counts[2] = ((Long) row[1]).intValue();
                break;
            }
        }

        return counts;
    }

    public int getResourceCountByCategory(Subject user, ResourceCategory category, InventoryStatus status) {
        Query queryCount;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS_ADMIN);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_CATEGORY_AND_INVENTORY_STATUS);
            queryCount.setParameter("subject", user);
        }

        queryCount.setParameter("inventoryStatus", status);
        queryCount.setParameter("category", category);

        long count = (Long) queryCount.getSingleResult();

        return (int) count;
    }

    public int getResourceCountByTypeAndIds(Subject user, ResourceType type, int[] resourceIds) {
        Query queryCount;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_TYPE_AND_IDS_ADMIN);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_TYPE_AND_IDS);
            queryCount.setParameter("subject", user);
        }

        List<Integer> resourceList = ArrayUtils.wrapInList(resourceIds);
        queryCount.setParameter("ids", resourceList);

        queryCount.setParameter("type", type);

        long count = (Long) queryCount.getSingleResult();

        return (int) count;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findResourcesMarkedForAsyncDeletion(Subject user) {
        if (!authorizationManager.isOverlord(user)) {
            throw new IllegalArgumentException("Only the overlord can purge resources marked for deletion");
        }

        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_RESOURCES_MARKED_FOR_ASYNC_DELETION);
        List<Integer> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    public List<RecentlyAddedResourceComposite> findRecentlyAddedPlatforms(Subject user, long ctime, int maxItems) {
        Query query;

        if (authorizationManager.isInventoryManager(user)) {
            query = entityManager.createNamedQuery(Resource.QUERY_RECENTLY_ADDED_PLATFORMS_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_RECENTLY_ADDED_PLATFORMS);
            query.setParameter("subject", user);
        }

        query.setParameter("oldestEpochTime", ctime);
        if ((maxItems > 100) || (maxItems < 0)) {//cap infininte(-1) and large requests to 100
            query.setMaxResults(100); // this query is only used by the dashboard portlet, let's not blow it up
        } else {
            query.setMaxResults(maxItems);
        }
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<RecentlyAddedResourceComposite> findRecentlyAddedServers(Subject user, long ctime, int platformId) {
        Query query;

        if (authorizationManager.isInventoryManager(user)) {
            query = entityManager.createNamedQuery(Resource.QUERY_RECENTLY_ADDED_SERVERS_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_RECENTLY_ADDED_SERVERS);
            query.setParameter("subject", user);
        }

        query.setParameter("oldestEpochTime", ctime);
        query.setParameter("platformId", platformId);
        query.setMaxResults(100); // this query is only used by the dashboard portlet, let's not blow it up
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public AutoGroupComposite getResourceAutoGroup(Subject user, int resourceId) {
        Query query;
        boolean isInventoryManager = authorizationManager.isInventoryManager(user);
        if (isInventoryManager) {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_RESOURCE_AUTOGROUP_COMPOSITE_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_RESOURCE_AUTOGROUP_COMPOSITE);
            query.setParameter("subject", user);
        }

        query.setParameter("id", resourceId);

        AutoGroupComposite result;
        try {
            result = (AutoGroupComposite) query.getSingleResult();
        } catch (NoResultException nore) {
            return null; // Better throw a PermissionException ?
        }

        if (isInventoryManager) {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_ID_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_ID);
            query.setParameter("subject", user);
        }

        query.setParameter("id", resourceId);
        result.setResources(query.getResultList()); // query result is a List<ResourceWithAvailability>

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<AutoGroupComposite> findResourcesAutoGroups(Subject subject, int[] resourceIds) {
        List<AutoGroupComposite> results = new ArrayList<AutoGroupComposite>();
        List<Integer> ids = ArrayUtils.wrapInList(resourceIds);
        if ((ids == null) || (ids.size() == 0)) {
            return results;
        }

        boolean isInventoryManager = authorizationManager.isInventoryManager(subject);
        Query query;
        if (isInventoryManager) {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_RESOURCE_AUTOGROUPS_COMPOSITE_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_RESOURCE_AUTOGROUPS_COMPOSITE);
            query.setParameter("subject", subject);
        }

        query.setParameter("ids", ids);

        AutoGroupComposite oneComp;
        try {
            oneComp = (AutoGroupComposite) query.getSingleResult();
        } catch (NoResultException nre) {
            return null; // better throw a PermissionException ?
        }

        if (isInventoryManager) {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_IDS_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_AVAILABILITY_BY_RESOURCE_IDS);
            query.setParameter("subject", subject);
        }

        query.setParameter("ids", ids);

        // We are not doing a query with constructor here, as this would fire a select per
        // resource and row. So we need to construct the ResourceWithAvailability objects ourselves.
        List<Object[]> objs = query.getResultList();
        for (Object[] ob : objs) {
            Resource r = (Resource) ob[0];
            AvailabilityType at = (AvailabilityType) ob[1];
            ResourceWithAvailability rwa = new ResourceWithAvailability(r, at);

            AutoGroupComposite comp = new AutoGroupComposite(oneComp);
            List res = new ArrayList(1); // hack to get around type safety
            res.add(rwa);
            comp.setResources(res);
            results.add(comp);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public List<AutoGroupComposite> findChildrenAutoGroups(Subject user, int parentResourceId, int[] resourceTypeIds) {
        Query query;

        List<Integer> typeIds = ArrayUtils.wrapInList(resourceTypeIds);

        if (null != typeIds) {
            if (authorizationManager.isInventoryManager(user)) {
                query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_BY_TYPE_ADMIN);
            } else {
                query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_BY_TYPE);
                query.setParameter("subject", user);
            }
            query.setParameter("resourceTypeIds", typeIds);
        } else {
            if (authorizationManager.isInventoryManager(user)) {
                query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_ADMIN);
            } else {
                query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES);
                query.setParameter("subject", user);
            }
        }

        Resource parentResource = entityManager.getReference(Resource.class, parentResourceId);
        query.setParameter("parent", parentResource);
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        List<AutoGroupComposite> resourceAutoGroups = query.getResultList();

        for (AutoGroupComposite composite : resourceAutoGroups) {
            ResourceSubCategory sc = composite.getResourceType().getSubCategory();
            if (sc != null) {
                sc.getId();
            }

            if (authorizationManager.isInventoryManager(user)) {
                query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_PARENT_AND_TYPE_ADMIN);
            } else {
                query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_PARENT_AND_TYPE);
                query.setParameter("subject", user);
            }

            query.setParameter("parent", parentResource);
            query.setParameter("type", composite.getResourceType());
            query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

            List<Object[]> objs = query.getResultList();
            @SuppressWarnings("rawtypes")
            List results = new ArrayList<ResourceWithAvailability>(objs.size());
            for (Object[] ob : objs) {
                Resource r = (Resource) ob[0];
                AvailabilityType at = (AvailabilityType) ob[1];
                ResourceWithAvailability rwa = new ResourceWithAvailability(r, at);
                results.add(rwa);
            }
            composite.setResources(results);
        }

        List<AutoGroupComposite> fullComposites = new ArrayList<AutoGroupComposite>();

        calculateSubcategorySummary(parentResource, parentResource.getResourceType().getChildSubCategories(),
            resourceAutoGroups, 0, fullComposites);
        fullComposites.addAll(resourceAutoGroups);

        return fullComposites;
    }

    public List<AutoGroupComposite> findChildrenAutoGroups(Subject user, int parentResourceId) {
        return findChildrenAutoGroups(user, parentResourceId, (int[]) null);
    }

    private void calculateSubcategorySummary(Resource parentResource, List<ResourceSubCategory> subcategories,
        List<AutoGroupComposite> resourceAutoGroups, int depth, List<AutoGroupComposite> fullComposites) {

        for (ResourceSubCategory subCategory : subcategories) {
            List<AutoGroupComposite> matches = new ArrayList<AutoGroupComposite>();
            for (AutoGroupComposite ac : resourceAutoGroups) {
                ResourceSubCategory searchCategory = ac.getResourceType().getSubCategory();

                while (searchCategory != null) {
                    if (subCategory.equals(searchCategory)) {
                        matches.add(ac);
                    }

                    searchCategory = searchCategory.getParentSubCategory();
                }
            }

            if (matches.size() > 0) {
                int count = 0;
                double avail = 0;
                for (AutoGroupComposite mac : matches) {
                    count += mac.getMemberCount();
                    avail += ((mac.getAvailability() == null) ? 0d : mac.getAvailability()) * mac.getMemberCount();
                }

                avail = avail / count;

                AutoGroupComposite categoryComposite = new AutoGroupComposite(avail, parentResource, subCategory, count);
                categoryComposite.setDepth(depth);
                fullComposites.add(categoryComposite);
            }

            if (subCategory.getChildSubCategories() != null) {
                calculateSubcategorySummary(parentResource, subCategory.getChildSubCategories(), resourceAutoGroups,
                    depth + 1, fullComposites);
            }

            // We matched all descendants above, but only list children directly as the child sub categories will already
            // be listed above and will show matches as necessary
            for (AutoGroupComposite match : matches) {
                if (match.getResourceType().getSubCategory().equals(subCategory)) {
                    match.setDepth(depth + 1);
                    fullComposites.add(match);
                    resourceAutoGroups.remove(match);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findExplicitResourcesByResourceGroup(Subject user, ResourceGroup group,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP);
            queryCount.setParameter("subject", user);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_EXPLICIT_RESOURCE_GROUP, pageControl);
            query.setParameter("subject", user);
        }

        queryCount.setParameter("group", group);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("group", group);
        List<Resource> results = query.getResultList();
        return new PageList<Resource>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findExplicitResourceIdsByResourceGroup(int resourceGroupId) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_EXPLICIT_IDS_BY_RESOURCE_GROUP_ADMIN);
        query.setParameter("groupId", resourceGroupId);

        List<Integer> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findImplicitResourceIdsByResourceGroup(int resourceGroupId) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_IMPLICIT_IDS_BY_RESOURCE_GROUP_ADMIN);
        query.setParameter("groupId", resourceGroupId);

        List<Integer> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceIdFlyWeight> findFlyWeights(int[] resourceIds) {
        Integer[] ids = ArrayUtils.wrapInArray(resourceIds);
        if (ids.length == 0) {
            return new ArrayList<ResourceIdFlyWeight>();
        }

        List<ResourceIdFlyWeight> results = new ArrayList<ResourceIdFlyWeight>();

        Arrays.sort(ids); // likely that ids in close proximity are co-located physically (data block-wise)
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_FLY_WEIGHTS_BY_RESOURCE_IDS);
        for (int i = 0; i < ids.length; i += 1000) {
            Integer[] batchRange = ArrayUtils.copyOfRange(ids, i, i + 1000);
            query.setParameter("resourceIds", Arrays.asList(batchRange));
            List<ResourceIdFlyWeight> batchResults = query.getResultList();
            results.addAll(batchResults);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findImplicitResourcesByResourceGroup(Subject user, ResourceGroup group,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP);
            queryCount.setParameter("subject", user);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_BY_IMPLICIT_RESOURCE_GROUP, pageControl);
            query.setParameter("subject", user);
        }

        queryCount.setParameter("group", group);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("group", group);
        List<Resource> results = query.getResultList();
        return new PageList<Resource>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    // RHQ-796, queries now return the parent resource attached
    public PageList<ResourceWithAvailability> findExplicitResourceWithAvailabilityByResourceGroup(Subject subject,
        ResourceGroup group, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT);
            queryCount.setParameter("subject", subject);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP, pageControl);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("groupId", group.getId());
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("groupId", group.getId());

        List<ResourceWithAvailability> results = query.getResultList();
        return new PageList<ResourceWithAvailability>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    // RHQ-796, queries now return the parent resource attached
    public PageList<ResourceWithAvailability> findImplicitResourceWithAvailabilityByResourceGroup(Subject subject,
        ResourceGroup group, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager
                .createNamedQuery(Resource.QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN, pageControl);
        } else {
            queryCount = entityManager
                .createNamedQuery(Resource.QUERY_FIND_IMPLICIT_RESOURCES_FOR_RESOURCE_GROUP_COUNT);
            queryCount.setParameter("subject", subject);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP, pageControl);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("groupId", group.getId());
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("groupId", group.getId());

        List<ResourceWithAvailability> results = query.getResultList();
        return new PageList<ResourceWithAvailability>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceHealthComposite> findResourceHealth(Subject user, int[] resourceIds, PageControl pc) {
        pc.initDefaultOrderingField("res.name");

        List<Integer> resourceIdList = ArrayUtils.wrapInList(resourceIds);

        if ((resourceIdList == null) || (resourceIdList.size() == 0)) {
            return new PageList<ResourceHealthComposite>(pc);
        }

        Query queryCount = PersistenceUtility
            .createCountQuery(entityManager, Resource.QUERY_GET_RESOURCE_HEALTH_BY_IDS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Resource.QUERY_GET_RESOURCE_HEALTH_BY_IDS, pc);

        queryCount.setParameter("resourceIds", resourceIdList);
        query.setParameter("resourceIds", resourceIdList);

        // because of the use of the GROUP BY clause, the query count will be returned as
        // the number of rows not as a single number
        long count = queryCount.getResultList().size();
        List<ResourceHealthComposite> results = query.getResultList();

        return new PageList<ResourceHealthComposite>(results, (int) count, pc);
    }

    @SuppressWarnings("unused")
    private void setImplicitMarkers(ResourceGroup group, List<ResourceWithAvailability> resources) {
        for (ResourceWithAvailability res : resources) {
            boolean explicitMember = false;
            for (ResourceGroup explicitGroup : res.getResource().getExplicitGroups()) {
                if (explicitGroup.getId() == group.getId()) {
                    explicitMember = true;
                    break;
                }
            }

            res.setExplicit(explicitMember);
        }
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @SuppressWarnings("unchecked")
    // note: this method also eagerly loads the parent resource, so that more context info is displayed for each record
    public PageList<Resource> findAvailableResourcesForResourceGroup(Subject user, int groupId, ResourceType type,
        ResourceCategory category, String nameFilter, int[] excludeIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        List<Integer> excludeList = ArrayUtils.wrapInList(excludeIds);

        Query queryCount;
        Query query;

        /*
         * don't use WITH_PARENT form for the queryCount; after it runs through the PersistenceUtility
         * you'll get the error "query specified join fetching, but the owner of the fetched association
         * was not present in the select list"
         */
        if ((excludeList != null) && (excludeList.size() != 0)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP_WITH_EXCLUDES);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_WITH_PARENT_FOR_RESOURCE_GROUP_WITH_EXCLUDES, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_WITH_PARENT_FOR_RESOURCE_GROUP, pageControl);
        }

        if ((excludeList != null) && (excludeList.size() != 0)) {
            queryCount.setParameter("excludeIds", excludeList);
            query.setParameter("excludeIds", excludeList);
        }

        nameFilter = QueryUtility.formatSearchParameter(nameFilter);

        queryCount.setParameter("groupId", groupId);
        query.setParameter("groupId", groupId);

        queryCount.setParameter("type", type);
        query.setParameter("type", type);

        queryCount.setParameter("category", category);
        query.setParameter("category", category);

        query.setParameter("search", nameFilter);
        queryCount.setParameter("search", nameFilter);

        query.setParameter("escapeChar", QueryUtility.getEscapeCharacter());
        queryCount.setParameter("escapeChar", QueryUtility.getEscapeCharacter());

        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        queryCount.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        long count = (Long) queryCount.getSingleResult();

        List<Resource> resources = query.getResultList();

        return new PageList<Resource>(resources, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findAvailableResourcesForRepo(Subject user, int repoId, String search,
        ResourceCategory category, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_REPO);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_REPO, pageControl);

        queryCount.setParameter("repoId", repoId);
        query.setParameter("repoId", repoId);

        search = QueryUtility.formatSearchParameter(search);
        queryCount.setParameter("search", search);
        query.setParameter("search", search);

        query.setParameter("escapeChar", QueryUtility.getEscapeCharacter());
        queryCount.setParameter("escapeChar", QueryUtility.getEscapeCharacter());

        queryCount.setParameter("category", category);
        query.setParameter("category", category);

        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        queryCount.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        long count = (Long) queryCount.getSingleResult();

        List<Resource> resources = query.getResultList();

        return new PageList<Resource>(resources, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    // note, typeId can be null
    public PageList<Resource> findAvailableResourcesForDashboardPortlet(Subject user, Integer typeId,
        ResourceCategory category, int[] excludeIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        List<Integer> excludeList = ArrayUtils.wrapInList(excludeIds);

        Query queryCount;
        Query query;
        if ((excludeList != null) && (excludeList.size() != 0)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET_WITH_EXCLUDES);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET_WITH_EXCLUDES, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_DASHBOARD_PORTLET, pageControl);
        }

        if ((excludeList != null) && (excludeList.size() != 0)) {
            queryCount.setParameter("excludeIds", excludeList);
            query.setParameter("excludeIds", excludeList);
        }

        queryCount.setParameter("typeId", typeId);
        query.setParameter("typeId", typeId);

        queryCount.setParameter("category", category);
        query.setParameter("category", category);

        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        queryCount.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        long count = (Long) queryCount.getSingleResult();

        List<Resource> resources = query.getResultList();

        return new PageList<Resource>(resources, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findResourceByIds(Subject subject, int[] resourceIds, boolean attachParentResource,
        PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        List<Integer> idList = ArrayUtils.wrapInList(resourceIds);

        if ((idList == null) || (idList.size() == 0)) {
            return new PageList<Resource>(Collections.EMPTY_LIST, 0, pageControl);
        }

        String queryCountName;
        String queryName;

        /*
         * don't use WITH_PARENT form for the queryCountName; after it runs through the PersistenceUtility
         * you'll get the error "query specified join fetching, but the owner of the fetched association
         * was not present in the select list"
         */
        if (authorizationManager.isInventoryManager(subject)) {
            if (attachParentResource) {
                queryName = Resource.QUERY_FIND_WITH_PARENT_BY_IDS_ADMIN;
            } else {

                queryName = Resource.QUERY_FIND_BY_IDS_ADMIN;
            }
            queryCountName = Resource.QUERY_FIND_BY_IDS_ADMIN;
        } else {
            if (attachParentResource) {
                queryName = Resource.QUERY_FIND_WITH_PARENT_BY_IDS;
            } else {

                queryName = Resource.QUERY_FIND_BY_IDS;
            }
            queryCountName = Resource.QUERY_FIND_BY_IDS;
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryCountName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pageControl);

        if (authorizationManager.isInventoryManager(subject) == false) {
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("ids", idList);
        query.setParameter("ids", idList);

        long count = (Long) queryCount.getSingleResult();
        List<Resource> resources = query.getResultList();

        return new PageList<Resource>(resources, (int) count, pageControl);
    }

    public Resource getResourceTree(int rootResourceId, boolean recursive) {
        Subject overlord = subjectManager.getOverlord();
        Resource root = getResourceById(overlord, rootResourceId);
        if (root != null) {
            prefetchResource(root, recursive);
            // load the parent - note we only load the root resource's parent
            if (root.getParentResource() != null) {
                root.getParentResource().getId();
            }
        }
        return root;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public List<ResourceError> findResourceErrors(Subject user, int resourceId, ResourceErrorType errorType) {
        // do authz check
        if (!authorizationManager.canViewResource(user, resourceId)) {
            throw new PermissionException("User [" + user + "] does not have permission to view resource ["
                + resourceId + "]");
        }

        // we passed authz check, now get the errors
        Query query = entityManager.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE);
        query.setParameter("resourceId", resourceId);
        query.setParameter("errorType", errorType);
        return query.getResultList();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public List<ResourceError> findResourceErrors(Subject user, int resourceId) {
        // do authz check
        if (!authorizationManager.canViewResource(user, resourceId)) {
            throw new PermissionException("User [" + user + "] does not have permission to view resource ["
                + resourceId + "]");
        }

        // we passed authz check, now get the errors
        Query query = entityManager.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID);
        query.setParameter("resourceId", resourceId);
        return query.getResultList();
    }

    public void addResourceError(ResourceError resourceError) {
        ResourceErrorType resourceErrorType = resourceError.getErrorType();

        if (resourceErrorType == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION
            || resourceErrorType == ResourceErrorType.AVAILABILITY_CHECK
            || resourceErrorType == ResourceErrorType.UPGRADE) {
            // there should be at most one invalid plugin configuration error, availability check
            // or upgrade error per resource, so delete any currently existing ones before we add this new one
            Subject overlord = subjectManager.getOverlord();
            int resourceId = resourceError.getResource().getId();
            resourceManager.clearResourceConfigErrorByType(overlord, resourceId, resourceErrorType);
        }

        entityManager.persist(resourceError);

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int clearResourceConfigErrorByType(Subject subject, int resourceId, ResourceErrorType resourceErrorType) {

        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("Cannot delete resource errors of type [" + resourceErrorType + "]. User ["
                + subject.getName() + "] does not have permission to operate on resource ID [" + resourceId + "].");
        }

        Query q = entityManager
            .createQuery("DELETE FROM ResourceError e WHERE e.resource.id = :resourceId AND e.errorType = :type");

        q.setParameter("resourceId", resourceId);
        q.setParameter("type", resourceErrorType);

        int updates = q.executeUpdate();
        return updates;
    }

    public void clearResourceConfigError(int resourceId) {
        // TODO change sig to get user passed in, rather than using overlord/assuming user is authz'ed
        Subject s = subjectManager.getOverlord();

        // make a direct local call - no need to go through the ByType method's REQUIRES_NEW interface here
        int cleared = clearResourceConfigErrorByType(s, resourceId, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);

        if (cleared > 1) {
            log.warn("Resource [" + resourceId + "] had [" + cleared
                + "] INVALID_PLUGIN_CONFIGURATION ResourceErrors associated with it.");
        }

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteResourceError(Subject user, int resourceErrorId) {
        ResourceError error = entityManager.find(ResourceError.class, resourceErrorId);

        if (error != null) {
            if (!authorizationManager.hasResourcePermission(user, Permission.MODIFY_RESOURCE, error.getResource()
                .getId())) {
                throw new PermissionException("Cannot delete Resource error [" + resourceErrorId + "]. User [" + user
                    + "] does not have permission to modify Resource [" + error.getResource().getName() + "].");
            }

            entityManager.remove(error);
        }

        return;
    }

    public Map<Integer, InventoryStatus> getResourceStatuses(int rootResourceId, boolean descendents) {
        Resource root = entityManager.find(Resource.class, rootResourceId);
        Map<Integer, InventoryStatus> statuses = new LinkedHashMap<Integer, InventoryStatus>();
        statuses.put(root.getId(), root.getInventoryStatus());
        getResourceStatuses(rootResourceId, descendents, statuses);

        return statuses;
    }

    public Resource getPlatform(Agent agent) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_PLATFORM_BY_AGENT);
        query.setParameter("category", ResourceCategory.PLATFORM);
        query.setParameter("agent", agent);

        try {
            Resource platform = (Resource) query.getSingleResult();
            return platform;
        } catch (NoResultException e) {
            //this means that the agent didn't send any info to us yet.
            //this can happen during the inital resource upgrade sync between
            //the agent and server.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public ResourceAvailabilitySummary getAvailabilitySummary(Subject user, int resourceId) {
        if (!authorizationManager.canViewResource(user, resourceId)) {
            throw new PermissionException("Cannot view resource availability. User [" + user
                + "] does not have permission to view the resource [" + resourceId + "].");
        }

        Query query = entityManager.createNamedQuery(Availability.FIND_BY_RESOURCE);
        query.setParameter("resourceId", resourceId);

        List<Availability> availabilities = query.getResultList();

        return new ResourceAvailabilitySummary(availabilities);
    }

    @SuppressWarnings("unchecked")
    public List<ResourceFlyweight> findResourcesByAgent(Subject user, int agentId, PageControl unlimitedInstance) {
        // Note: I didn't put these queries in as named queries since they have very specific prefeching
        // for this use case.

        String reportingQueryString = "" //
            + "    SELECT res.id, res.uuid, res.name, res.resourceKey, " //
            + "           parent.id, parent.name, " //
            + "           currentAvail.availabilityType, " //
            + "           type.id, type.name, type.plugin, type.singleton, type.category, " //
            + "           subCategory.id, subCategory.name, " //
            + "           parentSubCategory.id, parentSubCategory.name " //
            + "      FROM Resource res " //
            + "      JOIN res.currentAvailability currentAvail " //
            + "      JOIN res.resourceType type " //
            + " LEFT JOIN type.subCategory subCategory " //
            + " LEFT JOIN subCategory.parentSubCategory parentSubCategory " //
            + " LEFT JOIN res.parentResource parent " //
            + "     WHERE res.inventoryStatus = :inventoryStatus " //
            + "       AND res.agent.id = :agentId";

        Query reportingQuery = entityManager.createQuery(reportingQueryString);
        reportingQuery.setParameter("agentId", agentId);
        reportingQuery.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<Object[]> reportingQueryResults = reportingQuery.getResultList();
        List<ResourceFlyweight> resources = getFlyWeightObjectGraphFromReportingQueryResults(reportingQueryResults);

        if (!authorizationManager.isInventoryManager(user)) {
            String authorizationQueryString = "" //
                + "SELECT res.id " //
                + "  FROM Resource res " //
                + " WHERE res.inventoryStatus = :inventoryStatus " //
                + "   AND res.agent.id = :agentId " //
                + "   AND res.id IN ( SELECT rr.id " //
                + "                     FROM Resource rr " //
                + "                     JOIN rr.implicitGroups g " //
                + "                     JOIN g.roles r " //
                + "                     JOIN r.subjects s " //
                + "                    WHERE s = :subject)";

            Query authorizationQuery = entityManager.createQuery(authorizationQueryString);
            authorizationQuery.setParameter("agentId", agentId);
            authorizationQuery.setParameter("subject", user);
            authorizationQuery.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
            List<Integer> visibleResources = authorizationQuery.getResultList();

            HashSet<Integer> visibleIdSet = new HashSet<Integer>(visibleResources);

            ListIterator<ResourceFlyweight> iter = resources.listIterator();
            while (iter.hasNext()) {
                ResourceFlyweight res = iter.next();
                res.setLocked(!visibleIdSet.contains(res.getId()));
            }
        }

        return resources;
    }

    private List<ResourceFlyweight> getFlyWeightObjectGraphFromReportingQueryResults(List<Object[]> reportQueryResults) {
        List<ResourceFlyweight> resources = new ArrayList<ResourceFlyweight>();

        FlyweightCache flyweightCache = new FlyweightCache();
        for (Object[] prefetched : reportQueryResults) {
            // casts
            int i = 0;
            Integer resourceId = (Integer) prefetched[i++];
            String resourceUuid = (String) prefetched[i++];
            String resourceName = (String) prefetched[i++];
            String resourceKey = (String) prefetched[i++];

            Integer parentId = (Integer) prefetched[i++];
            String parentName = (String) prefetched[i++];

            AvailabilityType availType = (AvailabilityType) prefetched[i++];

            Integer typeId = (Integer) prefetched[i++];
            String typeName = (String) prefetched[i++];
            String typePlugin = (String) prefetched[i++];
            Boolean typeSingleton = (Boolean) prefetched[i++];
            ResourceCategory typeCategory = (ResourceCategory) prefetched[i++];

            Integer subCategoryId = (Integer) prefetched[i++];
            String subCategoryName = (String) prefetched[i++];

            Integer parentSubCategoryId = (Integer) prefetched[i++];
            String parentSubCategoryName = (String) prefetched[i++];

            if (subCategoryId != null) {
                //we don't need the reference to the sub category here. We need it just in the cache.
                flyweightCache.constructSubCategory(subCategoryId, subCategoryName, parentSubCategoryId,
                    parentSubCategoryName);
            }

            //we don't need the resource type reference here, only in the cache
            flyweightCache.constructResourceType(typeId, typeName, typePlugin, typeSingleton, typeCategory,
                subCategoryId);

            ResourceFlyweight resourceFlyweight = flyweightCache.constructResource(resourceId, resourceName,
                resourceUuid, resourceKey, parentId, typeId, availType);

            resources.add(resourceFlyweight);
        }

        return resources;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceFlyweight> findResourcesByCompatibleGroup(Subject user, int compatibleGroupId,
        PageControl pageControl) {
        // Note: I didn't put these queries in as named queries since they have very specific pre-fetching
        // for this use case.

        String reportingQueryString = "" //
            + "    SELECT res.id, res.uuid, res.name, res.resourceKey, " //
            + "           parent.id, parent.name, " //
            + "           currentAvail.availabilityType, " //
            + "           type.id, type.name, type.plugin, type.singleton, type.category, " //
            + "           subCategory.id, subCategory.name, " //
            + "           parentSubCategory.id, parentSubCategory.name " //
            + "      FROM Resource res " //
            + "      JOIN res.implicitGroups g " //
            + "      JOIN res.currentAvailability currentAvail " //
            + "      JOIN res.resourceType type " //
            + " LEFT JOIN type.subCategory subCategory " //
            + " LEFT JOIN subCategory.parentSubCategory parentSubCategory " //
            + " LEFT JOIN res.parentResource parent " //
            + "     WHERE res.inventoryStatus = :inventoryStatus " //
            + "       AND g.id = :groupId";

        Query reportingQuery = entityManager.createQuery(reportingQueryString);
        reportingQuery.setParameter("groupId", compatibleGroupId);
        reportingQuery.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<Object[]> reportingQueryResults = reportingQuery.getResultList();
        List<ResourceFlyweight> resources = getFlyWeightObjectGraphFromReportingQueryResults(reportingQueryResults);

        //        if (false) { //!authorizationManager.isInventoryManager(user)) {
        //            String authorizationQueryString = "" //
        //                + "    SELECT res.id \n" //
        //                + "      FROM Resource res " //
        //                + "     WHERE res.inventoryStatus = :inventoryStatus " //
        //                + "       AND (res.id IN (SELECT rr.id FROM Resource rr JOIN rr.explicitGroups g WHERE g.id = :groupId)\n"
        //                + "           OR res.id IN (SELECT rr.id FROM Resource rr JOIN rr.parentResource.explicitGroups g WHERE g.id = :groupId)\n"
        //                + "           OR res.id IN (SELECT rr.id FROM Resource rr JOIN rr.parentResource.parentResource.explicitGroups g WHERE g.id = :groupId)\n"
        //                + "           OR res.id IN (SELECT rr.id FROM Resource rr JOIN rr.parentResource.parentResource.parentResource.explicitGroups g WHERE g.id = :groupId)\n"
        //                + "           OR res.id IN (SELECT rr.id FROM Resource rr JOIN rr.parentResource.parentResource.parentResource.parentResource.explicitGroups g WHERE g.id = :groupId)) \n"
        //                + "       AND res.id IN (SELECT rr.id FROM Resource rr JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s WHERE s = :subject)";
        //
        //            Query authorizationQuery = entityManager.createQuery(authorizationQueryString);
        //            authorizationQuery.setParameter("groupId", compatibleGroupId);
        //            authorizationQuery.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        //            authorizationQuery.setParameter("subject", user);
        //            List<Integer> visibleResources = authorizationQuery.getResultList();
        //
        //            HashSet<Integer> visibleIdSet = new HashSet<Integer>(visibleResources);
        //
        //            ListIterator<ResourceFlyweight> iter = resources.listIterator();
        //            while (iter.hasNext()) {
        //                ResourceFlyweight res = iter.next();
        //                res.setLocked(!visibleIdSet.(res.getId()));
        //            }
        //        }

        return resources;
    }

    @SuppressWarnings("unchecked")
    private void getResourceStatuses(int parentResourceId, boolean descendents, Map<Integer, InventoryStatus> statuses) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_GET_STATUSES_BY_PARENT);
        query.setParameter("parentResourceId", parentResourceId);
        for (Object[] is : (List<Object[]>) query.getResultList()) {
            statuses.put((Integer) is[0], (InventoryStatus) is[1]);
            if (descendents) {
                getResourceStatuses((Integer) is[0], descendents, statuses);
            }
        }
    }

    private void prefetchResource(Resource resource, boolean recursive) {
        if (resource == null) {
            return; // Nothing to do on invalid input
        }
        resource.getId();
        resource.getPluginConfiguration().getNotes(); // Initialize the lazy plugin config
        // Init the lazy parent...
        // Don't fetch the parent's children, otherwise we'll end up in infinite recursion.
        prefetchResource(resource.getParentResource(), false);
        if (recursive) {
            // Recurse...
            for (Resource child : resource.getChildResources()) {
                prefetchResource(child, recursive);
            }
        }
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // Remote Interface Impl
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public Resource getResource(Subject subject, int resourceId) {
        return getResourceById(subject, resourceId);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public ResourceAvailability getLiveResourceAvailability(Subject subject, int resourceId) {
        Resource res = getResourceById(subject, resourceId);
        //platforms are never unknown, just up or down, so we need to default the availability to a different value
        //depending on the resource's category
        ResourceAvailability results = new ResourceAvailability(res,
            res.getResourceType().getCategory() == ResourceCategory.PLATFORM ? AvailabilityType.DOWN
                : AvailabilityType.UNKNOWN);

        try {
            // first, quickly see if we can even ping the agent, if not, don't bother trying to get the resource avail
            Agent agent = agentManager.getAgentByResourceId(subjectManager.getOverlord(), resourceId);
            if (agent == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Resource [" + resourceId + "] does not exist or has no agent assigned");
                }
                new IllegalStateException("No agent is associated with the resource with id [" + resourceId + "]");
            } else if (agent.getName().startsWith(ResourceHandlerBean.DUMMY_AGENT_NAME_PREFIX)
                && agent.getAgentToken().startsWith(ResourceHandlerBean.DUMMY_AGENT_TOKEN_PREFIX)) {
                // dummy agent created from REST
                return getResourceById(subject, resourceId).getCurrentAvailability();
            }
            AgentClient client = agentManager.getAgentClient(agent);
            if (client == null) {
                throw new IllegalStateException("No agent is associated with the resource with id [" + resourceId + "]");
            }

            AvailabilityReport report = null;

            boolean agentPing = client.ping(5000L);
            if (agentPing) {
                // we can't serialize the resource due to the hibernate proxies (agent can't deserialize hibernate objs)
                // but we know we only need the basics for the agent to collect availability, so create a bare resource object
                Resource bareResource = new Resource(res.getResourceKey(), res.getName(), res.getResourceType());
                bareResource.setId(res.getId());
                bareResource.setUuid(res.getUuid());
                report = client.getDiscoveryAgentService().getCurrentAvailability(bareResource, false);
            }

            if (report == null) {
                report = new AvailabilityReport(client.getAgent().getName());
                Availability fakeAvail = new Availability(res,
                    res.getResourceType().getCategory() == ResourceCategory.PLATFORM ? AvailabilityType.DOWN
                        : AvailabilityType.UNKNOWN);
                fakeAvail.setStartTime(System.currentTimeMillis());
                report.addAvailability(fakeAvail);
            }

            // The report is most likely empty as it's unlikely the avail has changed.  Don't merge it and return
            AvailabilityType foundAvail = report.forResource(res.getId());
            if (foundAvail != null) {
                availabilityManager.mergeAvailabilityReport(report);
            } else {
                foundAvail = res.getCurrentAvailability() == null ? AvailabilityType.UNKNOWN : res
                    .getCurrentAvailability().getAvailabilityType();
            }

            results.setAvailabilityType(foundAvail);

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to get live availability: " + e.getMessage());
            }
        }

        return results;
    }

    // lineage is a getXXX (not findXXX) because it logically returns a single object, but modeled as a list here
    public List<Resource> findResourceLineage(Subject subject, int resourceId) {
        List<Resource> result = getResourceLineage(resourceId);

        for (Resource resource : result) {
            if (!authorizationManager.canViewResource(subject, resource.getId())) {
                throw new PermissionException("User [" + subject + "] does not have permission to view resource ["
                    + resource.getId() + "]");
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public List<ResourceInstallCount> findResourceInstallCounts(Subject subject, boolean groupByVersions) {
        String queryName = groupByVersions ? Resource.QUERY_RESOURCE_VERSION_REPORT : Resource.QUERY_RESOURCE_REPORT;
        Query query = entityManager.createNamedQuery(queryName);
        List<ResourceInstallCount> results = query.getResultList();

        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public List<ResourceInstallCount> findResourceComplianceCounts(Subject subject) {
        Query query = null;
        List<ResourceInstallCount> results = null;

        query = entityManager.createNamedQuery(Resource.QUERY_RESOURCE_VERSION_AND_DRIFT_IN_COMPLIANCE);
        results = query.getResultList();

        query = entityManager.createNamedQuery(Resource.QUERY_RESOURCE_VERSION_AND_DRIFT_OUT_OF_COMPLIANCE);
        results.addAll(query.getResultList());

        return results;
    }

    public PageList<ResourceComposite> findResourceCompositesByCriteria(Subject subject, ResourceCriteria criteria) {

        boolean isInventoryManager = authorizationManager.isInventoryManager(subject);

        String compositeProjection;
        if (isInventoryManager) {
            compositeProjection = "" //
                + "new org.rhq.core.domain.resource.composite.ResourceComposite(" //
                + "   %alias%, " // Resource
                + "   %alias%.currentAvailability.availabilityType ) "; // AvailabilityType
        } else {
            compositeProjection = ""
                + " new org.rhq.core.domain.resource.composite.ResourceComposite(" //
                + "   %alias%, " // Resource
                + "   %alias%.currentAvailability.availabilityType, " // AvailabilityType
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 8 ), " // MANAGE_MEASUREMENTS
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 4 ), " // MODIFY_RESOURCE
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 10 ), " // CONTROL
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 7 ), " // MANAGE_ALERTS
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 14 ), " // MANAGE_EVENTS
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 13 ), " // CONFIGURE_READ
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 11 ), " // CONFIGURE_WRITE
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 9 ), " // MANAGE_CONTENT
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 6 ), " // CREATE_CHILD_RESOURCES
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 5 ), " // DELETE_RESOURCES
                + "   ( SELECT count(p) FROM %alias%.implicitGroups g JOIN g.roles r JOIN r.subjects s JOIN r.permissions p WHERE s.id = %subjectId% AND p = 16 ))"; // MANAGE_DRIFT
            compositeProjection = compositeProjection.replace("%subjectId%", String.valueOf(subject.getId()));
        }
        compositeProjection = compositeProjection.replace("%alias%", criteria.getAlias());

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        generator.alterProjection(compositeProjection);

        if (isInventoryManager == false) {
            if (criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            }

            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, null,
                subject.getId());
        }

        CriteriaQueryRunner<ResourceComposite> queryRunner = new CriteriaQueryRunner<ResourceComposite>(criteria,
            generator, entityManager, false); // don't auto-init bags, we're returning composites not entities
        PageList<ResourceComposite> results = queryRunner.execute();

        for (ResourceComposite nextComposite : results) {
            Resource nextResource = nextComposite.getResource();
            ResourceType nextResourceType = nextResource.getResourceType();
            ResourceFacets facets = typeManager.getResourceFacets(nextResourceType.getId());

            queryRunner.initFetchFields(nextResource); // manual field fetch for composite-wrapped entity
            nextComposite.setResourceFacets(facets);
        }

        return results;
    }

    public PageList<Resource> findResourcesByCriteria(Subject subject, ResourceCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);

        if (authorizationManager.isInventoryManager(subject) == false) {
            if (criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            }

            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, null,
                subject.getId());
        }

        CriteriaQueryRunner<Resource> queryRunner = new CriteriaQueryRunner<Resource>(criteria, generator,
            entityManager);
        PageList<Resource> results = queryRunner.execute();
        return results;
    }

    @Override
    public List<Resource> findResourcesByCriteriaBounded(Subject subject, ResourceCriteria criteria, int maxResources,
        int maxResourcesByType) {

        // find all of the requested resources but don't return them until they meet our bounded return requirements
        // maintain any requested sorting
        criteria.clearPaging();

        // perform the requested criteria query
        PageList<Resource> results = findResourcesByCriteria(subject, criteria);

        // If not specified use the default maxResources
        if (maxResources <= 0) {
            try {
                maxResources = Integer.parseInt(System.getProperty(
                    "rhq.server.findResourcesByCriteriaBounded.maxResources", BOUNDED_MAX_RESOURCES));
            } catch (NumberFormatException e) {
            }
            if (maxResources <= 0) {
                maxResources = Integer.parseInt(BOUNDED_MAX_RESOURCES);
            }
        }

        if (results.getTotalSize() <= maxResources) {
            return results;
        }

        // If not specified use the default maxResourcesByType
        if (maxResourcesByType <= 0) {
            try {
                maxResourcesByType = Integer.parseInt(System.getProperty(
                    "rhq.server.findResourcesByCriteriaBounded.maxResourcesByType", BOUNDED_MAX_RESOURCES_BY_TYPE));
            } catch (NumberFormatException e) {
            }
            if (maxResourcesByType <= 0) {
                maxResourcesByType = Integer.parseInt(BOUNDED_MAX_RESOURCES_BY_TYPE);
            }
        }

        // We need to trim the returned resources, enforce maxResourcesByType
        Map<Integer, Integer> typeCounts = new HashMap<Integer, Integer>();

        for (Iterator<Resource> i = results.iterator(); i.hasNext();) {
            Resource r = i.next();
            Integer typeId = r.getResourceType().getId();
            Integer count = typeCounts.get(typeId);
            if (null == count) {
                count = 0;
            }
            typeCounts.put(typeId, ++count);
            if (count > maxResourcesByType) {
                i.remove();
            }
        }

        // If after we've trimmed all types the results are still more than maxSize then we need to just chop
        // keeping the most important (presumably the beginning of the list, if it's sorted)
        while (maxResources < results.size()) {
            results.remove(maxResources);
        }

        return results;
    }

    public Resource getPlaformOfResource(Subject subject, int resourceId) {
        Resource resource = null;
        Resource parent = null;
        do {
            resource = parent;
            if (resource != null) {
                resourceId = parent.getId();
            }
            parent = getParentResource(resourceId);
            if (parent == null)
                break;
            if (parent.getResourceType().getCategory().equals(ResourceCategory.PLATFORM)) {
                resource = parent;
                parent = null;
                break;
            }

        } while (parent != null);
        if (resource != null) {
            if (!authorizationManager.canViewResource(subject, resource.getId())) {
                throw new PermissionException("User [" + subject + "] does not have permission to view resource ["
                    + resource.getId() + "]");
            }
        }
        return resource;
    }

    public Resource getParentResource(Subject subject, int resourceId) {
        Resource resource = getParentResource(resourceId);

        if (!authorizationManager.canViewResource(subject, resource.getId())) {
            throw new PermissionException("User [" + subject + "] does not have permission to view resource ["
                + resource.getId() + "]");
        }

        return resource;
    }

    public PageList<Resource> findChildResources(Subject subject, int parentResourceId, PageControl pageControl) {
        Resource parentResource = getResourceById(subject, parentResourceId);

        return (findChildResources(subject, parentResource, pageControl));
    }

    public <T> List<DisambiguationReport<T>> disambiguate(List<T> results, IntExtractor<? super T> extractor,
        DisambiguationUpdateStrategy updateStrategy) {
        return Disambiguator.disambiguate(results, updateStrategy, extractor, entityManager,
            typeManager.getDuplicateTypeNames());
    }

    public void updateAncestry(Subject subject, int resourceId) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (null == resource) {
            throw new ResourceNotFoundException(resourceId);
        }

        updateAncestry(resource);
    }

    private void updateAncestry(Resource resource) {
        resource.updateAncestryForResource();

        for (Resource child : resource.getChildResources()) {
            child.setParentResource(resource);
            updateAncestry(child);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findIdsByTypeIds(List<Integer> resourceTypeIds) {
        return entityManager.createNamedQuery(Resource.QUERY_FIND_IDS_BY_TYPE_IDS)
            .setParameter("resourceTypeIds", resourceTypeIds).getResultList();
    }

    @Override
    public Integer getResourceCount(List<Integer> resourceTypeIds) {
        return (Integer) entityManager.createNamedQuery(Resource.QUERY_FIND_COUNT_BY_TYPES)
            .setParameter("resourceTypeIds", resourceTypeIds).getSingleResult();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public List<Integer> disableResources(Subject subject, int[] resourceIds) {

        List<Integer> disableResourceIds = new ArrayList<Integer>();

        // one report for each agent, keyed by agent name
        Map<Agent, AvailabilityReport> reports = resourceManager.getDisableResourcesReportInNewTransaction(subject,
            resourceIds, disableResourceIds);

        // Set the resources disabled via the standard mergeInventoryReport mechanism, from the server service
        // level. We do this for a few reasons:
        // - The server service uses locking to ensure we don't conflict with an actual report from the agent
        // - It ensure all necessary db modifications take place, like avail history and current avail
        // - It ensures that all ancillary avail change logic, like alerting, still happens.
        DiscoveryServerServiceImpl service = new DiscoveryServerServiceImpl();
        for (AvailabilityReport report : reports.values()) {
            service.mergeAvailabilityReport(report);
        }

        return disableResourceIds;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Map<Agent, AvailabilityReport> getDisableResourcesReportInNewTransaction(Subject subject, int[] resourceIds,
        List<Integer> disableResourceIds) {

        // one report for each agent
        Map<Agent, AvailabilityReport> reports = new HashMap<Agent, AvailabilityReport>();
        long now = System.currentTimeMillis();

        boolean isInventoryManager = authorizationManager.isInventoryManager(subject);

        for (Integer resourceId : resourceIds) {
            if (disableResourceIds.contains(resourceId)) {
                continue;
            }

            // make sure the user is authorized to disable this resource (which implies you can disable all its children)
            // TODO: this may require its own permission, but until someone needs it we'll piggyback on DELETE, at least
            // that gives a resource-level permission option.
            if (!isInventoryManager
                && !authorizationManager.hasResourcePermission(subject, Permission.DELETE_RESOURCE, resourceId)) {
                throw new PermissionException("You do not have permission to disable resource [" + resourceId + "]");
            }

            Resource resource = entityManager.find(Resource.class, resourceId);

            if (null == resource) {
                log.info("Disable resource not possible, resource with id [" + resourceId + "] was not found");
                continue;
            }

            // you can't disable a platform
            if (null == resource.getParentResource()) {
                log.info("Disabling a platform is not allowed, skipping platform resource with id [" + resourceId + "]");
                continue;
            }

            // disable the resource and all its children, get the family resource ids
            if (log.isDebugEnabled()) {
                log.debug("Subject [" + subject + "] is setting resource [" + resource + "] and its children DISABLED.");
            }

            List<Integer> familyResourceIds = getFamily(resource);
            disableResourceIds.addAll(familyResourceIds);

            // add the family resource id's to the appropriate avail report
            Agent agent = resource.getAgent();
            AvailabilityReport report = reports.get(agent);
            if (null == report) {
                report = new AvailabilityReport(agent.getName());
                report.setEnablementReport(true);
                reports.put(agent, report);
            }

            for (Integer familyResourceId : familyResourceIds) {
                report.addAvailability(new AvailabilityReport.Datum(familyResourceId, AvailabilityType.DISABLED, now));
            }
        }

        return reports;
    }

    private List<Integer> getFamily(Resource resource) {

        // note - this query is good only to 6 levels deep
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_DESCENDANTS);
        query.setParameter("resourceId", resource.getId());

        List<Integer> resourceIds = query.getResultList();

        return resourceIds;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public List<Integer> enableResources(Subject subject, int[] resourceIds) {

        List<Integer> enableResourceIds = new ArrayList<Integer>();

        // one report for each agent, keyed by agent name
        Map<Agent, AvailabilityReport> reports = resourceManager.getEnableResourcesReportInNewTransaction(subject,
            resourceIds, enableResourceIds);

        // Set the resources disabled via the standard mergeInventoryReport mechanism, from the server service
        // level. We do this for a few reasons:
        // - The server service uses locking to ensure we don't conflict with an actual report from the agent
        // - It ensure all necessary db modifications take place, like avail history and current avail
        // - It ensures that all ancillary avail change logic, like alerting, still happens.
        DiscoveryServerServiceImpl service = new DiscoveryServerServiceImpl();
        for (AvailabilityReport report : reports.values()) {
            service.mergeAvailabilityReport(report);
        }

        // On a best effort basic, ask the relevant agents that their next avail report be full, so that we get
        // the current avail type for the newly enabled resources.  If we can't contact the agent don't worry about
        // it; if it's down we'll get a full report when it comes up.
        // TODO: This may need to be made out of band if perf becomes an issue.
        for (Agent agent : reports.keySet()) {
            try {
                AgentClient agentClient = agentManager.getAgentClient(agent);
                agentClient.getDiscoveryAgentService().requestFullAvailabilityReport();
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to notify Agent ["
                        + agent
                        + "] of enabled resources. The agent is likely down. This is ok, the avails will be updated when the agent is restarted or prompt command 'avail --force is executed'.");
                }
            }
        }

        return enableResourceIds;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Map<Agent, AvailabilityReport> getEnableResourcesReportInNewTransaction(Subject subject, int[] resourceIds,
        List<Integer> enableResourceIds) {

        // one report for each agent, keyed by agent name
        Map<Agent, AvailabilityReport> reports = new HashMap<Agent, AvailabilityReport>();
        long now = System.currentTimeMillis();

        boolean isInventoryManager = authorizationManager.isInventoryManager(subject);

        for (Integer resourceId : resourceIds) {
            if (enableResourceIds.contains(resourceId)) {
                continue;
            }

            // make sure the user is authorized to enable this resource (which implies you can enable all its children)
            // TODO: this may require its own permission, but until someone needs it we'll piggyback on DELETE, at least
            // that gives a resource-level permission option.
            if (!isInventoryManager
                && !authorizationManager.hasResourcePermission(subject, Permission.DELETE_RESOURCE, resourceId)) {
                throw new PermissionException("You do not have permission to enable resource [" + resourceId + "]");
            }

            Resource resource = entityManager.find(Resource.class, resourceId);
            if (null == resource) {
                log.info("Enable resource not possible, resource with id [" + resourceId + "] was not found");
                continue;
            }

            // you can't enable a platform
            if (null == resource.getParentResource()) {
                log.info("Enabling a platform is not allowed, skipping platform resource with id [" + resourceId + "]");
                continue;
            }

            // enable the resource and all its children, get the hierarchy
            if (log.isDebugEnabled()) {
                log.debug("Subject [" + subject + "] is setting resource [" + resource + "] and its children enabled.");
            }

            List<Integer> familyResourceIds = getFamily(resource);
            enableResourceIds.addAll(familyResourceIds);

            // add the family resource id's to the appropriate avail report
            Agent agent = resource.getAgent();
            AvailabilityReport report = reports.get(agent);
            if (null == report) {
                report = new AvailabilityReport(agent.getName());
                report.setEnablementReport(true);
                reports.put(agent, report);
            }

            for (Integer familyResourceId : familyResourceIds) {
                report.addAvailability(new AvailabilityReport.Datum(familyResourceId, AvailabilityType.UNKNOWN, now));
            }
        }

        return reports;
    }

    @Override
    public PageList<Resource> findGroupMemberCandidateResources(Subject subject, ResourceCriteria criteria,
        int[] alreadySelectedResourceIds) {

        PageControl originalPageControl = getPageControl(criteria);
        if (originalPageControl.isUnlimited()) {
            throw new UnsupportedOperationException("Supplied criteria has an unlimited PageControl");
        }

        Set<Integer> alreadySelectedResourceIdSet = new HashSet<Integer>(
            ArrayUtils.wrapInList(alreadySelectedResourceIds == null ? new int[0] : alreadySelectedResourceIds));

        PageControl pageControl = (PageControl) originalPageControl.clone();
        criteria.setPageControl(pageControl);

        int requiredPageSize = pageControl.getPageSize();
        criteria.setRestriction(COUNT_ONLY);
        int totalSize = findResourcesByCriteria(subject, criteria).getTotalSize();
        int totalPages = (totalSize / requiredPageSize) + (((totalSize % requiredPageSize) > 0) ? 1 : 0);

        criteria.setRestriction(COLLECTION_ONLY);
        List<Resource> candidates = new LinkedList<Resource>();
        for (int pageNumber = 0; candidates.size() < requiredPageSize && pageNumber < totalPages; pageNumber++) {
            pageControl.setPageNumber(pageNumber);
            PageList<Resource> foundResources = findResourcesByCriteria(subject, criteria);
            Collection<Resource> filteredResources = filterOutAlreadySelectedResources(foundResources,
                alreadySelectedResourceIdSet);

            candidates.addAll(filteredResources);
        }
        if (candidates.size() > requiredPageSize) {
            candidates = candidates.subList(0, requiredPageSize);
        }

        return new PageList<Resource>(candidates, totalSize, originalPageControl);
    }

    private Collection<Resource> filterOutAlreadySelectedResources(Collection<Resource> foundResources,
        Collection<Integer> alreadySelectedResourceIds) {
        List<Resource> result = new LinkedList<Resource>();
        for (Resource foundResource : foundResources) {
            if (!alreadySelectedResourceIds.contains(foundResource.getId())) {
                result.add(foundResource);
            }
        }
        return result;
    }
}
