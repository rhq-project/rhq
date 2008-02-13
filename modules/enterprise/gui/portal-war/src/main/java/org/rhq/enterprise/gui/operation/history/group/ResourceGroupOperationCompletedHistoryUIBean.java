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
package org.rhq.enterprise.gui.operation.history.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceGroupOperationCompletedHistoryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ResourceGroupOperationCompletedHistoryUIBean";

    private ResourceGroup resourceGroup;
    private OperationManagerLocal manager = LookupUtil.getOperationManager();

    public ResourceGroupOperationCompletedHistoryUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupOperationCompletedHistoryDataModel(
                PageControlView.ResourceGroupOperationCompletedHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public String delete() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedItems = FacesContextUtility.getRequest().getParameterValues("completedSelectedItems");

        List<String> success = new ArrayList<String>();
        Map<String, String> failure = new HashMap<String, String>();

        String next = null;
        Integer doomed;

        for (int i = 0; i < selectedItems.length; i++) {
            try {
                next = selectedItems[i];
                doomed = Integer.valueOf(next);

                manager.deleteOperationHistory(subject, doomed, true);

                success.add(next);
            } catch (Exception e) {
                failure.put(next, ThrowableUtil.getAllMessages(e, true));
            }
        }

        if (success.size() > 0) {
            // one success message for all successful deletions
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted operation records: "
                + StringUtility.getListAsDelimitedString(success));
        }

        for (Map.Entry<String, String> error : failure.entrySet()) {
            // one message per failed deletion (hopefully rare)
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete operation record: "
                + error.getKey() + ". Cause: " + error.getValue());
        }

        return "success";
    }

    private class ListGroupOperationCompletedHistoryDataModel extends PagedListDataModel<GroupOperationHistory> {
        public ListGroupOperationCompletedHistoryDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<GroupOperationHistory> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceGroup requestGroup = EnterpriseFacesContextUtility.getResourceGroup();
            OperationManagerLocal manager = LookupUtil.getOperationManager();

            if (requestGroup == null) {
                requestGroup = resourceGroup; // request not associated with a resource - use the resource we used before
            } else {
                resourceGroup = requestGroup; // request switched the resource this UI bean is using
            }

            PageList<GroupOperationHistory> results;
            results = manager.getCompletedGroupOperationHistories(subject, resourceGroup.getId(), pc);
            return results;
        }
    }
}