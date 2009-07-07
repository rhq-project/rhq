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
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UpdateException;

/**
 * A manager that provides methods for creating, updating, deleting, and querying {@link ResourceGroup}s.
 *
 * @author Ian Springer
 */
@Local
public interface ResourceGroupManagerLocal {

    ResourceGroup updateResourceGroup(Subject user, ResourceGroup group, RecursivityChangeType changeType)
        throws ResourceGroupAlreadyExistsException, ResourceGroupUpdateException;

    ResourceGroup getResourceGroupById(Subject user, int id, GroupCategory category)
        throws ResourceGroupNotFoundException;

    int getResourceGroupCountByCategory(Subject subject, GroupCategory category);

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

    void setResourceType(int resourceGroupId);

    int getImplicitGroupMemberCount(int resourceGroupId);

    PageList<ResourceGroupComposite> findResourceGroupComposites(Subject subject, GroupCategory groupCategory,
        ResourceCategory resourceCategory, ResourceType resourceType, String nameFilter, Integer resourceId,
        Integer groupId, PageControl pc);

    List<Integer> findDeletedResourceGroupIds(int[] groupIds);

    void ensureMembershipMatches(Subject subject, int groupId, int[] resourceIds) throws ResourceGroupUpdateException;

    /*
     * Methods also in the remote interface
     */
    void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds) throws ResourceGroupNotFoundException,
        ResourceGroupUpdateException;

    ResourceGroup createResourceGroup(Subject user, ResourceGroup group) throws CreateException;

    void deleteResourceGroup(Subject user, int groupId) throws DeleteException;

    ResourceGroup getResourceGroup(Subject subject, int groupId) throws FetchException;

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

    PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc)
        throws FetchException;

    PageList<ResourceGroup> findResourceGroups(Subject subject, ResourceGroup criteria, PageControl pc)
        throws FetchException;

    PageList<ResourceGroupComposite> findResourceGroupComposites(Subject subject, ResourceGroup criteria, PageControl pc)
        throws FetchException;

    void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds) throws UpdateException;

    void setRecursive(Subject subject, int groupId, boolean isRecursive) throws UpdateException;

    ResourceGroup updateResourceGroup(Subject user, ResourceGroup group) throws UpdateException;
}