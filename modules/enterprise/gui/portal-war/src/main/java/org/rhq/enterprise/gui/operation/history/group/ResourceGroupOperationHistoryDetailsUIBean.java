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

import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.operation.model.OperationParameters;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceGroupOperationHistoryDetailsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ResourceGroupOperationHistoryDetailsUIBean";

    private GroupOperationHistory history;
    private OperationParameters parameters;

    private OperationManagerLocal operationManager = LookupUtil.getOperationManager();

    private void init() {
        if (this.history == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer operationId = FacesContextUtility.getRequiredRequestParameter("opId", Integer.class);
            OperationManagerLocal operationManager = LookupUtil.getOperationManager();

            this.history = (GroupOperationHistory) operationManager
                .getOperationHistoryByHistoryId(subject, operationId);

            this.parameters = new OperationParameters(this.history);
        }
    }

    public GroupOperationHistory getHistory() {
        init();

        return this.history;
    }

    public OperationParameters getParameters() {
        init();

        return parameters;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupOperationHistoryDetailsDataModel(PageControlView.NONE, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListGroupOperationHistoryDetailsDataModel extends PagedListDataModel<ResourceOperationHistory> {
        public ListGroupOperationHistoryDetailsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ResourceOperationHistory> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            GroupOperationHistory groupHistory = getHistory();

            PageList<ResourceOperationHistory> resourceHistories = null;
            resourceHistories = operationManager.getResourceOperationHistoriesByGroupHistoryId(subject, groupHistory
                .getId(), pc);

            return resourceHistories;
        }
    }
}