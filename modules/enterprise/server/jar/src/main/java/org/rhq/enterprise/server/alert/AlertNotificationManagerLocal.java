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

import org.rhq.core.domain.alert.notification.EmailNotification;
import org.rhq.core.domain.alert.notification.RoleNotification;
import org.rhq.core.domain.alert.notification.SnmpNotification;
import org.rhq.core.domain.alert.notification.SubjectNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 */

@Local
public interface AlertNotificationManagerLocal {
    int addEmailNotifications(Subject subject, Integer alertDefinitionId, String[] emails, boolean isAlertTemplate);

    int addRoleNotifications(Subject subject, Integer alertDefinitionId, Integer[] roleIds, boolean isAlertTemplate);

    int addSubjectNotifications(Subject subject, Integer alertDefinitionId, Integer[] subjectId, boolean isAlertTemplate);

    void setSnmpNotification(Subject subject, Integer alertDefinitionId, SnmpNotification snmpNotification,
        boolean isAlertTemplate);

    PageList<EmailNotification> getEmailNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<RoleNotification> getRoleNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<SubjectNotification> getSubjectNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<SnmpNotification> getSnmpNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<EmailNotification> getEmailNotifications(Integer[] alertNotificationIds, PageControl pageControl);

    PageList<RoleNotification> getRoleNotifications(Integer[] alertNotificationIds, PageControl pageControl);

    PageList<SubjectNotification> getSubjectNotifications(Integer[] alertNotificationIds, PageControl pageControl);

    PageList<RoleNotification> getRoleNotificationsByRoles(Integer[] roleIds, PageControl pageControl);

    PageList<SubjectNotification> getSubjectNotificationsBySubjects(Integer[] subjectId, PageControl pageControl);

    int removeNotifications(Subject subject, Integer alertDefinitionId, Integer[] notificationIds,
        boolean isAlertTemplate);

    int purgeOrphanedAlertNotifications();
}