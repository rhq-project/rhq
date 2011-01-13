/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.operation;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceDateTimeField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * Responsible for defining and populating the Smart GWT datasource details and
 * translating the deserialized content into specific record entries for display
 * 
 * @author Simeon Pinder
 */
public class RecentOperationsDataSource extends
    RPCDataSource<DisambiguationReport<ResourceOperationLastCompletedComposite>> {

    // fields
    public static final String FIELD_RESOURCE = "resource";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_OPERATION = "operation";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_STATUS = "status";

    private Portlet portlet;

    //config attributes
    private boolean operationsRangeLastEnabled = false;
    private int operationsRangeCompleted = -1;

    /** Build list of fields for the datasource and then adds them to it.
     */
    public RecentOperationsDataSource(Portlet portlet) {
        this.portlet = portlet;

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField resourceField = new DataSourceTextField(FIELD_RESOURCE, MSG
            .dataSource_recentOperations_field_resource());
        resourceField.setPrimaryKey(true);
        fields.add(resourceField);

        DataSourceTextField locationField = new DataSourceTextField(FIELD_LOCATION, MSG
            .dataSource_recentOperations_field_location(), 200);
        fields.add(locationField);

        DataSourceTextField operationField = new DataSourceTextField(FIELD_OPERATION, MSG
            .dataSource_recentOperations_field_operation());
        fields.add(operationField);

        DataSourceDateTimeField timeField = new DataSourceDateTimeField(FIELD_TIME, MSG
            .dataSource_recentOperations_field_time());
        fields.add(timeField);

        DataSourceTextField statusField = new DataSourceTextField(FIELD_STATUS, MSG
            .dataSource_recentOperations_field_status());
        fields.add(statusField);

        return fields;
    }

    /** Fetch the ProblemResource data, and populate the response object appropriately.
     * 
     * @param request incoming request
     * @param response outgoing response
     */
    public void executeFetch(final DSRequest request, final DSResponse response) {
        PageControl pageControl = new PageControl();
        //retrieve current portlet display settings
        if ((this.portlet != null) && (this.portlet instanceof OperationsPortlet)) {
            OperationsPortlet operationsPortlet = (OperationsPortlet) this.portlet;
            //populate criteria with portlet preferences defined.
            if (operationsPortlet != null) {
                if (isOperationsRangeCompletedEnabled()) {
                    pageControl.setPageSize(getOperationsRangeCompleted());
                    operationsPortlet.getCompletedOperationsGrid().setEmptyMessage(
                        OperationsPortlet.RANGE_DISABLED_MESSAGE_DEFAULT);
                } else {//show the component, return no results and indicate that you've disabled this display
                    pageControl.setPageSize(0);
                    operationsPortlet.getCompletedOperationsGrid().setEmptyMessage(
                        OperationsPortlet.RANGE_DISABLED_MESSAGE);
                    response.setData(null);
                    response.setTotalRows(0);
                    //pass off for processing
                    processResponse(request.getRequestId(), response);
                    return;
                }
            }
        }

        int resourceId = getFilter(request, "id", Integer.class);

        GWTServiceLookup.getOperationService().findRecentCompletedOperations(resourceId, pageControl,
            new AsyncCallback<List<DisambiguationReport<ResourceOperationLastCompletedComposite>>>() {

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_recentOperations_error_fetchFailure(),
                        throwable);
                }

                public void onSuccess(
                    List<DisambiguationReport<ResourceOperationLastCompletedComposite>> recentOperationsList) {

                    //translate DisambiguationReport into dataset entries
                    response.setData(buildList(recentOperationsList));
                    //entry count
                    if (null != recentOperationsList) {
                        response.setTotalRows(recentOperationsList.size());
                    } else {
                        response.setTotalRows(0);
                    }
                    //pass off for processing
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    /** Translates the DisambiguationReport of ResourceOperationLastCompletedComposites into specific
     *  and ordered record values.
     * 
     * @param list DisambiguationReport of entries.
     * @return Record[] ordered record entries.
     */
    protected Record[] buildList(List<DisambiguationReport<ResourceOperationLastCompletedComposite>> list) {

        ListGridRecord[] dataValues = null;
        if (list != null) {
            dataValues = new ListGridRecord[list.size()];
            int indx = 0;

            for (DisambiguationReport<ResourceOperationLastCompletedComposite> report : list) {
                ListGridRecord record = new ListGridRecord();
                //disambiguated Resource name, decorated with html anchors to problem resources 
                record.setAttribute(FIELD_RESOURCE, ReportDecorator.decorateResourceName(
                    ReportDecorator.GWT_RESOURCE_URL, report.getResourceType(), report.getOriginal().getResourceName(),
                    report.getOriginal().getResourceId(), true));
                //disambiguated resource lineage, decorated with html anchors
                record.setAttribute(FIELD_LOCATION, ReportDecorator.decorateResourceLineage(report.getParents(), true));
                //operation name.
                record.setAttribute(FIELD_OPERATION, report.getOriginal().getOperationName());
                //timestamp.
                record.setAttribute(FIELD_TIME, new Date(report.getOriginal().getOperationStartTime()));
                String link = generateResourceOperationStatusLink(report);
                record.setAttribute(FIELD_STATUS, link);

                dataValues[indx++] = record;
            }
        }
        return dataValues;
    }

    /** Generates the ResourceOperationHistory status link from DisambiguationReport passed in.
     * 
     * @param report
     * @return html string for display in table.
     */
    private String generateResourceOperationStatusLink(
        DisambiguationReport<ResourceOperationLastCompletedComposite> report) {

        OperationRequestStatus opStatus = report.getOriginal().getOperationStatus();
        String iconLink = ImageManager.getOperationResultsIcon(opStatus);
        String statusStr = "";
        switch (opStatus) {
        case SUCCESS: {
            statusStr = MSG.common_status_success();
            break;
        }
        case FAILURE: {
            statusStr = MSG.common_status_failed();
            break;
        }
        case INPROGRESS: {
            statusStr = MSG.common_status_inprogress();
            break;
        }
        case CANCELED: {
            statusStr = MSG.common_status_canceled();
            break;
        }
        }

        String link = "<a href='"
            + LinkManager.getSubsystemResourceOperationHistoryLink(report.getOriginal().getResourceId(), report
                .getOriginal().getOperationHistoryId()) + "'>";
        String img = "<img alt='" + statusStr + "' title='" + statusStr + "' src='";
        img += ImageManager.getFullImagePath(iconLink);
        link = link + img + "'></img></a>";
        return link;
    }

    @Override
    public DisambiguationReport<ResourceOperationLastCompletedComposite> copyValues(Record from) {
        throw new UnsupportedOperationException("ResourceOperations data is read only");
    }

    @Override
    public ListGridRecord copyValues(DisambiguationReport<ResourceOperationLastCompletedComposite> from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_RESOURCE, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL, from
            .getResourceType(), from.getOriginal().getResourceName(), from.getOriginal().getResourceId(), true));
        record.setAttribute(FIELD_LOCATION, ReportDecorator.decorateResourceLineage(from.getParents(), true));
        record.setAttribute(FIELD_OPERATION, from.getOriginal().getOperationName());
        record.setAttribute(FIELD_TIME, from.getOriginal().getOperationStartTime());
        record.setAttribute(FIELD_STATUS, generateResourceOperationStatusLink(from));

        record.setAttribute("entity", from);

        return record;
    }

    public boolean isOperationsRangeCompletedEnabled() {
        return operationsRangeLastEnabled;
    }

    public void setOperationsRangeCompleteEnabled(boolean operationsRangeLastEnabled) {
        this.operationsRangeLastEnabled = operationsRangeLastEnabled;
    }

    public int getOperationsRangeCompleted() {
        return operationsRangeCompleted;
    }

    public void setOperationsRangeCompleted(int operationsRangeCompleted) {
        this.operationsRangeCompleted = operationsRangeCompleted;
    }

}
