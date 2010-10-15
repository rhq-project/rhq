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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/** Responsible for defining and populating the Smart GWT datasource details and
 *  translating the deserialized content into specific record entries for display
 * 
 * @author Simeon Pinder
 */
public class ProblemResourcesDataSource extends RPCDataSource<DisambiguationReport<ProblemResourceComposite>> {
    public static final String resource = "resource";
    public static final String location = "location";
    public static final String alerts = "alerts";
    public static final String available = "available";
    private Portlet portlet = null;
    private long oldestDate = -1;
    //configure elements
    private int maximumProblemResourcesToDisplay = -1;
    private int maximumProblemResourcesWithinHours = -1;

    /** Build list of fields for the datasource and then adds them to it.
     * @param problemResourcesPortlet
     */
    public ProblemResourcesDataSource(Portlet problemResourcesPortlet) {
        this.portlet = problemResourcesPortlet;
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

        DataSourceTextField locationField = new DataSourceTextField(location, "Location");
        fields.add(locationField);

        DataSourceTextField alertsField = new DataSourceTextField(alerts, "Alerts");
        fields.add(alertsField);

        DataSourceImageField availabilityField = new DataSourceImageField(available, "Current Availability");
        fields.add(availabilityField);

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

        long ctime = -1;
        int maxItems = -1;
        //retrieve current portlet display settings
        if ((this.portlet != null) && (this.portlet instanceof ProblemResourcesPortlet)) {
            ProblemResourcesPortlet problemPortlet = (ProblemResourcesPortlet) this.portlet;
            //populate criteria with portlet preferences defined.
            if (problemPortlet != null) {
                if (getMaximumProblemResourcesToDisplay() > 0) {
                    maxItems = getMaximumProblemResourcesToDisplay();
                }
                //define the time window
                if (getMaximumProblemResourcesWithinHours() > 0) {
                    ctime = System.currentTimeMillis() - (getMaximumProblemResourcesWithinHours() * 60 * 60 * 1000);
                    setOldestDate(ctime);
                }
            }
        }

        if (userStillLoggedIn()) {
            GWTServiceLookup.getResourceService().findProblemResources(ctime, maxItems,
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
        } else {
            Log.debug("user not logged in. Not fetching resources with alerts/unavailability.");
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
        }
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
                record.setAttribute(resource, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL,
                    report.getResourceType(), report.getOriginal().getResourceName(), report.getOriginal()
                        .getResourceId()));
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

    @Override
    public ListGridRecord copyValues(DisambiguationReport<ProblemResourceComposite> from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(resource, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL, from
            .getResourceType(), from.getOriginal().getResourceName(), from.getOriginal().getResourceId()));
        record.setAttribute(location, ReportDecorator.decorateResourceLineage(from.getParents()));
        record.setAttribute(alerts, from.getOriginal().getNumAlerts());
        if (from.getOriginal().getAvailabilityType().compareTo(AvailabilityType.DOWN) == 0) {
            record.setAttribute(available, "/images/icons/availability_red_16.png");
        } else {
            record.setAttribute(available, "/images/icons/availability_green_16.png");
        }

        record.setAttribute("entity", from);
        return record;
    }

    @Override
    public DisambiguationReport<ProblemResourceComposite> copyValues(ListGridRecord from) {
        throw new UnsupportedOperationException("ProblemResource data is read only");
    }

    public long getOldestDate() {
        return oldestDate;
    }

    public void setOldestDate(long oldestDate) {
        this.oldestDate = oldestDate;
    }

    public int getMaximumProblemResourcesToDisplay() {
        return maximumProblemResourcesToDisplay;
    }

    public void setMaximumProblemResourcesToDisplay(int maxPerRow) {
        this.maximumProblemResourcesToDisplay = maxPerRow;
    }

    public void setMaximumProblemResourcesWithinHours(int maximumProblemResourcesWithinHours) {
        this.maximumProblemResourcesWithinHours = maximumProblemResourcesWithinHours;
    }

    public int getMaximumProblemResourcesWithinHours() {
        return maximumProblemResourcesWithinHours;
    }
}
