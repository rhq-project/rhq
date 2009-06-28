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
package org.rhq.enterprise.server.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.annotation.IgnoreDependency;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.cloud.FailoverList;
import org.rhq.core.domain.cloud.FailoverListDetails;
import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.composite.FailoverListComposite;
import org.rhq.core.domain.cloud.composite.FailoverListDetailsComposite;
import org.rhq.core.domain.cloud.composite.FailoverListComposite.ServerEntry;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.AgentManagerLocal;

/**
 * This session beans acts as the single interface with which the distribution algorithm
 * will interact.  The distribution algorithm runs as a result of various changes in the
 * system including but not limited to: newly registering agents, currently connecting
 * agents, cloud membership changes (server added/removed), and redistributions according 
 * to agent load.  The result of the distribution algorithm is a single (or a set of)
 * {@link FailoverList} objects that are sent down to the connected agents.  The agents
 * then use these lists to determine which server to fail over to, if their primary server
 * is unreachable and/or goes down.
 * 
 * @author Joseph Marques
 * @author Jay Shaughnessy
 */
@Stateless
public class FailoverListManagerBean implements FailoverListManagerLocal {
    private final Log log = LogFactory.getLog(FailoverListManagerBean.class);

    /** The variation in load between most loaded and least loaded server that indicates balanced load. */
    private static final double ACCEPTABLE_DISPARITY = 0.10;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    @IgnoreDependency
    CloudManagerLocal cloudManager;

    @EJB
    AgentManagerLocal agentManager;

    @EJB
    FailoverListManagerLocal failoverListManager;

    public FailoverListComposite getExistingForSingleAgent(String agentName) {
        Agent agent = agentManager.getAgentByName(agentName);

        if (null == agent) {
            throw new IllegalArgumentException("No agent found for registration name: " + agentName);
        }

        return doGetExistingForSingleAgent(agent);
    }

    private FailoverListComposite doGetExistingForSingleAgent(Agent agent) {

        FailoverListComposite result = null;
        Query query = entityManager.createNamedQuery(FailoverList.QUERY_GET_VIA_AGENT);
        query.setParameter("agent", agent);
        try {
            FailoverList serverList = (FailoverList) query.getSingleResult();

            List<ServerEntry> serverEntries = new ArrayList<ServerEntry>();
            for (FailoverListDetails next : serverList.getServerList()) {
                serverEntries.add(next.getServer().getServerEntry());
            }

            result = new FailoverListComposite(serverEntries);

        } catch (NoResultException e) {
            result = null;
        }

        return result;
    }

    public FailoverListComposite getForSingleAgent(PartitionEvent event, String agentName) {
        // If a server list already exists then just return it
        Agent agent = agentManager.getAgentByName(agentName);

        if (null == agent) {
            throw new IllegalArgumentException("No agent found for registration name: " + agentName);
        }

        FailoverListComposite result = doGetExistingForSingleAgent(agent);

        if (null == result) {
            result = generateServerList(event, agent);
        }

        return result;
    }

    private FailoverListComposite generateServerList(PartitionEvent event, Agent agent) {
        List<Server> servers = cloudManager.getAllCloudServers();
        List<Agent> agents = new ArrayList<Agent>(1);

        agents.add(agent);

        // get the current agent assignments for the servers
        // TODO (jshaughn) Note that "load" in the query name is not true load but rather the count of agents assigned to
        // each server (by server list ordinal).  This is fine until we decide to introduce relative load values for the
        // agents.  Even at that this algorithm may be ok for adding new agents and defer to a full repartition to apply
        // agent-specific load factors.
        Query query = entityManager.createNamedQuery(FailoverListDetails.QUERY_GET_ASSIGNED_LOADS);
        @SuppressWarnings("unchecked")
        List<FailoverListDetailsComposite> existingLoads = query.getResultList();

        Map<Agent, FailoverListComposite> results = getForAgents(event, servers, agents, existingLoads);

        return (results.get(agent));
    }

    public Map<Agent, FailoverListComposite> refresh(PartitionEvent event) {
        List<Server> servers = cloudManager.getAllCloudServers();
        List<Agent> agents = agentManager.getAllAgents();

        return getForAgents(event, servers, agents, null);
    }

    public Map<Agent, FailoverListComposite> refresh(PartitionEvent event, List<Server> servers, List<Agent> agents) {

        // clear out the existing server lists because we're going to generate new ones for all agents
        for (Agent next : agents) {
            deleteServerListsForAgent(next);
        }

        failoverListManager.clear();

        return getForAgents(event, servers, agents, null);
    }

    private Map<Agent, FailoverListComposite> getForAgents(PartitionEvent event, List<Server> servers,
        List<Agent> agents, List<FailoverListDetailsComposite> existingLoads) {
        Map<Agent, FailoverListComposite> result = new HashMap<Agent, FailoverListComposite>(agents.size());

        // create a bucket for each server to which we will assign agents 
        List<ServerBucket> buckets = new ArrayList<ServerBucket>(servers.size());
        for (Server next : servers) {
            buckets.add(new ServerBucket(next));
        }

        // initialize the result map
        Map<Agent, List<ServerBucket>> agentServerListMap = new HashMap<Agent, List<ServerBucket>>(agents.size());
        for (Agent next : agents) {
            agentServerListMap.put(next, new ArrayList<ServerBucket>(servers.size()));
        }

        // assign server lists level by level: primary, then secondary, the tertiary, etc        
        for (int level = 0; (level < servers.size()); ++level) {

            // Initialize the bucket loads for the next round
            initBuckets(buckets, existingLoads, level);

            // assign a server for this level to each agent, balancing as we go
            for (Agent next : agents) {

                List<ServerBucket> serverList = agentServerListMap.get(next);

                // When assigning primary (i.e. level 0), supply the current primary as the preferred server.
                // This should reduce connection churn by letting most agents stay put (but affects balancing, we'll deal with
                // that below)
                ServerBucket bestBucket = null;

                // Rotate the list (makes the last entry the first entry) on each iteration. This
                // enhances bucket distribution amongst the levels and ensures that we don't starve
                // buckets at the end of the list.
                Collections.rotate(buckets, 1);

                if ((0 == level) && (null != next.getServer())) {
                    bestBucket = ServerBucket.getBestBucket(buckets, serverList, next.getAffinityGroup(), next
                        .getServer().getName());
                } else {
                    bestBucket = ServerBucket.getBestBucket(buckets, serverList, next.getAffinityGroup(), null);
                }

                if (null == bestBucket) {
                    // this should never happen but let's defensively check and log
                    log.error("Unexpected Condition! null bucket in getForAllAgents()");
                    continue;
                }

                serverList.add(bestBucket);
                // note that assigned load takes into consideration compute power of the server
                bestBucket.assignedLoad += (getAgentLoad(next) / bestBucket.computePower);
                bestBucket.assignedAgents.add(next);
            }

            // For debugging logServerList("Level " + level, agentServerListMap);

            // The first pass does a best-effort balancing as it goes but may need further balancing because:
            // - the assignment of primary servers tries to retain the current primary server of an existing agent.
            //   This disrupts the load balancing (but reduces churn).
            // - the algorithm is greedy, assigning servers as they are available, this can overload a server near the
            //   end of assignments (due to, for example, constraints avoiding server duplication in a server list).
            // Now, if necessary for load balance, force some agents to new servers.
            if (balanceLoad(buckets, agentServerListMap)) {
                // for debugging logServerList("Forced Rebalance!", agentServerListMap);
            }
        }

        // generate the result Map
        for (Agent next : agentServerListMap.keySet()) {
            List<ServerEntry> serverEntries = new ArrayList<ServerEntry>(servers.size());

            for (ServerBucket bucket : agentServerListMap.get(next)) {
                serverEntries.add(bucket.serverEntry);
            }

            result.put(next, new FailoverListComposite(serverEntries));
        }

        /* now that the intense in-memory manipulation is complete, let's do the stuff that needs to persist the
         * results to the database; clear out the existing server lists **just** before persisting the new ones 
         * to keep row lock hold time low
         */
        failoverListManager.persist(event, agentServerListMap);

        return result;
    }

    private void initBuckets(List<ServerBucket> buckets, List<FailoverListDetailsComposite> existingLoads, int level) {
        for (ServerBucket bucket : buckets) {
            bucket.assignedLoad = 0.0;
            bucket.assignedAgents.clear();

            if (null != existingLoads) {
                int serverId = bucket.server.getId();

                for (FailoverListDetailsComposite existingLoad : existingLoads) {
                    if ((existingLoad.ordinal == level) && (existingLoad.serverId == serverId)) {
                        bucket.assignedLoad = (existingLoad.assignedAgentCount / bucket.computePower);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Force agents to new servers, if possible, to give us better load balance. A perfect balance is not necessarily
     * produced due to:<pre>
     * Currently, this algorithm will not break affinity.
     * An ACCEPTABLE_DISPARITY between the high server load and low server load is achieved
     * no legal swaps are possible.
     * <pre> 
     */
    private boolean balanceLoad(List<ServerBucket> buckets, Map<Agent, List<ServerBucket>> agentServerListMap) {
        boolean done = false;
        boolean rebalanced = false;

        // need at least two buckets to balance
        if (buckets.size() < 2)
            return false;

        do {
            // sort buckets from high load to low load
            Collections.sort(buckets, new Comparator<ServerBucket>() {
                public int compare(ServerBucket bucket1, ServerBucket bucket2) {
                    return (bucket2.assignedLoad > bucket1.assignedLoad) ? 1 : -1;
                }
            });

            ServerBucket lowBucket = buckets.get(buckets.size() - 1);

            // if the load disparity is acceptable then we're done.
            if (getLoadDisparity(buckets.get(0).assignedLoad, lowBucket.assignedLoad) < ACCEPTABLE_DISPARITY) {
                done = true;
                continue;
            }

            // find an agent to move by traversing the buckets from high to low (excluding lowest bucket)
            for (ServerBucket bucket : buckets) {

                // if we've looked in all of the buckets and found nothing to move then we're done
                if (bucket == lowBucket) {
                    done = true;
                    break;
                }

                AffinityGroup affinityGroup = bucket.server.getAffinityGroup();
                boolean checkAffinity = ((null != affinityGroup) && !affinityGroup.equals(lowBucket.server
                    .getAffinityGroup()));
                int highIndex = -1;
                double highLoad = 0.0;
                double load = 0.0;

                for (int i = 0, size = bucket.assignedAgents.size(); (i < size); ++i) {
                    Agent agent = bucket.assignedAgents.get(i);

                    // we don't move an agent with satisfied affinity to a bucket that breaks affinity
                    if (checkAffinity && affinityGroup.equals(agent.getAffinityGroup())) {
                        continue;
                    }

                    // we don't move an agent that is already assigned to lowBucket
                    if (agentServerListMap.get(agent).contains(lowBucket)) {
                        continue;
                    }

                    load = getAgentLoad(agent);

                    if (load > highLoad) {
                        // protect against a move that would send too much load to the lowBucket, effectively just
                        // reversing the problem and allowing this algorithm to thrash. Don't allow a move that
                        // increases the lowBucket load higher than the current bucket.
                        if (!((lowBucket.assignedLoad + load) > (bucket.assignedLoad - load))) {
                            highIndex = i;
                            highLoad = load;
                        }
                    }
                }

                // If we found an agent to move then make the move, otherwise look in the next bucket
                if (highIndex > -1) {
                    Agent agent = bucket.assignedAgents.remove(highIndex);
                    lowBucket.assignedAgents.add(agent);
                    agentServerListMap.get(agent).remove(bucket);
                    agentServerListMap.get(agent).add(lowBucket);
                    lowBucket.assignedLoad += highLoad;
                    bucket.assignedLoad -= highLoad;
                    rebalanced = true;
                    break;
                }
            }
        } while (!done);

        return rebalanced;
    }

    private double getLoadDisparity(Double highLoad, Double lowLoad) {
        return ((highLoad - lowLoad) / highLoad);
    }

    // TODO (jshaughn) figure out how to measure agent load. It should be relative to all other agents, probably normalized such that the average agent
    // is load 1.0. All agents must have positive load. If the load needs to be computed here perhaps it should be stored on the AgentServerList
    // to avoid recalculation, if it is expensive.
    private double getAgentLoad(Agent agent) {
        if (null == agent)
            return 0.0;

        return 1.0;
    }

    @SuppressWarnings("unused")
    private void logServerList(String debugTitle, Map<Agent, List<ServerBucket>> agentServerListMap) {

        //if (!log.isInfoEnabled())
        //    return;

        StringBuilder sb = new StringBuilder("\nServerList (");
        sb.append(debugTitle);
        sb.append(") :");

        for (Agent agent : agentServerListMap.keySet()) {
            sb.append("\n\n Agent: " + agent.getName());
            for (ServerBucket bucket : agentServerListMap.get(agent)) {
                sb.append("\n   ");
                sb.append(bucket.assignedLoad);
                sb.append(" : ");
                sb.append(bucket.server.getName());
            }
        }

        sb.append("\n\n");
        System.out.println(sb.toString());
        log.info(sb.toString());
    }

    public void deleteServerListsForAgent(Agent agent) {
        Query query1 = entityManager.createNamedQuery(FailoverListDetails.QUERY_DELETE_VIA_AGENT);
        Query query2 = entityManager.createNamedQuery(FailoverList.QUERY_DELETE_VIA_AGENT);
        query1.setParameter("agent", agent);
        query2.setParameter("agent", agent);
        query1.executeUpdate();
        query2.executeUpdate();
    }

    public void deleteServerListDetailsForServer(int serverId) {
        Query query = entityManager.createNamedQuery(FailoverListDetails.QUERY_DELETE_VIA_SERVER);
        query.setParameter("serverId", serverId);
        query.executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void clear() {
        Query query = entityManager.createNamedQuery(FailoverListDetails.QUERY_TRUNCATE);
        query.executeUpdate();
        query = entityManager.createNamedQuery(FailoverList.QUERY_TRUNCATE);
        query.executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void persist(PartitionEvent event, Map<Agent, List<ServerBucket>> agentServerListMap) {
        FailoverList fl = null;
        FailoverListDetails failoverListDetails = null;
        PartitionEventDetails eventDetails = null;
        List<ServerBucket> servers = null;

        for (Agent next : agentServerListMap.keySet()) {
            fl = new FailoverList(event, next);
            entityManager.persist(fl);

            servers = agentServerListMap.get(next);

            for (int i = 0, size = servers.size(); (i < size); ++i) {
                Server server = entityManager.find(Server.class, servers.get(i).server.getId());
                failoverListDetails = new FailoverListDetails(fl, i, server);
                entityManager.persist(failoverListDetails);
                // event details only shows the current primary server topology
                if (0 == i) {
                    eventDetails = new PartitionEventDetails(event, next, server);
                    entityManager.persist(eventDetails);
                }
            }
        }
    }
}
