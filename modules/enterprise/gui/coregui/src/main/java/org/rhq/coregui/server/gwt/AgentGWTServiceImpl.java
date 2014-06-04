/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.server.gwt;

import java.util.List;

import org.rhq.core.domain.install.remote.AgentInstall;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.gwt.AgentGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Simeon Pinder
 * @author Jirka Kremser
 */
public class AgentGWTServiceImpl extends AbstractGWTServiceImpl implements AgentGWTService {

    private static final long serialVersionUID = 1L;

    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();

    @Override
    public AgentInstall getAgentInstallByAgentName(String agentName) throws RuntimeException {
        try {
            //security handled in AgentManagerBean. requires Inventory_Manager
            return SerialUtility.prepare(agentManager.getAgentInstallByAgentName(getSessionSubject(), agentName),
                "AgentService.getAgentInstallByAgentName");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Agent getAgentForResource(int resourceId) throws RuntimeException {
        try {
            //security handled in AgentManagerBean. requires View_Resource.
            return SerialUtility.prepare(agentManager.getAgentByResourceId(getSessionSubject(), resourceId),
                "AgentService.getAgentForResource");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Boolean pingAgentForResource(int resourceId) throws RuntimeException {
        try {
            //security handled in AgentManagerBean. requires View_Resource.
            return SerialUtility.prepare(agentManager.pingAgentByResourceId(getSessionSubject(), resourceId),
                "AgentService.pingAgentForResource");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public List<Agent> getAllAgents() throws RuntimeException {
        try {
            //security handled in AgentManagerBean. requires View_Resource.
            return SerialUtility.prepare(agentManager.getAllAgents(), "AgentService.getAllAgents");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<Agent> getAgentsByServer(Integer serverId, PageControl pc) throws RuntimeException {
        try {
            //TODO: security handled in AgentManagerBean. requires View_Resource.
            return SerialUtility.prepare(agentManager.getAgentsByServer(getSessionSubject(), serverId, pc),
                "AgentService.getAgentsByServer");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Agent getAgentByID(int agentId) throws RuntimeException {
        try {
            return SerialUtility.prepare(agentManager.getAgentByID(agentId), "AgentService.getAgentByID");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    //    public PageList<Availability> findAvailabilityForResource(int resourceId, PageControl pc) {
    //        try {
    //            return SerialUtility.prepare(availabilityManager.findAvailabilityForResource(getSessionSubject(),
    //                resourceId, pc), "AvailabilityService.findAvailabilityForResource");
    //        } catch (Throwable t) {
    //            throw getExceptionToThrowToClient(t);
    //        }
    //    }
}
