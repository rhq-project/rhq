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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.ResourceNameDisambiguatingPagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the list of resources that are currently subscribed to a repo.
 */
public class RepoSubscriptionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "RepoSubscriptionsUIBean";

    public RepoSubscriptionsUIBean() {
    }

    private static final IntExtractor<Resource> RESOURCE_ID_EXTRACTOR = new IntExtractor<Resource>() {
        public int extract(Resource r) {
            return r.getId();
        }
    };

    public String deleteSelectedRepoSubscriptions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedRepoSubscriptions();
        int repoId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] resourceIds = getIntegerArray(selected);

        if ((resourceIds != null) && (resourceIds.length > 0)) {
            try {
                RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

                // TODO: we should have a SLSB method that takes a list of resources
                for (int resourceId : resourceIds) {
                    manager.unsubscribeResourceFromRepos(subject, resourceId, new int[] { repoId });
                }

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Unsubscribed [" + resourceIds.length
                    + "] resources from repository");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to unsubscribe one or more resources from repository", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new RepoSubscriptionsDataModel(PageControlView.RepoSubscriptionsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class RepoSubscriptionsDataModel extends ResourceNameDisambiguatingPagedListDataModel<Resource> {
        public RepoSubscriptionsDataModel(PageControlView view, String beanName) {
            super(view, beanName, true);
        }

        public PageList<Resource> fetchDataForPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

            PageList<Resource> results = manager.findSubscribedResources(subject, id, pc);
            return results;
        }

        protected IntExtractor<Resource> getResourceIdExtractor() {
            return RESOURCE_ID_EXTRACTOR;
        }
    }

    private String[] getSelectedRepoSubscriptions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedRepoSubscriptions");
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