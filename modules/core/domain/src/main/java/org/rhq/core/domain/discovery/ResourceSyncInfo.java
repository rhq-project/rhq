/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;

/**
 * Sync info for any non-platform resource.
 *
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
@Entity
@Table(name = "RHQ_RESOURCE")
public class ResourceSyncInfo extends SyncInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "PARENT_RESOURCE_ID")
    private Integer parentId;

    @OneToMany(mappedBy = "parentId", fetch = FetchType.EAGER)
    private Collection<ResourceSyncInfo> childSyncInfos;

    // JPA requires public or protected no-param constructor; Externalizable requires public no-param constructor.
    public ResourceSyncInfo() {
    }

    public Collection<ResourceSyncInfo> getChildSyncInfos() {
        return childSyncInfos;
    }

    // for testing
    public static ResourceSyncInfo buildResourceSyncInfo(Resource resource) {
        Collection<ResourceSyncInfo> children;

        if (resource.getChildResources() != null) {
            children = new HashSet<ResourceSyncInfo>(resource.getChildResources().size());
            for (Resource child : resource.getChildResources()) {
                children.add(buildResourceSyncInfo(child));
            }
        } else {
            children = new HashSet<ResourceSyncInfo>(0);
        }

        return buildResourceSyncInfo(resource, children);
    }

    // for testing
    public static ResourceSyncInfo buildResourceSyncInfo(Resource resource, Collection<ResourceSyncInfo> children) {

        ResourceSyncInfo syncInfo = new ResourceSyncInfo(resource.getId(), resource.getUuid(), resource.getMtime(),
            resource.getInventoryStatus(), children);

        return syncInfo;
    }


    public static ResourceSyncInfo buildResourceSyncInfo(SyncInfo syncInfo) {

        return buildResourceSyncInfo(syncInfo, ((Collection<ResourceSyncInfo>) null));
    }

    public static ResourceSyncInfo buildResourceSyncInfo(SyncInfo syncInfo, Collection<ResourceSyncInfo> children) {

        ResourceSyncInfo resourceSyncInfo = new ResourceSyncInfo(syncInfo.getId(), syncInfo.getUuid(),
            syncInfo.getMtime(), syncInfo.getInventoryStatus(), children);

        return resourceSyncInfo;
    }

    private ResourceSyncInfo(int id, String uuid, long mtime, InventoryStatus istatus,
        Collection<ResourceSyncInfo> children) {
        super(id, uuid, mtime, istatus);
        this.childSyncInfos = children;
    }
}
