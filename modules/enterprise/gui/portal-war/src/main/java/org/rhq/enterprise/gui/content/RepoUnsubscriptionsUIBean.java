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
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.ResourceNameDisambiguatingPagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoUnsubscriptionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "RepoUnsubscriptionsUIBean";

    private String searchString = null;
    private String searchCategory = null;

    private static final IntExtractor<Resource> RESOURCE_ID_EXTRACTOR = new IntExtractor<Resource>() {
        public int extract(Resource r) {
            return r.getId();
        }
    };

    public RepoUnsubscriptionsUIBean() {
    }

    public String getSearchCategory() {
        if (this.searchCategory == null) {
            this.searchCategory = ResourceCategory.PLATFORM.name();
        }

        return this.searchCategory;
    }

    public void setSearchCategory(String category) {
        searchCategory = category;
    }

    public String getSearchString() {
        return this.searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String associateSelectedContentSourcesWithRepo() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedRepoUnsubscriptions();
        int repoId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] resourceIds = getIntegerArray(selected);

        if ((resourceIds != null) && (resourceIds.length > 0)) {
            try {
                RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

                for (int resourceId : resourceIds) {
                    manager.subscribeResourceToRepos(subject, resourceId, new int[] { repoId });
                }

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Subscribed [" + resourceIds.length
                    + "] resources with repository");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to subscribe one or more resources with repository", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new RepoUnsubscriptionsDataModel(PageControlView.RepoUnsubscriptionsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class RepoUnsubscriptionsDataModel extends ResourceNameDisambiguatingPagedListDataModel<Resource> {
        public RepoUnsubscriptionsDataModel(PageControlView view, String beanName) {
            super(view, beanName, true);
        }

        public PageList<Resource> fetchDataForPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceManagerLocal manager = LookupUtil.getResourceManager();
            int repoId = Integer.parseInt(FacesContextUtility.getRequiredRequestParameter("id"));
            String search = FacesContextUtility
                .getOptionalRequestParameter("repoUnsubscriptionsListForm:searchStringFilter");
            String category = FacesContextUtility
                .getOptionalRequestParameter("repoUnsubscriptionsListForm:searchCategoryFilter");
            ResourceCategory categoryEnum = ResourceCategory.PLATFORM;

            if (search != null && search.trim().equals("")) {
                search = null;
            }

            if (category != null) {
                categoryEnum = ResourceCategory.valueOf(category);
            }

            PageList<Resource> results = manager.findAvailableResourcesForRepo(subject, repoId, search, categoryEnum,
                pc);

            //PageList<ResourceComposite> results = manager.findResourceComposites(subject, categoryEnum, null, null, search, pc);

            return results;
        }

        protected IntExtractor<Resource> getResourceIdExtractor() {
            return RESOURCE_ID_EXTRACTOR;
        }
    }

    private String[] getSelectedRepoUnsubscriptions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedRepoUnsubscriptions");
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