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
package org.rhq.core.pc.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This thread factory just adds a {@link UncaughtExceptionHandler} to threads it creates so that there are logs of the
 * exceptions that cause threads to die.
 *
 * <p>Use this class when providing a thread factory to threadpools.</p>
 *
 * <p>This factory's constructor can be told to create daemon or non-daemon threads.</p>
 *
 * @author Greg Hinkle
 */
public class LoggingThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {
    private final Log log;
    private final String poolName;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final boolean daemon;

    /**
     * Creates a factory that will produce either daemon or non-daemon threads. Be careful if you pass in <code>
     * false</code> for the <code>daemon</code> parameter; if you do, make sure your thread pool and all its threads
     * created by this factory are properly shutdown; otherwise you could prevent the VM from exiting properly.
     *
     * @param poolName the name of the thread pool that will be using this factory
     * @param daemon   if <code>true</code>, the factory will create daemon threads; <code>false</code> for non-daemon
     *                 threads
     */
    public LoggingThreadFactory(String poolName, boolean daemon) {
        this.poolName = poolName;
        this.log = LogFactory.getLog("org.rhq.threadpools." + poolName);
        this.daemon = daemon;

        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    /**
     * @see java.util.concurrent.ThreadFactory#newThread(Runnable)
     */
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, poolName + "-" + threadNumber.getAndIncrement());

        t.setDaemon(this.daemon);

        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }

        t.setUncaughtExceptionHandler(this);

        return t;
    }

    /**
     * This simply logs the exception via this factory's logger.
     *
     * @see UncaughtExceptionHandler#uncaughtException(Thread, Throwable)
     */
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception on scheduled thread [" + t.getName() + "]", e);
    }

    public String getPoolName() {
        return poolName;
    }

    @Override
    public String toString() {
        return "LoggingThreadFactory[poolName=" + poolName + "]";
    }
}