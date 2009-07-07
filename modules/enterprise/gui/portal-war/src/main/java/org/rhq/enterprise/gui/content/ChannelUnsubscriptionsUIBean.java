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
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ChannelUnsubscriptionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ChannelUnsubscriptionsUIBean";

    private String searchString = null;
    private String searchCategory = null;

    public ChannelUnsubscriptionsUIBean() {
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

    public String associateSelectedContentSourcesWithChannel() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedChannelUnsubscriptions();
        int channelId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] resourceIds = getIntegerArray(selected);

        if ((resourceIds != null) && (resourceIds.length > 0)) {
            try {
                ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();

                for (int resourceId : resourceIds) {
                    manager.subscribeResourceToChannels(subject, resourceId, new int[] { channelId });
                }

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Subscribed [" + resourceIds.length
                    + "] resources with channel");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to subscribe one or more resources with channel", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ChannelUnsubscriptionsDataModel(PageControlView.ChannelUnsubscriptionsList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ChannelUnsubscriptionsDataModel extends PagedListDataModel<Resource> {
        public ChannelUnsubscriptionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<Resource> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceManagerLocal manager = LookupUtil.getResourceManager();
            int channelId = Integer.parseInt(FacesContextUtility.getRequiredRequestParameter("id"));
            String search = FacesContextUtility
                .getOptionalRequestParameter("channelUnsubscriptionsListForm:searchStringFilter");
            String category = FacesContextUtility
                .getOptionalRequestParameter("channelUnsubscriptionsListForm:searchCategoryFilter");
            ResourceCategory categoryEnum = ResourceCategory.PLATFORM;

            if (search != null && search.trim().equals("")) {
                search = null;
            }

            if (category != null) {
                categoryEnum = ResourceCategory.valueOf(category);
            }

            PageList<Resource> results = manager.getAvailableResourcesForChannel(subject, channelId, search,
                categoryEnum, pc);

            //PageList<ResourceComposite> results = manager.findResourceComposites(subject, categoryEnum, null, null, search, pc);

            return results;
        }
    }

    private String[] getSelectedChannelUnsubscriptions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedChannelUnsubscriptions");
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