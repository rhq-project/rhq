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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This contains a set of measurement schedules for one resource.
 *
 * @author <a href="mailto:heiko.rupp@redhat.com">Heiko W. Rupp</a>
 */
public class ResourceMeasurementScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<MeasurementScheduleRequest> measurementSchedules = new HashSet<MeasurementScheduleRequest>();
    private final int resourceId;

    /**
     * Creates a {@link ResourceMeasurementScheduleRequest} object that will contain measurement schedules for the given
     * resource.
     *
     * @param resourceId ID of the resource that is associated with the encapsulated schedules
     */
    public ResourceMeasurementScheduleRequest(int resourceId) {
        this.resourceId = resourceId;
    }

    public void addMeasurementScheduleRequest(MeasurementScheduleRequest scheduleRequest) {
        this.measurementSchedules.add(scheduleRequest);
    }

    public Set<MeasurementScheduleRequest> getMeasurementSchedules() {
        return measurementSchedules;
    }

    public void setMeasurementSchedules(Set<MeasurementScheduleRequest> measurementSchedules) {
        this.measurementSchedules = measurementSchedules;
    }

    public int getResourceId() {
        return resourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (!(o instanceof ResourceMeasurementScheduleRequest))) {
            return false;
        }

        final ResourceMeasurementScheduleRequest rmsr = (ResourceMeasurementScheduleRequest) o;
        return (this.resourceId == rmsr.resourceId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.resourceId;

        return result;
    }

}