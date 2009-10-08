 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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