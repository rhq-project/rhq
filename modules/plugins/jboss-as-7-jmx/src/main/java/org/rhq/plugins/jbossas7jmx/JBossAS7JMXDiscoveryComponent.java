/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.jbossas7jmx;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Just returns the singleton jmx mbeans parent resource.x
 * 
 * @author Jay Shaughnessy
 */
public class JBossAS7JMXDiscoveryComponent<T extends ResourceComponent<?>> implements ResourceDiscoveryComponent<T> {

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context) {

        HashSet<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();

        String key = "JBossAS7JMX";
        String name = "jmx mbeans";
        String version = this.getClass().getPackage().getImplementationVersion();
        String description = "JMX MBeans Deployed to AS 7.x";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        result.add(resource);

        return result;
    }
}