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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.server.metrics.MetricsServer;

/**
 * @author John Sanda
 */
public class MeasurementCollector implements Runnable {

    private final Log log = LogFactory.getLog(MeasurementCollector.class);

    private PriorityQueue<Schedule> queue;

    private MetricsServer metricsServer;

    private ReentrantLock queueLock;

    public void setQueue(PriorityQueue<Schedule> queue) {
        this.queue = queue;
    }

    public void setMetricsServer(MetricsServer metricsServer) {
        this.metricsServer = metricsServer;
    }

    public void setQueueLock(ReentrantLock queueLock) {
        this.queueLock = queueLock;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        int metricsCollected = 0;
        // TODO parameterize threshold
        int threshold = 500;
        try {
            log.info("Starting metrics collections...");
            Set<Schedule> schedules = new HashSet<Schedule>();
            try {
                queueLock.lock();
                Schedule first = queue.peek();
                if (first != null && first.getNextCollection() <= System.currentTimeMillis()) {
                    Schedule next = first;
                    while (next != null && next.getNextCollection() == first.getNextCollection() &&
                        schedules.size() < threshold) {
                        schedules.add(queue.poll());
                        next = queue.peek();
                    }
                }
            } finally {
                queueLock.unlock();
            }

            if (schedules.isEmpty()) {
                log.debug("No schedules are ready for collections.");
                return;
            }
            log.debug("There are " + schedules.size() + " schedules ready for collection.");

            Set<MeasurementDataNumeric> data = new HashSet<MeasurementDataNumeric>(schedules.size());
            for (Schedule schedule : schedules) {
                data.add(new MeasurementDataNumeric(schedule.getNextCollection(), schedule.getId(),
                    schedule.getNextValue()));
                schedule.updateCollection();
            }
            metricsCollected = data.size();
            metricsServer.addNumericData(data);

            try {
                queueLock.lock();
                for (Schedule schedule : schedules) {
                    queue.offer(schedule);
                }
            } finally {
                queueLock.unlock();
            }
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("Finished collecting and storing " + metricsCollected + " raw metric in " +
                (endTime - startTime) + " ms.");
        }
    }
}
