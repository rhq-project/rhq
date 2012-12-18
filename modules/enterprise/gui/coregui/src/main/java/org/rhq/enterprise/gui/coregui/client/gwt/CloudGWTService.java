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

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.cloud.FailoverListDetails;
import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.composite.AffinityGroupCountComposite;
import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.criteria.AgentCriteria;
import org.rhq.core.domain.criteria.PartitionEventCriteria;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

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

    void updateServer(Server server) throws RuntimeException;

    List<FailoverListDetails> getFailoverListDetailsByAgentId(int agentId, PageControl pc) throws RuntimeException;;

    PageList<PartitionEvent> findPartitionEventsByCriteria(PartitionEventCriteria criteria) throws RuntimeException;

    PageList<Server> findServersByCriteria(ServerCriteria criteria) throws RuntimeException;

    PageList<Agent> findAgentsByCriteria(AgentCriteria criteria) throws RuntimeException;

    void cloudPartitionEventRequest() throws RuntimeException;

    void purgeAllEvents() throws RuntimeException;

    void deletePartitionEvents(int[] eventIds) throws RuntimeException;

    PageList<PartitionEventDetails> getPartitionEventDetails(int partitionEventId, PageControl pageControl)
        throws RuntimeException;

    PageList<AffinityGroupCountComposite> getAffinityGroupCountComposites(PageControl pageControl)
        throws RuntimeException;

    int deleteAffinityGroups(int[] affinityGroupIds) throws RuntimeException;

    int createAffinityGroup(AffinityGroup affinityGroup) throws RuntimeException;

    void updateAffinityGroup(AffinityGroup affinityGroup) throws RuntimeException;

    PageList<Server> getServerMembersByAffinityGroupId(int affinityGroupId, PageControl pageControl)
        throws RuntimeException;

    PageList<Server> getServerNonMembersByAffinityGroupId(int affinityGroupId, PageControl pageControl)
        throws RuntimeException;

    PageList<Agent> getAgentMembersByAffinityGroupId(int affinityGroupId, PageControl pageControl)
        throws RuntimeException;

    PageList<Agent> getAgentNonMembersByAffinityGroupId(int affinityGroupId, PageControl pageControl)
        throws RuntimeException;

    AffinityGroup getAffinityGroupById(int affinityGroupId) throws RuntimeException;

    void addServersToGroup(int affinityGroupId, Integer[] serverIds) throws RuntimeException;

    void removeServersFromGroup(Integer[] serverIds) throws RuntimeException;

    void addAgentsToGroup(int affinityGroupId, Integer[] agentIds) throws RuntimeException;

    void removeAgentsFromGroup(Integer[] agentIds) throws RuntimeException;
}
