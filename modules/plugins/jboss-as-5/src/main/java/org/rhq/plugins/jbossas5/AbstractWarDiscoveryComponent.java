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
package org.rhq.plugins.jbossas5;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URI;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.deployers.spi.management.ManagementView;

/**
 * @author Ian Springer
 */
public abstract class AbstractWarDiscoveryComponent extends AbstractManagedDeploymentDiscoveryComponent
{
    public static final ComponentType WEB_APP_MBEAN_TYPE = new ComponentType("MBean", "WebApplication");
    public static final ComponentType SERVLET_MBEAN_TYPE = new ComponentType("MBean", "Servlet");
    public static final ComponentType WEB_APP_MANAGER_MBEAN_TYPE = new ComponentType("MBean", "WebApplicationManager");

    public static final String VIRTUAL_HOST_PROPERTY = "virtualHost";
    public static final String CONTEXT_PATH_PROPERTY = "contextPath";

    private Pattern WEB_APPLICATION_MBEAN_COMPONENT_NAME_PATTERN = Pattern.compile("//([^/]+)(.*)");

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ApplicationServerComponent> discoveryContext)
    {
        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(discoveryContext);
        ManagementView managementView =
                discoveryContext.getParentResourceComponent().getConnection().getManagementView();
        Map<String, ContextInfo> docBaseToWebAppMBeanComponentNamesMap =
                getWebAppMBeanComponentInfo(managementView);
        for (DiscoveredResourceDetails discoveredResource : discoveredResources)
        {
            Configuration pluginConfig = discoveredResource.getPluginConfiguration();
            String deploymentName = pluginConfig.getSimple(
                    AbstractManagedDeploymentComponent.DEPLOYMENT_NAME_PROPERTY).getStringValue();
            URI deploymentURI = URI.create(deploymentName);
            String docBase = deploymentURI.getPath(); 
            ContextInfo contextInfo = docBaseToWebAppMBeanComponentNamesMap.get(docBase);
            pluginConfig.put(new PropertySimple(VIRTUAL_HOST_PROPERTY, contextInfo.getVirtualHost()));
            pluginConfig.put(new PropertySimple(CONTEXT_PATH_PROPERTY, contextInfo.getContextPath()));
        }
        return discoveredResources;
    }

    private Map<String, ContextInfo> getWebAppMBeanComponentInfo(ManagementView managementView)
    {
        Set<ManagedComponent> webAppMBeanComponents;
        try
        {
            webAppMBeanComponents = managementView.getComponentsForType(WEB_APP_MBEAN_TYPE);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
        Map<String, ContextInfo> docBaseToContextInfoMap = new HashMap();
        for (ManagedComponent webAppMBeanComponent : webAppMBeanComponents)
        {
            // e.g. for non-Seam apps - "/C:/opt/jboss-5.1.0.GA/server/default/deploy/jmx-console.war/",
            //      or, for Seam apps - "/C:/opt/jboss-5.1.0.GA/server/default/tmp/aaaa-otoxhu-fuoc2rj2-1-fuoc3l2h-v/admin-console.war/"
            String docBase = (String)ManagedComponentUtils.getSimplePropertyValue(webAppMBeanComponent, "docBase");
            // Convert the Seam docBase to the same form as non-Seam docBases, so we can correlate it with the
            // corresponding "war" ManagedDeployment, whose name will be a vfs URL with a path in the form of a
            // non-Seam docBase.
            docBase = docBase.replaceFirst("/tmp/[^/]+/", "/deploy/");
            ContextInfo contextInfo = new ContextInfo(docBase);
            // TODO (ips): The below incorrectly assumes there is a single virtual host per docBase.
            docBaseToContextInfoMap.put(docBase, contextInfo);
            Matcher nameMatcher = WEB_APPLICATION_MBEAN_COMPONENT_NAME_PATTERN.matcher(webAppMBeanComponent.getName());
            if (!nameMatcher.find()) {
                log.error("WebApplication MBean component name does not match pattern ["
                        + WEB_APPLICATION_MBEAN_COMPONENT_NAME_PATTERN + "].");
                continue;
            }
            String virtualHost = nameMatcher.group(1);
            contextInfo.setVirtualHost(virtualHost);
            String contextPath = nameMatcher.group(2);
            contextInfo.setContextPath(contextPath);
        }
        return docBaseToContextInfoMap;
    }

    class ContextInfo
    {
        private String docBase;
        private String virtualHost;
        private String contextPath;

        ContextInfo(String docBase)
        {
            this.docBase = docBase;
        }

        public String getDocBase()
        {
            return docBase;
        }

        public String getVirtualHost()
        {
            return virtualHost;
        }

        public void setVirtualHost(String virtualHost)
        {
            this.virtualHost = virtualHost;
        }

        public String getContextPath()
        {
            return contextPath;
        }

        public void setContextPath(String contextPath)
        {
            this.contextPath = contextPath;
        }
    }
}
