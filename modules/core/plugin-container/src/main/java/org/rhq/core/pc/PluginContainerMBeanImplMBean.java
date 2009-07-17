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
package org.rhq.core.pc;

import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * The management interface for the {@link PluginContainer} itself.
 * 
 * @author John Mazzitelli
 */
public interface PluginContainerMBeanImplMBean {

    String OBJECT_NAME = "rhq.pc:type=PluginContainer";

    /**
     * Tells the PC to perform a manual discovery. This will execute an immediate server scan
     * followed, optionally, by a service scan.
     * 
     * @param detailedDiscovery if true, will perform a "service scan" immediately after the "server scan".
     * @return the results of the discovery
     */
    String executeDiscovery(Boolean detailedDiscovery);

    /**
     * Retrieves the plugin dependency information, show you the order in which the plugins are deployed
     * and the plugins they depend on.
     * 
     * @return plugin dependency graph in an operation result configuration object
     */
    OperationResult retrievePluginDependencyGraph();

    /**
     * Retrieves information on all created and assigned plugin classloaders. There is
     * one plugin classloader for each deployed plugin.
     * 
     * @return plugin classloader info
     */
    OperationResult retrievePluginClassLoaderInformation();

    /**
     * Retrieves information on all created discovery classloaders. These are created
     * for discovery components that need to discover resources under a parent resource where
     * that parent resource is from a different plugin than the discovery component.
     * 
     * @return discovery classloader info
     */
    OperationResult retrieveDiscoveryClassLoaderInformation();

    /**
     * Retrieves information on all created and assigned resource classloaders. Each resource
     * is assigned a classloader. Some resources share classloaders so there will be duplicate
     * classloaders in the returned result.
     * 
     * @return resource classloader info
     */
    OperationResult retrieveAllResourceClassLoaderInformation();

    /**
     * Retrieves information on all unique resource classloaders. Each resource
     * is assigned one of these classloaders. Some resources share classloaders, but no duplicate
     * classloaders will be in the returned result. The number of resources assigned to each
     * classloader is in the result map.
     * 
     * @return unique resource classloader info
     */
    OperationResult retrieveUniqueResourceClassLoaderInformation();

    /**
     * Returns the number of classloaders assigned to a plugin.
     * 
     * @return plugin classloader count
     */
    int getNumberOfPluginClassLoaders();

    /**
     * Returns the number of classloaders assigned to plugins' discovery components.
     * 
     * @return discovery classloader count
     */
    int getNumberOfDiscoveryClassLoaders();

    /**
     * Returns the number of classloaders assigned to individual resources.
     * 
     * @return resource classloader count
     */
    int getNumberOfResourceClassLoaders();
}
