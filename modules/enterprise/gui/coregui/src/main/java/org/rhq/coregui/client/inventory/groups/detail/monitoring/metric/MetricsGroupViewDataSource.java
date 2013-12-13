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

package org.rhq.coregui.client.inventory.groups.detail.monitoring.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.ui.MetricDisplayValue;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;

import static org.rhq.core.domain.measurement.DataType.COMPLEX;
import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricsGridFieldName.*;

/**
 * A simple data source to read in metric data summaries for a resource.
 * This doesn't support paging - everything is returned in one query. Since
 * the number of metrics per resource is relatively small (never more than tens of them),
 * we just load them all in at once.
 *
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class MetricsGroupViewDataSource extends RPCDataSource<MetricDisplaySummary, Criteria> {

    private static final int NUMBER_OF_METRIC_POINTS = 60;

    private final ResourceGroup resourceGroup;
    private List<MetricDisplaySummary> metricDisplaySummaries;
    private List<List<MeasurementDataNumericHighLowComposite>> metricsDataList;
    private int[] definitionArrayIds;

    public MetricsGroupViewDataSource(ResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
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
                String contents = "<span id='sparkline_" + resourceGroup.getId() + "-"
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
        record.setAttribute(METRIC_DEF_ID.getValue(), from.getDefinitionId());
        record.setAttribute(METRIC_SCHEDULE_ID.getValue(), from.getScheduleId());
        record.setAttribute(METRIC_UNITS.getValue(), from.getUnits());
        record.setAttribute(METRIC_NAME.getValue(), from.getMetricName());
        record.setAttribute(RESOURCE_GROUP_ID.getValue(), resourceGroup.getId());
        return record;
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

        // This latch is the last thing that gets executed after we have executed the
        // 1 query
        final CountDownLatch countDownLatch = CountDownLatch.create(1, new Command() {

            @Override
            public void execute() {

                // NOTE: this runs after the queryMetricDisplaySummaries is complete
                queryGroupMetrics(resourceGroup, request, response);
            }
        });

        organizeMeasurementDefinitionOrder(resourceGroup);
        queryMetricDisplaySummaries(definitionArrayIds, CustomDateRangeState.getInstance().getStartTime(),
            CustomDateRangeState.getInstance().getEndTime(), countDownLatch);

    }

    private void queryGroupMetrics(final ResourceGroup resourceGroup, final DSRequest request, final DSResponse response) {

        GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(resourceGroup.getId(),
            definitionArrayIds, CustomDateRangeState.getInstance().getStartTime(),
            CustomDateRangeState.getInstance().getEndTime(), NUMBER_OF_METRIC_POINTS,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.warn("Error retrieving recent metrics charting data for resource [" + resourceGroup.getId()
                        + "]:" + caught.getMessage());
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> measurementDataList) {
                    if (null != measurementDataList && !measurementDataList.isEmpty()) {
                        metricsDataList = measurementDataList;
                        response.setData(buildRecords(metricDisplaySummaries));
                        processResponse(request.getRequestId(), response);
                        new Timer() {
                            @Override
                            public void run() {
                                BrowserUtility.graphSparkLines();
                            }
                        }.schedule(150);
                    }
                }
            });

    }

    private void organizeMeasurementDefinitionOrder(ResourceGroup resourceGroup) {
        Set<MeasurementDefinition> definitions = getMetricDefinitions(resourceGroup);

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
    }

    private void queryMetricDisplaySummaries(int[] measurementDefIds, Long startTime, Long endTime,
        final CountDownLatch countDownLatch) {
        GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForCompatibleGroup(
            resourceGroup.getId(), measurementDefIds, startTime, endTime, false,
            new AsyncCallback<ArrayList<MetricDisplaySummary>>() {
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

    private Set<MeasurementDefinition> getMetricDefinitions(ResourceGroup resourceGroup) {
        Set<MeasurementDefinition> definitions = new HashSet<MeasurementDefinition>();
        for (MeasurementDefinition measurementDefinition : resourceGroup.getResourceType().getMetricDefinitions()) {
            if (measurementDefinition.getDataType() == MEASUREMENT || measurementDefinition.getDataType() == COMPLEX) {
                definitions.add(measurementDefinition);
            }
        }
        return definitions;
    }
}
