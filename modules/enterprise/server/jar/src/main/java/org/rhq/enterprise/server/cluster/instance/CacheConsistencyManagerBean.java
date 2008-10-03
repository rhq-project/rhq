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
package org.rhq.enterprise.server.cluster.instance;

import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

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

    private final String TIMER_DATA = "CacheConsistencyManagerBean.reloadServerCacheIfNeeded";

    @SuppressWarnings("unchecked")
    public void scheduleServerCacheReloader() {
        /* each time the webapp is reloaded, it would create 
         * duplicate events if we don't cancel the existing ones
         */
        Collection<Timer> timers = timerService.getTimers();
        for (Timer existingTimer : timers) {
            log.debug("Found timer: " + existingTimer.toString());
            existingTimer.cancel();
        }

        // start it now, and repeat every 30 seconds
        timerService.createTimer(0, 30000, TIMER_DATA);
    }

    @Timeout
    public void handleHeartbeatTimer(Timer timer) {
        timer.cancel();

        try {
            reloadServerCacheIfNeeded();
        } finally {
            // start it now, and repeat every 30 seconds
            timerService.createTimer(0, 30000, TIMER_DATA);
        }
    }

    public void reloadServerCacheIfNeeded() {
        List<Integer> agentIds = serverManager.getAndClearAgentsWithStatus();

        // do nothing if nothing to do
        if (agentIds.size() == 0) {
            return;
        }

        // otherwise print informational messages for poor-man's verification purposes
        long startTime = System.currentTimeMillis();
        for (Integer nextAgentId : agentIds) {
            log.debug("Agent[id=" + nextAgentId + " is stale ");
            cacheManager.reloadCachesForAgent(nextAgentId);
        }
        long endTime = System.currentTimeMillis();

        String serverName = serverManager.getIdentity();
        log.info("Took [" + (endTime - startTime) + "]ms to reload cache for " + serverName);
    }
}
