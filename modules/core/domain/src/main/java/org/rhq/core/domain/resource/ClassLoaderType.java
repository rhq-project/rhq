/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.resource;

/**
 * Resource types define special classloading rules needed when their corresponding
 * resource components are running in the plugin container. This enum defines
 * the different types of classloading rules supported.
 * 
 * John Mazzitelli
 */
public enum ClassLoaderType {
    /**
     * This is the most common classloader type - it says that the resource type's classloader
     * can be shared with its parent. That is to say, the resource's parent resource will supply
     * the classloader.
     */
    SHARED,

    /**
     * When each resource instance of a resource type needs its own separate classloader,
     * it will specify this classloader type. This is needed if a managed resource potentially
     * needs different jars/libraries compared to another managed resource of the same type
     * (e.g. different client jars for different versions of the same type of resource).
     */
    INSTANCE
}
