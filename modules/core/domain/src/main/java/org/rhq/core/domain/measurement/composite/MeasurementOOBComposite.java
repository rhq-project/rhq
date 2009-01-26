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
    private int definitionId;
    private int factor72;
    private int factor48;
    private int factor24;


    public MeasurementOOBComposite(String resourceName, int resourceId, String scheduleName, int scheduleId, int definitionId,
                                   long factor72) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.scheduleName = scheduleName;
        this.scheduleId = scheduleId;
        this.definitionId = definitionId;
        this.factor72 = (int) factor72;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MeasurementOOBComposite");
        sb.append("{resourceName='").append(resourceName).append('\'');
        sb.append(", resourceId=").append(resourceId);
        sb.append(", scheduleName='").append(scheduleName).append('\'');
        sb.append(", scheduleId=").append(scheduleId);
        sb.append(", definitionId=").append(definitionId);
        sb.append(", factor72=").append(factor72);
        sb.append(", factor48=").append(factor48);
        sb.append(", factor24=").append(factor24);
        sb.append('}');
        return sb.toString();
    }
}
