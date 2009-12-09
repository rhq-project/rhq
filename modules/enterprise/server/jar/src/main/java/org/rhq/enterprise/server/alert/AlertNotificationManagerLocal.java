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

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.EmailNotification;
import org.rhq.core.domain.alert.notification.NotificationTemplate;
import org.rhq.core.domain.alert.notification.RoleNotification;
import org.rhq.core.domain.alert.notification.SnmpNotification;
import org.rhq.core.domain.alert.notification.SubjectNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;

/**
 * @author Joseph Marques
 */

@Local
public interface AlertNotificationManagerLocal {
    int addEmailNotifications(Subject subject, Integer alertDefinitionId, String[] emails);

    int addRoleNotifications(Subject subject, Integer alertDefinitionId, Integer[] roleIds);

    int addSubjectNotifications(Subject subject, Integer alertDefinitionId, Integer[] subjectId);

    PageList<EmailNotification> getEmailNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<RoleNotification> getRoleNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<SubjectNotification> getSubjectNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<SnmpNotification> getSnmpNotifications(Integer alertDefinitionId, PageControl pageControl);

    PageList<EmailNotification> getEmailNotifications(Integer[] alertNotificationIds, PageControl pageControl);

    PageList<RoleNotification> getRoleNotifications(Integer[] alertNotificationIds, PageControl pageControl);

    PageList<SubjectNotification> getSubjectNotifications(Integer[] alertNotificationIds, PageControl pageControl);

    PageList<RoleNotification> getRoleNotificationsByRoles(Integer[] roleIds, PageControl pageControl);

    PageList<SubjectNotification> getSubjectNotificationsBySubjects(Integer[] subjectId, PageControl pageControl);

    /**
     * Remove the passed notifications from the passed alert definition (all identified by their id)
     * @param subject Caller
     * @param alertDefinitionId alert definition to modify
     * @param notificationIds Notifications to remove
     * @return number of notifications removed
     */
    int removeNotifications(Subject subject, Integer alertDefinitionId, Integer[] notificationIds);

    int purgeOrphanedAlertNotifications();

    public Configuration getAlertPropertiesConfiguration(AlertNotification notification);

    /**
     * Return a list of all available AlertSenders in the system by their shortname.
     * @return list of senders.
     */
    List<String> listAllAlertSenders();

    ConfigurationDefinition getConfigurationDefinitionForSender(String shortName);

    /**
     * Add a new AlertNotification to the passed definition
     * @param user subject of the caller
     * @param alertDefinitionId Id of the alert definition
     * @param senderName shortName of the {@link AlertSender}
     * @param configuration Properties for this alert sender.
     */
    void addAlertNotification(Subject user, int alertDefinitionId, String senderName, Configuration configuration);

    /**
     * Return notifications for a certain alertDefinitionId
     *
     * NOTE: this only returns notifications that have an AlertSender defined.
     *
     * @param user Subject of the caller
     * @param alertDefinitionId Id of the alert definition
     * @return list of defined notification of the passed alert definition
     *
     *
     */
    List<AlertNotification> getNotificationsForAlertDefinition(Subject user, int alertDefinitionId);

    AlertSenderInfo getAlertInfoForSender(String shortName);

    /**
     * Take the passed NotificationTemplate and apply its Notifications to the passed AlertDefinition
     * @param template NotificationTemplate to apply
     * @param def AlertDefinition  to apply the template to
     * @param removeOldNotifications Shall old Notifications on the Definition be removed?
     */
    void applyNotificationTemplateToAlertDefinition(NotificationTemplate template, AlertDefinition def, boolean removeOldNotifications);
}