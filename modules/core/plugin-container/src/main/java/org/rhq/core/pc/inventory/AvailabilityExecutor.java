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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * Runs a periodic scan for resource availability.
 *
 * @author Jay Shaughnessy
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
    private static final Random RANDOM = new Random();
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
    private AtomicBoolean sendChangesOnlyReport;
    private final Object lock = new Object();
    private int scanHistorySize = 1;
    private LinkedList<Scan> scanHistory = new LinkedList<Scan>();

    public AvailabilityExecutor(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
        this.sendChangesOnlyReport = new AtomicBoolean(false);
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
     * <p>This will return <code>null</code> if there is nothing committed to inventory. Having no committed inventory 
     * is rare.  There will be no committed inventory if this is a brand new agent whose inventory hasn't been committed 
     * yet or if the platform and all its children have been deleted (in which case the agent should be uninstalled, or 
     * the user will want to re-import the platform).
     * 
     * The report can be empty if there is nothing to report. This can happen for a changesOnly report when there
     * are no changes.</p>
     *
     * @return the report containing all the availabilities that need to be sent to the Server, or <code>null</code> if
     *         there is no inventory or nothing to report. The report can be empty
     *
     * @throws Exception if failed to create and prepare the report
     */
    @Nullable
    public AvailabilityReport call() throws Exception {
        AvailabilityReport availabilityReport;

        synchronized (lock) {
            if (inventoryManager.getPlatform().getInventoryStatus() != InventoryStatus.COMMITTED) {
                return null;
            }

            boolean changesOnly = sendChangesOnlyReport.get();
            availabilityReport = new AvailabilityReport(changesOnly, inventoryManager.getAgent().getName());

            // Follow up full reports with changesOnly reports 
            if (!changesOnly) {
                sendChangesOnlyReportNextTime();
            }

            long start = System.currentTimeMillis();
            Scan scan = new Scan(start, !changesOnly);

            // TODO back to debug
            log.info("Scan Starting: " + new Date(start));
            if (log.isDebugEnabled()) {
                //log.debug("Scan Starting: " + new Date(start));
            }

            try {
                checkInventory(inventoryManager.getPlatform(), availabilityReport, AvailabilityType.UP, false, scan);
            } catch (RuntimeException e) {
                if (Thread.interrupted()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Exception occurred during availability check, but this thread has been interrupted, "
                            + "so most likely the plugin container is shutting down: " + e);
                    }
                    return availabilityReport;
                }
            }

            scan.setEndTime(System.currentTimeMillis());

            // TODO back to debug
            log.info("Scan Ended   : " + new Date(scan.getEndTime()) + " : " + scan.toString());
            // Is this too much logging?
            if (log.isDebugEnabled()) {
                //log.debug("Scan Ended   : " + new Date(scan.getEndTime()) + " : " + scan.toString());
            }

            addScanHistory(scan);

            if (log.isDebugEnabled()) {
                long end = System.currentTimeMillis();
                ObjectOutputStream oos = null;
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
                    oos = new ObjectOutputStream(baos);
                    oos.writeObject(availabilityReport);
                    log.debug("Built availability report for [" + availabilityReport.getResourceAvailability().size()
                        + "] resources with a size of [" + baos.size() + "] bytes in [" + (end - start) + "]ms");
                } finally {
                    if (null != oos) {
                        oos.close();
                    }
                }
            }
        }

        return availabilityReport;
    }

    protected void checkInventory(Resource resource, AvailabilityReport availabilityReport,
        AvailabilityType parentAvailType, boolean isForced, Scan scan) {

        // Only report avail for committed Resources - that's all the Server cares about.
        if (resource.getId() == 0 || resource.getInventoryStatus() != InventoryStatus.COMMITTED) {
            return;
        }

        ResourceContainer resourceContainer = this.inventoryManager.getResourceContainer(resource);
        // Only report avail for synchronized Resources, otherwise the Server will likely know nothing of the Resource.
        if (resourceContainer == null
            || resourceContainer.getSynchronizationState() != ResourceContainer.SynchronizationState.SYNCHRONIZED) {
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

        ++scan.numResources;

        // See if this resource is scheduled for an avail check
        boolean checkAvail = false;
        boolean deferToParent = false;
        Long availabilityScheduleTime = resourceContainer.getAvailabilityScheduleTime();
        MeasurementScheduleRequest availScheduleRequest = resourceContainer.getAvailabilitySchedule();

        // if no avail check is scheduled or we're forcing the check, schedule the next check. Note that a forcedCheck
        // is "off-schedule" so we need to push out the next check.  
        if ((null == availabilityScheduleTime) || isForced) {
            // if there is no availability schedule (platform) then just perform the avail check
            // (note, platforms always return UP anyway).
            if (null == availScheduleRequest) {
                checkAvail = true;

            } else {
                // if the schedule is enabled then schedule the next avail check, else just defer to the parent type
                if (availScheduleRequest.isEnabled()) {
                    // Schedule the avail check at some time between now and (now + collectionInterval). By
                    // doing this random assignment for the first scheduled collection, we'll spread out the actual
                    // check times going forward. Do not check it on this pass (unless we're forced)
                    int interval = (int) availScheduleRequest.getInterval(); // intervals are short enough for safe cast
                    availabilityScheduleTime = scan.startTime + RANDOM.nextInt(interval + 1);
                    resourceContainer.setAvailabilityScheduleTime(availabilityScheduleTime);

                    ++scan.numScheduledRandomly;

                } else {
                    deferToParent = true;
                }
            }
        } else {
            // check avail if this resource scheduled time has been reached
            checkAvail = scan.startTime >= availabilityScheduleTime;

            if (checkAvail) {
                long interval = availScheduleRequest.getInterval(); // intervals are short enough for safe cast
                resourceContainer.setAvailabilityScheduleTime(scan.startTime + interval);
                ++scan.numPushedByInterval;
            }
        }

        // find out what the avail was the last time we checked it. this may be null
        Availability previous = this.inventoryManager.getAvailabilityIfKnown(resource);
        AvailabilityType current = (null == previous) ? AvailabilityType.UNKNOWN : previous.getAvailabilityType();

        // If the resource's parent is DOWN, the rules are that the resource and all of the parent's other
        // descendants, must also be DOWN. So, there's no need to even ask the resource component
        // for its current availability - its current avail is set to the parent avail type and that's that.
        // Otherwise, checkAvail as needed. 
        if (deferToParent || (AvailabilityType.DOWN == parentAvailType)) {
            current = parentAvailType;
            ++scan.numDeferToParent;

            // For the DOWN parent case it's unclear to me whether we should push out the avil check time of
            // the child.  For now, we'll leave it alone and let the next check happen according to the
            // schedule already established.

        } else {
            // regardless of whether the avail schedule is met, we still must check avail if isForce is true or if
            // it's a full report and we don't yet have an avail for the resource.
            if (!checkAvail && (isForced || (scan.isFull && null == previous))) {
                checkAvail = true;
            }

            if (checkAvail) {
                current = AvailabilityType.UNKNOWN;
                try {
                    ++scan.numGetAvailabilityCalls;

                    // if the component is started, ask what its current availability is as of right now;
                    // if it's not started, then assume it's down, and the next time we check,
                    // we'll see if it's started and check for real then - otherwise, keep assuming it's
                    // down (this is for the case when a plugin component can't start for whatever reason
                    // or is just slow to start)
                    if (resourceContainer.getResourceComponentState() == ResourceComponentState.STARTED) {
                        current = safeGetAvailability(resourceComponent);
                    } else {
                        this.inventoryManager.activateResource(resource, resourceContainer, false);
                        if (resourceContainer.getResourceComponentState() == ResourceComponentState.STARTED) {
                            current = safeGetAvailability(resourceComponent);
                        }
                    }
                } catch (Throwable t) {
                    ResourceError resourceError = new ResourceError(resource, ResourceErrorType.AVAILABILITY_CHECK,
                        t.getLocalizedMessage(), ThrowableUtil.getStackAsString(t), System.currentTimeMillis());
                    this.inventoryManager.sendResourceErrorToServer(resourceError);
                    if (log.isDebugEnabled()) {
                        if (t instanceof TimeoutException) {
                            // no need to log the stack trace for timeouts...
                            log.debug("Failed to collect availability on " + resource + " (call timed out)");
                        } else {
                            log.debug("Failed to collect availability on " + resource, t);
                        }
                    }
                }
                // Assume DOWN if for some reason the avail check failed 
                if (AvailabilityType.UNKNOWN == current) {
                    current = AvailabilityType.DOWN;
                }
            }
        }

        // Add the availability to the report if it changed from its previous state or if this is a full report.
        // Update the resource container only if the avail has changed.
        boolean availChanged = (null != current && AvailabilityType.UNKNOWN != current && (null == previous || current != previous
            .getAvailabilityType()));

        if (availChanged || scan.isFull) {
            Availability availability;

            if (availChanged) {
                ++scan.numAvailabilityChanges;

                availability = this.inventoryManager.updateAvailability(resource, current);

                // if the resource avail changed to UP then we must perform avail checks for all
                // children, to ensure their avails are up to date. Note that if it changed to NOT UP
                // then the children will just get the parent avail type and there is no avail check anyway.
                if (!isForced && (AvailabilityType.UP == current)) {
                    isForced = true;
                }
            } else {
                // avoid the overhead of updating the resource container, the avail type did not change
                availability = new Availability(resource, current);
            }

            // update the report
            availabilityReport.addAvailability(availability);
        }

        for (Resource child : resource.getChildResources()) {
            checkInventory(child, availabilityReport, current, isForced, scan);
        }

        return;
    }

    private AvailabilityType safeGetAvailability(AvailabilityFacet component) {
        AvailabilityType availType = component.getAvailability();
        switch (availType) {
        case UP:
            return AvailabilityType.UP;
        case DOWN:
            return AvailabilityType.DOWN;
        default:
            if (log.isDebugEnabled()) {
                log.debug("ResourceComponent " + component + " getAvailability() returned " + availType
                    + ". This is invalid and is being replaced with DOWN.");
            }
            return AvailabilityType.DOWN;
        }
    }

    /**
     * This tells the executor to send a full availability report the next time it sends one. Public-scoped so tests
     * can call this.
     */
    public void sendFullReportNextTime() {
        this.sendChangesOnlyReport.set(false);

        if (log.isTraceEnabled()) {
            log.trace("\nFull report requested by: " + getSmallStackTrace(new Throwable()));
        }
    }

    static private String getSmallStackTrace(Throwable t) {
        StringBuilder smallStack = new StringBuilder();

        StackTraceElement[] stack = (null == t) ? new Exception().getStackTrace() : t.getStackTrace();
        for (int i = 1; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if (ste.getClassName().startsWith("org.rhq")) {
                smallStack.append(ste.toString());
                smallStack.append("\n");
            }
        }
        return smallStack.toString();
    }

    /**
     * This tells the executor to send a minimal availability report the next time it sends one (that is, do not send a
     * resource availability if it hasn't changed from its last known state). Public-scoped so test code can call this.
     */
    public void sendChangesOnlyReportNextTime() {
        this.sendChangesOnlyReport.set(true);
    }

    public void addScanHistory(Scan scan) {
        synchronized (scanHistory) {
            if (scanHistory.size() == scanHistorySize) {
                scanHistory.removeLast();
            }
            scanHistory.push(scan);
        }
    }

    public List<Scan> getScanHistory() {
        synchronized (scanHistory) {
            List<Scan> result = new ArrayList<Scan>(scanHistory.size());
            result.addAll(scanHistory);
            return result;
        }
    }

    public Scan getMostRecentScanHistory() {
        synchronized (scanHistory) {
            return scanHistory.isEmpty() ? null : scanHistory.get(0);
        }
    }

    public void setScanHistorySize(int scanHistorySize) {
        synchronized (scanHistory) {
            if (scanHistorySize < 1) {
                return;
            }
            while (scanHistory.size() > scanHistorySize) {
                scanHistory.removeLast();
            }
            this.scanHistorySize = scanHistorySize;
        }
    }

    public static class Scan {
        private long startTime;
        private long endTime;
        private long runtime;

        private boolean isFull = false;
        private boolean isForced = false;

        int numResources = 0;
        int numGetAvailabilityCalls = 0;
        int numScheduledRandomly = 0;
        int numPushedByInterval = 0;
        int numAvailabilityChanges = 0;
        int numDeferToParent = 0;

        public Scan(long startTime, boolean isFull) {
            this.startTime = startTime;
            this.isFull = isFull;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
            this.runtime = endTime - startTime;
        }

        public long getRuntime() {
            return runtime;
        }

        public boolean isFull() {
            return isFull;
        }

        public boolean isForced() {
            return isForced;
        }

        public void setForced(boolean isForced) {
            this.isForced = isForced;
        }

        public int getNumResources() {
            return numResources;
        }

        public int getNumGetAvailabilityCalls() {
            return numGetAvailabilityCalls;
        }

        public int getNumScheduledRandomly() {
            return numScheduledRandomly;
        }

        public int getNumPushedByInterval() {
            return numPushedByInterval;
        }

        public int getNumAvailabilityChanges() {
            return numAvailabilityChanges;
        }

        public int getNumDeferToParent() {
            return numDeferToParent;
        }

        @Override
        public String toString() {
            return "Scan [startTime=" + startTime + ", endTime=" + endTime + ", runtime=" + runtime + ", isFull="
                + isFull + ", isForced=" + isForced + ", numResources=" + numResources + ", numGetAvailabilityCalls="
                + numGetAvailabilityCalls + ", numScheduledRandomly=" + numScheduledRandomly + ", numPushedByInterval="
                + numPushedByInterval + ", numAvailabilityChanges=" + numAvailabilityChanges + ", numDeferToParent="
                + numDeferToParent + "]";
        }
    }
}
