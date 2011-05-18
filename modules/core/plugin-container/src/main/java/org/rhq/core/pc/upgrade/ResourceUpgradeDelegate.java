/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

/**
 * This is a helper class to {@link InventoryManager} that takes care of resource upgrade.
 * Note that this class is not thread-safe in any manner.
 * 
 * This class must not call (explicitly or implicitly anywhere in its outgoing call chain)
 * any other plugin container manager other than the InventoryManager.  This is because
 * this delegate is called within the Inventory Manager's initialize method and hence
 * many other managers are not yet initialized themselves yet.
 * 
 * @author Lukas Krejci
 */
public class ResourceUpgradeDelegate {

    private static final Log log = LogFactory.getLog(ResourceUpgradeDelegate.class);

    private boolean enabled = true;

    private Set<ResourceUpgradeRequest> requests = new HashSet<ResourceUpgradeRequest>();
    private Set<ResourceUpgradeRequest> originalResourceData = new HashSet<ResourceUpgradeRequest>();    
    private InventoryManager inventoryManager;
    private Set<Resource> failedResources = new HashSet<Resource>();
    private Map<Resource, Set<ResourceType>> failedResourceTypesPerParent = new HashMap<Resource, Set<ResourceType>>();
    private boolean mergeFailed;

    public ResourceUpgradeDelegate(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    /**
     * Is the delegate enabled? In another words has the resource upgrade phase finished in the {@link IventoryManager#initialize()}
     * method?
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Disables all future operations of the delegate.
     */
    public void disable() {
        enabled = false;
    }

    /**
     * Asks the resource's discovery component to upgrade the resource.
     * The resource will be updated with the data from the upgrade. I.e. when the resource
     * is activated after it was queued for upgrade, it will actually start with the upgraded
     * data.
     * <p>
     * If later on the upgrade fails to finish due to communication error with server or the
     * server doesn't approve some upgrades for whatever reason, the resources will be restarted
     * with the original data.
     * 
     * @param resourceContainer
     * @return true if the resource was queued for upgrade with no problems,
     * false if there was some problem upgrading and the resource container was deactivated as
     * a results of that.
     * @throws PluginContainerException on error
     */
    public boolean processAndQueue(ResourceContainer resourceContainer) throws PluginContainerException {
        if (enabled) {
            return executeResourceUpgradeFacetAndStoreRequest(resourceContainer);
        }

        return true;
    }

    /**
     * @return true if {@link #sendRequests()} threw an exception, false otherwise
     */
    public boolean hasUpgradeMergeFailed() {
        return mergeFailed;
    }

    /**
     * Tells whether given resource had a upgrade failure during the {@link #processAndQueue(ResourceContainer)} invocation.
     * 
     * @param resource the resource to test
     * @return true if there was an error upgrading this resource, false otherwise
     */
    public boolean hasUpgradeFailed(Resource resource) {
        return mergeFailed || failedResources.contains(resource);
    }

    /**
     * Tells whether at least one of the children with given resource type of the given parent resource
     * had an upgrade failure during {@link #processAndQueue(ResourceContainer)} invocation.
     * 
     * @return true if at least one of the children of given type failed to upgrade, false otherwise
     */
    public boolean hasUpgradeFailedInChildren(Resource parentResource, ResourceType childrenResourceType) {
        if (mergeFailed) {
            return true;
        }

        Set<ResourceType> failedTypes = failedResourceTypesPerParent.get(parentResource);

        return failedTypes != null && failedTypes.contains(childrenResourceType);
    }

    public void sendRequests() throws Throwable {
        if (enabled && requests.size() > 0) {
            try {
                //check the validity of the upgrades now that we have a complete picture
                //about the changes and the inventory looks like it was already upgraded.
                for(ResourceUpgradeRequest request : requests) {
                    ResourceContainer container = inventoryManager.getResourceContainer(request.getResourceId());
                    if (container != null) {
                        Resource resource = container.getResource();
                        String upgradeErrors = null;
                        if ((upgradeErrors = checkUpgradeValid(resource, request)) != null) {
                            //the resource is in its upgraded state but it's going to get reverted back to the original state
                            //in the code below. Let's use the original resource for the error message so that we don't confuse
                            //the user.                            
                            for(ResourceUpgradeRequest orig : originalResourceData) {
                                if (orig.equals(request)) {
                                    orig.updateResource(resource);
                                    break;
                                }
                            }
                            String errorString = "Upgrading the resource [" + resource + "] using these updates [" + request
                                + "] would render the inventory invalid because of the following reasons: " + upgradeErrors;

                            //now switch the resource back to the upgraded state for the rest of the code below again
                            request.updateResource(resource);
                            
                            log.error(errorString);

                            IllegalStateException ex = new IllegalStateException(errorString);
                            ex.fillInStackTrace();

                            //set the error and clear out everything else, so that we send the error
                            //to the server and locally roll back to the previous state.
                            request.setErrorProperties(ex);
                            request.clearUpgradeData();
                            
                            if (request.getUpgradeErrorMessage() != null) {
                                rememberFailure(resource);
                                inventoryManager.deactivateResource(resource);
                            }

                        }
                    }
                }
                
                //now before we talk to server and sync up the upgraded data,
                //reset the resources to their original values so that any changes
                //the server makes to the upgrade data are applied to the "vanilla" state 
                //of the resources. i.e we only want to make changes the server approves.
                for (ResourceUpgradeRequest request : originalResourceData) {
                    ResourceContainer container = inventoryManager.getResourceContainer(request.getResourceId());
                    if (container != null) {
                        Resource resource = container.getResource();
                        request.updateResource(resource);
                    }
                }
                
                //merge the resources with the data as received from the server
                //(this can differ from what the upgrade "wants" because the server is
                //free to disallow some changes, e.g. resource name change)
                inventoryManager.mergeResourcesFromUpgrade(requests);
                
                //and now restart all the "touched" resources with the true intended
                //data
                for (ResourceUpgradeRequest request : requests) {
                    ResourceContainer container = inventoryManager.getResourceContainer(request.getResourceId());
                    if (container != null) {
                        Resource resource = container.getResource();
                        inventoryManager.activateResource(resource, container, true);
                    }
                }
            } catch (Throwable t) {
                mergeFailed = true;

                //deactivate all the resources to be upgraded. We might have a problem
                //because they have not been upgraded because the merge failed.
                for (ResourceUpgradeRequest request : requests) {
                    ResourceContainer container = inventoryManager.getResourceContainer(request.getResourceId());
                    if (container != null) {
                        inventoryManager.deactivateResource(container.getResource());
                    }
                }

                throw t;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends ResourceComponent> boolean executeResourceUpgradeFacetAndStoreRequest(
        ResourceContainer resourceContainer) throws PluginContainerException {

        ResourceComponent<T> parentResourceComponent = resourceContainer.getResourceContext()
            .getParentResourceComponent();
        
        Resource parentResource = resourceContainer.getResource().getParentResource();

        ResourceContainer parentResourceContainer = (parentResource != null) ? inventoryManager
            .getResourceContainer(resourceContainer.getResource().getParentResource()) : null;

        ResourceContext<?> parentResourceContext = parentResourceContainer == null ? null : parentResourceContainer.getResourceContext();
        
        Resource resource = resourceContainer.getResource();
        
        ResourceDiscoveryComponent<ResourceComponent<T>> discoveryComponent = PluginContainer.getInstance()
            .getPluginComponentFactory().getDiscoveryComponent(resource.getResourceType(), parentResourceContainer);

        if (!(discoveryComponent instanceof ResourceUpgradeFacet)) {
            //well, there's no point in continuing if the resource doesn't support the facet
            return true;
        }

        ResourceUpgradeContext<ResourceComponent<T>> upgradeContext = inventoryManager.createResourceUpgradeContext(
            resource, parentResourceContext, parentResourceComponent, discoveryComponent);

        ResourceUpgradeRequest request = new ResourceUpgradeRequest(resource.getId());

        request.setTimestamp(System.currentTimeMillis());

        ResourceUpgradeReport upgradeReport = null;
        try {
            upgradeReport = inventoryManager.invokeDiscoveryComponentResourceUpgradeFacet(resource.getResourceType(),
                discoveryComponent, upgradeContext);
        } catch (Throwable t) {
            log.error("ResourceUpgradeFacet threw an exception while upgrading resource [" + resource + "]", t);
            request.setErrorProperties(t);
        }

        if (upgradeReport != null && upgradeReport.hasSomethingToUpgrade()) {
            request.fillInFromReport(upgradeReport);
        }

        if (request.hasSomethingToUpgrade()) {
            requests.add(request);
        }

        if (request.getUpgradeErrorMessage() != null) {
            rememberFailure(resource);
            return false;
        }
        
        //alright, everything went fine with the upgrade. Let's update the data of the resource
        //right now so that it starts up as if it was already upgraded. This is to ensure that
        //its children will use a parent component that behaves like the upgraded one.
        //We are going to roll back the upgraded data if the upgrade fails to sync with the server
        //later on.
        
        //remember the original values
        ResourceUpgradeRequest original = new ResourceUpgradeRequest(resource.getId());
        original.fillInFromResource(resource);
        originalResourceData.add(original);
        
        //update the resource
        request.updateResource(resource);
        
        return true;
    }

    private String checkUpgradeValid(Resource resource, ResourceUpgradeReport upgradeReport) {
        StringBuilder s = new StringBuilder();

        if (!checkResourceKeyUniqueAmongSiblings(resource, upgradeReport)) {
            s.append("\nAnother inventoried sibling resource of the same type already has the proposed resource key or would have it after the upgrade.");
        }

        return s.length() > 0 ? s.toString() : null;
    }

    private boolean checkResourceKeyUniqueAmongSiblings(Resource resource, ResourceUpgradeReport upgradeReport) {
        Resource parent = resource.getParentResource();
        if (parent == null) {
            //there is only a single platform resource on an agent
            return true;
        }

        for (Resource sibling : parent.getChildResources()) {
            //we'd have a resource key conflict if there was a resource
            //of the same type under the same parent that would have the same
            //resource key.
            if (upgradeReport.getNewResourceKey() != null
                && sibling.getResourceType().equals(resource.getResourceType())
                && !sibling.getUuid().equals(resource.getUuid())) {

                if (sibling.getResourceKey().equals(upgradeReport.getNewResourceKey())) {
                    return false;
                }
            }
        }

        return true;
    }
    
    private void rememberFailure(Resource resource) {
        failedResources.add(resource);
        Resource parentResource = resource.getParentResource();
        
        Set<ResourceType> failedResourceTypesInParent = failedResourceTypesPerParent.get(parentResource);
        if (failedResourceTypesInParent == null) {
            failedResourceTypesInParent = new HashSet<ResourceType>();
            failedResourceTypesPerParent.put(parentResource, failedResourceTypesInParent);
        }

        failedResourceTypesInParent.add(resource.getResourceType());
    }
}
