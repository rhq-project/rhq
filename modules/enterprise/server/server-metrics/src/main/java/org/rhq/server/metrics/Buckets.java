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

package org.rhq.server.metrics;

import org.joda.time.DateTime;

/**
 * @author John Sanda
 */
public class Buckets {
    static class Bucket {
        // start time is inclusive
        private long startTime;

        // end time is exclusive
        private long endTime;

        private ArithmeticMeanCalculator mean;
        private double max;
        private double min;
        private int count;

        public Bucket(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.mean = new ArithmeticMeanCalculator();
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public Bucket insert(double value, double min, double max) {
            mean.add(value);
            if (count == 0) {
                this.min = min;
                this.max = max;
            } else {
                if (min < this.min) {
                    this.min = min;
                }
                if (max > this.max) {
                    this.max = max;
                }
            }
            count++;
            return this;
        }

        public double getAvg() {
            if (count == 0) {
                return Double.NaN;
            }
            return mean.getArithmeticMean();
        }

        public double getMax() {
            if (count == 0) {
                return Double.NaN;
            }
            return max;
        }

        public double getMin() {
            if (count == 0) {
                return Double.NaN;
            }
            return min;
        }
    }

    // Default value
    private int numDataPoints = 60;

    private Bucket[] buckets;

    private long interval;

    public Buckets(DateTime beginTime, DateTime endTime) {
        this(beginTime.getMillis(), endTime.getMillis());
    }

    public Buckets(long beginTime, long endTime, int numberOfDataPoints) {
        if (numberOfDataPoints<=0) {
            throw new IllegalArgumentException("Number of buckets must be > 0");
        }
        numDataPoints = numberOfDataPoints;
        buckets = new Bucket[numDataPoints];
        interval = (endTime - beginTime) / numDataPoints;
        for (int i = 1; i <= numDataPoints; ++i) {
            buckets[i - 1] = new Bucket(beginTime + (interval * (i - 1)), beginTime + (interval * i));
        }
    }

    public Buckets(long beginTime, long endTime) {
        this(beginTime, endTime,60);
    }

    public int getNumDataPoints() {
        return numDataPoints;
    }

    public long getInterval() {
        return interval;
    }

    public Bucket get(int index) {
        return buckets[index];
    }

    public void insert(long timestamp, double value, double min, double max) {
        for (Bucket bucket : buckets) {
            if (timestamp >= bucket.getStartTime() && timestamp < bucket.getEndTime()) {
                bucket.insert(value, min, max);
                return;
            }
        }
    }
}
