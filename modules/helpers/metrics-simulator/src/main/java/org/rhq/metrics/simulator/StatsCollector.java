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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Duration;

import org.rhq.metrics.simulator.stats.Stats;

/**
 * @author John Sanda
 */
public class StatsCollector implements Runnable {

    private final Log log = LogFactory.getLog(StatsCollector.class);

    private Stats stats;

    private long previousRawInsertTotal;

    private long lastRunTimestamp;

    private SimpleDateFormat dateFormat;

    public StatsCollector(Stats stats) {
        this.stats = stats;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long totalRawInserts = stats.getTotalRawInserts();

        // inserts will be null on the first run
        if (lastRunTimestamp == 0) {
            lastRunTimestamp = now;
            previousRawInsertTotal = totalRawInserts;
            return;
        }

        long lastRawInsertsCount = totalRawInserts - previousRawInsertTotal;
        Duration duration = new Duration(lastRunTimestamp, now);
        stats.addRawInsertsPerMinute(lastRawInsertsCount);

        StringBuilder data = new StringBuilder("Statistics Report\n")
            .append("------------------------------------------------------------------------------------\n")
            .append("Sampling period start time: " + dateFormat.format(new Date(lastRunTimestamp))).append("\n")
            .append("Sampling period length: " + duration.toStandardSeconds().getSeconds()).append(" seconds\n")
            .append("Total raw metrics inserted: ").append(totalRawInserts).append("\n")
            .append("Raw inserts this sampling period: ").append(lastRawInsertsCount).append("\n")
            .append(stats.getRawInsertsPerMinute()).append("\n")
            .append(stats.getRawInsertTimes()).append("\n")
            .append("------------------------------------------------------------------------------------");

        log.info(data);

        lastRunTimestamp = now;
        previousRawInsertTotal = totalRawInserts;
    }

    public void reportSummaryStats() {
        log.info("Reporting statistics for entire simulation run.");
        log.info(stats.getRawInsertsPerMinute());
    }

}
