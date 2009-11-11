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
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author jortel
 *
 */
public class RepoImportUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "RepoImportUIBean";

    private int selectedProvider = 10001;

    public RepoImportUIBean() {
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

    public SelectItem[] getProviderOptions() {
        List<OptionItem<Integer>> list = new ArrayList<OptionItem<Integer>>();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();
        PageControl pc = new PageControl();
        PageList<ContentSource> results = manager.getAllContentSources(subject, pc);
        for (ContentSource p : results) {
            OptionItem<Integer> item = new OptionItem<Integer>(p.getId(), p.getName());
            list.add(item);
        }
        return SelectItemUtils.convertFromListOptionItem(list, false);
    }

    public String[] getSelectedRepos() {
        return FacesContextUtility.getRequest().getParameterValues("selectedRepos");
    }

    public String importSelected() {
        String[] selected = getSelectedRepos();
        // TODO: Actually import the repos using a manager bean.
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
            // TODO: Replace with call to get 'candidate' repos for provider.
            PageList<Repo> results = manager.getAssociatedRepos(subject, selectedProvider, pc);
            return results;
        }
    }
}
