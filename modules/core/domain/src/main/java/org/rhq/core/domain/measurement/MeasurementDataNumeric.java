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

    // A shortcut to know if this should be handled as a per minute metric
    private transient boolean perMinuteCollection;
    private transient NumericType numericType;

    protected MeasurementDataNumeric() {
        super();
    }

    public MeasurementDataNumeric(MeasurementScheduleRequest request, Double value) {
        super(request);
        this.value = value;
        this.perMinuteCollection = request.isPerMinute();
        this.numericType = request.getNumericType();
    }

    public MeasurementDataNumeric(long collectionTime, MeasurementScheduleRequest request, Double value) {
        super(collectionTime, request);
        this.value = value;
        this.perMinuteCollection = request.isPerMinute();
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
        return this.perMinuteCollection;
    }

    public NumericType getNumericType() {
        return this.numericType;
    }

    @Override
    public String toString() {
        return "MeasurementDataNumeric[" + "value=[" + new DecimalFormat("0.00").format(this.value) + "], "
            + super.toString() + "]";
    }
}