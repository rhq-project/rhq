/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.domain.discovery;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;

/**
 * This immutable POJO returns the information necessary for the agent to perform a complete sync with the server
 * inventory.  It does not provide *all* of the sync info, only the platform and its top level *service* hierarchy. It
 * expects the agent to call back to the server for each of the platform's top level servers and therefore provides
 * only the top level server Ids.
 *
 * @author Jay Shaughnessy
 */
public class PlatformSyncInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private ResourceSyncInfo platform;
    private Set<ResourceSyncInfo> services;
    private Set<Integer> topLevelServerIds;

    public PlatformSyncInfo(ResourceSyncInfo platform, Set<ResourceSyncInfo> services, Set<Integer> topLevelServerIds) {
        super();
        this.platform = platform;
        this.services = services;
        this.topLevelServerIds = topLevelServerIds;
    }

    /**
     * @return just the platform sync info
     */
    public ResourceSyncInfo getPlatform() {
        return platform;
    }

    /**
     * @return the sync info for the platform hierarchy excluding the platform itself and the top level servers
     */
    public Set<ResourceSyncInfo> getServices() {
        return services;
    }

    /**
     * @return just the type level server ids, so that the agent can call back for sync info on each top level server
     */
    public Set<Integer> getTopLevelServerIds() {
        return topLevelServerIds;
    }

    // for testing
    public static PlatformSyncInfo buildPlatformSyncInfo(Resource platform) {
        Set<Integer> toplevelServerIds = new HashSet<Integer>();
        for (Resource r : platform.getChildResources()) {
            if (r.getResourceType().getCategory().equals(ResourceCategory.SERVER) && !r.isSynthetic()) {
                toplevelServerIds.add(r.getId());
            }
        }

        ResourceSyncInfo resSyncInfo = ResourceSyncInfo.buildResourceSyncInfo(platform);

        PlatformSyncInfo syncInfo = new PlatformSyncInfo(resSyncInfo, new HashSet<ResourceSyncInfo>(1),
            toplevelServerIds);

        return syncInfo;
    }
}
