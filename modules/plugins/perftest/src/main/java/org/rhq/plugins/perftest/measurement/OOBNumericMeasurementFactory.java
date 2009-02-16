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
 * Generates measurement values, using a step function which repeats every 7days
 *
 * @author Charles Crouch
 */
public class OOBNumericMeasurementFactory implements MeasurementFactory {
    // MeasurementFactory Implementation  --------------------------------------------

    public MeasurementData nextValue(MeasurementScheduleRequest request) {
        double value = request.getScheduleId();

        long currentMillis = System.currentTimeMillis();
        int currentDays = (int) (currentMillis / (24 * 60 * 60 * 1000));
        int dayOfWeek = currentDays % 7;

        // depending on what day it is add 0%, 10%, ... 60% to the value
        // Thursday is 0%
        value += (value * 0.1 * dayOfWeek);

        MeasurementDataNumeric data = new MeasurementDataNumeric(request, value);
        return data;
    }
}