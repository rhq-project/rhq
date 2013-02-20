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

package org.rhq.metrics.simulator;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * @author John Sanda
 */
public class Stats {

    /**
     * The total number of raw data inserted
     */
    private AtomicLong totalRawInserts = new AtomicLong(0);

    /**
     * A set of per-minute samples of raw inserts
     */
    private NavigableSet<RawInserts> rawInsertsPerMinute = new TreeSet<RawInserts>();

    private int rawInsertsThisInterval;

    private long currentInterval;

    private DescriptiveStatistics rawDataInsertTimes = new DescriptiveStatistics(200);

    private ReentrantLock insertCountsLock = new ReentrantLock();

    private ReentrantLock insertTimesLock = new ReentrantLock();

    public void addRawInserts(int count) {
        totalRawInserts.addAndGet(count);
        try {
            insertCountsLock.lock();
            rawInsertsThisInterval += count;
        } finally {
            insertCountsLock.unlock();
        }
    }

    public long getTotalRawInserts() {
        return totalRawInserts.get();
    }

    public RawInserts getRawInsertsForLastInterval() {
        return rawInsertsPerMinute.pollLast();
    }

    public void startNewInterval(long startTime) {
        int insertsLastInterval;
        try {
            insertCountsLock.lock();
            insertsLastInterval = rawInsertsThisInterval;
            rawInsertsThisInterval = 0;
        } finally {
            insertCountsLock.unlock();
        }
        rawInsertsPerMinute.add(new RawInserts(currentInterval, insertsLastInterval));
        currentInterval = startTime;
    }

    public void addRawDataInsertTime(long time) {
        try {
            insertTimesLock.lock();
            rawDataInsertTimes.addValue(time);
        } finally {
            insertTimesLock.unlock();
        }
    }

    public InsertionTimes getInsertionTimes() {
        try {
            insertTimesLock.lock();
            return new InsertionTimes(rawDataInsertTimes.getMax(), rawDataInsertTimes.getMin(),
                rawDataInsertTimes.getMean(), rawDataInsertTimes.getStandardDeviation());
        } finally {
            insertTimesLock.unlock();
        }
    }
}
