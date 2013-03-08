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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * @author John Sanda
 */
public class Stats {

    /**
     * The total number of raw inserts
     */
    private AtomicLong totalRawInserts = new AtomicLong(0);

    private AtomicInteger rawInsertsThisMinute = new AtomicInteger(0);

    private DescriptiveStatistics rawInsertsPerMinute = new DescriptiveStatistics(200);

    private DescriptiveStatistics rawInsertTimesPerMinute = new DescriptiveStatistics(200);

    private ReentrantLock insertTimesLock = new ReentrantLock();

    public void addRawInserts(int count) {
        totalRawInserts.addAndGet(count);
        rawInsertsThisMinute.addAndGet(count);
    }

    public long getTotalRawInserts() {
        return totalRawInserts.get();
    }

    /**
     * Called by measurement collectors to report insertion times. This method uses an
     * internal lock to allow for concurrent access.
     *
     * @param time The time to insert a set of raw metrics
     */
    public void addRawInsertTime(long time) {
        try {
            insertTimesLock.lock();
            rawInsertTimesPerMinute.addValue(time);
        } finally {
            insertTimesLock.unlock();
        }
    }

    public Aggregate getRawInsertTimes() {
        try {
            insertTimesLock.lock();
            return new Aggregate("raw insertion times (milliseconds)", rawInsertTimesPerMinute.getMax(),
                rawInsertTimesPerMinute.getMin(), rawInsertTimesPerMinute.getMean(),
                rawInsertTimesPerMinute.getStandardDeviation());
        } finally {
            insertTimesLock.unlock();
        }
    }

    /**
     * Called by {@link org.rhq.metrics.simulator.StatsCollector} to report the number of raw inserts for a given
     * minute. Since there is only a single {@link org.rhq.metrics.simulator.StatsCollector} this method does not
     * support concurrent access.
     *
     * @param value The number of raw metrics inserted in a given minute.
     */
    public void addRawInsertsPerMinute(long value) {
        rawInsertsPerMinute.addValue(value);
    }

    public Aggregate getRawInsertsPerMinute() {
        return new Aggregate("Raw inserts per minute", rawInsertsPerMinute.getMax(), rawInsertsPerMinute.getMin(),
            rawInsertsPerMinute.getMean(), rawInsertsPerMinute.getStandardDeviation());
    }

}
