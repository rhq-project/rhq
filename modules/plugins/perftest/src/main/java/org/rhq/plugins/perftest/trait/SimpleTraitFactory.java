/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.perftest.trait;

import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

import java.util.Random;

/**
 * Create mock trait data.
 *
 * @author Ian Springer
 */
public class SimpleTraitFactory implements TraitFactory {

    private static final Random RANDOM = new Random();

    public MeasurementDataTrait nextValue(MeasurementScheduleRequest request) {
        // generate "red" roughly 10% of the time, and "green" the rest of the time
        String value = (RANDOM.nextInt(10) % 9) == 0 ? "red" : "green";

        return new MeasurementDataTrait(request, value);
    }
}
