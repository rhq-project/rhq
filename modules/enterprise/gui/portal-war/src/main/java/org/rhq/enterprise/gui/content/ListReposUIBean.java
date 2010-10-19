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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListReposUIBean extends PagedDataTableUIBean {

    private final Log log = LogFactory.getLog(ListReposUIBean.class);

    public static final String MANAGED_BEAN_NAME = "ListReposUIBean";

    private RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
    private ContentSourceManagerLocal contentSourceManager = LookupUtil.getContentSourceManager();

    public ListReposUIBean() {
    }

    public String createNewRepo() {
        return "createNewRepo";
    }

    public String importRepos() {
        return "importRepos";
    }

    public String syncSelectedRepos() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedRepos();
        int[] repoIds = StringUtility.getIntArray(selected);

        if (repoIds.length > 0) {
            int syncCount = 0;
            try {
                syncCount = repoManager.synchronizeRepos(subject, repoIds);
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete repositories.", e);
            }
            if (syncCount > 0) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Synchronizing [" + syncCount
                    + "] content sources.");
            } else {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                    "Selected Repositories have no content to sync.");
            }
        }

        return "success";
    }

    public String deleteSelectedRepos() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedRepos();
        int[] ids = StringUtility.getIntArray(selected);

        if (ids.length > 0) {
            try {
                for (Integer id : ids) {
                    repoManager.deleteRepo(subject, id);
                }

                FacesContextUtility
                    .addMessage(FacesMessage.SEVERITY_INFO, "Deleted [" + ids.length + "] repositories.");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete repositories.", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListReposDataModel(PageControlView.ReposList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListReposDataModel extends PagedListDataModel<Repo> {
        public ListReposDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<Repo> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

            PageList<Repo> results = manager.findRepos(subject, pc);
            for (Repo repo : results) {
                repo.setSyncStatus(manager.calculateSyncStatus(subject, repo.getId()));
            }
            return results;
        }
    }

    private String[] getSelectedRepos() {
        return FacesContextUtility.getRequest().getParameterValues("selectedRepos");
    }

}