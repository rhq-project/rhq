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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;
import org.rhq.enterprise.gui.coregui.client.gwt.ClusterGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.resource.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ClusterGWTServiceImpl extends AbstractGWTServiceImpl implements ClusterGWTService {


    private ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();


    public ResourceGroup createAutoClusterBackingGroup(ClusterKey clusterKey, boolean addResources) {
        return SerialUtility.prepare(
                clusterManager.createAutoClusterBackingGroup(getSessionSubject(), clusterKey, addResources),
                "ClusterGWTServiceImpl.createAutoClusterBackingGroup");
    }

    public ResourceGroup getAutoClusterBackingGroup(ClusterKey clusterKey) {
        return SerialUtility.prepare(
                clusterManager.getAutoClusterBackingGroup(getSessionSubject(), clusterKey),
                "ClusterGWTServiceImpl.getAutoClusterBackingGroup");
    }

    public List<Resource> getAutoClusterResources(ClusterKey clusterKey) {
        return SerialUtility.prepare(
                clusterManager.getAutoClusterResources(getSessionSubject(), clusterKey),
                "ClusterGWTServiceImpl.getAutoClusterResources");
    }

    public ClusterFlyweight getClusterTree(int groupId) {
        return SerialUtility.prepare(
                clusterManager.getClusterTree(getSessionSubject(), groupId),
                "ClusterGWTServiceImpl.getClusterTree");

    }
}
