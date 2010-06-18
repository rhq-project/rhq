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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.alert.converter.AlertNotificationConverter;
import org.rhq.enterprise.gui.common.framework.EnterpriseFacesContextUIBean;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;

/**
 * Backing bean for Alert Sender Plugins configuration
 *
 * @author Justin Harris
 */
@Scope(ScopeType.EVENT)
@Name("alertNotificationsUIBean")
public class AlertNotificationsUIBean extends EnterpriseFacesContextUIBean {

    @RequestParameter("nid")
    private Integer notificationId;
    @RequestParameter("context")
    private String context;
    @RequestParameter("contextId")
    private Integer contextId;

    @In
    private AlertNotificationManagerLocal alertNotificationManager;

    private List<AlertNotification> alertNotifications;
    private Set<AlertNotification> selectedNotifications;
    private String selectedNewSender;
    private AlertNotification activeNotification;
    private ConfigurationDefinition activeConfigDefinition;
    private AlertNotificationConverter notificationConverter;
    private Map<String, String> alertSenders;

    public List<AlertNotification> getAlertNotifications() {
        return alertNotifications;
    }

    public void setAlertNotifications(List<AlertNotification> alertNotifications) {
        this.alertNotifications = alertNotifications;
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

    public Map<String, String> getAlertSenders() {
        return this.alertSenders;
    }

    private void lookupActiveConfigDefinition() {
        if (this.activeNotification != null) {
            String senderName = this.activeNotification.getSenderName();
            this.activeConfigDefinition = this.alertNotificationManager.getConfigurationDefinitionForSender(senderName);
        }
    }

    @Create
    public void initNotifications() {
        reloadAlertNotifications();
        this.selectedNotifications = new HashSet<AlertNotification>();
        this.notificationConverter = new AlertNotificationConverter();
        this.notificationConverter.setAlertNotifications(alertNotifications);
        this.alertSenders = lookupAlertSenders();

        selectActiveNotification();
    }

    public void reloadAlertNotifications() {
        this.alertNotifications = this.alertNotificationManager.getNotificationsForAlertDefinition(getSubject(),
            contextId);
    }

    // Sets the initial state of the bean given the requrest parameters, this allows
    // us to maintain the selected item across requests.
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

    private Map<String, String> lookupAlertSenders() {
        Map<String, String> result = new TreeMap<String, String>();

        for (String sender : this.alertNotificationManager.listAllAlertSenders()) {
            result.put(sender, sender);
        }

        return result;
    }

    public String addAlertSender() {
        try {
            Configuration newSenderConfig = null;
            ConfigurationDefinition configDefinition = this.alertNotificationManager
                .getConfigurationDefinitionForSender(this.selectedNewSender);

            if (configDefinition != null) {
                newSenderConfig = configDefinition.getDefaultTemplate().createConfiguration();
            } else {
                newSenderConfig = new Configuration();
            }

            AlertNotification newNotification = new AlertNotification(this.selectedNewSender, newSenderConfig);

            AlertNotification newlyCreated = this.alertNotificationManager.addAlertNotification(getSubject(),
                this.contextId, newNotification);

            reloadAlertNotifications();
            this.activeNotification = newlyCreated;
            this.selectedNotifications.clear();
            this.selectedNotifications.add(this.activeNotification);
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to create alert notification", t);
        }

        return OUTCOME_SUCCESS;
    }

    public String saveConfiguration() {
        try {
            if (this.activeNotification != null) {
                this.alertNotificationManager.updateAlertNotification(getSubject(), this.contextId,
                    this.activeNotification);

                reselectActiveNotificationUsingDataComparison(this.activeNotification);
            }
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to save alert notification", t);
        }

        return OUTCOME_SUCCESS;
    }

    public void reselectActiveNotificationUsingDataComparison(AlertNotification notificationWithLatestData) {
        /* 
         * figure out which one should be selected from the backing store.  this method should be called after
         * executing save() from either the AlertNotificationUIBean or CustomContentUIBean.  the effect of updating
         * an alert notification will overwrite all existing notifications with the latest data.  since the ids
         * of the alert notifications are difference after a save-action, but since we know what the new data should 
         * be based off of the last known state of the activeNotification from above, we can select the correct one
         * by testing for data equality.
         */
        reloadAlertNotifications();
        for (AlertNotification nextNotification : this.alertNotifications) {
            if (notificationWithLatestData.equalsData(nextNotification)) {
                this.activeNotification = nextNotification;
                this.selectedNotifications.clear();
                this.selectedNotifications.add(this.activeNotification);
            }
        }
    }

    public String removeSelected() {
        try {
            this.alertNotificationManager.removeNotifications(getSubject(), this.contextId, getSelectedIds());
            this.alertNotifications.removeAll(this.selectedNotifications); // only remove if no errors
            this.activeNotification = null;
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to save remove notifications", t);
        }

        return OUTCOME_SUCCESS;
    }

    private Integer[] getSelectedIds() {
        Integer[] results = new Integer[this.selectedNotifications.size()];
        int i = 0;
        for (AlertNotification nextNotification : selectedNotifications) {
            results[i++] = nextNotification.getId();
        }
        return results;
    }
}
