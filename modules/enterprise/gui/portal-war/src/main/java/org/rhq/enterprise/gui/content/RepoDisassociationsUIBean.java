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
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the list of content sources that are currently disassociated from a repo.
 */
public class RepoDisassociationsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "RepoDisassociationsUIBean";

    public RepoDisassociationsUIBean() {
    }

    public String associateSelectedContentSourcesWithRepo() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedRepoDisassociations();
        int repoId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] contentSourceIds = getIntegerArray(selected);

        if ((contentSourceIds != null) && (contentSourceIds.length > 0)) {
            try {
                RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();
                manager.addContentSourcesToRepo(subject, repoId, contentSourceIds);

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Associated [" + contentSourceIds.length
                    + "] content sources with repository");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to associate one or more content sources with repository", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new RepoDisassociationsDataModel(PageControlView.RepoDisassociationsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class RepoDisassociationsDataModel extends PagedListDataModel<ContentSource> {
        public RepoDisassociationsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ContentSource> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();

            int repoId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);

            PageList<ContentSource> results = manager.getAvailableContentSourcesForRepo(subject, repoId, pc);
            return results;
        }
    }

    private String[] getSelectedRepoDisassociations() {
        return FacesContextUtility.getRequest().getParameterValues("selectedRepoDisassociations");
    }

    private int[] getIntegerArray(String[] input) {
        if (input == null) {
            return new int[0];
        }

        int[] output = new int[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.valueOf(input[i]).intValue(); // force it to parse to make sure its valid
        }

        return output;
    }
}