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
package org.rhq.enterprise.server.core.comm;

import mazz.i18n.Logger;
import org.jboss.remoting.InvokerLocator;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryListener;

/**
 * Listens for new agents coming on and going offline.
 *
 * @author John Mazzitelli
 */
public class ServerAutoDiscoveryListener implements AutoDiscoveryListener {
    /**
     * Logger
     */
    private static final Logger LOG = ServerI18NFactory.getLogger(ServerAutoDiscoveryListener.class);

    /**
     * The list to be modified when new agents are seen coming online and old agents going offline.
     */
    private final KnownAgents m_agents;

    /**
     * Constructor for {@link ServerAutoDiscoveryListener}.
     *
     * @param agents the list that will contain all known agents' remote endpoints
     */
    public ServerAutoDiscoveryListener(KnownAgents agents) {
        m_agents = agents;
    }

    /**
     * This adds the new agent's <code>locator</code> URL to the list of known agents.
     *
     * @see AutoDiscoveryListener#serverOnline(InvokerLocator)
     */
    public void serverOnline(InvokerLocator locator) {
        if (m_agents.addAgent(locator)) {
            LOG.debug(ServerI18NResourceKeys.AUTO_DETECTED_NEW_AGENT, locator);
        }

        return;
    }

    /**
     * This removes the given <code>locator</code> URL from the list of known agents.
     *
     * @see AutoDiscoveryListener#serverOffline(InvokerLocator)
     */
    public void serverOffline(InvokerLocator locator) {
        if (m_agents.removeAgent(locator)) {
            LOG.debug(ServerI18NResourceKeys.AUTO_DETECTED_DOWNED_AGENT, locator);
        }

        return;
    }
}