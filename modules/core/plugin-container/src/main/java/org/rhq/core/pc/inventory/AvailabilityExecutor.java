/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.pc.inventory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;

/**
 * Runs a periodic scan for resource availability.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class AvailabilityExecutor implements Runnable, Callable<AvailabilityReport> {
    // the get-availability-timeout will rarely, if ever, want to be overridden. It will default to be 5 seconds
    // and that's what it probably should always be. However, there may be a rare instance where someone wants
    // to give this availability executor a bit more time to wait for the resource's availability response
    // and is willing to live with the possible consequences (that being, delayed avail reports and possibly
    // false-down alerts getting triggered). Rather than changing this timeout, people should be using
    // the asynchronous-availability-check capabilities that are exposed to the plugins. Because we do not
    // want to encourage people from changing this, we do not expose this "backdoor" system property as a
    // standard plugin configuration setting/agent preference - if someone wants to do this, they must
    // explicitly pass in -D to the JVM running the plugin container.
    private static final int GET_AVAILABILITY_TIMEOUT;
    static {
        int timeout;
        try {
            timeout = Integer.parseInt(System.getProperty("rhq.agent.plugins.availability-scan.timeout", "5000"));
        } catch (Throwable t) {
            timeout = 5000;
        }
        GET_AVAILABILITY_TIMEOUT = timeout;
    }

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
     * Returns the availability report that should be sent to the Server.
     *
     * <p>This will return <code>null</code> if there is no committed inventory and thus no availability reports should
     * be sent. This will rarely happen - only if this is a brand new agent whose inventory hasn't been committed yet or
     * if the platform and all its children have been deleted (in which case the agent should be uninstalled or the user
     * will want to re-import the platform). In either case, it will be in rare, corner cases that this will ever return
     * null, and the condition that caused the null should eventually go away.</p>
     *
     * @return the report containing all the availabilities that need to be sent to the Server
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
                availabilityReport.isChangesOnlyReport(), true, false);

            // In enterprise mode, the server will need at least one resource so it can derive what agent
            // is sending this report.  If the report is empty (meaning, nothing has changed since the last
            // availability check), let's add the platform record to the report
            if (availabilityReport.getResourceAvailability().isEmpty()) {
                checkInventory(inventoryManager.getPlatform(), availabilityReport, false, false, false);
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
                                boolean checkChildren, boolean parentIsDown) {
        // Only report avail for committed Resources - that's all the Server cares about.
        if (resource.getInventoryStatus() != InventoryStatus.COMMITTED) {
            return;
        }
        ResourceContainer resourceContainer = this.inventoryManager.getResourceContainer(resource);
        // Only report avail for synchronized Resources, otherwise chances are the Server will know nothing of the
        // Resource.
        if (resourceContainer == null ||
            resourceContainer.getSynchronizationState() != ResourceContainer.SynchronizationState.SYNCHRONIZED) {
            return;
        }
        AvailabilityFacet resourceComponent;
        try {
            resourceComponent = resourceContainer.createResourceComponentProxy(AvailabilityFacet.class,
                FacetLockType.NONE, GET_AVAILABILITY_TIMEOUT, true, false);
        } catch (PluginContainerException e) {
            // TODO (ips): Why aren't we logging this as an error?
            if (log.isDebugEnabled()) {
                log.debug("Could not create resource component proxy for " + resource + ".", e);
            }
            return;
        }

        AvailabilityType current = null;

        // Find out what the avail was the last time we checked it.
        Availability previous = this.inventoryManager.getAvailabilityIfKnown(resource);

        if (parentIsDown) {
            // If the resource's parent is down, the rules are that the resource and all of the parent's other
            // descendants, must also be down, so there's no need to even ask the resource component for its
            // current availability - its current avail is DOWN and that's that.
            current = AvailabilityType.DOWN;
        } else {
            try {
                // if the component is started, ask what its current availability is as of right now;
                // if it's not started, then assume it's down, and the next time we check,
                // we'll see if it's started and check for real then - otherwise, keep assuming it's
                // down (this is for the case when a plugin component can't start for whatever reason
                // or is just slow to start)
                if (resourceContainer.getResourceComponentState() == ResourceComponentState.STARTED) {
                    current = resourceComponent.getAvailability();
                } else {
                    this.inventoryManager.activateResource(resource, resourceContainer, false);
                    if (resourceContainer.getResourceComponentState() == ResourceComponentState.STARTED) {
                        current = resourceComponent.getAvailability();
                    }
                }
            } catch (Throwable t) {
                ResourceError resourceError = new ResourceError(resource, ResourceErrorType.AVAILABILITY_CHECK, t,
                    System.currentTimeMillis());
                this.inventoryManager.sendResourceErrorToServer(resourceError);
                // TODO GH: Put errors in report, rather than sending them to the Server separately.
                if (log.isDebugEnabled()) {
                    if (t instanceof TimeoutException) {
                        // no need to log the stack trace for timeouts...
                        log.debug("Failed to collect availability on " + resource + " (call timed out)");
                    } else {
                        log.debug("Failed to collect availability on " + resource, t);
                    }
                }
            }
            if (current == null) {
                current = AvailabilityType.DOWN;
            }
        }

        // Only add the availability to the report if it changed from its previous state.
        // If this is the first time we've been executed, reportChangesOnly will be false,
        // and we will send a full report as our very first report.
        if ((previous == null) || (previous.getAvailabilityType() != current) || !reportChangesOnly) {
            Availability availability = this.inventoryManager.updateAvailability(resource, current);
            availabilityReport.addAvailability(availability);
        }

        if (checkChildren) {
            // Wrap in a fresh HashSet to avoid ConcurrentModificationExceptions.
            Set<Resource> children = new HashSet<Resource>(resource.getChildResources());
            for (Resource child : children) {
                checkInventory(child, availabilityReport, reportChangesOnly, true, current == AvailabilityType.DOWN);
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
