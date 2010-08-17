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
package org.rhq.enterprise.server.resource.cluster;

import java.util.List;

import javax.ejb.Remote;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;

/**
 * @author Greg Hinkle
 */
@Remote
public interface ClusterManagerRemote {

    /**
     * Given a cluster key create a backing group.
     * @param clusterKey
     * @param addResources If true, the new group will be assigned the current resources defined by the clusterKey.
     * Otherwise no resources will be assigned to the new group.
     * @throws IllegalArgumentException if a backing group exists for this clusterKey
     */
    ResourceGroup createAutoClusterBackingGroup(Subject subject, ClusterKey clusterKey, boolean addResources);

    /**
     * Return the backing group for the supplied cluster key.  Resource membership will represent the resources
     * last set for the group and may not reflect current membership. See {@link #getAutoClusterResources(String)}
     * @param clusterKey
     * @return The backing group, or null if the key does not have a backing group.
     */
    @Nullable
    ResourceGroup getAutoClusterBackingGroup(Subject subject, ClusterKey clusterKey);

    /**
     * Given a cluster key get the auto cluster resource membership. The membership is always determined
     * at call time, regardless of whether a backing group exists. To get the backing group, if it exists,
     * for a cluster key then call {@link #getAutoClusterBackingGroup(String)}.
     */
    List<Resource> getAutoClusterResources(Subject subject, ClusterKey clusterKey);


    /**
     * Load a fully populated tree view of the "cluster nodes" for a cluster group. These are
     * summary nodes and do not contain the actual resources in the group.
     *
     * @param subject
     * @param groupId
     * @return
     */
    ClusterFlyweight getClusterTree(Subject subject, int groupId);

}
