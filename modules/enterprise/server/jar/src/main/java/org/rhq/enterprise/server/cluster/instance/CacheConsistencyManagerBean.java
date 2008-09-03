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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.Agent;
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

    @EJB
    ServerManagerLocal serverManager;

    @EJB
    AlertConditionCacheManagerLocal cacheManager;

    public void reloadServerCacheIfNeeded() {
        List<Agent> agents = serverManager.getAgentsWithStatus();

        // do nothing if nothing to do
        if (agents.size() == 0) {
            return;
        }

        // otherwise print informational messages for poor-man's verification purposes
        for (Agent nextAgent : agents) {
            log.info("Agent[id=" + nextAgent.getId() + ", name=" + nextAgent.getName() + "] is stale ");
            List<String> statusMessages = nextAgent.getStatusMessages();
            for (String nextMessage : statusMessages) {
                log.info(nextMessage);
            }
            nextAgent.clearStatus();
        }

        // finally, perform the reload
        cacheManager.reload();

        String serverName = serverManager.getIdentity();
        log.info("Cache for " + serverName + " is up to date");
    }
}
