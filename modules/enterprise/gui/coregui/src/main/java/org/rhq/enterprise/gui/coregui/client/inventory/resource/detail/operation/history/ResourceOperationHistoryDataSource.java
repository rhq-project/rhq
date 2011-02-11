/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
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

import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ian Springer
 */
public class ResourceOperationHistoryDataSource extends AbstractOperationHistoryDataSource<ResourceOperationHistory> {

    public static abstract class Field extends AbstractOperationHistoryDataSource.Field {
        public static final String RESOURCE = "resource";
    }

    public static abstract class CriteriaField {
        public static final String RESOURCE_ID = "resourceId";
        public static final String GROUP_OPERATION_HISTORY_ID = "groupOperationHistoryId";
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField resourceField = new DataSourceTextField(Field.RESOURCE);
        fields.add(resourceField);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        if (request.getCriteria().getValues().containsKey(CriteriaField.RESOURCE_ID)) {
            int resourceId = getFilter(request, CriteriaField.RESOURCE_ID, Integer.class);
            criteria.addFilterResourceIds(resourceId);
        }

        if (request.getCriteria().getValues().containsKey(CriteriaField.GROUP_OPERATION_HISTORY_ID)) {
            int groupOperationHistoryId = getFilter(request, CriteriaField.GROUP_OPERATION_HISTORY_ID, Integer.class);
            criteria.addFilterGroupOperationHistoryId(groupOperationHistoryId);
        }

        criteria.setPageControl(getPageControl(request));

        operationService.findResourceOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<DisambiguationReport<ResourceOperationHistory>>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_operationHistory_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<DisambiguationReport<ResourceOperationHistory>> result) {
                    response.setData(buildRecordsFromDisambiguationReports(result));
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    private ListGridRecord[] buildRecordsFromDisambiguationReports(
            Collection<DisambiguationReport<ResourceOperationHistory>> disambiguationReports) {
        List<ResourceOperationHistory> dataObjects = new ArrayList<ResourceOperationHistory>(disambiguationReports.size());
        Map<Integer, String> idToDisambiguatedNameMap = new HashMap<Integer, String>();
        for (DisambiguationReport<ResourceOperationHistory> disambiguationReport : disambiguationReports) {
            ResourceOperationHistory dataObject = disambiguationReport.getOriginal();
            dataObjects.add(dataObject);
            String disambiguatedName = ReportDecorator.decorateDisambiguationReport(disambiguationReport,
                    disambiguationReport.getId(), true);
            idToDisambiguatedNameMap.put(dataObject.getId(), disambiguatedName);
        }

        ListGridRecord[] records = buildRecords(dataObjects, true);
        for (ListGridRecord record : records) {
            Integer id = record.getAttributeAsInt(Field.ID);
            String disambiguatedName = idToDisambiguatedNameMap.get(id);
            record.setAttribute(Field.RESOURCE, disambiguatedName);
        }

        return records;
    }

}
