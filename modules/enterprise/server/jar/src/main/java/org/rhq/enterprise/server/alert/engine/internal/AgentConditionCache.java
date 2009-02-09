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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.composite.AbstractAlertConditionCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionBaselineCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionChangesCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionEventCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionScheduleCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionTraitCategoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.internal.AlertConditionCacheCoordinator.Cache;
import org.rhq.enterprise.server.alert.engine.mbean.AlertConditionCacheMonitor;
import org.rhq.enterprise.server.alert.engine.model.AlertConditionOperator;
import org.rhq.enterprise.server.alert.engine.model.EventCacheElement;
import org.rhq.enterprise.server.alert.engine.model.InvalidCacheElementException;
import org.rhq.enterprise.server.alert.engine.model.MeasurementBaselineCacheElement;
import org.rhq.enterprise.server.alert.engine.model.MeasurementNumericCacheElement;
import org.rhq.enterprise.server.alert.engine.model.MeasurementTraitCacheElement;
import org.rhq.enterprise.server.alert.engine.model.NumericDoubleCacheElement;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
class AgentConditionCache extends AbstractConditionCache {

    private Map<Integer, List<NumericDoubleCacheElement>> measurementDataCache;
    private Map<Integer, List<MeasurementTraitCacheElement>> measurementTraitCache;
    private Map<Integer, List<EventCacheElement>> eventsCache;

    private AlertConditionManagerLocal alertConditionManager;
    private MeasurementDataManagerLocal measurementDataManager;
    private SubjectManagerLocal subjectManager;
    private EntityManagerFacadeLocal entityManagerFacade;

    public AgentConditionCache(int agentId) {
        super();

        measurementDataCache = new HashMap<Integer, List<NumericDoubleCacheElement>>();
        measurementTraitCache = new HashMap<Integer, List<MeasurementTraitCacheElement>>();
        eventsCache = new HashMap<Integer, List<EventCacheElement>>();

        alertConditionManager = LookupUtil.getAlertConditionManager();
        measurementDataManager = LookupUtil.getMeasurementDataManager();
        subjectManager = LookupUtil.getSubjectManager();

        entityManagerFacade = LookupUtil.getEntityManagerFacade();

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
            log.debug("Loading Alert Condition Caches for agent[id=" + agentId + "]...");

            Subject overlord = subjectManager.getOverlord();

            EnumSet<AlertConditionCategory> supportedCategories = EnumSet.of(AlertConditionCategory.BASELINE,
                AlertConditionCategory.CHANGE, AlertConditionCategory.TRAIT, AlertConditionCategory.THRESHOLD,
                AlertConditionCategory.EVENT);

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

                    entityManagerFacade.flush();
                    entityManagerFacade.clear();

                    rowsProcessed += alertConditions.size();
                    if (rowsProcessed >= alertConditions.getTotalSize()) {
                        break; // we've processed all data, we can stop now
                    }

                    pc.setPageNumber(pc.getPageNumber() + 1);
                }
                log.debug("Loaded " + rowsProcessed + " Alert Condition Composites of type '" + nextCategory + "'");
            }

            log.debug("Loaded Alert Condition Caches for agent[id=" + agentId + "]");
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
        AlertConditionOperator alertConditionOperator = AlertConditionCacheUtils.getAlertConditionOperator(
            alertConditionCategory, alertCondition.getComparator(), alertCondition.getOption());

        if (alertConditionCategory == AlertConditionCategory.BASELINE) {
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
                log.info("Failed to create NumericDoubleCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, calculatedValue));
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
                log.info("Failed to create NumericDoubleCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, numeric));
            }
        } else if (alertConditionCategory == AlertConditionCategory.TRAIT) {
            AlertConditionTraitCategoryComposite traitsComposite = (AlertConditionTraitCategoryComposite) composite;
            String value = traitsComposite.getValue();

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
                log.info("Failed to create StringCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, value));
            }

        } else if (alertConditionCategory == AlertConditionCategory.THRESHOLD) {
            AlertConditionScheduleCategoryComposite thresholdComposite = (AlertConditionScheduleCategoryComposite) composite;
            Double thresholdValue = alertCondition.getThreshold();

            MeasurementNumericCacheElement cacheElement = null;
            try {
                cacheElement = new MeasurementNumericCacheElement(alertConditionOperator, thresholdValue,
                    alertConditionId);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create NumberDoubleCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, thresholdValue));
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
                        eventDetails, eventSeverity));
            }

            addTo("eventsCache", eventsCache, eventComposite.getResourceId(), cacheElement, alertConditionId, stats);
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
            log.debug("Check Measurements[size=" + measurementData.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(EventSource source, Event... events) {
        if ((events == null) || (events.length == 0)) {
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            Resource resource = source.getResource();
            List<EventCacheElement> cacheElements = lookupEventCacheElements(resource.getId());

            for (Event event : events) {
                processCacheElements(cacheElements, event.getSeverity(), event.getTimestamp(), stats, event.getDetail());
            }

            AlertConditionCacheMonitor.getMBean().incrementEventCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementEventProcessingTime(stats.getAge());
            log.debug("Check Events[size=" + events.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error(t);
        }
        return stats;
    }

    private List<? extends NumericDoubleCacheElement> lookupMeasurementDataCacheElements(int scheduleId) {
        return measurementDataCache.get(scheduleId); // yup, might be null
    }

    private List<MeasurementTraitCacheElement> lookupMeasurementTraitCacheElements(int scheduleId) {
        return measurementTraitCache.get(scheduleId); // yup, might be null
    }

    private List<EventCacheElement> lookupEventCacheElements(int resourceId) {
        return eventsCache.get(resourceId); // yup, might be null
    }

    private Double getCalculatedBaselineValue(int conditionId, AlertConditionBaselineCategoryComposite composite,
        String optionStatus, Double threshold) {
        int baselineId = composite.getBaselineId();

        if (AlertConditionCacheUtils.isInvalidDouble(threshold)) {
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

        return percentage * baselineValue;
    }

    @Override
    public int getCacheSize(Cache cache) {
        if (cache == AlertConditionCacheCoordinator.Cache.MeasurementDataCache) {
            return AlertConditionCacheUtils.getMapListCount(measurementDataCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.MeasurementTraitCache) {
            return AlertConditionCacheUtils.getMapListCount(measurementTraitCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.EventsCache) {
            return AlertConditionCacheUtils.getMapListCount(eventsCache);
        } else {
            throw new IllegalArgumentException("The " + AgentConditionCache.class.getSimpleName()
                + " either does not manage caches of type " + cache.type + ", or does not support obtaining their size");
        }
    }

}