/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.metrics.simulator.stats;

/**
 * @author John Sanda
 */
public class Aggregate {

    private String name;
    private double max;
    private double min;
    private double mean;
    private double standardDeviation;


    public Aggregate(String name, double max, double min, double mean, double standardDeviation) {
        this.name = name;
        this.max = max;
        this.min = min;
        this.mean = mean;
        this.standardDeviation = standardDeviation;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    @Override
    public String toString() {
        return name + ": {min: " + getMin() + ", mean: " + getMean() + ", max: " + getMax() + ", standardDeviation: " +
            getStandardDeviation() + "}";
    }
}
