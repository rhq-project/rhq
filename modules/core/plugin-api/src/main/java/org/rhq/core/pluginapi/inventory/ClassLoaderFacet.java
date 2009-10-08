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
package org.rhq.core.pluginapi.inventory;

import java.net.URL;
import java.util.List;

/**
 * This allows a discovery component to provide additional jars to placement in a plugin classloader. This is used
 * to support those resource types that require different classloaders per connection to different managed resource
 * instances.
 * 
 * <i>Note:</i> this facet is to be implemented by {@link ResourceDiscoveryComponent} instances, unlike the other
 * facets that are implemented by the resource instance components.
 * 
 * @param <T> the parent resource component type for those resources discovered by this discovery component
 *
 * @author John Mazzitelli
 */
public interface ClassLoaderFacet<T extends ResourceComponent> {
    /**
     * This method provides the location for additional jars that are needed in the resource's classloader
     * in order to properly connect and talk to the managed resource.
     * 
     * @param context information for the component that helps it perform its discovery
     * @param details provides information on the managed resource instance that is to be connected to
     * 
     * @return list of URLs to jar files needed in order to successfully connect to the managed
     *         resource instance described by <code>details</code>. The returned value can be <code>null</code>,
     *         empty or contain any number of URLs.
     *
     * @throws Exception if the plugin needs additional jars to manage the resource but those jars cannot be found. 
     */
    List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext<T> context, DiscoveredResourceDetails details)
        throws Exception;
}
