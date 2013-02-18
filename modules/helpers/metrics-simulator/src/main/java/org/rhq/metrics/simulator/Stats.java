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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * @author John Sanda
 */
public class Stats {

    private List<RawDataStats> rawDataInserts = new ArrayList<RawDataStats>();

    private SummaryStatistics rawDataInsertTimes = new SummaryStatistics();

    private RawDataStats currentRawDataStats;

    private ReentrantLock rawDataInsertsLock = new ReentrantLock();

    private ReentrantLock rawDataInsertTimesLock = new ReentrantLock();

    public void rawDataInserted(int count) {
        try {
            rawDataInsertsLock.lock();
            currentRawDataStats.getStatistics().addValue(count);
        } finally {
            rawDataInsertsLock.unlock();
        }
    }

    public void addRawDataInsertTime(long time) {
        try {
            rawDataInsertTimesLock.lock();
            rawDataInsertTimes.addValue(time);
        } finally {
            rawDataInsertTimesLock.unlock();
        }
    }

    public void startNewSample() {
        if (currentRawDataStats == null) {
            currentRawDataStats = new RawDataStats();
            currentRawDataStats.startSamplingPeriod();
        } else {
            RawDataStats lastRawDataStats = currentRawDataStats;
            try {
                rawDataInsertsLock.lock();
                lastRawDataStats.endSamplingPeriod();
                currentRawDataStats = new RawDataStats();
                currentRawDataStats.startSamplingPeriod();
            } finally {
                rawDataInsertsLock.unlock();
            }
            rawDataInserts.add(lastRawDataStats);
        }
    }

    public RawDataStats getLastSample() {
        if (rawDataInserts.isEmpty()) {
            return null;
        }
        return rawDataInserts.get(rawDataInserts.size() - 1);
    }

    public StatisticalSummaryValues getAggregateSummary() {
        AggregateSummaryStatistics summary = new AggregateSummaryStatistics();
        List<SummaryStatistics> stats = new ArrayList<SummaryStatistics>(rawDataInserts.size());
        for (RawDataStats raw : rawDataInserts) {
            stats.add(raw.getStatistics());
        }
        return summary.aggregate(stats);
    }

    public SummaryStatistics getRawDataInsertTimes() {
        return rawDataInsertTimes;
    }

}
