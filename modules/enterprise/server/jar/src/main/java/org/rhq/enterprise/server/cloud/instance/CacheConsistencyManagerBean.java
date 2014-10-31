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
package org.rhq.enterprise.server.cloud.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;

/**
 * Each server has partitioned alerts condition cache data.  This session bean exists
 * to check whether or not the something has changed in the system that would require
 * the cache to asynchronously reload itself.
 * 
 * @author Joseph Marques
 */
@Stateless
public class CacheConsistencyManagerBean implements CacheConsistencyManagerLocal {

    private final Log log = LogFactory.getLog(CacheConsistencyManagerBean.class);

    @Resource
    TimerService timerService;

    @EJB
    ServerManagerLocal serverManager;

    @EJB
    AlertConditionCacheManagerLocal cacheManager;

    @EJB
    CacheConsistencyManagerLocal cacheConsistencyManager;

    @Override
    public void scheduleServerCacheReloader() {
        /* each time the webapp is reloaded, it would create 
         * duplicate events if we don't cancel the existing ones
         */
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            log.debug("Found timer - attempting to cancel: " + existingTimer.toString());
            try {
                existingTimer.cancel();
            } catch (Exception e) {
                log.warn("Failed in attempting to cancel timer: " + existingTimer.toString());
            }
        }

        timerService.createIntervalTimer(30000L, 30000L, new TimerConfig(null, false));
    }

    @Override
    @Timeout
    public void handleHeartbeatTimer(Timer timer) {
        try {
            cacheConsistencyManager.reloadServerCacheIfNeededNSTx();
        } catch (Throwable t) {
            log.error("Failed to reload server cache if needed - will try again later. Cause: " + t);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void reloadServerCacheIfNeededNSTx() {
        // try reload the global cache separate from the agent caches for purposes of isolated failures
        reloadGlobalCacheIfNeeded();
        reloadAgentCachesAsNeeded();
    }

    private void reloadGlobalCacheIfNeeded() {
        try {
            boolean hadServerStatus = serverManager.getAndClearServerStatus();
            if (hadServerStatus == false) {
                if (log.isDebugEnabled()) {
                    log.debug("Global cache does not need reloading");
                }
                return;
            }

            long startTime = System.currentTimeMillis();
            cacheManager.reloadGlobalCache();
            long endTime = System.currentTimeMillis();

            String serverName = serverManager.getIdentity();
            log.info(serverName + " took [" + (endTime - startTime) + "]ms to reload global cache");
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to reload global cache", t);
            } else {
                log.error("Failed to reload global cache, cause: " + t.getMessage());
            }
        }
    }

    private void reloadAgentCachesAsNeeded() {
        /* 
         * catch absolutely everything, so that even if this REQUIRES_NEW transaction rollback, 
         * it doesn't rollback the caller (where we reschedule the TIMER to trigger this job again
         */
        List<Integer> agentIds = new ArrayList<Integer>();
        try {
            agentIds = serverManager.getAndClearAgentsWithStatus();

            // do nothing if nothing to do
            if (agentIds.size() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("No agent caches need reloading");
                }
                return;
            }

            // otherwise print informational messages for poor-man's verification purposes
            long startTime = System.currentTimeMillis();
            for (Integer nextAgentId : agentIds) {
                log.debug("Agent[id=" + nextAgentId + "] is stale ");
                cacheManager.reloadCachesForAgent(nextAgentId);
            }
            long endTime = System.currentTimeMillis();

            String serverName = serverManager.getIdentity();

            if (log.isDebugEnabled()) {
                log.debug(serverName + " took [" + (endTime - startTime)
                    + "]ms to reload cache for the follow agentIds: " + agentIds + " agents");
            } else {
                log.info(serverName + " took [" + (endTime - startTime) + "]ms to reload cache for " + agentIds.size()
                    + " agents");
            }
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to reload caches for the following agents: " + agentIds, t);
            } else {
                log.error("Failed to reload caches for the following agents: " + agentIds + ", cause: "
                    + t.getMessage());
            }
        }
    }

}
