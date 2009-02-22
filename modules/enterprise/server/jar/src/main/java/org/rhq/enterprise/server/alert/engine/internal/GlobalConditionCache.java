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
import org.rhq.enterprise.server.alert.engine.model.AlertConditionOperator;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityCacheElement;
import org.rhq.enterprise.server.alert.engine.model.InvalidCacheElementException;
import org.rhq.enterprise.server.alert.engine.model.ResourceConfigurationCacheElement;
import org.rhq.enterprise.server.alert.engine.model.ResourceOperationCacheElement;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.common.EntityManagerFacadeLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
class GlobalConditionCache extends AbstractConditionCache {

    private Map<Integer, Map<Integer, List<ResourceOperationCacheElement>>> resourceOperationCache;
    private Map<Integer, List<AvailabilityCacheElement>> availabilityCache;
    private Map<Integer, List<ResourceConfigurationCacheElement>> resourceConfigurationCache;

    private AlertConditionManagerLocal alertConditionManager;
    private SubjectManagerLocal subjectManager;
    private EntityManagerFacadeLocal entityManagerFacade;

    public GlobalConditionCache() {
        super();

        resourceOperationCache = new HashMap<Integer, Map<Integer, List<ResourceOperationCacheElement>>>();
        availabilityCache = new HashMap<Integer, List<AvailabilityCacheElement>>();
        resourceConfigurationCache = new HashMap<Integer, List<ResourceConfigurationCacheElement>>();

        alertConditionManager = LookupUtil.getAlertConditionManager();
        subjectManager = LookupUtil.getSubjectManager();
        entityManagerFacade = LookupUtil.getEntityManagerFacade();

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
                AlertConditionCategory.CONTROL, AlertConditionCategory.RESOURCE_CONFIG);

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

                    entityManagerFacade.flush();
                    entityManagerFacade.clear();

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
        AlertConditionOperator alertConditionOperator = AlertConditionCacheUtils.getAlertConditionOperator(
            alertConditionCategory, alertCondition.getComparator(), alertCondition.getOption());

        if (alertConditionCategory == AlertConditionCategory.AVAILABILITY) {
            /*
             * This is a hack, because we're not respecting the persist alertCondition option, we're instead overriding
             * it with AvailabilityType.UP to satisfy the desired semantics.
             *
             * TODO: jmarques - should associate a specific operator with availability UI selections to make biz
             * processing more consistent with the model
             */
            AlertConditionAvailabilityCategoryComposite availabilityComposite = (AlertConditionAvailabilityCategoryComposite) composite;

            try {
                AvailabilityCacheElement cacheElement = new AvailabilityCacheElement(alertConditionOperator,
                    AvailabilityType.UP, availabilityComposite.getAvailabilityType(), alertConditionId);
                addTo("availabilityCache", availabilityCache, availabilityComposite.getResourceId(), cacheElement,
                    alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create AvailabilityCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        availabilityComposite.getAvailabilityType(), AvailabilityType.UP));
            }
        } else if (alertConditionCategory == AlertConditionCategory.CONTROL) {
            AlertConditionControlCategoryComposite controlComposite = (AlertConditionControlCategoryComposite) composite;
            String option = alertCondition.getOption();
            OperationRequestStatus operationRequestStatus = OperationRequestStatus.valueOf(option.toUpperCase());

            try {
                ResourceOperationCacheElement cacheElement = new ResourceOperationCacheElement(alertConditionOperator,
                    operationRequestStatus, alertConditionId);

                // auto-boxing always safe
                addToResourceOperationCache(controlComposite.getResourceId(), controlComposite
                    .getOperationDefinitionId(), cacheElement, alertConditionId, stats);
            } catch (InvalidCacheElementException icee) {
                log.info("Failed to create ResourceOperationCacheElement with parameters: "
                    + AlertConditionCacheUtils.getCacheElementErrorString(alertConditionId, alertConditionOperator,
                        null, operationRequestStatus));
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
                        null, null));
            }

            addTo("resourceConfigurationCache", resourceConfigurationCache, resourceConfigurationComposite
                .getResourceId(), cacheElement, alertConditionId, stats);
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

                processCacheElements(cacheElements, availabilityType, availability.getStartTime().getTime(), stats);
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

    private List<ResourceConfigurationCacheElement> lookupResourceConfigurationCacheElements(int resourceId) {
        return resourceConfigurationCache.get(resourceId); // yup, might be null
    }

    @Override
    public int getCacheSize(Cache cache) {
        if (cache == AlertConditionCacheCoordinator.Cache.AvailabilityCache) {
            return AlertConditionCacheUtils.getMapListCount(availabilityCache);
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