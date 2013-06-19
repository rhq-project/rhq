/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.InvalidExpressionException;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.group.ResourceGroupDeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionCreateException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionDeleteException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionNotFoundException;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionUpdateException;


/**
 * The remote interface to the SLSB GroupDefinitionManager.
 *
 * @author Jirka Kremser
 */
@Remote
public interface GroupDefinitionManagerRemote {

    /**
     * <p>Creates a new group definition.</p>
     * <p>The subject needs to have <code>MANAGE_INVENTORY</code> permission.</p>
     * 
     * @param subject the user who is asking create the group definition
     * @param newGroupDefinition the object defining the group definition
     * @return instance of <code>GroupDefinition</code>
     * @throws GroupDefinitionAlreadyExistsException
     * @throws GroupDefinitionCreateException
     */
    GroupDefinition createGroupDefinition(Subject subject, GroupDefinition newGroupDefinition)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionCreateException;

    /**
     * <p>Fetches the group definitions based on provided criteria.</p>
     * <p>The subject needs to have <code>MANAGE_INVENTORY</code> permission.</p>
     * 
     * @param subject the user who is asking to find the group definitions
     * @param criteria the criteria
     * @return instance of <code>GroupDefinition</code>
     */
    PageList<GroupDefinition> findGroupDefinitionsByCriteria(Subject subject, ResourceGroupDefinitionCriteria criteria);

    /**
     * <p>Deletes the given group definition.</p>
     * <p>The subject needs to have <code>MANAGE_INVENTORY</code> permission.</p>
     * 
     * @param subject the user who is asking to remove the group definition
     * @param groupDefinitionId the id of a group definition to be deleted
     * @throws GroupDefinitionNotFoundException
     * @throws GroupDefinitionDeleteException
     */
    void removeGroupDefinition(Subject subject, Integer groupDefinitionId) throws GroupDefinitionNotFoundException,
        GroupDefinitionDeleteException;

    /**
     * <p>Updates the given group definition.</p>
     * <p>The subject needs to have <code>MANAGE_INVENTORY</code> permission.</p>
     * 
     * @param subject the user who is asking to update the group definition
     * @param updated the object defining the group definition to be changed (based on its id)
     * @return the updated group definition
     * @throws GroupDefinitionAlreadyExistsException
     * @throws GroupDefinitionUpdateException
     * @throws InvalidExpressionException
     * @throws ResourceGroupUpdateException
     */
    GroupDefinition updateGroupDefinition(Subject subject, GroupDefinition updated)
        throws GroupDefinitionAlreadyExistsException, GroupDefinitionUpdateException, InvalidExpressionException,
        ResourceGroupUpdateException;

    /**
     * <p>Explicitly recalculates the group membership, depending on the GroupDefinition's expression.</p>
     * <p>The subject needs to have <code>MANAGE_INVENTORY</code> permission.</p>
     * 
     * @param subject the user who is asking to recalculate the group membership
     * @param groupDefinitionId the id of a group definition to be recalculated
     * @throws ResourceGroupDeleteException
     * @throws GroupDefinitionDeleteException
     * @throws GroupDefinitionNotFoundException
     * @throws InvalidExpressionException
     */
    void calculateGroupMembership(Subject subject, int groupDefinitionId) throws ResourceGroupDeleteException,
        GroupDefinitionDeleteException, GroupDefinitionNotFoundException, InvalidExpressionException;

}