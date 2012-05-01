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
package org.rhq.core.pc.availability;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;

/**
 * A utility class that can be used by plugins whose components may not be able to collect availability statuses
 * fast enough. This thread pool object can be used to submit {@link AvailabilityCollectorRunnable} instances,
 * each of which will be used to collect availability statuses for a managed resource. 
 * 
 * This class is used in conjunction with instances of {@link AvailabilityCollectorRunnable} - read its javadoc for more.
 *
 * @author John Mazzitelli
 */
public class AvailabilityCollectorThreadPool implements Executor {
    private static final Log log = LogFactory.getLog(AvailabilityCollectorThreadPool.class);

    /**
     * To avoid many plugins/components needing to spawn their own thread pools/threads, this
     * will allow everyone to reuse the same thread pool. This thread pool will be allowed to grow as needed,
     * but will reuse threads when they become available.
     */
    private ExecutorService threadPool;

    public void initialize() {
        synchronized (this) {
            if (threadPool == null) {
                log.debug("Initializing AvailabilityCollector thread pool");
                ThreadFactory daemonFactory = new LoggingThreadFactory("AvailabilityCollector", true);
                threadPool = Executors.newCachedThreadPool(daemonFactory);
            }
        }
        return;
    }

    public void shutdown() {

        synchronized (this) {
            if (threadPool != null) {
                log.debug("Shutting down AvailabilityCollector thread pool...");
                PluginContainer pluginContainer = PluginContainer.getInstance();
                pluginContainer.shutdownExecutorService(threadPool, true);
                threadPool = null;
            }
        }
        return;
    }

    /**
     * Given a {@link AvailabilityCollectorRunnable} instance, this will run that instance in a thread, thus
     * allowing the availability status for a managed resource to be periodically checked asynchronously. The
     * given runnable will store its last known availability status so it can be retrieved very fast.
     * 
     * The given runnable must be of type {@link AvailabilityCollectorRunnable}, otherwise a runtime exception will occur.
     * 
     * @param runnable the availability collector runnable that will be invoked in a thread to being collecting
     *                 availability status of a managed resource.
     */
    public void execute(Runnable runnable) {
        if (runnable instanceof AvailabilityCollectorRunnable) {
            synchronized (this) {
                if (threadPool != null) {
                    threadPool.execute(runnable);
                }
            }
            return;
        } else if (runnable == null) {
            throw new NullPointerException("runnable == null");
        } else {
            throw new IllegalArgumentException("Runnable is of type [" + runnable.getClass() + "]; must be of type ["
                + AvailabilityCollectorRunnable.class + "]");
        }
    }
}
