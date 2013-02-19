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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class StatsCollector implements Runnable {

    private final Log log = LogFactory.getLog(StatsCollector.class);

    private Stats stats;

    public StatsCollector(Stats stats) {
        this.stats = stats;
    }

    @Override
    public void run() {
        long totalRawInserts = stats.getTotalRawInserts();
        long now = System.currentTimeMillis();
        stats.startNewInterval(now);
        RawInserts inserts = stats.getRawInsertsForLastInterval();

        // inserts will be null on the first run
        if (inserts == null) {
            return;
        }

        log.info("Inserted " + totalRawInserts + " raw metrics in total.");
        log.info("Inserted " + inserts.getCount() + " for interval starting at " + new Date(inserts.getTimestamp()));
        logInsertionTimeStats();
    }

    private void logInsertionTimeStats() {
        InsertionTimes insertionTimes = stats.getInsertionTimes();
        log.info("Summary of raw insertion time stats (milliseconds):\n" +
            "Min: " + insertionTimes.getMin() + "\n" +
            "Mean: " + insertionTimes.getMean() + "\n" +
            "Max: " + insertionTimes.getMax() + "\n" +
            "Standard Deviation: " + insertionTimes.getStandardDeviation());
    }

    public void logSummary() {
//        log.info("Summary of raw data inserts/minute\n" + stats.getAggregateSummary());
//        log.info("Summary of raw data insertion times in milliseconds\n" + stats.getRawDataInsertTimes());
    }
}
