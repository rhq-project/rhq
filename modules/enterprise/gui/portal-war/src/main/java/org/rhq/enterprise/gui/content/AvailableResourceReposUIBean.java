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
import org.rhq.core.domain.content.composite.RepoComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AvailableResourceReposUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "AvailableResourceReposUIBean";

    public AvailableResourceReposUIBean() {
    }

    public String subscribeSelectedResourceRepos() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedResourceRepos();
        int resourceId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] repoIds = getIntegerArray(selected);

        if ((repoIds != null) && (repoIds.length > 0)) {
            try {
                RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();
                // TODO: mazz - this shouldn't required Permission.MANAGE_INVENTORY
                manager.subscribeResourceToRepos(subject, resourceId, repoIds);

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Subscribed [" + repoIds.length
                    + "] repositories to resource");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to subscribe one or more repositories to resource", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResourceReposDataModel(PageControlView.AvailableResourceReposList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ResourceReposDataModel extends PagedListDataModel<RepoComposite> {
        public ResourceReposDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<RepoComposite> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();

            PageList<RepoComposite> results = manager.findAvailableResourceSubscriptions(subject, id, pc);
            return results;
        }
    }

    private String[] getSelectedResourceRepos() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAvailableResourceRepos");
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