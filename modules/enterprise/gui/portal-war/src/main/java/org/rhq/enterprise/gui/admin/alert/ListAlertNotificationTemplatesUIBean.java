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

import java.util.List;

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.alert.notification.AlertNotificationTemplate;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.EnterpriseFacesContextUIBean;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;

@Scope(ScopeType.PAGE)
@Name("ListAlertNotificationTemplatesUIBean")
public class ListAlertNotificationTemplatesUIBean extends EnterpriseFacesContextUIBean {

    @In
    private AlertNotificationManagerLocal alertNotificationManager;

    public PageList<AlertNotificationTemplate> getAlertNotificationTemplates() {
        List<AlertNotificationTemplate> templates = alertNotificationManager.listNotificationTemplates(getSubject());
        return new PageList<AlertNotificationTemplate>(templates, new PageControl(0, templates.size()));
    }

    public String deleteSelectedAlertNotificationTemplates() {
        try {
            Subject subject = getSubject();
            String[] selectedNotificationTemplates = getSelectedNotificationTemplates();
            Integer[] selectedNotificationTemplateIds = StringUtility.getIntegerArray(selectedNotificationTemplates);
            int deleted = alertNotificationManager
                .deleteNotificationTemplates(subject, selectedNotificationTemplateIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + deleted
                + " alert notification templates");
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Deletion failed: " + t.getMessage());
        }

        return OUTCOME_SUCCESS;
    }

    public String createNewAlertNotificationTemplate() {
        return OUTCOME_CREATE;
    }

    private String[] getSelectedNotificationTemplates() {
        String[] results = FacesContextUtility.getRequest().getParameterValues("selectedNotificationTemplates");
        return results;
    }

}
