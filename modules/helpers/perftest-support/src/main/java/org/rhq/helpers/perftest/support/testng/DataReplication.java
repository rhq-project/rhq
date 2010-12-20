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

    //TODO this is most likely insufficient. This is going to require similar configuration
    //as Exporter so that we know what relationships to replicate and how.
    /**
     * The JPA entities to replicate (along with their dependent entities) 
     */
    Class<?>[] rootEntities();
    
    //TODO this is going to disappear if we go for the replication configuration file
    /**
     * If defined, the method specified by this attribute can restrict the 
     * entities to be replicated. It is provided by the class of the entities
     * to be fetched and can return a WHERE SQL fragment to restrict what entities
     * are going to be fetched (note that the fragment is SQL, *NOT* JPQL).
     * <p>
     * The method has to have the following signature:<br/>
     * <code>
     * String &lt;method-name&gt;(int replicaNumber, Class<?> entityClass)
     * </code>
     */
    String replicaRestrictor() default "";
    
    /**
     * How many replicas should be prepared and how they should be distributed
     * among the test invocations. The default is a replica per invocation.
     */
    ReplicaCreationStrategy replicaCreationStrategy() default ReplicaCreationStrategy.PER_INVOCATION;
    
    /**
     * The name of a method that is able to provide the next id to use for an entity
     * when creating the replicas. If empty (the default), it is assumed that JPA
     * is setup to generate the ids on its own.
     * <p>
     * The method must have the following signature:<br/>
     * <code>
     * Object &lt;method-name&gt;(Connection jdbcConnection, Class<?> entityClass)
     * </code> 
     */
    String nextIdProvider() default "";
    
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
