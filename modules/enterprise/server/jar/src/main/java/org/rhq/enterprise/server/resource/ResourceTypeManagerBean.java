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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;

/**
 * A manager that provides methods for creating, updating, deleting, and querying
 * {@link org.rhq.core.domain.resource.ResourceType}s.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
@Stateless
public class ResourceTypeManagerBean implements ResourceTypeManagerLocal {
    private final Log log = LogFactory.getLog(ResourceTypeManagerBean.class);

    // TODO: Add a getResourceTypeByResourceId method.

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager;

    public ResourceType getResourceTypeById(Subject subject, int id) throws ResourceTypeNotFoundException {
        // this operation does not need to be secured; types are data side-effects of authorized resources
        ResourceType resourceType = entityManager.find(ResourceType.class, id);
        if (resourceType == null) {
            throw new ResourceTypeNotFoundException("Resource type with id [" + id + "] does not exist.");
        }

        return resourceType;
    }

    public ResourceType getResourceTypeByParentAndName(Subject subject, @Nullable
    ResourceType parent, @NotNull
    String name) throws ResourceTypeNotFoundException {
        // TODO: authz check - or does this operation really need to be secured?
        // TODO: these queries can actually return a set - resource type uniqueness is on parent/name/plugin not just parent/name
        try {
            ResourceType resourceType;
            if (parent != null) {
                resourceType = (ResourceType) entityManager
                    .createNamedQuery(ResourceType.QUERY_FIND_BY_PARENT_AND_NAME).setParameter("parent", parent)
                    .setParameter("name", name).getSingleResult();
            } else {
                resourceType = (ResourceType) entityManager.createNamedQuery(ResourceType.QUERY_FIND_ROOT_TYPE_BY_NAME)
                    .setParameter("name", name).getSingleResult();
            }

            return resourceType;
        }

        // NOTE: Sun's JPA Javadocs say getSingleResult() throws a EntityNotFoundException, but JBoss' impl instead
        //       throws a NoResultException.
        catch (NoResultException e) {
            throw new ResourceTypeNotFoundException("Resource type with parent [" + parent + "] and name [" + name
                + "] does not exist in inventory.");
        }
    }

    /**
     * Returns the resource type by name and plugin or null if the type is not found
     *
     * @param  name
     * @param  plugin
     *
     * @return
     */
    public ResourceType getResourceTypeByNameAndPlugin(String name, String plugin) {
        try {
            Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_NAME_AND_PLUGIN);
            query.setParameter("name", name);
            query.setParameter("plugin", plugin);
            return (ResourceType) query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getChildResourceTypes(Subject subject, ResourceType parent) {
        final String queryName = "ResourceType.findChildren";

        //Query queryCount = null;
        Query query = null;
        if (authorizationManager.isInventoryManager(subject)) {
            //queryCount = PersistenceUtility.createCountQuery(entityManager, queryName + "_admin");
            query = entityManager.createNamedQuery(queryName + "_admin");
        } else {
            //queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
            //queryCount.setParameter("subject", subject);
            query = entityManager.createNamedQuery(queryName);
            query.setParameter("subject", subject);
        }

        //queryCount.setParameter("parent", parent);
        //long count = (Long)queryCount.getSingleResult();

        query.setParameter("parent", parent);

        //PersistenceUtility.setDataPage(query, pageControl.getPageEntityIndex(), pageControl.getPagesize());
        List<ResourceType> results = query.getResultList();

        //return new PageList<Resource>(results, (int)count);
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getChildResourceTypesByCategory(Subject subject, Resource parentResource,
        ResourceCategory category) {
        final String QUERY_NAME = ResourceType.QUERY_FIND_CHILDREN_BY_CATEGORY;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(QUERY_NAME + "_admin");
        } else {
            query = entityManager.createNamedQuery(QUERY_NAME);
            // TODO: Uncomment the below line once the query supports authz.
            //query.setParameter(5, subject.getId());
        }

        query.setParameter(1, parentResource.getId());
        query.setParameter(2, category.name());
        query.setParameter(3, parentResource.getId());
        query.setParameter(4, category.name());

        List<ResourceType> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getUtilizedChildResourceTypesByCategory(Subject subject, Resource parentResource,
        ResourceCategory category) {
        final String QUERY_NAME = ResourceType.QUERY_FIND_UTILIZED_CHILDREN_BY_CATEGORY;
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(QUERY_NAME + "_admin");
        } else {
            query = entityManager.createNamedQuery(QUERY_NAME);
            query.setParameter("subject", subject);
        }

        query.setParameter("parentResource", parentResource);
        query.setParameter("category", category);
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<ResourceType> results = query.getResultList();
        return results;
    }

    /**
     * Obtain ResourceTypes that match a given category or all if category is null.
     *
     * @param  subject  subject of the caller
     * @param  category the category to check for. If this is null, entries from all categories will be returned.
     *
     * @return a List of ResourceTypes
     *
     * @see    ResourceCategory
     */
    @SuppressWarnings("unchecked")
    public List<ResourceType> getAllResourceTypesByCategory(Subject subject, ResourceCategory category) {
        Query query;

        if (category != null) {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_CATEGORY);
            query.setParameter("category", category);
        } else {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_ALL);
        }

        List<ResourceType> res = query.getResultList();
        return res;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getUtilizedResourceTypesByCategory(Subject subject, ResourceCategory category,
        String nameFilter) {
        final String queryName = ResourceType.QUERY_FIND_UTILIZED_BY_CATEGORY;

        Query query = null;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(queryName + "_admin");
        } else {
            query = entityManager.createNamedQuery(queryName);
            query.setParameter("subject", subject);
        }

        query.setParameter("category", category);
        query.setParameter("nameFilter", PersistenceUtility.formatSearchParameter(nameFilter));
        query.setParameter("inventoryStatus", InventoryStatus.COMMITTED);

        List<ResourceType> results = query.getResultList();
        return results;
    }

    /**
     * Return all ResourceTypes that are children of the passed ones
     *
     * @param  types List of ResourceTypes
     *
     * @return List of ResourceTypes. If nothing is found, then this list is empty
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, SortedSet<ResourceType>> getChildResourceTypesForResourceTypes(List<ResourceType> types) {
        // nothing to do
        if ((types == null) || (types.size() == 0)) {
            return new HashMap<Integer, SortedSet<ResourceType>>();
        }

        // save array with inputs as we need this later
        List<Integer> ids = new ArrayList<Integer>(types.size());
        for (ResourceType type : types) {
            ids.add(type.getId());
        }

        Query q = entityManager.createNamedQuery(ResourceType.FIND_CHILDREN_BY_PARENT);
        q.setParameter("resourceType", types);
        List<ResourceType> childResourceTypes = q.getResultList();

        /*
         * We have the original children of the input. Now get their children as well until we are done. We need to pay
         * attention a) not to add duplicates. This is done by using a Set later. b) prevent running in cycles. This
         * should not happen if the ResourceTypes are    correctly added the system, but one never knows
         */
        List<ResourceType> newChildResourceTypes = new ArrayList<ResourceType>();
        newChildResourceTypes.addAll(childResourceTypes);
        int numberOfChildrenToFetch = childResourceTypes.size();
        while (numberOfChildrenToFetch > 0) {
            if (log.isTraceEnabled()) {
                log.trace("*** getting children for " + newChildResourceTypes);
            }

            q.setParameter("resourceType", newChildResourceTypes);
            List<ResourceType> newChildren = q.getResultList();

            newChildResourceTypes = new ArrayList<ResourceType>();

            // check for infinite recursion and only add children we did not yet see
            for (ResourceType rt : newChildren) {
                if (!childResourceTypes.contains(rt)) {
                    childResourceTypes.add(rt);
                    newChildResourceTypes.add(rt);
                }
            }

            numberOfChildrenToFetch = newChildResourceTypes.size();
        }

        /*
         * now sort the result list in the result map Idea is to have a map <key of parent, List of children>.
         *
         * As we have an arbitrary depth of the tree, parents are not only the originally wanted parents, but also
         * children that have grand children
         */
        Map<Integer, SortedSet<ResourceType>> result = new HashMap<Integer, SortedSet<ResourceType>>();
        for (ResourceType childType : childResourceTypes) {
            for (ResourceType parent : childType.getParentResourceTypes()) {
                Integer id = parent.getId();

                // add a new map entry if not yet there
                if (!result.containsKey(id)) {
                    result.put(id, new TreeSet<ResourceType>());
                }

                SortedSet<ResourceType> rtl = result.get(id);
                rtl.add(childType);
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("============ input: ");
            log.trace(ids);
            Set<Integer> keys = result.keySet();
            for (Integer key : keys) {
                log.trace("Key: " + key);
                SortedSet<ResourceType> rts = result.get(key);
                for (ResourceType rt : rts) {
                    log.trace("  \\-> " + rt);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getResourceTypesForCompatibleGroups(Subject subject) {
        final String queryName = ResourceType.QUERY_FIND_BY_RESOURCE_GROUP;

        Query query = null;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(queryName + "_admin");
        } else {
            query = entityManager.createNamedQuery(queryName);
            query.setParameter("subject", subject);
        }

        List<ResourceType> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getResourceTypeCountsByGroup(Subject subject, ResourceGroup group) {
        final String queryName = ResourceType.QUERY_GET_RESOURCE_TYPE_COUNTS_BY_GROUP;
        Query query = entityManager.createNamedQuery(queryName);
        query.setParameter("groupId", group.getId());
        List results = query.getResultList();

        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0, sz = results.size(); i < sz; i++) {
            Object[] elements = (Object[]) results.get(i);
            long count = (Long) elements[2];
            map.put((String) elements[1], (int) count);
        }

        return map;
    }

    public boolean ensureResourceType(Subject subject, Integer resourceTypeId, Integer[] resourceIds)
        throws ResourceTypeNotFoundException {
        Set<Integer> uniqueIds = new HashSet<Integer>();
        uniqueIds.addAll(Arrays.asList(resourceIds));
        Integer[] ids = uniqueIds.toArray(new Integer[0]);
        ResourceType type = this.getResourceTypeById(subject, resourceTypeId);
        int count = resourceManager.getResourceCountByTypeAndIds(subject, type, ids);
        return (count == ids.length);
    }

    /**
     * Return which facets are available for the passed return type. This is e.g. used to determine which tabs (Monitor,
     * Inventory, ...) can be displayed for a resource of a certain type
     */
    public ResourceFacets getResourceFacets(Subject subject, int resourceTypeId) throws ResourceTypeNotFoundException {
        ResourceType resourceType = this.getResourceTypeById(subject, resourceTypeId);
        ResourceFacets resourceFacets = new ResourceFacets(!resourceType.getMetricDefinitions().isEmpty(), resourceType
            .getResourceConfigurationDefinition() != null, !resourceType.getOperationDefinitions().isEmpty(),
            !resourceType.getPackageTypes().isEmpty(), exposesCallTimeMetrics(resourceType));
        return resourceFacets;
    }

    private static boolean exposesCallTimeMetrics(ResourceType resourceType) {
        Set<MeasurementDefinition> measurementDefs = resourceType.getMetricDefinitions();
        for (MeasurementDefinition measurementDef : measurementDefs) {
            if (measurementDef.getDataType() == DataType.CALLTIME) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, ResourceTypeTemplateCountComposite> getTemplateCountCompositeMap() {
        Query templateCountQuery = entityManager.createNamedQuery(ResourceType.FIND_ALL_TEMPLATE_COUNT_COMPOSITES);
        List<ResourceTypeTemplateCountComposite> composites = templateCountQuery.getResultList();

        Map<Integer, ResourceTypeTemplateCountComposite> compositeMap = new HashMap<Integer, ResourceTypeTemplateCountComposite>();
        for (ResourceTypeTemplateCountComposite next : composites) {
            compositeMap.put(next.getType().getId(), next);
        }
        return compositeMap;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getResourceTypesByPlugin(String pluginName) {
        final String queryName = ResourceType.QUERY_FIND_BY_PLUGIN;

        Query query = entityManager.createNamedQuery(queryName);
        query.setParameter("plugin", pluginName);

        List<ResourceType> results = query.getResultList();
        return results;
    }
}