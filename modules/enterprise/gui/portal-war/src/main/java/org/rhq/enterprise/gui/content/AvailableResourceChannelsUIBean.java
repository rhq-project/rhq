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
import org.rhq.core.domain.content.composite.ChannelComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AvailableResourceChannelsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "AvailableResourceChannelsUIBean";

    public AvailableResourceChannelsUIBean() {
    }

    public String subscribeSelectedResourceChannels() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedResourceChannels();
        int resourceId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] channelIds = getIntegerArray(selected);

        if ((channelIds != null) && (channelIds.length > 0)) {
            try {
                ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();
                // TODO: mazz - this shouldn't required Permission.MANAGE_INVENTORY
                manager.subscribeResourceToChannels(subject, resourceId, channelIds);

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Subscribed [" + channelIds.length
                    + "] channels to resource");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to subscribe one or more channels to resource", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResourceChannelsDataModel(PageControlView.AvailableResourceChannelsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ResourceChannelsDataModel extends PagedListDataModel<ChannelComposite> {
        public ResourceChannelsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<ChannelComposite> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();

            PageList<ChannelComposite> results = manager.getAvailableResourceSubscriptions(subject, id, pc);
            return results;
        }
    }

    private String[] getSelectedResourceChannels() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAvailableResourceChannels");
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