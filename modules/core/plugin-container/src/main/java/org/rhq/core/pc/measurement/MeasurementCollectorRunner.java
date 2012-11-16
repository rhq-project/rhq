/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.measurement;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.exception.ThrowableUtil;

/**
* Executes the collection of measurements. Every call results in one new batch of measurements collected. Each batch is
* limited to a single resource and the measurements that are due to be collected at that time.
*
* @author Greg Hinkle
*/
public class MeasurementCollectorRunner implements Callable<MeasurementReport>, Runnable {
    private Log log = LogFactory.getLog(MeasurementCollectorRunner.class);

    private MeasurementManager measurementManager;

    // this is only kept when in debug mode to help figure out which metrics are slowing things down. 
    private ScheduleHistory scheduleHistory = new ScheduleHistory();

    public MeasurementCollectorRunner(MeasurementManager measurementManager) {
        this.measurementManager = measurementManager;
    }

    public MeasurementReport call() {
        MeasurementReport report = null;
        try {
            this.measurementManager.getLock().readLock().lock();

            report = this.measurementManager.getActiveReport();
            long start = System.currentTimeMillis();

            InventoryManager im = PluginContainer.getInstance().getInventoryManager();

            Set<ScheduledMeasurementInfo> requests = this.measurementManager.getNextScheduledSet();

            if (requests != null) {
                ScheduledMeasurementInfo next = requests.iterator().next();

                if ((System.currentTimeMillis() - 30000L) > next.getNextCollection()) {
                    this.measurementManager.incrementLateCollections(requests.size());
                    if (log.isDebugEnabled()) {
                        log.debug("Measurement collection is falling behind... Missed requested time by ["
                            + (System.currentTimeMillis() - requests.iterator().next().getNextCollection()) + "ms]");

                        if (!scheduleHistory.isEmpty()) {
                            log.debug("The most recent measurement requests prior to this detected delay: "
                                + scheduleHistory);
                        }
                    }

                    // BZ 834019 - reschedule these requests for the future, and away from the set of requests on this schedule
                    this.measurementManager.rescheduleLateCollections(requests);
                    return report;
                }

                Integer resourceId = next.getResourceId();
                ResourceContainer container = im.getResourceContainer(resourceId);
                if (container.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED
                    || container.getAvailability() == null
                    || container.getAvailability().getAvailabilityType() == AvailabilityType.DOWN) {
                    // Don't collect metrics for resources that are down
                    if (log.isDebugEnabled()) {
                        log.debug("Measurements not collected for inactive resource component: "
                            + container.getResource());
                    }
                } else {
                    MeasurementFacet measurementComponent = ComponentUtil
                        .getComponent(resourceId, MeasurementFacet.class, FacetLockType.READ,
                            MeasurementManager.FACET_METHOD_TIMEOUT, true, true);

                    if (log.isDebugEnabled()) {
                        scheduleHistory.addRequests(requests);
                    }

                    getValues(measurementComponent, report, requests, container.getResource());
                }

                this.measurementManager.reschedule(requests);

                report.incrementCollectionTime(System.currentTimeMillis() - start);
            }
        } catch (Throwable t) {
            log.error("Failed to run measurement collection", t);
        } finally {
            this.measurementManager.getLock().readLock().unlock();
        }

        return report;
    }

    private void getValues(MeasurementFacet measurementComponent, MeasurementReport report,
        Set<? extends MeasurementScheduleRequest> requests, Resource resource) {
        try {
            long start = System.currentTimeMillis();
            measurementComponent.getValues(report, Collections.unmodifiableSet(requests));
            long duration = (System.currentTimeMillis() - start);
            if (duration > 2000L || log.isTraceEnabled()) {
                String message = "[PERF] Collection of measurements for [" + resource + "] (component=["
                    + measurementComponent + "]) took [" + duration + "]ms";
                if (log.isDebugEnabled()) {
                    message += " for requests: " + requests;
                }
                log.info(message);
            }
        } catch (Throwable t) {
            this.measurementManager.incrementFailedCollections(requests.size());
            if (log.isDebugEnabled()) {
                log.warn("Failure to collect measurement data for " + resource + ", requests=" + requests
                    + ", report.size()=" + report.getDataCount(), t);
            } else {
                log.warn("Failure to collect measurement data for " + resource + " - cause: "
                    + ThrowableUtil.getAllMessages(t));
            }
        }
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.error("Could not get measurement report.", e);
        }
    }

    private static class ScheduleHistory extends ArrayDeque<String> {
        private static final long serialVersionUID = 1L;
        private static final int HISTORY_SIZE;
        static {
            int historySize;
            try {
                historySize = Integer.parseInt(System.getProperty(
                    "rhq.agent.plugins.measurement-schedule.history.size", "500"));
            } catch (Throwable t) {
                historySize = 500;
            }
            HISTORY_SIZE = historySize;
        }

        private void makeSpace() {
            if (size() == HISTORY_SIZE) {
                removeLast();
            }
        }

        public void addRequests(Set<ScheduledMeasurementInfo> requests) {
            String now = new Date().toString() + " ";

            for (ScheduledMeasurementInfo request : requests) {
                push(now + request.toString());
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            while (!isEmpty()) {
                sb.append("\n  ");
                sb.append(pop());
            }
            return sb.toString();
        }

        @Override
        public void push(String e) {
            makeSpace();
            super.push(e);
        }

        @Override
        public boolean add(String e) {
            makeSpace();
            return super.add(e);
        }

        @Override
        public void addFirst(String e) {
            makeSpace();
            super.addFirst(e);
        }

        @Override
        public void addLast(String e) {
            makeSpace();
            super.addLast(e);
        }
    }
}