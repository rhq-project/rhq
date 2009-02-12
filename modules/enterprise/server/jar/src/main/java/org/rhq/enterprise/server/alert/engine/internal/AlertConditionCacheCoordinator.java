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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This singleton that contains multiple {@link AgentConditionCache}s and one {@link GlobalConditionCache}.
 * Each {@link AgentConditionCache} will maintain {@link AbstractCacheElement}s for data that can ONLY be
 * reported by an agent, and thus can be perfectly segmented on an agent-by-agent basis.  On the other hand,
 * the {@link GlobalConditionCache} will maintain {@link AbstractCacheElement}s for data that can either be
 * agent-side or server-side initiated.
 * 
 * This manager forms a centralized interface through which to interact with the children caches.
 * 
 * @author Joseph Marques
 */
public final class AlertConditionCacheCoordinator {

    private static final Log log = LogFactory.getLog(AlertConditionCacheCoordinator.class);

    private static final AlertConditionCacheCoordinator instance = new AlertConditionCacheCoordinator();

    public enum Cache {
        MeasurementDataCache(Type.Agent), //
        MeasurementTraitCache(Type.Agent), //
        ResourceOperationCache(Type.Global), //
        AvailabilityCache(Type.Global), //
        EventsCache(Type.Agent), //
        ResourceConfigurationCache(Type.Global);

        public enum Type {
            Global, //
            Agent;
        };

        public Type type;

        Cache(Type type) {
            this.type = type;
        }
    }

    private GlobalConditionCache globalCache;
    private Map<Integer, AgentConditionCache> agentCaches;

    private ReentrantReadWriteLock agentReadWriteLock;

    private AgentManagerLocal agentManager;

    private AlertConditionCacheCoordinator() {
        agentManager = LookupUtil.getAgentManager();

        globalCache = new GlobalConditionCache();

        // create the collections ahead of time
        agentCaches = new HashMap<Integer, AgentConditionCache>();
        agentReadWriteLock = new ReentrantReadWriteLock();
    }

    public static AlertConditionCacheCoordinator getInstance() {
        return instance;
    }

    public void reloadGlobalCache() {
        try {
            // simply "forget" about the old cache, let the JVM release the memory in time
            globalCache = new GlobalConditionCache();
            log.debug("Reloaded global cache");
        } catch (Throwable t) {
            log.error("Error reloading global cache", t); // don't let any exceptions bubble up to the calling SLSB layer
        }
    }

    public void reloadCachesForAgent(int agentId) {
        AgentConditionCache agentCache = null;
        try {
            agentCache = new AgentConditionCache(agentId);
        } catch (Throwable t) {
            log.error("Error reloading cache for agent[id=" + agentId + "]", t); // don't let any exceptions bubble up to the calling SLSB layer
        }

        if (agentCache != null) {
            agentReadWriteLock.writeLock().lock();
            try {
                // simply "forget" about the old cache, let the JVM release the memory in time
                agentCaches.put(agentId, agentCache);
                log.debug("Reloaded agent[id=" + agentId + "] cache");
            } catch (Throwable t) {
                log.error("Error reloading cache for agent[id=" + agentId + "]", t); // don't let any exceptions bubble up to the calling SLSB layer
            } finally {
                agentReadWriteLock.writeLock().unlock();
            }
        }
    }

    public AlertConditionCacheStats checkConditions(MeasurementData... measurementData) {
        if (measurementData == null || measurementData.length == 0) {
            return new AlertConditionCacheStats();
        }

        MeasurementData datum = measurementData[0];
        Integer agentId = getAgentId(datum);
        if (agentId == null) {
            log.error("Could not find agent for scheduleId = " + datum.getScheduleId());
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = null;
        AgentConditionCache agentCache = null;
        agentReadWriteLock.readLock().lock();
        try {
            agentCache = agentCaches.get(agentId);
        } catch (Throwable t) {
            log.error("Error during checkConditions", t); // don't let any exceptions bubble up to the calling SLSB layer
        } finally {
            agentReadWriteLock.readLock().unlock();
        }
        if (agentCache != null) {
            stats = agentCache.checkConditions(measurementData);
        } else {
            stats = new AlertConditionCacheStats();
        }

        return stats;
    }

    public AlertConditionCacheStats checkConditions(OperationHistory operationHistory) {
        AlertConditionCacheStats stats = null;
        try {
            stats = globalCache.checkConditions(operationHistory);
        } catch (Throwable t) {
            log.error("Error during checkConditions", t); // don't let any exceptions bubble up to the calling SLSB layer
        }
        if (stats == null) {
            stats = new AlertConditionCacheStats();
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(ResourceConfigurationUpdate update) {
        AlertConditionCacheStats stats = null;
        try {
            stats = globalCache.checkConditions(update);
        } catch (Throwable t) {
            log.error("Error during checkConditions", t); // don't let any exceptions bubble up to the calling SLSB layer
        }
        if (stats == null) {
            stats = new AlertConditionCacheStats();
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(EventSource source, Event... events) {
        if (source == null) {
            return new AlertConditionCacheStats();
        }

        Integer agentId = getAgentId(source);
        if (agentId == null) {
            log.error("Could not find agent for resourceId = " + source.getResource().getId());
            return new AlertConditionCacheStats();
        }

        AlertConditionCacheStats stats = null;
        AgentConditionCache agentCache = null;
        agentReadWriteLock.readLock().lock();
        try {
            agentCache = agentCaches.get(agentId);
        } catch (Throwable t) {
            log.error("Error during checkConditions", t); // don't let any exceptions bubble up to the calling SLSB layer
        } finally {
            agentReadWriteLock.readLock().unlock();
        }
        if (agentCache != null) {
            stats = agentCache.checkConditions(source, events);
        } else {
            stats = new AlertConditionCacheStats();
        }
        return stats;
    }

    public AlertConditionCacheStats checkConditions(Availability... availabilities) {
        AlertConditionCacheStats stats = null;
        try {
            stats = globalCache.checkConditions(availabilities);
        } catch (Throwable t) {
            log.error("Error during checkConditions", t); // don't let any exceptions bubble up to the calling SLSB layer
        }
        if (stats == null) {
            stats = new AlertConditionCacheStats();
        }
        return stats;
    }

    private Integer getAgentId(EventSource source) {
        try {
            int resourceId = source.getResource().getId();
            Integer agentId = agentManager.getAgentIdByResourceId(resourceId);
            return agentId;
        } catch (Throwable t) {
            log.error("Error looking up agent by EventSource", t);
        }
        return null;
    }

    private Integer getAgentId(MeasurementData datum) {
        try {
            int scheduleId = datum.getScheduleId();
            Integer agentId = agentManager.getAgentIdByScheduleId(scheduleId);
            return agentId;
        } catch (Throwable t) {
            log.error("Error looking up agent by MeasurementData", t);
        }
        return null;
    }

    public int getCacheSize(AlertConditionCacheCoordinator.Cache cache) {
        int result = 0;
        if (cache.type == Cache.Type.Global) {
            result += globalCache.getCacheSize(cache);
        } else if (cache.type == Cache.Type.Agent) {
            List<AgentConditionCache> cachesCopy = null;
            synchronized (agentReadWriteLock) {
                cachesCopy = new ArrayList<AgentConditionCache>(agentCaches.values());
            }

            for (AgentConditionCache agentCache : cachesCopy) {
                result += agentCache.getCacheSize(cache);
            }
        } else {
            log.error("The " + AlertConditionCacheCoordinator.class.getSimpleName()
                + " does not support getting the size for caches of type " + cache.type);
        }
        return result;
    }

    public Map<String, Integer> getCacheCounts() {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        for (Cache cache : Cache.values()) {
            counts.put(cache.name(), getCacheSize(cache));
        }
        return counts;
    }
}
