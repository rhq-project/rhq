package org.rhq.enterprise.server.cluster.instance;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;

@Stateless
public class CacheConsistencyManagerBean implements CacheConsistencyManagerLocal {

    private final Log log = LogFactory.getLog(CacheConsistencyManagerBean.class);

    @EJB
    ServerManagerLocal serverManager;

    @EJB
    AlertConditionCacheManagerLocal cacheManager;

    public void reloadServerCacheIfNeeded() {
        String serverName = serverManager.getIdentity();
        List<Agent> agents = serverManager.getAgentsWithStatus();

        // do nothing if nothing to do
        if (agents.size() == 0) {
            log.info("Cache for " + serverName + " is up to date");
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
    }
}
