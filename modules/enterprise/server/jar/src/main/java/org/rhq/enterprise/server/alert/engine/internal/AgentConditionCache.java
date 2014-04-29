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
package org.rhq.enterprise.server.alert.engine.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.composite.AbstractAlertConditionCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionBaselineCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionChangesCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionDriftCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionEventCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionRangeCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionScheduleCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionTraitCategoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.internal.AlertConditionCacheCoordinator.Cache;
import org.rhq.enterprise.server.alert.engine.mbean.AlertConditionCacheMonitor;
import org.rhq.enterprise.server.alert.engine.model.CallTimeDataCacheElement;
import org.rhq.enterprise.server.alert.engine.model.CallTimeDataCacheElement.CallTimeElementValue;
import org.rhq.enterprise.server.alert.engine.model.DriftCacheElement;
import org.rhq.enterprise.server.alert.engine.model.EventCacheElement;
import org.rhq.enterprise.server.alert.engine.model.InvalidCacheElementException;
import org.rhq.enterprise.server.alert.engine.model.MeasurementBaselineCacheElement;
import org.rhq.enterprise.server.alert.engine.model.MeasurementNumericCacheElement;
import org.rhq.enterprise.server.alert.engine.model.MeasurementRangeNumericCacheElement;
import org.rhq.enterprise.server.alert.engine.model.MeasurementTraitCacheElement;
import org.rhq.enterprise.server.alert.engine.model.NumericDoubleCacheElement;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
class AgentConditionCache extends AbstractConditionCache {

    private final Map<Integer, List<NumericDoubleCacheElement>> measurementDataCache; // key: schedule ID
    private final Map<Integer, List<MeasurementTraitCacheElement>> measurementTraitCache; // key: schedule ID
    private final Map<Integer, List<CallTimeDataCacheElement>> callTimeCache; // key: schedule ID
    private final Map<Integer, List<EventCacheElement>> eventsCache; // key: resource ID
    private final Map<Integer, List<DriftCacheElement>> driftCache; // key: resource ID

    private final AlertConditionManagerLocal alertConditionManager;
    private final MeasurementDataManagerLocal measurementDataManager;
    private final SubjectManagerLocal subjectManager;

    private final int agentId;

    public AgentConditionCache(int agentId) {
        super();

        this.agentId = agentId;

        measurementDataCache = new HashMap<Integer, List<NumericDoubleCacheElement>>();
        measurementTraitCache = new HashMap<Integer, List<MeasurementTraitCacheElement>>();
        callTimeCache = new HashMap<Integer, List<CallTimeDataCacheElement>>();
        eventsCache = new HashMap<Integer, List<EventCacheElement>>();
        driftCache = new HashMap<Integer, List<DriftCacheElement>>();

        alertConditionManager = LookupUtil.getAlertConditionManager();
        measurementDataManager = LookupUtil.getMeasurementDataManager();
        subjectManager = LookupUtil.getSubjectManager();

        loadCachesForAgent(agentId);
    }

    /**
     * This method is used to do the initial loading from the database for a particular agent. In the high availability
     * infrastructure each server instance in the cloud will only be responsible for monitoring a select number of
     * agents at any given point in time. When an agent makes a connection to this server instance, the caches for that
     * agent will be loaded at that time.
     *
     * @return the number of conditions that re/loaded
     */
    private AlertConditionCacheStats loadCachesForAgent(int agentId) {
        AlertConditionCacheStats stats = new AlertConditionCacheStats();

        try {
            if (log.isDebugEnabled()) {
                log.debug("Loading Alert Condition Caches for agent[id=" + agentId + "]...");
            }

            Subject overlord = subjectManager.getOverlord();

            EnumSet<AlertConditionCategory> supportedCategories = EnumSet.of(AlertConditionCategory.BASELINE,
                AlertConditionCategory.CHANGE, AlertConditionCategory.TRAIT, AlertConditionCategory.THRESHOLD,
                AlertConditionCategory.EVENT, AlertConditionCategory.DRIFT, AlertConditionCategory.RANGE);

            for (AlertConditionCategory nextCategory : supportedCategories) {
                // page thru all alert definitions
                int rowsProcessed = 0;
                PageControl pc = new PageControl();
                pc.setPageNumber(0);
                pc.setPageSize(PAGE_SIZE); // condition composites are small so we can grab alot; use the setter, constructor limits this to 100

                while (true) {
                    PageList<? extends AbstractAlertConditionCategoryComposite> alertConditions = null;
                    alertConditions = alertConditionManager.getAlertConditionComposites(overlord, agentId,
                        nextCategory, pc);

                    if (alertConditions.isEmpty()) {
                        break; // didn't get any rows back, must not have any data or no more rows left to process
                    }

                    for (AbstractAlertConditionCategoryComposite nextComposite : alertConditions) {
                        insertAlertConditionComposite(agentId, nextComposite, stats);
                    }

                    rowsProcessed += alertConditions.size();
                    if (rowsProcessed >= alertConditions.getTotalSize()) {
                        break; // we've processed all data, we can stop now
                    }

                    pc.setPageNumber(pc.getPageNumber() + 1);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Loaded " + rowsProcessed + " Alert Condition Composites of type '" + nextCategory + "'");
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Loaded Alert Condition Caches for agent[id=" + agentId + "]");
            }
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error loading cache for agent[id=" + agentId + "]", t);
        }
        return stats;
    }

    private void insertAlertConditionComposite(int agentId, AbstractAlertConditionCategoryComposite composite,
        AlertConditionCacheStats stats) {

        AlertCondition alertCondition = composite.getCondition();
        int alertConditionId = alertCondition.getId(); // auto-unboxing is safe here because as the PK it's guaranteed to be non-null

        AlertConditionCategory alertConditionCategory = alertCondition.getCategory();
        AlertConditionOperator alertConditionOperator = AlertConditionCacheUtils
            .getAlertConditionOperator(alertCondition);

        if (DataType.CALLTIME == composite.getDataType()) { // call-time cases start here
            if (alertConditionCategory == AlertConditionCategory.CHANGE) {
                AlertConditionChangesCategoryComposite changesComposite = (AlertConditionChangesCategoryComposite) composite;
                int scheduleId = changesComposite.getScheduleId();

                try {
                    CallTimeDataCacheElement cacheElement = new CallTimeDataCacheElement(alertConditionOperator,
                        CallTimeElementValue.valueOf(alertCondition.getOption()), alertCondition.getComparator(),
                        alertCondition.getThreshold(), alertConditionId, alertCondition.getName());

                    addTo("callTimeDataCache", callTimeCache, scheduleId, cacheElement, alertConditionId, stats);
                } catch (InvalidCacheElementException icee) {
                    log.info("Failed to create CallTimeDataCacheElement with parameters: "
                        + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                            null, alertCondition.getThreshold(), icee));
                }
            } else if (alertConditionCategory == AlertConditionCategory.THRESHOLD) {
                AlertConditionScheduleCategoryComposite thresholdComposite = (AlertConditionScheduleCategoryComposite) composite;

                try {
                    CallTimeDataCacheElement cacheElement = new CallTimeDataCacheElement(alertConditionOperator,
                        CallTimeElementValue.valueOf(alertCondition.getOption()), null, alertCondition.getThreshold(),
                        alertConditionId, alertCondition.getName());

                    addTo("measurementDataCache", callTimeCache, thresholdComposite.getScheduleId(), cacheElement,
                        alertConditionId, stats);
                } catch (InvalidCacheElementException icee) {
                    log.info("Failed to create CallTimeDataCacheElement with parameters: "
                        + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                            null, alertCondition.getThreshold(), icee));
                }

            }// last call-time case
        } else if (alertConditionCategory == AlertConditionCategory.BASELINE) { // normal cases start here
            AlertConditionBaselineCategoryComposite baselineComposite = (AlertConditionBaselineCategoryComposite) composite;
            // option status for baseline gets set to "mean", but it's rather useless since the UI
            // current doesn't allow alerting off of other baseline properties such as "min" and "max"
            Double threshold = alertCondition.getThreshold();
            String optionStatus = alertCondition.getOption();

            /*
             * yes, calculatedValue may be null, but that's OK because the match
             * method for MeasurementBaselineCacheElement handles nulls just fine
             */
            Double calculatedValue = getCalculatedBaselineValue(alertConditionId, baselineComposite, optionStatus,
                threshold);

            try {
                MeasurementBaselineCacheElement cacheElement = new MeasurementBaselineCacheElement(
                    alertConditionOperator, calculatedValue, alertConditionId, optionStatus);

                // auto-boxing (of alertConditionId) is always safe
                addTo("measurementDataCache", measurementDataCache, baselineComposite.getScheduleId(), cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create MeasurementBaselineCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, calculatedValue, icee));
            }
        } else if (alertConditionCategory == AlertConditionCategory.CHANGE) {
            AlertConditionChangesCategoryComposite changesComposite = (AlertConditionChangesCategoryComposite) composite;
            int scheduleId = changesComposite.getScheduleId();

            MeasurementDataNumeric numeric = measurementDataManager.getCurrentNumericForSchedule(scheduleId);

            try {
                MeasurementNumericCacheElement cacheElement = new MeasurementNumericCacheElement(
                    alertConditionOperator, (numeric == null) ? null : numeric.getValue(), alertConditionId);

                addTo("measurementDataCache", measurementDataCache, scheduleId, cacheElement, alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create MeasurementNumericCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, numeric, icee));
            }
        } else if (alertConditionCategory == AlertConditionCategory.TRAIT) {
            AlertConditionTraitCategoryComposite traitsComposite = (AlertConditionTraitCategoryComposite) composite;
            String value = null;

            switch (alertConditionOperator) {
            case CHANGES:
                MeasurementDataTrait trait = measurementDataManager.getCurrentTraitForSchedule(traitsComposite.getScheduleId());
                if (trait != null) {
                    traitsComposite.setValue(trait.getValue());
                }
                value = traitsComposite.getValue();
                break;
            case REGEX:
                value = traitsComposite.getCondition().getOption();
                break;
            default:
                log.error("Invalid operator for Trait condition: " + alertConditionOperator);
            }

            try {
                /*
                 * don't forget special defensive handling to allow for null trait calculation;
                 * this might happen if a newly committed resource has some alert template applied to
                 * it for some trait that it has not yet gotten from the agent
                 */
                MeasurementTraitCacheElement cacheElement = new MeasurementTraitCacheElement(alertConditionOperator,
                    value, alertConditionId);

                addTo("measurementTraitCache", measurementTraitCache, traitsComposite.getScheduleId(), cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create MeasurementTraitCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, value, icee));
            }

        } else if (alertConditionCategory == AlertConditionCategory.THRESHOLD) {
            AlertConditionScheduleCategoryComposite thresholdComposite = (AlertConditionScheduleCategoryComposite) composite;
            Double thresholdValue = alertCondition.getThreshold();

            MeasurementNumericCacheElement cacheElement = null;
            try {
                cacheElement = new MeasurementNumericCacheElement(alertConditionOperator, thresholdValue,
                    alertConditionId);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create MeasurementNumericCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, thresholdValue, icee));
            }

            if (cacheElement != null) {
                addTo("measurementDataCache", measurementDataCache, thresholdComposite.getScheduleId(), cacheElement,
                    alertConditionId, stats);

            }
        } else if (alertConditionCategory == AlertConditionCategory.EVENT) {
            AlertConditionEventCategoryComposite eventComposite = (AlertConditionEventCategoryComposite) composite;
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
                log.info("Failed to create EventCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        eventDetails, eventSeverity, icee));
            }

            addTo("eventsCache", eventsCache, eventComposite.getResourceId(), cacheElement, alertConditionId, stats);
        } else if (alertConditionCategory == AlertConditionCategory.DRIFT) {
            AlertConditionDriftCategoryComposite driftComposite = (AlertConditionDriftCategoryComposite) composite;

            String driftDefNameRegexStr = driftComposite.getCondition().getName();
            String driftPathNameRegexStr = driftComposite.getCondition().getOption();

            DriftCacheElement cacheElement = null;
            try {
                cacheElement = new DriftCacheElement(alertConditionOperator, driftDefNameRegexStr,
                    driftPathNameRegexStr, alertConditionId);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create DriftCacheElement: id=" + alertConditionId + ", operator="
                    + alertConditionOperator + ", driftDefNameRegex=" + driftDefNameRegexStr + ", driftPathNameRegex="
                    + driftPathNameRegexStr);
            }
            addTo("driftCache", driftCache, driftComposite.getResourceId(), cacheElement, alertConditionId, stats);
        } else if (alertConditionCategory == AlertConditionCategory.RANGE) {
            AlertConditionRangeCategoryComposite rangeComposite = (AlertConditionRangeCategoryComposite) composite;
            Double loValue = alertCondition.getThreshold();
            String hiValueStr = alertCondition.getOption();

            MeasurementNumericCacheElement cacheElement = null;
            try {
                if (hiValueStr == null) {
                    throw new NumberFormatException("The range alert condition is missing the high value");
                }
                Double hiValue = Double.valueOf(hiValueStr);
                cacheElement = new MeasurementRangeNumericCacheElement(alertConditionOperator, loValue, hiValue,
                    alertConditionId);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create MeasurementRangeNumericCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        hiValueStr, loValue, icee));
            } catch (NumberFormatException nfe) {
                log.info("Failed to create MeasurementRangeNumericCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        hiValueStr, loValue, nfe));
            }

            if (cacheElement != null) {
                addTo("measurementDataCache", measurementDataCache, rangeComposite.getScheduleId(), cacheElement,
                    alertConditionId, stats);

            }
        }
    }

    public AlertConditionCacheStats checkConditions(MeasurementData... measurementData) {
        if ((measurementData == null) || (measurementData.length == 0)) {
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            for (MeasurementData datum : measurementData) {
                int scheduleId = datum.getScheduleId();

                if (datum instanceof MeasurementDataNumeric) {
                    List<? extends NumericDoubleCacheElement> conditionCacheElements = lookupMeasurementDataCacheElements(scheduleId);

                    Double providedValue = ((MeasurementDataNumeric) datum).getValue();

                    processCacheElements(conditionCacheElements, providedValue, datum.getTimestamp(), stats);
                } else if (datum instanceof MeasurementDataTrait) {
                    List<MeasurementTraitCacheElement> cacheElements = lookupMeasurementTraitCacheElements(scheduleId);

                    processCacheElements(cacheElements, ((MeasurementDataTrait) datum).getValue(),
                        datum.getTimestamp(), stats);
                } else {
                    log.error(getClass().getSimpleName() + " does not support " + "checking conditions against "
                        + datum.getClass().getSimpleName() + " types");
                }
            }

            AlertConditionCacheMonitor.getMBean().incrementMeasurementCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementMeasurementProcessingTime(stats.getAge());
            if (log.isDebugEnabled())
                log.debug("Check Measurements[size=" + measurementData.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during measurement data cache processing for agent[id=" + agentId + "]", t);
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(CallTimeData... callTime) {
        if ((callTime == null) || (callTime.length == 0)) {
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            HashMap<Integer, HashMap<String, ArrayList<CallTimeDataValue>>> order = produceOrderedCallTimeDataStructure(callTime);
            for (Integer scheduleId : order.keySet()) {
                List<? extends CallTimeDataCacheElement> conditionCacheElements = lookupCallTimeDataCacheElements(scheduleId);
                for (String callDest : order.get(scheduleId).keySet()) {
                    for (CallTimeDataValue provided : order.get(scheduleId).get(callDest)) {
                        processCacheElements(conditionCacheElements, provided, provided.getBeginTime(), stats, callDest);
                    }
                }
            }
            AlertConditionCacheMonitor.getMBean().incrementCallTimeCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementCallTimeProcessingTime(stats.getAge());
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during calltime cache processing for agent[id=" + agentId + "]", t);
        }

        return stats;
    }

    private HashMap<Integer, HashMap<String, ArrayList<CallTimeDataValue>>> produceOrderedCallTimeDataStructure(
        CallTimeData... callTime) {
        long beginTime = 0;
        if (log.isDebugEnabled()) {
            beginTime = System.nanoTime();
        }

        //Insert all CallTimeDataValue in data structure
        HashMap<Integer, HashMap<String, ArrayList<CallTimeDataValue>>> order = new HashMap<Integer, HashMap<String, ArrayList<CallTimeDataValue>>>();
        for (CallTimeData ctd : callTime) {
            if (!order.containsKey(ctd.getScheduleId()))
                order.put(ctd.getScheduleId(), new HashMap<String, ArrayList<CallTimeDataValue>>());

            HashMap<String, ArrayList<CallTimeDataValue>> partialOrder = order.get(ctd.getScheduleId());
            for (String callDestination : ctd.getValues().keySet()) {
                if (!partialOrder.containsKey(callDestination))
                    partialOrder.put(callDestination, new ArrayList<CallTimeDataValue>());

                ArrayList<CallTimeDataValue> list = partialOrder.get(callDestination);
                list.add(ctd.getValues().get(callDestination));
            }
        }

        //sort all lists in the data structure
        for (HashMap<String, ArrayList<CallTimeDataValue>> topList : order.values())
            for (ArrayList<CallTimeDataValue> bottomList : topList.values())
                Collections.sort(bottomList, getCallTimeComparator());

        if (log.isDebugEnabled()) {
            log.debug("sorting call-time data during alerting took: "
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beginTime) + "ms");
        }
        return order;
    }

    private Comparator<CallTimeDataValue> getCallTimeComparator() {
        return new Comparator<CallTimeDataValue>() {
            public int compare(CallTimeDataValue arg0, CallTimeDataValue arg1) {
                if (arg0 == null || arg1 == null)
                    throw new IllegalArgumentException("Call-time data value entries must not be null!");

                if (arg0 == arg1)
                    return 0;

                //differing begin times:
                if (arg0.getBeginTime() < arg1.getBeginTime())
                    return -1;
                if (arg0.getBeginTime() > arg1.getBeginTime())
                    return 1;

                // begin time equality:
                if (arg0.getBeginTime() == arg1.getBeginTime()) {
                    if (arg0.getEndTime() == arg1.getEndTime())
                        return 0;
                    if (arg0.getEndTime() < arg1.getEndTime())
                        return -1;
                    if (arg0.getEndTime() > arg1.getEndTime())
                        return 1;
                }
                return Integer.MIN_VALUE;
            }
        };
    }

    /**
     * This operates differently from the other {{checkConditions()}} methods.  Because it's possible that one
     * batch of events may contain both an event triggering a problem event and also an event triggering its
     * recovery alert, we return after a matched condition to allow for the caller to check remaining
     * events only after a cache refresh has been performed.
     *
     * @param stats not null. the stats object to update
     * @param source
     * @param events
     * @return the number of eventsProcessed until a match was found or all events were processed.
     */
    public AlertConditionCacheStats checkConditions(EventSource source, List<Event> events) {
        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        if ((events == null) || events.isEmpty()) {
            return stats;
        }

        int initialSize = events.size();
        try {
            Resource resource = source.getResource();
            List<EventCacheElement> cacheElements = lookupEventCacheElements(resource.getId());

            for (Iterator<Event> i = events.iterator(); i.hasNext();) {
                Event event = i.next();
                i.remove();
                int matched = stats.matched;
                processCacheElements(cacheElements, event.getSeverity(), event.getTimestamp(), stats,
                    event.getDetail(), "sourceLocation=" + source.getLocation());
                if (matched < stats.matched) {
                    break;
                }
            }

            AlertConditionCacheMonitor.getMBean().incrementEventCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementEventProcessingTime(stats.getAge());
            if (log.isDebugEnabled()) {
                log.debug("Check Events[size=" + (initialSize - events.size()) + "] - " + stats);
            }
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during event cache processing for agent[id=" + agentId + "]", t);
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(DriftChangeSetSummary driftChangeSetSummary) {
        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            int resourceId = driftChangeSetSummary.getResourceId();
            List<DriftCacheElement> cacheElements = lookupDriftCacheElements(resourceId);

            processCacheElements(cacheElements, DriftCacheElement.UNUSED_CONDITION_VALUE,
                driftChangeSetSummary.getCreatedTime(), stats, driftChangeSetSummary);

            AlertConditionCacheMonitor.getMBean().incrementDriftCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementDriftProcessingTime(stats.getAge());
            if (log.isDebugEnabled()) {
                log.debug("Check Drift[resourceId=" + resourceId + "] - " + stats);
            }
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during drift cache processing for agent[id=" + agentId + "]", t);
        }
        return stats;
    }

    private List<? extends NumericDoubleCacheElement> lookupMeasurementDataCacheElements(int scheduleId) {
        return measurementDataCache.get(scheduleId); // yup, might be null
    }

    private List<? extends CallTimeDataCacheElement> lookupCallTimeDataCacheElements(int scheduleId) {
        return callTimeCache.get(scheduleId); // yup, might be null
    }

    private List<MeasurementTraitCacheElement> lookupMeasurementTraitCacheElements(int scheduleId) {
        return measurementTraitCache.get(scheduleId); // yup, might be null
    }

    private List<EventCacheElement> lookupEventCacheElements(int resourceId) {
        return eventsCache.get(resourceId); // yup, might be null
    }

    private List<DriftCacheElement> lookupDriftCacheElements(int resourceId) {
        return driftCache.get(resourceId); // yup, might be null
    }

    private Double getCalculatedBaselineValue(int conditionId, AlertConditionBaselineCategoryComposite composite,
        String optionStatus, Double threshold) {
        int baselineId = composite.getBaselineId();

        if (AlertConditionCacheUtils.isInvalidDouble(threshold)) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: threshold was null");
        }

        // auto-unboxing of threshold is safe here
        Double baselineValue = 0.0;

        if (optionStatus == null) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: optionStatus string was null");
        } else if (optionStatus.equals("min")) {
            baselineValue = composite.getMinValue();
        } else if (optionStatus.equals("mean")) {
            baselineValue = composite.getMeanValue();
        } else if (optionStatus.equals("max")) {
            baselineValue = composite.getMaxValue();
        } else {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: unrecognized optionStatus string of '" + optionStatus + "'");
            return null;
        }

        if (AlertConditionCacheUtils.isInvalidDouble(baselineValue)) {
            log.error("Failed to calculate baseline for [conditionId=" + conditionId + ", baselineId=" + baselineId
                + "]: optionStatus string was '" + optionStatus + "', but the corresponding baseline value was null");
        }

        return threshold * baselineValue;
    }

    @Override
    public int getCacheSize(Cache cache) {
        if (cache == AlertConditionCacheCoordinator.Cache.MeasurementDataCache) {
            return AlertConditionCacheUtils.getMapListCount(measurementDataCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.MeasurementTraitCache) {
            return AlertConditionCacheUtils.getMapListCount(measurementTraitCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.CallTimeDataCache) {
            return AlertConditionCacheUtils.getMapListCount(callTimeCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.EventsCache) {
            return AlertConditionCacheUtils.getMapListCount(eventsCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.DriftCache) {
            return AlertConditionCacheUtils.getMapListCount(driftCache);
        } else {
            throw new IllegalArgumentException("The " + AgentConditionCache.class.getSimpleName()
                + " either does not manage caches of type " + cache.type + ", or does not support obtaining their size");
        }
    }

}