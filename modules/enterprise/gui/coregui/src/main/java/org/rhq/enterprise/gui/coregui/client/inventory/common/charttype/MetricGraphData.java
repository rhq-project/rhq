/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.JsonMetricProducer;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;

/**
 * The data portion of the graphs making these methods accessible via JSNI to
 * classes extending this and implementing graphs.
 * Provide i18n labels and format the json data for the graph.
 * @see AbstractGraph
 *
 * @author Mike Thompson
 */
public class MetricGraphData implements JsonMetricProducer {

    private static final Integer DEFAULT_CHART_HEIGHT = 200;
    // i18n
    final protected Messages MSG = CoreGUI.getMessages();
    private final String chartTitleMinLabel = MSG.chart_title_min_label();
    private final String chartTitleAvgLabel = MSG.chart_title_avg_label();
    private final String chartTitlePeakLabel = MSG.chart_title_peak_label();
    private final String chartDateLabel = MSG.chart_date_label();
    private final String chartTimeLabel = MSG.chart_time_label();
    private final String chartDownLabel = MSG.chart_down_label();
    private final String chartUnknownLabel = MSG.chart_unknown_label();
    private final String chartNoDataLabel = MSG.chart_no_data_label();
    private final String chartHoverStartLabel = MSG.chart_hover_start_label();
    private final String chartHoverEndLabel = MSG.chart_hover_end_label();
    private final String chartHoverPeriodLabel = MSG.chart_hover_period_label();
    private final String chartHoverBarLabel = MSG.chart_hover_bar_label();
    private final String chartHoverTimeFormat = MSG.chart_hover_time_format();
    private final String chartHoverDateFormat = MSG.chart_hover_date_format();
    private int entityId = 0;
    private String entityName;
    private int definitionId;
    private MeasurementUnits adjustedMeasurementUnits;
    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> metricData;
    private PageList<MeasurementOOBComposite> measurementOOBCompositeList;
    private MeasurementOOBComposite lastOOB;
    private Integer chartHeight;

    public MetricGraphData() {

    }

    /**
     *
     * @param entityId
     * @param entityName
     * @param def
     * @param metricData
     */
    public MetricGraphData(int entityId, String entityName, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> metricData) {
        this.entityName = entityName;
        setEntityId(entityId);
        setDefinitionId(def.getId());
        this.definition = def;
        this.metricData = metricData;
    }

    public MetricGraphData(int entityId, String entityName, MeasurementDefinition measurementDef,
        List<MeasurementDataNumericHighLowComposite> metrics,  PageList<MeasurementOOBComposite> measurementOOBCompositeList) {
        this.entityName = entityName;
        setEntityId(entityId);
        setDefinitionId(measurementDef.getId());
        this.definition = measurementDef;
        this.metricData = metrics;
        this.measurementOOBCompositeList = measurementOOBCompositeList;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
        //this.definition = null;
    }


    public void setEntityName(String entityName) {
        this.entityName = entityName;
        //this.definition = null;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        //this.definition = null;
    }

    public MeasurementDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(MeasurementDefinition definition) {
        this.definition = definition;
    }

    public String getChartId() {
        return entityId + "-" + definitionId;
    }


    public void setMetricData(List<MeasurementDataNumericHighLowComposite> metricData) {
        this.metricData = metricData;
    }


    public String getChartTitleMinLabel() {
        return chartTitleMinLabel;
    }

    public String getChartTitleAvgLabel() {
        return chartTitleAvgLabel;
    }

    public String getChartTitlePeakLabel() {
        return chartTitlePeakLabel;
    }

    public String getChartDateLabel() {
        return chartDateLabel;
    }

    public String getChartDownLabel() {
        return chartDownLabel;
    }

    public String getChartTimeLabel() {
        return chartTimeLabel;
    }

    public String getChartUnknownLabel() {
        return chartUnknownLabel;
    }

    public String getChartNoDataLabel() {
        return chartNoDataLabel;
    }

    public String getChartHoverStartLabel() {
        return chartHoverStartLabel;
    }

    public String getChartHoverEndLabel() {
        return chartHoverEndLabel;
    }

    public String getChartHoverPeriodLabel() {
        return chartHoverPeriodLabel;
    }

    public String getChartHoverBarLabel() {
        return chartHoverBarLabel;
    }

    public String getChartHoverTimeFormat() {
        return chartHoverTimeFormat;
    }

    public String getChartHoverDateFormat() {
        return chartHoverDateFormat;
    }

    public Integer getChartHeight() {
        if (null != chartHeight) {
            return chartHeight;
        } else {

            return DEFAULT_CHART_HEIGHT;
        }
    }

    public void setChartHeight(Integer chartHeight) {
        this.chartHeight = chartHeight;
    }

    public String getChartTitle() {

        return (entityName == null) ? definition.getDisplayName() : entityName + " - "+definition.getDisplayName();

    }

    /**
     * Returns the y-axis units normalized to highest scale (Bytes -> Gb).
     * NOTE: this requires a dependency such that getJsonMetrics is called
     * before this method as the adjustedMeasurementUnits are calculated in that method.
     * @return yAxisUnits -- normalized to highest UOM.
     */
    public String getYAxisUnits() {
        if (adjustedMeasurementUnits == null) {
            Log.warn("AbstractMetricD3GraphView.adjustedMeasurementUnits is populated by getJsonMetrics. Make sure it is called first.");
            return "";
        } else {
            return adjustedMeasurementUnits.toString();
        }
    }

    public String getXAxisTitle() {
        return MSG.view_charts_time_axis_label();
    }

    @Override
    /**
     * Format the json for the front JSNI(javascript) UI to consume.
     * @todo: future: this should really use GSON or some Json marshaller
     */
    public String getJsonMetrics() {
        StringBuilder sb = new StringBuilder("[");
        boolean gotAdjustedMeasurementUnits = false;
        if (null != metricData) {
            long firstBarTime = metricData.get(0).getTimestamp();
            long secondBarTime = metricData.get(1).getTimestamp();
            long barDuration = secondBarTime - firstBarTime;
            String barDurationString = MeasurementConverterClient.format((double) barDuration,
                MeasurementUnits.MILLISECONDS, true);

            calculateOOB();

            for (MeasurementDataNumericHighLowComposite measurement : metricData) {
                sb.append("{ \"x\":" + measurement.getTimestamp() + ",");

                if (null != lastOOB) {
                    sb.append(" \"baselineMin\":" + lastOOB.getBlMin() + ", ");
                    sb.append(" \"baselineMax\":" + lastOOB.getBlMax() + ", ");
                }

                    if (!Double.isNaN(measurement.getValue())) {

                        MeasurementNumericValueAndUnits newHigh = normalizeUnitsAndValues(measurement.getHighValue(),
                            definition.getUnits());
                        MeasurementNumericValueAndUnits newLow = normalizeUnitsAndValues(measurement.getLowValue(),
                            definition.getUnits());
                        MeasurementNumericValueAndUnits newValue = normalizeUnitsAndValues(measurement.getValue(),
                            definition.getUnits());
                        if (!gotAdjustedMeasurementUnits) {
                            adjustedMeasurementUnits = newValue.getUnits();
                            gotAdjustedMeasurementUnits = true;
                        }
                        sb.append(" \"barDuration\": \"" + barDurationString + "\", ");
                        sb.append(" \"high\":" + newHigh.getValue() + ",");
                        sb.append(" \"low\":" + newLow.getValue() + ",");
                        sb.append(" \"y\":" + newValue.getValue() + "},");
                    } else {
                        // give it some values so that we dont have NaN
                        sb.append(" \"high\":0,");
                        sb.append(" \"low\":0,");
                        sb.append(" \"y\":0,");
                        sb.append(" \"nodata\":true },");
                }
                if (!sb.toString().endsWith("},")) {
                    sb.append(" },");
                }
            }
            sb.setLength(sb.length() - 1); // delete the last ','
        }
        sb.append("]");
        Log.debug("Json data for: "+getChartTitle());
        Log.debug(sb.toString());
        return sb.toString();
    }

    /**
     * When the scale is adjusted down to a level where bars no longer have
     * highs and lows different from the avg value then have probably hit a
     * scale where aggregates no longer exist and we are looking the individual
     * values. At the very least the data is less interesting and a trendline
     * connecting the points has less meaning because we are essentially a scatterplot.
     * A different algorithm could be used here but this will suffice.
     * @return true if the graphs should show the bar avg line meaning there is aggregates in the data
     * @see MetricStackedBarGraph
     */
    public boolean showBarAvgTrendLine() {
        for (MeasurementDataNumericHighLowComposite measurement : metricData) {
            boolean noValuesInCurrentBarUndefined = (!Double.isNaN(measurement.getValue()) && !Double.isNaN(measurement.getHighValue())  && !Double.isNaN(measurement.getLowValue()));
            boolean foundAggregateBar = (measurement.getValue() != measurement.getHighValue() || measurement.getHighValue() != measurement.getLowValue());
            // if there exists a even one aggregate bar then I can short circuit this and exit
            if (noValuesInCurrentBarUndefined && foundAggregateBar){
                return true;
            }
        }
        return false;

    }

    private void calculateOOB() {
        if (measurementOOBCompositeList != null && !measurementOOBCompositeList.isEmpty()) {
            Log.debug("OOB List size: " + measurementOOBCompositeList.size());
            List<MeasurementOOBComposite> selectedOOBs = new ArrayList<MeasurementOOBComposite>();
            for (MeasurementOOBComposite measurementOOBComposite : measurementOOBCompositeList) {
                Log.debug("measurementOOBComposite = " + measurementOOBComposite);
                if (measurementOOBComposite.getDefinitionId() == definitionId) {
                    selectedOOBs.add(measurementOOBComposite);
                }
            }
            // take the last one (most current) matching the defId
            lastOOB = selectedOOBs.isEmpty() ? null : selectedOOBs.get(selectedOOBs.size() - 1);
        } else {
            lastOOB = null;
        }
    }


    private MeasurementNumericValueAndUnits normalizeUnitsAndValues(double value, MeasurementUnits measurementUnits) {
        MeasurementNumericValueAndUnits newValue = MeasurementConverterClient.fit(value, measurementUnits);
        MeasurementNumericValueAndUnits returnValue;

        // adjust for percentage numbers
        if (measurementUnits.equals(MeasurementUnits.PERCENTAGE)) {
            returnValue = new MeasurementNumericValueAndUnits(newValue.getValue() * 100, newValue.getUnits());
        } else {
            returnValue = new MeasurementNumericValueAndUnits(newValue.getValue(), newValue.getUnits());
        }

        return returnValue;
    }

    /**
     * If there is more than 2 days time window then return true so we can show day of week
     * in axis labels. Function to switch the timescale to whichever is more appropriate hours
     * or hours with days of week.
     * @return true if difference between startTime and endTime is >= x days
     */
    public boolean shouldDisplayDayOfWeekInXAxisLabel() {
        Long startTime = metricData.get(0).getTimestamp();
        Long endTime = metricData.get(metricData.size() - 1).getTimestamp();
        long timeThreshold = 24 * 60 * 60 * 1000; // 1 days
        return startTime + timeThreshold < endTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MetricGraphData");
        sb.append("{chartTitleMinLabel='").append(chartTitleMinLabel).append('\'');
        sb.append(", chartTitleAvgLabel='").append(chartTitleAvgLabel).append('\'');
        sb.append(", chartTitlePeakLabel='").append(chartTitlePeakLabel).append('\'');
        sb.append(", chartDateLabel='").append(chartDateLabel).append('\'');
        sb.append(", chartTimeLabel='").append(chartTimeLabel).append('\'');
        sb.append(", chartDownLabel='").append(chartDownLabel).append('\'');
        sb.append(", chartUnknownLabel='").append(chartUnknownLabel).append('\'');
        sb.append(", chartNoDataLabel='").append(chartNoDataLabel).append('\'');
        sb.append(", chartHoverStartLabel='").append(chartHoverStartLabel).append('\'');
        sb.append(", chartHoverEndLabel='").append(chartHoverEndLabel).append('\'');
        sb.append(", chartHoverPeriodLabel='").append(chartHoverPeriodLabel).append('\'');
        sb.append(", chartHoverBarLabel='").append(chartHoverBarLabel).append('\'');
        sb.append(", entityId=").append(entityId);
        sb.append(", entityName='").append(entityName).append('\'');
        sb.append(", definitionId=").append(definitionId);
        sb.append(", adjustedMeasurementUnits=").append(adjustedMeasurementUnits);
        sb.append(", definition=").append(definition);
        sb.append(", chartHeight=").append(chartHeight);
        sb.append('}');
        return sb.toString();
    }
}
