/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert.common;

import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Scope(ScopeType.EVENT)
@Name("alertNotificationStoreUIBean")
public class AlertNotificationStoreUIBean {

    @RequestParameter("context")
    private String context;

    @RequestParameter("contextId")
    private Integer contextId;

    private AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();

    public List<AlertNotification> lookupNotifications(Subject subject) {
        System.out.println("AlertNotificationStore: lookupNotifications context = " + context);
        System.out.println("AlertNotificationStore: lookupNotifications contextId = " + contextId);

        if (context.equals("template")) {
            return this.alertNotificationManager.getNotificationsForTemplate(subject, contextId);
        } else {
            return this.alertNotificationManager.getNotificationsForAlertDefinition(subject, contextId);
        }
    }

    public AlertNotification addNotification(Subject subject, String senderType, String notificationName,
        Configuration configuration) {
        if (context.equals("template")) {
            return alertNotificationManager.addAlertNotificationToTemplate(subject, contextId, senderType,
                notificationName, configuration);
        } else {
            return this.alertNotificationManager.addAlertNotification(subject, contextId, senderType, notificationName,
                configuration);
        }
    }

    public void removeNotifications(Subject subject, Integer[] notificationIds) {
        if (context.equals("template")) {
            this.alertNotificationManager.removeNotificationsFromTemplate(subject, contextId, notificationIds);
        } else {
            this.alertNotificationManager.removeNotifications(subject, contextId, notificationIds);
        }
    }
}
