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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;

/**
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
@Entity
@Table(name = "RHQ_RESOURCE")
public class PlatformSyncInfo extends SyncInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "parentResource", fetch = FetchType.EAGER)
    private Set<Resource> topLevelServers;

    // JPA requires public or protected no-param constructor; Externalizable requires public no-param constructor.
    public PlatformSyncInfo() {
    }

    public Collection<Resource> getTopLevelServers() {
        return topLevelServers;
    }

    // for testing
    public static PlatformSyncInfo buildPlatformSyncInfo(Resource platform) {
        Set<Resource> toplevelServers = platform.getChildResources();

        PlatformSyncInfo syncInfo = new PlatformSyncInfo(platform.getId(), platform.getUuid(), platform.getMtime(),
            platform.getInventoryStatus(), (null == toplevelServers ? Collections.EMPTY_SET : toplevelServers));

        return syncInfo;
    }

    // for testing
    private PlatformSyncInfo(int id, String uuid, long mtime, InventoryStatus istatus, Set<Resource> children) {
        super(id, uuid, mtime, istatus);
        this.topLevelServers = children;
    }

}
