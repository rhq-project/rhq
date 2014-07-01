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
package org.rhq.core.domain.measurement.calltime;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

/**
 * The composite object returned by GUI-initiated call-time data queries.
 *
 * @author Ian Springer
 */
public class CallTimeDataComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    String callDestination;
    private double minimum;
    private double maximum;
    private double total;
    private long count;
    private double average;

    /** No-args constructor for JAXB serialization purposes */
    @SuppressWarnings("unused")
    private CallTimeDataComposite() {
    }

    public CallTimeDataComposite(String callDestination, double minimum,
            double maximum, double total, long count, double average)
    {
        this.callDestination = callDestination;
        this.minimum = minimum;
        this.maximum = maximum;
        this.total = total;
        this.count = count;
        this.average = average;
    }

    public CallTimeDataComposite(@NotNull String callDestination, @NotNull Number minimum, @NotNull Number maximum,
        @NotNull Number total, @NotNull Number count, @NotNull Number average) {
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
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1) + "[" + "callDestination=" + this.callDestination + ", " + "minimum="
            + this.minimum + ", " + "maximum=" + this.maximum + ", " + "total=" + this.total + ", " + "count="
            + this.count + ", " + "average=" + this.average + "]";
    }
}