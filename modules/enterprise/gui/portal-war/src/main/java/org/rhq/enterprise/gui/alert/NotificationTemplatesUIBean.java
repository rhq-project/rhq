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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;


import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.NotificationTemplate;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.richfaces.model.selection.SimpleSelection;

/**
 * Backing bean for NotificationTemplates for Alert Sender Plugins configuration
 *
 * @author jharris
 * @author Heiko Rupp
 */
@Scope(ScopeType.PAGE)
@Name("notificationTemplatesUIBean")
public class NotificationTemplatesUIBean implements Serializable {

    private final static String SUCESS_OUTCOME = "success";

    @RequestParameter("tid")
    private Integer templateId;
    @In("#{webUser.subject}")
    private Subject subject;
    @In
    private AlertNotificationManagerLocal alertNotificationManager;
    @In
    private AlertNotificationTemplateStore alertNotificationTemplateStore;

    private String newTemplateName;
    private String newTemplateDescription;
    private List<NotificationTemplate> notificationTemplates;
    private SimpleSelection selectedTemplates;
    private Integer selectedTemplateId;

    public String getSelectedTemplateId() {
        if (this.selectedTemplateId != null) {
            return this.selectedTemplateId.toString();
        }

        return null;
    }

    public String getNewTemplateName() {
        return newTemplateName;
    }

    public void setNewTemplateName(String newTemplateName) {
        this.newTemplateName = newTemplateName;
    }

    public String getNewTemplateDescription() {
        return newTemplateDescription;
    }

    public void setNewTemplateDescription(String newTemplateDescription) {
        this.newTemplateDescription = newTemplateDescription;
    }

    public SimpleSelection getSelectedTemplates() {
        return selectedTemplates;
    }

    public void setSelectedTemplates(SimpleSelection selectedTemplates) {
        this.selectedTemplates = selectedTemplates;
    }

    public List<NotificationTemplate> getNotificationTemplates() {
        return this.notificationTemplates;
    }

    @Create
    public void initNotificationTemplates() {
        this.selectedTemplates = new SimpleSelection();
        this.notificationTemplates = alertNotificationManager.listNotificationTemplates(this.subject);

        selectActiveTemplate();
    }

    private void selectActiveTemplate() {
        if (this.templateId != null) {
            this.selectedTemplateId = this.templateId;

            for (int i = 0; i < this.notificationTemplates.size(); i++) {
                NotificationTemplate template = this.notificationTemplates.get(i);

                if (template.getId() == this.templateId) {
                    this.selectedTemplates.addKey(i);
                    this.alertNotificationTemplateStore.setNotificationTemplate(template);

                    return;
                }
            }
        }
    }

    public String createNotificationTemplate() {

        try {
            NotificationTemplate template = alertNotificationManager.createNotificationTemplate(this.newTemplateName,
                    this.newTemplateDescription, new ArrayList<AlertNotification>(), true);

            this.selectedTemplateId = template.getId();
        } catch (IllegalArgumentException iae) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,"Creation of template failed: " + iae.getMessage());
        }

        return SUCESS_OUTCOME;
    }

    public String deleteNotificationTemplate() {
        int numDeleted = alertNotificationManager.deleteNotificationTemplates(this.subject, getSelectedIds());

        String summary = getDeletionSummary(numDeleted);
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, summary);

        // Re-get the list of still existing templates
        this.notificationTemplates = alertNotificationManager.listNotificationTemplates(this.subject);

        return null;
    }

    private Integer[] getSelectedIds() {
        List<Integer> selectedIds = new ArrayList<Integer>(selectedTemplates.size());

        for (NotificationTemplate template : getSelectedTemplateList()) {
            selectedIds.add(template.getId());
        }

        return selectedIds.toArray(new Integer[selectedIds.size()]);
    }

    private String getDeletionSummary(int numberDeleted) {
        StringBuilder builder = new StringBuilder("Deleted ");

        builder.append(numberDeleted);
        builder.append(" template");

        if (numberDeleted != 1) {
            builder.append("s");
        }

        return builder.toString();
    }

    public String editNotificationTemplate() {
        List<NotificationTemplate> selected = getSelectedTemplateList();

        if (selected.size() == 1) {
            this.selectedTemplateId = selected.get(0).getId();

            return SUCESS_OUTCOME;
        }

        return null;
    }

    private List<NotificationTemplate> getSelectedTemplateList() {
        List<NotificationTemplate> templateList = new ArrayList<NotificationTemplate>(selectedTemplates.size());

        Iterator rowKeys = this.selectedTemplates.getKeys();
        while (rowKeys.hasNext()) {
            Integer row = (Integer) rowKeys.next();
            templateList.add(notificationTemplates.get(row));
        }

        return templateList;
    }
}
