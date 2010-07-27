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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.enterprise.server.util.QueryUtility;

/**
 * A manager that provides methods for creating, updating, deleting, and querying
 * {@link org.rhq.core.domain.resource.ResourceType}s.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
@Stateless
public class ResourceTypeManagerBean implements ResourceTypeManagerLocal, ResourceTypeManagerRemote {
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

    // remote
    public ResourceType getResourceTypeByNameAndPlugin(Subject subject, String name, String plugin) {
        return getResourceTypeByNameAndPlugin(name, plugin);
    }

    // local
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
        Query query = null;

        query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_CHILDREN);
        query.setParameter("resourceTypeId", parent.getId());

        List<ResourceType> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getChildResourceTypesByCategory(Subject subject, Resource parentResource,
        ResourceCategory category) {

        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_CHILDREN_BY_CATEGORY_admin);
        } else {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_CHILDREN_BY_CATEGORY);
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
        Query query;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_UTILIZED_CHILDREN_BY_CATEGORY_admin);
        } else {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_UTILIZED_CHILDREN_BY_CATEGORY);
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

    public List<String> getUtilizedResourceTypeNamesByCategory(Subject subject, ResourceCategory category,
        String nameFilter, String pluginName) {

        List<ResourceType> types = getUtilizeTypes_helper(subject, category, nameFilter, pluginName);
        List<String> typeNames = new ArrayList<String>();
        for (ResourceType type : types) {
            if (typeNames.contains(type.getName()) == false) {
                typeNames.add(type.getName());
            }
        }
        return typeNames;
    }

    public List<ResourceType> getUtilizedResourceTypesByCategory(Subject subject, ResourceCategory category,
        String nameFilter) {
        List<ResourceType> types = getUtilizeTypes_helper(subject, category, nameFilter, null);
        return types;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceType> getUtilizeTypes_helper(Subject subject, ResourceCategory category, String nameFilter,
        String pluginName) {

        Query query = null;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_UTILIZED_BY_CATEGORY_admin);
        } else {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_UTILIZED_BY_CATEGORY);
            query.setParameter("subject", subject);
        }

        query.setParameter("category", category);
        query.setParameter("nameFilter", QueryUtility.formatSearchParameter(nameFilter));
        query.setParameter("escapeChar", QueryUtility.getEscapeCharacter());
        query.setParameter("pluginName", pluginName);
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
    public List<ResourceType> getResourceTypesForCompatibleGroups(Subject subject, String pluginName) {
        Query query = null;
        if (authorizationManager.isInventoryManager(subject)) {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_RESOURCE_GROUP_admin);
        } else {
            query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_BY_RESOURCE_GROUP);
            query.setParameter("subject", subject);
        }
        query.setParameter("pluginName", pluginName);

        List<ResourceType> results = query.getResultList();
        return results;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getResourceTypeCountsByGroup(Subject subject, ResourceGroup group, boolean recursive) {
        final String queryName = recursive ? ResourceType.QUERY_GET_IMPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP
            : ResourceType.QUERY_GET_EXPLICIT_RESOURCE_TYPE_COUNTS_BY_GROUP;
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
        int[] ids = ArrayUtils.unwrapCollection(uniqueIds);
        ResourceType type = this.getResourceTypeById(subject, resourceTypeId);
        int count = resourceManager.getResourceCountByTypeAndIds(subject, type, ids);
        return (count == ids.length);
    }

    /**
     * Return which facets are available for the passed return type. This is e.g. used to determine which tabs (Monitor,
     * Inventory, ...) can be displayed for a resource of a certain type
     */
    @SuppressWarnings("unchecked")
    public ResourceFacets getResourceFacets(int resourceTypeId) {
        ResourceFacets cachedFacet = ResourceFacetsCache.getSingleton().getResourceFacets(resourceTypeId);
        if (cachedFacet != null) {
            return cachedFacet;
        }
        // be paranoid and fallback to getting the results directly from the database
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_RESOURCE_FACETS);
        query.setParameter("resourceTypeId", resourceTypeId);
        List<ResourceFacets> facets = query.getResultList();
        if (facets.size() != 1) {
            return new ResourceFacets(resourceTypeId, false, false, false, false, false, false, false, false);
        }
        return facets.get(0);
    }

    @SuppressWarnings("unchecked")
    public void reloadResourceFacetsCache() {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_RESOURCE_FACETS);
        query.setParameter("resourceTypeId", null);
        List<ResourceFacets> facets = query.getResultList();

        ResourceFacetsCache.getSingleton().reload(facets);
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

    @SuppressWarnings("unchecked")
    public PageList<ResourceType> findResourceTypesByCriteria(Subject subject, ResourceTypeCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        ;

        CriteriaQueryRunner<ResourceType> queryRunner = new CriteriaQueryRunner(criteria, generator, entityManager);
        return queryRunner.execute();
    }

    @SuppressWarnings("unchecked")
    public List<String> getDuplicateTypeNames() {
        Query query = entityManager.createNamedQuery(ResourceType.QUERY_FIND_DUPLICATE_TYPE_NAMES);
        List<String> results = query.getResultList();
        return results;
    }

    public List<ResourceType> getResourceTypeAncestorsWithOperations(Subject subject, int resourceTypeId) {
        List<ResourceType> types = getAllResourceTypeAncestors(subject, resourceTypeId);
        List<ResourceType> results = excludeThoseWithoutOperations(types);
        return results;
    }

    public List<ResourceType> getResourceTypeDescendantsWithOperations(Subject subject, int resourceTypeId) {
        List<ResourceType> types = getAllResourceTypeDescendants(subject, resourceTypeId);
        List<ResourceType> results = excludeThoseWithoutOperations(types);
        return results;
    }

    private List<ResourceType> excludeThoseWithoutOperations(List<ResourceType> types) {
        List<ResourceType> results = new ArrayList<ResourceType>();
        for (ResourceType next : types) {
            if (next.getOperationDefinitions() != null && next.getOperationDefinitions().size() != 0) {
                results.add(next);
            }
        }
        return results;
    }

    public List<ResourceType> getAllResourceTypeAncestors(Subject subject, int resourceTypeId) {
        Set<ResourceType> uniqueTypes = new HashSet<ResourceType>();
        Stack<ResourceType> toProcess = new Stack<ResourceType>();
        toProcess.add(entityManager.find(ResourceType.class, resourceTypeId));

        boolean sawTopLevelServer = false;
        while (toProcess.size() > 0) {
            ResourceType next = toProcess.pop();
            Set<ResourceType> parentTypes = next.getParentResourceTypes();
            if (parentTypes != null && parentTypes.size() != 0) {
                toProcess.addAll(parentTypes);
            } else {
                if (next.getCategory() == ResourceCategory.SERVER) {
                    sawTopLevelServer = true;
                }
            }
            uniqueTypes.add(next);
        }

        List<ResourceType> results = new ArrayList<ResourceType>(uniqueTypes);

        if (sawTopLevelServer) {
            ResourceTypeCriteria criteria = new ResourceTypeCriteria();
            criteria.addFilterCategory(ResourceCategory.PLATFORM);
            List<ResourceType> platforms = findResourceTypesByCriteria(subject, criteria);
            results.addAll(platforms);
        }

        Collections.sort(results);
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<ResourceType> getAllResourceTypeDescendants(Subject subject, int resourceTypeId) {
        ResourceType first = entityManager.find(ResourceType.class, resourceTypeId);

        if (first.getCategory() == ResourceCategory.PLATFORM) {
            ResourceTypeCriteria criteria = new ResourceTypeCriteria();
            List<ResourceType> allResourceTypes = findResourceTypesByCriteria(subject, criteria);

            List<ResourceType> results = new ArrayList<ResourceType>();
            for (ResourceType nextType : allResourceTypes) {
                if (nextType.getCategory() != ResourceCategory.PLATFORM) {
                    results.add(nextType);
                }
            }
            Collections.sort(results);
            return results;
        }

        Set<ResourceType> uniqueTypes = new HashSet<ResourceType>();
        Stack<ResourceType> toProcess = new Stack<ResourceType>();
        toProcess.add(first);

        Query findChildrenQuery = entityManager.createNamedQuery(ResourceType.FIND_CHILDREN_BY_PARENT);
        while (toProcess.size() > 0) {
            ResourceType next = toProcess.pop();

            findChildrenQuery.setParameter("resourceType", Arrays.asList(next));
            List<ResourceType> childTypes = findChildrenQuery.getResultList();

            if (childTypes != null) {
                toProcess.addAll(childTypes);
            }
            uniqueTypes.add(next);
        }

        List<ResourceType> results = new ArrayList<ResourceType>(uniqueTypes);
        Collections.sort(results);
        return results;
    }

}
