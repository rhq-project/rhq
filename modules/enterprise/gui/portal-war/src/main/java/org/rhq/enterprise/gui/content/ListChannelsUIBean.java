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
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListChannelsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListChannelsUIBean";

    private ChannelManagerLocal contentSourceManager = LookupUtil.getChannelManagerLocal();

    public ListChannelsUIBean() {
    }

    public String createNewChannel() {
        return "createNewChannel";
    }

    public String deleteSelectedChannels() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedChannels();
        Integer[] ids = getIntegerArray(selected);

        if (ids.length > 0) {
            try {
                for (Integer id : ids) {
                    contentSourceManager.deleteChannel(subject, id);
                }

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted [" + ids.length + "] channels.");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete channels.", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListChannelsDataModel(PageControlView.ChannelsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListChannelsDataModel extends PagedListDataModel<Channel> {
        public ListChannelsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<Channel> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();

            PageList<Channel> results = manager.getAllChannels(subject, pc);
            return results;
        }
    }

    private String[] getSelectedChannels() {
        return FacesContextUtility.getRequest().getParameterValues("selectedChannels");
    }

    private Integer[] getIntegerArray(String[] input) {
        if (input == null) {
            return new Integer[0];
        }

        Integer[] output = new Integer[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.valueOf(input[i]);
        }

        return output;
    }
}