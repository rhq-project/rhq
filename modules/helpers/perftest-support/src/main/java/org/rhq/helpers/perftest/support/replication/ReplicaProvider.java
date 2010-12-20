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

import java.util.List;
import java.util.Map;

/**
 * This class stores the current replicas for each test.
 * To use it, just call the {@link #get()} method inside your test.
 * 
 * @author Lukas Krejci
 */
public class ReplicaProvider {

    private static ThreadLocal<Map<Class<?>, List<Object>>> REPLICA = new ThreadLocal<Map<Class<?>,List<Object>>>();
    
    private ReplicaProvider() {
        
    }
    
    /**
     * Retrieves the replicas set up for this test.
     * 
     * @return a map where keys are entity types and values are lists of IDs of the replicas.
     */
    public static Map<Class<?>, List<Object>> get() {
        return REPLICA.get();
    }
    
    public static void set(Map<Class<?>, List<Object>> replica) {
        REPLICA.set(replica);
    }
}
