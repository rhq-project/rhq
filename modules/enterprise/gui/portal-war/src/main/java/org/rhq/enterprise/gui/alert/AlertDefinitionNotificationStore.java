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
package org.rhq.enterprise.gui.alert;

import java.io.Serializable;
import java.util.List;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;

@Scope(ScopeType.PAGE)
@Name("alertDefinitionNotificationStore")
public class AlertDefinitionNotificationStore implements AlertNotificationStore, Serializable {

    @In
    private AlertNotificationManagerLocal alertNotificationManager;
    @In
    private AlertDefinition alertDefinition;

    public List<AlertNotification> lookupNotifications(Subject subject) {
        return this.alertNotificationManager.getNotificationsForAlertDefinition(subject, this.alertDefinition.getId());
    }

    public AlertNotification addNotification(Subject subject, String senderType, String notificationName, Configuration configuration) {
         return this.alertNotificationManager.addAlertNotification(subject,
                this.alertDefinition.getId(), senderType, notificationName, configuration);
    }

    public void removeNotifications(Subject subject, Integer[] notificationIds) {
        this.alertNotificationManager.removeNotifications(subject, this.alertDefinition.getId(), notificationIds);
    }
    
}