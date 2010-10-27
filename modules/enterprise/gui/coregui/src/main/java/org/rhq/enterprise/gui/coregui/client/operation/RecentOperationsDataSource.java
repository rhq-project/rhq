package org.rhq.enterprise.gui.coregui.client.operation;

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
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/** Responsible for defining and populating the Smart GWT datasource details and
 *  translating the deserialized content into specific record entries for display
 * 
 * @author Simeon Pinder
 */
public class RecentOperationsDataSource extends
    RPCDataSource<DisambiguationReport<ResourceOperationLastCompletedComposite>> {
    //ui keys
    public static final String resource = "resource";
    public static final String location = "location";
    public static final String operation = "operation";
    public static final String time = "time";
    public static final String status = "status";
    private Portlet portlet;

    //config attributes
    private boolean operationsRangeLastEnabled = false;
    private int operationsRangeCompleted = -1;
    public static String RANGE_DISABLED_MESSAGE = "(Results currently disabled. Change settings to enable results.)";

    /** Build list of fields for the datasource and then adds them to it.
     */
    public RecentOperationsDataSource(Portlet portlet) {
        this.portlet = portlet;
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField resourceField = new DataSourceTextField(resource, "Resource");
        resourceField.setPrimaryKey(true);
        fields.add(resourceField);

        DataSourceTextField locationField = new DataSourceTextField(location, "Location", 200);
        fields.add(locationField);

        DataSourceTextField operationField = new DataSourceTextField(operation, "Operation");
        fields.add(operationField);

        DataSourceDateTimeField timeField = new DataSourceDateTimeField(time, "Date/Time");
        fields.add(timeField);

        DataSourceTextField statusField = new DataSourceTextField(status, "Status");
        fields.add(statusField);

        return fields;
    }

    /* Intercept DSRequest object to pipe into custom fetch request.
     * (non-Javadoc)
     * @see com.smartgwt.client.data.DataSource#transformRequest(com.smartgwt.client.data.DSRequest)
     */
    protected Object transformRequest(DSRequest request) {
        DSResponse response = new DSResponse();
        response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
        // Assume success
        response.setStatus(0);
        switch (request.getOperationType()) {
        case FETCH:
            executeFetch(request, response);
            break;
        default:
            break;
        }

        return request.getData();
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
                if (isOperationsRangeCompletedEnabled()) {
                    pageSize = getOperationsRangeCompleted();
                    operationsPortlet.getCompletedOperationsGrid().setEmptyMessage(
                        OperationsPortlet.RANGE_DISABLED_MESSAGE_DEFAULT);
                } else {//show the component, return no results and indicate that you've disabled this display
                    pageSize = 0;
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

        if (userStillLoggedIn()) {//check session validity
            GWTServiceLookup.getOperationService().findRecentCompletedOperations(pageSize,
                new AsyncCallback<List<DisambiguationReport<ResourceOperationLastCompletedComposite>>>() {

                    public void onFailure(Throwable throwable) {
                        CoreGUI.getErrorHandler().handleError("Failed to load recently completed operations.",
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
        } else {
            Log.debug("user not logged in. Not fetching recently completed operations.");
            //answer datasource
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
        }
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
                record.setAttribute(resource, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL,
                    report.getResourceType(), report.getOriginal().getResourceName(), report.getOriginal()
                        .getResourceId(), true));
                //disambiguated resource lineage, decorated with html anchors
                record.setAttribute(location, ReportDecorator.decorateResourceLineage(report.getParents(), true));
                //operation name.
                record.setAttribute(operation, report.getOriginal().getOperationName());
                //timestamp.
                record.setAttribute(time, new Date(report.getOriginal().getOperationStartTime()));
                String link = generateResourceOperationStatusLink(report);
                record.setAttribute(status, link);

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
        //TODO: refactor this out for more general case
        String link = "<a href='" + "/rhq/resource/operation/resourceOperationHistoryDetails-plain.xhtml?id="
            + report.getOriginal().getResourceId() + "&opId=" + report.getOriginal().getResourceId() + "'>";
        String img = "<img alt='" + report.getOriginal().getOperationStatus() + "' title='"
            + report.getOriginal().getOperationStatus() + "' src='/images/icons/";
        if (report.getOriginal().getOperationStatus().compareTo(OperationRequestStatus.SUCCESS) == 0) {
            img += "availability_green_16.png'";
        } else if (report.getOriginal().getOperationStatus().compareTo(OperationRequestStatus.FAILURE) == 0) {
            img += "availability_red_16.png";
        } else if (report.getOriginal().getOperationStatus().compareTo(OperationRequestStatus.CANCELED) == 0) {
            img += "availability_yellow_16.png";
        } else {
            img += "availability_grey_16.png";
        }
        link = link + img + "'></img></a>";
        return link;
    }

    @Override
    public DisambiguationReport<ResourceOperationLastCompletedComposite> copyValues(ListGridRecord from) {
        throw new UnsupportedOperationException("ResourceOperations data is read only");
    }

    @Override
    public ListGridRecord copyValues(DisambiguationReport<ResourceOperationLastCompletedComposite> from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(resource, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL, from
            .getResourceType(), from.getOriginal().getResourceName(), from.getOriginal().getResourceId(), true));
        record.setAttribute(location, ReportDecorator.decorateResourceLineage(from.getParents(), true));
        record.setAttribute(operation, from.getOriginal().getOperationName());
        record.setAttribute(time, from.getOriginal().getOperationStartTime());
        record.setAttribute(status, generateResourceOperationStatusLink(from));

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
