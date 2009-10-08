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
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

/**
 * @author Jason Dobies
 */
public interface MeasurementFactory {
    /**
     * Returns a measurement data value for the specified measurement. The actual generation of the next value is
     * dependent on the implementation of this interface.
     *
     * @param  request describes which measurement is being calculated
     *
     * @return next measurement data point
     */
    MeasurementData nextValue(MeasurementScheduleRequest request);
}