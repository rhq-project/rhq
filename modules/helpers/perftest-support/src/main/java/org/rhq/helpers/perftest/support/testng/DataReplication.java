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

/**
 * Specifies how to replicate the test data.
 * This annotation is only processed as a part of the {@link DatabaseState}
 * 
 * @author Lukas Krejci
 */
public @interface DataReplication {

    /**
     * The JPA entities to replicate (along with their dependent entities) 
     */
    Class<?>[] rootEntities();
    
    /**
     * If true (the default) the replicator will prepare a copy of the root
     * entities for each thread the test is going to run in. 
     */
    boolean perThread() default true;
    
    /**
     * If {@link #perThread()} is false, this specifies the number of replicas
     * of the root entities to make. 
     */
    int replicationCount() default -1;
    
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
     * void &lt;method-name&gt;(Object original, Object replica)
     * </code> 
     */
    String replicaPreparer() default "";
}
