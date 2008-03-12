/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.pc.inventory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.clientapi.agent.PluginContainerException;

/**
 * Runs a periodic scan for resource availability.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli  
 */
public class AvailabilityExecutor implements Runnable, Callable<AvailabilityReport> {
    private static final int GET_AVAILABILITY_TIMEOUT = 5 * 1000; // 5 seconds

    private final Log log = LogFactory.getLog(AvailabilityExecutor.class);

    private InventoryManager inventoryManager;
    private AtomicBoolean sendChangedOnlyReport;
    private final Object lock = new Object();
    
    public AvailabilityExecutor(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
        this.sendChangedOnlyReport = new AtomicBoolean(false);
    }

    public void run() {
        try {
            synchronized (lock) {
                AvailabilityReport report = call();
                inventoryManager.handleReport(report);
            }
        } catch (Exception e) {
            log.warn("Availability report collection failed", e);
        }
    }

    /**
     * Returns the availability report that should be sent to the server.
     *
     * <p>This will return <code>null</code> if there is no committed inventory and thus no availability reports should
     * be sent. This will rarely happen - only if this is a brand new agent whose inventory hasn't been committed yet or
     * if the platform and all its children have been deleted (in which case the agent should be uninstalled or the user
     * will want to re-import the platform). In either case, it will be in rare, corner cases that this will ever return
     * null and the condition that caused the null should eventually go away.</p>
     *
     * @return the report containing all the availabilities that need to be sent to the server
     *
     * @throws Exception if failed to create and prepare the report
     */
    public AvailabilityReport call() throws Exception {
        log.debug("Running Availability Scan...");

        AvailabilityReport availabilityReport;

        synchronized (lock) {
            availabilityReport = new AvailabilityReport(sendChangedOnlyReport.get(), inventoryManager.getAgent()
                .getName());

            if (inventoryManager.getPlatform().getInventoryStatus() != InventoryStatus.COMMITTED) {
                return null;
            }

            long start = System.currentTimeMillis();

            checkInventory(inventoryManager.getPlatform(), availabilityReport,
                availabilityReport.isChangesOnlyReport(), true);

            // In enterprise mode, the server will need at least one resource so it can derive what agent
            // is sending this report.  If the report is empty (meaning, nothing has changed since the last
            // availability check), let's add the platform record to the report
            if (availabilityReport.getResourceAvailability().size() == 0) {
                checkInventory(inventoryManager.getPlatform(), availabilityReport, false, false);
            }

            // if we have non-platform resources in the report, the agent has had
            // resources imported. So next time, only send changed only reports.
            if (availabilityReport.getResourceAvailability().size() > 1) {
                sendChangedOnlyReportNextTime();
            }

            long end = System.currentTimeMillis();

            if (log.isDebugEnabled()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(availabilityReport);
                log.debug("Built availability report for [" + availabilityReport.getResourceAvailability().size()
                    + "] resources with a size of [" + baos.size() + "] bytes in [" + (end - start) + "]ms");
            }
        }

        return availabilityReport;
    }

    private void checkInventory(Resource resource, AvailabilityReport availabilityReport, boolean reportChangesOnly,
        boolean checkChildren) {
        ResourceContainer resourceContainer = this.inventoryManager.getResourceContainer(resource);
        ResourceComponent resourceComponent = null;
        if (resourceContainer != null) {
            try {
                resourceComponent = resourceContainer.createResourceComponentProxy(ResourceComponent.class, FacetLockType.NONE, GET_AVAILABILITY_TIMEOUT, true, false);
            }
            catch (PluginContainerException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not create resource component proxy for " + resource, e);
                } else {
                    log.warn("Could not create resource component proxy for " + resource);
                }
            }
        }

        if (resourceComponent != null) {
            AvailabilityType current = null;

            // Only report availability for committed resources; don't bother with new, ignored or deleted resources.
            if (resource.getInventoryStatus() == InventoryStatus.COMMITTED) {
                try {
                    // this is what we think the availability is, based on a previous check
                    Availability previous = this.inventoryManager.getAvailabilityIfKnown(resource);

                    // if the component is started, ask what its current availability is as of right now
                    // if its not started, then assume its down, our next time checking we'll see if its
                    // started and check for real then - otherwise, keep assuming its down (this is for
                    // the case when a plugin component can't start for whatever reason or is just slow
                    // to start)
                    if (resourceContainer.getResourceComponentState() == ResourceComponentState.STARTED) {
                        current = resourceComponent.getAvailability();
                    }

                    if (current == null) {
                        current = AvailabilityType.DOWN;
                    }

                    if (resourceContainer.getSynchronizationState() == ResourceContainer.SynchronizationState.SYNCHRONIZED) {
                        Availability availability = this.inventoryManager.updateAvailability(resource, current);

                        // only add the availability to the report if it changed from its previous state
                        // if this is the first time we've been executed, reportChangesOnly will be false
                        // and we will send a full report as our very first report
                        if ((previous == null) || (previous.getAvailabilityType() != current) || !reportChangesOnly) {
                            availabilityReport.addAvailability(availability);
                        }
                    }
                } catch (Throwable e) {
                    // TODO GH: Put errors in report
                    if (log.isDebugEnabled())
                        if (e instanceof TimeoutException)
                            // no need to log the stack trace for timeouts...
                            log.debug("Failed to collect availability on resource " + resource, e);
                        else
                            log.debug("Failed to collect availability on resource " + resource + " (call timed out)");
                }
            }

            if (resourceComponent != null && checkChildren) {
                // we used to check to see if (current == AvailabilityType.UP) here,  however, this causes a problem;
                // if a resource and all of its children were previously UP, and the resource itself goes down (or,
                // a user changes the resource's plugin configuration such that it makes it invalid and cannot connect
                // to the resource), then we used to only report the resource was DOWN - but because of the if-check,
                // we never reported the children being DOWN (they always remained with a green light; whether or not
                // they really were up - we never knew the true status because if the parent wasn't UP, we never bothered
                // checking the children).
                // TODO: should we still check current != null (that is, UP or DOWN)?
                // I leave this check in here for now - this means we never check children unless the resource is committed;
                // if resource has an inventory status of anything other than committed, we never put its children's
                // availabilities in the report
                if (current != null) {
                    for (Resource child : resource.getChildResources()) {
                        checkInventory(child, availabilityReport, reportChangesOnly, true);
                    }
                }
            }
        }

        return;
    }

    /**
     * This tells the executor to send a full availability report the next time it sends one. Package-scoped so the
     * InventoryManager can call this.
     */
    void sendFullReportNextTime() {
        this.sendChangedOnlyReport.set(false);
    }

    /**
     * This tells the executor to send a minimal availability report the next time it sends one (that is, do not send a
     * resource availability if it hasn't changed from its last known state). Package-scoped so the InventoryManager can
     * call this.
     */
    void sendChangedOnlyReportNextTime() {
        this.sendChangedOnlyReport.set(true);
    }
}