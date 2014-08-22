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
package org.rhq.coregui.client.gwt;

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
 * API for HAAC console, mostly CRUD operations for entities <code>Server</code>, <code>Agent</code>, 
 * <code>PartitionEvent</code> and <code>AffinityGroup</code>.
 *
 * @author Jiri Kremser
 */
public interface TopologyGWTService extends RemoteService {

    /**
     * Returns all the servers with agent count.
     * 
     * @param pageControl the page control instance
     * @return a list of all available servers (the servers in <code>MAINTENANCE</code> or <code>DOWN</code> mode are included as well)
     * @throws RuntimeException
     */
    PageList<ServerWithAgentCountComposite> getServers(PageControl pageControl) throws RuntimeException;

    /**
     * Deletes the servers with provided ids.
     * 
     * @param serverIds array of server ids
     * @throws RuntimeException
     */
    void deleteServers(int[] serverIds) throws RuntimeException;

    /**
     * Updates the server mode to particular servers.
     * 
     * @param serverIds the array of ids of the servers whose modes are object of update
     * @param mode the new operation mode
     * @throws RuntimeException
     */
    void updateServerManualMaintenance(int[] serverIds, boolean manualMaintenance) throws RuntimeException;

    /**
     * Updates the server.
     * 
     * @param server instance of Server
     * @throws RuntimeException
     */
    void updateServer(Server server) throws RuntimeException;

    /**
     * Returns the list of <code>FailoverListDetails</code> for a particular agent.
     * 
     * @param agentId the id the agent
     * @param pageControl the page control instance
     * @return a list of <code>FailoverListDetails</code> instances
     * @throws RuntimeException
     */
    PageList<FailoverListDetails> getFailoverListDetailsByAgentId(int agentId, PageControl pageControl) throws RuntimeException;;

    /**
     * Finder for <code>PartitionEvent</code> instances
     * 
     * @param criteria the criteria for finding partition events
     * @return a list of <code>codePartitionEvent</code> instances
     * @throws RuntimeException
     */
    PageList<PartitionEvent> findPartitionEventsByCriteria(PartitionEventCriteria criteria) throws RuntimeException;

    /**
     * Finder for <code>Server</code> instances
     * 
     * @param criteria the criteria for finding servers
     * @return a list of <code>Server</code> instances
     * @throws RuntimeException
     */
    PageList<Server> findServersByCriteria(ServerCriteria criteria) throws RuntimeException;

    /**
     * Finder for <code>Agent</code> instances
     * 
     * @param criteria the criteria for finding agents
     * @return a list of <code>Agent</code> instances
     * @throws RuntimeException
     */
    PageList<Agent> findAgentsByCriteria(AgentCriteria criteria) throws RuntimeException;

    /**
     * Request forcing the repartition of the cluster. All agents should eventually reconnects to its most preferred server 
     * (The first one in their failover list.)
     * 
     * @throws RuntimeException
     */
    void cloudPartitionEventRequest() throws RuntimeException;

    /**
     * Deletes all the partition events.
     * 
     * @throws RuntimeException
     */
    void purgeAllEvents() throws RuntimeException;

    /**
     * Deletes some partition events.
     * 
     * @param eventIds the list of ids of partition events to delete
     * @throws RuntimeException
     */
    void deletePartitionEvents(int[] eventIds) throws RuntimeException;

    /**
     * Returns the list of <code>PartitionEventDetails</code> instances.
     * 
     * @param partitionEventId
     * @param pageControl the page control instance
     * @return list of <code>PartitionEventDetails</code> instances
     * @throws RuntimeException
     */
    PageList<PartitionEventDetails> getPartitionEventDetails(int partitionEventId, PageControl pageControl)
        throws RuntimeException;

    /**
     * Returns the list with <code>AffinityGroupCountComposite</code> instances, i.e. affinity groups with agent and server 
     * counts.
     * 
     * @param pageControl the page control instance
     * @return list with <code>AffinityGroupCountComposite</code> instances
     * @throws RuntimeException
     */
    PageList<AffinityGroupCountComposite> getAffinityGroupCountComposites(PageControl pageControl)
        throws RuntimeException;

    /**
     * Deletes some affinity groups.
     * 
     * @param affinityGroupIds array of ids of affinity group to delete
     * @return the number of deleted affinity groups
     * @throws RuntimeException
     */
    int deleteAffinityGroups(int[] affinityGroupIds) throws RuntimeException;

    /**
     * Creates new affinity group.
     * 
     * @param affinityGroup instance of <code>AffinityGroup</code> to create
     * @return the new id of the affinity group
     * @throws RuntimeException
     */
    int createAffinityGroup(AffinityGroup affinityGroup) throws RuntimeException;

    /**
     * Updates existing affinity group.
     * 
     * @param affinityGroup  instance of <code>AffinityGroup</code>
     * @throws RuntimeException
     */
    void updateAffinityGroup(AffinityGroup affinityGroup) throws RuntimeException;

    /**
     * Returns the affinity group based on the provided id.
     * 
     * @param affinityGroupId the id of an affinity group
     * @return the instance of <code>AffinityGroup</code>
     * @throws RuntimeException
     */
    AffinityGroup getAffinityGroupById(int affinityGroupId) throws RuntimeException;

    /**
     * Add servers to an existing affinity group.
     * 
     * @param affinityGroupId id of the affinity group into which the servers should be added
     * @param serverIds array of ids of servers to be added
     * @throws RuntimeException
     */
    void addServersToGroup(int affinityGroupId, Integer[] serverIds) throws RuntimeException;

    /**
     * Removes the servers from the affinity group. There is no need to provide the id of affinity group,
     * because it is part of the server data.
     * 
     * @param serverIds array of ids of servers to be removed
     * @throws RuntimeException
     */
    void removeServersFromGroup(Integer[] serverIds) throws RuntimeException;

    /**
     * Add agents to an existing affinity group.
     * 
     * @param affinityGroupId id of the affinity group into which the agents should be added
     * @param agentIds array of ids of agents to be added
     * @throws RuntimeException
     */
    void addAgentsToGroup(int affinityGroupId, Integer[] agentIds) throws RuntimeException;

    /**
     * Removes the agents from the affinity group. There is no need to provide the id of affinity group,
     * because it is part of the agent data.
     * 
     * @param agentIds array of ids of agents to be removed
     * @throws RuntimeException
     */
    void removeAgentsFromGroup(Integer[] agentIds) throws RuntimeException;
    
    /**
     * Returns the id of managed reource representing this agent instance
     * 
     * @param agentId an id of <code>Agent</code> instance
     * @return id of the associated resource <code>null</code> if there is no associated resource
     * @throws RuntimeException
     */
    Integer getResourceIdOfAgent(int agentId) throws RuntimeException;
}
