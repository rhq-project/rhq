/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * boo!
 *
 * @author John Sanda
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD })
public @interface DeployCluster {

    /**
     * @return The number of nodes in the cluster. Defaults to two.
     */
    int numNodes() default 2;

    /**
     * @return A flag that specifies whether or not to wait for all cluster nodes to start.
     * The approach that is currently used to determine whether or a node is started is to
     * open a Thrift connection to that node. This attribute defaults to true.
     */
    boolean waitForClusterToStart() default true;

    /**
     * @return A flag that specifies whether or not to wait for schema agreement across the
     * cluster. Defaults to true.
     */
    boolean waitForSchemaAgreement() default true;

}
