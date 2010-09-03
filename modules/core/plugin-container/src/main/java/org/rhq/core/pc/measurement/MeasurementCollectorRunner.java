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

import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.AvailabilityType;
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
                if ((System.currentTimeMillis() - 30000L) > requests.iterator().next().getNextCollection()) {
                    this.measurementManager.incrementLateCollections(requests.size());
                    log.debug("Measurement collection is falling behind... Missed requested time by ["
                        + (System.currentTimeMillis() - requests.iterator().next().getNextCollection()) + "ms]");

                    this.measurementManager.reschedule(requests);
                    return report;
                }

                Integer resourceId = requests.iterator().next().getResourceId();
                ResourceContainer container = im.getResourceContainer(resourceId);
                if (container.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED
                    || container.getAvailability() == null
                    || container.getAvailability().getAvailabilityType() == AvailabilityType.DOWN) {
                    // Don't collect metrics for resources that are down
                    if (log.isDebugEnabled()) {
                        log.debug("Measurements not collected for inactive resource component: " + container.getResource());
                    }
                } else {
                    MeasurementFacet measurementComponent = ComponentUtil.getComponent(resourceId, MeasurementFacet.class,
                        FacetLockType.READ, MeasurementManager.FACET_METHOD_TIMEOUT, true, true);

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
            measurementComponent.getValues(report, (Set<MeasurementScheduleRequest>) requests);
            long duration = (System.currentTimeMillis() - start);
            if (duration > 2000) {
                log.info("[PERF] Collection of measurements for [" + resource + "] (component=[" + measurementComponent
                    + "]) took [" + duration + "]ms");
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
}