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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceDateTimeField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
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
public class ScheduledOperationsDataSource extends
    RPCDataSource<DisambiguationReport<ResourceOperationScheduleComposite>> {

    // fields
    public static final String FIELD_RESOURCE = "resource";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_OPERATION = "operation";
    public static final String FIELD_TIME = "time";

    //config settings
    private boolean operationsRangeNextEnabled = false;
    private int operationsRangeScheduled = -1;
    private Portlet portlet;

    /** Build list of fields for the datasource and then adds them to it.
     */
    public ScheduledOperationsDataSource(Portlet portlet) {
        this.portlet = portlet;

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField resourceField = new DataSourceTextField(FIELD_RESOURCE,
            MSG.dataSource_scheduledOperations_field_resource());
        resourceField.setPrimaryKey(true);
        fields.add(resourceField);

        DataSourceTextField locationField = new DataSourceTextField(FIELD_LOCATION,
            MSG.dataSource_scheduledOperations_field_location(), 200);
        fields.add(locationField);

        DataSourceTextField operationField = new DataSourceTextField(FIELD_OPERATION,
            MSG.dataSource_scheduledOperations_field_operation());
        fields.add(operationField);

        DataSourceDateTimeField timeField = new DataSourceDateTimeField(FIELD_TIME,
            MSG.dataSource_scheduledOperations_field_time());
        fields.add(timeField);

        return fields;
    }

    /** Fetch the ProblemResource data, and populate the response object appropriately.
     * 
     * @param request incoming request
     * @param response outgoing response
     */
    public void executeFetch(final DSRequest request, final DSResponse response) {

        int pageSize = -1;
        //retrieve current portlet display settings
        if ((this.portlet != null) && (this.portlet instanceof OperationsPortlet)) {
            OperationsPortlet operationsPortlet = (OperationsPortlet) this.portlet;
            //populate criteria with portlet preferences defined.
            if (operationsPortlet != null) {
                if (isOperationsRangeScheduleEnabled()) {
                    pageSize = getOperationsRangeScheduled();
                    operationsPortlet.getScheduledOperationsGrid().setEmptyMessage(
                        OperationsPortlet.RANGE_DISABLED_MESSAGE_DEFAULT);
                } else {//show the component, return no results and indicate that you've disabled this display
                    pageSize = 0;
                    operationsPortlet.getScheduledOperationsGrid().setEmptyMessage(
                        OperationsPortlet.RANGE_DISABLED_MESSAGE);
                    response.setData(null);
                    response.setTotalRows(0);
                    //pass off for processing
                    processResponse(request.getRequestId(), response);
                    return;
                }
            }
        }

        GWTServiceLookup.getOperationService().findScheduledOperations(pageSize,
            new AsyncCallback<List<DisambiguationReport<ResourceOperationScheduleComposite>>>() {

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_scheduledOperations_error_fetchFailure(),
                        throwable);
                }

                public void onSuccess(List<DisambiguationReport<ResourceOperationScheduleComposite>> scheduledOpsList) {

                    //translate DisambiguationReport into dataset entries
                    response.setData(buildList(scheduledOpsList));
                    //entry count
                    if (null != scheduledOpsList) {
                        response.setTotalRows(scheduledOpsList.size());
                    } else {
                        response.setTotalRows(0);
                    }
                    //pass off for processing
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    /** Translates the DisambiguationReport of ProblemResourceComposites into specific
     *  and ordered record values.
     * 
     * @param list DisambiguationReport of entries.
     * @return Record[] ordered record entries.
     */
    protected Record[] buildList(List<DisambiguationReport<ResourceOperationScheduleComposite>> list) {

        ListGridRecord[] dataValues = null;
        if (list != null) {
            dataValues = new ListGridRecord[list.size()];
            int indx = 0;

            for (DisambiguationReport<ResourceOperationScheduleComposite> report : list) {
                ListGridRecord record = new ListGridRecord();

                //disambiguated Resource name, decorated with html anchors to problem resources 
                record.setAttribute(FIELD_RESOURCE, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL,
                    report.getResourceType(), report.getOriginal().getResourceName(), report.getOriginal()
                        .getResourceId(), true));
                //disambiguated resource lineage, decorated with html anchors
                record.setAttribute(FIELD_LOCATION, ReportDecorator.decorateResourceLineage(report.getParents(), true));
                //operation name.
                record.setAttribute(FIELD_OPERATION, report.getOriginal().getOperationName());
                //timestamp.
                record.setAttribute(FIELD_TIME, new Date(report.getOriginal().getOperationNextFireTime()));

                dataValues[indx++] = record;
            }
        }
        return dataValues;
    }

    @Override
    public DisambiguationReport<ResourceOperationScheduleComposite> copyValues(Record from) {
        throw new UnsupportedOperationException("ResourceOperations data is read only");
    }

    @Override
    public ListGridRecord copyValues(DisambiguationReport<ResourceOperationScheduleComposite> from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_RESOURCE, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL,
            from.getResourceType(), from.getOriginal().getResourceName(), from.getOriginal().getResourceId(), true));
        record.setAttribute(FIELD_LOCATION, ReportDecorator.decorateResourceLineage(from.getParents(), true));
        record.setAttribute(FIELD_OPERATION, from.getOriginal().getOperationName());
        record.setAttribute(FIELD_TIME, from.getOriginal().getOperationNextFireTime());

        record.setAttribute("entity", from);

        return record;

    }

    public boolean isOperationsRangeScheduleEnabled() {
        return operationsRangeNextEnabled;
    }

    public void setOperationsRangeScheduleEnabled(boolean operationsRangeNextEnabled) {
        this.operationsRangeNextEnabled = operationsRangeNextEnabled;
    }

    public int getOperationsRangeScheduled() {
        return operationsRangeScheduled;
    }

    public void setOperationsRangeScheduled(int operationsRangeScheduled) {
        this.operationsRangeScheduled = operationsRangeScheduled;
    }

}
