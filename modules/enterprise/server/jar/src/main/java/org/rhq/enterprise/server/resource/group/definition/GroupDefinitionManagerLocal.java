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
package org.rhq.enterprise.server.resource.group.definition;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.resource.group.definition.framework.ExpressionEvaluator;

@Local
public interface GroupDefinitionManagerLocal extends GroupDefinitionManagerRemote {

    void recalculateDynaGroups(Subject subject);

    GroupDefinition getById(int groupDefinitionId) throws GroupDefinitionNotFoundException;

    Integer calculateGroupMembership_helper(Subject subject, int groupDefinitionId, ExpressionEvaluator.Result result)
        throws ResourceGroupDeleteException, GroupDefinitionNotFoundException, GroupDefinitionNotFoundException;

    PageList<GroupDefinition> getGroupDefinitions(Subject subject, PageControl pc);

    int getGroupDefinitionCount(Subject subject);

    int getAutoRecalculationGroupDefinitionCount(Subject subject);

    int getDynaGroupCount(Subject subject);

    void removeManagedResource_helper(Subject subject, int groupDefinitionId, Integer doomedGroupId)
        throws GroupDefinitionDeleteException, GroupDefinitionNotFoundException;

    void updateGroupsByCannedExpressions(String plugin, List<CannedGroupExpression> expressions);

}