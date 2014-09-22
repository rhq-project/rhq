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
package org.rhq.enterprise.server.resource.group;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * A manager that provides methods for creating, updating, deleting, and querying {@link ResourceGroup}s.
 *
 * @author Ian Springer
 */
@Local
public interface ResourceGroupManagerLocal {

    /**
     * NOTE: This is only used to support AutoGroups currently but the idea may be expanded in the future.
     *
     * Creates a private, "user-owned" (aka "subject-owned") resource group.  This group differs from a role-owned
     * group in that it can not be assigned to a role, instead it is owned by the user that creates it.  It comprises
     * only a set of resources for which the user has, minimally, view permission.  Since a user's view permissions
     * can change, membership and authz checking must be performed as needed when taking any action on the group.
     * <br/><br/>
     * A user does not need MANAGE_INVENTORY to create a sub-group because it is private to his view.
     *  <br/><br/>
     * This call does not populate the group with members, it only creates the group. The group is automatically set
     * to be non-recursive.
     * <br/><br/>
     * All user-owned groups are deleted if the the user is deleted.
     *
     * @param user The user for which the group will be created.
     * @param group The group characteristics. Any membership defined here is ignored. The recursivity setting is
     * ignored.
     * @return The new group.
     */
    ResourceGroup createPrivateResourceGroup(Subject user, ResourceGroup group);

    ResourceGroup getResourceGroupById(Subject user, int id, GroupCategory category)
        throws ResourceGroupNotFoundException;

    /**
     * Get a summary of counts, by category, of the user's assigned, visible groups.
     *
     * @param user
     * @return A 2 element int array with counts for mixed, compatible as a[0], a[1] respectively.
     */
    int[] getResourceGroupCountSummary(Subject user);

    void enableRecursivityForGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupUpdateException;

    void removeAllResourcesFromGroup(Subject subject, int groupId) throws ResourceGroupDeleteException;

    PageList<ResourceGroup> findAvailableResourceGroupsForRole(Subject subject, int roleId, int[] excludeIds,
        PageControl pageControl);

    PageList<ResourceGroup> findResourceGroupByIds(Subject subject, int[] resourceGroupIds, PageControl pageControl);

    void updateImplicitGroupMembership(Subject subject, Resource resource);

    List<Resource> findResourcesForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId);

    List<Resource> findResourcesForResourceGroup(Subject subject, int groupId, GroupCategory category);

    /**
     * Return the {@link MeasurementDefinition}s for the passed comatible group
     *
     * @param  subject {@link Subject} of the calling user
     * @param  groupId                id of the group
     * @param  displayTypeSummaryOnly TODO
     *
     * @return a set of Definitions, which is empty for an invalid groupId
     */
    int[] findDefinitionsForCompatibleGroup(Subject subject, int groupId, boolean displayTypeSummaryOnly);

    /**
     * Get the {@link MeasurementDefinition}s for the passed autogroup
     *
     * @param  subject {@link Subject} of the calling user
     * @param  autoGroupParentResourceId    id of the parent resource
     * @param  autoGroupChildResourceTypeId Id of the {@link ResourceType} of the children
     * @param  displayTypeSummaryOnly       TODO
     *
     * @return a set of Definitions which is empty for an invalid autoGroupChildResourceType
     */
    int[] findDefinitionsForAutoGroup(Subject subject, int autoGroupParentResourceId, int autoGroupChildResourceTypeId,
        boolean displayTypeSummaryOnly);

    ResourceGroup getByGroupDefinitionAndGroupByClause(int groupDefinitionId, String groupByClause);

    void setResourceTypeInNewTx(int resourceGroupId) throws ResourceGroupDeleteException;

    int getExplicitGroupMemberCount(int resourceGroupId);

    int getImplicitGroupMemberCount(int resourceGroupId);

    PageList<ResourceGroupComposite> findResourceGroupComposites(Subject subject, GroupCategory groupCategory,
        ResourceCategory resourceCategory, String resourceTypeName, String pluginName, String nameFilter,
        Integer resourceId, Integer groupId, PageControl pc);

    PageList<ResourceGroupComposite> findResourceGroupCompositesByCriteria(Subject subject,
        ResourceGroupCriteria criteria);

    List<Integer> findDeletedResourceGroupIds(int[] groupIds);

    /**
     * This method ensures that the explicit group membership is set to the specified resources.  Members
     * will be added or removed as necessary.  Make sure you pass the correct value for the <setType>
     * parameter.
     * <br/><br/>
     * For global groups requires MANAGE_INVENTORY. For private groups requires VIEW permission on all specified
     * resources.
     *
     * @param subject
     * @param groupId
     * @param resourceIds
     * @param setType Set to false if the specified resourceIds will not alter the group type (compatible or
     * mixed). Set true to have the group type (re)set automatically, based on the new group membership.
     * @throws ResourceGroupUpdateException
     * @throws ResourceGroupDeleteException
     */
    void setAssignedResources(Subject subject, int groupId, int[] resourceIds, boolean setType)
        throws ResourceGroupUpdateException, ResourceGroupDeleteException;

    /**
     * This method ensures that the resource will have exactly the specified set of explicit group
     * membership. Make sure you pass the correct value for the <setType> parameter.
     *
     * @param subject
     * @param resourceId
     * @param resourceGroupIds
     * @param setType Set to false if addition or removal of the specified resourceId will not alter the group
     *        type for the specified resource groups (compatible or mixed). Set true to have the group type
     *        (re)set automatically, based on the new group membership.
     * @throws ResourceGroupUpdateException
     * @throws ResourceGroupDeleteException
     */
    void setAssignedResourceGroupsForResource(Subject subject, int groupId, int[] resourceIds, boolean setType)
        throws ResourceGroupUpdateException, ResourceGroupDeleteException;

    void uninventoryMembers(Subject subject, int groupId);

    ResourceGroup updateResourceGroup(Subject user, ResourceGroup group, RecursivityChangeType changeType)
        throws ResourceGroupAlreadyExistsException, ResourceGroupUpdateException;

    ResourceGroup updateResourceGroup(Subject user, ResourceGroup group, RecursivityChangeType changeType,
        boolean updateMembership) throws ResourceGroupAlreadyExistsException, ResourceGroupUpdateException;

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds);

    ResourceGroup createResourceGroup(Subject user, ResourceGroup group);

    void deleteResourceGroup(Subject user, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException;

    void deleteResourceGroups(Subject user, int[] groupIds) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException;

    ResourceGroup getResourceGroup(Subject subject, int groupId);

    ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId);

    PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc);

    void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds);

    void setRecursive(Subject subject, int groupId, boolean isRecursive);

    ResourceGroup updateResourceGroup(Subject subject, ResourceGroup group);

    PageList<ResourceGroup> findResourceGroupsByCriteria(Subject subject, ResourceGroupCriteria criteria);
}