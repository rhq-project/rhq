package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.CallTimeDataCriteria;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferences;

/**
 * A data source to read in calltime (aka response time) metric data.
 * 
 * @author John Mazzitelli
 */
public class CalltimeDataSource extends RPCDataSource<CallTimeDataComposite, CallTimeDataCriteria> {

    // dest, count, min, max, avg, total string values here must match the sort names in CallTimeDataCriteria
    public static final String FIELD_DESTINATION = "calltimedatavalue.key.callDestination";
    public static final String FIELD_REQUESTCOUNT = "count";
    public static final String FIELD_MIN = "minimum";
    public static final String FIELD_MAX = "maximum";
    public static final String FIELD_AVG = "average";
    public static final String FIELD_TOTAL = "total";
    public static final String FILTER_DESTINATION = "destinationFilter";
    // these string fields are attributes we store on the record that contain the value with a units label
    public static final String FIELD_MIN_STRING = "minimumString";
    public static final String FIELD_MAX_STRING = "maximumString";
    public static final String FIELD_AVG_STRING = "averageString";
    public static final String FIELD_TOTAL_STRING = "totalString";

    private final EntityContext entityContext;

    public CalltimeDataSource(EntityContext ec) {
        this.entityContext = ec;
    }

    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField nameField = new ListGridField(FIELD_DESTINATION, MSG.view_resource_monitor_calltime_destination());
        nameField.setWidth("50%");
        fields.add(nameField);

        ListGridField alertsField = new ListGridField(FIELD_REQUESTCOUNT, MSG.view_resource_monitor_calltime_count());
        alertsField.setWidth("10%");
        fields.add(alertsField);

        ListGridField minField = new ListGridField(FIELD_MIN, MSG.view_resource_monitor_calltime_minimum());
        minField.setWidth("10%");
        minField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return record.getAttribute(FIELD_MIN_STRING);
            }
        });
        fields.add(minField);

        ListGridField maxField = new ListGridField(FIELD_MAX, MSG.view_resource_monitor_calltime_maximum());
        maxField.setWidth("10%");
        maxField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return record.getAttribute(FIELD_MAX_STRING);
            }
        });
        fields.add(maxField);

        ListGridField avgField = new ListGridField(FIELD_AVG, MSG.view_resource_monitor_calltime_average());
        avgField.setWidth("10%");
        avgField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return record.getAttribute(FIELD_AVG_STRING);
            }
        });
        fields.add(avgField);

        ListGridField totalField = new ListGridField(FIELD_TOTAL, MSG.view_resource_monitor_calltime_total());
        totalField.setWidth("10%");
        totalField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return record.getAttribute(FIELD_TOTAL_STRING);
            }
        });
        fields.add(totalField);

        return fields;
    }

    @Override
    public CallTimeDataComposite copyValues(Record from) {
        // we should never need this method - we only go in one direction
        // if we ever need this, just have copyValues store an "object" attribute whose value is "from"
        // which this method then just reads out. Since we don't need this now, save memory by not
        // keeping the CallTimeDataComposite around
        return null;
    }

    @Override
    public ListGridRecord copyValues(CallTimeDataComposite from) {
        double[] durations = new double[] { from.getMinimum(), from.getMaximum(), from.getAverage(), from.getTotal() };
        String[] durationStrings = MeasurementConverterClient.formatToSignificantPrecision(durations,
            MeasurementUnits.MILLISECONDS, true);

        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_DESTINATION, from.getCallDestination());
        record.setAttribute(FIELD_REQUESTCOUNT, from.getCount());
        record.setAttribute(FIELD_MIN, durations[0]);
        record.setAttribute(FIELD_MAX, durations[1]);
        record.setAttribute(FIELD_AVG, durations[2]);
        record.setAttribute(FIELD_TOTAL, durations[3]);
        record.setAttribute(FIELD_MIN_STRING, durationStrings[0]);
        record.setAttribute(FIELD_MAX_STRING, durationStrings[1]);
        record.setAttribute(FIELD_AVG_STRING, durationStrings[2]);
        record.setAttribute(FIELD_TOTAL_STRING, durationStrings[3]);
        return record;
    }

    @Override
    protected CallTimeDataCriteria getFetchCriteria(DSRequest request) {
        UserPreferences prefs = UserSessionManager.getUserPreferences();
        MeasurementUserPreferences mprefs = new MeasurementUserPreferences(prefs);
        ArrayList<Long> range = mprefs.getMetricRangePreferences().getBeginEndTimes();
        String destinationFilter = getFilter(request, FILTER_DESTINATION, String.class);

        CallTimeDataCriteria criteria = new CallTimeDataCriteria();
        criteria.addFilterBeginTime(range.get(0));
        criteria.addFilterEndTime(range.get(1));
        if (destinationFilter != null && destinationFilter.length() > 0) {
            criteria.addFilterDestination(destinationFilter);
        }

        return criteria;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final CallTimeDataCriteria criteria) {
        GWTServiceLookup.getMeasurementDataService().findCallTimeDataForContext(this.entityContext, criteria,
            new AsyncCallback<PageList<CallTimeDataComposite>>() {

                public void onSuccess(PageList<CallTimeDataComposite> result) {
                    sendSuccessResponse(request, response, result);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_calltime_loadFailed(), caught);
                }
            });
    }
}
