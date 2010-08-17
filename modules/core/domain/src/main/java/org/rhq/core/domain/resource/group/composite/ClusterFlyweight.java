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
import java.util.List;

import org.rhq.core.domain.resource.group.ClusterKey;

/**
 * @author Greg Hinkle
 */
public class ClusterFlyweight implements Serializable {

    private int groupId;

    private ClusterKeyFlyweight clusterKey;

    private String name;

    private List<ClusterFlyweight> children;

    private int members;

    public ClusterFlyweight() {
    }

    public ClusterFlyweight(int groupId) {
        this.groupId = groupId;
    }

    public ClusterFlyweight(ClusterKeyFlyweight clusterKey) {
        this.clusterKey = clusterKey;
    }

    public void addResource(String s) {
        if (name == null) {
            name = s;
        } else if (!name.equals(s)) {
            name = "?";
        }
    }

    public int getMembers() {
        return members;
    }

    public void setMembers(int members) {
        this.members = members;
    }

    public void setChildren(List<ClusterFlyweight> clusterKeyFlyweights) {
        this.children = clusterKeyFlyweights;
    }

    public int getGroupId() {
        return groupId;
    }

    public ClusterKeyFlyweight getClusterKey() {
        return clusterKey;
    }

    public String getName() {
        return name;
    }

    public List<ClusterFlyweight> getChildren() {
        return children;
    }








}
