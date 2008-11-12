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
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.transfer.EventReport;
import org.rhq.core.domain.resource.Resource;
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
    private static final int POLLER_THREAD_POOL_CORE_SIZE = 1;
    private static final int POLLER_THREAD_POOL_MAX_SIZE = 3;
    private static final int POLLER_INITIAL_DELAY_SECS = 0;

    private PluginContainerConfiguration pcConfig;
    private ScheduledThreadPoolExecutor senderThreadPool;
    private EventReport activeReport = new EventReport();
    private ReentrantReadWriteLock reportLock = new ReentrantReadWriteLock(true);
    private ScheduledThreadPoolExecutor pollerThreadPool;
    private Map<PollerKey, Runnable> pollerThreads;
    private Sigar sigar;

    public void initialize() {
        // Schedule sender thread(s) to send Event reports to the Server periodically.
        EventSenderRunner senderRunner = new EventSenderRunner(this);
        this.senderThreadPool = new ScheduledThreadPoolExecutor(SENDER_THREAD_POOL_CORE_SIZE, new LoggingThreadFactory(
            SENDER_THREAD_POOL_NAME, true));
        this.senderThreadPool.scheduleAtFixedRate(senderRunner, SENDER_INITIAL_DELAY_SECS, SENDER_PERIOD_SECS,
            TimeUnit.SECONDS);

        // Set up a thread pool for polling threads. Polling threads will be added to the pool via calls to
        // registerEventPoller().
        this.pollerThreadPool = new ScheduledThreadPoolExecutor(POLLER_THREAD_POOL_CORE_SIZE, new LoggingThreadFactory(
            POLLER_THREAD_POOL_NAME, true));
        this.pollerThreadPool.setMaximumPoolSize(POLLER_THREAD_POOL_MAX_SIZE);
        this.pollerThreads = new HashMap<PollerKey, Runnable>();
    }

    public void shutdown() {
        if (this.senderThreadPool != null) {
            this.senderThreadPool.shutdownNow();
        }
        if (this.pollerThreadPool != null) {
            this.pollerThreadPool.shutdownNow();
        }
        if (this.sigar != null) {
            this.sigar.close();
        }
    }

    public void setConfiguration(PluginContainerConfiguration config) {
        this.pcConfig = config;
    }

    void publishEvents(@NotNull
    Set<Event> events, @NotNull
    Resource resource) {
        try {
            this.reportLock.readLock().lock();
            for (Event event : events) {
                EventSource eventSource = createEventSource(event, resource);
                this.activeReport.addEvent(event, eventSource);
            }
        } catch (Throwable t) {
            log.error("Failed to add Events for " + resource + " to Event report: " + events, t);
        } finally {
            this.reportLock.readLock().unlock();
        }
    }

    Sigar getSigar() {
        if (this.sigar == null) {
            this.sigar = new Sigar();
        }
        return this.sigar;
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

    void registerEventPoller(EventPoller poller, int pollingInterval, Resource resource, String sourceLocation) {
        EventPollerRunner pollerRunner = new EventPollerRunner(poller, resource, this);
        Runnable pollerFuture = (Runnable) this.pollerThreadPool.scheduleAtFixedRate(pollerRunner,
            POLLER_INITIAL_DELAY_SECS, pollingInterval, TimeUnit.SECONDS);
        PollerKey pollerKey = new PollerKey(resource.getId(), poller.getEventType(), sourceLocation);
        this.pollerThreads.put(pollerKey, pollerFuture);
    }

    void unregisterEventPoller(Resource resource, String eventType, String sourceLocation) {
        PollerKey pollerKey = new PollerKey(resource.getId(), eventType, sourceLocation);
        if (this.pollerThreads.containsKey(pollerKey)) {
            Runnable pollerThread = this.pollerThreads.get(pollerKey);
            boolean wasRemoved = this.pollerThreadPool.remove(pollerThread);
            if (!wasRemoved) {
                log.error("Failed to remove poller with " + pollerKey + " from thread pool.");
            }
            this.pollerThreads.remove(pollerKey);
        }
    }

    private EventSource createEventSource(Event event, Resource resource) {
        EventDefinition eventDefinition = EventUtility.getEventDefinition(event.getType(), resource.getResourceType());
        if (eventDefinition == null) {
            throw new IllegalArgumentException("Unknown type - no EventDefinition found with name '" + event.getType()
                + "'.");
        }
        //noinspection ConstantConditions
        return new EventSource(event.getSourceLocation(), eventDefinition, resource);
    }

    static class PollerKey {
        int resourceId;
        String eventType;
        String sourceLocation;

        PollerKey(int resourceId, String eventType, String sourceLocation) {
            this.resourceId = resourceId;
            this.eventType = eventType;
            this.sourceLocation = sourceLocation;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;

            PollerKey that = (PollerKey) obj;

            if (resourceId != that.resourceId)
                return false;
            if (!eventType.equals(that.eventType))
                return false;
            if (sourceLocation != null ? !sourceLocation.equals(that.sourceLocation) : that.sourceLocation != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = resourceId;
            result = 31 * result + eventType.hashCode();
            result = 31 * result + (sourceLocation != null ? sourceLocation.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "PollerKey[resourceId=" + this.resourceId + ", eventType=" + this.eventType + "]";
        }
    }
}
