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
package org.rhq.plugins.perftest.measurement;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

/**
 * The simplest algorithm for generating measurement values, a random number is simply used as the value.
 *
 * @author Jason Dobies
 */
public class SimpleNumericMeasurementFactory implements MeasurementFactory {
    // MeasurementFactory Implementation  --------------------------------------------

    public MeasurementData nextValue(MeasurementScheduleRequest request) {
        // We want "random" data, but just enough randomness to possibly trigger som OOBs
        // but not flood the system with unrealistic OOBs.
        // I ran a test to confirm that every 10,000 executions of Math.random will produce
        // between 1 and 4 numbers that will be lower than 0.0001 or higher than 0.9999
        // When the random number is between .0001 and .9999, the normal metric value is the schedule id.
        // When the random number is really low, the metric value will be 10% lower than normal.
        // When the random number is really high, the metric value will be 10% higher than normal.

        double value = request.getScheduleId();
        double random = Math.random();

        if (random < 0.0001) {
            value = value * 0.90;
        } else if (random > 0.9999) {
            value = value * 1.10;
        }

        MeasurementDataNumeric data = new MeasurementDataNumeric(request, value);
        return data;
    }
}