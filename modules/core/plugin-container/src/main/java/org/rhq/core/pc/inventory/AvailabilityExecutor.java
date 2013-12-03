/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.pc.inventory;

import static org.rhq.core.domain.measurement.AvailabilityType.DOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UNKNOWN;
import static org.rhq.core.domain.measurement.AvailabilityType.UP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Runs a periodic scan for resource availability.
 *
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class AvailabilityExecutor implements Runnable, Callable<AvailabilityReport> {

    private static final Log LOG = LogFactory.getLog(AvailabilityExecutor.class);

    protected final InventoryManager inventoryManager;

    private final AtomicBoolean sendChangesOnlyReport;
    private static final Random RANDOM = new Random();

    // NOTE: this is probably useless. The concurrency of the availability checks is mainly guarded by the size of the
    // availabilityThreadPoolExecutor in InventoryManager. While this lock object would prevent multiple avail checks
    // from running concurrently even if the size of the above executor was more than 1 (which it isn't), the problem
    // we'd then face would be that we use multiple instances of AvailabilityExecutor in InventoryManager:
    // availabilityExecutor field but also local instances in executeAvailabilityScanImmediately() and
    // getCurrentAvailability(). This means that the only thing preventing from the multiple availability checks
    // happening concurrently is the size of the thread pool and this object serves little purpose in that regard.
    private final Object lock = new Object();

    private int scanHistorySize = 1;
    private final LinkedList<Scan> scanHistory = new LinkedList<Scan>();

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
            LOG.warn("Availability report collection failed", e);
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

            startScan(inventoryManager.getPlatform(), availabilityReport, changesOnly);
        }

        return availabilityReport;
    }

    /**
     * This is an entry point for the recursive availability scan. I.e. usually this method is called with the platform
     * resource so that the avail scan is executed for the whole platform.
     *
     * @param scanRoot the resource to root the availability scan at
     * @param availabilityReport the availability report to fill
     * @param changesOnly whether to only report changes or produce a full report
     */
    protected void startScan(Resource scanRoot, AvailabilityReport availabilityReport, boolean changesOnly) {
        long start = System.currentTimeMillis();
        Scan scan = new Scan(start, !changesOnly);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scan Starting: " + new Date(start));
        }

        AvailabilityType parentAvailabilityType = null;

        //determine the parent availability
        Resource parent = scanRoot.getParentResource();
        while (parent != null) {
            Availability parentAvail = inventoryManager.getAvailabilityIfKnown(parent);
            if (parentAvail != null && parentAvail.getAvailabilityType() == DOWN) {
                parentAvailabilityType = DOWN;
                break;
            }

            parent = parent.getParentResource();
        }

        // we've gone up past the platform but didn't encounter a single down resource, hence the parent avail type
        // is to be considered UP (because it either truly is UP or is UNKNOWN as of now)
        if (parentAvailabilityType == null) {
            parentAvailabilityType = UP;
        }

        try {
            checkInventory(scanRoot, availabilityReport, parentAvailabilityType, false, scan);
        } catch (InterruptedException e) {
            LOG.debug("Availability check was interrupted", e);
            return;
        } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                if (Thread.interrupted()) {
                    LOG.debug("Exception occurred during availability check, but this thread has been interrupted, "
                        + "so most likely the plugin container is shutting down: " + e);
                } else {
                    LOG.debug("Exception occurred during availability check: " + e);
                }
            }
            return;
        }

        scan.setEndTime(System.currentTimeMillis());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Scan Ended   : " + new Date(scan.getEndTime()) + " : " + scan.toString());
        }

        addScanHistory(scan);

        if (LOG.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            ObjectOutputStream oos = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
                oos = new ObjectOutputStream(baos);
                oos.writeObject(availabilityReport);
                LOG.debug("Built availability report for [" + availabilityReport.getResourceAvailability().size()
                    + "] resources with a size of [" + baos.size() + "] bytes in [" + (end - start) + "]ms");
            } catch (IOException e) {
                LOG.debug("Failed to log the availability report details.", e);
            } finally {
                StreamUtil.safeClose(oos);
            }
        }
    }

    /**
      * Checks the availability of one resource and then its children.
      *
      * @throws InterruptedException if this checking thread was interrupted
      */
    protected void checkInventory(Resource resource, AvailabilityReport availabilityReport,
        AvailabilityType parentAvailType, boolean isForced, Scan scan) throws InterruptedException {

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

        // The avail proxy guarantees fast response time for an avail check
        AvailabilityFacet resourceAvailabilityProxy = resourceContainer.getAvailabilityProxy();

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
                if (LOG.isTraceEnabled()) {
                    LOG.trace("No availScheduleRequest for " + resource + ". checkAvail set to true");
                }
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

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Forced availabilityScheduleTime to " + new Date(availabilityScheduleTime) + " for "
                            + resource);
                    }
                    ++scan.numScheduledRandomly;

                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Deferred availability to parent for " + resource);
                    }
                    deferToParent = true;
                }
            }
        } else {
            // check avail if this resource scheduled time has been reached
            checkAvail = scan.startTime >= availabilityScheduleTime;

            if (checkAvail) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Scheduled time has been reached for " + resource);
                }
                long interval = availScheduleRequest.getInterval(); // intervals are short enough for safe cast
                resourceContainer.setAvailabilityScheduleTime(scan.startTime + interval);
                ++scan.numPushedByInterval;
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Scheduled time has not been reached for " + resource);
                }
            }
        }

        // find out what the avail was the last time we checked it. this may be null
        Availability previous = this.inventoryManager.getAvailabilityIfKnown(resource);
        AvailabilityType previousType = (null == previous) ? UNKNOWN : previous.getAvailabilityType();
        AvailabilityType current = null;

        // If the resource's parent is DOWN, the rules are that the resource and all of the parent's other
        // descendants, must also be DOWN. So, there's no need to even ask the resource component
        // for its current availability - its current avail is set to the parent avail type and that's that.
        // Otherwise, checkAvail as needed.
        if (deferToParent || (DOWN == parentAvailType)) {
            current = parentAvailType;
            ++scan.numDeferToParent;

            // For the DOWN parent case it's unclear to me whether we should push out the avail check time of
            // the child.  For now, we'll leave it alone and let the next check happen according to the
            // schedule already established.

            if (LOG.isTraceEnabled()) {
                LOG.trace("Gave parent availability " + parentAvailType + " to " + resource);
            }
        } else {
            // regardless of whether the avail schedule is met, we still must check avail if isForce is true or if
            // it's a full report and we don't yet have an avail for the resource.
            if (!checkAvail && (isForced || (scan.isFull && null == previous))) {
                checkAvail = true;
            }

            if (checkAvail) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Now checking availability for " + resource);
                }

                try {
                    ++scan.numGetAvailabilityCalls;

                    // if the component is started, ask what its current availability is as of right now;
                    // if it's not started, then assume it's down, and the next time we check,
                    // we'll see if it's started and check for real then - otherwise, keep assuming it's
                    // down (this is for the case when a plugin component can't start for whatever reason
                    // or is just slow to start)
                    if (resourceContainer.getResourceComponentState() == ResourceComponentState.STARTED) {
                        current = translate(resourceAvailabilityProxy.getAvailability(), previousType);

                    } else {
                        // try to start the component and then perform the avail check
                        this.inventoryManager.activateResource(resource, resourceContainer, false);
                        if (resourceContainer.getResourceComponentState() == ResourceComponentState.STARTED) {
                            current = translate(resourceAvailabilityProxy.getAvailability(), previousType);
                        } else {
                            current = DOWN;
                        }
                    }
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Current availability is " + current + " for " + resource);
                    }
                } catch (Throwable t) {
                    ResourceError resourceError = new ResourceError(resource, ResourceErrorType.AVAILABILITY_CHECK,
                        t.getLocalizedMessage(), ThrowableUtil.getStackAsString(t), System.currentTimeMillis());
                    this.inventoryManager.sendResourceErrorToServer(resourceError);
                    LOG.warn("Availability collection failed with exception on " + resource
                        + ", availability will be reported as " + DOWN.name(), t);
                    current = DOWN;
                }
            } else {
                current = previousType;
            }
        }

        // Add the availability to the report if it changed from its previous state or if this is a full report.
        // Update the resource container only if the avail has changed.
        boolean availChanged = (UNKNOWN != current && current != previousType);

        if (availChanged || scan.isFull) {
            Availability availability;

            if (availChanged) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Availability changed for " + resource);
                }
                ++scan.numAvailabilityChanges;

                availability = this.inventoryManager.updateAvailability(resource, current);

                // if the resource avail changed to UP then we must perform avail checks for all
                // children, to ensure their avails are up to date. Note that if it changed to NOT UP
                // then the children will just get the parent avail type and there is no avail check anyway.
                if (!isForced && (UP == current)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Forcing availability check for children of " + resource);
                    }
                    isForced = true;
                }
            } else {
                // avoid the overhead of updating the resource container, the avail type did not change
                availability = new Availability(resource, current);
            }

            // update the report
            availabilityReport.addAvailability(availability);
        }

        for (Resource child : this.inventoryManager.getContainerChildren(resource, resourceContainer)) {
            checkInventory(child, availabilityReport, current, isForced, scan);
        }

    }

    /**
     * Resources must report UP or DOWN, If current is UNKNOWN, return previously set avail, otherwise current.
     */
    private AvailabilityType translate(AvailabilityType current, AvailabilityType previousType) {
        return current == UNKNOWN ? previousType : current;
    }

    /**
     * This tells the executor to send a full availability report the next time it sends one. Public-scoped so tests
     * can call this.
     */
    public void sendFullReportNextTime() {
        this.sendChangesOnlyReport.set(false);

        if (LOG.isTraceEnabled()) {
            LOG.trace("\nFull report requested by: " + getSmallStackTrace(new Throwable()));
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
        private final long startTime;
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
