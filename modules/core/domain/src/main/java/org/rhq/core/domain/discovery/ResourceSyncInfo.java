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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.rhq.core.domain.resource.InventoryStatus;

/**
 * @author Ian Springer
 */
@Entity
@Table(name = "RHQ_RESOURCE")
public class ResourceSyncInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Server-assigned id
     */
    @Column(name = "ID", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * Agent-assigned uuid
     */
    @Column(name = "UUID")
    private String uuid;

    /**
     * Last modified time
     */
    @Column(name = "MTIME")
    private long mtime;

    @Column(name = "INVENTORY_STATUS")
    @Enumerated(EnumType.STRING)
    private InventoryStatus inventoryStatus;

    @JoinColumn(name = "PARENT_RESOURCE_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ResourceSyncInfo parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    private Collection<ResourceSyncInfo> childSyncInfos;

    // JPA requires public or protected no-param constructor; Externalizable requires public no-param constructor.
    public ResourceSyncInfo() {
    }

    public int getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getMtime() {
        return mtime;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public Collection<ResourceSyncInfo> getChildSyncInfos() {
        return childSyncInfos;
    }

}
