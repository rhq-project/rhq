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

import java.util.Date;
import java.util.List;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.JsonMetricProducer;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;

/**
 * The data portion of the graphs making these methods accessible via JSNI to
 * classes extending this and implementing graphs.
 * Provide i18n labels and format the json data for the graph taking things into
 * consideration such as
 * @author Mike Thompson
 */
public class MetricGraphData implements JsonMetricProducer {

    // i18n
    final protected Messages MSG = CoreGUI.getMessages();
    private final String chartTitleMinLabel = MSG.chart_title_min_label();
    private final String chartTitleAvgLabel = MSG.chart_title_avg_label();
    private final String chartTitlePeakLabel = MSG.chart_title_peak_label();
    private final String chartDateLabel = MSG.chart_date_label();
    private final String chartTimeLabel = MSG.chart_time_label();
    private final String chartDownLabel = MSG.chart_down_label();
    private final String chartUnknownLabel = MSG.chart_unknown_label();

    private int entityId;
    private String entityName;
    private int definitionId;

    private MeasurementUnits adjustedMeasurementUnits;
    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> metricData;
    private PageList<Availability> availabilityDownList;

    public MetricGraphData() {

    }

    /**
     * Constructor for the dashboard case when it as a saved configuration.
     * @param entityId
     * @param measurementDefId
     */
    public MetricGraphData(int entityId, int measurementDefId){
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
        Log.debug("DefId: "+ definitionId);
        Log.debug("EntityId: "+entityId );
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

    public String getYAxisTitle() {
        if (null != definition.getDisplayName() && definition.getDisplayName().length() > 55) {
            return entityName + " - " + definition.getDisplayName().substring(0, 55) + "...";
        } else {
            return entityName + " - " + definition.getDisplayName();
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
    public String getJsonMetrics() {
        StringBuilder sb = new StringBuilder("[");
        boolean gotAdjustedMeasurementUnits = false;
        //Log.debug(" avail records loaded: "+getAvailabilityDownList().size());
        if(null !=  metricData){
            for (MeasurementDataNumericHighLowComposite measurement : metricData) {
                sb.append("{ x:" + measurement.getTimestamp() + ",");
                if (isTimestampDownOrDisabled(measurement.getTimestamp())) {
                    sb.append(" down:true, ");
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
                    sb.append(" high:" + newHigh.getValue() + ",");
                    sb.append(" low:" + newLow.getValue() + ",");
                    sb.append(" y:" + newValue.getValue() + "},");
                } else {
                    // NaN measure no measurement was collected
                    sb.append(" nodata:true },");
                }
            }
            sb.setLength(sb.length() - 1); // delete the last ','
        }
        sb.append("]");
        //Log.debug("Json data has "+data.size()+" entries.");
        //Log.debug(sb.toString());
        return sb.toString();
    }

    private boolean isTimestampDownOrDisabled(long timestamp) {
        Date timestampDate = new Date(timestamp);
        if (null == availabilityDownList) {
            //@todo: take this out this is just to testing purposes
            Log.debug("AvailabilityList is null");
        }
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
        MeasurementNumericValueAndUnits returnValue = null;

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
}
