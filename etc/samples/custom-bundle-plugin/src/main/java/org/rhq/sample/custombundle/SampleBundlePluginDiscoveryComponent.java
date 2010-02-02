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
package org.rhq.sample.custombundle;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * This can be the start of your own custom bundle plugin's discovery component.
 * Usually, there isn't much you have to do here.
 *
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class SampleBundlePluginDiscoveryComponent implements ResourceDiscoveryComponent {
    private final Log log = LogFactory.getLog(SampleBundlePluginDiscoveryComponent.class);

    /**
     * Review the javadoc for both {@link ResourceDiscoveryComponent} and {@link ResourceDiscoveryContext} to learn what
     * you need to do in this method.
     *
     * @see ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {

        log.info("Discovering my custom bundle plugin's main resource");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        // key = this must be a unique string across all of your resources - see docs for uniqueness rules
        // name = this is the name you give the new resource; it does not necessarily have to be unique
        // version = this is any string that corresponds to the resource's version
        // description = this is any string that you want to assign as the default description for your resource
        String key = "My Bundle Handler Resource Key";
        String name = "My Bundle Handler Resource";
        String version = "1.0";
        String description = "This describes My Bundle Handler Resource";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        set.add(resource);

        return set;
    }
}