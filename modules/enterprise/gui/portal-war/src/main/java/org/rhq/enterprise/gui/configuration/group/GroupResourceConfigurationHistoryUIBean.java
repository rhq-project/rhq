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
package org.rhq.enterprise.gui.configuration.group;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class GroupResourceConfigurationHistoryUIBean extends PagedDataTableUIBean {
    public static final String VIEW_ID = "/rhq/group/configuration/history.xhtml";

    public static final String MANAGED_BEAN_NAME = "GroupResourceConfigurationHistoryUIBean";

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupResourceConfigurationUpdatesDataModel(
                PageControlView.GroupResourceConfigurationUpdateHistory,
                GroupResourceConfigurationHistoryUIBean.MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    public String deleteSelectedUpdates() {
        String[] selectedUpdates = getSelectedUpdates();
        Integer[] groupConfigUpdateIds = StringUtility.getIntegerArray(selectedUpdates);

        try {
            configurationManager.deleteGroupResourceConfigurationUpdates(getSubject(), getResourceGroup().getId(),
                groupConfigUpdateIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + groupConfigUpdateIds.length
                + " group updates.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete selected group updates.", e);
        }

        return "success";
    }

    private class ListGroupResourceConfigurationUpdatesDataModel extends
        PagedListDataModel<GroupResourceConfigurationUpdate> {

        public ListGroupResourceConfigurationUpdatesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<GroupResourceConfigurationUpdate> fetchPage(PageControl pc) {
            return configurationManager.findGroupResourceConfigurationUpdates(getResourceGroup().getId(),
                pc);
        }
    }

    private String[] getSelectedUpdates() {
        return FacesContextUtility.getRequest().getParameterValues("selectedUpdates");
    }

}
