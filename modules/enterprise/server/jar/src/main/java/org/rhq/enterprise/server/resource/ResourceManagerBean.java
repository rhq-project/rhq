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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.SchedulerException;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.ResourceChannel;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.AlertDefinitionCreationException;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.operation.ResourceOperationSchedule;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * A manager that provides methods for creating, updating, deleting, and querying {@link Resource}s.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.resource.ResourceManagerRemote")
//@WebContext(contextRoot = "/webservices")
public class ResourceManagerBean implements ResourceManagerLocal, ResourceManagerRemote {
    private final Log log = LogFactory.getLog(ResourceManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    @IgnoreDependency
    private AlertTemplateManagerLocal alertTemplateManager;
    @EJB
    private AlertManagerLocal alertManager;
    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private ResourceGroupManagerLocal groupManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager; // ourself, for xactional semantic consistency
    @EJB
    @IgnoreDependency
    private OperationManagerLocal operationManager;

    public void createResource(Subject user, Resource resource, int parentId) throws ResourceAlreadyExistsException {
        Resource parent = null;
        if (parentId != Resource.ROOT_ID) {
            // not trying to create a root resource, so lookup the parent
            parent = entityManager.find(Resource.class, parentId);

            // uh-oh if the parent could not be found
            if (parent == null) {
                throw new ResourceNotFoundException("Intended parent for new resource does not exist.");
            }
        }

        if (!authorizationManager.hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, parent.getId())) {
            throw new PermissionException("You do not have permission to add this resource as a child.");
        }

        if (getResourceByParentAndKey(user, parent, resource.getResourceKey(), resource.getResourceType().getPlugin(),
            resource.getResourceType().getName()) != null) {
            throw new ResourceAlreadyExistsException("Resource with key '" + resource.getResourceKey()
                + "' already exists.");
        }

        if (parent != Resource.ROOT) {
            // Only set the relationships if this is not a root resource.
            // The cardinal rule is to add the relationship in both directions,
            // so the parent will have direct access to this child after the method returns.
            resource.setParentResource(parent);
            parent.addChildResource(resource);
        }

        entityManager.persist(resource);

        // Execute sub-methods as overlord to bypass additional security checks.
        Subject overlord = this.subjectManager.getOverlord();
        updateImplicitMembership(overlord, resource);
        updateAlertDefinitions(overlord, resource);
    }

    private void updateAlertDefinitions(Subject subject, Resource resource) {
        ResourceType resourceType = resource.getResourceType();
        Subject overlord = subjectManager.getOverlord();

        /*
         * updates to AlertDefinitions from existing templates occurs implicitly by the system; thus, we need to perform
         * actions as the overlord and *not* as the user creating this resource
         */
        List<AlertDefinition> alertTemplates = alertTemplateManager.getAlertTemplates(overlord, resourceType.getId(),
            PageControl.getUnlimitedInstance());
        try {
            for (AlertDefinition template : alertTemplates) {
                // again, the overlord needs to call out to this system side-effect method
                alertTemplateManager.updateAlertDefinitionsForResource(overlord, template, resource.getId());
            }
        } catch (AlertDefinitionCreationException adce) {
            /* should never happen because AlertDefinitionCreationException is only ever
             * thrown if updateAlertDefinitionsForResource isn't called as the overlord
             *
             * but we'll log it anyway, just in case, so it isn't just swallowed
             */
            log.error(adce);
        }
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

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Integer> deleteResources(Subject user, Integer[] resourceIds) {
        List<Integer> deletedResourceIds = new ArrayList<Integer>();

        for (Integer resourceId : resourceIds) {
            if (!deletedResourceIds.contains(resourceId)) {
                deletedResourceIds.addAll(deleteResource(user, resourceId));
            }
        }

        return deletedResourceIds;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Integer> deleteResource(Subject user, Integer resourceId) {
        List<Integer> deletedResourceIds = new ArrayList<Integer>();
        if (resourceId == null) // sanity check
        {
            return deletedResourceIds;
        }

        Resource resource = resourceManager.getResourceTree(resourceId);
        if (resource == null) {
            log.info("Delete resouce not possible, as resource with id [" + resourceId + "] was not found");
            return deletedResourceIds; // Resource not found. TODO give a nice message to the user
        }

        // make sure the user is authorized to delete this resource (which implies you can delete all its children)
        if (!authorizationManager.hasResourcePermission(user, Permission.DELETE_RESOURCE, resourceId)) {
            throw new PermissionException("You do not have permission to delete resource [" + resourceId + "]");
        }

        // if the resource has no parent, its a top root resource and its agent should be purged too
        Agent doomedAgent = null;
        if (resource.getParentResource() == null) {
            doomedAgent = agentManager.getAgentByResourceId(resourceId);
        }

        AgentClient agentClient = agentManager.getAgentClient(resourceId);

        // delete the resource and all its children
        log.info("User [" + user + "] is deleting resource [" + resource + "]");
        deleteResourceRecursive(user, resource, deletedResourceIds);

        // still need to tell the agent about the removed resources so it stops avail reports
        if (agentClient != null) {
            try {
                agentClient.getDiscoveryAgentService().removeResource(resourceId);
            } catch (Exception e) {
                log.warn("Couldn't send inventory removal request to agent for resource[" + resourceId + "]", e);
            }
        }

        if (doomedAgent != null) {
            agentManager.deleteAgent(doomedAgent);
        }

        return deletedResourceIds;
    }

    /**
     * Deletes the resource and all its children. All deleted resources will have their IDs stuffed in <code>
     * deletedResourceIds</code> so the caller can know how many resources were really deleted and their IDs.
     *
     * @param user
     * @param resource
     * @param deletedResourceIds
     */
    private void deleteResourceRecursive(Subject user, Resource resource, List<Integer> deletedResourceIds) {
        // delete its children first
        Set<Resource> children = new HashSet<Resource>(resource.getChildResources());
        for (Resource child : children) {
            deleteResourceRecursive(user, child, deletedResourceIds);
        }

        // unschedule any resource operations
        Subject overlord = subjectManager.getOverlord();
        List<ResourceOperationSchedule> ops;
        int doomedResourceId = resource.getId();

        try {
            ops = operationManager.getScheduledResourceOperations(overlord, doomedResourceId);

            for (ResourceOperationSchedule schedule : ops) {
                try {
                    operationManager.unscheduleResourceOperation(overlord, schedule.getJobId().toString(),
                        doomedResourceId);
                } catch (SchedulerException e) {
                    log.warn("Failed to unschedule job [" + schedule + "] for a resource being deleted [" + resource
                        + "]", e);
                }
            }
        } catch (SchedulerException e1) {
            log.warn("Failed to get jobs for a resource being deleted [" + resource
                + "]; will not attempt to unshedule anything", e1);
        }

        // now delete the resource itself, relying on our cascading rules to take over and delete everything else
        resourceManager.deleteSingleResourceInNewTransaction(user, resource);

        deletedResourceIds.add(doomedResourceId);

        return;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteSingleResourceInNewTransaction(Subject user, Resource resource) {
        // if this resource was subscribed to channels, unsubscribe it
        // we are guaranteed to be in a new tx, so doing a bulk delete FIRST THING is ok
        entityManager.createNamedQuery(ResourceChannel.DELETE_BY_RESOURCE_ID).setParameter("resourceId",
            resource.getId()).executeUpdate();

        resource = entityManager.find(Resource.class, resource.getId());

        if (log.isDebugEnabled()) {
            log.debug("User [" + user + "] is deleting resource [" + resource + "]");
        }

        // we need to manually remove this resource from all its implicit and explicit groups
        Set<ResourceGroup> groups = new HashSet<ResourceGroup>(resource.getImplicitGroups());
        for (ResourceGroup group : groups) {
            group.removeImplicitResource(resource);
            entityManager.merge(group);
        }

        entityManager.flush();

        groups = new HashSet<ResourceGroup>(resource.getExplicitGroups());
        for (ResourceGroup group : groups) {
            group.removeExplicitResource(resource);
            entityManager.merge(group);
        }

        entityManager.flush();

        cleanupCircularObjectGraphs(resource.getId());
        cleanupScheduledOperationsForResource(resource.getId());
        entityManager.flush();

        alertConditionCacheManager.updateConditions(resource);

        // now we can purge the resource, let cascading do the rest
        entityManager.remove(resource);

        return;
    }

    private void cleanupScheduledOperationsForResource(Integer resourceId) {
        Subject overlord = subjectManager.getOverlord();
        try {
            List<ResourceOperationSchedule> schedules = operationManager.getScheduledResourceOperations(overlord,
                resourceId);

            for (ResourceOperationSchedule schedule : schedules) {
                try {
                    /*
                     * unscheduleResourceOperation already takes care of ignoring requests to delete unknown schedules,
                     * which would happen if the following sequence occurs:
                     *
                     * - a user tries to delete a resource, gets the list of resource operation schedules - just then, one
                     * or more of the schedules completes it's last scheduled firing, and is removed - then we try to
                     * unschedule it here, except that the jobid will no longer be known
                     */
                    operationManager.unscheduleResourceOperation(overlord, schedule.getJobId().toString(), resourceId);
                } catch (SchedulerException ise) {
                    log.warn("Failed to unschedule job [" + schedule + "] for a resource being deleted [" + resourceId
                        + "]", ise);
                }
            }
        } catch (SchedulerException se) {
            log.warn("Failed to get jobs for a resource being deleted [" + resourceId
                + "]; will not attempt to unshedule anything", se);
        }
    }

    /*
     * but first we must do explicit purging of parts of the data model that are circular (don't have a single, directed
     * path to each leaf)
     */
    private void cleanupCircularObjectGraphs(Integer resourceId) {
        /*
         * Here, the alerts subsystem has a two paths from a resource to an alert condition log, so we are going to
         * explicitly delete the left-hand path
         *
         *                        Resource                             |1                             |n
         *      AlertDefinition                      1/           \1                     n/             \n
         *   Alert             AlertCondition                     1\              /1                      n\
         * /n                     AlertConditionLogs
         */
        alertManager.deleteAlerts(subjectManager.getOverlord(), resourceId);
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

        resource.setInventoryStatus(newStatus);
        resource.setItime(now);

        resource = entityManager.merge(resource);
        if (setDescendents) {
            for (Resource childResource : resource.getChildResources()) {
                childResource.setInventoryStatus(newStatus);
                childResource.setItime(now);

                childResource = entityManager.merge(childResource);
                setResourceStatus(user, childResource, newStatus, setDescendents);
            }
        } else if (resource.getResourceType().getCategory() == ResourceCategory.PLATFORM) {
            // Commit platform services when the platform is committed
            for (Resource childResource : resource.getChildResources()) {
                if (childResource.getResourceType().getCategory() == ResourceCategory.SERVICE) {
                    childResource.setInventoryStatus(newStatus);
                    childResource.setItime(now);

                    childResource = entityManager.merge(childResource);
                    setResourceStatus(user, childResource, newStatus, setDescendents);
                }
            }
        }

        return resource;
    }

    @NotNull
    public Resource getResourceById(Subject user, int id) {
        Resource resource = entityManager.find(Resource.class, id);
        if (resource == null) {
            throw new ResourceNotFoundException(id);
        }

        if (!authorizationManager.canViewResource(user, resource.getId())) {
            throw new PermissionException("User [" + user.getName() + "] does not have permission to view resource ["
                + id + "]");
        }

        return resource;
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
    public List<ResourceWithAvailability> getResourcesByParentAndType(Subject user, Resource parent, ResourceType type) {

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

    @NotNull
    public List<Resource> getResourceLineage(int resourceId) {
        List<Resource> resourceLineage = new ArrayList<Resource>();
        Resource resource = entityManager.find(Resource.class, resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException(resourceId);
        }

        resourceLineage.add(resource);
        int childResourceId = resourceId;
        Resource parent;
        while ((parent = getParentResource(childResourceId)) != null) {
            resourceLineage.add(0, parent);
            childResourceId = parent.getId(); // This also ensures Hibernate actually populates parent's fields.
        }

        return resourceLineage;
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> getResourceByParentAndInventoryStatus(Subject user, Resource parent,
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
    public PageList<Resource> getChildResources(Subject user, Resource parent, PageControl pageControl) {
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
    public List<Integer> getChildrenResourceIds(int parentResourceId) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_IDS_ADMIN);
        query.setParameter("parentResourceId", parentResourceId);

        List<Integer> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getValidCommittedResourceIds(Integer[] resourceIds) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_VALID_COMMITTED_RESOURCE_IDS_ADMIN);
        query.setParameter("resourceIds", Arrays.asList(resourceIds));

        List<Integer> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> getChildResourcesByCategoryAndInventoryStatus(Subject user, Resource parent,
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
    public PageList<Resource> getResourcesByCategory(Subject user, ResourceCategory category,
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
        ResourceType type = (ResourceType) entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME)
            .setParameter("name", typeName).getSingleResult();
        Resource parentResource = null;

        if (parentResourceId > 0) {
            parentResource = getResourceById(user, parentResourceId);
        }

        type = entityManager.find(ResourceType.class, type.getId());

        return findResourceComposites(user, category, type, parentResource, searchString, pageControl);
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
    public PageList<ResourceComposite> findResourceComposites(Subject user, ResourceCategory category,
        ResourceType type, Resource parentResource, String searchString, PageControl pageControl) {
        pageControl.addDefaultOrderingField("res.name");
        pageControl.addDefaultOrderingField("res.id");

        Query query;
        Query queryCount;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_COMPOSITE_COUNT_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_COMPOSITE_ADMIN,
                pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_COMPOSITE_COUNT);
            query = PersistenceUtility
                .createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_COMPOSITE, pageControl);
            queryCount.setParameter("subject", user);
            query.setParameter("subject", user);
        }

        searchString = PersistenceUtility.formatSearchParameter(searchString);

        query.setParameter("category", category);
        queryCount.setParameter("category", category);
        query.setParameter("resourceType", type);
        queryCount.setParameter("resourceType", type);
        query.setParameter("search", searchString);
        queryCount.setParameter("search", searchString);
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
    public PageList<ResourceComposite> getResourceCompositeForParentAndTypeAndCategory(Subject user,
        ResourceCategory category, int resourceTypeId, Resource parentResource, PageControl pageControl) {
        // pageControl.initDefaultOrderingField(); // not needed since findResourceComposites will set it

        ResourceType resourceType = null;
        if (resourceTypeId != -1) {
            try {
                resourceType = entityManager.find(ResourceType.class, resourceTypeId);
            } catch (NoResultException nre) {
                ; // No problem
            }
        }

        return findResourceComposites(user, category, resourceType, parentResource, null, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> getResourcesByType(Subject user, ResourceType resourceType, PageControl pageControl) {
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

    public int getResourceCountByTypeAndIds(Subject user, ResourceType type, Integer[] resourceIds) {
        Query queryCount;
        if (authorizationManager.isInventoryManager(user)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_TYPE_AND_IDS_ADMIN);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_TYPE_AND_IDS);
            queryCount.setParameter("subject", user);
        }

        List<Integer> resourceList = Arrays.asList(resourceIds);
        queryCount.setParameter("ids", resourceList);

        queryCount.setParameter("type", type);

        long count = (Long) queryCount.getSingleResult();

        return (int) count;
    }

    @SuppressWarnings("unchecked")
    public List<RecentlyAddedResourceComposite> getRecentlyAddedPlatforms(Subject user, long ctime) {
        Query query;

        if (authorizationManager.isInventoryManager(user)) {
            query = entityManager.createNamedQuery(Resource.QUERY_RECENTLY_ADDED_PLATFORMS_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_RECENTLY_ADDED_PLATFORMS);
            query.setParameter("subject", user);
        }

        query.setParameter("oldestEpochTime", ctime);
        query.setMaxResults(100); // this query is only used by the dashboard portlet, let's not blow it up
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<RecentlyAddedResourceComposite> getRecentlyAddedServers(Subject user, long ctime, int platformId) {
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

    public List<AutoGroupComposite> getResourcesAutoGroups(Subject subject, List<Integer> resourceIds) {
        List<AutoGroupComposite> results = new ArrayList<AutoGroupComposite>();

        if ((resourceIds == null) || (resourceIds.size() == 0)) {
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

        query.setParameter("ids", resourceIds);

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

        query.setParameter("ids", resourceIds);

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
    public List<AutoGroupComposite> getChildrenAutoGroups(Subject user, int parentResourceId) {
        Query query;
        if (authorizationManager.isInventoryManager(user)) {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES_ADMIN);
        } else {
            query = entityManager.createNamedQuery(Resource.QUERY_FIND_CHILDREN_AUTOGROUP_COMPOSITES);
            query.setParameter("subject", user);
        }

        Resource parentResource = entityManager.getReference(Resource.class, parentResourceId);
        query.setParameter("parent", parentResource);

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

        calculateSubcategorySummary(parentResource.getResourceType().getSubCategories(), resourceAutoGroups, 0,
            fullComposites);
        fullComposites.addAll(resourceAutoGroups);

        return fullComposites;
    }

    private void calculateSubcategorySummary(List<ResourceSubCategory> subcategories,
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

                AutoGroupComposite categoryComposite = new AutoGroupComposite(avail, subCategory, count);
                categoryComposite.setDepth(depth);
                fullComposites.add(categoryComposite);
            }

            if (subCategory.getChildSubCategories() != null) {
                calculateSubcategorySummary(subCategory.getChildSubCategories(), resourceAutoGroups, depth + 1,
                    fullComposites);
            }

            // We matched all descendents above, but only list children directly as the child sub categories will already
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
    public PageList<Resource> getExplicitResourcesByResourceGroup(Subject user, ResourceGroup group,
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
    public List<Integer> getExplicitResourceIdsByResourceGroup(int resourceGroupId) {
        Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_EXPLICIT_IDS_BY_RESOURCE_GROUP_ADMIN);
        query.setParameter("groupId", resourceGroupId);

        List<Integer> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> getImplicitResourcesByResourceGroup(Subject user, ResourceGroup group,
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

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @SuppressWarnings("unchecked")
    public PageList<ResourceWithAvailability> getExplicitResourceWithAvailabilityByResourceGroup(Subject subject,
        ResourceGroup group, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP);
            queryCount.setParameter("subject", subject);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_EXPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP, pageControl);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("group", group);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("group", group);

        List<ResourceWithAvailability> results = query.getResultList();
        return new PageList<ResourceWithAvailability>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceWithAvailability> getImplicitResourceWithAvailabilityByResourceGroup(Subject subject,
        ResourceGroup group, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = entityManager
                .createNamedQuery(Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_COUNT_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_ADMIN, pageControl);
        } else {
            queryCount = entityManager
                .createNamedQuery(Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP_COUNT);
            queryCount.setParameter("subject", subject);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_IMPLICIT_RESOURCES_WITH_AVAILABILITY_FOR_RESOURCE_GROUP, pageControl);
            query.setParameter("subject", subject);
        }

        queryCount.setParameter("group", group);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("group", group);

        List<ResourceWithAvailability> results = query.getResultList();

        //setImplicitMarkers(group, results);
        return new PageList<ResourceWithAvailability>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<ResourceHealthComposite> getResourceHealth(Subject user, Integer[] resourceIds, PageControl pc) {
        pc.initDefaultOrderingField("res.name");

        if ((resourceIds == null) || (resourceIds.length == 0)) {
            return new PageList<ResourceHealthComposite>(pc);
        }

        Query queryCount = PersistenceUtility
            .createCountQuery(entityManager, Resource.QUERY_GET_RESOURCE_HEALTH_BY_IDS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Resource.QUERY_GET_RESOURCE_HEALTH_BY_IDS, pc);

        List<Integer> resourceIdsList = Arrays.asList(resourceIds);
        queryCount.setParameter("resourceIds", resourceIdsList);
        query.setParameter("resourceIds", resourceIdsList);

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
    public PageList<Resource> getAvailableResourcesForResourceGroup(Subject user, int groupId, ResourceType type,
        ResourceCategory category, String nameFilter, Integer[] excludeIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if ((excludeIds != null) && (excludeIds.length != 0)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP_WITH_EXCLUDES);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP_WITH_EXCLUDES, pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_GET_AVAILABLE_RESOURCES_FOR_RESOURCE_GROUP, pageControl);
        }

        if ((excludeIds != null) && (excludeIds.length != 0)) {
            List<Integer> excludeList = Arrays.asList(excludeIds);
            queryCount.setParameter("excludeIds", excludeList);
            query.setParameter("excludeIds", excludeList);
        }

        nameFilter = PersistenceUtility.formatSearchParameter(nameFilter);

        queryCount.setParameter("groupId", groupId);
        query.setParameter("groupId", groupId);

        queryCount.setParameter("type", type);
        query.setParameter("type", type);

        queryCount.setParameter("category", category);
        query.setParameter("category", category);

        query.setParameter("search", nameFilter);
        queryCount.setParameter("search", nameFilter);

        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);
        queryCount.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        long count = (Long) queryCount.getSingleResult();

        List<Resource> resources = query.getResultList();

        return new PageList(resources, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<Resource> getAvailableResourcesForDashboardPortlet(Subject user, Integer typeId,
        ResourceCategory category, Integer[] excludeIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        Query queryCount;
        Query query;
        if ((excludeIds != null) && (excludeIds.length != 0)) {
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

        if ((excludeIds != null) && (excludeIds.length != 0)) {
            List<Integer> excludeList = Arrays.asList(excludeIds);
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
    public PageList<Resource> getResourceByIds(Subject subject, Integer[] resourceIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("res.name");

        if ((resourceIds == null) || (resourceIds.length == 0)) {
            return new PageList<Resource>(Collections.EMPTY_LIST, 0, pageControl);
        }

        Query queryCount;
        Query query;

        if (authorizationManager.isInventoryManager(subject)) {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_IDS_ADMIN);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_BY_IDS_ADMIN,
                pageControl);
        } else {
            queryCount = PersistenceUtility.createCountQuery(entityManager, Resource.QUERY_FIND_BY_IDS);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager, Resource.QUERY_FIND_BY_IDS, pageControl);
            queryCount.setParameter("subject", subject);
            query.setParameter("subject", subject);
        }

        List<Integer> resourceList = Arrays.asList(resourceIds);
        queryCount.setParameter("ids", resourceList);
        query.setParameter("ids", resourceList);

        long count = (Long) queryCount.getSingleResult();
        List<Resource> resources = query.getResultList();

        return new PageList<Resource>(resources, (int) count, pageControl);
    }

    public Resource getResourceTree(int rootResourceId) {
        Resource root = entityManager.find(Resource.class, rootResourceId);

        if (root != null) {
            prefetchResource(root);

            // load the parent - note we only load the root resource's parent
            if (root.getParentResource() != null) {
                root.getParentResource().getId();
            }
        }

        return root;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public List<ResourceError> getResourceErrors(Subject user, int resourceId, ResourceErrorType errorType) {
        // do authz check
        getResourceById(user, resourceId);

        // we passed auth check, now get the errors
        Query query = entityManager.createNamedQuery(ResourceError.QUERY_FIND_BY_RESOURCE_ID_AND_ERROR_TYPE);
        query.setParameter("resourceId", resourceId);
        query.setParameter("errorType", errorType);
        return query.getResultList();
    }

    public void addResourceError(ResourceError resourceError) {
        Resource resource = entityManager.find(Resource.class, resourceError.getResource().getId());

        if (resource == null) {
            throw new ResourceNotFoundException("Resource error has been assigned an unknown resource: "
                + resourceError);
        }

        if (resourceError.getErrorType() == ResourceErrorType.INVALID_PLUGIN_CONFIGURATION) {
            // there should only ever be one invalid plugin configuration error per resource
            // delete any currently existing ones before we add this new one
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

    private void prefetchResource(Resource resource) {
        if (resource == null) {
            return; // Nothing to do on invalid input
        }

        resource.getId(); //getPluginConfiguration().getNotes(); // Initialize the lazy plugin config
        for (Resource child : resource.getChildResources()) {
            prefetchResource(child);
        }
    }
}