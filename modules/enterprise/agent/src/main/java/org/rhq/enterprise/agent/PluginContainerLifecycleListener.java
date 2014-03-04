/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

package org.rhq.enterprise.agent;

import org.rhq.core.clientapi.agent.lifecycle.PluginContainerLifecycle;

/**
 * @author Lukas Krejci
 * @since 4.11
 */
public final class PluginContainerLifecycleListener implements PluginContainerLifecycle {
    private final AgentMain agent;

    public PluginContainerLifecycleListener(AgentMain agent) {
        this.agent = agent;
    }

    @Override
    public void start() {
        agent.startPluginContainer(0L);
    }

    @Override
    public void stop() {
        agent.shutdownPluginContainer();
    }

    @Override
    public void updatePlugins() {
        agent.updatePlugins();
        stop();
        start();
    }
}
