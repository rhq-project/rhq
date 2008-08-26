package org.rhq.enterprise.server.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cluster.AffinityGroup;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.cluster.composite.FailoverListComposite.ServerEntry;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.core.AgentManagerLocal;

@Stateless
public class FailoverListManagerBean implements FailoverListManagerLocal {
    private final Log log = LogFactory.getLog(FailoverListManagerBean.class);

    /** The variation in load between most loaded and least loaded server that indicates balanced load. */
    private static final double ACCEPTABLE_DISPARITY = 0.10;

    @EJB
    ClusterManagerLocal clusterManager;

    @EJB
    AgentManagerLocal agentManager;

    public FailoverListComposite getForSingleAgent(String agentRegistrationToken) {
        /* 
         * dummy implementation that return a simple FailoverList
         * until the distribution algorithm is written 
         */
        List<Server> servers = clusterManager.getAllServers();
        List<ServerEntry> serverEntries = new ArrayList<ServerEntry>();
        for (Server next : servers) {
            serverEntries.add(next.getServerEntry());
        }
        FailoverListComposite results = new FailoverListComposite(serverEntries);
        return results;

        // For existing agents I think what we can do here is return the server list we've previously generated. For new
        // agents we need to perform the whole she-bang for the single agent.
    }

    // Currently assigns to all known agents. This seems right even though some "down" agents may be dead and never come back
    // online. That is really a separate design decision and is subject to change. 
    public Map<Agent, FailoverListComposite> getForAllAgents() {
        List<Server> servers = clusterManager.getAllServers();
        List<Agent> agents = agentManager.getAllAgents();

        return getForAgents(servers, agents);
    }

    // This is primarily a testing entry point
    public Map<Agent, FailoverListComposite> getForAgents(List<Server> servers, List<Agent> agents) {
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
                    LogFactory.getLog(FailoverListManagerBean.class).error(
                        "Unexpected Condition! null bucket in getForAllAgents()");
                    continue;
                }

                serverList.add(bestBucket);
                // note that assigned load takes into consideration compute power of the server
                bestBucket.assignedLoad += (getAgentLoad(next) / bestBucket.computePower);
                bestBucket.assignedAgents.add(next);
            }

            logServerList("Level " + level, agentServerListMap);

            // The first pass does a best-effort balancing as it goes but may need further balancing because:
            // - the assignment of primary servers tries to retain the current primary server of an existing agent.
            //   This disrupts the load balancing (but reduces churn).
            // - the algorithm is greedy, assigning servers as they are available, this can overload a server near the
            //   end of assignments (due to, for example, constraints avoiding server duplication in a server list).
            // Now, if necessary for load balance, force some agents to new servers.
            if (balanceLoad(buckets, agentServerListMap)) {
                this.logServerList("Forced Rebalance!", agentServerListMap);
            }

            // Initialize the bucket loads for the next round
            for (ServerBucket bucket : buckets) {
                bucket.assignedLoad = 0.0;
                bucket.assignedAgents.clear();
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

        return result;
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

    private static class ServerBucket {
        Server server;
        ServerEntry serverEntry;
        int computePower;
        double assignedLoad;
        List<Agent> assignedAgents;

        ServerBucket(Server server) {
            this.server = server;
            this.serverEntry = server.getServerEntry();
            // TODO get the computePower from the server
            this.computePower = 1;
            this.assignedLoad = 0.0;
            assignedAgents = new ArrayList<Agent>();
        }

        static ServerBucket getBestBucket(List<ServerBucket> buckets, List<ServerBucket> usedBuckets,
            AffinityGroup affinityGroup, String preferredServerName) {
            ServerBucket result = null;

            // if the preferred server is available and does not break affinity, use it
            if ((null != preferredServerName)
                && (null == ServerBucket.getBucketByName(usedBuckets, preferredServerName))) {
                result = ServerBucket.getBucketByName(buckets, preferredServerName);
                if ((null != result) && (null != affinityGroup)
                    && (!affinityGroup.equals(result.server.getAffinityGroup()))) {
                    result = null;
                }
            }

            if (null != result)
                return result;

            for (ServerBucket next : buckets) {

                if (null == ServerBucket.getBucketByName(usedBuckets, next.server.getName())) {
                    if (null == result) {
                        // start with the first available candidate                        
                        result = next;
                        continue;
                    }

                    if (null == affinityGroup) {
                        if (next.assignedLoad < result.assignedLoad) {
                            result = next;
                        }

                        continue;
                    }

                    // affinity logic                    
                    if (!affinityGroup.equals(result.server.getAffinityGroup())) {
                        // always prefer affinity
                        if (affinityGroup.equals(next.server.getAffinityGroup())) {
                            result = next;
                        } else if (next.assignedLoad < result.assignedLoad) {
                            result = next;
                        }
                    } else if (affinityGroup.equals(next.server.getAffinityGroup())
                        && (next.assignedLoad < result.assignedLoad)) {

                        // if affinity is satisfied and assigned load is preferable, use this candidate
                        result = next;
                    }
                }
            }

            return result;
        }

        static ServerBucket getBucketByName(List<ServerBucket> buckets, String serverName) {
            for (ServerBucket next : buckets) {
                if (next.server.getName().equals(serverName))
                    return next;
            }

            return null;
        }
    }

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
}
