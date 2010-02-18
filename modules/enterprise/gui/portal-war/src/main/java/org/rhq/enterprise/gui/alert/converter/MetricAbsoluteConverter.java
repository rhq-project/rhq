/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
 */
package org.rhq.enterprise.gui.alert.converter;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;

public class MetricAbsoluteConverter {

    private final static Double DEFAULT_VALUE = 0.0;

    public Double getForDisplay(Double threshold, MeasurementDefinition measurementDefinition) {
        if (threshold == null) {
            threshold = DEFAULT_VALUE;
        }

        MeasurementUnits units = measurementDefinition.getUnits();
        return MeasurementUnits.scaleUp(threshold, units);
    }

    public Double getForThreshold(Double displayValue, MeasurementDefinition measurementDefinition) {
        if (displayValue == null) {
            displayValue = DEFAULT_VALUE;
        }

        MeasurementUnits units = measurementDefinition.getUnits();
        return MeasurementUnits.scaleDown(displayValue, units);
    }

}