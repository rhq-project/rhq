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
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.event.alert.notification.RoleNotification;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewDefinitionNotificationsRolesAction extends ViewDefinitionNotificationsAction<RoleNotification> {
    private Log log = LogFactory.getLog(ViewDefinitionNotificationsRolesAction.class);

    @Override
    protected PageList<RoleNotification> getPageList(Subject subject, AlertDefinition alertDefinition,
        PageControl pageControl) {
        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        PageList<RoleNotification> notifications = alertNotificationManager.getRoleNotifications(alertDefinition
            .getId(), pageControl);

        return notifications;
    }
}