/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

/**
 * This class is responsible for performing the resource upgrade on behalf on of {@link InventoryManager}.
 * Despite its name, it is not an {@link java.util.concurrent.Executor} implementation.
 * 
 * @author Lukas Krejci
 */
public class ResourceUpgradeExecutor implements Runnable {
    private static final Log log = LogFactory.getLog(ResourceUpgradeExecutor.class);

    /**
     * The upgrade runs only once in its lifetime.
     */
    private AtomicBoolean enabled = new AtomicBoolean(true);
    private AtomicBoolean started = new AtomicBoolean(false);
    
    private InventoryManager inventoryManager;

    private ConcurrentLinkedQueue<ResourceUpgradeRequest> requests;

    public ResourceUpgradeExecutor(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
        requests = new ConcurrentLinkedQueue<ResourceUpgradeRequest>();
    }

    /**
     * This method is called from within the {@link InventoryManager} during the 
     * discovery process as individual upgrade requests are generated.
     * <p>
     * This method calls the appropriate discovery component to perform the upgrade and
     * at the same time stores off the results so that they can be sent to the server
     * at the end of the discovery in one big batch.
     * <p>
     * The provided set of discovery results is updated if this method detects that an existing
     * resource upgrade would result in a resource key collision with some of the newly discovered
     * resources.
     * 
     * @param request the upgrade request
     * @param discoveredResources the set of discovered resources that this method can optionally remove some elements from.
     */
    public <T extends ResourceComponent> Map<ResourceUpgradeContext<T>, ResourceUpgradeReport> processAndQueue(
        ResourceUpgradePendingRequest<T> request, Set<Resource> discoveredResources) {

        return enabled.get() ? executeResourceUpgradeAndStoreRequest(request, discoveredResources) : null;
    }

    /**
     * @return true if the executor is ready to perform the upgrade, false otherwise.
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * This method is called from within the {@link InventoryManager} once it's collected
     * all the upgrade requests.
     */
    public void sendRequests() {
        if (enabled.get() && requests.size() > 0) {
            HashSet<ResourceUpgradeRequest> currentCopy = new HashSet<ResourceUpgradeRequest>(requests);
            inventoryManager.mergeResourceFromUpgrade(currentCopy);
            requests.removeAll(currentCopy);
        }
    }

    /**
     * Performs the resource upgrade. First, a full inventory report is pulled down from the
     * server to ensure we have an up-to-date picture of the inventory.
     * After that a full discovery is performed and both the upgrade report and discovery report
     * are sent to the server.
     */
    public void run() {
        if (!started.getAndSet(true)) {
            if (log.isDebugEnabled()) {
                log.debug("Starting resource upgrade.");
            }

            try {
                //pull the inventory down from the server.
                //this will ensure that we have an up-to-date info on the
                //server inventory before we try to upgrade.
                boolean syncResult = inventoryManager.handleReport(new InventoryReport(inventoryManager.getAgent()), true);
                if (!syncResult) {
                    log.warn("Resource upgrade failed to sync up the inventory with the server.");
                    return;
                }

                //fire off the full discovery
                inventoryManager.executeServerScanImmediately();
                inventoryManager.executeServiceScanImmediately();
            } catch (Throwable t) {
                log.warn("Resource upgrade failed.", t);
            } finally {
                //make sure to switch off the upgrade...
                this.enabled.set(false);

                //clear up so that we don't hold unnecessary instances.
                requests.clear();

                if (log.isDebugEnabled()) {
                    log.debug("Resource upgrade finished.");
                }
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("ResourceUpgradeExecutor already ran before. Skipping the execution.");
            }
        }
    }

    /**
     * Executes the resource upgrade request on the agent (if the appropriate discovery component support {@link ResourceUpgradeFacet}).
     * 
     * @param request the upgrade request to execute on the agent.
     * @param discoveredResources the set of the discovered resources that this method can remove some elements from
     * @return the resulting upgrade map as reported by the plugin from {@link ResourceUpgradeFacet#upgrade(Set, ResourceUpgradeContext, Set)} method.
     * 
     * @throws PluginContainerException
     */
    private <T extends ResourceComponent> Map<ResourceUpgradeContext<T>, ResourceUpgradeReport> executeResourceUpgradeAndStoreRequest(
        ResourceUpgradePendingRequest<T> request, Set<Resource> discoveredResources) {

        ResourceDiscoveryContext<T> discoveryContext = request.getDiscoveryContext();
        Set<Resource> newResources = request.getDiscoveredResources();
        Integer parentResourceId = request.getParentResourceId();

        try {
            ResourceType resourceType = discoveryContext.getResourceType();
            ResourceContainer parentResourceContainer = null;
            Resource parentResource = null;
            if (parentResourceId != null) {
                parentResourceContainer = inventoryManager.getResourceContainer(parentResourceId);
                if (parentResourceContainer != null) {
                    parentResource = parentResourceContainer.getResource();
                }
            }

            @SuppressWarnings("unchecked")
            ResourceDiscoveryComponent<T> discoveryComponent = PluginContainer.getInstance()
                .getPluginComponentFactory().getDiscoveryComponent(resourceType, parentResourceContainer);

            ResourceContext<?> parentResourceContext = discoveryContext.getParentResourceContext();

            T parentComponent = discoveryContext.getParentResourceComponent();

            //check if the discovery component supports resource upgrade
            //if it does, perform the resource upgrade straight away here during the discovery process.
            if (discoveryComponent instanceof ResourceUpgradeFacet) {
                //the siblings are the resources of the type that is currently being discovered that are already
                //present in the inventory. These are the candidate resources for the upgrade.
                Set<Resource> siblings = parentResource == null ? Collections.singleton(inventoryManager.getPlatform())
                    : inventoryManager.getResourcesWithType(resourceType, parentResource.getChildResources());

                //filter out not committed resources
                //XXX what about uninventoried or ignored ones? is it correct to not upgrade those?
                Iterator<Resource> siblingsIterator = siblings.iterator();
                while (siblingsIterator.hasNext()) {
                    if (siblingsIterator.next().getInventoryStatus() != InventoryStatus.COMMITTED) {
                        siblingsIterator.remove();
                    }
                }
                
                //get the upgrade context of the parent resource so that it can be passed to the upgrade method
                //of the discovery component.
                ResourceUpgradeContext<?> parentUpgradeContext = null;

                if (parentResource != null) {
                    Resource grandParent = parentResource.getParentResource();
                    ResourceContainer grandParentContainer = grandParent == null ? null : inventoryManager
                        .getResourceContainer(grandParent);

                    @SuppressWarnings("unchecked")
                    ResourceDiscoveryComponent<ResourceComponent> parentDiscoveryComponent = PluginContainer
                        .getInstance().getPluginComponentFactory().getDiscoveryComponent(
                            parentResource.getResourceType(), grandParentContainer);

                    ResourceComponent<?> grandParentResourceComponent = grandParentContainer == null ? null
                        : grandParentContainer.getResourceComponent();

                    parentUpgradeContext = inventoryManager.createResourceUpgradeContext(parentResource,
                        grandParentResourceComponent, parentDiscoveryComponent);
                }

                //convert the sibling resources into upgrade context objects so that the plugin methods don't access the
                //domain objects directly.
                Set<ResourceUpgradeContext<T>> siblingContexts = new HashSet<ResourceUpgradeContext<T>>(siblings.size());

                //but we are going to need to update the resources in the end, so map the contexts with the resources.
                Map<ResourceUpgradeContext<T>, Resource> siblingContextToResource = new HashMap<ResourceUpgradeContext<T>, Resource>();

                for (Resource sibling : siblings) {
                    ResourceUpgradeContext<T> siblingContext = inventoryManager.createResourceUpgradeContext(sibling,
                        parentComponent, discoveryComponent);
                    siblingContexts.add(siblingContext);
                    siblingContextToResource.put(siblingContext, sibling);
                }

                //convert the new resources into contexts.
                //map the resources by resource key so that we can later check for uniqueness of the reported results.
                Set<ResourceUpgradeContext<T>> newResourceContexts = new HashSet<ResourceUpgradeContext<T>>();
                Map<String, Resource> newResourceKeyToResource = new HashMap<String, Resource>();

                for (Resource newResource : newResources) {
                    ResourceUpgradeContext<T> newUpgradeContext = inventoryManager.createResourceUpgradeContext(
                        newResource, parentComponent, discoveryComponent);
                    newResourceContexts.add(newUpgradeContext);
                    newResourceKeyToResource.put(newResource.getResourceKey(), newResource);
                }

                //ask the discovery component to upgrade the siblings.
                Map<ResourceUpgradeContext<T>, ResourceUpgradeReport> results;
                try {
                    results = inventoryManager.invokeDiscoveryComponentResourceUpgradeFacet(resourceType,
                        discoveryComponent, siblingContexts, parentUpgradeContext, newResourceContexts);

                    //now go through the results and create the upgrade requests.
                    for (Map.Entry<ResourceUpgradeContext<T>, ResourceUpgradeReport> upgradeEntry : results.entrySet()) {
                        Resource siblingToUpgrade = siblingContextToResource.get(upgradeEntry.getKey());
                        ResourceUpgradeRequest newData = new ResourceUpgradeRequest(siblingToUpgrade.getId(), upgradeEntry.getValue());

                        requests.add(newData);

                        //if there was a resource key upgrade, remove a resource with the same resource key
                        //from the discovery results. Otherwise we'd end up with 2 sibling resources with
                        //the same resource key, which is illegal.
                        if (newData.getNewResourceKey() != null) {
                            Resource newResourceToRemove = newResourceKeyToResource.get(newData.getNewResourceKey());
                            if (newResourceToRemove != null) {
                                discoveredResources.remove(newResourceToRemove);
                            }
                        }
                    }

                    return results;
                } catch (Throwable t) {
                    log.warn("Exception in discovery component " + discoveryComponent.getClass() + " upgrade method.",
                        t);
                }
            }
        } catch (PluginContainerException e) {
            log.warn("Failed resource upgrade of children of resource id " + parentResourceId, e);
        }
        return null;
    }
}
