/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
 *
 */
package org.rhq.server.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.rhq.core.domain.measurement.MeasurementBaseline;

/**
 * @author Stefan Negrea
 */
public class MetricBaselineCalculator {

    public List<MeasurementBaseline> calculateBaselines(List<String> scheduleIds, long startTime, long endTime) {
        List<MeasurementBaseline> calculatedBaselines = new ArrayList<MeasurementBaseline>();

        MeasurementBaseline measurementBaseline;
        for (String scheduleId : scheduleIds) {
            //TODO: do processing to find the actual baseline for this schedule id
            //!use data from Cassandra!

            //for now just return a values
            measurementBaseline = this.generateRandomBaseline();

            if (measurementBaseline != null) {
                calculatedBaselines.add(measurementBaseline);
            }
        }

        return calculatedBaselines;
    }

    private MeasurementBaseline generateRandomBaseline() {
        Random random = new Random(12345);

        MeasurementBaseline randomBaseline = new MeasurementBaseline();
        randomBaseline.setMax(random.nextDouble() * 1000);
        randomBaseline.setMin(random.nextDouble() * 1000);
        randomBaseline.setMean(random.nextDouble() * 1000);

        return new MeasurementBaseline();
    }
}
