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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ReplicationResult extends HashMap<Class<?>, Set<ReplicaDescriptor>>{

    private static final long serialVersionUID = 1L;

    public ReplicationResult() {
        super();
    }

    public ReplicationResult(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ReplicationResult(int initialCapacity) {
        super(initialCapacity);
    }

    public ReplicationResult(Map<Class<?>, ? extends Set<ReplicaDescriptor>> m) {
        super(m);
    }


    public void put(ReplicaDescriptor replica) {
        Set<ReplicaDescriptor> list = get(replica.getEntity());
        if (list == null) {
            list = new HashSet<ReplicaDescriptor>();
            put(replica.getEntity(), list);
        }
        
        list.add(replica);
    }
    
    public void putAll(Collection<ReplicaDescriptor> replicas) {
        for(ReplicaDescriptor r : replicas) {
            put(r);
        }
    }
}
