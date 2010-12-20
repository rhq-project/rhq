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

import org.testng.annotations.Test;

/**
 * How the replicas should be created so that the tests can consume them.
 *
 * @author Lukas Krejci
 */
public enum ReplicaCreationStrategy {

    /**
     * The number of replicas corresponds to the {@link Test#threadPoolSize()} and
     * each thread of execution is provided with its own replica. This means that if
     * the invocationCount of the test is 4 and threadPoolSize is 2, the test will
     * be invoked 4 times, twice with each replica.
     */
    PER_THREAD,
    
    /**
     * The number of replicas corresponds to the {@link Test#invocationCount()} and each
     * test invocation is handed its own replica regardless of the thread it is invoked in.
     */
    PER_INVOCATION
}
