/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.plugins.platform;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class DirectoryDiscoveryComponent implements ManualAddFacet<DirectoryComponent>, ResourceDiscoveryComponent<DirectoryComponent> {


    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
                                                      ResourceDiscoveryContext<DirectoryComponent> context) throws InvalidPluginConfigurationException {

        String path = pluginConfiguration.getSimpleValue("path");
        if (path==null || path.isEmpty()) {
            throw new InvalidPluginConfigurationException("Path must not be empty");
        }
        if (path.equals("/")) {
            throw new InvalidPluginConfigurationException("/ is forbidden");
        }

        DiscoveredResourceDetails result = new DiscoveredResourceDetails(
            context.getResourceType(),
            path,
            path,
            null,
            "A directory",
            pluginConfiguration,
            null);
        return result;
    }

    /**
     * We do not support auto-discovery of directories
     * @param  context the discovery context that provides the information to the component that helps it perform its
     *                 discovery
     *
     * @return
     * @throws InvalidPluginConfigurationException
     * @throws Exception
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<DirectoryComponent> context) throws InvalidPluginConfigurationException, Exception {
        return Collections.emptySet();
    }
}
