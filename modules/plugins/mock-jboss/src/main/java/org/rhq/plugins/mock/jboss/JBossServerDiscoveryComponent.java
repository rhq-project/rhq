/*
 * JBoss, a division of Red Hat.
 * Copyright 2005-2007, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.plugins.mock.jboss;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.mock.jboss.scenario.ScenarioServer;

/**
 * Author: Jason Dobies
 */
public class JBossServerDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log LOG = LogFactory.getLog(JBossServerDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        LOG.info("Discovering resources of Server Type: " + context.getResourceType());

        ScenarioLoader scenarioLoader = ScenarioLoader.getInstance();

        List<ScenarioServer> scenarioServers = scenarioLoader.getServers();
        Set<DiscoveredResourceDetails> servers = new HashSet<DiscoveredResourceDetails>();
        for (ScenarioServer scenarioServer : scenarioServers) {
            DiscoveredResourceDetails server = new DiscoveredResourceDetails(context.getResourceType(), scenarioServer
                .getInstallPath(), scenarioServer.getServerName(), scenarioServer.getVersion(), "mock jboss server",
                null, null);
            servers.add(server);
        }

        LOG.info("Discovered " + servers.size() + " servers");

        return servers;
    }
}