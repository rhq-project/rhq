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
package org.rhq.enterprise.server.alert;

import javax.ejb.Local;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 */
@Local
public interface GroupAlertDefinitionManagerLocal {
    @Deprecated
    // remove along with portal war
    PageList<AlertDefinition> findGroupAlertDefinitions(Subject subject, int resourceGroupId, PageControl pageControl);

    int createGroupAlertDefinitions(Subject subject, AlertDefinition alertDefinition, Integer resourceGroupId)
        throws InvalidAlertDefinitionException, AlertDefinitionCreationException;

    /**
     * INTERNAL-USE-ONLY No permission checking performed. Ids assumed to be group def ids.  Use
     * {@link AlertDefinitionManagerRemote#removeAlertDefinitions(Subject, int[])} for full authz and
     * handling of different levels of alert def types.
     *
     * @param subject
     * @param groupAlertDefinitionIds
     * @return
     */
    int removeGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds);

    /**
     * INTERNAL-USE-ONLY No permission checking performed. Ids assumed to be group def ids.  Use
     * {@link AlertDefinitionManagerRemote#enableAlertDefinitions(Subject, int[])} for full authz and
     * handling of different levels of alert def types.
     *
     * @param subject
     * @param groupAlertDefinitionIds
     * @return
     */
    int enableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds);

    /**
     * INTERNAL-USE-ONLY No permission checking performed. Ids assumed to be group def ids.  Use
     * {@link AlertDefinitionManagerRemote#disableAlertDefinitions(Subject, int[])} for full authz and
     * handling of different levels of alert def types.
     *
     * @param subject
     * @param groupAlertDefinitionIds
     * @return
     */
    int disableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds);

    /**
     * @param subject
     * @param groupAlertDefinition
     * @param resetMatching Incur the overhead of resetting any partial alert matching that has taken place. This *must*
     * be set true if updating conditions, dampening rules or the conditionExpressin (ANY vs ALL).  If in doubt, set to true
     * as the loss of partial matching is better than corrupted matching.
     * @return
     * @throws InvalidAlertDefinitionException
     * @throws AlertDefinitionUpdateException
     */
    AlertDefinition updateGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        boolean resetMatching) throws InvalidAlertDefinitionException, AlertDefinitionUpdateException;

    // required to implement system side-effects as a result of modifying group membership or deleting groups
    void addGroupMemberAlertDefinitions(Subject subject, int groupId, int[] resourcesIdsToAdd)
        throws AlertDefinitionCreationException;

    void removeGroupMemberAlertDefinitions(Subject subject, int groupId, Integer[] resourceIdsToRemove);

    void purgeAllGroupAlertDefinitions(Subject subject, int groupId);
}