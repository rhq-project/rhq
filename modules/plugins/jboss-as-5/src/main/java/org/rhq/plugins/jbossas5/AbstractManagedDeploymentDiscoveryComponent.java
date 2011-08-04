/*
* Jopr Management Platform
* Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.managed.api.DeploymentState;
import org.jboss.profileservice.spi.Profile;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.plugins.jbossas5.util.ConversionUtils;

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.NoSuchDeploymentException;

/**
 * Discovery component for discovering JBAS 5.x/6.x deployments (EARs, WARs, EJB-JARs, etc.).
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public abstract class AbstractManagedDeploymentDiscoveryComponent
        implements ResourceDiscoveryComponent<ProfileServiceComponent>, ResourceUpgradeFacet<ProfileServiceComponent>
{

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ProfileServiceComponent> discoveryContext)
    {
        ResourceType resourceType = discoveryContext.getResourceType();
        log.trace("Discovering " + resourceType.getName() + " Resources...");
        KnownDeploymentTypes deploymentType = ConversionUtils.getDeploymentType(resourceType);
        String deploymentTypeString = deploymentType.getType();

        ManagementView managementView = discoveryContext.getParentResourceComponent().getConnection().getManagementView();
        // TODO (ips): Only refresh the ManagementView *once* per runtime discovery scan, rather than every time this
        //             method is called. Do this by providing a runtime scan id in the ResourceDiscoveryContext.        
        managementView.load();

        Set<String> deploymentNames = null;
        try
        {
            deploymentNames = managementView.getDeploymentNamesForType(deploymentTypeString);
        }
        catch (Exception e)
        {
            log.error("Unable to get deployment names for type " + deploymentTypeString, e);
        }

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(deploymentNames.size());

        ProfileService profileService = discoveryContext.getParentResourceComponent().getConnection().getProfileService();

        // Create a resource details for each managed deployment found.
        for (String deploymentName : deploymentNames)
        {
            // example of a deployment name: vfszip:/C:/opt/jboss-6.0.0.Final/server/default/deploy/http-invoker.sar/invoker.war/
            try
            {
                ManagedDeployment managedDeployment = managementView.getDeployment(deploymentName);
                if (!accept(managedDeployment)) {
                    continue;
                }
                String resourceName = managedDeployment.getSimpleName();
                // @TODO remove this when AS5 actually implements this for sars, and some other DeploymentTypes that haven't implemented getSimpleName()
                if (resourceName.equals("%Generated%"))
                {
                    resourceName = getResourceName(deploymentName);
                }

                // example of a resource key: {default}http-invoker.sar/invoker.war
                String resourceKey = buildResourceKey(managedDeployment, profileService);

                managedDeployment.getSimpleName();
                String version = null; // TODO (ManagedDeployment "version" property?)
                DiscoveredResourceDetails resource =
                        new DiscoveredResourceDetails(resourceType,
                                resourceKey,
                                resourceName,
                                version,
                                resourceType.getDescription(),
                                discoveryContext.getDefaultPluginConfiguration(),
                                null);
                resource.getPluginConfiguration().put(
                        new PropertySimple(AbstractManagedDeploymentComponent.DEPLOYMENT_NAME_PROPERTY, deploymentName));
                discoveredResources.add(resource);
            }
            catch (NoSuchDeploymentException e)
            {
                // This is a bug in the profile service that occurs often, so don't log the stack trace.
                log.error("ManagementView.getDeploymentNamesForType() returned [" + deploymentName
                        + "] as a deployment name, but calling getDeployment() with that name failed.");
            }
            catch (Exception e)
            {
                log.error("An error occurred while discovering " + resourceType + " Resources.", e);
            }
        }

        log.trace("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " Resources.");
        return discoveredResources;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<ProfileServiceComponent> upgradeContext) {
        String inventoriedResourceKey = upgradeContext.getResourceKey();

        // check if the inventoried resource already has the new resource key format.
        // the new format is "{default}http-invoker.sar/invoker.war", while the old format
        // was "vfszip:/C:/opt/jboss-6.0.0.Final/server/default/deploy/http-invoker.sar/invoker.war/".
        if (inventoriedResourceKey.startsWith("{")) {
            // key is already in the new format
            return null;
        }

        // key is in the old format - build a key in the new format
        ManagementView managementView = upgradeContext.getParentResourceComponent().getConnection().getManagementView();
        ManagedDeployment deployment;
        try {
            deployment = managementView.getDeployment(inventoriedResourceKey);
        } catch (NoSuchDeploymentException e) {
            throw new IllegalStateException(e);
        }
        ProfileService profileService = upgradeContext.getParentResourceComponent().getConnection().getProfileService();
        String resourceKey = buildResourceKey(deployment, profileService);

        ResourceUpgradeReport upgradeReport = new ResourceUpgradeReport();
        upgradeReport.setNewResourceKey(resourceKey);

        return upgradeReport;
    }

    private static String buildResourceKey(ManagedDeployment deployment, ProfileService profileService) {
        StringBuilder resourceKey = new StringBuilder();

        String profileName = getProfileName(deployment, profileService);
        resourceKey.append('{').append(profileName).append("}");

        List<String> deploymentAncestrySimpleNames = new ArrayList<String>();
        ManagedDeployment parentDeployment = deployment;
        do {
            deploymentAncestrySimpleNames.add(0, parentDeployment.getSimpleName());
        } while ((parentDeployment = parentDeployment.getParent()) != null);

        for (int i = 0, deploymentAncestrySimpleNamesSize = deploymentAncestrySimpleNames.size();
             i < deploymentAncestrySimpleNamesSize; i++) {
            String deploymentSimpleName = deploymentAncestrySimpleNames.get(i);
            resourceKey.append(deploymentSimpleName);
            if (i != (deploymentAncestrySimpleNamesSize - 1)) {
                resourceKey.append("/");
            }
        }

        return resourceKey.toString();
    }

    private static String getProfileName(ManagedDeployment deployment, ProfileService profileService) {
        Collection<ProfileKey> profileKeys = profileService.getActiveProfileKeys();
        for (ProfileKey profileKey : profileKeys) {
            Profile profile;
            try {
                profile = profileService.getActiveProfile(profileKey);
            } catch (Exception e) {
                DeploymentManager deploymentManager = profileService.getDeploymentManager();
                try {
                    deploymentManager.loadProfile(profileKey);
                } catch (Exception e1) {
                    continue;
                }
                DeploymentState deploymentState = deployment.getDeploymentState();
                try {
                    DeploymentProgress progress;
                    if (deploymentState == DeploymentState.STARTED || deploymentState == DeploymentState.STARTING) {
                        progress = deploymentManager.start(deployment.getName());
                    } else {
                        progress = deploymentManager.stop(deployment.getName());
                    }
                    progress.run();
                    DeploymentStatus status = progress.getDeploymentStatus();
                    if (status.isFailed()) {
                        continue;
                    }
                } catch (Exception e1) {
                    continue;
                }
                return profileKey.getName();
            }
            if (profile.hasDeployment(deployment.getName())) {
                return profileKey.getName();
            }
        }
        return ProfileKey.DEFAULT;
    }

    protected abstract boolean accept(ManagedDeployment managedDeployment);

    private static String getResourceName(String fullPath)
    {
        int lastSlashIndex = fullPath.lastIndexOf("/");
        return fullPath.substring(lastSlashIndex + 1);
    }

}
