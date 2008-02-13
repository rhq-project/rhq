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
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
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

public class ResourceGroupOperationPendingHistoryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ResourceGroupOperationPendingHistoryUIBean";

    private ResourceGroup resourceGroup;
    private OperationManagerLocal manager = LookupUtil.getOperationManager();

    /*
     * the schedule page (in 1.x known at the current subtab) always showed the status of the currently running
     * operation; and since we can have many operations INPROGRESS, the one that's currently being executed on the
     * resource must be the first one scheduled (the oldest) of them
     */
    private ResourceOperationHistory oldestInProgressResourceOperation = null;

    public ResourceGroupOperationPendingHistoryUIBean() {
    }

    public ResourceOperationHistory getOldestInProgressResourceOperation() {
        if (oldestInProgressResourceOperation == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource resource = EnterpriseFacesContextUtility.getResource();

            oldestInProgressResourceOperation = manager.getOldestInProgressResourceOperation(subject, resource.getId());
        }

        return oldestInProgressResourceOperation;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupOperationPendingHistoryDataModel(
                PageControlView.ResourceGroupOperationPendingHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public String cancel() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedItems = FacesContextUtility.getRequest().getParameterValues("pendingSelectedItems");

        List<String> success = new ArrayList<String>();
        Map<String, String> failure = new HashMap<String, String>();

        String next = null;
        Integer doomed = null;

        for (int i = 0; i < selectedItems.length; i++) {
            try {
                next = selectedItems[i];
                doomed = Integer.valueOf(next);

                manager.cancelOperationHistory(subject, doomed, false);

                success.add(next);
            } catch (Exception e) {
                failure.put(next, ThrowableUtil.getAllMessages(e, true));
            }
        }

        if (success.size() > 0) {
            // one success message for all successful cancel request
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Sent cancel request to the follow in-progress operations: "
                    + StringUtility.getListAsDelimitedString(success));
        }

        for (Map.Entry<String, String> error : failure.entrySet()) {
            // one message per failure (hopefully rare)
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to send the cancel request to: "
                + error.getKey() + ". Cause: " + error.getValue());
        }

        return "success";
    }

    private class ListGroupOperationPendingHistoryDataModel extends PagedListDataModel<GroupOperationHistory> {
        public ListGroupOperationPendingHistoryDataModel(PageControlView view, String beanName) {
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
            results = manager.getPendingGroupOperationHistories(subject, requestGroup.getId(), pc);
            return results;
        }
    }
}