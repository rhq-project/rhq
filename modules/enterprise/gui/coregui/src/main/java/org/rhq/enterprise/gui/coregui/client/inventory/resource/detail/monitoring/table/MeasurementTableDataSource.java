package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.ui.MetricDisplayValue;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A simple data source to read in metric data summaries for a resource.
 * This doesn't support paging - everything is returned in one query. Since
 * the number of metrics per resource is relatively small (never more than tens of them),
 * we just load them all in at once.
 * 
 * @author John Mazzitelli
 */
public class MeasurementTableDataSource extends RPCDataSource<MetricDisplaySummary> {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_ALERT_COUNT = "alertCount";
    public static final String FIELD_MIN_VALUE = "min";
    public static final String FIELD_MAX_VALUE = "max";
    public static final String FIELD_AVG_VALUE = "avg";
    public static final String FIELD_LAST_VALUE = "last";

    private int resourceId;

    public MeasurementTableDataSource(int resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField nameField = new ListGridField(FIELD_NAME, MSG.common_title_name());
        nameField.setWidth("20%");
        fields.add(nameField);

        ListGridField alertsField = new ListGridField(FIELD_ALERT_COUNT, MSG.view_resource_monitor_table_alerts());
        alertsField.setWidth("20%");
        fields.add(alertsField);

        ListGridField minField = new ListGridField(FIELD_MIN_VALUE, MSG.view_resource_monitor_table_min());
        minField.setWidth("15%");
        fields.add(minField);

        ListGridField maxField = new ListGridField(FIELD_MAX_VALUE, MSG.view_resource_monitor_table_max());
        maxField.setWidth("15%");
        fields.add(maxField);

        ListGridField avgField = new ListGridField(FIELD_AVG_VALUE, MSG.view_resource_monitor_table_avg());
        avgField.setWidth("15%");
        fields.add(avgField);

        ListGridField lastField = new ListGridField(FIELD_LAST_VALUE, MSG.view_resource_monitor_table_last());
        lastField.setWidth("15%");
        fields.add(lastField);

        return fields;
    }

    @Override
    public MetricDisplaySummary copyValues(Record from) {
        // we should never need this method - we only go in one direction
        // if we ever need this, just have copyValues store an "object" attribute whose value is "from"
        // which this method then just reads out. Since we don't need this now, save memory by not
        // keeping the MetricDisplayValue around
        return null;
    }

    @Override
    public ListGridRecord copyValues(MetricDisplaySummary from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(FIELD_NAME, from.getLabel());
        record.setAttribute(FIELD_ALERT_COUNT, String.valueOf(from.getAlertCount()));
        record.setAttribute(FIELD_MIN_VALUE, getMetricStringValue(from.getMinMetric()));
        record.setAttribute(FIELD_MAX_VALUE, getMetricStringValue(from.getMaxMetric()));
        record.setAttribute(FIELD_AVG_VALUE, getMetricStringValue(from.getAvgMetric()));
        record.setAttribute(FIELD_LAST_VALUE, getMetricStringValue(from.getLastMetric()));

        return record;
    }

    private String getMetricStringValue(MetricDisplayValue value) {
        return (value != null) ? value.toString() : "";
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        // see MetricsTableUIBean for the old JSF class to see where this came from

        MeasurementScheduleCriteria criteria = new MeasurementScheduleCriteria();
        criteria.addFilterResourceId(this.resourceId);
        criteria.setPageControl(PageControl.getUnlimitedInstance());

        GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForResource(resourceId, null,
            new AsyncCallback<ArrayList<MetricDisplaySummary>>() {

                @Override
                public void onSuccess(ArrayList<MetricDisplaySummary> result) {
                    response.setData(buildRecords(result));
                    processResponse(request.getRequestId(), response);
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot load metrics", caught);
                }
            });
    }
}
