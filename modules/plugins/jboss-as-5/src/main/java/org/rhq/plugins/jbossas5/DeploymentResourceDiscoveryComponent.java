 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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

import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.jbossas5.util.ConversionUtil;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.NoSuchDeploymentException;

import java.util.Set;
import java.util.HashSet;

/**
 * Discovery class for discovering deployable resources like ear/war/jar/sar
 *
 * @author: Mark Spritzler
 */
public class DeploymentResourceDiscoveryComponent implements ResourceDiscoveryComponent<ProfileJBossServerComponent> {
    private final Log log = LogFactory.getLog(this.getClass());

    private final String DEPLOYMENT_PROPERTY_NAME = "deploymentName";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ProfileJBossServerComponent> resourceDiscoveryContext)
    {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
        ResourceType resourceType = resourceDiscoveryContext.getResourceType();
        log.info("Discovering " + resourceType.getName() + " Resources..." );
        String deploymentTypeString = ConversionUtil.getDeploymentTypeString(resourceType);

        // TODO (ips): Only refresh the ManagementView *once* per runtime discovery scan, rather than every time this
        //             method is called. Do this by providing a runtime scan id in the ResourceDiscoveryContext.
        ProfileServiceFactory.refreshCurrentProfileView();

        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();

        Set<String> deployments = null;
        try
        {
            deployments = managementView.getDeploymentNamesForType(deploymentTypeString);
        }
        catch (Exception e)
        {
            log.error("Unable to get deployment for type " + deploymentTypeString, e);
        }

        if (deployments != null)
        {

            discoveredResources = new HashSet<DiscoveredResourceDetails>(deployments.size());
            /* Create a resource for each managed component found. We know all managed components will be of a
               type we're interested in, so we can just add them all. There may be need for multiple iterations
               over lists retrieved from different component types, but that is possible through the current API.
            */
            for (String deployment : deployments)
            {
                try
                {
                    ManagedDeployment managedDeployment = managementView.getDeployment(deployment, ManagedDeployment.DeploymentPhase.APPLICATION);
                    String resourceName = managedDeployment.getSimpleName();
                    // @TODO remove this when AS5 actually implements this for sars, and some other DeploymentTypes that haven't implemented getSimpleName()
                    if (resourceName.equals("%Generated%"))
                    {
                        resourceName = getResourceName(deployment);
                    }
                    String version = "?"; // TODO
                    DiscoveredResourceDetails resource =
                            new DiscoveredResourceDetails(resourceType,
                                    deployment,
                                    resourceName,
                                    version,
                                    resourceType.getDescription(), null, null);

                    resource.getPluginConfiguration().put(new PropertySimple(DEPLOYMENT_PROPERTY_NAME, deployment));
                    discoveredResources.add(resource);
                }
                catch (NoSuchDeploymentException e)
                {
                    // This is a bug in the profile service that occurs often, so don't log the stack trace.
                    log.error("ManagementView.getDeploymentNamesForType() returned [" + deployment
                            + "] as a deployment name, but calling getDeployment() with that name failed.");
                }
                catch (Exception e)
                {
                    log.error("An error occurred while discovering " + resourceType + " Resources.", e);
                }
            }
        }

        log.info("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " Resources." );
        return discoveredResources;
    }

    private static String getResourceName(String fullPath)
    {
        int lastSlashIndex = fullPath.lastIndexOf("/");                
        return fullPath.substring(lastSlashIndex + 1);
    }
}
