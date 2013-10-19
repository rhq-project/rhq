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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Jay Shaughnessy
 */
@Remote
public interface ResourceGroupManagerRemote {

    /**
     * @param subject
     * @param groupId
     * @param resourceIds
     */
    void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds);

    /**
     * @param subject
     * @param resourceGroup
     * @return The new ResourceGroup
     * @throws ResourceGroupAlreadyExistsException
     */
    ResourceGroup createResourceGroup(Subject subject, ResourceGroup resourceGroup);

    /**
     * @param subject
     * @param groupId
     * @throws ResourceGroupNotFoundException
     * @throws ResourceGroupDeleteException
     */
    void deleteResourceGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException;

    /**
     * @param subject
     * @param groupIds
     * @throws ResourceGroupNotFoundException
     * @throws ResourceGroupDeleteException
     */
    void deleteResourceGroups(Subject subject, int[] groupIds) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException;

    /**
     * @param subject
     * @param groupId
     * @return the resource group
     * @throws ResourceGroupNotFoundException
     */
    ResourceGroup getResourceGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException;

    /**
     * @param subject
     * @param groupId
     * @return the composite
     * @throws ResourceGroupNotFoundException
     */
    ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId)
        throws ResourceGroupNotFoundException;

    /**
     * @param subject
     * @param roleId
     * @param pc
     * @return not null
     */
    PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc);

    /**
     * @param subject
     * @param groupId
     * @param resourceIds
     */
    void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds);

    /**
     * @param subject
     * @param groupId
     * @param isRecursive
     */
    void setRecursive(Subject subject, int groupId, boolean isRecursive);

    /**
     * @param subject
     * @param newResourceGroup
     * @return the updated resource group
     */
    ResourceGroup updateResourceGroup(Subject subject, ResourceGroup newResourceGroup);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<ResourceGroup> findResourceGroupsByCriteria(Subject subject, ResourceGroupCriteria criteria);

}
