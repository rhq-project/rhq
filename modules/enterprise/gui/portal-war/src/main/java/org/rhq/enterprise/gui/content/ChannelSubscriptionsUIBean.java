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
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the list of resources that are currently subscribed to a channel.
 */
public class ChannelSubscriptionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ChannelSubscriptionsUIBean";

    public ChannelSubscriptionsUIBean() {
    }

    public String deleteSelectedChannelSubscriptions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedChannelSubscriptions();
        int channelId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] resourceIds = getIntegerArray(selected);

        if ((resourceIds != null) && (resourceIds.length > 0)) {
            try {
                ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();

                // TODO: we should have a SLSB method that takes a list of resources
                for (int resourceId : resourceIds) {
                    manager.unsubscribeResourceFromChannels(subject, resourceId, new int[] { channelId });
                }

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Unsubscribed [" + resourceIds.length
                    + "] resources from channel");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to unsubscribe one or more resources from channel", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ChannelSubscriptionsDataModel(PageControlView.ChannelSubscriptionsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ChannelSubscriptionsDataModel extends PagedListDataModel<Resource> {
        public ChannelSubscriptionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<Resource> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();

            PageList<Resource> results = manager.findSubscribedResources(subject, id, pc);
            return results;
        }
    }

    private String[] getSelectedChannelSubscriptions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedChannelSubscriptions");
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