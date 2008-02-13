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
package org.rhq.core.pluginapi.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;

/**
 * The context object that {@link ResourceDiscoveryComponent} objects will have access to when needing to perform its
 * work. This context will have all the information that the discovery components will need to build new resources that
 * it discovers.
 *
 * <p>A plugin writer will use this context to build {@link DiscoveredResourceDetails details} of resources that were
 * either {@link #getAutoDiscoveredProcesses() auto-discovered} or are to be
 * {@link #getPluginConfigurations() manually discovered}.</p>
 *
 * <p>This context class is currently designed to be an immutable object. Instances of this context object are to be
 * created by the plugin container only.</p>
 *
 * @param  <T> the parent resource component type for those resources discovered by the discovery component that is
 *             assigned this context. In other words, for all the resources created by the discovery component, <code>
 *             T</code> is the resource component type of the parent of those new resources.
 *
 * @author John Mazzitelli
 */
public class ResourceDiscoveryContext<T extends ResourceComponent> {
    private final ResourceType resourceType;
    private final T parentComponent;
    private final SystemInfo systemInformation;
    private final List<ProcessScanResult> processScanResults;
    private final List<Configuration> pluginConfigurations;

    /**
     * Creates a new {@link ResourceDiscoveryContext} object. The plugin container is responsible for instantiating
     * these objects; plugin writers should never have to actually create context objects.
     *
     * <p>This creates a context object that contains information on both auto-discovered resources (i.e. those found
     * via process scans) and manually discovered resources (i.e. resources whose plugin configurations were directly
     * supplied by a user).</p>
     *
     * @param resourceType         the resource type of resources to be discovered which includes the default plugin
     *                             configuration
     * @param parentComponent      the parent component of the component that will be assigned this context
     * @param systemInfo           information about the system on which the plugin and its plugin container are running
     * @param processScanResults   processes that were auto-discovered by the plugin container on behalf of the plugin
     *                             via process scans (may be <code>null</code> or empty if nothing was auto-discovered)
     * @param pluginConfigurations for resources that are already known to exist (more specifically, resources that a
     *                             user told us exists), this contains plugin configurations that provide connection
     *                             information to those existing managed resources. (may be <code>null</code> or empty
     *                             if there are no other known resources)
     */
    @SuppressWarnings("unchecked")
    public ResourceDiscoveryContext(ResourceType resourceType, T parentComponent, SystemInfo systemInfo,
        List<ProcessScanResult> processScanResults, List<Configuration> pluginConfigurations) {
        this.resourceType = resourceType;
        this.parentComponent = parentComponent;
        this.systemInformation = systemInfo;
        this.processScanResults = (processScanResults != null) ? processScanResults : Collections.EMPTY_LIST;
        this.pluginConfigurations = (pluginConfigurations != null) ? pluginConfigurations : Collections.EMPTY_LIST;
    }

    /**
     * The {@link ResourceDiscoveryComponent} that is assigned this context will have the responsibility to discover
     * resources of the resource type returned by this method.
     *
     * @return type of resources to be discovered
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * The resource components for all newly discovered resources will become children of this parent resource
     * component.
     *
     * @return parent component of all new resources discovered
     *
     * @see    ResourceContext#getParentResourceComponent()
     */
    public T getParentResourceComponent() {
        return parentComponent;
    }

    /**
     * Returns a {@link SystemInfo} object that contains information about the platform/operating system that the plugin
     * is running on. With this object, you can natively obtain things such as the process table (containing
     * {@link ProcessInfo process information}) about all running processes), the operating system name, and other
     * things. Please refer to the javadoc on {@link SystemInfo} for more details on the types of information you can
     * access.
     *
     * @return system information object
     */
    public SystemInfo getSystemInformation() {
        return systemInformation;
    }

    /**
     * After having scanned all running processes, if the plugin container auto-discovered some resources for the
     * discovery component, those processes will be returned. The returned list of processes and their associated scans
     * should be used to build new discovered resources to be included in the discovery component's
     * {@link ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext) set of discovered resources} unless
     * for some reason the discovery component does not wish to include them.
     *
     * @return the processes the plugin container has auto-discovered on behalf of the discovery component
     */
    public List<ProcessScanResult> getAutoDiscoveredProcesses() {
        return new ArrayList<ProcessScanResult>(processScanResults);
    }

    /**
     * Returns plugin configurations for other known resources. This will contain one or more plugin configurations if,
     * for example, a user told us about resources that should exist and wants to manually discover them (whether or not
     * that resource can be auto-discovered or has a {@link #getAutoDiscoveredProcesses() process} associated with it).
     * This may be empty if there are no resources that were "manually discovered".
     *
     * @return list of plugin configurations, may be empty
     */
    public List<Configuration> getPluginConfigurations() {
        return new ArrayList<Configuration>(pluginConfigurations);
    }

    /**
     * Returns the default plugin configuration as defined in the {@link #getResourceType() resource type}'s default
     * template. This returns a new copy of the default configuration; it will not return the same configuration
     * instance to any future call to this method.
     *
     * @return a new copy of the default plugin configuration
     */
    public Configuration getDefaultPluginConfiguration() {
        ConfigurationDefinition definition = resourceType.getPluginConfigurationDefinition();
        if (definition != null) {
            ConfigurationTemplate template = definition.getDefaultTemplate();
            if (template != null) {
                return template.getConfiguration().deepCopy();
            }
        }

        return new Configuration(); // there is no default plugin config available, return an empty one
    }
}