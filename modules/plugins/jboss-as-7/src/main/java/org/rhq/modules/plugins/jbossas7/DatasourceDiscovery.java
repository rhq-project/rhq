/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Discover Datasources
 *
 * @author Heiko W. Rupp
 */
public class DatasourceDiscovery implements ResourceDiscoveryComponent<SubsystemComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<SubsystemComponent> discoveryContext) throws InvalidPluginConfigurationException, Exception {

/*
        if (subsys instanceof DataSourcesSubsystemElement) {
            DataSourcesSubsystemElement element = (DataSourcesSubsystemElement) subsys;
            DataSources sources = element.getDatasources();

            Set<DiscoveredResourceDetails> details  = new HashSet<DiscoveredResourceDetails>();

            for (DataSource source : sources.getDataSource()) {
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        discoveryContext.getResourceType(), // Type
                        source.getJndiName(), // Key
                        source.getJndiName(), // Name TODO improve
                        null, // version
                        "A datasource", // description
                        discoveryContext.getDefaultPluginConfiguration(),
                        null // Process scans

                );
                details.add(detail);
            }
            return details;
        }

*/
        return Collections.emptySet();
    }
}
