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
package org.rhq.core.pc.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.Sigar;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.transfer.EventReport;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.event.EventPoller;

/**
 * Manager for the Plugin Container's Event subsystem.
 *
 * @author Ian Springer
 */
public class EventManager implements ContainerService {
    private final Log log = LogFactory.getLog(this.getClass());

    private static final String SENDER_THREAD_POOL_NAME = "EventManager.sender";
    private static final int SENDER_THREAD_POOL_CORE_SIZE = 2;
    private static final int SENDER_INITIAL_DELAY_SECS = 30;
    private static final int SENDER_PERIOD_SECS = 30;

    private static final String POLLER_THREAD_POOL_NAME = "EventManager.poller";
    private static final int POLLER_THREAD_POOL_CORE_SIZE = 5; // TODO: Make this configurable.
    private static final int POLLER_INITIAL_DELAY_SECS = 0;

    private PluginContainerConfiguration pcConfig;
    private ScheduledThreadPoolExecutor senderThreadPool;
    private EventReport activeReport = new EventReport();
    private ReentrantReadWriteLock reportLock = new ReentrantReadWriteLock(true);
    private ScheduledThreadPoolExecutor pollerThreadPool;
    private Map<EventSource, Runnable> pollerThreads;
    private Sigar sigar;

    public void initialize() {
        // Schedule sender thread(s) to send Event reports to the Server periodically.
        EventSenderRunner senderRunner = new EventSenderRunner(this);
        this.senderThreadPool = new ScheduledThreadPoolExecutor(SENDER_THREAD_POOL_CORE_SIZE, new LoggingThreadFactory(
                SENDER_THREAD_POOL_NAME, true));
        this.senderThreadPool.scheduleAtFixedRate(senderRunner, SENDER_INITIAL_DELAY_SECS,
                SENDER_PERIOD_SECS, TimeUnit.SECONDS);

        // Set up a thread pool for polling threads. Polling threads will be added to the pool via calls to
        // registerEventPoller().
        this.pollerThreadPool = new ScheduledThreadPoolExecutor(POLLER_THREAD_POOL_CORE_SIZE, new LoggingThreadFactory(
                POLLER_THREAD_POOL_NAME, true));
        this.pollerThreads = new HashMap<EventSource, Runnable>();

        this.sigar = new Sigar();
    }

    public void shutdown() {
        if (this.senderThreadPool != null) {
            this.senderThreadPool.shutdownNow();
        }
        if (this.pollerThreadPool != null) {
            this.pollerThreadPool.shutdownNow();
        }
        this.sigar.close();
    }

    public void setConfiguration(PluginContainerConfiguration config) {
        this.pcConfig = config;
    }

    void publishEvents(@NotNull Set<Event> events, @NotNull EventSource eventSource) {
        try {
            this.reportLock.readLock().lock();
            this.activeReport.addEvents(events, eventSource);
        } catch (Throwable t) {
            log.error("Failed to add Events " + events + " to Event report.", t);
        } finally {
            this.reportLock.readLock().unlock();
        }
    }

    Sigar getSigar() {
        return sigar;
    }

    /**
     * Sends the given Event report to the Server, if this Plugin Container has Server services that it can communicate
     * with.
     *
     * @param report the Event report to be sent
     */
    void sendEventReport(EventReport report) {
        if (!report.getEvents().isEmpty() && this.pcConfig.getServerServices() != null) {
            try {
                this.pcConfig.getServerServices().getEventServerService().mergeEventReport(report);
            } catch (Exception e) {
                log.warn("Failure to report Events to Server.", e);
            }
        }
    }

    EventReport swapReport() {
        try {
            this.reportLock.writeLock().lock();
            EventReport previousReport = this.activeReport;
            this.activeReport = new EventReport();
            return previousReport;
        } finally {
            this.reportLock.writeLock().unlock();
        }
    }

    void registerEventPoller(EventPoller poller, EventSource eventSource) {
        long pollingInterval = Math.max(EventPoller.MINIMUM_POLLING_INTERVAL, Math.min(EventPoller.MAXIMUM_POLLING_INTERVAL, poller.getPollingInterval()));
        EventPollerRunner pollerRunner = new EventPollerRunner(poller, eventSource, this);
        Runnable pollerFuture =
                (Runnable) this.pollerThreadPool.scheduleAtFixedRate(pollerRunner, POLLER_INITIAL_DELAY_SECS,
                        pollingInterval, TimeUnit.SECONDS);
        this.pollerThreads.put(eventSource, pollerFuture);
    }

    void unregisterEventPoller(EventSource eventSource) {
        if (this.pollerThreads.containsKey(eventSource)) {
            Runnable pollerFuture = this.pollerThreads.get(eventSource);
            boolean wasRemoved = this.pollerThreadPool.remove(pollerFuture);
            if (!wasRemoved) {
                log.error("Failed to remove poller with " + eventSource + " from thread pool.");
            }
            this.pollerThreads.remove(eventSource);
        }
    }
}
