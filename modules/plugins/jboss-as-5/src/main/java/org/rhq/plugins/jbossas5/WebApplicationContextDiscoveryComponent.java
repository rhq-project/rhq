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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.helper.MoreKnownComponentTypes;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.util.RegularExpressionNameMatcher;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;

/**
 * A component for discovering the contexts of a WAR - one context per vhost the WAR is deployed to.
 *
 * @author Ian Springer
 */
public class WebApplicationContextDiscoveryComponent
        implements ResourceDiscoveryComponent<AbstractManagedDeploymentComponent>
{
    private static final String CONTEXT_COMPONENT_NAME = "ContextMO";

    // A regex for the names of all MBean:WebApplication components for a WAR
    // (one component per vhost that WAR is deployed to).
    private static final String WEB_APPLICATION_COMPONENT_NAMES_REGEX_TEMPLATE =
            "jboss.web:J2EEApplication=none,J2EEServer=none,j2eeType=WebModule,name=//[^/]+%"
                    + AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY + "%";

    // The name of the MBean:WebApplicationManager component for a WAR.
    private static final String WEB_APPLICATION_MANAGER_COMPONENT_NAME_TEMPLATE =
            "jboss.web:host=%" + WebApplicationContextComponent.VIRTUAL_HOST_PROPERTY + "%,"
          + "path=%" + AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY + "%,type=Manager";

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<AbstractManagedDeploymentComponent> discoveryContext) throws Exception
    {
        ResourceType resourceType = discoveryContext.getResourceType();
        log.trace("Discovering " + resourceType.getName() + " Resources...");

        AbstractManagedDeploymentComponent parentWarComponent = discoveryContext.getParentResourceComponent();
        ManagedDeployment deployment = parentWarComponent.getManagedDeployment();
        ManagedComponent contextComponent = deployment.getComponent(CONTEXT_COMPONENT_NAME);
        // e.g. "/jmx-console"
        String contextPath = (String)ManagedComponentUtils.getSimplePropertyValue(contextComponent, "contextRoot");

        ManagementView managementView = parentWarComponent.getConnection().getManagementView();

        // TODO (ips): Only refresh the ManagementView *once* per runtime discovery scan, rather than every time this
        //             method is called. Do this by providing a runtime scan id in the ResourceDiscoveryContext.
        managementView.load();
        
        Set<ManagedComponent> webApplicationComponents = getWebApplicationComponents(contextPath, managementView);
        Set<DiscoveredResourceDetails> discoveredResources = new LinkedHashSet(webApplicationComponents.size());
        for (ManagedComponent webApplicationComponent : webApplicationComponents)
        {
            String virtualHost = getWebApplicationComponentVirtualHost(webApplicationComponent);
            String resourceName = "//" + virtualHost + contextPath;
            //noinspection UnnecessaryLocalVariable
            String resourceKey = virtualHost;
            String resourceVersion = null;

            Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();

            // Make sure to set the "virtualHost" and "contextPath" props before setting the "componentName" props,
            // since those two props are referenced in the template for the "componentName" property.
            pluginConfig.put(new PropertySimple(WebApplicationContextComponent.VIRTUAL_HOST_PROPERTY, virtualHost));
            pluginConfig.put(new PropertySimple(AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY, contextPath));

            // e.g. "jboss.web:J2EEApplication=none,J2EEServer=none,j2eeType=WebModule,name=//localhost/jmx-console"
            String webApplicationManagerComponentName =
                    ResourceComponentUtils.replacePropertyExpressionsInTemplate(WEB_APPLICATION_MANAGER_COMPONENT_NAME_TEMPLATE,
                    pluginConfig);
            pluginConfig.put(new PropertySimple(ManagedComponentComponent.Config.COMPONENT_NAME,
                    webApplicationManagerComponentName));

            DiscoveredResourceDetails resource =
                    new DiscoveredResourceDetails(resourceType,
                            resourceKey,
                            resourceName,
                            resourceVersion,
                            resourceType.getDescription(),
                            pluginConfig,
                            null);

            discoveredResources.add(resource);
        }

        log.trace("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " Resources.");
        return discoveredResources;
    }

    static Set<String> getVirtualHosts(String contextPath, ManagementView managementView) throws Exception
    {
        Set<String> virtualHosts = new HashSet();
        Set<ManagedComponent> webApplicationManagerComponents = getWebApplicationComponents(contextPath,
                managementView);
        for (ManagedComponent webApplicationManagerComponent : webApplicationManagerComponents)
        {
            virtualHosts.add(getWebApplicationComponentVirtualHost(webApplicationManagerComponent));
        }
        return virtualHosts;
    }

    private static Set<ManagedComponent> getWebApplicationComponents(String contextPath, ManagementView managementView)
            throws Exception
    {
        String webApplicationManagerComponentNamesRegex =
                WEB_APPLICATION_COMPONENT_NAMES_REGEX_TEMPLATE.replaceAll("%"
                        + AbstractWarDiscoveryComponent.CONTEXT_PATH_PROPERTY + "%", contextPath);
        ComponentType webApplicationComponentType = MoreKnownComponentTypes.MBean.WebApplication.getType();
        return managementView.getMatchingComponents(webApplicationManagerComponentNamesRegex,
                        webApplicationComponentType, new RegularExpressionNameMatcher());
    }

    private static String getWebApplicationComponentVirtualHost(ManagedComponent webApplicationComponent)
    {
        ObjectName objectName;
        try
        {
            objectName = new ObjectName(webApplicationComponent.getName());
        }
        catch (MalformedObjectNameException e)
        {
            throw new IllegalStateException("'" + webApplicationComponent.getName() + "' is not a valid JMX ObjectName.");
        }
        // e.g. "//localhost/jmx-console"
        String name = objectName.getKeyProperty("name");
        // Return just the host portion, e.g. "localhost".
        return name.substring(2, name.lastIndexOf('/'));
    }
}