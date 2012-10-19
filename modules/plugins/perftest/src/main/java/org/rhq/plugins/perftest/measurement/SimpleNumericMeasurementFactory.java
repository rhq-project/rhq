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

import java.util.Random;

/**
 * Create mock numeric data.
 *
 * @author Ian Springer
 */
public class SimpleNumericMeasurementFactory implements MeasurementFactory {

    private static final Random RANDOM = new Random();

    public MeasurementData nextValue(MeasurementScheduleRequest request) {
        // generate a random value between 95,000 and 105,000
        double value = 100000 + (RANDOM.nextInt(10000) - 5000);

        return new MeasurementDataNumeric(request, value);
    }

}