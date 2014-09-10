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

package org.rhq.core.pc.measurement;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
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
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.agent.AgentServiceStreamRemoter;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Manage the scheduled process of measurement data collection, detection and sending across all plugins.
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

    private static final Random RANDOM = new Random();

    static final int FACET_METHOD_TIMEOUT = 30 * 1000; // 30 seconds

    static final Log LOG = LogFactory.getLog(MeasurementManager.class);

    private final ScheduledThreadPoolExecutor collectorThreadPool;
    private final ScheduledThreadPoolExecutor senderThreadPool;

    private final MeasurementSenderRunner measurementSenderRunner;
    private final MeasurementCollectorRunner measurementCollectorRunner;

    private final PluginContainerConfiguration configuration;

    private final PriorityQueue<ScheduledMeasurementInfo> scheduledRequests = new PriorityQueue<ScheduledMeasurementInfo>(
        10000);

    private final InventoryManager inventoryManager;

    private final Map<Integer, String> traitCache = new HashMap<Integer, String>();

    private final Map<Integer, CachedValue> perMinuteCache = new HashMap<Integer, CachedValue>();

    private volatile MeasurementReport activeReport = new MeasurementReport();

    private final ReentrantReadWriteLock measurementLock = new ReentrantReadWriteLock(true);

    // -- monitoring information
    private final AtomicLong collectedMeasurements = new AtomicLong(0);
    private final AtomicLong totalTimeCollecting = new AtomicLong(0);
    private final AtomicLong sinceLastCollectedMeasurements = new AtomicLong(0);
    private final AtomicLong sinceLastCollectedTime = new AtomicLong(System.currentTimeMillis());

    private final AtomicLong lateCollections = new AtomicLong(0);
    private final AtomicLong failedCollection = new AtomicLong(0);

    public MeasurementManager(PluginContainerConfiguration configuration, AgentServiceStreamRemoter streamRemoter,
        InventoryManager inventoryManager) {

        super(MeasurementAgentService.class, streamRemoter);

        this.configuration = configuration;
        this.inventoryManager = inventoryManager;

        if (configuration.isInsideAgent()) {
            int threadPoolSize = configuration.getMeasurementCollectionThreadPoolSize();
            collectorThreadPool = new ScheduledThreadPoolExecutor(threadPoolSize, new LoggingThreadFactory(
                COLLECTOR_THREAD_POOL_NAME, true));
            senderThreadPool = new ScheduledThreadPoolExecutor(2, new LoggingThreadFactory(SENDER_THREAD_POOL_NAME,
                true));
            measurementSenderRunner = new MeasurementSenderRunner(this);
            measurementCollectorRunner = new MeasurementCollectorRunner(this);
        } else {
            senderThreadPool = null;
            collectorThreadPool = null;
            measurementSenderRunner = null;
            measurementCollectorRunner = null;
        }
    }

    public void initialize() {
        LOG.info("Initializing Measurement Manager...");

        if (configuration.isStartManagementBean()) {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            try {
                server.registerMBean(this, new ObjectName(OBJECT_NAME));
            } catch (JMException e) {
                LOG.error("Unable to register MeasurementManagerMBean", e);
            }
        }

        if (configuration.isInsideAgent()) {
            long collectionInitialDelaySecs = configuration.getMeasurementCollectionInitialDelay();

            // Schedule the measurement sender to send measurement reports periodically.
            senderThreadPool.scheduleAtFixedRate(measurementSenderRunner, collectionInitialDelaySecs, 30,
                TimeUnit.SECONDS);
            // Schedule the measurement collector to collect metrics periodically, whenever there are one or more
            // metrics due to be collected.
            collectorThreadPool.schedule(new MeasurementCollectionRequester(), collectionInitialDelaySecs,
                TimeUnit.SECONDS);

            // Load persistent measurement schedules from the InventoryManager and reconstitute them.
            Resource platform = inventoryManager.getPlatform();
            if (platform == null) {
                throw new IllegalStateException("null platform");
            }
            reschedule(platform);
        }

        LOG.info("Measurement Manager initialized.");
    }

    class MeasurementCollectionRequester implements Runnable {
        @Override
        public void run() {
            try {
                while (!collectorThreadPool.isShutdown()) {
                    long next = getNextExpectedCollectionTime();
                    if (next == Long.MIN_VALUE) {
                        Thread.sleep(10000);
                    } else {
                        long delay = next - System.currentTimeMillis();
                        if (delay <= 0) {
                            //                  collectorThreadPool.execute(measurementCollectorRunner);
                            measurementCollectorRunner.call();
                        } else {
                            if (!collectorThreadPool.isShutdown()) {
                                Thread.sleep(delay);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Log nothing - if we got interrupted, it's probably because the PC is shutting down.
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                LOG.error("Measurement collection shutting down due to error", e);
            } finally {
                LOG.info("Shutting down measurement collection...");
            }
        }
    }

    private void reschedule(Resource resource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("In Reschedule for: " + resource);
        }

        int resourceId = resource.getId();

        if (resourceId != 0) {
            ResourceContainer container = this.inventoryManager.getResourceContainer(resourceId);
            if (container != null) {
                Set<MeasurementScheduleRequest> schedules = container.getMeasurementSchedule();
                if (schedules != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Rescheduling: " + resource.getId());
                    }
                    scheduleCollection(resourceId, schedules);
                }

                for (Resource child : this.inventoryManager.getContainerChildren(resource, container)) {
                    reschedule(child);
                }
            }
        }
    }

    public MeasurementReport getActiveReport() {
        return activeReport;
    }

    ReentrantReadWriteLock getLock() {
        return measurementLock;
    }

    /**
     * Check if the passed trait is new or has changed
     * @param  scheduleId
     * @param  traitValue
     *
     * @return true if the value is new or changed and should be included in the report
     */
    public boolean checkTrait(int scheduleId, String traitValue) {

        if (traitCache.containsKey(scheduleId)) {
            String historic = traitCache.get(scheduleId);
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

    /**
     * If you want to get a cached value of a trait, pass in its schedule ID.
     * This is useful if you don't care to obtain the latest-n-greated value of the trait,
     * and you want to avoid making a live call to the managed resource to obtain its value.
     * Note that if the trait is not yet cached, this will return null, and the caller will
     * be forced to make a live call to obtain the trait value, but at least this can help
     * avoid unnecessarily calling the live resource.
     *
     * @param scheduleId the schedule for the trait for a specific resource
     * @return the trait's cached value, <code>null</code> if not available
     */
    public String getCachedTraitValue(int scheduleId) {
        return traitCache.get(scheduleId);
    }

    public void perMinuteItizeData(MeasurementReport report) {
        Iterator<MeasurementDataNumeric> iter = report.getNumericData().iterator();
        while (iter.hasNext()) {
            MeasurementData d = iter.next();
            MeasurementDataNumeric numeric = (MeasurementDataNumeric) d;
            if (numeric.isPerMinuteCollection()) {
                Double perMinuteValue = updatePerMinuteMetric(numeric);
                if (perMinuteValue == null) {
                    // This is the first collection, don't return the value yet
                    iter.remove();
                } else {
                    // set the value to the transformed rate value
                    numeric.setValue(perMinuteValue);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        if (this.collectorThreadPool != null) {
            LOG.debug("Shutting down measurement collector thread pool...");
            PluginContainer.shutdownExecutorService(this.collectorThreadPool, true);
        }

        if (this.senderThreadPool != null) {
            LOG.debug("Shutting down measurement sender thread pool...");
            PluginContainer.shutdownExecutorService(this.senderThreadPool, true);
        }

        if (configuration.isStartManagementBean()) {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            try {
                server.unregisterMBean(new ObjectName(OBJECT_NAME));
            } catch (JMException e) {
                LOG.warn("Unable to unregister MeasurementManagerMBean", e);
            }
        }
    }

    /**
     * This remoted method allows the server to schedule a bunch of resources with one call.
     *
     * This method will update the set of {@link MeasurementSchedule}s in the agent.
     *
     * Use {@link #scheduleCollection(Set)} if you want to replace the existing ones.
     *
     * @param scheduleRequests
     */
    @Override
    public synchronized void updateCollection(Set<ResourceMeasurementScheduleRequest> scheduleRequests) {
        for (ResourceMeasurementScheduleRequest resourceRequest : scheduleRequests) {
            ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resourceRequest.getResourceId());
            if (resourceContainer != null) {
                // Update (not overwrite) measurement schedule data ...
                resourceContainer.updateMeasurementSchedule(resourceRequest.getMeasurementSchedules());
                // ... and then reschedule collection
                scheduleCollection(resourceRequest.getResourceId(), resourceRequest.getMeasurementSchedules());
                if (resourceRequest.getAvailabilitySchedule() != null) {
                    // Set availability schedule data  if present
                    // This method also triggers a reschedule of availability check
                    resourceContainer.setAvailabilitySchedule(resourceRequest.getAvailabilitySchedule());
                }
            } else {
                // This will happen when the server sends down schedules to an agent with a cleaned inventory
                // Its ok to skip these because the agent will request a reschedule once its been able to synchronize
                // and add these to inventory
                LOG.trace("Resource container was null - could not schedule collection for resource "
                    + resourceRequest.getResourceId());
            }
        }

        // TODO GH: Should I kick the pool or should I just go with the 30 second granularity on collections?
        // If I get much more granular then the server could end up with too many small reports from many agents
        // This may be another reason to have a separate sending mechanism from the collection mechanism.
        clearDuplicateSchedules();

    }

    /**
     * This remoted method allows the server to schedule a bunch of resources with one call.
     *
     * BE CAREFUL, as this will replace all existing schedules with the passed set.
     *
     * Use {@link #updateCollection(Set)} if you want to schedule additional {@link MeasurementSchedule}s
     *
     * @param scheduleRequests
     */
    @Override
    public synchronized void scheduleCollection(Set<ResourceMeasurementScheduleRequest> scheduleRequests) {
        for (ResourceMeasurementScheduleRequest resourceRequest : scheduleRequests) {
            ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resourceRequest.getResourceId());
            if (resourceContainer != null) {
                // Set measurement schedule data ...
                resourceContainer.setMeasurementSchedule(resourceRequest.getMeasurementSchedules());
                // ... and then reschedule collection
                scheduleCollection(resourceRequest.getResourceId(), resourceRequest.getMeasurementSchedules());
                // Set availability schedule data
                // This method also triggers a reschedule of availability check
                resourceContainer.setAvailabilitySchedule(resourceRequest.getAvailabilitySchedule());
            } else {
                // This will happen when the server sends down schedules to an agent with a cleaned inventory
                // It's ok to skip these because the agent will request a reschedule once its been able to synchronize
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
        // see BZ 751231 for why we delay the first collection
        if (configuration != null) {
            firstCollection += (configuration.getMeasurementCollectionInitialDelay() * 1000L);
        } else {
            firstCollection += 30000L;
        }

        for (MeasurementScheduleRequest request : requests) {
            ScheduledMeasurementInfo info = new ScheduledMeasurementInfo(request, resourceId);

            info.setNextCollection(firstCollection);

            this.scheduledRequests.remove(info);

            // Don't add it if collection is disabled for this resource
            if (info.isEnabled()) {
                this.scheduledRequests.offer(info);
            }
        }
    }

    @Override
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
                LOG.debug("Found duplicate schedule - will remove it: " + info);
                iter.remove();
            } else {
                set.add(info.getScheduleId());
            }
        }
    }

    // spinder 12/16/11. BZ 760139. Modified to return empty sets instead of 'null' even for erroneous conditions.
    //         Server side logging or erroneous runtime conditions still occurs, but callers to getRealTimeMeasurementValues
    //         won't have to additionally check for null values now. This is a safe and better pattern.
    public Set<MeasurementData> getRealTimeMeasurementValue(int resourceId, Set<MeasurementScheduleRequest> requests) {
        if (requests.size() == 0) {
            // There's no need to even call getValues() on the ResourceComponent if the list of metric names is empty.
            return Collections.emptySet();
        }
        MeasurementFacet measurementFacet;
        ResourceContainer resourceContainer = inventoryManager.getResourceContainer(resourceId);
        if (resourceContainer == null) {
            LOG.warn("Can not get resource container for resource with id " + resourceId);
            return Collections.emptySet();
        }
        Resource resource = resourceContainer.getResource();
        ResourceType resourceType = resource.getResourceType();
        if (resourceType.getMetricDefinitions().isEmpty())
            return Collections.emptySet();

        try {
            measurementFacet = ComponentUtil.getComponent(resourceId, MeasurementFacet.class, FacetLockType.READ,
                FACET_METHOD_TIMEOUT, true, true, true);
        } catch (Exception e) {
            LOG.warn("Cannot get measurement facet for Resource [" + resourceId + "]. Cause: " + e);
            return Collections.emptySet();
        }

        MeasurementReport report = new MeasurementReport();
        for (MeasurementScheduleRequest request : requests) {
            request.setEnabled(true);
        }

        try {
            measurementFacet.getValues(report, Collections.unmodifiableSet(requests));
        } catch (Throwable t) {
            LOG.error("Could not get measurement values", t);
            return Collections.emptySet();
        }

        Iterator<MeasurementDataNumeric> iterator = report.getNumericData().iterator();
        while (iterator.hasNext()) {
            MeasurementDataNumeric numeric = iterator.next();
            if (numeric.isPerMinuteCollection()) {
                CachedValue currentValue = perMinuteCache.get(numeric.getScheduleId());
                if (currentValue == null) {
                    iterator.remove();
                } else {
                    numeric.setValue(calculatePerMinuteValue(numeric, currentValue));
                }
            }
        }

        Set<MeasurementData> values = new HashSet<MeasurementData>();
        values.addAll(report.getNumericData());
        values.addAll(report.getTraitData());
        return values;
    }

    @Override
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

    /**
     * Reschedules the given measurement schedules so the next collection occurs in the future.
     * The next collection will be pushed out by the number of seconds of the schedule's collection
     * interval.
     *
     * @param scheduledMeasurementInfos the schedules to reschedule
     */
    public synchronized void reschedule(Set<ScheduledMeasurementInfo> scheduledMeasurementInfos) {

        for (ScheduledMeasurementInfo scheduledMeasurement : scheduledMeasurementInfos) {
            long interval = scheduledMeasurement.getInterval();
            scheduledMeasurement.setNextCollection(scheduledMeasurement.getNextCollection() + interval);
            this.scheduledRequests.offer(scheduledMeasurement);
        }
    }

    /**
     * Reschedules the given [late] measurement schedules so the next collection occurs in the future, and with
     * some randomization to the nextCollection times. Late collections are those that were not actually
     * performed due to collection falling behind.  The nextCollection will be set to:
     * <pre>
     * Now + 30s + [1..Interval]
     *
     * Where [1..Interval] is some random number of seconds no lower that 1 and no higher than the standard interval
     * for the measurement.
     * </pre>
     *
     * @param scheduledMeasurementInfos the late schedules to reschedule
     */
    synchronized void rescheduleLateCollections(Set<ScheduledMeasurementInfo> scheduledMeasurementInfos) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Rescheduling [" + scheduledMeasurementInfos.size() + "] late collections: "
                + scheduledMeasurementInfos);
        }

        long now = System.currentTimeMillis();
        for (ScheduledMeasurementInfo scheduledMeasurement : scheduledMeasurementInfos) {
            // push out 30s from the current time to at least get a minimal 30s interval
            long nextCollection = now + 30000L;

            // then add a random number of seconds [1..interval].  This will spread out the next collection times to
            // hopefully avoid the "hot-spot" that caused us to fall behind.
            long interval = scheduledMeasurement.getInterval();
            int maxRandomInterval = (int) (interval / 1000L); // exclusive upper bound
            long randomInterval = ((RANDOM.nextInt(maxRandomInterval) + 1) * 1000L);

            nextCollection += randomInterval;

            if (LOG.isTraceEnabled()) {
                LOG.trace("Rescheduling next collection of [" + scheduledMeasurement + "] for "
                    + new Date(nextCollection));
            }

            scheduledMeasurement.setNextCollection(nextCollection);
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

    private Double updatePerMinuteMetric(MeasurementDataNumeric numeric) {
        CachedValue previousValue = this.perMinuteCache.get(numeric.getScheduleId());
        this.perMinuteCache.put(numeric.getScheduleId(), new CachedValue(numeric.getTimestamp(), numeric.getValue()));
        return calculatePerMinuteValue(numeric, previousValue);
    }

    private Double calculatePerMinuteValue(MeasurementDataNumeric numeric, CachedValue currentValue) {
        Double perMinuteValue = null;
        if (currentValue != null) {
            long timeDifference = numeric.getTimestamp() - currentValue.timestamp;
            perMinuteValue = (60000D / timeDifference) * (numeric.getValue() - currentValue.value);
            if (numeric.getRawNumericType() == NumericType.TRENDSDOWN)
                perMinuteValue *= -1D; // Multiply by -1, so per-minute value is positive.
            if (perMinuteValue < 0)
                // A negative value means the raw metric must have been reset, which means we can't accurately
                // calculate a per-minute value this time around; return null to indicate this.
                perMinuteValue = null;
        }
        return perMinuteValue;
    }

    @Override
    public Map<String, Object> getMeasurementScheduleInfoForResource(int resourceId) {
        Map<String, Object> results = null;

        for (ScheduledMeasurementInfo info : new PriorityQueue<ScheduledMeasurementInfo>(scheduledRequests)) {
            if (info.getResourceId() == resourceId) {
                if (results == null) {
                    results = new HashMap<String, Object>();
                }
                String scheduleId = String.valueOf(info.getScheduleId());
                String interval = String.valueOf(info.getInterval()) + "ms";
                results.put(scheduleId, interval);
            }
        }

        return results;
    }

    /**
     * Given the name of a trait, this will find the value of that trait for the given resource.
     *
     * @param container the container of the resource whose trait value is to be obtained
     * @param traitName the name of the trait whose value is to be obtained
     *
     * @return the value of the trait, or <code>null</code> if unknown
     */
    public String getTraitValue(ResourceContainer container, String traitName) {
        Integer traitScheduleId = null;
        Set<MeasurementScheduleRequest> schedules = container.getMeasurementSchedule();
        for (MeasurementScheduleRequest schedule : schedules) {
            if (schedule.getName().equals(traitName)) {
                if (schedule.getDataType() != DataType.TRAIT) {
                    throw new IllegalArgumentException("Measurement named [" + traitName + "] for resource ["
                        + container.getResource().getName() + "] is not a trait, it is of type ["
                        + schedule.getDataType() + "]");
                }
                traitScheduleId = schedule.getScheduleId();
            }
        }
        if (traitScheduleId == null) {
            throw new IllegalArgumentException("There is no trait [" + traitName + "] for resource ["
                + container.getResource().getName() + "]");
        }

        String traitValue = getCachedTraitValue(traitScheduleId);
        if (traitValue == null) {
            // the trait hasn't been collected yet, so it isn't cached. We need to get its live value
            Set<MeasurementScheduleRequest> requests = new HashSet<MeasurementScheduleRequest>();
            requests.add(new MeasurementScheduleRequest(MeasurementScheduleRequest.NO_SCHEDULE_ID, traitName, 0, true,
                DataType.TRAIT));
            Set<MeasurementData> dataset = getRealTimeMeasurementValue(container.getResource().getId(), requests);
            if (dataset.size() == 1) {
                Object value = dataset.iterator().next().getValue();
                if (value != null) {
                    traitValue = value.toString();
                }
            } else if (dataset.isEmpty()) {
                // so the agent doesn't have a cached value of the trait an the resource seems to be either down
                // or unable to collect the trait.
                // Let's try asking the server for the last known value
                MeasurementDataTrait value = configuration.getServerServices().getMeasurementServerService()
                    .getLastKnownTraitValue(traitScheduleId);

                if (value != null) {
                    traitValue = value.getValue();
                }
            } else {
                throw new IllegalStateException(
                    "Asked for value of trait " + traitName + " on resource " + container.getResource() +
                        " but got more than one value back. This is unexpected.");
            }
        }

        return traitValue;
    }

    // -- MBean monitoring methods

    @Override
    public long getMeasurementsCollected() {
        return this.collectedMeasurements.get();
    }

    @Override
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

    @Override
    public long getCurrentlyScheduleMeasurements() {
        return this.scheduledRequests.size();
    }

    @Override
    public long getTotalTimeCollectingMeasurements() {
        return this.totalTimeCollecting.get();
    }

    @Override
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

    @Override
    public long getFailedCollections() {
        return failedCollection.get();
    }

    private static class CachedValue {
        CachedValue(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        long timestamp;
        double value;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

}
