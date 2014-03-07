/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.ping;

import org.rhq.core.clientapi.agent.ping.PingAgentService;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.agent.AgentServiceStreamRemoter;

/**
 * A simple ping service designed to just return or timeout trying.
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author Jay Shaughnessy
 */
public class PingManager extends AgentService implements PingAgentService, ContainerService {

    public PingManager(AgentServiceStreamRemoter streamRemoter) {
        super(PingAgentService.class, streamRemoter);
    }

    public void shutdown() {
    }

    public long ping() throws Exception {
        return System.currentTimeMillis();
    }
}
