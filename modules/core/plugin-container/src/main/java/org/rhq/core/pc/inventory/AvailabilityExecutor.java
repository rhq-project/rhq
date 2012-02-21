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
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    // TODO: remove these 
    int totalResources;
    int numAvailChecks;
    int numRandomSched;
    int numPushed;
    int numAvailChanges;
    int numDeferToParent;

    public AvailabilityExecutor(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
        this.sendChangesOnlyReport = new AtomicBoolean(false);
    }

    public void run() {
        try {
            synchronized (lock) {
                System.out.println("\n------------------> JOB KICKING OFF AVAIL SCAN at "
                    + DateFormat.getTimeInstance().format(new Date(System.currentTimeMillis())));

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
    public AvailabilityReport call() throws Exception {
        log.debug("Running Availability Scan...");

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

            if (log.isDebugEnabled()) {
                System.out.println("------> (0) NEW AVAIL SCAN (" + (changesOnly ? "Changes Only" : "Full") + " at "
                    + DateFormat.getTimeInstance().format(new Date(start)));

                totalResources = 0;
                numAvailChecks = 0;
                numRandomSched = 0;
                numPushed = 0;
                numAvailChanges = 0;
                numDeferToParent = 0;
            }

            checkInventory(inventoryManager.getPlatform(), availabilityReport, changesOnly, true, AvailabilityType.UP,
                start, false);

            if (log.isDebugEnabled()) {

                System.out.println("------> (0) DONE (total=" + totalResources + ", numReported="
                    + availabilityReport.getResourceAvailability().size() + ", numChecks=" + numAvailChecks
                    + ", numChanges=" + numAvailChanges + ", numDefer=" + numDeferToParent + ", numRandom="
                    + numRandomSched + ", numPushed=" + numPushed);
            }

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

    private String res(Resource res) {
        return "[" + res.getId() + ", " + res.getName() + "]";
    }

    protected void checkInventory(Resource resource, AvailabilityReport availabilityReport, boolean reportChangesOnly,
        boolean checkChildren, AvailabilityType parentAvailType, long checkInventoryTime, boolean forceCheck) {

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

        ++totalResources;

        // See if this resource is scheduled for an avail check
        boolean checkAvail = false;
        boolean deferToParent = false;
        Long availabilityScheduleTime = resourceContainer.getAvailabilityScheduleTime();
        MeasurementScheduleRequest availScheduleRequest = resourceContainer.getAvailabilitySchedule();

        // if no avail check is scheduled or we're forcing the check, schedule the next check. Note that a forcedCheck
        // is "off-schedule" so we need to push out the next check.  
        if ((null == availabilityScheduleTime) || forceCheck) {
            // if there is no availability schedule for the resource (not yet set, or maybe it's a platform) then
            // always perform the avail check (note, platforms always return UP anyway).
            if (null == availScheduleRequest) {
                // System.out.println("------> (1) setting checkAvail true: no schedule request for " + res(resource));
                checkAvail = true;

            } else {
                // if the schedule is enabled then schedule the next avail check, else just defer to the parent type
                if (availScheduleRequest.isEnabled()) {
                    // Schedule the avail check at some time between now and (now + collectionInterval). By
                    // doing this random assignment for the first scheduled collection, we'll spread out the actual
                    // check times going forward. Do not check it on this pass (unless we're forced)
                    int interval = (int) availScheduleRequest.getInterval(); // intervals are short enough for safe cast
                    availabilityScheduleTime = checkInventoryTime + RANDOM.nextInt(interval + 1);
                    resourceContainer.setAvailabilityScheduleTime(availabilityScheduleTime);

                    ++numRandomSched;
                    //System.out.println("------> (2) scheduled avail check at "
                    //    + DateFormat.getTimeInstance().format(new Date(availabilityScheduleTime)) + " for "
                    //    + res(resource));

                } else {
                    deferToParent = true;
                    ++numDeferToParent;
                }
            }
        } else {
            // check avail if this resource scheduled time has been reached
            checkAvail = checkInventoryTime >= availabilityScheduleTime;

            if (checkAvail) {
                long interval = availScheduleRequest.getInterval(); // intervals are short enough for safe cast
                resourceContainer.setAvailabilityScheduleTime(checkInventoryTime + interval);
                ++numPushed;
                //System.out.println("------> (4) setting checkAvail true, moving check from "
                //    + DateFormat.getTimeInstance().format(new Date(availabilityScheduleTime)) + " to "
                //    + DateFormat.getTimeInstance().format(new Date(checkInventoryTime + interval)) + " for "
                //    + res(resource));
            }
        }

        // find out what the avail was the last time we checked it. this may be null
        Availability previous = this.inventoryManager.getAvailabilityIfKnown(resource);
        AvailabilityType current = (null == previous) ? null : previous.getAvailabilityType();

        // regardless of whether the avail schedule is met, we still must check avail if forceCheck is true or if
        // it's a full report and we don't yet have an avail for the resource.
        if (!checkAvail && (forceCheck || (!reportChangesOnly && null == previous))) {
            checkAvail = true;

            //System.out.println("------> (5) setting checkAvail true forceCheck=" + forceCheck + ", fullReport="
            //    + !reportChangesOnly + ", previous=" + previous + " for " + res(resource));
        }

        if (checkAvail) {
            if (deferToParent || (AvailabilityType.UP != parentAvailType)) {
                // If the resource's parent is not up, the rules are that the resource and all of the parent's other
                // descendants, must also be of the parent avail type, so there's no need to even ask the resource component
                // for its current availability - its current avail is set to the parent avail type and that's that.
                current = parentAvailType;

            } else {
                current = null;
                try {
                    ++numAvailChecks;

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
                    ResourceError resourceError = new ResourceError(resource, ResourceErrorType.AVAILABILITY_CHECK,
                        t.getLocalizedMessage(), ThrowableUtil.getStackAsString(t), System.currentTimeMillis());
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
        }

        // Add the availability to the report if it changed from its previous state or if this is a full report.
        // Update the resource container only if the avail has changed.
        boolean availChanged = (previous == null) || (previous.getAvailabilityType() != current);

        if (availChanged || !reportChangesOnly) {
            Availability availability = null;

            if (availChanged) {
                ++numAvailChanges;
                // System.out.println("------> (6) injecting new avail=" + current + " for " + res(resource));

                availability = this.inventoryManager.updateAvailability(resource, current);

                // if the resource avail changed to UP then we must perform avail checks for all
                // children, to ensure their avails are up to date. Note that if it changed to NOT UP
                // then the children will just get the parent avail type and there is no avail check anyway.

                if (!forceCheck && (AvailabilityType.UP == current)) {
                    forceCheck = true;

                    //System.out.println("------> (7) setting forceCheck=true for " + res(resource));
                }
            } else {
                // avoid the overhead of updating the resource container, the avail type did not change
                availability = new Availability(resource, new Date(), current);
            }

            // update the report
            availabilityReport.addAvailability(availability);
        }

        if (checkChildren) {
            for (Resource child : resource.getChildResources()) {
                checkInventory(child, availabilityReport, reportChangesOnly, true, current, checkInventoryTime,
                    forceCheck);
            }
        }

        return;
    }

    /**
     * This tells the executor to send a full availability report the next time it sends one. Package-scoped so the
     * InventoryManager can call this.
     */
    void sendFullReportNextTime() {
        this.sendChangesOnlyReport.set(false);

        if (log.isDebugEnabled()) {
            System.out.println("\nFull report requested by: " + getSmallStackTrace(new Throwable()));
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
     * resource availability if it hasn't changed from its last known state). Package-scoped so the InventoryManager can
     * call this.
     */
    void sendChangesOnlyReportNextTime() {
        this.sendChangesOnlyReport.set(true);
    }
}
