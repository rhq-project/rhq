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
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * A manager that provides methods for creating, updating, deleting, and querying
 * {@link org.rhq.core.domain.resource.ResourceType}s.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
@Local
public interface ResourceTypeManagerLocal extends ResourceTypeManagerRemote {
    // TODO: Add a getResourceTypeByResourceId method.

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

    /**
     * All this does is set the resource type's uninventoryMissing flag to the given boolean value.
     *
     * DO NOT USE THIS - THIS IS FOR INTERNAL USE ONLY.
     *
     * @param subject
     * @param resourceTypeId
     * @param uninventoryMissingFlag
     */
    void setResourceTypeUninventoryMissingFlag(Subject subject, int resourceTypeId, boolean uninventoryMissingFlag);

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

    List<String> getDuplicateTypeNames();

    ResourceType getResourceTypeByNameAndPlugin(String name, String plugin);

    HashMap<Integer, String> getResourceTypeDescendantsWithOperations(Subject subject, int resourceTypeId);

    List<ResourceType> getAllResourceTypeAncestors(Subject subject, int resourceTypeId);

    @Deprecated
    /**
     *
     * @param subject subject of the caller
     * @param resourceTypeId resource type to begin with
     * @return list of all {@link org.rhq.core.domain.resource.ResourceType}s of all descendants
     * @deprecated This method is not currently being used at all
     */
    List<ResourceType> getAllResourceTypeDescendants(Subject subject, int resourceTypeId);
}