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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class CreateContentSourceUIBean {
    private ContentSource newContentSource = new ContentSource();
    private Map<String, ContentSourceType> contentSourceTypes = null;
    private ContentSourceType selectedContentSourceType = null;

    public SelectItem[] getContentSourceTypeNames() {
        loadContentSourceTypes();

        if (this.contentSourceTypes == null) {
            return new SelectItem[0];
        }

        List<String> names = new ArrayList<String>(this.contentSourceTypes.keySet());
        Collections.sort(names);
        SelectItem[] items = new SelectItem[names.size()];

        int i = 0;
        for (String name : names) {
            items[i++] = new SelectItem(name);
        }

        return items;
    }

    public ConfigurationDefinition getContentSourceTypeConfigurationDefinition() {
        return (selectedContentSourceType != null) ? selectedContentSourceType
            .getContentSourceConfigurationDefinition() : null;
    }

    public String getSelectedContentSourceTypeName() {
        return (selectedContentSourceType != null) ? selectedContentSourceType.getName() : null;
    }

    public void setSelectedContentSourceTypeName(String name) {
        loadContentSourceTypes();

        ContentSourceType cst = null;

        if (name != null) {
            cst = contentSourceTypes.get(name);
        }

        updateSelectedContentSourceType(cst);
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

    public String changedSelectedContentSourceTypeName() {
        return "changedSelectedContentSourceTypeName";
    }

    public ContentSource getContentSource() {
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
        ContentSource created = manager.createContentSource(subject, newContentSource);

        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Saved [" + created.getName() + "] with the ID of ["
            + created.getId() + "]");

        contentSourceTypes = null;
        selectedContentSourceType = null;
        newContentSource = new ContentSource();

        return "save";
    }

    public String cancel() {
        contentSourceTypes = null;
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

            // reset the configuration - make it null if there is on config for the new type
            if (cst.getContentSourceConfigurationDefinition() == null) {
                newContentSource.setConfiguration(null);
            } else {
                newContentSource.setConfiguration(cst.getContentSourceConfigurationDefinition().getDefaultTemplate()
                    .createConfiguration());
            }

            // reset the content source's sync schedule and other settings  to the new type's defaults
            newContentSource.setSyncSchedule(cst.getDefaultSyncSchedule());
            newContentSource.setLazyLoad(cst.isDefaultLazyLoad());
            newContentSource.setDownloadMode(cst.getDefaultDownloadMode());
        }
    }

    private void loadContentSourceTypes() {
        if (this.contentSourceTypes == null) {
            ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
            Set<ContentSourceType> types = manager.getAllContentSourceTypes();
            this.contentSourceTypes = new HashMap<String, ContentSourceType>(types.size());
            for (ContentSourceType cst : types) {
                this.contentSourceTypes.put(cst.getName(), cst);
            }
        }
    }
}