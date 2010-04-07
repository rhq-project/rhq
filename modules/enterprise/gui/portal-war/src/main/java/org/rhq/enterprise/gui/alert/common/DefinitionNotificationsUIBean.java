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
import java.util.Map;
import java.util.TreeMap;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.alert.notification.AlertNotificationTemplate;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.EnterpriseFacesContextUIBean;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;

@Scope(ScopeType.EVENT)
@Name("definitionNotificationsUIBean")
public class DefinitionNotificationsUIBean extends EnterpriseFacesContextUIBean {

    @In
    private AlertNotificationManagerLocal alertNotificationManager;

    private String selectedTemplate;
    private Boolean clearExistingNotifications;
    private Map<String, String> notificationTemplates;

    public String getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(String selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public Boolean getClearExistingNotifications() {
        return clearExistingNotifications;
    }

    public void setClearExistingNotifications(Boolean clearExistingNotifications) {
        this.clearExistingNotifications = clearExistingNotifications;
    }

    public Map<String, String> getNotificationTemplates() {
        return this.notificationTemplates;
    }

    public String addAlertSenderFromTemplate() {
        int alertDefinitionId = FacesContextUtility.getRequiredRequestParameter("ad", Integer.class);
        this.alertNotificationManager.applyNotificationTemplateToAlertDefinition(getSelectedTemplate(),
            alertDefinitionId, getClearExistingNotifications());

        return "success";
    }

    @Create
    public void init() {
        this.notificationTemplates = lookupNotificationTemplates();
    }

    private Map<String, String> lookupNotificationTemplates() {
        Map<String, String> result = new TreeMap<String, String>();
        List<AlertNotificationTemplate> templates = this.alertNotificationManager
            .listNotificationTemplates(getSubject());

        for (AlertNotificationTemplate template : templates) {
            String displayName = getNotificationDisplayName(template);
            result.put(displayName, template.getName()); // displayed text, option value
        }

        return result;
    }

    private String getNotificationDisplayName(AlertNotificationTemplate template) {
        StringBuilder builder = new StringBuilder(template.getName());

        builder.append(" (");
        builder.append(template.getDescription());
        builder.append(")");

        return builder.toString();
    }

}