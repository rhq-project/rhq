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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for Alert Sender Plugins configuration
 *
 * @author jharris
 */
@Scope(ScopeType.PAGE)
@Name("alertNotificationsUIBean")
public class AlertNotificationsUIBean {

    private final static String SUCCESS_OUTCOME = "success";
    @RequestParameter("ad")
    private Integer alertDefinitionId;
    @RequestParameter("nid")
    private Integer notificationId;
    private List<AlertNotification> alertNotifications;
    private Set<AlertNotification> selectedNotifications;
    private String selectedNewSender;
    private AlertNotification activeNotification;
    private ConfigurationDefinition activeConfigDefinition;
    private AlertNotificationConverter notificationConverter;
    private AlertNotificationManagerLocal alertNotificationManager;

    public List<AlertNotification> getAlertNotifications() {
        return alertNotifications;
    }

    public Set<AlertNotification> getSelectedNotifications() {
        return selectedNotifications;
    }

    public void setSelectedNotifications(Set<AlertNotification> selectedNotifications) {
        this.selectedNotifications = selectedNotifications;
    }

    public String getSelectedNewSender() {
        return selectedNewSender;
    }

    public void setSelectedNewSender(String selectedNewSender) {
        this.selectedNewSender = selectedNewSender;
    }

    public AlertNotification getActiveNotification() {
        return activeNotification;
    }

    public void setActiveNotification(AlertNotification activeNotification) {
        this.activeNotification = activeNotification;

        lookupActiveConfigDefinition();
    }

    public ConfigurationDefinition getActiveConfigDefinition() {
        return activeConfigDefinition;
    }

    public AlertNotificationConverter getNotificationConverter() {
        return notificationConverter;
    }

    private void lookupActiveConfigDefinition() {
        if (this.activeNotification != null) {
            String senderName = this.activeNotification.getSenderName();
            this.activeConfigDefinition = this.alertNotificationManager.getConfigurationDefinitionForSender(senderName);
        }
    }

    public AlertDefinition getAlertDefinition() {
        AlertDefinitionManagerLocal definitionManager = LookupUtil.getAlertDefinitionManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        return definitionManager.getAlertDefinitionById(subject, alertDefinitionId);
    }

    @Create
    public void initNotifications() {
        this.alertNotificationManager = LookupUtil.getAlertNotificationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        this.alertNotifications = this.alertNotificationManager.getNotificationsForAlertDefinition(subject, alertDefinitionId);
        this.selectedNotifications = new HashSet<AlertNotification>();
        this.notificationConverter = new AlertNotificationConverter();
        this.notificationConverter.setAlertNotifications(alertNotifications);

        selectActiveNotification();
    }

    // Sets the initial state of the bean given the requrest parameters, this allows
    // us to maintain the selected item across requrests.
    private void selectActiveNotification() {
        if (this.notificationId != null) {
            for (AlertNotification notification : this.alertNotifications) {
                if (notification.getId() == this.notificationId) {
                    setActiveNotification(notification);
                    this.selectedNotifications.add(notification);

                    return;
                }
            }
        }
    }

    public Map<String, String> getAllAlertSenders() {
        Map<String, String> result = new HashMap<String, String>();

        for (String sender : this.alertNotificationManager.listAllAlertSenders()) {
            result.put(sender, sender);
        }

        return result;
    }

    public String addAlertSender() {
        AlertNotificationManagerLocal notificationManager = LookupUtil.getAlertNotificationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Configuration newSenderConfig = null;
        ConfigurationDefinition configDefinition = notificationManager.getConfigurationDefinitionForSender(selectedNewSender);

        if (configDefinition != null) {
            newSenderConfig = configDefinition.getDefaultTemplate().createConfiguration();
        } else {
            newSenderConfig = new Configuration();
        }

        this.activeNotification = notificationManager.addAlertNotification(subject, alertDefinitionId, selectedNewSender, newSenderConfig);

        return SUCCESS_OUTCOME;
    }

    public String saveConfiguration() {
        if (this.activeNotification != null) {
            this.alertNotificationManager.updateAlertNotification(this.activeNotification);
        }

        return SUCCESS_OUTCOME;
    }

    public String removeSelected() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        List<Integer> ids = getSelectedIds(this.selectedNotifications);

        alertNotificationManager.removeNotifications(subject, alertDefinitionId, toArray(ids));

        return SUCCESS_OUTCOME;
    }

    private List<Integer> getSelectedIds(Collection<AlertNotification> notifications) {
        List<Integer> ids = new ArrayList<Integer>(notifications.size());
        for (AlertNotification notification : notifications) {
            ids.add(notification.getId());
        }

        return ids;
    }

    private Integer[] toArray(List<Integer> intList) {
        return intList.toArray(new Integer[intList.size()]);
    }
}
