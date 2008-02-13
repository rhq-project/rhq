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

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListContentSourcesUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListContentSourcesUIBean";

    private ContentSourceManagerLocal contentSourceManager = LookupUtil.getContentSourceManager();

    public ListContentSourcesUIBean() {
    }

    public String createNewContentSource() {
        return "createNewContentSource";
    }

    public String deleteSelectedContentSources() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedContentSources();
        Integer[] ids = getIntegerArray(selected);

        if (ids.length > 0) {
            try {
                for (Integer id : ids) {
                    contentSourceManager.deleteContentSource(subject, id);
                }

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted [" + ids.length
                    + "] content sources.");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete content sources.", e);
            }
        }

        return "success";
    }

    public String syncSelectedContentSources() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedContentSources();
        Integer[] ids = getIntegerArray(selected);

        if (ids.length > 0) {
            try {
                for (Integer id : ids) {
                    contentSourceManager.synchronizeAndLoadContentSource(subject, id);
                }

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Synchronizing [" + ids.length
                    + "] content sources.");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to synchronized content sources.",
                    e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListContentSourcesDataModel(PageControlView.ContentSourcesList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListContentSourcesDataModel extends PagedListDataModel<ContentSource> {
        public ListContentSourcesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<ContentSource> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();

            PageList<ContentSource> results = manager.getAllContentSources(subject, pc);
            return results;
        }
    }

    private String[] getSelectedContentSources() {
        return FacesContextUtility.getRequest().getParameterValues("selectedContentSources");
    }

    private Integer[] getIntegerArray(String[] input) {
        if (input == null) {
            return new Integer[0];
        }

        Integer[] output = new Integer[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.valueOf(input[i]);
        }

        return output;
    }
}