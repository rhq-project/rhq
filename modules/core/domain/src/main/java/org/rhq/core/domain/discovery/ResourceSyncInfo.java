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

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.Externalizable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import org.rhq.core.domain.resource.InventoryStatus;

/**
 * @author Ian Springer
 */
@Entity
@Table(name = "RHQ_RESOURCE")
public class ResourceSyncInfo implements Externalizable
{
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
    public ResourceSyncInfo()
    {
    }

    public int getId()
    {
        return id;
    }

    public String getUuid()
    {
        return uuid;
    }

    public long getMtime()
    {
        return mtime;
    }

    public InventoryStatus getInventoryStatus()
    {
        return inventoryStatus;
    }

    public Collection<ResourceSyncInfo> getChildSyncInfos()
    {
        return childSyncInfos;
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(id);
        out.writeUTF(uuid);
        out.writeLong(mtime);
        out.writeInt(inventoryStatus.ordinal());
        if (childSyncInfos.getClass().getName().contains("hibernate"))
        {
            out.writeObject(new LinkedHashSet<ResourceSyncInfo>(childSyncInfos));
        }
        else
        {
            out.writeObject(childSyncInfos);
        }
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        id = in.readInt();
        uuid = in.readUTF();
        mtime = in.readLong();
        inventoryStatus = InventoryStatus.values()[in.readInt()];
        childSyncInfos = (Set<ResourceSyncInfo>)in.readObject();
    }    
}
