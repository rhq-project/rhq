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

import java.util.Random;

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
        // We want "random" data, but just enough randomness to possibly trigger some
        // baseline-based alert definitions, but not flood the system with unrealistic alerts.
        // Adding nextGaussian to the scheduleId should produce a Normal distribution of metric 
        // values overtime, with the mean being the scheduleId itself.
        // By changing the baseline frequency and duration so that we only consider a short
        // period when calculating the baselines, and then don't recalculate it very frequently
        // it should be possible to have OOBs triggered at a reasonable rate, e.g. one high
        // and one low OOB per metric schedule per day.

        double value = request.getScheduleId();
        value += (new Random()).nextGaussian();

        MeasurementDataNumeric data = new MeasurementDataNumeric(request, value);
        return data;
    }
}