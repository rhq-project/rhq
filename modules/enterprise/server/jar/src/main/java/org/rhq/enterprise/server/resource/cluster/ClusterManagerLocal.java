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
package org.rhq.enterprise.server.resource.cluster;

import java.util.List;

import javax.ejb.Local;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

/*
 * @author Jay Shaughnessy
 */
@Local
public interface ClusterManagerLocal {

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

}
