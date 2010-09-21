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
package org.rhq.enterprise.gui.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
@RemoteServiceRelativePath("ResourceGroupGWTService")
public interface ResourceGroupGWTService extends RemoteService {

    GroupDefinition createGroupDefinition(GroupDefinition groupDefinition);

    ResourceGroup createResourceGroup(ResourceGroup group, int[] resourceIds);

    void deleteGroupDefinitions(int[] groupDefinitionIds);

    void deleteResourceGroups(int[] groupIds);

    PageList<GroupDefinition> findGroupDefinitionsByCriteria(ResourceGroupDefinitionCriteria criteria);

    PageList<ResourceGroup> findResourceGroupsByCriteria(ResourceGroupCriteria criteria);

    PageList<ResourceGroupComposite> findResourceGroupCompositesByCriteria(ResourceGroupCriteria criteria);

    void recalculateGroupDefinitions(int[] groupDefinitionIds);

    void setMembership(int groupId, int[] resourceIds, boolean setType);

    void updateGroupDefinition(GroupDefinition groupDefinition);

    void updateResourceGroup(ResourceGroup group);

    void updateResourceGroup(ResourceGroup group, boolean updateMembership);
}