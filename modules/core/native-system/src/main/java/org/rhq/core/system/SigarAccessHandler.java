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

package org.rhq.core.system;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.Sigar;

/**
 * An {@link InvocationHandler} for a {@link org.hyperic.sigar.SigarProxy}.
 * <p>A single instance of this class will be created by the {@link SigarAccess} class.</p>
 * This class holds a shared {@link Sigar} instance and serializes calls. If a thread waits more than
 * 'sharedSigarLockMaxWait' seconds, it will be given a new Sigar instance, which will be destroyed at the end of the
 * call.
 * <p>Every 5 minutes, a background task checks that 'localSigarInstancesWarningThreshold' has not been exceeded. It it
 * has, a warning message will be logged, optionally with a thread dump.
 * <p>This class is configurable with System properties:
 * <ul>
 *     <li><strong>sharedSigarLockMaxWait</strong>: maximum time in seconds a thread will wait for the shared Sigar lock
 *     acquistion; defaults to 2 seconds</li>
 *     <li><strong>localSigarInstancesWarningThreshold</strong>: threshold of currently living Sigar instances at which
 *     the background task will print warning messages; defaults to 50</li>
 *     <li><strong>maxLocalSigarInstances</strong>: maximum number of local Sigar instances which can be created, zero
 *     and negative values being interpreted as 'no limit'; defaults to 50</li>
 *     <li><strong>threadDumpOnlocalSigarInstancesWarningThreshold</strong>: if set to true (case insensitive), the
 *     background task will also log a thread dump when <strong>localSigarInstancesWarningThreshold</strong> is met</li>
 * </ul>
 * </p>
 *
 * @author Thomas Segismont
 */
class SigarAccessHandler implements InvocationHandler {
    private static final Log LOG = LogFactory.getLog(SigarAccessHandler.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final int SHARED_SIGAR_LOCK_MAX_WAIT = Integer.getInteger("sharedSigarLockMaxWait", 2);
    private static final int LOCAL_SIGAR_INSTANCES_WARNING_THRESHOLD = Integer.getInteger(
        "localSigarInstancesWarningThreshold", 50);
    private static final int MAX_LOCAL_SIGAR_INSTANCES = Integer.getInteger("maxLocalSigarInstances", 50);
    private static final boolean LIMIT_LOCAL_SIGAR_INSTANCES = MAX_LOCAL_SIGAR_INSTANCES > 0;
    private static final boolean THREAD_DUMP_ON_SIGAR_INSTANCES_THRESHOLD = Boolean
        .getBoolean("threadDumpOnlocalSigarInstancesWarningThreshold");

    private final SigarFactory sigarFactory;
    private final ReentrantLock sharedSigarLock;
    private final ReentrantLock localSigarLock;
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile int localSigarInstancesCount;
    private Sigar sharedSigar;
    private volatile boolean closed;

    SigarAccessHandler() {
        this(new DefaultSigarFactory());
    }

    SigarAccessHandler(SigarFactory sigarFactory) {
        this.sigarFactory = sigarFactory;
        sharedSigarLock = new ReentrantLock();
        localSigarLock = new ReentrantLock();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            private ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

            private AtomicInteger threadCounter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = defaultThreadFactory.newThread(runnable);
                thread.setName("SigarAccessHandler-" + threadCounter.incrementAndGet());
                // With daemon threads, there is no need to call #shutdown on the executor to let the JVM go down
                thread.setDaemon(true);
                return thread;
            }
        });
        scheduledExecutorService.scheduleWithFixedDelay(new ThresholdChecker(), 1, 5, MINUTES);
        localSigarInstancesCount = 0;
        closed = false;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // its possible in the time between this handler's creation and now, someone disabled the native layer.
        // throw a runtime exception if the native system was disabled
        if (SystemInfoFactory.isNativeSystemInfoDisabled()) {
            throw new SystemInfoException("Native system has been disabled");
        }

        // Acquire lock for shared Sigar instance. Wait 'sharedSigarLockMaxWait' seconds at most
        boolean acquiredLock = sharedSigarLock.tryLock(SHARED_SIGAR_LOCK_MAX_WAIT, SECONDS);
        if (acquiredLock) {
            try {
                if (closed) {
                    throw new SystemInfoException("SigarAccess has been closed");
                }
                if (sharedSigar == null) {
                    this.sharedSigar = sigarFactory.createSigarInstance();
                }
                return method.invoke(sharedSigar, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                sharedSigarLock.unlock();
            }
        } else {
            Sigar localSigar = createLocalSigarInstance();
            try {
                return method.invoke(localSigar, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                closeLocalSigarInstance(localSigar);
            }
        }
    }

    private Sigar createLocalSigarInstance() {
        localSigarLock.lock();
        try {
            if (LIMIT_LOCAL_SIGAR_INSTANCES && localSigarInstancesCount >= MAX_LOCAL_SIGAR_INSTANCES) {
                throw new RuntimeException("Too many Sigar instances created");
            }
            Sigar sigarInstance = sigarFactory.createSigarInstance();
            localSigarInstancesCount++;
            return sigarInstance;
        } finally {
            localSigarLock.unlock();
        }
    }

    private void closeLocalSigarInstance(Sigar sigar) {
        localSigarLock.lock();
        try {
            sigar.close();
            localSigarInstancesCount--;
        } finally {
            localSigarLock.unlock();
        }
    }

    void close() {
        if (sharedSigar != null) {
            sharedSigarLock.lock();
            try {
                closed = true;
                sharedSigar.close();
                sharedSigar = null;
            } finally {
                sharedSigarLock.unlock();
            }
        }
        scheduledExecutorService.shutdownNow();
    }

    int localSigarInstancesCount() {
        return localSigarInstancesCount;
    }

    interface SigarFactory {
        Sigar createSigarInstance();
    }

    private static class DefaultSigarFactory implements SigarFactory {
        @Override
        public Sigar createSigarInstance() {
            return new Sigar();
        }
    }

    private class ThresholdChecker implements Runnable {
        @Override
        public void run() {
            int currentCount = localSigarInstancesCount;
            if (currentCount > LOCAL_SIGAR_INSTANCES_WARNING_THRESHOLD) {
                StringBuilder sb = new StringBuilder();
                sb.append("There are ").append(currentCount).append(" local Sigar instances currently active. ")
                    .append("This may indicate that a call to the shared Sigar instance did not complete.");
                if (THREAD_DUMP_ON_SIGAR_INSTANCES_THRESHOLD) {
                    sb.append(LINE_SEPARATOR);
                    threadDump(sb);
                }
                LOG.warn(sb.toString());
            }
        }

        private void threadDump(StringBuilder sb) {
            ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
            for (ThreadInfo threadInfo : threadInfos) {
                sb.append(threadInfo);
                sb.append(LINE_SEPARATOR);
            }
        }
    }
}
