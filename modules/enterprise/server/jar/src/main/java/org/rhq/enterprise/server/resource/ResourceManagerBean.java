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
package org.rhq.enterprise.server.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.annotation.IgnoreDependency;

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
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackage;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.content.PackageInstallationStep;
import org.rhq.core.domain.content.ResourceRepo;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
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
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.resource.composite.ResourceIdFlyWeight;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.resource.composite.ResourceNamesDisambiguationResult;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.flyweight.FlyweightCache;
import org.rhq.core.domain.resource.flyweight.ResourceFlyweight;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.core.util.IntExtractor;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.jaxb.adapter.ResourceListAdapter;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.disambiguation.DisambiguationUpdateStrategy;
import org.rhq.enterprise.server.resource.disambiguation.Disambiguator;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
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
    private ResourceTypeManagerLocal typeManager;
    @EJB
    @IgnoreDependency
    private MeasurementScheduleManagerLocal measurementScheduleManager;

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
        if (!authorizationManager.hasResourcePermission(user, Permission.MODIFY_RESOURCE, resource.getId())) {
            throw new PermissionException("You do not have permission to modify resource");
        }

        /*if (getResourceByParentAndKey(user, resource.getParentResource(), resource.getResourceKey()) != null)
         * { throw new ResourceAlreadyExistsException("Resource with key '" + resource.getName() + "' already
         * exists");}*/
        return entityManager.merge(resource);
    }

    public List<Integer> uninventoryResources(Subject user, int[] resourceIds) {
        List<Integer> uninventoryResourceIds = new ArrayList<Integer>();

        for (Integer resourceId : resourceIds) {
            if (!uninventoryResourceIds.contains(resourceId)) {
                uninventoryResourceIds.addAll(uninventoryResource(user, resourceId));
            }
        }

        return uninventoryResourceIds;
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Integer> uninventoryResource(Subject user, int resourceId) {
        Resource resource = resourceManager.getResourceTree(resourceId, true);
        if (resource == null) {
            log.info("Delete resource not possible, as resource with id [" + resourceId + "] was not found");
            return Collections.emptyList(); // Resource not found. TODO give a nice message to the user
        }

        // make sure the user is authorized to delete this resource (which implies you can delete all its children)
        if (!authorizationManager.hasResourcePermission(user, Permission.DELETE_RESOURCE, resourceId)) {
            throw new PermissionException("You do not have permission to uninventory resource [" + resourceId + "]");
        }

        // if the resource has no parent, its a top root resource and its agent should be purged too
        // test code does not always follow this rule, so catch and continue.
        Agent doomedAgent = null;
        if (resource.getParentResource() == null) {
            try {
                // note, this needs to be done before the marking because the agent reference is going to be set to null
                doomedAgent = agentManager.getAgentByResourceId(resourceId);
            } catch (Exception e) {
                doomedAgent = null;
                log.warn("This warning should occur in TEST code only! " + e);
            }
        }

        AgentClient agentClient = null;
        try {
            // The test code does not always generate agents for the resources. Catch and log any problem but continue
            agentClient = agentManager.getAgentClient(resourceId);
        } catch (Throwable t) {
            log.warn("No AgentClient found for resource [" + resource
                + "]. Unable to inform agent of inventory removal (this may be ok): " + t);
        }

        // since we delete the resource asychronously now, we need to make sure we remove things that would cause
        // system side effects after markForDeletion completed but before the resource was actually removed from the DB
        Subject overlord = subjectManager.getOverlord();

        // delete the resource and all its children
        log.info("User [" + user + "] is marking resource [" + resource + "] for asynchronous uninventory");

        // set agent references null
        // foobar the resourceKeys
        // update the inventory status to UNINVENTORY
        Query toBeDeletedQuery = entityManager.createNamedQuery(Resource.QUERY_FIND_DESCENDENTS);
        toBeDeletedQuery.setParameter("resourceId", resourceId);
        List<Integer> toBeDeletedResourceIds = toBeDeletedQuery.getResultList();

        int i = 0;
        log.debug("== total size : " + toBeDeletedResourceIds.size());

        while (i < toBeDeletedResourceIds.size()) {
            int j = i + 1000;
            if (j > toBeDeletedResourceIds.size())
                j = toBeDeletedResourceIds.size();
            List<Integer> idsToDelete = toBeDeletedResourceIds.subList(i, j);
            log.debug("== Bounds " + i + ", " + j);

            boolean hasErrors = uninventoryResourcesBulkDelete(overlord, idsToDelete);
            if (hasErrors) {
                throw new IllegalArgumentException("Could not remove resources from their containing groups");
            }
            i = j;
        }

        Query markDeletedQuery = entityManager.createNamedQuery(Resource.QUERY_MARK_RESOURCES_FOR_ASYNC_DELETION);
        markDeletedQuery.setParameter("resourceId", resourceId);
        markDeletedQuery.setParameter("status", InventoryStatus.UNINVENTORIED);
        int resourcesDeleted = markDeletedQuery.executeUpdate();

        if (resourcesDeleted != toBeDeletedResourceIds.size()) {
            log.error("Tried to uninventory " + toBeDeletedResourceIds.size()
                + " resources, but actually uninventoried " + resourcesDeleted);
        }

        // still need to tell the agent about the removed resources so it stops avail reports
        if (agentClient != null) {
            try {
                agentClient.getDiscoveryAgentService().uninventoryResource(resourceId);
            } catch (Exception e) {
                log.warn(" Unable to inform agent of inventory removal for resource [" + resourceId + "]", e);
            }
        }

        if (doomedAgent != null) {
            agentManager.deleteAgent(doomedAgent);
        }

        return toBeDeletedResourceIds;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getResourceDescendantsByTypeAndName(Subject user, int resourceId, Integer resourceTypeId,
        String name) {
        Query descendantQuery = entityManager.createNamedQuery(Resource.QUERY_FIND_DESCENDENTS_BY_TYPE_AND_NAME);
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

        // now we can purge the resource, let cascading do the rest
        entityManager.remove(attachedResource);

        return;
    }

    private boolean uninventoryResourcesBulkDelete(Subject overlord, List<Integer> resourceIds) {
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

        return hasErrors;
    }

    private boolean uninventoryResourceBulkDeleteAsyncWork(Subject overlord, int resourceId) {
        String[] namedQueriesToExecute = new String[] { //
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
            AlertConditionLog.QUERY_DELETE_BY_RESOURCES, //    Don't
            AlertNotificationLog.QUERY_DELETE_BY_RESOURCES, // alter
            Alert.QUERY_DELETE_BY_RESOURCES, //                order
            AlertCondition.QUERY_DELETE_BY_RESOURCES, //       of
            AlertDampeningEvent.QUERY_DELETE_BY_RESOURCES, //  alert-
            AlertNotification.QUERY_DELETE_BY_RESOURCES, //    related
            AlertDefinition.QUERY_DELETE_BY_RESOURCES //       deletes
        };

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

            if (debugEnabled)
                log.debug("uninv, running query: " + namedQueryToExecute);
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
        if ((resource.getParentResource() != null)
            && (resource.getParentResource().getInventoryStatus() != InventoryStatus.COMMITTED)) {
            throw new IllegalStateException("Cannot commit resource [" + resource
                + "] to inventory, because its parent resource [" + resource.getParentResource()
                + "] has not yet been committed.");
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
        resource.setMtime(now);
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

        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_PAREBT_ID);
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

    public List<Resource> getResourceLineageAndSiblings(int resourceId) {
        List<Resource> resourceLineage = getResourceLineage(resourceId);
        LinkedList<Resource> result = new LinkedList<Resource>();

        result.add(resourceLineage.get(0));
        for (Resource resource : resourceLineage) {
            result.addAll(resource.getChildResources());
        }

        return result;
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
    public PageList<Resource> findChildResourcesByCategoryAndInventoryStatus(Subject user, Resource parent,
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

    public PageList<ResourceComposite> findResourceComposites(Subject user, ResourceCategory category, String typeName,
        int parentResourceId, String searchString, PageControl pageControl) {

        ResourceType type = null;
        Resource parentResource = null;

        if (null != typeName) {
            Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME);
            query.setParameter("name", typeName);
            type = (ResourceType) query.getSingleResult();
            // TODO: why is this being fetched after it was just loaded?!
            type = entityManager.find(ResourceType.class, type.getId());
        }
        if (parentResourceId > 0) {
            parentResource = getResourceById(user, parentResourceId);
        }

        String typeNameFilter = type == null ? null : type.getName();
        return findResourceComposites(user, category, typeNameFilter, null, parentResource, searchString, false,
            pageControl);
    }

    /**
     * This finder query can be used to find resources with various combinations of attributes in their composite form.
     * Except for the user parameter, the other parameters can be left null so that the query will not filter by that
     * attribute.
     *
     * @param  user
     * @param  category       Limit the search to a given {@link ResourceCategory}
     * @param  type           Limit the search to to a given {@link ResourceType}
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
        query.setMaxResults(maxItems); // this query is only used by the dashboard portlet, let's not blow it up
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

        return new PageList(resources, (int) count, pageControl);
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

        return new PageList(resources, (int) count, pageControl);
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

        return new PageList(resources, (int) count, pageControl);
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

    public void addResourceError(ResourceError resourceError) {
        Subject overlord = subjectManager.getOverlord();
        Resource resource;
        try {
            resource = getResourceById(overlord, resourceError.getResource().getId());
        } catch (ResourceNotFoundException rnfe) {
            throw new ResourceNotFoundException("Resource error contains an unknown Resource id: " + resourceError);
        }

        if (resourceError.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION
            || resourceError.getErrorType() == ResourceErrorType.AVAILABILITY_CHECK) {
            // there should be at most one invalid plugin configuration error and one availability check error per
            // resource, so delete any currently existing ones before we add this new one
            List<ResourceError> doomedErrors = resource.getResourceErrors(resourceError.getErrorType());

            // there should only ever be at most 1, but loop through the list just in case something got screwed up
            // and there ended up more than 1 associated with the resource.
            for (ResourceError doomedError : doomedErrors) {
                entityManager.remove(doomedError);
            }
        }

        entityManager.persist(resourceError);

        return;
    }

    public void clearResourceConfigError(int resourceId) {
        // TODO Add subject permissions to this method

        Query q = entityManager
            .createQuery("delete from ResourceError e where e.resource.id = :resourceId and e.errorType = :type");

        q.setParameter("resourceId", resourceId);
        q.setParameter("type", ResourceErrorType.INVALID_PLUGIN_CONFIGURATION);

        int updates = q.executeUpdate();

        if (updates > 1) {
            log.error("Resource [" + resourceId + "] has [" + updates
                + "] INVALID_PLUGIN_CONFIGURATION ResourceError associated with it.");
        }

    }

    public void deleteResourceError(Subject user, int resourceErrorId) {
        ResourceError error = entityManager.find(ResourceError.class, resourceErrorId);

        if (error != null) {
            if (!authorizationManager.canViewResource(user, error.getResource().getId())) {
                throw new PermissionException("Cannot delete resource error [" + resourceErrorId + "]. User [" + user
                    + "] does not have permission to operate on resource [" + error.getResource().getName() + "].");
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
        Resource platform = (Resource) query.getSingleResult();
        return platform;
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
        long upTime = 0;
        long downTime = 0;
        int failures = 0;
        long lastChange = 0;
        AvailabilityType current = null;
        for (Availability avail : availabilities) {
            if (avail.getAvailabilityType() == AvailabilityType.UP) {
                upTime += ((avail.getEndTime() != null ? avail.getEndTime().getTime() : System.currentTimeMillis()) - avail
                    .getStartTime().getTime());
            } else {
                downTime += ((avail.getEndTime() != null ? avail.getEndTime().getTime() : System.currentTimeMillis()) - avail
                    .getStartTime().getTime());
                failures++;
            }
            if (avail.getEndTime() == null) {
                lastChange = avail.getStartTime().getTime();
                current = avail.getAvailabilityType();
            }
        }

        return new ResourceAvailabilitySummary(upTime, downTime, failures, lastChange, current);
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
        //                res.setLocked(!visibleIdSet.contains(res.getId()));
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

    public ResourceAvailability getLiveResourceAvailability(Subject subject, int resourceId) {
        Resource res = getResourceById(subject, resourceId);
        ResourceAvailability results = new ResourceAvailability(res, null);

        try {
            Agent agent = res.getAgent();
            if (agent == null) {
                throw new IllegalStateException("No agent is associated with the resource with id [" + resourceId + "]");
            }

            // first, quickly see if we can even ping the agent, if not, don't bother trying to get the resource avail
            AgentClient client = agentManager.getAgentClient(agent);
            boolean agentPing = client.ping(5000L);
            if (agentPing) {
                // we can't serialize the resource due to the hibernate proxies (agent can't deserialize hibernate objs)
                // but we know we only need the basics for the agent to collect availability, so create a bare resource object
                Resource bareResource = new Resource(res.getResourceKey(), res.getName(), res.getResourceType());
                bareResource.setId(res.getId());
                bareResource.setUuid(res.getUuid());
                Availability avail = client.getDiscoveryAgentService().getCurrentAvailability(bareResource);
                if (avail != null) {
                    results.setAvailabilityType(avail.getAvailabilityType());
                }
            }
        } catch (Throwable ignore) {
        }

        return results;
    }

    // lineage is a getXXX (not findXXX) because it logically returns a single object, but modeled as a list here
    public @XmlJavaTypeAdapter(value = ResourceListAdapter.class)
    List<Resource> findResourceLineage(Subject subject, int resourceId) {
        List<Resource> result = null;

        result = getResourceLineage(resourceId);

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
        Query query = null;
        if (!groupByVersions) {
            query = entityManager.createNamedQuery(Resource.QUERY_RESOURCE_REPORT);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_RESOURCE_VERSION_REPORT);
        }

        List<ResourceInstallCount> results = query.getResultList();
        return results;
    }

    public PageList<ResourceComposite> findResourceCompositesByCriteria(Subject subject, ResourceCriteria criteria) {
        PageList<Resource> intermediate = findResourcesByCriteria(subject, criteria);

        List<ResourceComposite> results = new ArrayList<ResourceComposite>();
        for (Resource next : intermediate) {
            AvailabilityType availType = next.getCurrentAvailability().getAvailabilityType();
            Resource parent = next.getParentResource();
            ResourceComposite composite = new ResourceComposite(next, parent, availType);
            composite.setResourceFacets(typeManager.getResourceFacets(next.getResourceType().getId()));
            // TODO: jmarques: need to set resource permissions here, or alter criteria projection to include it
            results.add(composite);
        }

        return new PageList<ResourceComposite>(results, (int) intermediate.getTotalSize(), intermediate
            .getPageControl());
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> findResourcesByCriteria(Subject subject, ResourceCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;
        if (authorizationManager.isInventoryManager(subject) == false) {
            if (criteria.isInventoryManagerRequired()) {
                throw new PermissionException("Subject [" + subject.getName()
                    + "] requires InventoryManager permission for requested query criteria.");
            }

            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE, null,
                subject.getId());
        }

        CriteriaQueryRunner<Resource> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);

        return queryRunner.execute();
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

    public <T> ResourceNamesDisambiguationResult<T> disambiguate(List<T> results, IntExtractor<? super T> extractor,
        DisambiguationUpdateStrategy updateStrategy) {
        return Disambiguator.disambiguate(results, updateStrategy, extractor, entityManager);
    }
}
