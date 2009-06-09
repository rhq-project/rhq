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
package org.rhq.core.pluginapi.availability;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * A class that can be used by plugins whose components may not be able to collect availability statuses fast enough.
 * 
 * Typically, availability checks are very fast (sub-second). However, the plugin container puts a time limit
 * on how long it will wait for a plugin's resource component to return availability status from calls to
 * {@link AvailabilityFacet#getAvailability()}. This time limit is typically on the order of several seconds.
 * The purpose of this time limit is to avoid having a rogue or misbehaving plugin from causing delays in availability
 * reporting for the rest of the resources being managed within the system.
 * 
 * This class provides an implementation to help resource components that can't guarantee how fast its
 * availability checks will be. Some managed resources simply can't respond to avaiability checks fast enough. In this
 * case, this class will provide an asynchronous method that will collect availability without a timeout being involved
 * (in other words, availability will be retrieved by waiting as long as it takes). In order to tell the plugin container
 * what the managed resource's current availability is, this class will provide a fast method to return the last known
 * availability of the resource. In other words, it will be able to return the last know availability that was last retrieved
 * by the asynchronous task - this retrieval of the last known availability will be very fast.
 * 
 * @author John Mazzitelli
 */
public class AvailabilityCollectorRunnable implements Runnable {
    private static final Log log = LogFactory.getLog(AvailabilityCollectorRunnable.class);

    /**
     * The minimum interval allowed between availability checks, in milliseconds.
     */
    public static final long MIN_INTERVAL = 60000L;

    /**
     * The thread pool to give this runnable a thread to run in when it needs to check availability. 
     */
    private final Executor threadPool;

    /**
     * If <code>true</code>, this collector runnable should be actively polling the resource for availability status.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * The classloader to be used when checking availability.
     */
    private final ClassLoader contextClassloader;

    /**
     * The object that is used to check the availability for the managed resource.
     */
    private final AvailabilityFacet availabilityChecker;

    /**
     * The time, in milliseconds, that this collector will pause in between availability checks.
     */
    private final long interval;

    /**
     * The last known availability for the resource that this collector is monitoring.
     */
    private AtomicReference<AvailabilityType> lastKnownAvailability = new AtomicReference<AvailabilityType>();

    /**
     * Just a cache of the facet toString used in log messages. We don't want to keep calling toString on the
     * facet for fear we might get some odd blocking or exceptions thrown. So we call it once and cache it here. 
     */
    private final String facetId;

    /**
     * Creates a collector instance that will perform availability checking for a particular managed resource.
     * 
     * The interval is the time, in milliseconds, this collector will wait between availability checks.
     * This is the amount of time this collector will sleep after each time an availability
     * check returned with the latest status. A typically value should be something around 1 minute
     * but if an availability check takes alot of system resources to perform or adversely affects the
     * managed resource if performed too often, you can make this longer.
     * The shortest value allowed, however, is {@link #MIN_INTERVAL}.
     * 
     * @param availabilityChecker the object that is used to periodically check the managed resource (must not be <code>null</code>)
     * @param interval the interval, in millis, between checking availabilities.
     * @param contextClassloader the context classloader that will be used when checking availability
     * @param threadPool the thread pool to be used to submit this runnable when it needs to start
     */
    public AvailabilityCollectorRunnable(AvailabilityFacet availabilityChecker, long interval,
        ClassLoader contextClassloader, Executor threadPool) {

        if (availabilityChecker == null) {
            throw new IllegalArgumentException("availabilityChecker is null");
        }

        if (threadPool == null) {
            throw new IllegalArgumentException("threadPool is null");
        }

        if (interval < MIN_INTERVAL) {
            log.info("Interval is too short [" + interval + "] - setting to minimum of [" + MIN_INTERVAL + "]");
            interval = MIN_INTERVAL;
        }

        if (contextClassloader == null) {
            contextClassloader = Thread.currentThread().getContextClassLoader();
        }

        this.threadPool = threadPool;
        this.availabilityChecker = availabilityChecker;
        this.contextClassloader = contextClassloader;
        this.interval = interval;
        this.lastKnownAvailability.set(AvailabilityType.DOWN);
        this.facetId = availabilityChecker.toString();
    }

    /**
     * This returns the last known availability status that was most recently retrieved from the managed resource.
     * This will not perform a live check on the managed resource; instead, it immediately returns the last known
     * state of the managed resource. For those resource components using this availability collector utility,
     * their {@link AvailabilityFacet#getAvailability()} method should simply be calling this method.
     *
     * @return {@link AvailabilityType#UP} if the resource can be accessed; otherwise {@link AvailabilityType#DOWN}
     */
    public AvailabilityType getLastKnownAvailability() {
        return this.lastKnownAvailability.get();
    }

    /**
     * For those resource components using this availability collector utility,
     * their {@link ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)} method must call this
     * to start the availability checking that this object performs.
     */
    public void start() {
        boolean isStarted = (this.started.getAndSet(true));
        if (isStarted) {
            log.debug("Availability collector runnable [" + this.facetId + "] is already started");
        } else {
            this.threadPool.execute(this);
            log.debug("Availability collector runnable [" + this.facetId + "] submitted to thread pool");
        }
    }

    /**
     * For those resource components using this availability collector utility,
     * their {@link ResourceComponent#stop()} method must call this
     * to stop the availability checking that this object performs.
     */
    public void stop() {
        this.started.set(false);
        log.debug("Availability collector runnable [" + this.facetId + "] was told to stop");
    }

    /**
     * Performs the actual availability checking. This is the method that is invoked
     * after this runnable is {@link #start() submitted to the thread pool}.
     * You should not be calling this method directly - use {@link #start()} instead.
     */
    public void run() {
        log.debug("Availability collector runnable [" + this.facetId + "] started");

        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.contextClassloader);

        try {
            // while we are still running, we need to sleep then get the new availability
            do {
                try {
                    AvailabilityType availability = this.availabilityChecker.getAvailability();
                    this.lastKnownAvailability.set(availability);
                } catch (Throwable t) {
                    log.warn("Availability collector [" + this.facetId
                        + "] failed to get availability - keeping the last known availability of ["
                        + this.lastKnownAvailability.get() + "]. Cause: " + ThrowableUtil.getAllMessages(t));
                }

                try {
                    Thread.sleep(this.interval);
                } catch (InterruptedException e) {
                    // we got interrupted, we assume we need to shutdown
                    this.started.set(false);
                    log.debug("Availability collector [" + this.facetId + "] interrupted");
                }
            } while (this.started.get());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassloader);
        }

        log.debug("Availability collector runnable [" + this.facetId + "] stopped");
        return;
    }
}
