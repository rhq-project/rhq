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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.rhq.core.domain.measurement.Availability;
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
 *
 * @author Mike Thompson
 */
public class MetricGraphData implements JsonMetricProducer {

    // i18n
    final protected Messages MSG = CoreGUI.getMessages();
    private static final Integer DEFAULT_CHART_HEIGHT = 250;
    private final String chartTitleMinLabel = MSG.chart_title_min_label();
    private final String chartTitleAvgLabel = MSG.chart_title_avg_label();
    private final String chartTitlePeakLabel = MSG.chart_title_peak_label();
    private final String chartDateLabel = MSG.chart_date_label();
    private final String chartTimeLabel = MSG.chart_time_label();
    private final String chartDownLabel = MSG.chart_down_label();
    private final String chartUnknownLabel = MSG.chart_unknown_label();
    private final String chartHoverStartLabel = MSG.chart_hover_start_label();
    private final String chartHoverEndLabel = MSG.chart_hover_end_label();
    private final String chartHoverPeriodLabel = MSG.chart_hover_period_label();
    private final String chartHoverBarLabel = MSG.chart_hover_bar_label();

    private int entityId;
    private String entityName;
    private int definitionId;

    private MeasurementUnits adjustedMeasurementUnits;
    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> metricData;
    private List<DatePair> unknownIntervalList;
    private PageList<Availability> availabilityDownList;
    private PageList<MeasurementOOBComposite> measurementOOBCompositeList;
    private MeasurementOOBComposite lastOOB;

    private Integer chartHeight;

    public MetricGraphData() {

    }

    /**
     * Constructor for the dashboard case when it as a saved configuration.
     * @param entityId
     * @param measurementDefId
     */
    public MetricGraphData(int entityId, int measurementDefId) {
        setEntityId(entityId);
        setDefinitionId(measurementDefId);
    }

    public MetricGraphData(int entityId, String entityName, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> metricData) {
        this.entityName = entityName;
        setEntityId(entityId);
        setDefinitionId(def.getId());
        this.definition = def;
        this.metricData = metricData;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
        this.definition = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
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

    public List<MeasurementDataNumericHighLowComposite> getMetricData() {
        return metricData;
    }

    public void setMetricData(List<MeasurementDataNumericHighLowComposite> metricData) {
        this.metricData = metricData;
    }

    public PageList<Availability> getAvailabilityDownList() {
        return availabilityDownList;
    }

    public void setAvailabilityDownList(PageList<Availability> availabilityDownList) {
        this.availabilityDownList = availabilityDownList;
    }

    public PageList<MeasurementOOBComposite> getMeasurementOOBCompositeList() {
        return measurementOOBCompositeList;
    }

    public void setMeasurementOOBCompositeList(PageList<MeasurementOOBComposite> measurementOOBCompositeList) {
        this.measurementOOBCompositeList = measurementOOBCompositeList;
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

    public Integer getChartHeight() {
        if(null != chartHeight){
            return chartHeight;
        }else {

            return DEFAULT_CHART_HEIGHT;
        }
    }

    public void setChartHeight(Integer chartHeight) {
        this.chartHeight = chartHeight;
    }

    public String getYAxisTitle() {
        if (null != definition.getDisplayName() && definition.getDisplayName().length() > 55) {
            return definition.getDisplayName().substring(0, 55) + "...";
        } else {
            return definition.getDisplayName();
        }
    }

    /**
     * Returns the y-axis units normalized to highest scale (Bytes -> Gb).
     * NOTE: this requires a dependency such that getJsonMetrics is called
     * before this method as the adjustedMeasurementUnits are calculated in that method.
     * @return yAxisUnits -- normalized to highest UOM.
     */
    public String getYAxisUnits() {
        if (adjustedMeasurementUnits == null) {
            Log.error("AbstractMetricD3GraphView.adjustedMeasurementUnits is populated by getJsonMetrics. Make sure it is called first.");
        }
        return adjustedMeasurementUnits.toString();
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
        //Log.debug(" avail records loaded: "+getAvailabilityDownList().size());
        if (null != metricData) {
            long firstBarTime = metricData.get(0).getTimestamp();
            long secondBarTime = metricData.get(1).getTimestamp();
            long barDuration = secondBarTime - firstBarTime;
            String barDurationString = MeasurementConverterClient.format((double) barDuration,
                MeasurementUnits.MILLISECONDS, true);

            calculateOOB();
            calculateUnknownIntervals();
            dumpUnknownIntervals();

            for (MeasurementDataNumericHighLowComposite measurement : metricData) {
                sb.append("{ x:" + measurement.getTimestamp() + ",");

                if (null != availabilityDownList) {
                    // loop through the avail down intervals
                    for (Availability availability : availabilityDownList) {
                        // we know we are in an interval
                        Log.debug(" *** Avail: " + availability);
                        //if (barDateTime.after(datePair.startDateTime) && barDateTime.before(datePair.getEndDateTime())) {
                        if (measurement.getTimestamp() >= availability.getStartTime()
                            && measurement.getTimestamp() <= availability.getEndTime()) {
                            sb.append(" availStart:" + availability.getStartTime() + ", ");
                            sb.append(" availEnd:" + availability.getEndTime() + ", ");
                            long availDuration = availability.getEndTime() - availability.getStartTime();
                            String availDurationString = MeasurementConverterClient.format((double) availDuration,
                                MeasurementUnits.MILLISECONDS, true);
                            sb.append(" availDuration: \"" + availDurationString + "\", ");
                            break;
                        }
                    }
                }
                if (null != lastOOB) {
                    sb.append(" baselineMin:" + lastOOB.getBlMin() + ", ");
                    sb.append(" baselineMax:" + lastOOB.getBlMax() + ", ");
                }

                if (isAvailabilityDownOrDisabledForBar(measurement.getTimestamp())) {
                    sb.append(" down:true, ");
                } else {
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
                        sb.append(" barDuration: \"" + barDurationString + "\", ");
                        sb.append(" high:" + newHigh.getValue() + ",");
                        sb.append(" low:" + newLow.getValue() + ",");
                        sb.append(" y:" + newValue.getValue() + "},");
                    } else {
                        if (!isAvailabilityDownOrDisabledForBar(measurement.getTimestamp())) {
                            // NaN measure no measurement was collected
                            // loop through the unknown intervals
                            for (DatePair datePair : unknownIntervalList) {
                                // we know we are in an interval
                                if (measurement.getTimestamp() >= datePair.getStartDateTime().getTime()
                                    && measurement.getTimestamp() <= datePair.getEndDateTime().getTime()) {
                                    sb.append(" unknownStart:" + datePair.getStartDateTime().getTime() + ", ");
                                    sb.append(" unknownEnd:" + datePair.getEndDateTime().getTime() + ", ");
                                    break;
                                }
                            }
                            sb.append(" nodata:true },");
                        } else {
                            sb.append(" },");
                        }
                    }
                }
                if(!sb.toString().endsWith("},")){
                    sb.append(" },");
                }
            }
            //}
            sb.setLength(sb.length() - 1); // delete the last ','
        }
        sb.append("]");
        Log.debug(sb.toString());
        return sb.toString();
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

    private void dumpUnknownIntervals() {
        for (DatePair datePair : unknownIntervalList) {
            Log.debug("Interval: " + datePair.getStartDateTime() + " - " + datePair.getEndDateTime());
        }
    }

    private void calculateUnknownIntervals() {

        unknownIntervalList = new LinkedList<DatePair>();
        List<Integer> startPoints = new LinkedList<Integer>();
        //find all possible starting interval points
        int i = 0;
        for (MeasurementDataNumericHighLowComposite measurement : metricData) {
            boolean notAtStart = i > 1;
            boolean currentBarUndefined = Double.isNaN(measurement.getValue());
            boolean previousBarDefined = (notAtStart) ? !Double.isNaN(metricData.get(i - 1).getValue()) : false;
            if (currentBarUndefined && previousBarDefined && notAtStart) {
                //Log.debug("Adding Down or Disabled start Point: " + i);
                startPoints.add(i+1);
            }
            i++;
        }
        // iterate over the start points to the end of the consecutive bars or end of metricData
        // from the starting interval points find the interval end point
        for (Integer startPoint : startPoints) {
            Log.debug("StartPoint: " + new Date(metricData.get(startPoint).getTimestamp()));
            for (int j = 0; j < metricData.size() - 1; j++) {
                boolean notAtEnd = j < metricData.size();
                boolean currentBarUndefined = Double.isNaN(metricData.get(j).getValue());
                boolean nextBarDefined = (notAtEnd) ? !Double.isNaN(metricData.get(j + 1).getValue()) : false;
                if (currentBarUndefined && nextBarDefined && notAtEnd) {
                    Date startDate = new Date(metricData.get(startPoint).getTimestamp());
                    Date endDate = new Date(metricData.get(j).getTimestamp());
                    //Log.debug("\n\nStartDate: " + startDate);
                    //Log.debug("EndDate: " + endDate);
                    DatePair datePair = new DatePair(startDate, endDate);
                    unknownIntervalList.add(datePair);
                }
            }
        }
        //Log.debug("intervalDatePairList.size():" + unknownIntervalList.size());
    }

    private boolean isAvailabilityDownOrDisabledForBar(long timestamp) {
        Date timestampDate = new Date(timestamp);
        if (null != availabilityDownList) {
            for (Availability availability : availabilityDownList) {
                if (timestampDate.after(new Date(availability.getStartTime()))
                    && timestampDate.before(new Date(availability.getEndTime()))) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Immutable DatePair for storing the start and DateTime for an interval.
     * Used in measuring downtime and unknown intervals.
     * Intervals are inclusive of the startDateTime and endDateTime.
     */
    private final class DatePair {

        private Date startDateTime;
        private Date endDateTime;

        public DatePair(Date startDateTime, Date endDateTime) {
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
        }

        public Date getStartDateTime() {
            return startDateTime;
        }

        public Date getEndDateTime() {
            return endDateTime;
        }

    }
}
