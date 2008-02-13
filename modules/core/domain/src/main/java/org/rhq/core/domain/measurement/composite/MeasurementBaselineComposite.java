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
import org.rhq.core.domain.measurement.MeasurementBaseline;

/**
 * Used mainly as a smaller object for baseline calculations. Holds only the information necessary to compute baselines.
 */
public class MeasurementBaselineComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final double min;
    private final double max;
    private final double mean;
    private final int scheduleId;

    public MeasurementBaselineComposite(int id, double min, double max, double mean, int scheduleId) {
        this.id = id;
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.scheduleId = scheduleId;
    }

    public MeasurementBaselineComposite(MeasurementBaseline baseline) {
        this.id = baseline.getId();
        this.min = baseline.getMin();
        this.max = baseline.getMax();
        this.mean = baseline.getMean();
        this.scheduleId = baseline.getSchedule().getId();
    }

    public int getId() {
        return id;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getMean() {
        return mean;
    }

    public int getScheduleId() {
        return scheduleId;
    }
}