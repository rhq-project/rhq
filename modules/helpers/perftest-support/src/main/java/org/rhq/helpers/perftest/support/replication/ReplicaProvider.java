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
 * This class stores the current replicas for each test.
 * To use it, just call the {@link #get()} method inside your test.
 * 
 * @author Lukas Krejci
 */
public class ReplicaProvider {

    private static final ThreadLocal<ReplicaDispenser> DISPENSER = new ThreadLocal<ReplicaDispenser>();
    
    /**
     * This method is used during setup of the test execution. Don't call it from within the test.
     * 
     * @param dispenser
     */
    public static void setDispenser(ReplicaDispenser dispenser) {
        DISPENSER.set(dispenser);
    }
    
    /**
     * @return the replication results to be used by the current test.
     */
    public static ReplicationResult get() {
        return DISPENSER.get().getResult();
    }
}
