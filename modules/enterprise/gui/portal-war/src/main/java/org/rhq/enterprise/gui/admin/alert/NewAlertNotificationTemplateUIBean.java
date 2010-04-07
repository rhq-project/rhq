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

import java.util.ArrayList;

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.AlertNotificationTemplate;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.EnterpriseFacesContextUIBean;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;

@Scope(ScopeType.PAGE)
@Name("NewAlertNotificationTemplateUIBean")
public class NewAlertNotificationTemplateUIBean extends EnterpriseFacesContextUIBean {

    @In
    private AlertNotificationManagerLocal alertNotificationManager;

    private String name;
    private String description;

    private int createdTemplateId; // will be set as a result of create(), used for JSF navigation rules

    public String create() {
        try {
            AlertNotificationTemplate template = alertNotificationManager.createNotificationTemplate(this.name,
                this.description, new ArrayList<AlertNotification>(), true);
            createdTemplateId = template.getId();
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Created new alert notification template");
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Creation failed: " + t.getMessage());
        }

        return OUTCOME_SUCCESS;
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Template creation cancelled.");
        return OUTCOME_CANCELLED;
    }

    public int getCreatedTemplateId() {
        return createdTemplateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
