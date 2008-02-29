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
package org.rhq.core.pc.inventory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.SystemInfoFactory;

/**
 * @author <a href="mailto:ghinkle@jboss.com">Greg Hinkle</a>
 */
public class RuntimeDiscoveryExecutor implements Runnable, Callable<InventoryReport> {
    private Log log = LogFactory.getLog(RuntimeDiscoveryExecutor.class);

    private InventoryManager inventoryManager;
    private PluginContainerConfiguration pluginContainerConfiguration;

    /**
     * Optional agent to use when running the scan. If this is null, the platform's agent will be used.
     */
    private Resource resource;

    public RuntimeDiscoveryExecutor(InventoryManager inventoryManager,
        PluginContainerConfiguration pluginContainerConfiguration) {
        this.inventoryManager = inventoryManager;
        this.pluginContainerConfiguration = pluginContainerConfiguration;
    }

    /**
     * Creates a new <code>RuntimeDiscoveryExecutor</code> instance that will run a discovery scoped to the specified
     * agent.
     *
     * @param inventoryManager             hook back to the inventory manager
     * @param pluginContainerConfiguration configuration of this executor
     * @param resource                     scopes the runtime scan to a particular resource
     */
    public RuntimeDiscoveryExecutor(InventoryManager inventoryManager,
        PluginContainerConfiguration pluginContainerConfiguration, Resource resource) {
        this(inventoryManager, pluginContainerConfiguration);
        this.resource = resource;
    }

    public void run() {
        call();
    }

    public InventoryReport call() {
        try {
            log.info("Running runtime report for resource: " + resource);

            Agent reportAgent = inventoryManager.getAgent();

            InventoryReport report = new InventoryReport(reportAgent);
            report.setRuntimeReport(true);
            report.setStartTime(System.currentTimeMillis());

            runtimeDiscover(report);

            report.setEndTime(System.currentTimeMillis());

            if (log.isDebugEnabled()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(report);
                log.debug("Runtime report for " + report.getResourceCount() + " resources with a size of "
                    + baos.size() + " bytes");
            }

            this.inventoryManager.handleReport(report);

            return report;
        } catch (Exception e) {
            log.error("Error running runtime report", e);
            return null;
        }
    }

    private void runtimeDiscover(InventoryReport report) throws PluginContainerException {
        if (this.resource == null) {
            // Run a full scan for all resources in the inventory
            Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

            // Discover platform services here
            discoverForResource(platform, report, false);

            Set<Resource> servers = platform.getChildResources();
            for (Resource server : servers) {
                discoverForResource(server, report, false);
            }
        } else {
            // Run a single scan for just a resource and its descendants
            discoverForResource(resource, report, false);
        }
    }

    /**
     * @param  parent         The parent resource to look for children of
     * @param  report         The report to add the resource to
     * @param  parentReported true if the resources parent is already in the inventory report and therefore will include
     *                        this resource and its descendants in the report under that root
     *
     * @throws PluginContainerException
     */
    private void discoverForResource(Resource parent, InventoryReport report, boolean parentReported)
        throws PluginContainerException {
        // TODO GH: If resource.isRuntimeDiscoveryEnabled
        // TODO GH: If resoure.isInventoryStatusCommitted

        ResourceContainer parentContainer = inventoryManager.getResourceContainer(parent);
        if (parentContainer == null) {
            log.debug("Parent ResourceComponent unavailable " + "to allow for runtime discovery " + parent.toString());
            return;
        }

        if (parentContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
            log.debug("Parent ResourceComponent not in a running state " + "to allow for runtime discovery "
                + parent.toString());
            return;
        }

        if (parent.getInventoryStatus() != InventoryStatus.COMMITTED) {
            log.debug("Parent resource must first be imported (i.e., in the COMMITTED state) "
                + "to allow for runtime discovery " + parent.toString());
            return;
        }

        ResourceComponent parentComponent = parentContainer.getResourceComponent();
        if (parentComponent == null) {
            log.debug("Parent ResourceComponent was null so we can't runtime discovery on " + parent.toString());
            return;
        }

        PluginComponentFactory factory = PluginContainer.getInstance().getPluginComponentFactory();

        // For each child resource type of the server, do a discovery for resources of that type
        for (ResourceType childResourceType : parent.getResourceType().getChildResourceTypes()) {
            // Make sure we have a discovery component for that type, otherwise there is nothing to do
            ResourceDiscoveryComponent discoveryComponent = null;
            try {
                discoveryComponent = factory.getDiscoveryComponent(childResourceType);
            } catch (PluginContainerException pce) {
                log.error("Unable to run component discovery for [" + childResourceType + "]", pce);
            }

            if (discoveryComponent == null) {
                log.debug("Resource not discoverable, no component found " + childResourceType);
                continue; // Assume its not discoverable
            }

            // For this resource type, discover all resources of that type on this parent resource
            log.debug("Running Runtime discovery on server: " + parent.toString() + " for children: "
                + childResourceType);
            Set<Resource> childResources = executeComponentDiscovery(childResourceType, discoveryComponent,
                parentComponent);

            // For each discovered resource, update it in the inventory manager and recursively discover its child resources
            Set<Resource> newResources = new HashSet<Resource>();

            for (Resource childResource : childResources) {
                Resource matchedResource = inventoryManager.mergeResourceFromDiscovery(childResource, parent);
                boolean thisInReport = false;
                if ((matchedResource.getId() == 0) && !parentReported) {
                    report.addAddedRoot(parent);
                    thisInReport = true;
                    parentReported = true;
                }

                // If this is a new resource, add to list to be fired as resource add event
                if (childResource.getUuid().equals(matchedResource.getUuid())) {
                    newResources.add(childResource);
                }

                discoverForResource(matchedResource, report, thisInReport);
            }

            // Once we have all the discovered resources for this type, remove all resources of this type
            // under this server that were not rediscovered.
            if (!pluginContainerConfiguration.isInsideAgent()) {
                Set<Resource> removedResources = inventoryManager.removeStaleResources(parent, childResourceType,
                    childResources);

                inventoryManager.fireResourcesRemoved(removedResources);
                inventoryManager.fireResourcesAdded(newResources);
            }
        }
    }

    private Set<Resource> executeComponentDiscovery(ResourceType resourceType, ResourceDiscoveryComponent component,
        ResourceComponent parentComponent) {
        try {
            if (AvailabilityType.UP.equals(parentComponent.getAvailability())) // TODO GH: This is doing a live availability check, is that right?
            {
                ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, parentComponent,
                    SystemInfoFactory.createSystemInfo(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                    pluginContainerConfiguration.getContainerName());
                Set<DiscoveredResourceDetails> discoveredResources = component.discoverResources(context);
                Set<Resource> newResources = new HashSet<Resource>();
                if ((discoveredResources != null) && (discoveredResources.size() > 0)) {
                    for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
                        newResources.add(InventoryManager.createNewResource(discoveredResource));
                    }
                }

                return newResources;
            }
        } catch (Throwable e) {
            // TODO GH: Add server/parent - up/down semantics so this won't happen just because a server is not up
            log.warn("Failed to execute resource discovery", e);
        }

        return Collections.EMPTY_SET;
    }
}