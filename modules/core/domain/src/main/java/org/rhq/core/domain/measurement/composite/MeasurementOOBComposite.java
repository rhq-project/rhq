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
    private int count;
    private int factor;

    public MeasurementOOBComposite(String resourceName, int resourceId, String scheduleName, int scheduleId, int definitionId, long count,
                                   long factor) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
        this.scheduleName = scheduleName;
        this.scheduleId = scheduleId;
        this.definitionId = definitionId;
        this.count = (int) count;
        this.factor = (int) factor;
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

    public int getCount() {
        return count;
    }

    public int getDefinitionId() {
        return definitionId;
    }

    public int getFactor() {
        return factor;
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
        sb.append(", count=").append(count);
        sb.append(", factor=").append(factor);
        sb.append('}');
        return sb.toString();
    }
}
