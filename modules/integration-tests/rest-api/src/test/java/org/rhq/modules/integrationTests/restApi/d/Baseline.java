/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.integrationTests.restApi.d;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A metric baseline
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class Baseline {

    double min;
    double max;
    double mean;
    long computeTime;

    public Baseline() {
    }

    public Baseline(double min, double max, double mean, long computeTime) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.computeTime = computeTime;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public long getComputeTime() {
        return computeTime;
    }

    public void setComputeTime(long computeTime) {
        this.computeTime = computeTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Baseline baseline = (Baseline) o;

        if (Double.compare(baseline.max, max) != 0) return false;
        if (Double.compare(baseline.mean, mean) != 0) return false;
        if (Double.compare(baseline.min, min) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = min != +0.0d ? Double.doubleToLongBits(min) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = max != +0.0d ? Double.doubleToLongBits(max) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = mean != +0.0d ? Double.doubleToLongBits(mean) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
