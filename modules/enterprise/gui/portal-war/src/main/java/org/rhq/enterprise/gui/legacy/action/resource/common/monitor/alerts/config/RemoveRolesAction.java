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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that removes role notifications for an alert definition.
 */
public class RemoveRolesAction extends RemoveNotificationsAction {
    Log log = LogFactory.getLog(RemoveRolesAction.class);

    @Override
    protected int handleRemove(Subject subject, RemoveNotificationsForm removeForm) throws Exception {
        Integer[] roleNotificationIds = removeForm.getRoles();
        Integer alertDefinitionId = removeForm.getAd();

        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        return alertNotificationManager.removeNotifications(subject, alertDefinitionId, roleNotificationIds, removeForm
            .isAlertTemplate());
    }
}