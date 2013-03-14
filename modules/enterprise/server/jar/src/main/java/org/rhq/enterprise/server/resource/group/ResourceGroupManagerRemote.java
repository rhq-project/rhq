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

    void addResourcesToGroup(Subject subject, int groupId, int[] resourceIds);

    /**
     * @param subject
     * @param resourceGroup
     * @return The new ResourceGroup
     * @throws ResourceGroupAlreadyExistsException
     */
    ResourceGroup createResourceGroup(Subject subject, ResourceGroup resourceGroup);

    void deleteResourceGroup(Subject subject, int groupId) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException;

    void deleteResourceGroups(Subject subject, int[] groupIds) throws ResourceGroupNotFoundException,
        ResourceGroupDeleteException;

    ResourceGroup getResourceGroup(Subject subject, int groupId);

    ResourceGroupComposite getResourceGroupComposite(Subject subject, int groupId);

    PageList<ResourceGroup> findResourceGroupsForRole(Subject subject, int roleId, PageControl pc);

    void removeResourcesFromGroup(Subject subject, int groupId, int[] resourceIds);

    void setRecursive(Subject subject, int groupId, boolean isRecursive);

    ResourceGroup updateResourceGroup(Subject subject, ResourceGroup newResourceGroup);

    PageList<ResourceGroup> findResourceGroupsByCriteria(Subject subject, ResourceGroupCriteria criteria);

}
