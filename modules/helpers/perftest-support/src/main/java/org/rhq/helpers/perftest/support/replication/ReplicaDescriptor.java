/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.replication;

/**
 * Describes a replica.
 *
 * @author Lukas Krejci
 */
public class ReplicaDescriptor {

    private Class<?> entity;
    private Object originalId;
    private Object id;
    
    /**
     * @param entity
     * @param originalId
     * @param id
     */
    public ReplicaDescriptor(Class<?> entity, Object originalId, Object id) {
        super();
        this.entity = entity;
        this.originalId = originalId;
        this.id = id;
    }
    
    /**
     * @return the entity
     */
    public Class<?> getEntity() {
        return entity;
    }
    /**
     * @return the originalId
     */
    public Object getOriginalId() {
        return originalId;
    }
    /**
     * @return the id
     */
    public Object getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        
        if (!(other instanceof ReplicaDescriptor)) {
            return false;
        }
        
        ReplicaDescriptor o = (ReplicaDescriptor) other;
        
        return entity.equals(o.entity) && originalId.equals(o.originalId) && id.equals(o.id);
    }
    
    @Override
    public int hashCode() {
        int ret = entity.hashCode();
        ret = 31 * ret + originalId.hashCode();
        ret = 31 * ret + id.hashCode();
        
        return ret;
    }
}
