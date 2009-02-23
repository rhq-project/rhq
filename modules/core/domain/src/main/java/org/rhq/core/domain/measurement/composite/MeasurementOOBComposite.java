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
package org.rhq.core.domain.measurement.composite;

import java.io.Serializable;

import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;

/**
 * Composite that holds information about an oob
 *
 * @author Heiko W. Rupp
 */
public class MeasurementOOBComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private String resourceName;
    private int resourceId;
    private String scheduleName;
    private int scheduleId;
    private long timestamp;
    private int definitionId;
    private int factor72;
    private int factor48;
    private int factor24;
    private int avg72;
    private int avg48;
    private int avg24;
    private double blMin;
    private double blMax;
    private double dataMin;
    private double dataMax;
    private double outlier;
    private boolean outlierIsLow;
    private MeasurementUnits units;

    public MeasurementOOBComposite(String resourceName, int resourceId, String scheduleName, int scheduleId, long timestamp, int definitionId,
                                   int factor72, double avg72, double blMax, double blMin) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.scheduleName = scheduleName;
        this.scheduleId = scheduleId;
        this.definitionId = definitionId;
        this.factor72 = (int) factor72;
        this.avg72 = (int)avg72;
        this.blMin = blMin;
        this.blMax = blMax;
        this.timestamp = timestamp;
    }

        public MeasurementOOBComposite(String resourceName, int resourceId, String scheduleName, int scheduleId, long timestamp, int definitionId,
                                   int factor72, double blMax, double blMin, MeasurementUnits unit) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.scheduleName = scheduleName;
        this.scheduleId = scheduleId;
        this.definitionId = definitionId;
        this.factor72 = (int) factor72;
        this.blMin = blMin;
        this.blMax = blMax;
        this.timestamp = timestamp;
        this.units = unit;
    }

    public MeasurementOOBComposite(String resourceName, int resourceId, String scheduleName, int scheduleId, int definitionId,
                                   long factor72, long factor48, long factor24) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.scheduleName = scheduleName;
        this.scheduleId = scheduleId;
        this.definitionId = definitionId;
        this.factor72 = (int) factor72;
        this.factor48 = (int) factor48;
        this.factor24 = (int) factor24;
    }


    public int getFactor48() {
        return factor48;
    }

    public void setFactor48(int factor48) {
        this.factor48 = factor48;
    }

    public int getFactor24() {
        return factor24;
    }

    public void setFactor24(int factor24) {
        this.factor24 = factor24;
    }

    public String getResourceName() {
        return resourceName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public int getFactor72() {
        return factor72;
    }

    public int getAvg72() {
        return avg72;
    }

    public void setAvg72(int avg72) {
        this.avg72 = avg72;
    }

    public int getAvg48() {
        return avg48;
    }

    public void setAvg48(int avg48) {
        this.avg48 = avg48;
    }

    public int getAvg24() {
        return avg24;
    }

    public void setAvg24(int avg24) {
        this.avg24 = avg24;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getBlMin() {
        return blMin;
    }

    public double getBlMax() {
        return blMax;
    }

    public double getDataMin() {
        return dataMin;
    }

    public double getDataMax() {
        return dataMax;
    }

    public void setDataMin(double dataMin) {
        this.dataMin = dataMin;
    }

    public void setDataMax(double dataMax) {
        this.dataMax = dataMax;
    }

    public void setData(MeasurementDataNumeric1H data) {
        this.dataMin = data.getMin();
        this.dataMax = data.getMax();
    }

    public double getOutlier() {
        return outlier;
    }

    public void setOutlier(double outlier) {
        this.outlier = outlier;
    }

    public void calculateOutlier() {
        if ((blMin - dataMin) < (dataMax-blMax)) {
            outlier = dataMax;
            outlierIsLow = false;
        }
        else {
            outlier = dataMin;
            outlierIsLow = true;
        }
    }

    public String getFormattedBaseband() {

        String min = MeasurementConverter.format(blMin,units,true) ;
        String max = MeasurementConverter.format(blMax,units,true) ;

        String result;
        if (outlierIsLow)
            result = max + " - " + min;
        else
            result = min + " - " + max;

        return result;
    }

    public String getFormattedOutlier() {
        return MeasurementConverter.format(outlier,units,true) ;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MeasurementOOBComposite");
        sb.append("{resourceName='").append(resourceName).append('\'');
        sb.append(", resourceId=").append(resourceId);
        sb.append(", scheduleName='").append(scheduleName).append('\'');
        sb.append(", scheduleId=").append(scheduleId);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", definitionId=").append(definitionId);
        sb.append(", factor72=").append(factor72);
        sb.append(", factor48=").append(factor48);
        sb.append(", factor24=").append(factor24);
        sb.append(", avg72=").append(avg72);
        sb.append(", avg48=").append(avg48);
        sb.append(", avg24=").append(avg24);
        sb.append(", blMin=").append(blMin);
        sb.append(", blMax=").append(blMax);
        sb.append(", dataMin=").append(dataMin);
        sb.append(", dataMax=").append(dataMax);
        sb.append(", outlier=").append(outlier);
        sb.append('}');
        return sb.toString();
    }
}
