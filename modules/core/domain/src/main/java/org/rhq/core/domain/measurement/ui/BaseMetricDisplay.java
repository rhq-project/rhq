/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.measurement.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a metric that may be displayed in a list context. All fields refer to display needs in a list context in
 * the monitoring UI.
 */
public abstract class BaseMetricDisplay extends MeasurementSummary implements java.io.Serializable, Comparable {
    private static final long serialVersionUID = 1L;

    private Long beginTimeFrame;

    private Long endTimeFrame;

    private String label;

    private String units;

    private Integer scheduleId;

    private Integer definitionId;

    private Integer collectionType;

    private Boolean showNumberCollecting;

    private int numberCollecting = -1;

    private String metricSource;

    private int metricSourceId;

    private String description;

    private Map<String, MetricDisplayValue> metrics;

    protected static final List<String> attrKeyList = Arrays.asList(MetricDisplayConstants.attrKey);

    /**
     * Constructor for MetricDisplaySummary.
     */
    public BaseMetricDisplay() {
        super();
        metrics = new HashMap<String, MetricDisplayValue>();
    }

    public double[] getMetricValueDoubles() {
        List<Double> values = new ArrayList<Double>();
        for (Map.Entry<String, MetricDisplayValue> ent : metrics.entrySet()) {
            if ((ent.getKey() != null) && (ent.getValue() != null)) {
                values.add(ent.getValue().getValue());
            }
        }

        int i = 0;
        double[] rv = new double[values.size()];
        for (Double v : values) {
            rv[i] = v;
            ++i;
        }

        return rv;
    }

    public String[] getMetricKeys() {
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, MetricDisplayValue> ent : metrics.entrySet()) {
            if ((ent.getKey() != null) && (ent.getValue() != null)) {
                keys.add(ent.getKey());
            }
        }

        int i = 0;
        String[] rv = new String[keys.size()];
        for (String key : keys) {
            rv[i] = key;
            ++i;
        }

        return rv;
    }

    public void setMetric(String key, MetricDisplayValue value) {
        if (!attrKeyList.contains(key)) {
            throw new IllegalArgumentException(key + " is not a known metric value");
        }

        metrics.put(key, value);
    }

    public MetricDisplayValue getMetric(String key) {
        if (key == null) {
            throw new IllegalArgumentException("'null' is not a valid metric key");
        }

        return metrics.get(key);
    }

    public Map<String, MetricDisplayValue> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, MetricDisplayValue> metrics) {
        this.metrics = metrics;
    }

    public MetricDisplayValue getMinMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.MIN_KEY);
        if (mdv == null) {
            throw new IllegalArgumentException("No valid metric key: " + MetricDisplayConstants.MIN_KEY);
        }

        return mdv;
    }

    public MetricDisplayValue getMaxMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.MAX_KEY);
        if (mdv == null) {
            throw new IllegalArgumentException("No valid metric key: " + MetricDisplayConstants.MAX_KEY);
        }

        return mdv;
    }

    public MetricDisplayValue getAvgMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.AVERAGE_KEY);
        if (mdv == null) {
            throw new IllegalArgumentException("No valid metric key: " + MetricDisplayConstants.AVERAGE_KEY);
        }

        return mdv;
    }

    public MetricDisplayValue getLastMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.LAST_KEY);
        if (mdv == null) {
            throw new IllegalArgumentException("No valid metric key: " + MetricDisplayConstants.LAST_KEY);
        }

        return mdv;
    }

    public MetricDisplayValue getSummaryMetric() {
        MetricDisplayValue mdv = getMetric(MetricDisplayConstants.SUMMARY_KEY);
        if (mdv == null) {
            throw new IllegalArgumentException("No valid metric key: " + MetricDisplayConstants.SUMMARY_KEY);
        }

        return mdv;
    }

    /**
     * Method getBeginTimeFrame. All metrics displayed are within a timeframe. The beginning of that timeframe is
     * represented as the number of epoch seconds at which the timeframe commences, this method returns that Long value.
     *
     * @return Long
     */
    public Long getBeginTimeFrame() {
        return this.beginTimeFrame;
    }

    /**
     * Method setBeginTimeFrame. All metrics displayed are within a timeframe. The beginning of that timeframe is
     * represented as the number of epoch seconds at which the timeframe commences, this method sets that Long value.
     *
     * @param beginTimeFrame The beginTimeFrame to set
     */
    public void setBeginTimeFrame(Long beginTimeFrame) {
        this.beginTimeFrame = beginTimeFrame;
    }

    /**
     * Method getEndTimeFrame. All metrics displayed are within a timeframe. The end of that timeframe is represented as
     * the number of epoch seconds at which the timeframe is finished, this method returns that Long value.
     *
     * @return Long
     */
    public Long getEndTimeFrame() {
        return this.endTimeFrame;
    }

    /**
     * Method setEndTimeFrame. All metrics displayed are within a timeframe. The end of that timeframe is represented as
     * the number of epoch seconds at which the timeframe is finished, this method sets that Long value.
     *
     * @param endTimeFrame The endTimeFrame to set
     */
    public void setEndTimeFrame(Long endTimeFrame) {
        this.endTimeFrame = endTimeFrame;
    }

    /**
     * Method getLabel. The name of the metric as it is displayed, perhaps the "alias"
     *
     * @return String
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Method setLabel. The name of the metric as it is displayed, perhaps the "alias"
     *
     * @param label The label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Method getIntervalUnits. Returns the label for the units of the metric (if not intrinsic to the metric itself)
     * measurement
     *
     * @return String
     */
    public String getUnits() {
        return this.units;
    }

    /**
     * Method setIntervalUnits. Sets the label for the units of the metric (if not intrinsic to the metric itself)
     * measurement
     *
     * @param units The units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * Get the collection type for the metrics. This value matches to MeasurementConstants.COLL_TYPE_*
     */
    public Integer getCollectionType() {
        return this.collectionType;
    }

    public void setCollectionType(Integer collectionType) {
        this.collectionType = collectionType;
    }

    /**
     * @return Integer
     */
    public Integer getScheduleId() {
        return scheduleId;
    }

    /**
     * Sets the scheduleId.
     *
     * @param scheduleId The scheduleId to set
     */
    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Integer getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Integer definitionId) {
        this.definitionId = definitionId;
    }

    /**
     * Returns the showNumberCollecting.
     *
     * @return boolean
     */
    public Boolean getShowNumberCollecting() {
        return showNumberCollecting;
    }

    public String getMetricSource() {
        return metricSource;
    }

    public void setMetricSource(String string) {
        metricSource = string;
    }

    public int getMetricSourceId() {
        return metricSourceId;
    }

    public void setMetricSourceId(int id) {
        metricSourceId = id;
    }

    /**
     * Sets the showNumberCollecting.
     *
     * @param showNumberCollecting The showNumberCollecting to set
     */
    public void setShowNumberCollecting(Boolean showNumberCollecting) {
        this.showNumberCollecting = showNumberCollecting;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(BaseMetricDisplay.class.getName());
        sb.append("(label=").append(label);
        sb.append(",beginTimeFrame=").append(beginTimeFrame);
        sb.append(",endTimeFrame=").append(endTimeFrame);
        sb.append(",units=").append(units);
        sb.append(",collectionType=").append(collectionType);
        sb.append(",metricSource=").append(metricSource);
        sb.append(",scheduleId=").append(scheduleId);
        sb.append(",showNumberCollecting=").append(showNumberCollecting);
        sb.append("metrics[");
        for (String anAttrKey : MetricDisplayConstants.attrKey) {
            sb.append("\n").append(anAttrKey).append("=").append(metrics.get(anAttrKey));
        }

        sb.append("]\n)");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        if (arg0 instanceof BaseMetricDisplay) {
            BaseMetricDisplay to = (BaseMetricDisplay) arg0;
            return this.getLabel().compareTo(to.getLabel());
        }

        throw new IllegalArgumentException("Cannot compare to non-BaseMetricDisplay object: " + arg0);
    }

    /**
     * @return the numberCollecting
     */
    public int getNumberCollecting() {
        return numberCollecting;
    }

    /**
     * @param numberCollecting the numberCollecting to set
     */
    public void setNumberCollecting(int numberCollecting) {
        this.numberCollecting = numberCollecting;
    }
}