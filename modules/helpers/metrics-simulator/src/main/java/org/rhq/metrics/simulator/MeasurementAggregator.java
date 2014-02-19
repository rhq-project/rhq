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

import java.util.concurrent.ExecutorService;

import com.codahale.metrics.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.server.metrics.MetricsServer;

/**
 * @author John Sanda
 */
public class MeasurementAggregator implements Runnable {

    private final Log log = LogFactory.getLog(MeasurementAggregator.class);

    private MetricsServer metricsServer;

    private Metrics metrics;

    private ExecutorService aggregationQueue;

    private ShutdownManager shutdownManager;

    private int numSchedules;

    public MeasurementAggregator(MetricsServer metricsServer, ShutdownManager shutdownManager, Metrics metrics,
        ExecutorService aggregationQueue, int numSchedules) {
        this.metricsServer = metricsServer;
        this.shutdownManager = shutdownManager;
        this.metrics = metrics;
        this.aggregationQueue = aggregationQueue;
        this.numSchedules = numSchedules;
    }

    public void run() {
        aggregationQueue.submit(new Runnable() {
            @Override
            public void run() {
                Timer.Context context = metrics.totalAggregationTime.time();
                long start = System.currentTimeMillis();
                try {
                    log.info("Starting metrics aggregation");
                    metricsServer.calculateAggregates(0, numSchedules);
                } catch (Exception e) {
                    log.error("An error occurred while trying to perform aggregation", e);
                    log.error("Requesting simulation shutdown...");
                    shutdownManager.shutdown(1);
                } finally {
                    context.stop();
                    log.info("Finished metrics aggregation in " + (System.currentTimeMillis() - start) + " ms");
                    metrics.totalAggregationRuns.inc();
                }
            }
        });
    }
}
