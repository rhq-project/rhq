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

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * @deprecated this is no longer necessary in 4.10+ since the plugin container makes all avail checks asynchronous now
 */
@Deprecated()
public class AvailabilityCollectorRunnable implements Runnable {
    public static final long MIN_INTERVAL = 60000L;
    private final AvailabilityFacet availabilityChecker;
    public AvailabilityCollectorRunnable(AvailabilityFacet availabilityChecker, long interval,
        ClassLoader contextClassloader, Executor threadPool) {

        if (availabilityChecker == null) {
            throw new IllegalArgumentException("availabilityChecker is null");
        }

        this.availabilityChecker = availabilityChecker;
    }

    public AvailabilityType getLastKnownAvailability() {
        // this deprecated class will fall back to doing a sync call
        return this.availabilityChecker.getAvailability();
    }

    public void start() {
    }

    public void stop() {
    }

    public void run() {
    }
}
