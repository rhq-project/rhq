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
import java.util.List;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.composite.FailoverListComposite.ServerEntry;
import org.rhq.core.domain.resource.Agent;

public class ServerBucket {
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
        if ((null != preferredServerName) && (null == ServerBucket.getBucketByName(usedBuckets, preferredServerName))) {
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