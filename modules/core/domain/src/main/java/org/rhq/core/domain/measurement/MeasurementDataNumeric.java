 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.measurement;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * Subclass for numerical measurement data
 *
 * @author Heiko W. Rupp
 */
public class MeasurementDataNumeric extends MeasurementData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double value;

    // A shortcut to know if this should be handled as a per-minute metric
    private transient NumericType rawNumericType;

    protected MeasurementDataNumeric() {
        super();
    }

    public MeasurementDataNumeric(MeasurementScheduleRequest request, Double value) {
        super(request);
        this.value = value;
        this.rawNumericType = request.getRawNumericType();
    }

    public MeasurementDataNumeric(long collectionTime, MeasurementScheduleRequest request, Double value) {
        super(collectionTime, request);
        this.value = value;
        this.rawNumericType = request.getRawNumericType();
    }

    @Deprecated
    // Have to make this protected so that people only use the constructor taking a request (so the name can be set for live values)
    public MeasurementDataNumeric(MeasurementDataPK md, Double value) {
        super(md);
        this.value = value;
    }

    @Override
    public Double getValue() {
        return this.value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public boolean isPerMinuteCollection() {
        return this.rawNumericType != null;
    }

    public NumericType getRawNumericType() {
        return this.rawNumericType;
    }

    @Override
    public String toString() {
        return "MeasurementDataNumeric[" + "value=[" + new DecimalFormat("0.00").format(this.value) + "], "
            + super.toString() + "]";
    }
}