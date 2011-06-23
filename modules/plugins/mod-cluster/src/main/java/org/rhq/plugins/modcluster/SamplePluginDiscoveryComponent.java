/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.modcluster;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * This can be the start of your own custom plugin's discovery component. Review the javadoc for
 * {@link ResourceDiscoveryComponent}.
 *
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class SamplePluginDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {
    private final Log log = LogFactory.getLog(SamplePluginDiscoveryComponent.class);

    /**
     * This discovery method is the way the plugin supports "manual-add" capability. The plugin
     * descriptor must specify supportsManualAdd="true" to allow the resource to be manually added.
     * If that attribute is false, this method will never be used since it will not be possible to manually
     * add an instance of the resource.
     *
     * Review the javadoc for both {@link ManualAddFacet} and {@link ResourceDiscoveryContext} to learn what
     * you need to do in this method.
     *
     * @see ManualAddFacet#discoverResource(Configuration, ResourceDiscoveryContext)
     */
    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext context) throws InvalidPluginConfigurationException {

        // pluginConfiguration contains information on a resource that was manually added by the user.
        // take it and build a details object that represents that resource.

        // key = this must be a unique string across all of your resources - see docs for uniqueness rules
        // name = this is the name you give the new resource; it does not necessarily have to be unique
        // version = this is any string that corresponds to the resource's version
        // description = this is any string that you want to assign as the default description for your resource
        String key = "My Manually Added Resource Key";
        String name = "My Resource";
        String version = "1.0";
        String description = "This describes My Resource";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfiguration, null);

        return resource;
    }

    /**
     * Review the javadoc for both {@link ResourceDiscoveryComponent} and {@link ResourceDiscoveryContext} to learn what
     * you need to do in this method.
     *
     * @see ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext)
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        log.info("Discovering my custom plugin's resources");

        // if your plugin descriptor defined one or more <process-scan>s, then see if the plugin container
        // auto-discovered processes using those process scan definitions.  Process all those that were found.
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            // determine if you want to include the result in this method's returned set of discovered resources
        }

        // now perform your own discovery mechanism, if you have one.  For each resource discovered, you need to
        // create a details object that describe the resource that you discovered.
        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        // key = this must be a unique string across all of your resources - see docs for uniqueness rules
        // name = this is the name you give the new resource; it does not necessarily have to be unique
        // version = this is any string that corresponds to the resource's version
        // description = this is any string that you want to assign as the default description for your resource
        String key = "My Resource Key";
        String name = "My Resource";
        String version = "1.0";
        String description = "This describes My Resource";

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, null, null);

        set.add(resource);

        return set;
    }
}
