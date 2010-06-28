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

import javax.persistence.EntityNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.model.AbstractCacheElement;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
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
        CallTimeDataCache(Type.Agent), //
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

    private volatile GlobalConditionCache globalCache;
    private Map<Integer, AgentConditionCache> agentCaches;

    private ReentrantReadWriteLock agentReadWriteLock;

    private AgentManagerLocal agentManager;
    private StatusManagerLocal statusManager;

    private AlertConditionCacheCoordinator() {
        agentManager = LookupUtil.getAgentManager();
        statusManager = LookupUtil.getStatusManager();

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
            log.debug("Start reloading global cache");
            globalCache = new GlobalConditionCache();
            log.debug("Finished reloading global cache");
        } catch (Throwable t) {
            try {
                Throwable throwable = t;
                boolean found = false;
                while (throwable != null) {
                    if (throwable instanceof EntityNotFoundException) {
                        // we're trying to load a list of conditions at the very moment one is deleted out from under us
                        statusManager.markGlobalCache();
                        log.debug("EntityNotFoundException thrown during reload, resetting status bit for retry");
                        found = true;
                        break;
                    }
                    throwable = throwable.getCause();
                }
                if (!found) {
                    log.error("Error reloading global cache", t);
                }
            } catch (Throwable inner) {
                // again, don't let any exceptions bubble up to the calling SLSB layer
                log.error("Error while resetting agent status bit during failed global cache reload attempt", inner);
            }
        }
    }

    public void reloadCachesForAgent(int agentId) {
        AgentConditionCache agentCache = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Start reloading cache for agent[id=" + agentId + "]");
            }
            agentCache = new AgentConditionCache(agentId);
            if (log.isDebugEnabled()) {
                log.debug("Finished reloading cache for agent[id=" + agentId + "]");
            }
        } catch (Throwable t) {
            try {
                Throwable throwable = t;
                boolean found = false;
                while (throwable != null) {
                    if (throwable instanceof EntityNotFoundException) {
                        // we're trying to load a list of conditions at the very moment one is deleted out from under us
                        statusManager.updateByAgent(agentId);
                        log.debug("EntityNotFoundException thrown during reload, resetting status bit for retry");
                        found = true;
                        break;
                    }
                    throwable = throwable.getCause();
                }
                if (!found) {
                    log.error("Error reloading cache for agent[id=" + agentId + "]", t);
                }
            } catch (Throwable inner) {
                // again, don't let any exceptions bubble up to the calling SLSB layer
                log.error("Error while resetting agent status bit during failed cache reload attempt for agent[id="
                    + agentId + "]", inner);
            }
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

    public AlertConditionCacheStats checkConditions(CallTimeData... callTimeData) {
        if (callTimeData == null || callTimeData.length == 0) {
            return new AlertConditionCacheStats();
        }

        CallTimeData datum = callTimeData[0];
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
            stats = agentCache.checkConditions(callTimeData);
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

    private Integer getAgentId(CallTimeData datum) {
        try {
            int scheduleId = datum.getScheduleId();
            Integer agentId = agentManager.getAgentIdByScheduleId(scheduleId);
            return agentId;
        } catch (Throwable t) {
            log.error("Error looking up agent by CallTimeData", t);
        }
        return null;
    }

    public int getCacheSize(AlertConditionCacheCoordinator.Cache cache) {
        int result = 0;
        if (cache.type == Cache.Type.Global) {
            result += globalCache.getCacheSize(cache);
        } else if (cache.type == Cache.Type.Agent) {
            List<AgentConditionCache> cachesCopy = null;
            agentReadWriteLock.readLock().lock();
            try {
                cachesCopy = new ArrayList<AgentConditionCache>(agentCaches.values());
            } catch (Throwable t) {
                log.error("Error during getCacheSize", t); // don't let any exceptions bubble up to the calling SLSB layer
            } finally {
                agentReadWriteLock.readLock().unlock();
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
