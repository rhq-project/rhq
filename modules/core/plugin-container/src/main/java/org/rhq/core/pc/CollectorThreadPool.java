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
package org.rhq.core.pc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.measurement.MeasurementCollectorRunnable;

/**
 * A utility class that can be used by plugins whose components may not be able to collect data
 * fast enough. This thread pool object can be used to submit {@link Runnable} instances,
 * each of which will be used to collect information from a managed resource.
 *
 * This class is used in conjunction with instances of {@link AvailabilityCollectorRunnable} or
 * {@link MeasurementCollectorRunnable}. Read its javadoc for more information.
 *
 * @author John Mazzitelli
 */
public class CollectorThreadPool {
    private static final Log log = LogFactory.getLog(CollectorThreadPool.class);

    /**
     * To avoid many plugins/components needing to spawn their own thread pools/threads, this
     * will allow everyone to reuse the same thread pool. This thread pool will be allowed to grow as needed,
     * but will reuse threads when they become available.
     */
    private final ScheduledExecutorService executor;

    public CollectorThreadPool() {
        log.debug(this.toString());
        ThreadFactory daemonFactory = new LoggingThreadFactory("CollectorThreadPool", true);
        executor = Executors.unconfigurableScheduledExecutorService(Executors.newScheduledThreadPool(1, daemonFactory));
    }

    /**
     * Shuts down all tasks.
     */
    public void shutdown() {
        log.debug("Shutting down AvailabilityCollector thread pool...");
        PluginContainer pluginContainer = PluginContainer.getInstance();
        pluginContainer.shutdownExecutorService(executor, true);
    }

    /**
     * Returns the underlying scheduled executor, which is unconfigurable.
     */
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Returns a debug string.
     */
    public String toString() {
        return getClass().getName() + " executor=" + executor;
    }

}
