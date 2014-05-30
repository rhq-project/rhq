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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.composite.AbstractAlertConditionCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionAvailabilityCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionControlCategoryComposite;
import org.rhq.core.domain.alert.composite.AlertConditionResourceConfigurationCategoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.internal.AlertConditionCacheCoordinator.Cache;
import org.rhq.enterprise.server.alert.engine.mbean.AlertConditionCacheMonitor;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityCacheElement;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityDurationCacheElement;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityDurationComposite;
import org.rhq.enterprise.server.alert.engine.model.InvalidCacheElementException;
import org.rhq.enterprise.server.alert.engine.model.ResourceConfigurationCacheElement;
import org.rhq.enterprise.server.alert.engine.model.ResourceOperationCacheElement;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
class GlobalConditionCache extends AbstractConditionCache {

    private Map<Integer, Map<Integer, List<ResourceOperationCacheElement>>> resourceOperationCache; // key: resource ID, inner key: operation def ID
    private Map<Integer, List<AvailabilityCacheElement>> availabilityCache; // key: resource ID
    private Map<Integer, List<AvailabilityDurationCacheElement>> availabilityDurationCache; // key: resource ID
    private Map<Integer, List<ResourceConfigurationCacheElement>> resourceConfigurationCache; // key: resource ID

    private AlertConditionManagerLocal alertConditionManager;
    private SubjectManagerLocal subjectManager;

    public GlobalConditionCache() {
        super();

        resourceOperationCache = new HashMap<Integer, Map<Integer, List<ResourceOperationCacheElement>>>();
        availabilityCache = new HashMap<Integer, List<AvailabilityCacheElement>>();
        availabilityDurationCache = new HashMap<Integer, List<AvailabilityDurationCacheElement>>();
        resourceConfigurationCache = new HashMap<Integer, List<ResourceConfigurationCacheElement>>();

        alertConditionManager = LookupUtil.getAlertConditionManager();
        subjectManager = LookupUtil.getSubjectManager();

        loadCaches();
    }

    /**
     * @return the number of conditions that re/loaded
     */
    private AlertConditionCacheStats loadCaches() {
        AlertConditionCacheStats stats = new AlertConditionCacheStats();

        try {
            log.debug("Loading Global Condition Cache...");

            Subject overlord = subjectManager.getOverlord();

            EnumSet<AlertConditionCategory> supportedCategories = EnumSet.of(AlertConditionCategory.AVAILABILITY,
                AlertConditionCategory.AVAIL_DURATION, AlertConditionCategory.CONTROL,
                AlertConditionCategory.RESOURCE_CONFIG);

            for (AlertConditionCategory nextCategory : supportedCategories) {
                // page thru all alert definitions
                int rowsProcessed = 0;
                PageControl pc = new PageControl();
                pc.setPageNumber(0);
                pc.setPageSize(PAGE_SIZE); // condition composites are small so we can grab alot; use the setter, constructor limits this to 100

                while (true) {
                    PageList<? extends AbstractAlertConditionCategoryComposite> alertConditions = null;
                    alertConditions = alertConditionManager.getAlertConditionComposites(overlord, null, nextCategory,
                        pc);

                    if (alertConditions.isEmpty()) {
                        break; // didn't get any rows back, must not have any data or no more rows left to process
                    }

                    for (AbstractAlertConditionCategoryComposite nextComposite : alertConditions) {
                        insertAlertConditionComposite(nextComposite, stats);
                    }

                    rowsProcessed += alertConditions.size();
                    if (rowsProcessed >= alertConditions.getTotalSize()) {
                        break; // we've processed all data, we can stop now
                    }

                    pc.setPageNumber(pc.getPageNumber() + 1);
                }
                if (log.isDebugEnabled())
                    log.debug("Loaded " + rowsProcessed + " Alert Condition Composites of type '" + nextCategory + "'");
            }

            log.debug("Loaded Global Condition Cache");
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error loading global condition cache", t);
        }
        return stats;
    }

    private void insertAlertConditionComposite(AbstractAlertConditionCategoryComposite composite,
        AlertConditionCacheStats stats) {

        AlertCondition alertCondition = composite.getCondition();
        int alertConditionId = alertCondition.getId(); // auto-unboxing is safe here because as the PK it's guaranteed to be non-null

        AlertConditionCategory alertConditionCategory = alertCondition.getCategory();
        AlertConditionOperator alertConditionOperator = AlertConditionCacheUtils
            .getAlertConditionOperator(alertCondition);

        if (alertConditionCategory == AlertConditionCategory.AVAILABILITY) {
            AlertConditionAvailabilityCategoryComposite availabilityComposite = (AlertConditionAvailabilityCategoryComposite) composite;

            try {
                AvailabilityCacheElement cacheElement = new AvailabilityCacheElement(alertConditionOperator,
                    availabilityComposite.getAvailabilityType(), alertConditionId);
                addTo("availabilityCache", availabilityCache, availabilityComposite.getResourceId(), cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create AvailabilityCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        availabilityComposite.getAvailabilityType(), AvailabilityType.UP, icee));
            }
        } else if (alertConditionCategory == AlertConditionCategory.AVAIL_DURATION) {
            AlertConditionAvailabilityCategoryComposite availabilityComposite = (AlertConditionAvailabilityCategoryComposite) composite;

            try {
                AvailabilityDurationCacheElement cacheElement = new AvailabilityDurationCacheElement(
                    availabilityComposite.getAlertDefinitionId(), alertConditionOperator, alertCondition.getOption(),
                    availabilityComposite.getAvailabilityType(), alertConditionId);
                addTo("availabilityDurationCache", availabilityDurationCache, availabilityComposite.getResourceId(),
                    cacheElement, alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create AvailabilityCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        availabilityComposite.getAvailabilityType(), alertConditionOperator.toString(), icee));
            }
        } else if (alertConditionCategory == AlertConditionCategory.CONTROL) {
            AlertConditionControlCategoryComposite controlComposite = (AlertConditionControlCategoryComposite) composite;
            String option = alertCondition.getOption();
            OperationRequestStatus operationRequestStatus = OperationRequestStatus.valueOf(option.toUpperCase());

            try {
                ResourceOperationCacheElement cacheElement = new ResourceOperationCacheElement(alertConditionOperator,
                    operationRequestStatus, alertConditionId);

                // auto-boxing always safe
                addToResourceOperationCache(controlComposite.getResourceId(),
                    controlComposite.getOperationDefinitionId(), cacheElement, alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create ResourceOperationCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, operationRequestStatus, icee));
            }
        } else if (alertConditionCategory == AlertConditionCategory.RESOURCE_CONFIG) {
            AlertConditionResourceConfigurationCategoryComposite resourceConfigurationComposite = (AlertConditionResourceConfigurationCategoryComposite) composite;

            ResourceConfigurationCacheElement cacheElement = null;
            try {
                cacheElement = new ResourceConfigurationCacheElement(alertConditionOperator,
                    resourceConfigurationComposite.getResourceConfiguration(), alertConditionId);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create EventCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, null, icee));
            }

            addTo("resourceConfigurationCache", resourceConfigurationCache,
                resourceConfigurationComposite.getResourceId(), cacheElement, alertConditionId, stats);
        }
    }

    public AlertConditionCacheStats checkConditions(OperationHistory operationHistory) {
        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            if (operationHistory instanceof ResourceOperationHistory) {
                ResourceOperationHistory resourceOperationHistory = (ResourceOperationHistory) operationHistory;

                Resource resource = resourceOperationHistory.getResource();
                OperationDefinition operationDefinition = resourceOperationHistory.getOperationDefinition();
                OperationRequestStatus operationStatus = resourceOperationHistory.getStatus();

                List<ResourceOperationCacheElement> cacheElements = lookupResourceOperationHistoryCacheElements(
                    resource.getId(), operationDefinition.getId());

                processCacheElements(cacheElements, operationStatus, resourceOperationHistory.getModifiedTime(), stats);
            } else {
                if (log.isDebugEnabled())
                    log.debug(getClass().getSimpleName() + " does not support checking conditions against "
                        + operationHistory.getClass().getSimpleName() + " types");
            }

            AlertConditionCacheMonitor.getMBean().incrementOperationCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementOperationProcessingTime(stats.getAge());
            if (log.isDebugEnabled())
                log.debug("Check OperationHistory[size=1] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during global cache processing: ", t);
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(ResourceConfigurationUpdate update) {
        if (update == null) {
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            Resource resource = update.getResource();

            List<ResourceConfigurationCacheElement> cacheElements = lookupResourceConfigurationCacheElements(resource
                .getId());

            processCacheElements(cacheElements, update.getConfiguration(), update.getCreatedTime(), stats);

            AlertConditionCacheMonitor.getMBean().incrementResourceConfigurationCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementResourceConfigurationProcessingTime(stats.getAge());
            if (log.isDebugEnabled())
                log.debug("Check " + update + " - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during global cache processing: ", t);
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(Availability... availabilities) {
        if ((availabilities == null) || (availabilities.length == 0)) {
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            for (Availability availability : availabilities) {
                Resource resource = availability.getResource();
                AvailabilityType availabilityType = availability.getAvailabilityType();

                List<AvailabilityCacheElement> cacheElements = lookupAvailabilityCacheElements(resource.getId());

                processCacheElements(cacheElements, availabilityType, availability.getStartTime(), stats);

                // Avail Duration conditions are evaluated in two parts:
                // 1) First, an avail change that starts the clock ticking.
                // 2) Second, after the duration period, check to see if the avail state is still the same.
                // Here we check for part 1, see if we need to start duration processing for the avail change. If so,
                // make sure we capture the avail start time, which is an agent time, not a server time, so we can
                // correctly check for avail changes later (BZ 1099114).
                List<AvailabilityDurationCacheElement> durationCacheElements = lookupAvailabilityDurationCacheElements(resource
                    .getId());
                AvailabilityDurationCacheElement.checkCacheElements(durationCacheElements, resource, availability);
            }

            AlertConditionCacheMonitor.getMBean().incrementAvailabilityCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementAvailabilityProcessingTime(stats.getAge());
            if (log.isDebugEnabled())
                log.debug("Check Availability[size=" + availabilities.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during global cache processing: ", t);
        }
        return stats;
    }

    // Avail Duration conditions are evaluated in two parts:
    // 1) First, an avail change to the that starts the clock ticking.
    // 2) Second, after the duration period, check to see if the avail state is still the same.
    // Here we check for part 2, finish processing of the condition whose duration job finished and
    // determined the avail state to be satisfied.  Now hook into the alerting chassis...
    public AlertConditionCacheStats checkConditions(AvailabilityDurationComposite... composites) {
        if ((null == composites) || (composites.length == 0)) {
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = new AlertConditionCacheStats();
        try {
            for (AvailabilityDurationComposite composite : composites) {

                List<AvailabilityDurationCacheElement> cacheElements = lookupAvailabilityDurationCacheElements(composite
                    .getResourceId());

                // This method differs from the other <code>checkConditions<code> methods in that it is only
                // interested in a single condition for each composite, the one for which the duration job completed.
                if (!(null == cacheElements || cacheElements.isEmpty())) {
                    for (AvailabilityDurationCacheElement cacheElement : cacheElements) {
                        if (composite.getConditionId() == cacheElement.getAlertConditionTriggerId()) {
                            List<AvailabilityDurationCacheElement> cacheElementAsList = new ArrayList<AvailabilityDurationCacheElement>(
                                1);
                            cacheElementAsList.add(cacheElement);

                            processCacheElements(cacheElementAsList, composite.getAvailabilityType(),
                                System.currentTimeMillis(), stats);
                            break;
                        }
                    }
                }
            }

            AlertConditionCacheMonitor.getMBean().incrementAvailabilityDurationCacheElementMatches(stats.matched);
            AlertConditionCacheMonitor.getMBean().incrementAvailabilityDurationProcessingTime(stats.getAge());
            if (log.isDebugEnabled())
                log.debug("Check AvailabilityDuration[size=" + composites.length + "] - " + stats);
        } catch (Throwable t) {
            // don't let any exceptions bubble up to the calling SLSB layer
            log.error("Error during global cache processing: ", t);
        }
        return stats;
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

    private List<AvailabilityDurationCacheElement> lookupAvailabilityDurationCacheElements(int resourceId) {
        return availabilityDurationCache.get(resourceId); // yup, might be null
    }

    private List<ResourceConfigurationCacheElement> lookupResourceConfigurationCacheElements(int resourceId) {
        return resourceConfigurationCache.get(resourceId); // yup, might be null
    }

    @Override
    public int getCacheSize(Cache cache) {
        if (cache == AlertConditionCacheCoordinator.Cache.AvailabilityCache) {
            return AlertConditionCacheUtils.getMapListCount(availabilityCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.AvailabilityDurationCache) {
            return AlertConditionCacheUtils.getMapListCount(availabilityDurationCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.ResourceConfigurationCache) {
            return AlertConditionCacheUtils.getMapListCount(resourceConfigurationCache);
        } else if (cache == AlertConditionCacheCoordinator.Cache.ResourceOperationCache) {
            return AlertConditionCacheUtils.getMapMapListCount(resourceOperationCache);
        } else {
            throw new IllegalArgumentException("The " + GlobalConditionCache.class.getSimpleName()
                + " either does not manage caches of type " + cache.type + ", or does not support obtaining their size");
        }
    }
}