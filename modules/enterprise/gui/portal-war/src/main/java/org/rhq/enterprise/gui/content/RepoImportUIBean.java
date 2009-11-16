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

package org.rhq.enterprise.gui.content;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.OptionItem;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author jortel
 *
 */
public class RepoImportUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "RepoImportUIBean";

    private int selectedProvider = 0;
    private List<OptionItem<Integer>> providers = null;

    public RepoImportUIBean() {
        getProviderOptions();
    }

    /**
     * @return the selectedProvider
     */
    public String getSelectedProvider() {
        return String.valueOf(selectedProvider);
    }

    /**
     * @param selectedProvider the selectedProvider to set
     */
    public void setSelectedProvider(String selectedProvider) {
        dataModel = null;
        this.selectedProvider = Integer.valueOf(selectedProvider);
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean#getDataModel()
     */
    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ImportRepDataModel(PageControlView.ReposToImportList, MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    /**
     * Creates the list of content providers to be displayed in
     * the radio buttons.
     * @return An array of options.
     */
    public SelectItem[] getProviderOptions() {
        if (providers == null) {
            providers = new ArrayList<OptionItem<Integer>>();
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
            PageControl pc = new PageControl();
            PageList<ContentSource> results = manager.getAllContentSources(subject, pc);
            for (ContentSource p : results) {
                OptionItem<Integer> item = new OptionItem<Integer>(p.getId(), p.getName());
                providers.add(item);
                if (selectedProvider == 0) {
                    selectedProvider = p.getId();
                }
            }
        }
        return SelectItemUtils.convertFromListOptionItem(providers, false);
    }

    /**
     * Get the list of the selected repos.
     * @return An array of repo IDs.
     */
    public String[] getSelectedRepos() {
        return FacesContextUtility.getRequest().getParameterValues("selectedRepos");
    }

    /**
     * Import the selected repos.
     * To import a repo, is to change it from is_candidate=true to is_candidate=false.
     * @return The next page defined in the navigation.
     */
    public String importSelected() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedRepos();
        List<Integer> repoIds = new ArrayList<Integer>(selected.length);
        for (String sRepoId : selected) {
            repoIds.add(Integer.valueOf(sRepoId));
        }
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        try {
            repoManager.importCandidateRepo(subject, repoIds);
        } catch (RepoException e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to import one or more Repositories from Content Provider", e);
            return "failed";
        }
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, selected.length
            + " Repositories imported from Content Provider");
        return "success";
    }

    private class ImportRepDataModel extends PagedListDataModel<Repo> {
        public ImportRepDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<Repo> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
            PageList<Repo> results = manager.getCandidateRepos(subject, selectedProvider, pc);

            return results;
        }
    }
}
