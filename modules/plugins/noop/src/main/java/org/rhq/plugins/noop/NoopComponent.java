/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.noop;

import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Class that servers as discovery and component class
 * The discovery will on purpose never discover a resource
 * @author Heiko W. Rupp
 */
public class NoopComponent implements ResourceComponent, ResourceDiscoveryComponent {
    @Override
    public void start(ResourceContext context) throws Exception {
        // nothing to do
    }

    @Override
    public void stop() {
        // nothing to do
    }

    @Override
    public AvailabilityType getAvailability() {
        return null;
    }

    /**
     * Discovery method, that on purpose always returns an empty set of discovered resources.
     * This plugin is meant as base for plugins that
     * @param  context the discovery context that provides the information to the component that helps it perform its
     *                 discovery
     *
     * @return An empty set
     * @throws Exception in the case something is sooo wrong ..
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext context) throws  Exception {
        return Collections.emptySet();
    }
}
