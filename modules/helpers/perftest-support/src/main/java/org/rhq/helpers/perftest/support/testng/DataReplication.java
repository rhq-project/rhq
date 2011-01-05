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

package org.rhq.helpers.perftest.support.testng;

import org.rhq.helpers.perftest.support.replication.ReplicaCreationStrategy;

/**
 * Specifies how to replicate the test data.
 * This annotation is only processed as a part of the {@link DatabaseState}
 * 
 * @author Lukas Krejci
 */
public @interface DataReplication {

    /**
     * The path to the replication configuration. 
     */
    String url();

    /**
     * Where does the {@link #url()} point to. 
     */
    FileStorage storage() default FileStorage.CLASSLOADER;
    
    /**
     * How many replicas should be prepared and how they should be distributed
     * among the test invocations. The default is a replica per invocation.
     */
    ReplicaCreationStrategy replicaCreationStrategy() default ReplicaCreationStrategy.PER_INVOCATION;
        
    /**
     * This callback can be used to modify the replica before it is persisted to the database.
     * This can be used to modify the "names", "descriptions" and other data that is not significant
     * to the referential integrity but that can help identifying the entities.
     * <p>
     * The method must have the following signature:<br/>
     * <code>
     * void &lt;method-name&gt;(int replicaNumber, Object original, Object replica, Class&lt;?&gt; entityType)
     * </code> 
     */
    String replicaModifier() default "";
}
