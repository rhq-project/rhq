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

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.rhq.core.domain.resource.InventoryStatus;

/**
 * This is the abstract base class for SyncInfo, which may be for a platform or a [top level server] resource.
 *
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
@MappedSuperclass
public abstract class SyncInfo implements Serializable {
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

    // JPA requires public or protected no-param constructor; Externalizable requires public no-param constructor.
    public SyncInfo() {
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

    protected SyncInfo(int id, String uuid, long mtime, InventoryStatus istatus) {
        this.id = id;
        this.uuid = uuid;
        this.mtime = mtime;
        this.inventoryStatus = istatus;
    }

}
