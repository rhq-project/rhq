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
package org.rhq.coregui.server.gwt;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.coregui.client.gwt.GroupAlertDefinitionGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.alert.GroupAlertDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class GroupAlertDefinitionGWTServiceImpl extends AbstractGWTServiceImpl implements
    GroupAlertDefinitionGWTService {
    private static final long serialVersionUID = 1L;

    private GroupAlertDefinitionManagerLocal groupAlertDefManager = LookupUtil.getGroupAlertDefinitionManager();

    @Override
    public int createGroupAlertDefinitions(AlertDefinition groupAlertDefinition, Integer resourceGroupId)
        throws RuntimeException {
        try {
            int results = groupAlertDefManager.createGroupAlertDefinitions(getSessionSubject(), groupAlertDefinition,
                resourceGroupId);
            return results;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public AlertDefinition updateGroupAlertDefinitions(AlertDefinition groupAlertDefinition, boolean purgeInternals)
        throws RuntimeException {
        try {
            AlertDefinition results = groupAlertDefManager.updateGroupAlertDefinitions(getSessionSubject(),
                groupAlertDefinition, purgeInternals);
            return SerialUtility.prepare(results, "updateGroupAlertDefinitions");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}