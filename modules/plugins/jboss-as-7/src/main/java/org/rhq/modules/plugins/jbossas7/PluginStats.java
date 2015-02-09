/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton that keeps track of some statistics of this plugin.
 *
 * @author Heiko W. Rupp
 */
public class PluginStats {
    private static PluginStats ourInstance = new PluginStats();

    AtomicLong requestCount = new AtomicLong();
    AtomicLong requestTime = new AtomicLong();
    AtomicLong maxTime = new AtomicLong();

    public static PluginStats getInstance() {
        return ourInstance;
    }

    private PluginStats() {
    }

    public void incrementRequestCount() {
        requestCount.incrementAndGet();
    }

    public void addRequestTime(long time) {
        requestTime.addAndGet(time);
        long currentMax;
        do {
            currentMax = maxTime.get();
        } while (currentMax < time && !maxTime.compareAndSet(currentMax, time));
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getRequestTime() {
        return requestTime.get();
    }

    public long getMaxTime() {
        return maxTime.getAndSet(0);
    }
}
