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
package org.rhq.enterprise.gui.content;

import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentException;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.perspective.PerspectiveManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class CreateContentSourceUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "CreateContentSourceUIBean";

    private ContentSourceManagerLocal contentSourceManager = LookupUtil.getContentSourceManager();

    private PerspectiveManagerLocal perspectiveManager = LookupUtil.getPerspectiveManager();

    private ContentSource newContentSource = new ContentSource();
    private ContentSourceType selectedContentSourceType = null;

    public ConfigurationDefinition getContentSourceTypeConfigurationDefinition() {
        return (selectedContentSourceType != null) ? selectedContentSourceType
            .getContentSourceConfigurationDefinition() : null;
    }

    public SelectItem[] getDownloadModes() {
        DownloadMode[] modes = DownloadMode.values();
        SelectItem[] items = new SelectItem[modes.length];
        int i = 0;

        for (DownloadMode mode : modes) {
            items[i++] = new SelectItem(mode.name());
        }

        return items;
    }

    public String getSelectedDownloadMode() {
        return this.newContentSource.getDownloadMode().name();
    }

    public void setSelectedDownloadMode(String mode) {
        this.newContentSource.setDownloadMode(DownloadMode.valueOf(mode));
    }

    public ContentSource getContentSource() {
        HttpServletRequest request = FacesContextUtility.getRequest();

        // If the user gets here trying to create a new content source, reset the previous object's data.
        // This is to prevent prepopulation of data from an earlier failed attempt if the user navigates off
        // the second attempt without using the cancel button.
        if ("new".equals(request.getParameter("mode"))) {
            newContentSource = new ContentSource();
            updateSelectedContentSourceType(this.selectedContentSourceType);
        }

        return newContentSource;
    }

    public void setContentSource(ContentSource newContentSource) {
        this.newContentSource = newContentSource;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "The selected content source type does not require a configuration.";
    }

    public String getNullConfigurationMessage() {
        return "Content source has an empty configuration."; // is this ever really used?
    }

    public String save() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();

        try {
            ContentSource created = manager.createContentSource(subject, newContentSource);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Saved [" + created.getName()
                + "] with the ID of [" + created.getId() + "]");
        } catch (ContentException ce) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ce.getMessage());
            return "edit"; // stay in edit mode upon failure
        }

        selectedContentSourceType = null;
        newContentSource = new ContentSource();

        return "save";
    }

    public String cancel() {
        selectedContentSourceType = null;
        newContentSource = new ContentSource();
        return "cancel";
    }

    private void updateSelectedContentSourceType(ContentSourceType cst) {
        if (cst == null) {
            selectedContentSourceType = null;
            newContentSource.setContentSourceType(null);
        } else if (!cst.equals(newContentSource.getContentSourceType())) {
            selectedContentSourceType = cst;
            newContentSource.setContentSourceType(cst);

            // reset the configuration - make it null if there is no config for the new type
            if (cst.getContentSourceConfigurationDefinition() == null) {
                newContentSource.setConfiguration(null);
            } else {
                ConfigurationTemplate defaultTemplate = cst.getContentSourceConfigurationDefinition()
                    .getDefaultTemplate();

                if (defaultTemplate != null) {
                    newContentSource.setConfiguration(defaultTemplate.createConfiguration());
                } else {
                    newContentSource.setConfiguration(new Configuration());
                }
            }

            // reset the content source's sync schedule and other settings  to the new type's defaults
            newContentSource.setSyncSchedule(cst.getDefaultSyncSchedule());
            newContentSource.setLazyLoad(cst.isDefaultLazyLoad());
            newContentSource.setDownloadMode(cst.getDefaultDownloadMode());
        }
    }

    public String finishAddMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map added.");
        return "success";
    }

    public String finishEditMap() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Map updated.");
        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListContentSourceTypesDataModel(PageControlView.NONE, MANAGED_BEAN_NAME);
        } else {
            String typeName = FacesContextUtility.getOptionalRequestParameter("typeName");
            if (typeName != null) {
                if (this.selectedContentSourceType == null
                    || (this.selectedContentSourceType != null && !typeName.equals(this.selectedContentSourceType
                        .getName()))) {
                    this.selectedContentSourceType = contentSourceManager.getContentSourceType(typeName);
                    updateSelectedContentSourceType(this.selectedContentSourceType);
                }
            }
        }

        return dataModel;
    }

    private class ListContentSourceTypesDataModel extends PagedListDataModel<ContentSourceType> {
        public ListContentSourceTypesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ContentSourceType> fetchPage(PageControl pc) {
            Set<ContentSourceType> types = contentSourceManager.getAllContentSourceTypes();

            PageList<ContentSourceType> results = null;
            results = new PageList<ContentSourceType>(types, types.size(), PageControl.getUnlimitedInstance());
            return results;
        }
    }

    public String getCreateContentProviderPageLink(String typeName) {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String link = perspectiveManager.getPageLink(subject, "createContentProvider", typeName, null);

        if (null == link) {
            link = "/rhq/content/createContentProvider-plain.xhtml?mode=new&typeName=" + typeName;
        } else {
            link += "&mode=new&typeName=" + typeName;
        }

        return link;
    }
}