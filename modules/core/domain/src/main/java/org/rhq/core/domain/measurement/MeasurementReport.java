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
package org.rhq.core.domain.measurement;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.measurement.calltime.CallTimeData;

/**
 * A report of measurement information. When a plugin collects measurement data that is emitted from a monitored
 * resource, it will store that measurement data in an instance of this type.
 *
 * <p>This report has an optimized serialization strategy.</p>
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class MeasurementReport implements Externalizable {
    private static final long serialVersionUID = 1;

    private static final Log LOG = LogFactory.getLog(MeasurementReport.class);

    private Set<MeasurementDataNumeric> measurementNumericData = Collections
        .synchronizedSet(new LinkedHashSet<MeasurementDataNumeric>());
    private Set<MeasurementDataTrait> measurementTraitData = Collections
        .synchronizedSet(new LinkedHashSet<MeasurementDataTrait>());
    private Set<CallTimeData> callTimeData = Collections.synchronizedSet(new LinkedHashSet<CallTimeData>());

    private long collectionTime;

    /**
     * Adds the given piece of measurement data to this report. Items are added to this report's list of data in order.
     * That is, the order in which you add multiple pieces of measurement data is the same order in which you get them
     * in the list via {@link #getNumericData()}.
     *
     * @param value the measurement data to be added
     */
    public void addData(MeasurementDataNumeric value) {
        if (!measurementNumericData.add(value)) {
            LOG.info("Measurement collected twice, second value ignored: " + value);
        }
    }

    /**
     * Adds the given piece of measurement data to this report. Items are added to this report's list of data in order.
     * That is, the order in which you add multiple pieces of measurement data is the same order in which you get them
     * in the list via {@link #getNumericData()}.
     *
     * @param value the measurement data to be added
     */
    public void addData(MeasurementDataTrait value) {
        if (!measurementTraitData.add(value)) {
            LOG.info("Measurement collected twice, second value ignored: " + value);
        }
    }

    /**
     * Adds the given call-time data to this report. Items are added to this report's list of data in order. That is,
     * the order in which you add multiple pieces of call-time data is the same order in which you get them in the list
     * via {@link #getCallTimeData()}.
     *
     * @param data the call-time data to be added
     */
    public void addData(CallTimeData data) {
        this.callTimeData.add(data);
    }

    /**
     * Returns the entire list of all measurement data (i.e. numeric metrics or traits) stored in this report. The data
     * items are ordered in the list by the order in which they were
     *
     * @return the list of all the measurement data in this report
     */
    public Set<MeasurementDataNumeric> getNumericData() {
        return measurementNumericData;
    }

    public Set<MeasurementDataTrait> getTraitData() {
        return measurementTraitData;
    }

    /**
     * Returns the entire list of all call-time data stored in this report. The data items are ordered in the list by
     * the order in which they were {@link #addData(CallTimeData) added}.
     *
     * @return the list of all the call-time data in this report
     */
    @NotNull
    public Set<CallTimeData> getCallTimeData() {
        return callTimeData;
    }

    public long getDataCount() {
        return this.measurementNumericData.size() + this.measurementTraitData.size() + this.callTimeData.size();
    }

    public long getCollectionTime() {
        return collectionTime;
    }

    public void setCollectionTime(long collectionTime) {
        this.collectionTime = collectionTime;
    }

    public void incrementCollectionTime(long collectionTime) {
        this.collectionTime += collectionTime;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(measurementNumericData.size());
        for (MeasurementDataNumeric datum : measurementNumericData) {
            out.writeInt(datum.getScheduleId());
            out.writeLong(datum.getTimestamp());
            out.writeDouble(datum.getValue());
        }

        out.writeInt(measurementTraitData.size());
        for (MeasurementDataTrait datum : measurementTraitData) {
            out.writeInt(datum.getScheduleId());
            out.writeLong(datum.getTimestamp());
            String value = datum.getValue();
            out.writeUTF((value != null) ? value : "");
        }

        out.writeObject(this.callTimeData);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int scheduleId = in.readInt();
            long timestamp = in.readLong();
            double doubleValue = in.readDouble();
            measurementNumericData.add(new MeasurementDataNumeric(new MeasurementDataPK(timestamp, scheduleId),
                doubleValue));
        }

        count = in.readInt();
        for (int i = 0; i < count; i++) {
            int scheduleId = in.readInt();
            long timestamp = in.readLong();
            String stringValue = in.readUTF();

            // this should never be null, but i'm paranoid
            if (stringValue == null) {
                stringValue = "";
            }

            measurementTraitData
                .add(new MeasurementDataTrait(new MeasurementDataPK(timestamp, scheduleId), stringValue));
        }

        this.callTimeData = (Set<CallTimeData>) in.readObject();
    }
}