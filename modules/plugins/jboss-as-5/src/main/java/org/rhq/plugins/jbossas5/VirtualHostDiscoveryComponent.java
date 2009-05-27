/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.helper.MoreKnownComponentTypes;
import org.rhq.plugins.jbossas5.util.RegularExpressionNameMatcher;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;

/**
 * A component for discovering JBoss Web virtual hosts (vhosts).
 *
 * @author Ian Springer
 */
public class VirtualHostDiscoveryComponent
        implements ResourceDiscoveryComponent<JBossWebComponent>
{
    // A regex for the names of all MBean:WebHost components,
    // e.g. "jboss.web:host=localhost,type=Host"
    private static final String WEB_HOST_COMPONENT_NAMES_REGEX = "jboss.web:host=([^,]+),type=Host";

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<JBossWebComponent> discoveryContext) throws Exception
    {
        ResourceType resourceType = discoveryContext.getResourceType();
        log.trace("Discovering " + resourceType.getName() + " Resources...");

        JBossWebComponent jbossWebComponent = discoveryContext.getParentResourceComponent();
        ManagementView managementView = jbossWebComponent.getConnection().getManagementView();

        // TODO (ips): Only refresh the ManagementView *once* per runtime discovery scan, rather than every time this
        //             method is called. Do this by providing a runtime scan id in the ResourceDiscoveryContext.
        managementView.load();

        Set<ManagedComponent> webHostComponents = getWebHostComponents(managementView);
        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet(webHostComponents.size());
        for (ManagedComponent webHostComponent : webHostComponents)
        {
            // Parse the component name, e.g. "jboss.web:host=localhost,type=Host", to figure out the name.
            Pattern pattern = Pattern.compile(WEB_HOST_COMPONENT_NAMES_REGEX);
            Matcher matcher = pattern.matcher(webHostComponent.getName());
            if (!matcher.matches())
            {
                log.error("Component name '" + webHostComponent.getName() + "' does not match regex '"
                        + pattern + "'.");
                continue;
            }
            String name = matcher.group(1);

            String resourceKey = name;
            String resourceName = name;
            String resourceDescription = resourceType.getDescription();
            String resourceVersion = null;

            Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
            pluginConfig.put(new PropertySimple(ManagedComponentComponent.Config.COMPONENT_NAME,
                    webHostComponent.getName()));
            pluginConfig.put(new PropertySimple(VirtualHostComponent.NAME_PROPERTY, name));

            DiscoveredResourceDetails resource =
                    new DiscoveredResourceDetails(resourceType,
                            resourceKey,
                            resourceName,
                            resourceVersion,
                            resourceDescription,
                            pluginConfig,
                            null);

            discoveredResources.add(resource);
        }

        log.trace("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " Resources.");
        return discoveredResources;
    }

    private static Set<ManagedComponent> getWebHostComponents(ManagementView managementView)
            throws Exception
    {
        ComponentType webHostComponentType = MoreKnownComponentTypes.MBean.WebHost.getType();
        //return managementView.getMatchingComponents(WEB_HOST_COMPONENT_NAMES_REGEX,
        //        webHostComponentType, new RegularExpressionNameMatcher());
        return ManagedComponentUtils.getManagedComponents(managementView, webHostComponentType,
                WEB_HOST_COMPONENT_NAMES_REGEX, new RegularExpressionNameMatcher());
    }
}