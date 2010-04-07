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
package org.rhq.enterprise.gui.admin.alert;

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.alert.notification.AlertNotificationTemplate;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.EnterpriseFacesContextUIBean;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;

@Scope(ScopeType.PAGE)
@Name("AlertNotificationTemplateDetailsUIBean")
public class AlertNotificationTemplateDetailsUIBean extends EnterpriseFacesContextUIBean {

    @In
    private AlertNotificationManagerLocal alertNotificationManager;

    private AlertNotificationTemplate template;

    private void loadTemplate() {
        if (template == null) {
            int templateId = FacesContextUtility.getRequiredRequestParameter("templateId", Integer.class);
            template = alertNotificationManager.getAlertNotificationTemplate(getSubject(), templateId);
        }
    }

    public AlertNotificationTemplate getTemplate() {
        loadTemplate();

        return template;
    }

    public int getNotificationCount() {
        loadTemplate();

        return template.getNotifications().size();
    }

    public String edit() {
        return OUTCOME_EDIT;
    }

    public String editNotifications() {
        return OUTCOME_EDIT;
    }

    public String save() {
        try {
            alertNotificationManager.updateNotificationTemplate(getSubject(), template.getId(), template.getName(),
                template.getDescription());
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Alert notification template was successfully modified.");
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Error while saving alert notification template: " + t.getMessage());
        }

        return OUTCOME_SAVE;
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
            "Alert notification template modifications were cancelled.");

        return OUTCOME_CANCELLED;
    }

}
