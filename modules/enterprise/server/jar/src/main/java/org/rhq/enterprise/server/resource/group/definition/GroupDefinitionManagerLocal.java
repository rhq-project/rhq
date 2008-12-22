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

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionCreateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionUpdateException;
import org.rhq.enterprise.server.resource.group.definition.framework.ExpressionEvaluator;
import org.rhq.enterprise.server.resource.group.definition.framework.InvalidExpressionException;

@Local
public interface GroupDefinitionManagerLocal {

    void recalculateDynaGroups(Subject subject);

    GroupDefinition getById(int groupDefinitionId) throws GroupDefinitionNotFoundException;

    GroupDefinition createGroupDefinition(Subject subject, GroupDefinition newGroupDefinition)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionCreateException;

    GroupDefinition updateGroupDefinition(Subject subject, GroupDefinition updated)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionUpdateException, InvalidExpressionException,
        ResourceGroupUpdateException;

    void calculateGroupMembership(Subject subject, int groupDefinitionId) throws GroupDefinitionDeleteException,
        GroupDefinitionNotFoundException, InvalidExpressionException, ResourceGroupUpdateException;

    Integer calculateGroupMembership_helper(Subject subject, int groupDefinitionId, ExpressionEvaluator.Result result)
        throws GroupDefinitionNotFoundException, ResourceGroupUpdateException, GroupDefinitionNotFoundException;

    PageList<ResourceGroupComposite> getManagedResourceGroups(int groupDefinitionId, PageControl pc);

    PageList<GroupDefinition> getGroupDefinitions(PageControl pc);

    int getGroupDefinitionCount();

    int getAutoRecalculationGroupDefinitionCount();

    int getDynaGroupCount();

    void removeGroupDefinition(Subject subject, Integer groupDefinitionId) throws GroupDefinitionNotFoundException,
        GroupDefinitionDeleteException;

    void removeManagedResource_helper(Subject subject, int groupDefinitionId, Integer doomedGroupId)
        throws GroupDefinitionDeleteException, GroupDefinitionNotFoundException;
}