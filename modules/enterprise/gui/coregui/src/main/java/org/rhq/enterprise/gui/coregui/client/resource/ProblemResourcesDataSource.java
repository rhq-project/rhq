package org.rhq.enterprise.gui.coregui.client.resource;

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
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;

/** Responsible for defining and populating the Smart GWT datasource details and
 *  translating the deserialized content into specific record entries for display
 * 
 * @author spinder
 */
public class ProblemResourcesDataSource extends DataSource {
    public static final String resource = "resource";
    public static final String location = "location";
    public static final String alerts = "alerts";
    public static final String available = "available";

    /** Build list of fields for the datasource and then adds them to it.
     */
    public ProblemResourcesDataSource() {
        setClientOnly(false);
        setDataProtocol(DSProtocol.CLIENTCUSTOM);
        setDataFormat(DSDataFormat.CUSTOM);

        DataSourceTextField resourceField = new DataSourceTextField(resource, "Resource");
        resourceField.setPrimaryKey(true);

        DataSourceTextField locationField = new DataSourceTextField(location, "Location");

        DataSourceTextField alertsField = new DataSourceTextField(alerts, "Alerts");

        DataSourceImageField availablilityField = new DataSourceImageField(available, "Current Availability");

        setFields(resourceField, locationField, alertsField, availablilityField);
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

        ResourceCriteria c = new ResourceCriteria();
        GWTServiceLookup.getResourceService().findProblemResources(c,
            new AsyncCallback<List<DisambiguationReport<ProblemResourceComposite>>>() {

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError("Failed to load resources with alerts/unavailability.",
                        throwable);
                }

                public void onSuccess(List<DisambiguationReport<ProblemResourceComposite>> problemResourcesList) {

                    //translate DisambiguationReport into dataset entries
                    response.setData(buildList(problemResourcesList));
                    //entry count
                    if (null != problemResourcesList) {
                        response.setTotalRows(problemResourcesList.size());
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
    protected Record[] buildList(List<DisambiguationReport<ProblemResourceComposite>> list) {

        ListGridRecord[] dataValues = null;
        if (list != null) {
            dataValues = new ListGridRecord[list.size()];
            int indx = 0;

            for (DisambiguationReport<ProblemResourceComposite> report : list) {
                ListGridRecord record = new ListGridRecord();
                //disambiguated Resource name, decorated with html anchors to problem resources 
                record.setAttribute(resource, ReportDecorator.decorateResourceName(report.getResourceType(), report
                    .getOriginal().getResourceName(), report.getOriginal().getResourceId()));
                //disambiguated resource lineage, decorated with html anchors
                record.setAttribute(location, ReportDecorator.decorateResourceLineage(report.getParents()));
                //alert cnt.
                record.setAttribute(alerts, report.getOriginal().getNumAlerts());
                //populate availability icon
                if (report.getOriginal().getAvailabilityType().compareTo(AvailabilityType.DOWN) == 0) {
                    record.setAttribute(available, "/images/icons/availability_red_16.png");
                } else {
                    record.setAttribute(available, "/images/icons/availability_green_16.png");
                }

                dataValues[indx++] = record;
            }
        }
        return dataValues;
    }
}
