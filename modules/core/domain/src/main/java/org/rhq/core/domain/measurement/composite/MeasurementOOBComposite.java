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

/**
 * Composite that holds information about an oob
 *
 * @author Heiko W. Rupp
 */
public class MeasurementOOBComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private int resourceId;
    private String resourceName;
    private String resourceAncestry;
    private int resourceTypeId;
    private String scheduleName;
    private int scheduleId;
    private long timestamp;
    private int definitionId;
    private int factor;
    private double blMin;
    private double blMax;
    private double dataMin;
    private double dataMax;
    private double outlier;
    private MeasurementUnits units;
    private String parentName;
    private Integer parentId;
    private String formattedOutlier;
    private String formattedBaseband;

    public MeasurementOOBComposite() {
    }

    public MeasurementOOBComposite(String resourceName, int resourceId, String resourceAncestry, int resourceTypeId,
        String scheduleName, int scheduleId, long timestamp, int definitionId, int factor, double blMin, double blMax,
        MeasurementUnits units, String parentName, Integer parentId) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.resourceAncestry = resourceAncestry;
        this.resourceTypeId = resourceTypeId;
        this.scheduleName = scheduleName;
        this.scheduleId = scheduleId;
        this.definitionId = definitionId;
        this.factor = factor;
        this.blMin = blMin;
        this.blMax = blMax;
        this.timestamp = timestamp;
        this.units = units;
        this.parentId = parentId;
        this.parentName = parentName;
    }

    public MeasurementOOBComposite(String resourceName, int resourceId, String resourceAncestry, int resourceTypeId,
        String scheduleName, int scheduleId, long timestamp, int definitionId, int factor, double blMin, double blMax,
        MeasurementUnits unit) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.resourceAncestry = resourceAncestry;
        this.resourceTypeId = resourceTypeId;
        this.scheduleName = scheduleName;
        this.scheduleId = scheduleId;
        this.definitionId = definitionId;
        this.factor = factor;
        this.blMin = blMin;
        this.blMax = blMax;
        this.timestamp = timestamp;
        this.units = unit;
    }

    public String getResourceName() {
        return resourceName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getResourceAncestry() {
        return resourceAncestry;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
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

    public int getFactor() {
        return factor;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MeasurementUnits getUnits() {
        return units;
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

    public String getParentName() {
        return parentName;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setDataMin(double dataMin) {
        this.dataMin = dataMin;
    }

    public void setDataMax(double dataMax) {
        this.dataMax = dataMax;
    }

    /**
     * @deprecated the {@link MeasurementDataNumeric1H} class is being phased out. Use the {@link #setDataMin(double)}
     * and {@link #setDataMax(double)} for equivalent usage.
     */
    @Deprecated
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
        if ((blMin - dataMin) < (dataMax - blMax)) {
            outlier = dataMax;
        } else {
            outlier = dataMin;
        }
    }

    public String getFormattedBaseband() {
        return formattedBaseband;
    }

    public void setFormattedBaseband(String formattedBaseband) {
        this.formattedBaseband = formattedBaseband;
    }

    public String getFormattedOutlier() {
        return formattedOutlier;
    }

    public void setFormattedOutlier(String formattedOutlier) {
        this.formattedOutlier = formattedOutlier;
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
        sb.append(", factor=").append(factor);
        sb.append(", blMin=").append(blMin);
        sb.append(", blMax=").append(blMax);
        sb.append(", dataMin=").append(dataMin);
        sb.append(", dataMax=").append(dataMax);
        sb.append(", outlier=").append(outlier);
        sb.append(", units=").append(units);
        sb.append(", parentName='").append(parentName).append('\'');
        sb.append(", parentId=").append(parentId);
        sb.append('}');
        return sb.toString();
    }
}
