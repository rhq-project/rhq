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
package org.rhq.core.pc.measurement;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Manage the scheduled process of data collection, detection and sending across all plugins.
 *
 * <p/>
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class MeasurementManager extends AgentService implements MeasurementAgentService, ContainerService,
    MeasurementManagerMBean {
    public static final String OBJECT_NAME = "rhq.pc:type=MeasurementManager";

    private static final String COLLECTOR_THREAD_POOL_NAME = "MeasurementManager.collector";
    private static final String SENDER_THREAD_POOL_NAME = "MeasurementManager.sender";

    static final int FACET_METHOD_TIMEOUT = 30 * 1000; // 60 seconds

    private static final Log LOG = LogFactory.getLog(MeasurementManager.class);

    private ScheduledThreadPoolExecutor collectorThreadPool;
    private ScheduledThreadPoolExecutor senderThreadPool;

    private MeasurementSenderRunner measurementSenderRunner;
    private MeasurementCollectorRunner measurementCollectorRunner;

    private PluginContainerConfiguration configuration;

    private PriorityQueue<ScheduledMeasurementInfo> scheduledRequests = new PriorityQueue<ScheduledMeasurementInfo>(
        10000);

    private InventoryManager inventoryManager;

    private Map<Integer, String> traitCache = new HashMap<Integer, String>();

    private Map<Integer, CachedValue> perMinuteCache = new HashMap<Integer, CachedValue>();

    private MeasurementReport activeReport = new MeasurementReport();

    private ReentrantReadWriteLock measurementLock = new ReentrantReadWriteLock(true);

    // -- monitoring information
    private AtomicLong collectedMeasurements = new AtomicLong(0);
    private AtomicLong totalTimeCollecting = new AtomicLong(0);
    private AtomicLong sinceLastCollectedMeasurements = new AtomicLong(0);
    private AtomicLong sinceLastCollectedTime = new AtomicLong(System.currentTimeMillis());

    private AtomicLong lateCollections = new AtomicLong(0);
    private AtomicLong failedCollection = new AtomicLong(0);

    public MeasurementManager() {
        super(MeasurementAgentService.class);
    }

    public void initialize() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.registerMBean(this, new ObjectName(OBJECT_NAME));
        } catch (JMException e) {
            LOG.error("Unable to register MeasurementManagerMBean", e);
        }

        this.inventoryManager = PluginContainer.getInstance().getInventoryManager();

        int threadPoolSize = configuration.getMeasurementCollectionThreadPoolSize();
        long collectionInitialDelaySecs = configuration.getMeasurementCollectionInitialDelay();

        if (configuration.isInsideAgent()) {
            this.collectorThreadPool = new ScheduledThreadPoolExecutor(threadPoolSize, new LoggingThreadFactory(
                COLLECTOR_THREAD_POOL_NAME, true));

            this.senderThreadPool = new ScheduledThreadPoolExecutor(2, new LoggingThreadFactory(
                SENDER_THREAD_POOL_NAME, true));

            this.measurementSenderRunner = new MeasurementSenderRunner(this);
            this.measurementCollectorRunner = new MeasurementCollectorRunner(this);

            // Schedule the measurement sender to send measurement reports periodically.
            this.senderThreadPool.scheduleAtFixedRate(measurementSenderRunner, collectionInitialDelaySecs, 30, TimeUnit.SECONDS);
            // Schedule the measurement collector to collect metrics periodically, whenever there are one or more
            // metrics due to be collected.
            this.collectorThreadPool.schedule(new MeasurementCollectionRequestor(), collectionInitialDelaySecs, TimeUnit.SECONDS);

            // Load persistent measurement schedules from the InventoryManager and reconstitute them.
            Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
            reschedule(platform);
        }
    }

    public class MeasurementCollectionRequestor implements Runnable {
        public void run() {
            try {
                while (true) {
                    long next = getNextExpectedCollectionTime();
                    if (next == Long.MIN_VALUE) {
                        Thread.sleep(10000);
                    } else {
                        long delay = next - System.currentTimeMillis();
                        if (delay <= 0) {
                            //                  collectorThreadPool.execute(measurementCollectorRunner);
                            measurementCollectorRunner.call();
                        } else {
                            Thread.sleep(delay);
                        }
                    }
                }
            } catch (Throwable e) {
                LOG.error("Collection sender shutting down due to error", e);
            } finally {
                LOG.info("Shutting down measurement collection");
            }
        }
    }

    private void reschedule(Resource resource) {
        int resourceId = resource.getId();

        if (resourceId != 0) {
            ResourceContainer container = this.inventoryManager.getResourceContainer(resourceId);
            if (container != null) {
                Set<MeasurementScheduleRequest> schedules = container.getMeasurementSchedule();
                if (schedules != null) {
                    scheduleCollection(resourceId, schedules);
                }

                for (Resource child : resource.getChildResources()) {
                    reschedule(child);
                }
            }
        } else {
            LOG.debug("Will not reschedule schedules for resource - it is not sync'ed yet: " + resource);
        }
    }

    public MeasurementReport getActiveReport() {
        if (this.activeReport == null) {
            this.activeReport = new MeasurementReport();
        }

        return activeReport;
    }

    ReentrantReadWriteLock getLock() {
        return measurementLock;
    }

    /**
     * @param  scheduleId
     * @param  traitValue
     *
     * @return true if the value is new or changed and should be included in the report
     */
    public boolean checkTrait(int scheduleId, String traitValue) {
        String historic = traitCache.get(scheduleId);
        if (traitCache.containsKey(scheduleId)) {
            if (((historic == null) && (traitValue != null)) || ((historic != null) && !historic.equals(traitValue))) {
                traitCache.put(scheduleId, traitValue);
                return true;
            } else {
                return false;
            }
        } else {
            traitCache.put(scheduleId, traitValue);
            return true;
        }
    }

    public Double updatePerMinuteMetric(MeasurementDataNumeric numeric) {
        CachedValue existing = this.perMinuteCache.get(numeric.getScheduleId());
        Double value = null;
        if (existing != null) {
            long timeDifference = numeric.getTimestamp() - existing.timestamp;
            value = (60000D / timeDifference) * (numeric.getValue() - existing.value);
            if (((numeric.getNumericType() == NumericType.TRENDSUP) && (value < 0))
                || ((numeric.getNumericType() == NumericType.TRENDSDOWN) && (value > 0))) {
                value = null;
            }
        }

        this.perMinuteCache.put(numeric.getScheduleId(), new CachedValue(numeric.getTimestamp(), numeric.getValue()));
        return value;
    }

    public void shutdown() {
        if (this.collectorThreadPool != null) {
            this.collectorThreadPool.shutdownNow();
        }

        if (this.senderThreadPool != null) {
            this.senderThreadPool.shutdownNow();
        }

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.unregisterMBean(new ObjectName(OBJECT_NAME));
        } catch (JMException e) {
            LOG.warn("Unable to unregister MeasurementManagerMBean", e);
        }
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }


    public synchronized void updateCollection(Set<ResourceMeasurementScheduleRequest> scheduleRequests) {
        InventoryManager im = PluginContainer.getInstance().getInventoryManager();

        for (ResourceMeasurementScheduleRequest resourceRequest : scheduleRequests) {
            ResourceContainer resourceContainer = im.getResourceContainer(resourceRequest.getResourceId());
            if (resourceContainer != null) {
                resourceContainer.updateMeasurementSchedule(resourceRequest.getMeasurementSchedules());   // this is where we want to update rather than overwrite, right?

//                resourceContainer.setMeasurementSchedule(resourceRequest.getMeasurementSchedules());
                scheduleCollection(resourceRequest.getResourceId(), resourceRequest.getMeasurementSchedules());
            } else {
                // This will happen when the server sends down schedules to an agent with a cleaned inventory
                // Its ok to skip these because the agent will request a reschedule once its been able to synchronize
                // and add these to inventory
                LOG.debug("Resource container was null, could not schedule collection for resource "
                    + resourceRequest.getResourceId());
            }
        }

        // TODO GH: Should I kick the pool or should I just go with the 30 second granularity on collections?
        // If I get much more granular then the server could end up with too many small reports from many agents
        // This may be another reason to have a separate sending mechanism from the collection mechanism.
        clearDuplicateSchedules();

    }
    /**
     * This remoted method allows the server to schedule a bunch of resources with one call
     *
     * @param scheduleRequests
     */
    public synchronized void scheduleCollection(Set<ResourceMeasurementScheduleRequest> scheduleRequests) {
        InventoryManager im = PluginContainer.getInstance().getInventoryManager();

        for (ResourceMeasurementScheduleRequest resourceRequest : scheduleRequests) {
            ResourceContainer resourceContainer = im.getResourceContainer(resourceRequest.getResourceId());
            if (resourceContainer != null) {
//                resourceContainer.updateMeasurementSchedule(resourceRequest.getMeasurementSchedules());   // this is where we want to update rather than overwrite, right?

                resourceContainer.setMeasurementSchedule(resourceRequest.getMeasurementSchedules());
                scheduleCollection(resourceRequest.getResourceId(), resourceRequest.getMeasurementSchedules());
            } else {
                // This will happen when the server sends down schedules to an agent with a cleaned inventory
                // Its ok to skip these because the agent will request a reschedule once its been able to synchronize
                // and add these to inventory
                LOG.debug("Resource container was null, could not schedule collection for resource "
                    + resourceRequest.getResourceId());
            }
        }

        // TODO GH: Should I kick the pool or should I just go with the 30 second granularity on collections?
        // If I get much more granular then the server could end up with too many small reports from many agents
        // This may be another reason to have a separate sending mechanism from the collection mechanism.
        clearDuplicateSchedules();
    }

    /**
     * Used to direct reschedule resources from the persisted to disk schedules
     *
     * @param resourceId The resource to collect on
     * @param requests   The measurements to collect
     */
    public synchronized void scheduleCollection(int resourceId, Set<MeasurementScheduleRequest> requests) {
        // This ensures that all the schedules for a single resource start at the same time
        // This will enable them to be collected at the same time
        long firstCollection = System.currentTimeMillis();
        for (MeasurementScheduleRequest request : requests) {
            ScheduledMeasurementInfo info = new ScheduledMeasurementInfo(request, resourceId);
            info.setNextCollection(firstCollection);

            // This is a workaround for JDK Bug 6207984 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6207984)
            this.scheduledRequests.removeAll(Collections.singletonList(info));

            // Don't add it if collection is disabled for this resource
            if (info.isEnabled()) {
                this.scheduledRequests.offer(info);
            }
        }
    }

    public synchronized void unscheduleCollection(Set<Integer> resourceIds) {
        Iterator<ScheduledMeasurementInfo> itr = this.scheduledRequests.iterator();
        while (itr.hasNext()) {
            ScheduledMeasurementInfo info = itr.next();
            if (resourceIds.contains(info.getResourceId())) {
                itr.remove();
            }
        }
    }

    private void clearDuplicateSchedules() {
        HashSet<Integer> set = new HashSet<Integer>(this.scheduledRequests.size());
        Iterator<ScheduledMeasurementInfo> iter = this.scheduledRequests.iterator();
        while (iter.hasNext()) {
            ScheduledMeasurementInfo info = iter.next();
            if (set.contains(info.getScheduleId())) {
                LOG.debug("Found duplicate schedule - will remove it: " + info.toString());
                iter.remove();
            } else {
                set.add(info.getScheduleId());
            }
        }
    }

    public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, String... measurementName) {
        MeasurementFacet measurementFacet;

        try {
            measurementFacet = ComponentUtil.getComponent(resourceId, MeasurementFacet.class, FacetLockType.READ, FACET_METHOD_TIMEOUT, true, true);
        } catch (Exception e) {
            LOG.warn("Cannot get measurement facet for resource [" + resourceId + "]. Cause: " + e);
            return null;
        }

        MeasurementReport report = new MeasurementReport();

        Set<MeasurementScheduleRequest> allMeasurements = new HashSet<MeasurementScheduleRequest>();
        for (String name : measurementName) {
            MeasurementScheduleRequest request = new MeasurementScheduleRequest(1, name, 0, true, DataType.MEASUREMENT);
            allMeasurements.add(request);
        }

        try {
            measurementFacet.getValues(report, allMeasurements);
        } catch (Throwable t) {
            LOG.error("Could not get measurement values", t);
            return null;
        }

        return new HashSet<MeasurementData>(report.getNumericData());
    }

    public long getNextExpectedCollectionTime() {
        ScheduledMeasurementInfo nextScheduledMeasurement = this.scheduledRequests.peek();
        if (nextScheduledMeasurement == null) {
            return Long.MIN_VALUE;
        } else {
            return nextScheduledMeasurement.getNextCollection();
        }
    }

    /**
     * Returns the complete set of scheduled measurement collections.
     *
     * @return all measurement schedules
     */
    public synchronized Set<ScheduledMeasurementInfo> getNextScheduledSet() {
        ScheduledMeasurementInfo first = this.scheduledRequests.peek();
        if ((first == null) || (first.getNextCollection() > System.currentTimeMillis())) {
            return null;
        }

        Set<ScheduledMeasurementInfo> nextScheduledSet = new HashSet<ScheduledMeasurementInfo>();

        ScheduledMeasurementInfo next = this.scheduledRequests.peek();
        while ((next != null) && (next.getResourceId() == first.getResourceId())
            && (next.getNextCollection() == first.getNextCollection())) {
            nextScheduledSet.add(this.scheduledRequests.poll());
            next = this.scheduledRequests.peek();
        }

        return nextScheduledSet;
    }

    public synchronized void reschedule(Set<ScheduledMeasurementInfo> scheduledMeasurementInfos) {
        for (ScheduledMeasurementInfo scheduledMeasurement : scheduledMeasurementInfos) {
            // Iterate to next collection time
            scheduledMeasurement.setNextCollection(scheduledMeasurement.getNextCollection()
                + scheduledMeasurement.getInterval());
            this.scheduledRequests.offer(scheduledMeasurement);
        }
    }

    /**
     * Sends the given measurement report to the server, if this plugin container has server services that it can
     * communicate with.
     *
     * @param report
     */
    public void sendMeasurementReport(MeasurementReport report) {
        this.collectedMeasurements.addAndGet(report.getDataCount());
        this.sinceLastCollectedMeasurements.addAndGet(report.getDataCount());
        this.totalTimeCollecting.addAndGet(report.getCollectionTime());
        if (configuration.getServerServices() != null) {
            try {
                configuration.getServerServices().getMeasurementServerService().mergeMeasurementReport(report);
            } catch (Exception e) {
                LOG.warn("Failure to report measurements to server", e);
            }
        }
    }

    // -- MBean monitoring methods

    public long getMeasurementsCollected() {
        return this.collectedMeasurements.get();
    }

    public long getMeasurementsCollectedPerMinute() {
        long now = System.currentTimeMillis();

        // Make sure we track at least 60 seconds before resetting
        if ((now - this.sinceLastCollectedTime.get()) > 60000) {
            sinceLastCollectedTime.set(now);
            sinceLastCollectedMeasurements.set(0);
        }

        if ((now - sinceLastCollectedTime.get()) == 0) {
            return 0;
        }

        long collectionTimeInMinutes = (now - sinceLastCollectedTime.get()) / 1000L / 60L;
        if (collectionTimeInMinutes == 0) {
            return 0;
        }

        long ret = this.sinceLastCollectedMeasurements.get() / (collectionTimeInMinutes);
        return ret;
    }

    public long getCurrentlyScheduleMeasurements() {
        return this.scheduledRequests.size();
    }

    public long getTotalTimeCollectingMeasurements() {
        return this.totalTimeCollecting.get();
    }

    public long getLateCollections() {
        return lateCollections.get();
    }

    public MeasurementReport swapReport() {
        try {
            this.measurementLock.writeLock().lock();
            MeasurementReport previousReport = this.activeReport;

            this.activeReport = new MeasurementReport();

            return previousReport;
        } finally {
            this.measurementLock.writeLock().unlock();
        }
    }

    void incrementLateCollections(int count) {
        this.lateCollections.addAndGet(count);
    }

    void incrementFailedCollections(int count) {
        this.failedCollection.addAndGet(count);
    }

    public long getFailedCollections() {
        return failedCollection.get();
    }

    private static class CachedValue {
        private CachedValue(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        long timestamp;
        double value;
    }
}