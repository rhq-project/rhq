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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.DateTimeService;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.RawDataInsertedCallback;

/**
 * @author John Sanda
 */
public class MeasurementCollector implements Runnable {

    private final Log log = LogFactory.getLog(MeasurementCollector.class);

    private MetricsServer metricsServer;

    private int batchSize;

    private int startingScheduleId;

    private Metrics metrics;

    private DateTimeService dateTimeService;

    public MeasurementCollector(int batchSize, int startingScheduleId, Metrics metrics, MetricsServer metricsServer,
        DateTimeService dateTimeService) {
        this.batchSize = batchSize;
        this.startingScheduleId = startingScheduleId;
        this.metrics = metrics;
        this.metricsServer = metricsServer;
        this.dateTimeService = dateTimeService;
    }

    private Set<MeasurementDataNumeric> generateData() {
        Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>(batchSize);
        long timestamp = dateTimeService.nowInMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < batchSize; ++i) {
            data.add(new MeasurementDataNumeric(timestamp, startingScheduleId + i, random.nextDouble()));
        }

        return data;
    }

    @Override
    public void run() {
        final Timer.Context context = metrics.batchInsertTime.time();
        final Stopwatch stopwatch = new Stopwatch().start();
        metricsServer.addNumericData(generateData(), new RawDataInsertedCallback() {
            @Override
            public void onFinish() {
                stopwatch.stop();
                log.info("Finished inserting raw data in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
                context.stop();
            }

            @Override
            public void onSuccess(Void avoid) {
                metrics.rawInserts.mark();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to insert raw data", t);
            }
        });
    }

}
