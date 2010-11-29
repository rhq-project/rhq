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
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.problems.ProblemResourcesPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * Responsible for defining and populating the Smart GWT datasource details and
 * translating the deserialized content into specific record entries for display
 * 
 * @author Simeon Pinder
 */
public class ProblemResourcesDataSource extends RPCDataSource<DisambiguationReport<ProblemResourceComposite>> {

    public static final String FIELD_RESOURCE = "resource";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_ALERTS = "alerts";
    public static final String FIELD_AVAILABLE = "available";

    private Portlet portlet = null;
    private long oldestDate = -1;
    //configure elements
    private int maximumProblemResourcesToDisplay = -1;
    private int maximumProblemResourcesWithinHours = -1;

    /**
     * Build list of fields for the datasource and then adds them to it.
     * @param problemResourcesPortlet
     */
    public ProblemResourcesDataSource(Portlet problemResourcesPortlet) {
        this.portlet = problemResourcesPortlet;

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField resourceField = new DataSourceTextField(FIELD_RESOURCE, MSG
            .dataSource_problemResources_field_resource());
        resourceField.setPrimaryKey(true);
        fields.add(resourceField);

        DataSourceTextField locationField = new DataSourceTextField(FIELD_LOCATION, MSG
            .dataSource_problemResources_field_location());
        fields.add(locationField);

        DataSourceTextField alertsField = new DataSourceTextField(FIELD_ALERTS, MSG
            .dataSource_problemResources_field_alerts());
        fields.add(alertsField);

        DataSourceImageField availabilityField = new DataSourceImageField(FIELD_AVAILABLE, MSG
            .dataSource_problemResources_field_available());
        fields.add(availabilityField);

        return fields;
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

        GWTServiceLookup.getResourceService().findProblemResources(ctime, maxItems,
            new AsyncCallback<List<DisambiguationReport<ProblemResourceComposite>>>() {

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_problemResources_error_fetchFailure(),
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
                record.setAttribute(FIELD_RESOURCE, ReportDecorator.decorateResourceName(
                    ReportDecorator.GWT_RESOURCE_URL, report.getResourceType(), report.getOriginal().getResourceName(),
                    report.getOriginal().getResourceId(), true));
                //disambiguated resource lineage, decorated with html anchors
                record.setAttribute(FIELD_LOCATION, ReportDecorator.decorateResourceLineage(report.getParents(), true));
                //alert cnt.
                record.setAttribute(FIELD_ALERTS, report.getOriginal().getNumAlerts());
                //populate availability icon
                record.setAttribute(FIELD_AVAILABLE, ImageManager.getAvailabilityIconFromAvailType(report.getOriginal()
                    .getAvailabilityType()));

                dataValues[indx++] = record;
            }
        }
        return dataValues;
    }

    @Override
    public ListGridRecord copyValues(DisambiguationReport<ProblemResourceComposite> from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_RESOURCE, ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL, from
            .getResourceType(), from.getOriginal().getResourceName(), from.getOriginal().getResourceId(), true));
        record.setAttribute(FIELD_LOCATION, ReportDecorator.decorateResourceLineage(from.getParents(), true));
        record.setAttribute(FIELD_ALERTS, from.getOriginal().getNumAlerts());
        record.setAttribute(FIELD_AVAILABLE, ImageManager.getAvailabilityIconFromAvailType(from.getOriginal()
            .getAvailabilityType()));

        record.setAttribute("entity", from);
        return record;
    }

    @Override
    public DisambiguationReport<ProblemResourceComposite> copyValues(Record from) {
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
