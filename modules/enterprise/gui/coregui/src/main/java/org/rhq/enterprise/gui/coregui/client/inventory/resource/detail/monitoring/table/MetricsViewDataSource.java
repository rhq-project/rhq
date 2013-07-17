package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.ui.MetricDisplayValue;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * A simple data source to read in metric data summaries for a resource.
 * This doesn't support paging - everything is returned in one query. Since
 * the number of metrics per resource is relatively small (never more than tens of them),
 * we just load them all in at once.
 * 
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class MetricsViewDataSource extends RPCDataSource<MetricDisplaySummary, Criteria> {

    public static final int NUMBER_OF_METRIC_POINTS = 60;
    public static final String FIELD_SPARKLINE = "sparkline";
    public static final String FIELD_METRIC_LABEL = "label";
    public static final String FIELD_ALERT_COUNT = "alertCount";
    public static final String FIELD_MIN_VALUE = "min";
    public static final String FIELD_MAX_VALUE = "max";
    public static final String FIELD_AVG_VALUE = "avg";
    public static final String FIELD_LAST_VALUE = "last";
    public static final String FIELD_METRIC_DEF_ID = "defId";
    public static final String FIELD_METRIC_SCHED_ID = "schedId";
    public static final String FIELD_METRIC_UNITS = "units";
    public static final String FIELD_METRIC_NAME = "name";
    public static final String FIELD_RESOURCE_ID = "resourceId";
    final private Resource resource;
    private List<MetricDisplaySummary> metricDisplaySummaries;
    private List<List<MeasurementDataNumericHighLowComposite>> metricsDataList;
    private int[] definitionArrayIds;
    private MeasurementUserPreferences measurementUserPrefs;

    public MetricsViewDataSource(Resource resource) {
        this.resource = resource;
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

        ListGridField sparklineField = new ListGridField(FIELD_SPARKLINE, "chart");
        sparklineField.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return "";
                }
                String contents = "<span id='sparkline_" + resource.getId() + "-"
                    + record.getAttributeAsInt(FIELD_METRIC_DEF_ID) + "' class='dynamicsparkline' width='70' "
                    + "values='" + record.getAttribute(FIELD_SPARKLINE) + "'></span>";
                return contents;

            }
        });

        sparklineField.setWidth(80);
        fields.add(sparklineField);

        ListGridField nameField = new ListGridField(FIELD_METRIC_LABEL, MSG.common_title_name());
        nameField.setWidth("30%");
        fields.add(nameField);

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

        ListGridField alertsField = new ListGridField(FIELD_ALERT_COUNT, MSG.common_title_alerts());
        alertsField.setWidth("10%");
        fields.add(alertsField);

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
        MeasurementUtility.formatSimpleMetrics(from);

        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_SPARKLINE, getCsvMetricsForSparkline(from.getDefinitionId()));
        record.setAttribute(FIELD_METRIC_LABEL, from.getLabel());
        record.setAttribute(FIELD_ALERT_COUNT, String.valueOf(from.getAlertCount()));
        record.setAttribute(FIELD_MIN_VALUE, getMetricStringValue(from.getMinMetric()));
        record.setAttribute(FIELD_MAX_VALUE, getMetricStringValue(from.getMaxMetric()));
        record.setAttribute(FIELD_AVG_VALUE, getMetricStringValue(from.getAvgMetric()));
        record.setAttribute(FIELD_LAST_VALUE, getMetricStringValue(from.getLastMetric()));
        record.setAttribute(FIELD_METRIC_DEF_ID, from.getDefinitionId());
        record.setAttribute(FIELD_METRIC_SCHED_ID, from.getScheduleId());
        record.setAttribute(FIELD_METRIC_UNITS, from.getUnits());
        record.setAttribute(FIELD_METRIC_NAME, from.getMetricName());
        record.setAttribute(FIELD_RESOURCE_ID, resource.getId());
        return record;
    }

    private String getCsvMetricsForSparkline(int definitionId) {
        StringBuilder sb = new StringBuilder();
        Log.debug("getCsvMetricsForSparkline.metricsDataList("+definitionId+"): " + metricsDataList.size());
        List<MeasurementDataNumericHighLowComposite> selectedMetricsList = getMeasurementsForMeasurementDefId(definitionId);

        for (int i = 0; i < selectedMetricsList.size(); i++) {
                // take the last 20 values
                //if (i >= selectedMetricsList.size() - 20) {
                    MeasurementDataNumericHighLowComposite measurementData = selectedMetricsList.get(i);
                    if (!Double.isNaN(measurementData.getValue())) {
                        sb.append((int) measurementData.getValue());
                        sb.append(",");
                    }
                //}
            }

        if (sb.toString().endsWith(",")) {
            sb.setLength(sb.length() - 1);
        }
        Log.debug("getCsvMetricsForSparkline: " + sb.toString());

        return sb.toString();
    }

    List<MeasurementDataNumericHighLowComposite> getMeasurementsForMeasurementDefId(int definitionId){
        List<MeasurementDataNumericHighLowComposite> selectedMetricsDataList;
        int selectedIndex = 0;

        // find the ordinal position as specified when querying the metrics
        for (int i = 0; i < definitionArrayIds.length; i++) {
            if(definitionArrayIds[i] == definitionId){
                selectedIndex = i;
                break;
            }
        }

        return  metricsDataList.get(selectedIndex);
    }

    protected String getMetricStringValue(MetricDisplayValue value) {
        return (value != null) ? value.toString() : "";
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criterias for this datasource, just return null
        return null;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {

        GWTServiceLookup.getMeasurementScheduleService().findSchedulesForResourceAndType(resource.getId(),
            DataType.MEASUREMENT, null, true, new AsyncCallback<ArrayList<MeasurementSchedule>>() {
                @Override
                public void onSuccess(ArrayList<MeasurementSchedule> measurementSchedules) {
                    int[] scheduleIds = new int[measurementSchedules.size()];
                    int i = 0;
                    for (MeasurementSchedule measurementSchedule : measurementSchedules) {
                        scheduleIds[i++] = measurementSchedule.getId();
                    }

                    final CountDownLatch countDownLatch = CountDownLatch.create(2, new Command() {

                        @Override
                        public void execute() {
                            response.setData(buildRecords(metricDisplaySummaries));
                            processResponse(request.getRequestId(), response);

                            new Timer() {

                                @Override
                                public void run() {
                                    BrowserUtility.graphSparkLines();
                                }
                            }.schedule(150);
                            Log.debug("Finished CountdownLatch for metrics loaded: " + metricsDataList.size());
                        }
                    });

                    queryResourceMetrics(resource, measurementUserPrefs.getMetricRangePreferences().begin,
                        measurementUserPrefs.getMetricRangePreferences().end, countDownLatch);
                    queryMetricDisplaySummaries(scheduleIds, measurementUserPrefs.getMetricRangePreferences().begin,
                        measurementUserPrefs.getMetricRangePreferences().end, countDownLatch);
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot load schedules", caught);
                }
            });
    }

    private void queryMetricDisplaySummaries(int[] scheduleIds, Long startTime, Long endTime,
        final CountDownLatch countDownLatch) {
        GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForResource(resource.getId(),
            scheduleIds, startTime, endTime, new AsyncCallback<ArrayList<MetricDisplaySummary>>() {
                @Override
                public void onSuccess(ArrayList<MetricDisplaySummary> metricDisplaySummaries) {
                    setMetricDisplaySummaries(metricDisplaySummaries);
                    countDownLatch.countDown();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot load metrics", caught);
                    countDownLatch.countDown();
                }
            }

        );
    }

    void setMetricDisplaySummaries(List<MetricDisplaySummary> metricDisplaySummaries) {
        this.metricDisplaySummaries = metricDisplaySummaries;
    }

    public void queryResourceMetrics(final Resource resource, Long startTime, Long endTime,
        final CountDownLatch countDownLatch) {
        Set<MeasurementDefinition> definitions = resource.getResourceType().getMetricDefinitions();

        //build id mapping for measurementDefinition instances Ex. Free Memory -> MeasurementDefinition[100071]
        final HashMap<String, MeasurementDefinition> measurementDefMap = new HashMap<String, MeasurementDefinition>();
        for (MeasurementDefinition definition : definitions) {
            measurementDefMap.put(definition.getDisplayName(), definition);
        }
        //bundle definition ids for asynch call.
        definitionArrayIds = new int[definitions.size()];
        final String[] displayOrder = new String[definitions.size()];
        measurementDefMap.keySet().toArray(displayOrder);
        //sort the charting data ex. Free Memory, Free Swap Space,..System Load
        Arrays.sort(displayOrder);

        //organize definitionArrayIds for ordered request on server.
        int index = 0;
        for (String definitionToDisplay : displayOrder) {
            definitionArrayIds[index++] = measurementDefMap.get(definitionToDisplay).getId();
        }

        GWTServiceLookup.getMeasurementDataService().findDataForResource(resource.getId(), definitionArrayIds,
            startTime, endTime, NUMBER_OF_METRIC_POINTS,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.warn("Error retrieving recent metrics charting data for resource [" + resource.getId() + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> measurementDataList) {

                    if (!measurementDataList.isEmpty()) {
                        metricsDataList = measurementDataList;
                        Log.debug("Retrieved metrics metricsDataList.size: " + metricsDataList.size());
                        countDownLatch.countDown();
                    }
                }
            });

    }
}
