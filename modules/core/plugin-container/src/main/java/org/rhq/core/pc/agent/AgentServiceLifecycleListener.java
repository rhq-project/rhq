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
package org.rhq.core.pc.agent;

import org.rhq.core.pc.PluginContainer;

/**
 * Implementations of this interface that are
 * {@link PluginContainer#addAgentServiceLifecycleListener(AgentServiceLifecycleListener) added} to the plugin container
 * will be notified when any {@link AgentService} is started or stopped.
 *
 * <p>This is used, for example, by the agent since it needs to remote all agent services when they are started and
 * "unremote" the agent services when they are shutdown.</p>
 */
public interface AgentServiceLifecycleListener {
    /**
     * Notifies the listener that the plugin container has started the given agent service.
     *
     * @param agentService
     */
    void started(AgentService agentService);

    /**
     * Notifies the listener that the plugin container has stopped the given agent service.
     *
     * @param agentService
     */
    void stopped(AgentService agentService);
}