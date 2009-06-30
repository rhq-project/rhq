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
package org.rhq.enterprise.server.resource.group;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.CreateException;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.UpdateException;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * A manager that provides methods for creating, updating, deleting, and querying {@link ResourceGroup}s.
 *
 * @author Ian Springer
 */
@Local
public interface ResourceGroupManagerLocal {
    ResourceGroup createResourceGroup(Subject user, ResourceGroup group) throws CreateException;

    ResourceGroup updateResourceGroup(Subject user, ResourceGroup group, RecursivityChangeType changeType)
        throws ResourceGroupAlreadyExistsException, ResourceGroupUpdateException;

    ResourceGroup updateResourceGroup(Subject user, ResourceGroup group) throws UpdateException;

    void deleteResourceGroup(Subject user, int groupId) throws DeleteException;

    ResourceGroup getResourceGroupById(Subject user, int id, GroupCategory category)
        throws ResourceGroupNotFoundException;

    int getResourceGroupCountByCategory(Subject subject, GroupCategory category);

    void enableRecursivityForGroup(Subject subject, Integer groupId) throws ResourceGroupNotFoundException,
        ResourceGroupUpdateException;

    void addResourcesToGroup(Subject subject, Integer groupId, Integer[] resourceIds)
        throws ResourceGroupNotFoundException, ResourceGroupUpdateException;

    void removeResourcesFromGroup(Subject subject, Integer groupId, Integer[] resourceIds)
        throws ResourceGroupUpdateException;

    void removeAllResourcesFromGroup(Subject subject, Integer groupId) throws ResourceGroupDeleteException;

    PageList<ResourceGroup> getAvailableResourceGroupsForRole(Subject subject, Integer roleId, Integer[] excludeIds,
        PageControl pageControl);

    PageList<ResourceGroup> getResourceGroupByIds(Subject subject, Integer[] resourceGroupIds, PageControl pageControl);

    void updateImplicitGroupMembership(Subject subject, Resource resource);

    List<Resource> getResourcesForAutoGroup(Subject subject, int autoGroupParentResourceId,
        int autoGroupChildResourceTypeId);

    List<Resource> getResourcesForResourceGroup(Subject subject, int groupId, GroupCategory category);

    /**
     * Return the {@link MeasurementDefinition}s for the passed comatible group
     *
     * @param  subject {@link Subject} of the calling user
     * @param  groupId                id of the group
     * @param  displayTypeSummaryOnly TODO
     *
     * @return a set of Definitions, which is empty for an invalid groupId
     */
    int[] getDefinitionsForCompatibleGroup(Subject subject, int groupId, boolean displayTypeSummaryOnly);

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
    int[] getDefinitionsForAutoGroup(Subject subject, int autoGroupParentResourceId, int autoGroupChildResourceTypeId,
        boolean displayTypeSummaryOnly);

    /**
     * Get the ResourceGroup and Availability by id.
     *
     * @param  subject {@link Subject} of the calling user
     * @param  groupId id to search by
     *
     * @return ResourceGroupComposite composite object with the ResourceGroup and availability, as well as the count of
     *         resources in the group
     */
    ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId);

    ResourceGroup findByGroupDefinitionAndGroupByClause(int groupDefinitionId, String groupByClause);

    void setResourceType(int resourceGroupId) throws ResourceTypeNotFoundException;

    int getImplicitGroupMemberCount(int resourceGroupId);

    PageList<ResourceGroupComposite> getResourceGroupsFiltered(Subject subject, GroupCategory groupCategory,
        ResourceCategory resourceCategory, ResourceType resourceType, String nameFilter, Integer resourceId,
        Integer groupId, PageControl pc);

    List<Integer> getDeletedResourceGroupIds(List<Integer> groupIds);

    void ensureMembershipMatches(Subject subject, Integer groupId, List<Integer> resourceIds)
        throws ResourceGroupUpdateException;
}