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
package org.rhq.core.domain.measurement.calltime;

import org.jetbrains.annotations.NotNull;

/**
 * The composite object returned by GUI-initiated call-time data queries.
 *
 * @author Ian Springer
 */
public class CallTimeDataComposite {
    String callDestination;
    private double minimum;
    private double maximum;
    private double total;
    private long count;
    private double average;

    public CallTimeDataComposite(@NotNull
    String callDestination, @NotNull
    Number minimum, @NotNull
    Number maximum, @NotNull
    Number total, @NotNull
    Number count, @NotNull
    Number average) {
        this.callDestination = callDestination;
        this.minimum = minimum.doubleValue();
        this.maximum = maximum.doubleValue();
        this.total = total.doubleValue();
        this.count = count.longValue();
        this.average = average.doubleValue();
    }

    @NotNull
    public String getCallDestination() {
        return this.callDestination;
    }

    public double getMinimum() {
        return this.minimum;
    }

    public double getMaximum() {
        return this.maximum;
    }

    public double getTotal() {
        return this.total;
    }

    public long getCount() {
        return this.count;
    }

    public double getAverage() {
        return this.average;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + "callDestination=" + this.callDestination + ", " + "minimum="
            + this.minimum + ", " + "maximum=" + this.maximum + ", " + "total=" + this.total + ", " + "count="
            + this.total + ", " + "average=" + this.average + "]";
    }
}