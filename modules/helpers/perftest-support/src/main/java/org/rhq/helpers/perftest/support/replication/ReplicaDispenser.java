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

import java.util.HashMap;
import java.util.List;

import org.rhq.helpers.perftest.support.testng.DatabaseSetupInterceptor;

/**
 * This dispenses the replicas to be used by the tests based on the replication strategy set up for the test.
 * This class is used as a mediator between the {@link DatabaseSetupInterceptor} that processes the test annotations
 * and {@link ReplicaProvider} that provides the interface to be used by the tests themselves.
 * 
 * @author Lukas Krejci
 */
public class ReplicaDispenser {

    private HashMap<Thread, Integer> perThreadIndices;
    private int nextThreadIndex = 0;
    private List<ReplicationResult> results;
    private ThreadLocal<Integer> currentInvocation;

    public ReplicaDispenser(List<ReplicationResult> replicationResults, ReplicaCreationStrategy strategy) {
        if (strategy == ReplicaCreationStrategy.PER_THREAD) {
            perThreadIndices = new HashMap<Thread, Integer>();
        }

        results = replicationResults;
    }

    public void setCurrentTestInvocationNumber(int testInvocationNumber) {
        currentInvocation.set(testInvocationNumber);
    }

    /**
     * Retrieves the replicas set up for this test.
     */
    public ReplicationResult getResult() {
        if (perThreadIndices == null) {
            //we're using the per invocation strategy
            return results.get(currentInvocation.get());
        } else {
            synchronized (this) {
                Thread thread = Thread.currentThread();
                Integer idx = perThreadIndices.get(thread);
                if (idx == null) {
                    idx = nextThreadIndex++;
                    perThreadIndices.put(thread, idx);
                }

                return results.get(idx);
            }
        }
    }

}
