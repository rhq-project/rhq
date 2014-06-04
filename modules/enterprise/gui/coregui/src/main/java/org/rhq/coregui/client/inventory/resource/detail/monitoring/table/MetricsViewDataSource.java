/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.inventory.resource.detail.monitoring.table;

import static org.rhq.core.domain.measurement.DataType.COMPLEX;
import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.ALERT_COUNT;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.AVG_VALUE;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.LIVE_VALUE;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.MAX_VALUE;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.METRIC_DEF_ID;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.METRIC_LABEL;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.METRIC_NAME;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.METRIC_SCHEDULE_ID;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.METRIC_UNITS;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.MIN_VALUE;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.RESOURCE_ID;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.SPARKLINE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.InvocationException;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.ui.MetricDisplayValue;
import org.rhq.core.domain.resource.Resource;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;
import org.rhq.coregui.client.util.preferences.MeasurementUserPreferences;

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

    private static final int NUMBER_OF_METRIC_POINTS = 60;

    private final Resource resource;
    private List<MetricDisplaySummary> metricDisplaySummaries;
    private List<List<MeasurementDataNumericHighLowComposite>> metricsDataList;
    private Set<MeasurementData> liveMeasurementDataSet;
    private int[] definitionArrayIds;
    private int[] enabledScheduleIds;
    private int[] enabledScheduleDefinitionIds;
    private HashMap<Integer, MeasurementUnits> scheduleToMeasurementUnitMap = new HashMap<Integer, MeasurementUnits>();
    private final MeasurementUserPreferences measurementUserPrefs;

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

        ListGridField sparklineField = new ListGridField(SPARKLINE.getValue(), MSG.chart_metrics_sparkline_header());
        sparklineField.setCellFormatter(new CellFormatter() {
            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return "";
                }
                String contents = "<span id='sparkline_" + resource.getId() + "-"
                    + record.getAttributeAsInt(METRIC_DEF_ID.getValue()) + "' class='dynamicsparkline' width='70' "
                    + "values='" + record.getAttribute(SPARKLINE.getValue()) + "'></span>";
                return contents;

            }
        });

        sparklineField.setWidth(80);
        fields.add(sparklineField);

        ListGridField nameField = new ListGridField(METRIC_LABEL.getValue(), METRIC_LABEL.getLabel());
        nameField.setWidth("30%");
        fields.add(nameField);

        ListGridField minField = new ListGridField(MIN_VALUE.getValue(), MIN_VALUE.getLabel());
        minField.setWidth("15%");
        fields.add(minField);

        ListGridField maxField = new ListGridField(MAX_VALUE.getValue(), MAX_VALUE.getLabel());
        maxField.setWidth("15%");
        fields.add(maxField);

        ListGridField avgField = new ListGridField(AVG_VALUE.getValue(), AVG_VALUE.getLabel());
        avgField.setWidth("15%");
        fields.add(avgField);

        ListGridField liveField = new ListGridField(LIVE_VALUE.getValue(), LIVE_VALUE.getLabel());
        liveField.setWidth("15%");
        fields.add(liveField);

        ListGridField alertsField = new ListGridField(ALERT_COUNT.getValue(), ALERT_COUNT.getLabel());
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
        record.setAttribute(SPARKLINE.getValue(), getCsvMetricsForSparkline(from.getDefinitionId()));
        record.setAttribute(METRIC_LABEL.getValue(), from.getLabel());
        record.setAttribute(ALERT_COUNT.getValue(), String.valueOf(from.getAlertCount()));
        record.setAttribute(MIN_VALUE.getValue(), getMetricStringValue(from.getMinMetric()));
        record.setAttribute(MAX_VALUE.getValue(), getMetricStringValue(from.getMaxMetric()));
        record.setAttribute(AVG_VALUE.getValue(), getMetricStringValue(from.getAvgMetric()));
        record.setAttribute(LIVE_VALUE.getValue(), buildLiveValue(from));
        record.setAttribute(METRIC_DEF_ID.getValue(), from.getDefinitionId());
        record.setAttribute(METRIC_SCHEDULE_ID.getValue(), from.getScheduleId());
        record.setAttribute(METRIC_UNITS.getValue(), from.getUnits());
        record.setAttribute(METRIC_NAME.getValue(), from.getMetricName());
        record.setAttribute(RESOURCE_ID.getValue(), resource.getId());
        return record;
    }

    private String buildLiveValue(MetricDisplaySummary from) {
        StringBuilder sb = new StringBuilder();
        for (MeasurementData measurementData : liveMeasurementDataSet) {
            if (from.getScheduleId() == measurementData.getScheduleId()) {
                double doubleValue;
                if (measurementData.getValue() instanceof Number) {
                    doubleValue = ((Number) measurementData.getValue()).doubleValue();
                } else {
                    doubleValue = Double.parseDouble(measurementData.getValue().toString());
                }

                String liveValue = MeasurementConverterClient.format(doubleValue,
                    MeasurementUnits.valueOf(from.getUnits()), true, 0, 1);

                sb.append(liveValue);

                break;
            }
        }

        return sb.toString();
    }

    private String getCsvMetricsForSparkline(int definitionId) {
        StringBuilder sb = new StringBuilder();
        List<MeasurementDataNumericHighLowComposite> selectedMetricsList = getMeasurementsForMeasurementDefId(definitionId);

        for (MeasurementDataNumericHighLowComposite measurementData : selectedMetricsList) {
            if (!Double.isNaN(measurementData.getValue())) {
                sb.append((int) measurementData.getValue());
                sb.append(",");
            }
        }

        if (sb.toString().endsWith(",")) {
            sb.setLength(sb.length() - 1);
        }
        // handle the case where we have just installed the server so not much history
        // and our date range is set such that only one value returns which the
        // sparkline graph will not plot anything, so we need at least 2 values
        if (!sb.toString().contains(",")) {
            // append another value just so we have 2 values and it will graph
            return "0," + sb.toString();
        }

        return sb.toString();
    }

    private List<MeasurementDataNumericHighLowComposite> getMeasurementsForMeasurementDefId(int definitionId) {
        int selectedIndex = 0;

        // find the ordinal position as specified when querying the metrics
        for (int i = 0; i < definitionArrayIds.length; i++) {
            if (definitionArrayIds[i] == definitionId) {
                selectedIndex = i;
                break;
            }
        }

        return metricsDataList.get(selectedIndex);
    }

    protected String getMetricStringValue(MetricDisplayValue value) {
        return (value != null) ? value.toString() : "";
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // NOTE: we don't use criterias for this datasource, just return null
        return null;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {

        GWTServiceLookup.getMeasurementScheduleService().findSchedulesForResourceAndType(resource.getId(),
            DataType.MEASUREMENT, null, true, new AsyncCallback<ArrayList<MeasurementSchedule>>() {
                @Override
                public void onSuccess(ArrayList<MeasurementSchedule> measurementSchedules) {
                    enabledScheduleIds = new int[measurementSchedules.size()];
                    enabledScheduleDefinitionIds = new int[measurementSchedules.size()];
                    int i = 0;
                    for (MeasurementSchedule measurementSchedule : measurementSchedules) {
                        enabledScheduleIds[i] = measurementSchedule.getId();
                        enabledScheduleDefinitionIds[i++] = measurementSchedule.getDefinition().getId();
                    }

                    // This latch is the last thing that gets executed after we have executed the
                    // 2 queries in Parallel
                    final CountDownLatch countDownLatch = CountDownLatch.create(2, new Command() {

                        @Override
                        public void execute() {
                            // we needed the ResourceMetrics query and Metric Display Summary
                            // to finish before we can query the live metrics and populate the
                            // result response
                            queryLiveMetrics(request, response);
                        }
                    });

                    queryResourceMetrics(resource, measurementUserPrefs.getMetricRangePreferences().begin,
                        measurementUserPrefs.getMetricRangePreferences().end, countDownLatch);
                    queryMetricDisplaySummaries(enabledScheduleIds,
                        measurementUserPrefs.getMetricRangePreferences().begin,
                        measurementUserPrefs.getMetricRangePreferences().end, countDownLatch);
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot load schedules", caught);
                }
            });
    }

    private void queryLiveMetrics(final DSRequest request, final DSResponse response) {
        if (enabledScheduleDefinitionIds.length == 0) {
            prepareUI(null, request, response);
            return; // no enabled metrics, let's save 1 round trip
        }

        // actually go out and ask the agents for the data
        GWTServiceLookup.getMeasurementDataService(60000).findLiveData(resource.getId(), enabledScheduleDefinitionIds,
            new AsyncCallback<Set<MeasurementData>>() {
                @Override
                public void onSuccess(Set<MeasurementData> result) {
                    prepareUI(result, request, response);
                }

                /**
                 * Called when an asynchronous call fails to complete normally. {@link IncompatibleRemoteServiceException}s, {@link
                 * InvocationException}s, or checked exceptions thrown by the service method are examples of the type of failures that
                 * can be passed to this method.
                 * <p/>
                 * <p> If <code>caught</code> is an instance of an {@link IncompatibleRemoteServiceException} the application should
                 * try to get into a state where a browser refresh can be safely done. </p>
                 *
                 * @param caught failure encountered while executing a remote procedure call
                 */
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot load metrics", caught);
                }
            });
    }
    
    /**
     * Just a helper method to keep code DRY
     */
    private void prepareUI(Set<MeasurementData> result, DSRequest request, DSResponse response) {
        liveMeasurementDataSet = null == result ? Collections.<MeasurementData>emptySet() : result;
        response.setData(buildRecords(metricDisplaySummaries));
        processResponse(request.getRequestId(), response);
        new Timer() {
            @Override
            public void run() {
                BrowserUtility.graphSparkLines();
            }
        }.schedule(150);
    }

    private void queryMetricDisplaySummaries(int[] scheduleIds, Long startTime, Long endTime,
        final CountDownLatch countDownLatch) {
        if (enabledScheduleIds.length == 0) {
            setMetricDisplaySummaries(Collections.<MetricDisplaySummary>emptyList());
            countDownLatch.countDown();
            return; // no enabled metrics, let's save 1 round trip
        }
        
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

    private void setMetricDisplaySummaries(List<MetricDisplaySummary> metricDisplaySummaries) {
        this.metricDisplaySummaries = metricDisplaySummaries;
    }

    private void queryResourceMetrics(final Resource resource, final Long startTime, final Long endTime,
        final CountDownLatch countDownLatch) {
        List<MeasurementDefinition> definitions = getMetricDefinitions(resource);
        if (definitions.size() == 0) {
            countDownLatch.countDown();
            return;
        }

        // create a mapping of schedules ids to MeasurementUnits
        for (MeasurementDefinition definition : definitions) {
            if (null != definition.getSchedules()) {
                for (MeasurementSchedule schedule : definition.getSchedules()) {
                    scheduleToMeasurementUnitMap.put(schedule.getId(), definition.getUnits());
                }
            }
        }

        //bundle definition ids for asynch call.
        definitionArrayIds = new int[definitions.size()];

        //sort the charting data ex. Free Memory, Free Swap Space,..System Load
        Collections.sort(definitions, new Comparator<MeasurementDefinition>() {
            @Override
            public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });

        //organize definitionArrayIds for ordered request on server.
        int index = 0;
        for (MeasurementDefinition definitionToDisplay : definitions) {
            definitionArrayIds[index++] = definitionToDisplay.getId();
        }

        GWTServiceLookup.getMeasurementDataService().findDataForResource(resource.getId(), definitionArrayIds,
            startTime, endTime, NUMBER_OF_METRIC_POINTS,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.warn("Error retrieving recent metrics charting data for resource [" + resource.getId() + "]:"
                        + caught.getMessage());
                    countDownLatch.countDown();
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> measurementDataList) {
                    if (null != measurementDataList && !measurementDataList.isEmpty()) {
                        metricsDataList = measurementDataList;
                    }
                    countDownLatch.countDown();
                }
            });

    }

    private List<MeasurementDefinition> getMetricDefinitions(Resource resource) {
        List<MeasurementDefinition> definitions = new ArrayList<MeasurementDefinition>();
        for (MeasurementDefinition measurementDefinition : resource.getResourceType().getMetricDefinitions()) {
            if (measurementDefinition.getDataType() == MEASUREMENT || measurementDefinition.getDataType() == COMPLEX) {
                definitions.add(measurementDefinition);
            }
        }
        return definitions;
    }
}
