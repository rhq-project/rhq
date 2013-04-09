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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;

/**
 * A manager that provides methods for creating, updating, deleting, and querying
 * {@link org.rhq.core.domain.resource.ResourceType}s.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
@Local
public interface ResourceTypeManagerLocal {
    // TODO: Add a getResourceTypeByResourceId method.

    /**
     * Given a specific resource type ID, this will indicate if that type (and resources of that type)
     * are to be ignored or not. If the type is to be ignored (ignoreFlag == true), then all resources
     * that are already in inventory of that type will be uninventoried.
     * 
     * @param subject user making the request
     * @param resourceTypeId the type to change
     * @param ignoreFlag true if the type (and resources of that type) are to be ignored.
     */
    void setResourceTypeIgnoreFlagAndUninventoryResources(Subject subject, int resourceTypeId, boolean ignoreFlag);

    /**
     * All this does is set the resource type's ignore flag to the given boolean value.
     *
     * DO NOT USE THIS - THIS IS FOR INTERNAL USE ONLY.
     * You must use {@link #setResourceTypeIgnoreFlagAndUninventoryResources(Subject, int, boolean)}.
     *
     * @param subject
     * @param resourceTypeId
     * @param ignoreFlag
     */
    void setResourceTypeIgnoreFlag(Subject subject, int resourceTypeId, boolean ignoreFlag);

    ResourceType getResourceTypeById(Subject subject, int id) throws ResourceTypeNotFoundException;

    /**
     * @return the resource type by name and plugin or null if the type is not found
     */
    ResourceType getResourceTypeByNameAndPlugin(String name, String plugin);

    /**
     * Gets the list of resource types that are children of the specified resource type and that are viewable by the
     * specified user.
     *
     * @param  subject an authz subject
     * @param  parent  a resource type
     *
     * @return the list of resource types that are children of the specified resource type and that are viewable by the
     *         specified user
     */
    // TODO: Use PageList/PageControl.
    List<ResourceType> getChildResourceTypes(Subject subject, ResourceType parent);

    /**
     * Gets the list of resource types that are children of the specified resource type, that are in the specified
     * resource category, and that are viewable by the specified user.
     *
     * @param  subject          an authz subject
     * @param  parentResource   the parent resource
     * @param  resourceCategory a resource category
     *
     * @return the list of resource types that are children of the specified resource type, that are in the specified
     *         resource category, and that are viewable by the specified user
     */
    // TODO: Use PageList/PageControl?
    List<ResourceType> getChildResourceTypesByCategory(Subject subject, Resource parentResource,
        ResourceCategory resourceCategory);

    List<ResourceType> getUtilizedChildResourceTypesByCategory(Subject subject, Resource parentResource,
        ResourceCategory resourceCategory);

    List<ResourceType> getUtilizedResourceTypesByCategory(Subject subject, ResourceCategory category, String nameFilter);

    List<String> getUtilizedResourceTypeNamesByCategory(Subject subject, ResourceCategory category, String nameFilter,
        String pluginName);

    List<ResourceType> getResourceTypesForCompatibleGroups(Subject subject, String pluginName);

    Map<String, Integer> getResourceTypeCountsByGroup(Subject subject, ResourceGroup group, boolean recursive);

    boolean ensureResourceType(Subject subject, Integer resourceTypeId, Integer[] resourceIds)
        throws ResourceTypeNotFoundException;

    /**
     * Return which facets are available for the passed return type. This is e.g. used to determine which tabs (Monitor,
     * Inventory, ...) can be displayed for a resource of a certain type
     */
    ResourceFacets getResourceFacets(int resourceTypeId);

    void reloadResourceFacetsCache();

    /**
     * Obtain ResourceTypes that match a given category or all if category is null. Note that the caller needs to have
     * Permission.MANAGE_SETTING in order to successfully call this method.
     *
     * @param  subject  subject of the caller
     * @param  category the category to check for. If this is null, entries from all cateories will be returned.
     *
     * @return a List of ResourceTypes
     *
     * @see    Permission
     * @see    ResourceCategory
     */
    List<ResourceType> getAllResourceTypesByCategory(Subject subject, ResourceCategory category);

    /**
     * Return all ResourceTypes that are children of the passed ones
     *
     * @param  types List of ResourceTypes
     *
     * @return SortedSet of ResourceTypes. If nothing is found, then this set is empty
     */
    Map<Integer, SortedSet<ResourceType>> getChildResourceTypesForResourceTypes(List<ResourceType> types);

    Map<Integer, ResourceTypeTemplateCountComposite> getTemplateCountCompositeMap();

    List<ResourceType> getResourceTypesByPlugin(String pluginName);

    List<Integer> getResourceTypeIdsByPlugin(String plugin);

    Integer getResourceTypeCountByPlugin(String plugin);

    PageList<ResourceType> findResourceTypesByCriteria(Subject subject, ResourceTypeCriteria criteria);

    List<String> getDuplicateTypeNames();

    ResourceType getResourceTypeByNameAndPlugin(Subject subject, String name, String plugin);

    HashMap<Integer, String> getResourceTypeDescendantsWithOperations(Subject subject, int resourceTypeId);

    List<ResourceType> getAllResourceTypeAncestors(Subject subject, int resourceTypeId);

    List<ResourceType> getAllResourceTypeDescendants(Subject subject, int resourceTypeId);
}