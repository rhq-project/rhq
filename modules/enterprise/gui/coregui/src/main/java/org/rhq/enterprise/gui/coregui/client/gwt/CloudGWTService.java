/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.cloud.FailoverListDetails;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;

/**
 * @author Jiri Kremser
 */
public interface CloudGWTService extends RemoteService {

    /**
     * 
     * @return a list of all available servers (the servers in MAINTENANCE mode are included as well)
     * @throws RuntimeException
     */
    List<ServerWithAgentCountComposite> getServers(PageControl pc) throws RuntimeException;

    Server getServerById(int serverId) throws RuntimeException;

    List<Agent> getAgentsByServerName(String serverName) throws RuntimeException;

    void deleteServers(int[] serverIds) throws RuntimeException;

    void updateServerMode(int[] serverIds, Server.OperationMode mode) throws RuntimeException;
    
    List<FailoverListDetails> getFailoverListDetailsByAgentId(int agentId, PageControl pc);

}
