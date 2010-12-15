/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;

import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.OperationHistoryDataSource;

/**
 * @author Ian Springer
 */
public class ResourceOperationHistoryDataSource extends OperationHistoryDataSource {

    public static abstract class CriteriaField {
        public static final String RESOURCE_ID = "resourceId";
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        if (request.getCriteria().getValues().containsKey(CriteriaField.RESOURCE_ID)) {
            int resourceId = Integer.parseInt((String)request.getCriteria().getValues().get(
                CriteriaField.RESOURCE_ID));
            criteria.addFilterResourceIds(resourceId);
        }

        criteria.setPageControl(getPageControl(request));

        operationService.findResourceOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceOperationHistory>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_operationHistory_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<ResourceOperationHistory> result) {
                    response.setData(buildRecords(result));
                    processResponse(request.getRequestId(), response);
                }
            });
    }


}
