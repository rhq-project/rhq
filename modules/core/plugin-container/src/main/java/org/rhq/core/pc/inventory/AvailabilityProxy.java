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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;

/**
 * Proxy class for executing availability checks. Checks are done using a
 * supplied thread pool. If the resource availability does not return within one
 * second, the next call to {@link #getAvailability()} will return the
 * calculated availability, if available.
 *
 * With the potential of having thousands, and even tens of thousands, of instances
 * of this proxy, we must ensure that we keep it as lean as possible to reduce
 * memory footprint of the agent. For example, we do not create a logger object for
 * every proxy. Instead, LOG is static. This should be OK for how this proxy is used.
 *
 * @author Elias Ross
 */
public class AvailabilityProxy implements AvailabilityFacet, Callable<AvailabilityType> {

    private static final Log LOG = LogFactory.getLog(AvailabilityProxy.class); // purposefully static, don't create one per proxy

    /**
     * How long to wait for a resource to return their availability *immediately* (in ms).
     * If a resource takes longer than this, then the number of timeouts is incremented, and then
     * the container will just assume availability will be returned asynchronously for this resource.
     */
    private static final int AVAIL_SYNC_TIMEOUT;

    /**
     * Number of consecutive avail sync timeouts before we assume the resource's avail checking can not meet the async
     * timeout.  At that point stop slowing things down waiting for the timeout and instead, for this resource,
     * rely only on the async results. In other words, stop trying to report live avail if live avail checking is
     * consistently too slow. Max = 127.  We use a byte here to save space.
     */
    private static final byte AVAIL_SYNC_TIMEOUT_LIMIT;

    /**
     * How long to wait for an *async* future to return a resource availability (in ms).
     * If a resource takes longer than this during an async call (via a thread from the executor thread pool)
     * and another request comes in for the availability, then that async call will be canceled and a new
     * one will be resubmitted, restarting the clock. This just helps clean up any hung threads waiting
     * for an availability that is just taking too much time to complete.
     */
    private static final int AVAIL_ASYNC_TIMEOUT;

    static {
        int syncAvailTimeout;
        try {
            // unlikely to be changed but back-door configurable
            syncAvailTimeout = Integer.parseInt(System.getProperty("rhq.agent.plugins.availability-scan.sync-timeout",
                "1000"));
        } catch (Throwable t) {
            syncAvailTimeout = 1000;
        }
        AVAIL_SYNC_TIMEOUT = syncAvailTimeout;

        byte syncAvailTimeoutLimit;
        try {
            // unlikely to be changed but back-door configurable
            syncAvailTimeoutLimit = Byte.parseByte(System.getProperty(
                "rhq.agent.plugins.availability-scan.sync-timeout-limit", "5"));
        } catch (Throwable t) {
            syncAvailTimeoutLimit = 5;
        }
        if (syncAvailTimeoutLimit > 127) {
            syncAvailTimeoutLimit = 127;
        }
        AVAIL_SYNC_TIMEOUT_LIMIT = syncAvailTimeoutLimit;

        int asyncAvailTimeout;
        try {
            // unlikely to be changed but back-door configurable
            asyncAvailTimeout = Integer.parseInt(System.getProperty(
                "rhq.agent.plugins.availability-scan.async-timeout", "60000"));
        } catch (Throwable t) {
            asyncAvailTimeout = 60000;
        }
        AVAIL_ASYNC_TIMEOUT = asyncAvailTimeout;
    }

    private Future<AvailabilityType> availabilityFuture = null;

    private volatile Thread current;

    private long lastSubmitTime = 0;

    private final ResourceContainer resourceContainer;

    /**
     * Number of consecutive avail sync timeouts for the resource. This value is reset if availability is
     * returned synchronously (within the timeout period).  There is currently no way to 'reset' this (short
     * of agent restart) after it has triggered, meaning the resource will no longer try to report live avail.
     */
    private byte availSyncConsecutiveTimeouts = 0;

    /**
     * Constructs a new proxy.
     */
    public AvailabilityProxy(ResourceContainer resourceContainer) {
        this.resourceContainer = resourceContainer;
    }

    @Override
    public AvailabilityType call() throws Exception {
        current = Thread.currentThread();
        ClassLoader originalContextClassLoader = current.getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.resourceContainer.getResourceClassLoader());
            return this.resourceContainer.getResourceComponent().getAvailability();
        } finally {
            current.setContextClassLoader(originalContextClassLoader);
        }
    }

    /**
     * Returns the current or most currently reported availability. If
     * {@link AvailabilityType#UNKNOWN} is returned, then the availability is
     * being computed.
     *
     * @throws TimeoutException
     *             if an async check exceeds AVAIL_ASYNC_TIMEOUT
     */
    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType avail = UNKNOWN;

        try {
            // If the avail check timed out, or if we are not attempting synchronous checks (due to
            // exceeding the consecutive timeout limit) then the future will exist.
            if (availabilityFuture != null) {
                if (availabilityFuture.isDone()) {
                    // hold onto and report the last known value if necessary
                    avail = availabilityFuture.get();

                } else {
                    // We are still waiting on the previously submitted async avail check - let's just return
                    // the last one we got. Note that if the future is not done after a large amount of time,
                    // then it means this thread could somehow be hung or otherwise stuck and not returning. Not good.
                    // In this case, throw a detailed exception to the avail checker.
                    long elapsedTime = System.currentTimeMillis() - lastSubmitTime;
                    if (elapsedTime > getAsyncTimeout()) {

                        Throwable t = new Throwable();
                        if (current != null) {
                            t.setStackTrace(current.getStackTrace());
                        }
                        String msg = "Availability check ran too long [" + elapsedTime + "ms], canceled for ["
                            + this.resourceContainer + "]; Stack trace includes the timed out thread's stack trace.";
                        availabilityFuture.cancel(true);

                        // try again, maybe the situation will resolve in time for the next check
                        availabilityFuture = this.resourceContainer.submitAvailabilityCheck(this);
                        lastSubmitTime = System.currentTimeMillis();

                        throw new TimeoutException(msg, t);
                    } else {
                        return getLastAvailabilityType();
                    }
                }
            }

            // request a thread to do an avail check
            availabilityFuture = this.resourceContainer.submitAvailabilityCheck(this);
            lastSubmitTime = System.currentTimeMillis();

            // if we have exceeded the timeout too many times in a row assume that this is a slow
            // resource and stop performing synchronous checks, which would likely fail to return fast enough anyway.
            if (availSyncConsecutiveTimeouts < getSyncTimeoutLimit()) {
                // attempt to get availability synchronously
                avail = availabilityFuture.get(getSyncTimeout(), TimeUnit.MILLISECONDS);

                // success (failure will throw exception)
                availSyncConsecutiveTimeouts = 0;
                availabilityFuture = null;

            } else if (availSyncConsecutiveTimeouts == getSyncTimeoutLimit()) {
                // log one time that we are disabling synchronous checks for this resource
                ++availSyncConsecutiveTimeouts;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Disabling synchronous availability collection for [" + resourceContainer + "]; ["
                        + getSyncTimeoutLimit() + "] consecutive timeouts exceeding [" + getSyncTimeout() + "ms]");
                }
            }
        } catch (InterruptedException e) {
            LOG.debug("InterruptedException; shut down is (likely) in progress.");
            availabilityFuture.cancel(true);
            availabilityFuture = null;
            Thread.currentThread().interrupt();
            return UNKNOWN;

        } catch (ExecutionException e) {
            throw new RuntimeException("Availability check failed", e.getCause());

        } catch (java.util.concurrent.TimeoutException e) {
            // failed to get avail synchronously. next call to the future will return availability (we hope)
            ++availSyncConsecutiveTimeouts;
        }

        return processAvail(avail);
    }

    private AvailabilityType processAvail(AvailabilityType type) {
        AvailabilityType result = type;
        switch (type) {
        case UP:
        case DOWN:
            break;
        default:
            if (LOG.isDebugEnabled()) {
                LOG.debug("ResourceComponent [" + this.resourceContainer + "] getAvailability() returned " + type
                    + ". This is invalid and is being replaced with DOWN.");
            }
            result = DOWN;
        }

        // whenever changing to UP we reset the timeout counter.  This is because DOWN resources often respond
        // slowly to getAvailability() calls (for example, waiting for a connection attempt to time out).  When a
        // resource comes up we should give it a chance to respond quickly and provide live avail.
        AvailabilityType lastAvail = getLastAvailabilityType();
        if (result != getLastAvailabilityType()) {
            if (result == UP) {
                if (availSyncConsecutiveTimeouts >= getSyncTimeoutLimit()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Enabling synchronous availability collection for [" + resourceContainer
                            + "]; Availability has just changed from [" + lastAvail + "] to UP.");
                    }
                }
                availSyncConsecutiveTimeouts = 0;

            }
        }

        return result;
    }

    private AvailabilityType getLastAvailabilityType() {
        Availability av = this.resourceContainer.getAvailability();
        if (av != null) {
            AvailabilityType avt = av.getAvailabilityType();
            return (avt != null) ? avt : AvailabilityType.UNKNOWN;
        } else {
            return AvailabilityType.UNKNOWN;
        }
    }

    /**
     * Override point. Typically for testing.
     * @return something other than the env var setting.
     */
    protected long getAsyncTimeout() {
        return AVAIL_ASYNC_TIMEOUT;
    }

    /**
     * Override point. Typically for testing.
     * @return something other than the env var setting.
     */
    protected long getSyncTimeout() {
        return AVAIL_SYNC_TIMEOUT;
    }

    /**
     * Override point. Typically for testing.
     * @return something other than the env var setting.
     */
    protected byte getSyncTimeoutLimit() {
        return AVAIL_SYNC_TIMEOUT_LIMIT;
    }

    protected boolean isSyncDisabled() {
        return availSyncConsecutiveTimeouts >= getSyncTimeoutLimit();
    }

    /**
     * Debug string.
     */
    @Override
    public String toString() {
        return "AvailabilityProxy [resource=" + resourceContainer + ", lastSubmitTime="
            + new java.util.Date(lastSubmitTime) + ", availabilityFuture=" + availabilityFuture + ", current="
            + current + ", timeouts=" + availSyncConsecutiveTimeouts + "]";
    }
}
