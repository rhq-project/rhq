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
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ChannelManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ChannelContentSourcesUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ChannelContentSourcesUIBean";

    public ChannelContentSourcesUIBean() {
    }

    public String associateWithContentSources() {
        return "associateWithContentSources";
    }

    public String deleteSelectedChannelContentSources() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedChannelContentSources();
        int channelId = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
        int[] contentSourceIds = getIntegerArray(selected);

        if ((contentSourceIds != null) && (contentSourceIds.length > 0)) {
            try {
                ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();
                manager.removeContentSourcesFromChannel(subject, channelId, contentSourceIds);

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disassociated [" + contentSourceIds.length
                    + "] content sources from channel");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Failed to disassociate one or more content sources from channel", e);
            }
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ChannelContentSourcesDataModel(PageControlView.ChannelContentSourcesList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ChannelContentSourcesDataModel extends PagedListDataModel<ContentSource> {
        public ChannelContentSourcesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<ContentSource> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            ChannelManagerLocal manager = LookupUtil.getChannelManagerLocal();

            PageList<ContentSource> results = manager.getAssociatedContentSources(subject, id, pc);
            return results;
        }
    }

    private String[] getSelectedChannelContentSources() {
        return FacesContextUtility.getRequest().getParameterValues("selectedChannelContentSources");
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