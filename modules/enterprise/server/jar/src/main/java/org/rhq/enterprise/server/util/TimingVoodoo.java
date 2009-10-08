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
package org.rhq.enterprise.server.util;

/**
 * This helper class is full of "voodoo" functions related to synchronizing timed data between the agent and the server.
 * We call them "voodoo" because they deal mostly with rounding down and guestimating percentages.
 */
public final class TimingVoodoo {
    /**
     * Given the approximate time associated with a data point and the interval at which that data point is being
     * collected, compute the exact data point to which the data point corresponds.
     *
     * @param  approxTime the approximate time to which the data point corresponds
     * @param  interval   the collection interval
     *
     * @return the time, rounded down to the previous collection interval
     */
    public static long roundDownTime(long approxTime, long interval) {
        return approxTime - (approxTime % interval);
    }

    /**
     * Given the approximate time associated with a data point and the interval at which that data point is being
     * collected, compute the exact data point to which the data point corresponds.
     *
     * @param  approxTime the approximate time to which the data point corresponds
     * @param  interval   the collection interval
     *
     * @return the time, rounded up or down to the closest collection interval
     */
    public static long closestTime(long approxTime, long interval) {
        long mod = approxTime % interval;

        if (mod > (interval / 2)) {
            // Round up
            approxTime += interval;
        }

        return approxTime - mod;
    }
}