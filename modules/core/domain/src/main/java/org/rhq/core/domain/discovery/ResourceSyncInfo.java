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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;

/**
 * Sync info for a resource.  This is a lightweight "Resource" entity that contains only the information required
 * to perform Inventory Sync between the Agent and Server.
 * <p/>
 * Because this entity is meant for syncing between the agent and server, only "real" non-synthetic resources are
 * considered in the named queries and care should be taken that a synthetic resource never travels across the wire to
 * the agent.
 *
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries({
    @NamedQuery(name = ResourceSyncInfo.QUERY_SERVICE_CHILDREN, query = "" //
        + "SELECT r " //
        + "  FROM ResourceSyncInfo r " //
        + " WHERE r.id IN ( SELECT rr.id FROM Resource rr WHERE rr.parentResource.id IN ( :parentIds )" //
        + " AND rr.synthetic = false )" //
        + ""),
    @NamedQuery(name = ResourceSyncInfo.QUERY_TOP_LEVEL_SERVER, query = "" //
        + "SELECT rsi " //
        + "  FROM ResourceSyncInfo rsi " //
        + " WHERE rsi.id = :resourceId " //
        + "    OR rsi.id IN (SELECT rr.id FROM Resource rr WHERE rr.parentResource.id = :resourceId AND rr.synthetic = false) "
        + "    OR rsi.id IN (SELECT rr.id FROM Resource rr WHERE rr.parentResource.parentResource.id = :resourceId AND rr.synthetic = false) "
        + "    OR rsi.id IN (SELECT rr.id FROM Resource rr WHERE rr.parentResource.parentResource.parentResource.id = :resourceId AND rr.synthetic = false) "
        + "    OR rsi.id IN (SELECT rr.id FROM Resource rr WHERE rr.parentResource.parentResource.parentResource.parentResource.id = :resourceId AND rr.synthetic = false) "
        + "    OR rsi.id IN (SELECT rr.id FROM Resource rr WHERE rr.parentResource.parentResource.parentResource.parentResource.parentResource.id = :resourceId AND rr.synthetic = false) "
        + "   ") })
@Table(name = "RHQ_RESOURCE")
public class ResourceSyncInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Sync info for platform service children (for building up hierarchy that excludes the top level servers */
    public static final String QUERY_SERVICE_CHILDREN = "ResourceSyncInfo.platformServiceChildren";
    /** Sync info rooted at the specified top level server and including all of it's hierarchy (up to 5 levels below
     * the top level server. note that we support up to 6 levels below platform but we are starting one level down) */
    public static final String QUERY_TOP_LEVEL_SERVER = "ResourceSyncInfo.topLevelServer";

    // Native Queries not supported by HQL
    public static final String QUERY_NATIVE_QUERY_TOP_LEVEL_SERVER_ORACLE = "" //
        + "           SELECT r.id, r.uuid, r.mtime, r.inventory_status " //
        + "             FROM rhq_resource r " //
        + "             WHERE r.synthetic = 0 " //
        + "       START WITH r.id = :resourceId " //
        + " CONNECT BY PRIOR r.id = r.parent_resource_id AND r.synthetic = 0";
    public static final String QUERY_NATIVE_QUERY_TOP_LEVEL_SERVER_POSTGRES = "" //
        + " WITH RECURSIVE childResource AS " //
        + " (   SELECT r.id, r.uuid, r.mtime, r.inventory_status " //
        + "       FROM rhq_resource AS r " //
        + "      WHERE r.id = :resourceId " // non-recursive term
        + "       AND r.synthetic = false " //
        + "  UNION ALL " //
        + "     SELECT r.id, r.uuid, r.mtime, r.inventory_status " // recursive term
        + "       FROM rhq_resource AS r " //
        + "       JOIN childResource AS cr " //
        + "         ON (r.parent_resource_id = cr.id AND r.synthetic = false) " //
        + " ) " //
        + " SELECT id, uuid, mtime, inventory_status " //
        + "   FROM childResource ";

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceSyncInfo other = (ResourceSyncInfo) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    public ResourceSyncInfo(int id, String uuid, long mtime, InventoryStatus istatus) {
        this.id = id;
        this.uuid = uuid;
        this.mtime = mtime;
        this.inventoryStatus = istatus;
    }

    static public ResourceSyncInfo buildResourceSyncInfo(Resource res) {
        return new ResourceSyncInfo(res.getId(), res.getUuid(), res.getMtime(), res.getInventoryStatus());
    }
}
