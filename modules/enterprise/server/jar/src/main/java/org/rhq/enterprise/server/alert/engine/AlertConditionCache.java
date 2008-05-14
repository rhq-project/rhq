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
package org.rhq.enterprise.server.alert.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.alert.engine.jms.CachedConditionProducerLocal;
import org.rhq.enterprise.server.alert.engine.mbean.AlertConditionCacheMonitor;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;
import org.rhq.enterprise.server.alert.engine.model.AlertConditionOperator;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityCacheElement;
import org.rhq.enterprise.server.alert.engine.model.EventCacheElement;
import org.rhq.enterprise.server.alert.engine.model.InvalidCacheElementException;
import org.rhq.enterprise.server.alert.engine.model.MeasurementBaselineCacheElement;
import org.rhq.enterprise.server.alert.engine.model.NumericDoubleCacheElement;
import org.rhq.enterprise.server.alert.engine.model.OutOfBoundsCacheElement;
import org.rhq.enterprise.server.alert.engine.model.ResourceOperationCacheElement;
import org.rhq.enterprise.server.alert.engine.model.StringCacheElement;
import org.rhq.enterprise.server.alert.engine.model.UnsupportedAlertConditionOperatorException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementConstants;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementNotFoundException;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.operation.OperationDefinitionNotFoundException;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The singleton that contains the actual caches for the alert subsystem.
 * 
 * @author Joseph Marques
 * @author John Mazzittelli
 */
public class AlertConditionCache {
    private static final Log log = LogFactory.getLog(AlertConditionCache.class);

    /**
     * The actual cache - this is a singleton.
     */
    private static final AlertConditionCache instance = new AlertConditionCache();

    /**
     * Will be used to determine if the internal state of this cache is valid.
     */
    private Boolean validCache;

    /**
     * For concurrency control into the cache.
     */
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * For locking out threads when baseline calculations are occurring. When this is not null, this latch must be open
     * before any thread can access baseline related cache data. If null, there is no baseline calcs happening so
     * threads are free to access baseline cache at will (but a RW lock must still be obtained).
     */
    private CountDownLatch baselineCalcLatch = null;

    private enum CacheName {
        MeasurementDataCache, //
        MeasurementTraitCache, //
        MeasurementBaselineMap, //
        OutOfBoundsBaselineMap, //
        ResourceOperationCache, //
        AvailabilityCache, //
        Inverse;
    }

    /*
     * Instead of coming up with an overly complicated uber-map that can hold everything in the world, separate the
     * cache out so that cache management and access is simplified.  This allows for each cache to potentially take on a
     * different structure, a shape that would be most appropriate to the subsystem of information it is supposed to be
     * modeling and caching.
     *
     * TODO: jmarques - put these into a pojocache for fine-grained, cluster-wide diffs TODO: jmarques - think about
     * resourceGroup-level alert processing; not touching this topic right now
     */

    /*
     * structure: 
     *      map< measurementScheduleId, list < NumericDoubleCacheElement > >   (MeasurementDataNumeric cache)
     *      map< measurementScheduleId, list < StringCacheElement > >          (MeasurementDataTrait   cache)
     *
     * algorithm: 
     *      Use the measurementScheduleId to find a list of cached conditions for measurement numerics.
     */
    private Map<Integer, List<NumericDoubleCacheElement>> measurementDataCache;
    private Map<Integer, List<StringCacheElement>> measurementTraitCache;

    /*
     * structure: 
     *      map< baselineId, list< baselineCacheElement > >           
     *      map< baselineId, tuple< lowCacheElement, highCacheElement > >
     *
     * algorithm: 
     *      This map exists solely to assist with the cache maintenance process.  When a measurement
     *      baseline value changes, the maint process will come along and look up all measurementDataCache           
     *      entries that are affected by this change.  The process will continue by walking each of the           
     *      elements and updating the value.
     *
     *      Thus, by design, the processing thread that comes along and checks the measurementDataCache to see if
     *      any alert conditions have become true won't (and shouldn't) be able to tell the difference anymore
     *      between elements that refer to absolute value predicates and those that refer to calculated baselines.
     *
     *      While the measurementBaselineMap calculates the dataCache element off of the alertCondition threshold
     *      value, the outOfBoundsBaselineMap will have two entries for every baseline - one for "< 95%" and one
     *      for "> 105%".
     */
    private Map<Integer, List<MeasurementBaselineCacheElement>> measurementBaselineMap;
    private Map<Integer, Tuple<OutOfBoundsCacheElement, OutOfBoundsCacheElement>> outOfBoundsBaselineMap;

    /*
     * structure: 
     *      map< resourceId, map< operationDefinitionId, list< ResourceOperationCacheElement > > >
     *
     * algorithm: 
     *      Finding matching conditions are simple because a ResourceOperationHistory element already contains
     *      references back to its OperationDefinition element as well as the Resource it was executed against.
     *
     *      Using these two pieces of information, it's possible to lookup the list of states that should cause a
     *      firing.
     */
    private Map<Integer, Map<Integer, List<ResourceOperationCacheElement>>> resourceOperationCache;

    /*
     * structure: 
     *      map< resourceId, list< AvailabilityCacheElement > > >
     *
     * algorithm: 
     *      When an Availability object comes across the line, use the Resource object associated/attached with it
     *      to get the list of cache elements representing conditions created against this resource's availability.
     */
    private Map<Integer, List<AvailabilityCacheElement>> availabilityCache;

    /*
     * structure:
     *      map< resourceId, list< EventCacheElement > >
     * 
     * algorithm:
     *      When an EventReport comes across the line, use the Resource object it is coming from to get the list
     *      of cache elements representing conditions created against that resource's list of incoming Events  
     */
    private Map<Integer, List<EventCacheElement>> eventsCache;

    /*
     * structure: 
     *      map< alertConditionId, list< tuple< AbstractCacheElement, list< AbstractCacheElement > > > >
     *
     * algorithm: 
     *      Updating an AlertDefinition is an expensive operation.  If the definition is deleted or disabled, all of
     *      the corresponding cache elements that were constructed against any of the definition's nested conditions
     *      now need to be removed.  This would normally require iterating over the entirety of the cache.  This may
     *      not seem so bad for an individual update, but let's pretend that the user wants to disable all alerts on
     *      a particular resource (perhaps for a maintenance window) or, worse, disable all alerts across several
     *      resources.  Now we have to iterate over the ENTIRE cache multiple times, once for each AlertDefinition
     *      updated.  This is unacceptable.
     *
     *      To remedy this, we keep an inverse map to mark the locations where particular AlertCondition ids have been
     *      used to create cache elements.  This way, when an AlertCondition is updated, we can go to this map to learn
     *      which cache elements were created from this condition as well as their precise location(s) across all
     *      internal cache structures.
     *
     * note: 
     *      A list of tuples was used as the type for the map's value objects because a nested Map was insufficient.
     *      The cache for measurement data uses multiple lists, and that particular implementation actually references
     *      the same NumericDoubleCacheElement in these multiple lists.  Thus, a map would have been insufficient
     *      because it would not have been able to represent both lists that the cache element was a member of.
     *      A list of tuples can do this.
     *
     *      Furthermore, a map, even if it could have represented the data properly, wouldn't have been the most
     *      appropriate structure since the value objects in this structure need to be iterated over in full.  In
     *      other words, when this map is used to access bookkeeping information about a particular alert condition id,
     *      the entire list of corresponding bookkeeping entries needs to be visited; so there would be no benefit to
     *      using the nested map anyway.
     */
    private Map<Integer, List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>> inverseAlertConditionMap;

    /*
     * The SLBSs used by the cache.
     */
    private AlertConditionManagerLocal alertConditionManager;
    private AlertDefinitionManagerLocal alertDefinitionManager;
    private AvailabilityManagerLocal availabilityManager;
    private MeasurementBaselineManagerLocal measurementBaselineManager;
    private MeasurementDataManagerLocal measurementDataManager;
    private MeasurementScheduleManagerLocal measurementScheduleManager;
    private OperationManagerLocal operationManager;
    private SubjectManagerLocal subjectManager;
    private CachedConditionProducerLocal cachedConditionProducer;

    private AlertConditionCache() {
        measurementDataCache = new HashMap<Integer, List<NumericDoubleCacheElement>>();
        measurementTraitCache = new HashMap<Integer, List<StringCacheElement>>();
        measurementBaselineMap = new HashMap<Integer, List<MeasurementBaselineCacheElement>>();
        outOfBoundsBaselineMap = new HashMap<Integer, Tuple<OutOfBoundsCacheElement, OutOfBoundsCacheElement>>();
        resourceOperationCache = new HashMap<Integer, Map<Integer, List<ResourceOperationCacheElement>>>();
        availabilityCache = new HashMap<Integer, List<AvailabilityCacheElement>>();
        eventsCache = new HashMap<Integer, List<EventCacheElement>>();
        inverseAlertConditionMap = new HashMap<Integer, List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>>();

        alertConditionManager = LookupUtil.getAlertConditionManager();
        alertDefinitionManager = LookupUtil.getAlertDefinitionManager();
        availabilityManager = LookupUtil.getAvailabilityManager();
        measurementBaselineManager = LookupUtil.getMeasurementBaselineManager();
        measurementDataManager = LookupUtil.getMeasurementDataManager();
        measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
        operationManager = LookupUtil.getOperationManager();
        subjectManager = LookupUtil.getSubjectManager();
        cachedConditionProducer = LookupUtil.getCachedConditionProducerLocal();

        loadCaches();
    }

    public static AlertConditionCache getInstance() {
        return instance;
    }

    /**
     * This method is used to do the initial loading from the backing store. It could also be used "refresh" the cache
     * and make sure that it really does mirror what's persisted, even though it could be a rather expensive operation.
     * This method will query the backing store for all conditions on all AlertDefinitions, and translate them
     * on-the-fly to the cache's more efficient, internal representation of a condition. It is this translation, aside
     * from having to dump the existing contents of the cache, that could make it undesirable to call during regular
     * operation of the server. This problem gets even worse when we're talking about clustering the server-side of
     * things, at which point all nodes in the server cluster need to stabilize with the new information before alerts
     * processing can continue. Nonetheless, this method was made public / provided as part of this interface so as to
     * enable us to squirrel away a special "refresh" button somewhere in the UI just in case this cache ever gets
     * corrupted for some reason; the support person could simply tell the customer to press this magic button and,
     * after a pause, it would bring things back to a stable, ready state.
     *
     * @return the number of conditions that re/loaded from the backing store
     */
    public AlertConditionCacheStats loadCaches() {
        rwLock.writeLock().lock();

        waitForBaselineLatchToOpen(rwLock.writeLock());

        // this load method assumes the cache is totally clean; let's make sure that is true
        clearCaches();

        try {
            log.info("Loading Alert Condition Caches...");

            AlertConditionCacheStats stats = new AlertConditionCacheStats();

            Subject overlord = subjectManager.getOverlord();
            List<AlertDefinition> alertDefinitions = alertDefinitionManager
                .getAllAlertDefinitionsWithConditions(overlord);

            for (AlertDefinition alertDefinition : alertDefinitions) {
                /* 
                 * don't insert recovery alerts initially, they should only be added when the definition they're 
                 * recovering for triggers an alert; which is why this check is in the loadCaches() method instead
                 * of at the top of body of the insertAlertDefinition() method
                 * 
                 * be defensive about this: don't assume that the upstream process didn't allow null into the mix
                 */
                if (alertDefinition.getRecoveryId() != null && alertDefinition.getRecoveryId() != 0) {
                    continue;
                }

                /*
                 * deleted alertDefinitions should never come back from the alertDefinitionManager methods, so don't
                 * check for that
                 */
                if (alertDefinition.getEnabled() == true) {
                    insertAlertDefinition(overlord, alertDefinition, stats);
                }
            }

            // page thru all dynamic baselines
            int rowsProcessed = 0;
            PageControl pc = new PageControl();
            pc.setPageNumber(0);
            pc.setPageSize(1000); // baseline composites are small so we can grab alot; use the setter, constructor limits this to 100

            while (true) {
                PageList<MeasurementBaselineComposite> baselines = measurementBaselineManager
                    .getAllDynamicMeasurementBaselines(overlord, pc);

                if (baselines.size() <= 0) {
                    break; // didn't get any rows back, must not have any data or no more rows left to process
                }

                for (MeasurementBaselineComposite baseline : baselines) {
                    // don't insert borked baselines: only when min/max is correctly non-null, non-NAN, non-INF
                    if (isInvalidDouble(baseline.getMin()) || isInvalidDouble(baseline.getMax())) {
                        continue; // process the next baseline
                    }

                    // safe auto-unboxing here, because min/max both guaranteed to be valid by this point
                    insertOutOfBoundsBaseline(baseline.getId(), baseline.getMin(), baseline.getMax(), baseline
                        .getScheduleId(), stats);
                }

                rowsProcessed += baselines.size();
                if (rowsProcessed >= baselines.getTotalSize()) {
                    break; // we've processed all data, we can stop now
                }

                pc.setPageNumber(pc.getPageNumber() + 1);
            }

            log.info("Loaded Alert Condition Caches..." + stats);

            return stats;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * This will empty all internal caches. Be very careful calling this method as it will make all caches out of sync
     * from the database data. You'll probably want to call {@link #loadCaches()} after making a call to this method.
     */
    public void clearCaches() {
        rwLock.writeLock().lock();

        waitForBaselineLatchToOpen(rwLock.writeLock());

        try {
            measurementDataCache.clear();
            measurementTraitCache.clear();
            measurementBaselineMap.clear();
            outOfBoundsBaselineMap.clear();
            resourceOperationCache.clear();
            availabilityCache.clear();
            inverseAlertConditionMap.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * This method should be called when the baseline auto-calculation is about to do its thing. When this happens, this
     * cache locks out all access of anything baseline related until {@link #afterBaselineCalculation()} is called.
     */
    public void beforeBaselineCalculation() {
        rwLock.writeLock().lock();

        try {
            if (baselineCalcLatch != null) {
                // hmmm, why are we being called? we were already called and the after method wasn't called yet.
                // lets call after() now and restart a new latch
                afterBaselineCalculation();
            }

            baselineCalcLatch = new CountDownLatch(1);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * This method should be called after the baseline auto-calculation is finished. When this happens, all threads
     * previously locked out waiting for the baseline related caches are free to access them normally.
     *
     * <p>This method will reload the cache fully from the database while all other threads are locked out. This ensures
     * the cache can reload itself and get all the new baselines without other threads attempting to get cache
     * information while it is still being loaded.
     *
     * <p>TODO: this before/afterBaselineCalculation stuff has to go - we need a better runtime baseline calculation
     * algorithm that doesn't force us to reload the entire cache everytime we do it.
     */
    public void afterBaselineCalculation() {
        rwLock.writeLock().lock();

        try {
            if (baselineCalcLatch != null) {
                CountDownLatch latch = baselineCalcLatch;
                baselineCalcLatch = null;
                latch.countDown(); // release the hounds!
            }

            // TODO: I'm cheating here - i really dont't want to, but we have to load because
            //       we need to also clear due to baseline calc getting all new baseline IDs.
            //       we need to rewrite this whole baseline calc thing, so this all probably
            //       all goes away, especially if we use native bulk update and not
            //       bulk delete/insert (since baseline IDs remain intact and thus the cache
            //       doesn't break)
            loadCaches();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public AlertConditionCacheStats checkConditions(MeasurementData... measurementData) {
        if ((measurementData == null) || (measurementData.length == 0)) {
            return new AlertConditionCacheStats();
        }

        rwLock.readLock().lock();

        waitForBaselineLatchToOpen(rwLock.readLock());

        try {
            AlertConditionCacheStats stats = new AlertConditionCacheStats();

            for (MeasurementData datum : measurementData) {
                int scheduleId = datum.getScheduleId();

                if (datum instanceof MeasurementDataNumeric) {
                    List<NumericDoubleCacheElement> cacheElements = lookupMeasurementDataCacheElements(scheduleId);

                    processCacheElements(cacheElements, ((MeasurementDataNumeric) datum).getValue(), datum
                        .getTimestamp(), stats);
                } else if (datum instanceof MeasurementDataTrait) {
                    List<StringCacheElement> cacheElements = lookupMeasurementTraitCacheElements(scheduleId);

                    processCacheElements(cacheElements, ((MeasurementDataTrait) datum).getValue(),
                        datum.getTimestamp(), stats);
                } else {
                    log.error(getClass().getSimpleName() + " does not support " + "checking conditions against "
                        + datum.getClass().getSimpleName() + " types");
                }
            }

            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementMeasurementProcessingTime(stats.getAge());
            log.debug("Check Measurements[size=" + measurementData.length + "] - " + stats);
            return stats;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public AlertConditionCacheStats checkConditions(OperationHistory operationHistory) {
        rwLock.readLock().lock();

        try {
            AlertConditionCacheStats stats = new AlertConditionCacheStats();

            if (operationHistory instanceof ResourceOperationHistory) {
                ResourceOperationHistory resourceOperationHistory = (ResourceOperationHistory) operationHistory;

                Resource resource = resourceOperationHistory.getResource();
                OperationDefinition operationDefinition = resourceOperationHistory.getOperationDefinition();
                OperationRequestStatus operationStatus = resourceOperationHistory.getStatus();

                List<ResourceOperationCacheElement> cacheElements = lookupResourceOperationHistoryCacheElements(
                    resource.getId(), operationDefinition.getId());

                processCacheElements(cacheElements, operationStatus, resourceOperationHistory.getModifiedTime(), stats);
            } else {
                log.debug(getClass().getSimpleName() + " does not support checking conditions against "
                    + operationHistory.getClass().getSimpleName() + " types");
            }

            AlertConditionCacheMonitor.getMBean().incrementOperationCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementOperationProcessingTime(stats.getAge());
            log.debug("Check OperationHistory[size=1] - " + stats);
            return stats;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public AlertConditionCacheStats checkConditions(EventSource source, Event... events) {
        if ((events == null) || (events.length == 0)) {
            return new AlertConditionCacheStats();
        }

        rwLock.readLock().lock();

        try {
            AlertConditionCacheStats stats = new AlertConditionCacheStats();

            Resource resource = source.getResource();
            List<EventCacheElement> cacheElements = lookupEventCacheElements(resource.getId());

            for (Event event : events) {
                processCacheElements(cacheElements, event.getSeverity(), event.getTimestamp().getTime(), stats, event
                    .getDetail());
            }

            AlertConditionCacheMonitor.getMBean().incrementEventCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementEventProcessingTime(stats.getAge());
            log.debug("Check Events[size=" + events.length + "] - " + stats);
            return stats;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public AlertConditionCacheStats checkConditions(Availability... availabilities) {
        if ((availabilities == null) || (availabilities.length == 0)) {
            return new AlertConditionCacheStats();
        }

        rwLock.readLock().lock();

        try {
            AlertConditionCacheStats stats = new AlertConditionCacheStats();

            for (Availability availability : availabilities) {
                Resource resource = availability.getResource();
                AvailabilityType availabilityType = availability.getAvailabilityType();

                List<AvailabilityCacheElement> cacheElements = lookupAvailabilityCacheElements(resource.getId());

                processCacheElements(cacheElements, availabilityType, availability.getStartTime().getTime(), stats);
            }

            AlertConditionCacheMonitor.getMBean().incrementAvailabilityCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementAvailabilityProcessingTime(stats.getAge());
            log.debug("Check Availability[size=" + availabilities.length + "] - " + stats);
            return stats;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public AlertConditionCacheStats updateConditions(Resource deletedResource) {
        rwLock.writeLock().lock();

        waitForBaselineLatchToOpen(rwLock.writeLock());

        try {
            AlertConditionCacheStats stats = new AlertConditionCacheStats();

            int doomedResourceId = deletedResource.getId();

            /*
             * Deleting a resource removes any alert conditions keyed off of the status of one of its executed
             * operations
             */
            Map<Integer, List<ResourceOperationCacheElement>> resourceOperationMap = resourceOperationCache
                .get(doomedResourceId);
            if (resourceOperationMap != null) {
                for (Map.Entry<Integer, List<ResourceOperationCacheElement>> entry : resourceOperationMap.entrySet()) {
                    stats.deleted += entry.getValue().size();
                }

                resourceOperationCache.remove(doomedResourceId);
            }

            /*
             * Deleting a resource also removes any alert conditions keyed off of the its measurementSchedules as well
             * as its baseline calcuations
             */
            List<MeasurementBaseline> doomedMeasurementBaselines = measurementBaselineManager
                .findBaselinesForResource(deletedResource);
            if (doomedMeasurementBaselines != null) {
                for (MeasurementBaseline doomedBaseline : doomedMeasurementBaselines) {
                    int doomedBaselineId = doomedBaseline.getId();
                    int doomedScheduleId = doomedBaseline.getSchedule().getId();

                    stats.deleted += measurementDataCache.remove(doomedScheduleId).size();

                    /*
                     * Don't forget to remove the bookkeeping elements from bookkeeping maps; we don't need to remove
                     * anything further from the measurementDataCache because the above removal does it all; since the
                     * measurementDataCache holds references to:
                     *
                     * - absolute value conditions  - baseline conditions  - OOB conditions
                     *
                     * and since they are ALL keyed off of the scheduleId, a single removal from the measurmentDataCache
                     * will catch all of the "real" cache elements; all that's left to do is remove the cache element
                     * references from the bookkeeping maps, and that memory will be reclaimed.
                     */
                    measurementBaselineMap.remove(doomedBaselineId);
                    outOfBoundsBaselineMap.remove(doomedBaselineId);
                }
            }

            return stats;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // this could potentially take really long
    public AlertConditionCacheStats updateConditions(List<MeasurementBaselineComposite> measurementBaselines) {
        if ((measurementBaselines == null) || (measurementBaselines.size() == 0)) {
            return new AlertConditionCacheStats();
        }

        rwLock.writeLock().lock();

        waitForBaselineLatchToOpen(rwLock.writeLock());

        AlertConditionCacheStats stats = new AlertConditionCacheStats();

        try {
            for (MeasurementBaselineComposite measurementBaseline : measurementBaselines) {
                int baselineId = measurementBaseline.getId();

                List<MeasurementBaselineCacheElement> cacheElements = measurementBaselineMap.get(baselineId);
                if (cacheElements != null) {
                    // There are AlertConditions associated with this baseline, update them
                    // It is rare we get in here; only for alerts that are defined for watching baseline violations
                    for (MeasurementBaselineCacheElement cacheElement : cacheElements) {
                        int alertConditionId = cacheElement.getAlertConditionTriggerId();
                        AlertCondition alertCondition = alertConditionManager.getAlertConditionById(alertConditionId);

                        Double threshold = alertCondition.getThreshold();
                        String optionStatus = cacheElement.getOption();
                        Double newCalculatedValue = getCalculatedBaselineValue(alertConditionId, measurementBaseline,
                            optionStatus, threshold);

                        cacheElement.setAlertConditionValue(newCalculatedValue);

                        stats.updated++;
                    }
                }

                // don't insert borked baselines: only when min/max is correctly non-null, non-NAN, non-INF
                if (isInvalidDouble(measurementBaseline.getMin()) || isInvalidDouble(measurementBaseline.getMax())) {
                    continue; // process the next baseline
                }

                // safe auto-unboxing here, because min/max both guaranteed to be valid by this point
                double baselineMin = measurementBaseline.getMin();
                double baselineMax = measurementBaseline.getMax();

                Tuple<OutOfBoundsCacheElement, OutOfBoundsCacheElement> oobTuple = outOfBoundsBaselineMap
                    .get(baselineId);
                if (oobTuple == null) {
                    insertOutOfBoundsBaseline(measurementBaseline.getId(), baselineMin, baselineMax,
                        measurementBaseline.getScheduleId(), stats);
                } else {
                    double lowOOB = baselineMin * MeasurementConstants.LOW_OOB_THRESHOLD;
                    double highOOB = baselineMax * MeasurementConstants.HIGH_OOB_THRESHOLD;

                    // when dealing with negative numbers, the LOW_OOB_THRESHOLD will create a **larger** lowOOB than
                    // the highOOB...so, after multiplying by the thresholds, swap if necessary to correct the issue
                    if (lowOOB > highOOB) {
                        double temp = highOOB;
                        highOOB = lowOOB;
                        lowOOB = temp;
                    }

                    // auto-boxing is always safe
                    oobTuple.lefty.setAlertConditionValue(lowOOB);
                    oobTuple.righty.setAlertConditionValue(highOOB);

                    // stats is only used for *real* caches, not bookkeeping caches
                }
            }
        } finally {
            // let's defensively assume that we've somehow messed 
            // up the internal state and reset our valid switch
            validCache = null;

            rwLock.writeLock().unlock();
        }
        return stats;
    }

    public AlertConditionCacheStats updateConditions(AlertDefinition alertDefinition,
        AlertDefinitionEvent alertDefinitionEvent) {
        rwLock.writeLock().lock();

        waitForBaselineLatchToOpen(rwLock.writeLock());

        try {
            AlertConditionCacheStats stats = new AlertConditionCacheStats();

            if ((alertDefinitionEvent == AlertDefinitionEvent.CREATED)
                || (alertDefinitionEvent == AlertDefinitionEvent.ENABLED)) {
                // for CREATED, this call expects the alertDefinition to be persisted and have valid ids for the nested alertConditions
                insertAlertDefinition(subjectManager.getOverlord(), alertDefinition, stats);
            } else if ((alertDefinitionEvent == AlertDefinitionEvent.DELETED)
                || (alertDefinitionEvent == AlertDefinitionEvent.DISABLED)) {
                // disabling an alert, remove elements from cache
                removeAlertDefinition(alertDefinition, stats);
            } else {
                throw new AlertConditionCacheManagerException(getClass().getSimpleName() + " does not support "
                    + "updating conditions against " + alertDefinitionEvent.getClass().getSimpleName() + " types");
            }

            return stats;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Call this method from a thread that already has a read or write lock acquired but needs to wait for baseline
     * calculations to finish.
     *
     * @param lock the lock the calling thread currently owns
     */
    private void waitForBaselineLatchToOpen(Lock lock) {
        while (baselineCalcLatch != null) {
            CountDownLatch latch = baselineCalcLatch; // remember this, because once we unlock, baselineCalcLatch might go null on us

            lock.unlock();

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for baseline calcs to finish");
            } finally {
                lock.lock();
            }
        }
    }

    private <T extends AbstractCacheElement<S>, S> void processCacheElements(List<T> cacheElements, S providedValue,
        long timestamp, AlertConditionCacheStats stats, Object... extraParams) {
        if (cacheElements == null) {
            return; // nothing to do
        }

        int errors = 0;

        for (T cacheElement : cacheElements) {
            boolean matched = cacheElement.process(providedValue, extraParams);

            if (matched) // send positive event in case of a match
            {
                try {
                    if (OutOfBoundsCacheElement.class.isInstance(cacheElement)) {
                        // don't bother with active property for OutOfBoundsCacheElement types
                        double diff = ((Double) providedValue).doubleValue()
                            - ((Double) cacheElement.getAlertConditionValue()).doubleValue();
                        cachedConditionProducer.sendOutOfBoundsConditionMessage(cacheElement
                            .getAlertConditionTriggerId(), Double.valueOf(diff), timestamp);
                    } else {
                        /*
                         * Set the active property for alertCondition-based cache elements, and send it on its way;
                         * Thus, even if the element is already active, we're going to send another message with the new
                         * value
                         */
                        cacheElement.setActive(true); // no harm to always set active (though, technically, STATELESS operators don't need it)
                        cachedConditionProducer.sendActivateAlertConditionMessage(cacheElement
                            .getAlertConditionTriggerId(), timestamp, providedValue, extraParams);
                    }

                    stats.matched++;
                } catch (Exception e) {
                    log.error("Error processing matched cache element '" + cacheElement + "': " + e.getMessage());
                    errors++;
                }
            } else // no match, negative event
            {
                /*
                 * but only send negative events if we're,   1) a type of operator that supports STATEFUL events, and
                 * 2) currently active
                 */
                if (cacheElement.isType(AlertConditionOperator.Type.STATEFUL) && cacheElement.isActive()) {
                    cacheElement.setActive(false);

                    try {
                        // send negative message
                        cachedConditionProducer.sendDeactivateAlertConditionMessage(cacheElement
                            .getAlertConditionTriggerId(), timestamp);
                    } catch (Exception e) {
                        log.error("Error sending deactivation message for cache element '" + cacheElement + "': "
                            + e.getMessage());
                        errors++;
                    }
                } else {
                    /*
                     * negative message, but nothing was active...so do nothing.
                     *
                     * this will occur in the overwhelming majority of cases.  in theory, since most of the time
                     * conditions exist to alert people of non-ideal system state, it will not fire in the POSITIVE very
                     * often.  thus, we suppress the firing of negative events unless we know we've already sent a
                     * POSITIVE event that we need to compensate for.
                     */
                }
            }
        }

        if (errors != 0) {
            log.error("There were " + errors + " alert conditions that did not fire. "
                + "Please check the configuration of the JMS subsystem and try again. ");
        }
    }

    private <T extends AbstractCacheElement<?>> boolean addTo(String mapName, Map<Integer, List<T>> cache, Integer key,
        T cacheElement, Integer alertConditionId, AlertConditionCacheStats stats) {
        List<T> cacheElements = cache.get(key);

        if (cacheElements == null) {
            cacheElements = new ArrayList<T>();
            cache.put(key, cacheElements);
        }

        if (log.isDebugEnabled()) {
            log
                .debug("Inserted '" + mapName + "' element: " + "key=" + key + ", " + "value="
                    + cacheElement.toString());
        }

        // make sure to add bookkeeping information to the inverse cache too
        if (alertConditionId != null) {
            // OOB baseline stuff passed null because it doesn't invert to any persisted alert condition
            addToInverseAlertConditionMap(alertConditionId, cacheElement, cacheElements);
        }

        // and finally update stats and return whether it was success
        boolean success = cacheElements.add(cacheElement);
        if (success) {
            stats.created++;
        }

        return success;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractCacheElement<?>> boolean addToInverseAlertConditionMap(Integer alertConditionId,
        T inverseCacheElement, List<T> inverseList) {
        List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>> tuples = inverseAlertConditionMap
            .get(alertConditionId);

        if (tuples == null) {
            tuples = new ArrayList<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>();
            inverseAlertConditionMap.put(alertConditionId, tuples);
        }

        if (log.isDebugEnabled()) {
            log.debug("Inserted inverse element: " + "key=" + alertConditionId + ", " + "value=" + inverseCacheElement);
        }

        Tuple inverseTuple = new Tuple<T, List<T>>(inverseCacheElement, inverseList);
        return tuples.add(inverseTuple);
    }

    private boolean addToResourceOperationCache(Integer resourceId, Integer operationDefinitionId,
        ResourceOperationCacheElement cacheElement, Integer alertConditionId, AlertConditionCacheStats stats) {
        // resourceOperationCache = new HashMap< Integer, Map< Integer, List < ResourceOperationCacheElement > > >();
        Map<Integer, List<ResourceOperationCacheElement>> operationDefinitionMap = resourceOperationCache
            .get(resourceId);

        if (operationDefinitionMap == null) {
            operationDefinitionMap = new HashMap<Integer, List<ResourceOperationCacheElement>>();
            resourceOperationCache.put(resourceId, operationDefinitionMap);
        }

        return addTo("operationDefinitionMap", operationDefinitionMap, operationDefinitionId, cacheElement,
            alertConditionId, stats);
    }

    // TODO: jmarques - update model to replace comparator strings with AlertConditionOperator enum
    @Deprecated
    private AlertConditionOperator getAlertConditionOperator(AlertConditionCategory category, String comparator,
        String conditionOption) {
        if (category == AlertConditionCategory.CONTROL) {
            // the UI currently only supports one operator for control
            return AlertConditionOperator.EQUALS;
        }

        if (category == AlertConditionCategory.EVENT) {
            // the UI currently only supports one operator for events
            return AlertConditionOperator.GREATER_THAN_OR_EQUAL_TO;
        }

        if ((category == AlertConditionCategory.CHANGE) || (category == AlertConditionCategory.TRAIT)) {
            // the model currently supports CHANGE as a category type instead of a comparator
            return AlertConditionOperator.CHANGES;
        }

        if (category == AlertConditionCategory.AVAILABILITY) {
            AvailabilityType conditionOptionType = AvailabilityType.valueOf(conditionOption.toUpperCase());
            if (conditionOptionType == AvailabilityType.DOWN) {
                /*
                 * UI phrases this as "Goes DOWN", but we're going to store the cache element as CHANGES_FROM:UP
                 *
                 * This way, it'll work when the agent's goes suspect and null is persisted for AvailabilityType
                 */
                return AlertConditionOperator.CHANGES_FROM;
            } else if (conditionOptionType == AvailabilityType.UP) {
                /*
                 * UI phrases this as "Goes UP", but we're going to store the cache element as CHANGES_TO:UP
                 *
                 * This way, it'll work when the agent's comes back from being suspect, where it had null for its
                 * AvailabilityType
                 */
                return AlertConditionOperator.CHANGES_TO;
            } else {
                throw new UnsupportedAlertConditionOperatorException("Invalid alertCondition for AVAILABILITY category");
            }
        }

        if (comparator.equals("<")) {
            return AlertConditionOperator.LESS_THAN;
        } else if (comparator.equals(">")) {
            return AlertConditionOperator.GREATER_THAN;
        } else if (comparator.equals("=")) {
            return AlertConditionOperator.EQUALS;
        } else {
            throw new UnsupportedAlertConditionOperatorException("Comparator '" + comparator + "' "
                + "is not supported for ArtifactConditionCategory." + category.name());
        }
    }

    private List<NumericDoubleCacheElement> lookupMeasurementDataCacheElements(int scheduleId) {
        return measurementDataCache.get(scheduleId); // yup, might be null
    }

    private List<StringCacheElement> lookupMeasurementTraitCacheElements(int scheduleId) {
        return measurementTraitCache.get(scheduleId); // yup, might be null
    }

    private List<ResourceOperationCacheElement> lookupResourceOperationHistoryCacheElements(int resourceId,
        int definitionId) {
        Map<Integer, List<ResourceOperationCacheElement>> operationDefinitionMap = resourceOperationCache
            .get(resourceId);

        if (operationDefinitionMap == null) {
            return null;
        }

        return operationDefinitionMap.get(definitionId); // yup, might be null
    }

    private List<AvailabilityCacheElement> lookupAvailabilityCacheElements(int resourceId) {
        return availabilityCache.get(resourceId); // yup, might be null
    }

    private List<EventCacheElement> lookupEventCacheElements(int resourceId) {
        return eventsCache.get(resourceId); // yup, might be null
    }

    private void insertAlertDefinition(Subject subject, AlertDefinition alertDefinition, AlertConditionCacheStats stats) {
        Resource resource = alertDefinition.getResource();
        Set<AlertCondition> alertConditions = alertDefinition.getConditions();

        for (AlertCondition alertCondition : alertConditions) {
            /*
             * Granted, all processing within insertAlertCondition currently catches the various exceptions that can
             * occur; this catch exists just in case future implementors fail to handle some runtime exception because
             * we don't want one exception to halt the rest of the insertion processing
             */
            try {
                insertAlertCondition(subject, resource, alertCondition, stats);
            } catch (RuntimeException re) {
                log.error("Error inserting alert condition '" + alertCondition + "': " + re.getMessage());
            }
        }
    }

    private void insertAlertCondition(Subject subject, Resource resource, AlertCondition alertCondition,
        AlertConditionCacheStats stats) {
        MeasurementDefinition measurementDefinition = alertCondition.getMeasurementDefinition(); // will be null for some categories
        int alertConditionId = alertCondition.getId(); // auto-unboxing is safe here because as the PK it's guaranteed to be non-null

        AlertConditionCategory alertConditionCategory = alertCondition.getCategory();
        AlertConditionOperator alertConditionOperator = getAlertConditionOperator(alertConditionCategory,
            alertCondition.getComparator(), alertCondition.getOption());

        if (alertConditionCategory == AlertConditionCategory.BASELINE) {
            // option status for baseline gets set to "mean", but it's rather useless since the UI
            // current doesn't allow alerting off of other baseline properties such as "min" and "max"
            Double threshold = alertCondition.getThreshold();

            MeasurementBaseline measurementBaseline = measurementBaselineManager
                .findBaselineForResourceAndMeasurementDefinition(subject, resource.getId(), measurementDefinition
                    .getId());
            int measurementScheduleId = measurementBaseline.getSchedule().getId();
            String optionStatus = alertCondition.getOption();

            /* 
             * yes, calculatedValue may be null, but that's OK because the match 
             * method for MeasurementBaselineCacheElement handles nulls just fine
             */
            Double calculatedValue = getCalculatedBaselineValue(alertConditionId, measurementBaseline, optionStatus,
                threshold);

            try {
                MeasurementBaselineCacheElement cacheElement = new MeasurementBaselineCacheElement(
                    alertConditionOperator, calculatedValue, alertConditionId, optionStatus);

                // auto-boxing (of alertConditionId) is always safe
                addTo("measurementDataCache", measurementDataCache, measurementScheduleId, cacheElement,
                    alertConditionId, stats);
                addTo("measurementBaselineMap", measurementBaselineMap, measurementBaseline.getId(), cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create NumericDoubleCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, calculatedValue));
            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(stats.created - stats.deleted);
        } else if (alertConditionCategory == AlertConditionCategory.CHANGE) {
            Integer measurementScheduleId = getMeasurementScheduleId(resource.getId(), measurementDefinition.getId(),
                true);

            MeasurementDataNumeric measurementData = null;
            if (measurementScheduleId != null) {
                measurementData = measurementDataManager.getCurrentNumericForSchedule(measurementScheduleId);
            }

            try {
                NumericDoubleCacheElement cacheElement = new NumericDoubleCacheElement(alertConditionOperator,
                    (measurementData == null) ? null : measurementData.getValue(), alertConditionId);

                addTo("measurementDataCache", measurementDataCache, measurementScheduleId, cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create NumericDoubleCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, measurementData));
            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(stats.created - stats.deleted);
        } else if (alertConditionCategory == AlertConditionCategory.TRAIT) {
            Integer measurementScheduleId = getMeasurementScheduleId(resource.getId(), measurementDefinition.getId(),
                true);

            MeasurementDataTrait measurementTrait = null;
            if (measurementScheduleId != null) {
                // auto-unboxing safe because measurementScheduleId known to be non-null by this point
                measurementTrait = measurementDataManager.getCurrentTraitForSchedule(measurementScheduleId);
            }

            try {
                /* 
                 * don't forget special defensive handling to allow for null trait calculation;
                 * this might happen if a newly committed resource has some alert template applied to
                 * it for some trait that it has not yet gotten from the agent
                 */
                StringCacheElement cacheElement = new StringCacheElement(alertConditionOperator,
                    (measurementTrait == null) ? null : measurementTrait.getValue(), alertConditionId);

                addTo("measurementTraitCache", measurementTraitCache, measurementScheduleId, cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create StringCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, measurementTrait));
            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(stats.created - stats.deleted);
        } else if (alertConditionCategory == AlertConditionCategory.AVAILABILITY) {
            /*
             * This is a hack, because we're not respecting the persist alertCondition option, we're instead overriding
             * it with AvailabilityType.UP to satisfy the desired semantics.
             *
             * TODO: jmarques - should associate a specific operator with availability UI selections to make biz
             * processing more consistent with the model
             */

            // get the current value for the resource attached to this alert condition
            AvailabilityType alertConditionOperatorOption = availabilityManager.getCurrentAvailabilityTypeForResource(
                subject, resource.getId());
            try {
                AvailabilityCacheElement cacheElement = new AvailabilityCacheElement(alertConditionOperator,
                    AvailabilityType.UP, alertConditionOperatorOption, alertConditionId);
                addTo("availabilityCache", availabilityCache, resource.getId(), cacheElement, alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create AvailabilityCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        alertConditionOperatorOption, AvailabilityType.UP));
            }
            AlertConditionCacheMonitor.getMBean().incrementAvailabilityCacheElementCount(stats.created - stats.deleted);
        } else if (alertConditionCategory == AlertConditionCategory.CONTROL) {
            String option = alertCondition.getOption();
            OperationRequestStatus operationRequestStatus = OperationRequestStatus.valueOf(option.toUpperCase());

            String operationName = alertCondition.getName();
            int resourceTypeId = resource.getResourceType().getId();

            OperationDefinition operationDefinition = null;
            try {
                operationDefinition = operationManager.getOperationDefinitionByResourceTypeAndName(resourceTypeId,
                    operationName);
            } catch (OperationDefinitionNotFoundException odnfe) {
                log.error("AlertDefinition with name='" + alertCondition.getAlertDefinition().getName() + "' "
                    + "under resource with id='" + resource.getId() + "'" + "has condition with id=" + alertConditionId
                    + " " + "that references an operation with name='" + operationName);
                return;
            }

            try {
                ResourceOperationCacheElement cacheElement = new ResourceOperationCacheElement(alertConditionOperator,
                    operationRequestStatus, alertConditionId);

                // auto-boxing always safe
                addToResourceOperationCache(resource.getId(), operationDefinition.getId(), cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log
                    .info("Failed to create ResourceOperationCacheElement with parameters: "
                        + getCacheElementErrorString(alertConditionId, alertConditionOperator, null,
                            operationRequestStatus));
            }
            AlertConditionCacheMonitor.getMBean().incrementOperationCacheElementCount(stats.created - stats.deleted);
        } else if (alertConditionCategory == AlertConditionCategory.THRESHOLD) {
            Double thresholdValue = alertCondition.getThreshold();

            NumericDoubleCacheElement cacheElement = null;
            try {
                cacheElement = new NumericDoubleCacheElement(alertConditionOperator, thresholdValue, alertConditionId);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create NumberDoubleCacheElement with parameters: "
                    + getCacheElementErrorString(alertConditionId, alertConditionOperator, null, thresholdValue));
            }

            if (cacheElement != null) {
                Integer measurementScheduleId = getMeasurementScheduleId(resource.getId(), measurementDefinition
                    .getId(), false);
                if (measurementScheduleId != null) {
                    addTo("measurementDataCache", measurementDataCache, measurementScheduleId, cacheElement,
                        alertConditionId, stats);
                } else {
                    log.error("Missing schedule prevents element from being cached: " + cacheElement);
                }
            }
            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementCount(stats.created - stats.deleted);
        } else if (alertConditionCategory == AlertConditionCategory.EVENT) {
            EventSeverity eventSeverity = EventSeverity.valueOf(alertCondition.getName());
            String eventDetails = alertCondition.getOption();

            EventCacheElement cacheElement = null;
            try {
                if (eventDetails == null) {
                    cacheElement = new EventCacheElement(alertConditionOperator, eventSeverity, alertConditionId);
                } else {
                    cacheElement = new EventCacheElement(alertConditionOperator, eventDetails, eventSeverity,
                        alertConditionId);
                }
            } catch (InvalidCacheElementException icee) {
                log
                    .info("Failed to create EventCacheElement with parameters: "
                        + getCacheElementErrorString(alertConditionId, alertConditionOperator, eventDetails,
                            eventSeverity));
            }

            addTo("eventsCache", eventsCache, resource.getId(), cacheElement, alertConditionId, stats);
            AlertConditionCacheMonitor.getMBean().incrementEventCacheElementCount(stats.created - stats.deleted);
        }
    }

    private Double getCalculatedBaselineValue(int alertConditionId, MeasurementBaseline baseline, String optionStatus,
        Double threshold) {
        return getCalculatedBaselineValue_helper(alertConditionId, baseline.getId(), baseline.getMin(), baseline
            .getMean(), baseline.getMax(), optionStatus, threshold);
    }

    private Double getCalculatedBaselineValue(int alertConditionId, MeasurementBaselineComposite composite,
        String optionStatus, Double threshold) {
        return getCalculatedBaselineValue_helper(alertConditionId, composite.getId(), composite.getMin(), composite
            .getMean(), composite.getMax(), optionStatus, threshold);
    }

    // this is auto-unboxing heaven, so let's be overly defensive at every turn
    private Double getCalculatedBaselineValue_helper(int conditionId, int baselineId, Double min, Double mean,
        Double max, String optionStatus, Double threshold) {

        if (isInvalidDouble(threshold)) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: threshold was null");
        }

        // auto-unboxing of threshold is safe here 
        double percentage = threshold / 100.0;
        Double baselineValue = 0.0;

        if (optionStatus == null) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: optionStatus string was null");
        } else if (optionStatus.equals("min")) {
            baselineValue = min;
        } else if (optionStatus.equals("mean")) {
            baselineValue = mean;
        } else if (optionStatus.equals("max")) {
            baselineValue = max;
        } else {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: unrecognized optionStatus string of '" + optionStatus + "'");
            return null;
        }

        if (isInvalidDouble(baselineValue)) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: optionStatus string was '" + optionStatus + "', but the corresponding baseline value was null");
        }

        return percentage * baselineValue;
    }

    private void insertOutOfBoundsBaseline(int baselineId, double baselineMin, double baselineMax, int scheduleId,
        AlertConditionCacheStats stats) {
        /*
         * First, do some calculations to compute the cache elements from the raw data
         */
        double lowOOB = baselineMin * MeasurementConstants.LOW_OOB_THRESHOLD;
        double highOOB = baselineMax * MeasurementConstants.HIGH_OOB_THRESHOLD;

        /*
         * when dealing with negative numbers, the LOW_OOB_THRESHOLD will create a **larger** lowOOB than the
         * highOOB...so, after multiplying by the thresholds, swap if necessary to correct the issue
         */
        if (lowOOB > highOOB) {
            double temp = highOOB;

            highOOB = lowOOB;
            lowOOB = temp;
        }

        OutOfBoundsCacheElement lowCacheElement = null;
        OutOfBoundsCacheElement highCacheElement = null;

        try {
            lowCacheElement = new OutOfBoundsCacheElement(AlertConditionOperator.LESS_THAN, lowOOB, scheduleId);
        } catch (InvalidCacheElementException icee) {
            log.info("Failed to create OutOfBoundsCacheElement with parameters: "
                + getCacheElementErrorString(scheduleId, AlertConditionOperator.LESS_THAN, null, lowOOB));
        }

        try {
            highCacheElement = new OutOfBoundsCacheElement(AlertConditionOperator.GREATER_THAN, highOOB, scheduleId);
        } catch (InvalidCacheElementException icee) {
            log.info("Failed to create OutOfBoundsCacheElement with parameters: "
                + getCacheElementErrorString(scheduleId, AlertConditionOperator.GREATER_THAN, null, highOOB));
        }

        if ((lowCacheElement != null) && (highCacheElement != null)) {
            /*
             * Next, add the calculated values to the measurementDataCache
             */
            addTo("measurementDataCache", measurementDataCache, scheduleId, lowCacheElement, null, stats);
            addTo("measurementDataCache", measurementDataCache, scheduleId, highCacheElement, null, stats);

            /*
             * Now add the bookkeeping information to the outOfBoundsBaselineMap.
             *
             * Remember: stats only count for the "real" caches, not the bookkeeping stuff, so stats is unchanged here.
             */
            outOfBoundsBaselineMap.put(baselineId, new Tuple<OutOfBoundsCacheElement, OutOfBoundsCacheElement>(
                lowCacheElement, highCacheElement));
        }
    }

    private String getCacheElementErrorString(int conditionId, AlertConditionOperator operator, Object option,
        Object value) {
        return "id=" + conditionId + ", " + "operator=" + operator + ", "
            + ((option != null) ? ("option=" + option + ", ") : "") + "value=" + value;
    }

    private Integer getMeasurementScheduleId(int resourceId, int measurementDefinitionId, boolean okay) {
        try {
            MeasurementSchedule schedule = measurementScheduleManager.getMeasurementSchedule(subjectManager
                .getOverlord(), measurementDefinitionId, resourceId, false);
            return schedule.getId();
        } catch (MeasurementNotFoundException mnfe) {
            log.error("MeasureSchedule not found for " + "measurementDefinitionId=" + measurementDefinitionId + ", "
                + "resourceId=" + resourceId);
            if (okay) {
                log.error("Alert condition cache element should be unaffected by this");
            }
        }

        return null;
    }

    private void removeAlertDefinition(AlertDefinition alertDefinition, AlertConditionCacheStats stats) {
        for (AlertCondition alertCondition : alertDefinition.getConditions()) {
            try {
                removeAlertCondition(alertCondition, stats);
            } catch (RuntimeException re) {
                log.error("Error removing alert definition from cache: " + re.getMessage());
            }
        }
    }

    private void removeAlertCondition(AlertCondition alertCondition, AlertConditionCacheStats stats) {
        /*
         * remove the map bound to the alertCondition id being removed from the cache; we no longer need bookkeeping
         * information about it after we're done using it
         */
        List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>> inverseCacheElements = inverseAlertConditionMap
            .remove(alertCondition.getId());

        /*
         * if we have no inverse data for this alertCondition, we have no work to do; this should never happen, but
         * let's not fail with NPE if it does for some reason
         */
        if (inverseCacheElements == null) {
            log.error("There were no inverseCacheElements for " + alertCondition
                + ", but all alertConditions should be in the cache");
            return;
        }

        for (Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>> inverseCacheElement : inverseCacheElements) {
            // it's possible this might leave empty lists in the various other caches and maps - not a big deal
            inverseCacheElement.righty.remove(inverseCacheElement.lefty);
            stats.deleted++;
        }
    }

    public boolean isCacheValid() {
        rwLock.readLock().lock();

        waitForBaselineLatchToOpen(rwLock.readLock());

        try {
            if (validCache == null) {
                validCache = true;
                Collection<Tuple<OutOfBoundsCacheElement, OutOfBoundsCacheElement>> oobCacheElements = outOfBoundsBaselineMap
                    .values();

                Set<OutOfBoundsCacheElement> uniqueElements = new HashSet<OutOfBoundsCacheElement>();
                for (Tuple<OutOfBoundsCacheElement, OutOfBoundsCacheElement> cacheElement : oobCacheElements) {
                    if (uniqueElements.add(cacheElement.lefty) == false) {
                        validCache = false;
                        log.error("Duplicate (lefty) OutOfBoundsCacheElement: " + cacheElement.lefty);
                    }

                    if (uniqueElements.add(cacheElement.righty) == false) {
                        validCache = false;
                        log.error("Duplicate (righty) OutOfBoundsCacheElement: " + cacheElement.righty);
                    }
                }
            }

            return validCache;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public String[] getCacheNames() {
        CacheName[] caches = CacheName.values();
        String[] results = new String[caches.length];

        int i = 0;
        for (CacheName cache : caches) {
            results[i++] = cache.name();
        }

        return results;
    }

    public void printCache(String cacheName) {
        rwLock.readLock().lock();

        waitForBaselineLatchToOpen(rwLock.readLock());

        try {
            if (CacheName.MeasurementDataCache.name().equals(cacheName)) {
                printListCache(CacheName.MeasurementDataCache.name(), measurementDataCache);
            } else if (CacheName.MeasurementTraitCache.name().equals(cacheName)) {
                printListCache(CacheName.MeasurementTraitCache.name(), measurementTraitCache);
            } else if (CacheName.MeasurementBaselineMap.name().equals(cacheName)) {
                printListCache(CacheName.MeasurementBaselineMap.name(), measurementBaselineMap);
            } else if (CacheName.OutOfBoundsBaselineMap.name().equals(cacheName)) {
                printTupleCache(CacheName.OutOfBoundsBaselineMap.name(), outOfBoundsBaselineMap);
            } else if (CacheName.ResourceOperationCache.name().equals(cacheName)) {
                printNestedCache(CacheName.ResourceOperationCache.name(), resourceOperationCache);
            } else if (CacheName.AvailabilityCache.name().equals(cacheName)) {
                printListCache(CacheName.AvailabilityCache.name(), availabilityCache);
            } else if (CacheName.Inverse.name().equals(cacheName)) {
                printInverseCache();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void printAllCaches() {
        rwLock.readLock().lock();

        waitForBaselineLatchToOpen(rwLock.readLock());

        try {
            if (log.isDebugEnabled()) {
                log.debug(""); // visually separate logs better
                printListCache(CacheName.MeasurementDataCache.name(), measurementDataCache);
                printListCache(CacheName.MeasurementTraitCache.name(), measurementTraitCache);
                printListCache(CacheName.MeasurementBaselineMap.name(), measurementBaselineMap);
                printTupleCache(CacheName.OutOfBoundsBaselineMap.name(), outOfBoundsBaselineMap);
                printNestedCache(CacheName.ResourceOperationCache.name(), resourceOperationCache);
                printListCache(CacheName.AvailabilityCache.name(), availabilityCache);
                printInverseCache();
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private <T extends AbstractCacheElement<?>> void printTupleCache(String cacheName, Map<Integer, Tuple<T, T>> cache) {
        log.debug("Printing " + cacheName + "...");

        for (Map.Entry<Integer, Tuple<T, T>> cacheElement : cache.entrySet()) {
            Tuple<T, T> value = cacheElement.getValue();
            log.debug("key=" + cacheElement.getKey() + ", " + "lefty=" + value.lefty + ", " + "righty=" + value.righty);
        }
    }

    private <T extends AbstractCacheElement<?>> void printListCache(String cacheName, Map<Integer, List<T>> cache) {
        log.debug("Printing " + cacheName + "...");

        for (Map.Entry<Integer, List<T>> cacheElement : cache.entrySet()) {
            log.debug("key=" + cacheElement.getKey() + ", " + "value=" + cacheElement.getValue());
        }
    }

    private <T extends AbstractCacheElement<?>> void printNestedCache(String cacheName,
        Map<Integer, Map<Integer, List<T>>> nestedCache) {
        log.debug("Printing " + cacheName + "...");
        for (Map.Entry<Integer, Map<Integer, List<T>>> cache : nestedCache.entrySet()) {
            for (Map.Entry<Integer, List<T>> cacheElement : cache.getValue().entrySet()) {
                log.debug("key1=" + cache.getKey() + ", " + "key2=" + cacheElement.getKey() + ", " + "value="
                    + cacheElement.getValue());
            }
        }
    }

    private void printInverseCache() {
        log.debug("Printing inverseAlertConditionMap...");
        for (Map.Entry<Integer, List<Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>>>> inverseEntry : inverseAlertConditionMap
            .entrySet()) {
            log.debug("alertConditionId=" + inverseEntry.getKey() + " has the following mappings: ");
            for (Tuple<AbstractCacheElement<?>, List<AbstractCacheElement<?>>> tupleElement : inverseEntry.getValue()) {
                AbstractCacheElement<?> inverseElement = tupleElement.lefty;
                log.debug("abstractCacheElement=" + inverseElement);
            }
        }
    }

    private boolean isInvalidDouble(Double d) {
        return (d == null || d == Double.NaN || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY);
    }
}