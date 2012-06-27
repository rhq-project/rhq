/*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.domain.measurement;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.measurement.calltime.CallTimeData;

/**
 * A report of measurement information. When a plugin collects measurement data that is emitted from a monitored
 * resource, it will store that measurement data in an instance of this type.
 * <p/>
 * <p>This report has an optimized serialization strategy.</p>
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class MeasurementReport implements Serializable {
    private static final long serialVersionUID = 1;

    /*
     * instead of using synchronized collection wrappers - which won't GWT-compile - we can use the synchronized
     * modifier on the various methods that access these collections
     */
    private Set<MeasurementDataNumeric> measurementNumericData = new LinkedHashSet<MeasurementDataNumeric>();
    private Set<MeasurementDataTrait> measurementTraitData = new LinkedHashSet<MeasurementDataTrait>();
    private Set<CallTimeData> callTimeData = new LinkedHashSet<CallTimeData>();

    private long collectionTime;

    /**
     * Adds the given piece of numeric data to this report. Items are added to this report's list of numeric data in order.
     * That is, the order in which you add multiple pieces of numeric data is the same order in which you get them
     * in the list via {@link #getNumericData()}.
     *
     * @param value the measurement data to be added
     */
    public synchronized void addData(MeasurementDataNumeric value) {
        measurementNumericData.add(value); // note, duplicates will be silently ignored
    }

    /**
     * Adds the given piece of trait data to this report. Items are added to this report's list of trait data in order.
     * That is, the order in which you add multiple pieces of trait data is the same order in which you get them
     * in the list via {@link #getTraitData()}.
     *
     * @param value the measurement data to be added
     */
    public synchronized void addData(MeasurementDataTrait value) {
        measurementTraitData.add(value); // note, duplicates will be silently ignored
    }

    /**
     * Adds the given call-time data to this report. Items are added to this report's list of call-time data in order. That is,
     * the order in which you add multiple pieces of call-time data is the same order in which you get them in the list
     * via {@link #getCallTimeData()}.
     *
     * @param data the call-time data to be added
     */
    public synchronized void addData(CallTimeData data) {
        this.callTimeData.add(data);
    }

    /**
     * Returns the set of all numeric data items in this report. The data items are ordered in the set in the same order
     * in which they were {@link #addData(MeasurementDataNumeric) added}.
     *
     * @return the list of all the numeric data items in this report
     */
    public synchronized Set<MeasurementDataNumeric> getNumericData() {
        return measurementNumericData;
    }

    /**
     * Returns the set of all trait data items in this report. The data items are ordered in the set in the same order
     * in which they were {@link #addData(MeasurementDataTrait) added}.
     *
     * @return the list of all the trait data items in this report
     */
    public synchronized Set<MeasurementDataTrait> getTraitData() {
        return measurementTraitData;
    }

    /**
     * Returns the entire list of all call-time data items in this report. The data items are ordered in the set in the
     * same order in which they were {@link #addData(CallTimeData) added}.
     *
     * @return the list of all the call-time data items in this report
     */
    @NotNull
    public synchronized Set<CallTimeData> getCallTimeData() {
        return callTimeData;
    }

    /**
     * Returns the total number of data items (numeric metrics, traits, or call-time datums) in this report.
     *
     * @return the total number of data items (numeric metrics, traits, or call-time datums) in this report
     */
    public synchronized long getDataCount() {
        return this.measurementNumericData.size() + this.measurementTraitData.size() + this.callTimeData.size();
    }

    public synchronized long getCollectionTime() {
        return collectionTime;
    }

    public synchronized void setCollectionTime(long collectionTime) {
        this.collectionTime = collectionTime;
    }

    public synchronized void incrementCollectionTime(long collectionTime) {
        this.collectionTime += collectionTime;
    }

    /**
     * Adds measurement data from the given report and updates the collection time.
     * The assumption is the given report is newer than this instance.
     *
     * @param report the report that contains the measurements to add
     * @param metrics the metrics whose measurement data (if found in the given report) should be added to this report.
     *                If null or empty, then all of the data in the given report will be added to this report.
     * @param removeSourceData if true, any data that was transferred from <code>report</code> to this object will be removed
     *                         from the original <code>report</code> that was passed into this method.
     */
    public synchronized void add(MeasurementReport report, Set<MeasurementScheduleRequest> metrics,
        boolean removeSourceData) {
        if (metrics == null || metrics.isEmpty()) {
            measurementNumericData.addAll(report.measurementNumericData);
            measurementTraitData.addAll(report.measurementTraitData);
            callTimeData.addAll(report.callTimeData);
            if (removeSourceData) {
                report.measurementNumericData.clear();
                report.measurementTraitData.clear();
                report.callTimeData.clear();
            }
        } else {
            // note that usually the metric set is very small (typically not more than around 5, probably normally around 1 or 2)
            // so this loop isn't going to be performed with lots of iterations.
            for (MeasurementScheduleRequest metric : metrics) {
                switch (metric.getDataType()) {
                case MEASUREMENT: {
                    Iterator<MeasurementDataNumeric> i = report.measurementNumericData.iterator();
                    while (i.hasNext()) {
                        MeasurementDataNumeric data = i.next();
                        if (data.getName().equals(metric.getName())) {
                            measurementNumericData.add(data);
                            if (removeSourceData) {
                                i.remove();
                            }
                        }
                    }
                    break;
                }
                case TRAIT: {
                    Iterator<MeasurementDataTrait> i = report.measurementTraitData.iterator();
                    while (i.hasNext()) {
                        MeasurementDataTrait data = i.next();
                        if (data.getName().equals(metric.getName())) {
                            measurementTraitData.add(data);
                            if (removeSourceData) {
                                i.remove();
                            }
                        }
                    }
                    break;
                }
                case CALLTIME: {
                    // There is only ever one calltime metric per resource so if we are being asked to
                    // add the calltime data, we can avoid doing any iterations and just use addAll API.
                    callTimeData.addAll(report.callTimeData);
                    if (removeSourceData) {
                        report.callTimeData.clear();
                    }

                    break;
                }
                default: {
                    // ignore any others, we only care about measurement type data
                }
                }
            }
        }

        setCollectionTime(report.collectionTime);
    }

    /**
     * Returns a debug string.
     */
    @Override
    public String toString() {
        return "MeasurementReport [measurementNumericData="
                + measurementNumericData + ", measurementTraitData="
                + measurementTraitData + ", callTimeData=" + callTimeData
                + ", collectionTime=" + collectionTime + "]";
    }

}