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

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;

/**
 * @author Ian Springer
 */
public abstract class AbstractWarDiscoveryComponent extends AbstractManagedDeploymentDiscoveryComponent
{
    public static final String CONTEXT_PATH_PROPERTY = "contextPath";

    private static final String CONTEXT_COMPONENT_NAME = "ContextMO";

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ProfileServiceComponent> discoveryContext)
    {
        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(discoveryContext);
        ManagementView managementView =
                discoveryContext.getParentResourceComponent().getConnection().getManagementView();

        
        for (Iterator<DiscoveredResourceDetails> iterator = discoveredResources.iterator(); iterator.hasNext();)
        {
            DiscoveredResourceDetails discoveredResource = iterator.next();
            try
            {
                Configuration pluginConfig = discoveredResource.getPluginConfiguration();
                String deploymentName = pluginConfig.getSimple(
                        AbstractManagedDeploymentComponent.DEPLOYMENT_NAME_PROPERTY).getStringValue();
                ManagedDeployment deployment = managementView.getDeployment(deploymentName);
                ManagedComponent contextComponent = deployment.getComponent(CONTEXT_COMPONENT_NAME);
                // e.g. "/jmx-console"
                String contextPath = (String)ManagedComponentUtils.getSimplePropertyValue(contextComponent, "contextRoot");
                pluginConfig.put(new PropertySimple(CONTEXT_PATH_PROPERTY, contextPath));
            }
            catch (Exception e)
            {
                // Don't let one bad apple spoil the barrel.
                log.error("Failed to determine context root for WAR '" + discoveredResource.getResourceName() 
                        + "' - WAR will not be discovered.");
                iterator.remove();
            }
        }
        return discoveredResources;
    }
}
