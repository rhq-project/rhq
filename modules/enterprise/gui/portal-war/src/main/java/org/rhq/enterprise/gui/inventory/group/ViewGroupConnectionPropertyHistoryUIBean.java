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
package org.rhq.enterprise.gui.inventory.group;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.group.AggregatePluginConfigurationUpdate;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.common.Outcomes;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewGroupConnectionPropertyHistoryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewGroupConnectionPropertyHistoryUIBean";
    public static final String VIEW_ID = "/rhq/group/inventory/connectionHistory.xhtml";

    private ResourceGroup resourceGroup;
    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    public String begin() {
        return "viewHistory";
    }

    public String deleteSelectedUpdates() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

        String[] selectedUpdateStringIds = FacesContextUtility.getRequest().getParameterValues("selectedUpdates");
        Integer[] selectedUpdateIds = StringUtility.getIntegerArray(selectedUpdateStringIds);

        try {
            int deleteCount = configurationManager.deleteAggregatePluginConfigurationUpdates(subject, resourceGroup
                .getId(), selectedUpdateIds);
            if (deleteCount == selectedUpdateIds.length) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + selectedUpdateIds.length
                    + " group connection property updates.");
            } else {
                int failedToDelete = selectedUpdateIds.length - deleteCount;
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Unknown error deleting " + failedToDelete
                    + " group connection property updates.");
            }
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to delete selected group connection property updates.", e);
        }

        return Outcomes.SUCCESS;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupConnectionPropertyUpdateHistoryDataModel(
                PageControlView.GroupConnectionPropertyUpdateHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListGroupConnectionPropertyUpdateHistoryDataModel extends
        PagedListDataModel<AggregatePluginConfigurationUpdate> {
        public ListGroupConnectionPropertyUpdateHistoryDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<AggregatePluginConfigurationUpdate> fetchPage(PageControl pc) {
            ResourceGroup requestResourceGroup = EnterpriseFacesContextUtility.getResourceGroup();
            ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

            if (requestResourceGroup == null) {
                requestResourceGroup = resourceGroup;
            } else {
                resourceGroup = requestResourceGroup;
            }

            PageList<AggregatePluginConfigurationUpdate> results = null;
            results = configurationManager.findAggregatePluginConfigurationUpdatesByGroupId(
                requestResourceGroup.getId(), pc);
            return results;
        }
    }
}