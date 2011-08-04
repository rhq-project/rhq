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

import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoPackageVersionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "RepoPackageVersionsUIBean";
    public static final String FORM_ID = "repoPackageVersionsListForm";
    public static final String FILTER_ID = FORM_ID + ":" + "packageFilter";

    private String packageFilter;

    public RepoPackageVersionsUIBean() {
    }

    public void installSelectedPackages() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedPackages = FacesContextUtility.getRequest().getParameterValues("selectedPackages");
        int repoId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));

        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        ContentManagerLocal contentManager = LookupUtil.getContentManager();

        int[] packageIds = new int[selectedPackages.length];
        for (int i = 0; i < packageIds.length; i++) {
            packageIds[i] = Integer.parseInt(selectedPackages[i]);
        }

        try {
            List<Resource> resources = repoManager.findSubscribedResources(subject, repoId, PageControl
                .getUnlimitedInstance());
            int[] resourceIds = new int[resources.size()];
            for (int i = 0; i < resourceIds.length; i++) {
                resourceIds[i] = resources.get(i).getId();
            }

            contentManager.deployPackages(subject, resourceIds, packageIds);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to deploy packages: " + packageIds
                + " to Resources subscribed to repository: " + repoId + " Error: " + e.getMessage());
        }
    }

    public void deleteSelectedPackages() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedPackages = FacesContextUtility.getRequest().getParameterValues("selectedPackages");
        int repoId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));

        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        
        int[] packageIds = new int[selectedPackages.length];
        for (int i = 0; i < packageIds.length; i++) {
            packageIds[i] = Integer.parseInt(selectedPackages[i]);
        }
        
        try {
            if (!repoManager.deletePackageVersionsFromRepo(subject, repoId, packageIds)) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Not all packages where deleted because some of them are provided by content sources.");
            }
            
            //force reload of the package version list
            dataModel = null;
        } catch(Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete packages: " + packageIds
                + " from repository: " + repoId + " Error: " + e.getMessage());
        }
    }
    
    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new RepoPackageVersionsDataModel(PageControlView.RepoPackageVersionsList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public void init() {
        if (this.packageFilter == null) {
            this.packageFilter = FacesContextUtility.getOptionalRequestParameter(FILTER_ID);
        }
    }

    private class RepoPackageVersionsDataModel extends PagedListDataModel<PackageVersion> {
        public RepoPackageVersionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<PackageVersion> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            RepoPackageVersionsUIBean.this.init();

            RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

            PageList<PackageVersion> results;
            results = manager.findPackageVersionsInRepo(subject, id, getPackageFilter(), pc);
            return results;
        }
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public void setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
    }
}