/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.core.clientapi.agent.ping;


/**
 * The sole purpose of this interface is to check whether the agent is reachable and servicing requests.  It
 * differs from <code>AgentClient.ping()</code> which tests only for comm-layer connectivity but not that the agent
 * is initialized and servicing requests.  In general this can be used to surround services with potentially long
 * runtimes. For example:
 * <pre>
 *    if ( agentClient.getPingAgentService(2000L).ping() ) {
 *        agentClient.getDiscoveryAgentService().uninventoryResource(resourceId);
 *    }
 * </pre>
 *
 * @author Jay Shaughnessy
 */
public interface PingAgentService {
    /**
     * This service simply returns immediately and can be used to check whether the agent is reachable
     * and servicing requests.
     *
     * @return the system time on the agent (in ms)
     * @throws Exception if somehow the ping failed
     */
    long ping() throws Exception;
}