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
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing bean for the new AlertNotifications stuff
 * @author Heiko W. Rupp
 */
@Scope(ScopeType.PAGE)
@Name("ListNotificationsUIBean")
public class ListNotificationsUIBean {

    public static final String MANAGED_BEAN_NAME = "ListNotificationsUIBean";
    private static final String OUTCOME_SUCCESS = "success";

    private final Log log = LogFactory.getLog(ListNotificationsUIBean.class);

    private String selectedSender;

    private ConfigurationDefinition alertConfigurationDefinition;
    private Configuration alertProperties;

    @RequestParameter("ad")
    private Integer alertDefinitionId;

    public ListNotificationsUIBean() {
    }

    public Map<String,String> getAllAlertSenders() {
        AlertNotificationManagerLocal notificationManager = LookupUtil.getAlertNotificationManager();
        List<String> senders = notificationManager.listAllAlertSenders();
        Map<String,String> result = new HashMap<String,String>();
        for (String sender: senders)
            result.put(sender,sender);

        return result;
    }


    public ConfigurationDefinition getAlertConfigurationDefinition() {

        if (alertConfigurationDefinition==null) {
            lookupAlertConfigDefinition();
        }

        return alertConfigurationDefinition;
    }

    private void lookupAlertConfigDefinition() {
        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        alertConfigurationDefinition = mgr.getConfigurationDefinitionForSender(selectedSender);

    }

    public Configuration getAlertProperties() {
        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();

        // TODO in case of listing an existing notification get this from the properties on the notification
        if (alertProperties==null) {
            if (alertConfigurationDefinition==null)
                alertConfigurationDefinition = getAlertConfigurationDefinition();
            if (alertConfigurationDefinition!=null)
                alertProperties = alertConfigurationDefinition.getDefaultTemplate().getConfiguration();
            else
                alertProperties = new Configuration();
        }
        return alertProperties;
    }

    public void setAlertProperties(Configuration alertProperties) {
        this.alertProperties = alertProperties;
    }

    public String getSelectedSender() {
        return selectedSender;
    }

    public void setSelectedSender(String selectedSender) {
        // If the user selects a different sender in the drop down, clean out the
        // properties that may have been set
        if (!selectedSender.equals(this.selectedSender)) {
            alertProperties=null;
            alertConfigurationDefinition = null;
        }
        this.selectedSender = selectedSender;
    }

    public String getAlertDefinitionName() {
        if (alertDefinitionId==null)
            return "- unset - ";

        AlertDefinitionManagerLocal mgr = LookupUtil.getAlertDefinitionManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        AlertDefinition def = mgr.getAlertDefinitionById(subject,alertDefinitionId);

        return def.getName();
    }

    public String mySubmitForm() {
        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        mgr.addAlertNotification(subject, alertDefinitionId,selectedSender,alertProperties);
        return OUTCOME_SUCCESS;
    }

    public Collection<AlertNotification> getExistingNotifications() {
        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (alertDefinitionId==null)
            return new ArrayList<AlertNotification>();

        List<AlertNotification> notifications = mgr.getNotificationsForAlertDefinition(subject,alertDefinitionId);
        return notifications;
    }

    public String removeSelectedNotifications() {
        String[] stringItems = FacesContextUtility.getRequest().getParameterValues("selectedNotification");
        if (stringItems == null || stringItems.length == 0) {
            return OUTCOME_SUCCESS; // TODO leave message
        }

        Integer[] notificationIds = StringUtility.getIntegerArray(stringItems);

        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        mgr.removeNotifications(subject,alertDefinitionId,notificationIds);

        return OUTCOME_SUCCESS;
    }
}