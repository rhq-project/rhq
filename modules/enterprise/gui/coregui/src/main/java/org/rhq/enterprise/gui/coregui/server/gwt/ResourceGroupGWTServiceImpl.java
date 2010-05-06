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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;


/**
 * @author Greg Hinkle
 */
public class ResourceGroupGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceGroupGWTService {

    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
    private GroupDefinitionManagerLocal definitionManager = LookupUtil.getGroupDefinitionManager();


    public PageList<ResourceGroup> findResourceGroupsByCriteria(ResourceGroupCriteria criteria) {
        return SerialUtility.prepare(groupManager.findResourceGroupsByCriteria(getSessionSubject(), criteria),
                "ResourceGroupService.findResourceGroupsByCriteria");
    }

    // TODO GH: build and expose findResourceGroupCompositesByCriteria

    public PageList<GroupDefinition> findGroupDefinitionsByCriteria(ResourceGroupDefinitionCriteria criteria) {
        return SerialUtility.prepare(definitionManager.findGroupDefinitionsByCriteria(getSessionSubject(), criteria),
                "ResourceGroupService.findGroupDefinitionsByCriteria");
    }

    public void ensureMembershipMatches(int groupId, int[] resourceIds) {
        groupManager.ensureMembershipMatches(getSessionSubject(), groupId, resourceIds);
    }

    public ResourceGroup createResourceGroup(ResourceGroup group, int[] resourceIds) {
        group = groupManager.createResourceGroup(getSessionSubject(), group);

        ensureMembershipMatches(group.getId(), resourceIds);

        return SerialUtility.prepare(group, "ResourceGroupService.createResourceGroup");
    }

    public void deleteResourceGroup(int groupId) {
        try {
            groupManager.deleteResourceGroup(getSessionSubject(), groupId);
        } catch (ResourceGroupDeleteException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}