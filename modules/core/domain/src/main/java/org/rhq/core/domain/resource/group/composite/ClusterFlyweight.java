/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.resource.group.composite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a cluster node in a group tree. A cluster node represents an aggregation
 * of one or more individual resources that are identical, where "identical" means
 * the same resource type and resource key (see {@link ClusterKeyFlyweight}.
 * The number of individual resources that make up this cluster node is the
 * {@link #getMembers() member count}. The total size of the cluster (that is,
 * the maximum number of members that can be expected) is {@link #getClusterSize()}.
 * 
 * @author Greg Hinkle
 */
public class ClusterFlyweight implements Serializable {

    private static final long serialVersionUID = 1L;

    private int groupId;
    private ClusterKeyFlyweight clusterKey;
    private String name;
    private List<ClusterFlyweight> children = new ArrayList<ClusterFlyweight>(0);
    private int members = 0;
    private int clusterSize = 0;

    public ClusterFlyweight() {
    }

    public ClusterFlyweight(int groupId) {
        this.groupId = groupId;
    }

    public ClusterFlyweight(ClusterKeyFlyweight clusterKey) {
        this.clusterKey = clusterKey;
    }

    public int getGroupId() {
        return groupId;
    }

    public ClusterKeyFlyweight getClusterKey() {
        return clusterKey;
    }

    public String getName() {
        return (name != null) ? name : "?";
    }

    public void addResource(String s) {
        if (name == null) {
            name = s;
        } else if (!name.equals(s)) {
            name = "..."; // more than one "identical" resource had different names - don't know what to call this node
        }
    }

    public int getMembers() {
        return members;
    }

    public void setMembers(int members) {
        this.members = members;
    }

    public void incrementMembers() {
        this.members++;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(int size) {
        this.clusterSize = size;
    }

    public List<ClusterFlyweight> getChildren() {
        return children;
    }

    public void setChildren(List<ClusterFlyweight> clusterKeyFlyweights) {
        if (clusterKeyFlyweights == null) {
            this.children = new ArrayList<ClusterFlyweight>(0);
        } else {
            this.children = clusterKeyFlyweights;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ClusterFlyweight [name=").append(name).append(", clusterKey=").append(clusterKey).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + groupId;
        result = 31 * result + ((clusterKey == null) ? 0 : clusterKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClusterFlyweight)) {
            return false;
        }

        ClusterFlyweight other = (ClusterFlyweight) obj;

        if (groupId != other.groupId) {
            return false;
        }

        if (clusterKey == null) {
            if (other.clusterKey != null) {
                return false;
            }
        } else if (!clusterKey.equals(other.clusterKey)) {
            return false;
        }
        return true;
    }

}
