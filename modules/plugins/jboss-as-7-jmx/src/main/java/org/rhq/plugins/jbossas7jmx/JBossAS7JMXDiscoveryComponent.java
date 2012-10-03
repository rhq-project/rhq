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

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Just returns the singleton jmx mbeans parent resource.x
 * 
 * @author Jay Shaughnessy
 */
public class JBossAS7JMXDiscoveryComponent<T extends ResourceComponent<?>> implements ResourceDiscoveryComponent<T>,
    ClassLoaderFacet<ResourceComponent<?>> {

    @Override
    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext<ResourceComponent<?>> context,
        DiscoveredResourceDetails details) throws Exception {
        // TODO we need to get the AS7's install directory so we can find the client jars
        //      the parent component is the JBossAS7JMX server resource, see if it can somehow ask its parent
        //      for this information which can then be passed down to us. Not sure how to do this - via plugin config perhaps?
        return null;
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context) {

        HashSet<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();

        String key = "JBossAS7JMX";
        String name = "jmx mbeans";
        String version = "7"; // this should probably be the actual version of the remote AS7 server being monitored
        String description = "JMX MBeans Deployed to AS 7.x";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        result.add(resource);

        return result;
    }
}