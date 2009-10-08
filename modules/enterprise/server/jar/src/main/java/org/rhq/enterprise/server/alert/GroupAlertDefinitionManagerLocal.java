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
    PageList<AlertDefinition> findGroupAlertDefinitions(Subject subject, int resourceGroupId, PageControl pageControl);

    // Create operation always cascades
    int createGroupAlertDefinitions(Subject subject, AlertDefinition alertDefinition, Integer resourceGroupId)
        throws InvalidAlertDefinitionException;

    // this is a system side-effect of group processing, and thus should only ever by called by the overlord user
    void updateAlertDefinitionsForResource(Subject subject, AlertDefinition alertTemplate, Integer resourceGroupId)
        throws AlertDefinitionCreationException, InvalidAlertDefinitionException;

    int removeGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds);

    int enableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds);

    int disableGroupAlertDefinitions(Subject subject, Integer[] groupAlertDefinitionIds);

    AlertDefinition updateGroupAlertDefinitions(Subject subject, AlertDefinition groupAlertDefinition,
        boolean purgeInternals) throws InvalidAlertDefinitionException;

    // methods requires to implement system side-effects as a result of modifying group membership or deleting groups
    void addGroupAlertDefinitions(Subject subject, int groupId, int[] resourcesIdsToAdd);

    void removeGroupAlertDefinitions(Subject subject, int groupId, int[] resourceIdsToRemove);

    void purgeAllGroupAlertDefinitions(Subject subject, int groupId);
}