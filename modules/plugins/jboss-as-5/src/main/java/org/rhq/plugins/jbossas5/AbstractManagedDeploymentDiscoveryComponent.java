/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import static org.rhq.plugins.jbossas5.AbstractManagedDeploymentComponent.DEPLOYMENT_KEY_PROPERTY;
import static org.rhq.plugins.jbossas5.AbstractManagedDeploymentComponent.DEPLOYMENT_NAME_PROPERTY;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.jboss.profileservice.spi.ProfileKey;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.ConversionUtils;

/**
 * Discovery component for discovering JBAS 5.x/6.x deployments (EARs, WARs, EJB-JARs, etc.).
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public abstract class AbstractManagedDeploymentDiscoveryComponent implements
    ResourceDiscoveryComponent<ProfileServiceComponent<?>>, ResourceUpgradeFacet<ProfileServiceComponent<?>> {

    private static final Log LOG = LogFactory.getLog(AbstractManagedDeploymentDiscoveryComponent.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ProfileServiceComponent<?>> discoveryContext) {

        ResourceType resourceType = discoveryContext.getResourceType();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Discovering " + resourceType.getName() + " Resources...");
        }

        ProfileServiceComponent<?> parentResourceComponent = discoveryContext.getParentResourceComponent();
        ManagementView managementView = getManagementView(parentResourceComponent.getConnection());
        if (managementView == null) {
            return Collections.emptySet();
        }

        Set<String> deploymentNames = getDeploymentNamesForType(managementView, resourceType);
        if (deploymentNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>(
            deploymentNames.size());

        // Create a resource details for each managed deployment found.
        for (String deploymentName : deploymentNames) {

            // example of deploymentName: vfszip:/C:/opt/jboss-6.0.0.Final/server/default/deploy/http-invoker.sar/invoker.war/
            try {

                ManagedDeployment managedDeployment = managementView.getDeployment(deploymentName);
                if (!accept(managedDeployment)) {
                    continue;
                }

                String resourceName = managedDeployment.getSimpleName();
                if (resourceName.equals("%Generated%")) {
                    resourceName = deploymentName.substring(deploymentName.lastIndexOf("/") + 1);
                }

                // example of a resource key: {default}http-invoker.sar/invoker.war
                String resourceKey = buildResourceKey(managedDeployment);

                DiscoveredResourceDetails resource = new DiscoveredResourceDetails(resourceType, resourceKey,
                    resourceName, null, resourceType.getDescription(),
                    discoveryContext.getDefaultPluginConfiguration(), null);

                String deploymentKey = URI.create(deploymentName).getPath();
                resource.getPluginConfiguration().put(new PropertySimple(DEPLOYMENT_KEY_PROPERTY, deploymentKey));

                discoveredResources.add(resource);

            } catch (NoSuchDeploymentException e) {
                // This is a bug in the profile service that occurs often, so don't log the stack trace.
                LOG.error("ManagementView.getDeploymentNamesForType() returned [" + deploymentName
                    + "] as a deployment name, but calling getDeployment() with that name failed.");
            } catch (Exception e) {
                LOG.error("An error occurred while discovering " + resourceType + " resources.", e);
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Discovered " + discoveredResources.size() + " " + resourceType.getName() + " resource(s).");
        }
        return discoveredResources;
    }

    protected abstract boolean accept(ManagedDeployment managedDeployment);

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<ProfileServiceComponent<?>> upgradeContext) {
        // check if the inventoried resource already has the new resource key format.
        // the new format is "{default}http-invoker.sar/invoker.war", while the old format
        // was "vfszip:/C:/opt/jboss-6.0.0.Final/server/default/deploy/http-invoker.sar/invoker.war/".
        String newResourceKey = null;
        String existingResourceKey = upgradeContext.getResourceKey();
        if (!existingResourceKey.startsWith("{")) {
            newResourceKey = getNewResourceKey(upgradeContext, existingResourceKey);
        }

        Configuration newPluginConfiguration = null;
        Configuration existingPluginConfig = upgradeContext.getPluginConfiguration();
        if (existingPluginConfig.getSimpleValue(DEPLOYMENT_NAME_PROPERTY) != null) {
            newPluginConfiguration = getNewPluginConfig(upgradeContext);
        }

        if (newResourceKey != null || newPluginConfiguration != null) {
            ResourceUpgradeReport upgradeReport = new ResourceUpgradeReport();
            upgradeReport.setNewResourceKey(newResourceKey);
            upgradeReport.setNewPluginConfiguration(newPluginConfiguration);
            return upgradeReport;
        }

        return null;
    }

    private String getNewResourceKey(ResourceUpgradeContext<ProfileServiceComponent<?>> upgradeContext,
        String existingResourceKey) {

        String newResourceKey;
        ProfileServiceConnection connection = upgradeContext.getParentResourceComponent().getConnection();
        if (connection == null) {
            LOG.warn(getClass().getName() + ": could not upgrade resource, no profile service connection available");
            return null;
        }
        ManagementView managementView = connection.getManagementView();
        ManagedDeployment deployment;
        try {
            deployment = managementView.getDeployment(existingResourceKey);
        } catch (NoSuchDeploymentException e) {
            throw new IllegalStateException(e);
        }
        newResourceKey = buildResourceKey(deployment);
        return newResourceKey;
    }

    private Configuration getNewPluginConfig(ResourceUpgradeContext<ProfileServiceComponent<?>> upgradeContext) {
        Configuration newPluginConfiguration = upgradeContext.getPluginConfiguration().deepCopy(false);
        String deploymentName = newPluginConfiguration.getSimpleValue(DEPLOYMENT_NAME_PROPERTY);
        newPluginConfiguration.remove(DEPLOYMENT_NAME_PROPERTY);
        String deploymentKey = URI.create(deploymentName).getPath();
        newPluginConfiguration.put(new PropertySimple(DEPLOYMENT_KEY_PROPERTY, deploymentKey));
        return newPluginConfiguration;
    }

    private static String buildResourceKey(ManagedDeployment deployment) {
        StringBuilder resourceKey = new StringBuilder();
        resourceKey.append('{').append(ProfileKey.DEFAULT).append("}");
        List<String> ancestryNames = new ArrayList<String>();
        ManagedDeployment parentDeployment = deployment;
        do {
            ancestryNames.add(0, parentDeployment.getSimpleName());
        } while ((parentDeployment = parentDeployment.getParent()) != null);
        for (int i = 0, ancestryNamesSize = ancestryNames.size(); i < ancestryNamesSize; i++) {
            String deploymentSimpleName = ancestryNames.get(i);
            resourceKey.append(deploymentSimpleName);
            if (i != (ancestryNamesSize - 1)) {
                resourceKey.append("/");
            }
        }
        return resourceKey.toString();
    }

    static ManagementView getManagementView(ProfileServiceConnection connection) {
        if (connection == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No profile service connection available");
            }
            return null;
        }
        ManagementView managementView = connection.getManagementView();
        managementView.load();
        return managementView;
    }

    static Set<String> getDeploymentNamesForType(ManagementView managementView, ResourceType resourceType) {
        KnownDeploymentTypes deploymentType = ConversionUtils.getDeploymentType(resourceType);
        String deploymentTypeString = deploymentType.getType();
        Set<String> deploymentNames;
        try {
            deploymentNames = managementView.getDeploymentNamesForType(deploymentTypeString);
        } catch (Exception e) {
            LOG.error("Unable to get deployment names for type " + deploymentTypeString, e);
            deploymentNames = Collections.emptySet();
        }
        return deploymentNames;
    }
}
