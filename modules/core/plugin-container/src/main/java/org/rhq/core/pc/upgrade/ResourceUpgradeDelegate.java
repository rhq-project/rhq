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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

/**
 * This is a helper class to {@link InventoryManager} that takes care of resource upgrade.
 * Note that this class is not thread-safe in any manner.
 * 
 * @author Lukas Krejci
 */
public class ResourceUpgradeDelegate {

    private static final Log log = LogFactory.getLog(ResourceUpgradeDelegate.class);

    private boolean enabled = true;

    private Set<ResourceUpgradeRequest> requests = new HashSet<ResourceUpgradeRequest>();
    private InventoryManager inventoryManager;

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
     * @param resourceContainer
     * @return true if the resource was queued for upgrade, false otherwise
     * @throws PluginContainerException on error
     */
    public boolean processAndQueue(ResourceContainer resourceContainer) throws PluginContainerException {
        if (enabled) {
            return executeResourceUpgradeFacetAndStoreRequest(resourceContainer);
        }

        return false;
    }

    public void sendRequests() {
        if (enabled) {
            if (requests.size() > 0) {
                inventoryManager.mergeResourceFromUpgrade(requests);
                requests.clear();
            }
        }
    }

    private <T extends ResourceComponent> boolean executeResourceUpgradeFacetAndStoreRequest(
        ResourceContainer resourceContainer) throws PluginContainerException {

        @SuppressWarnings("unchecked")
        ResourceComponent<T> parentResourceComponent = resourceContainer.getResourceContext()
            .getParentResourceComponent();

        Resource parentResource = resourceContainer.getResource().getParentResource();

        ResourceContainer parentResourceContainer = (parentResource != null) ? inventoryManager
            .getResourceContainer(resourceContainer.getResource().getParentResource()) : null;

        Resource resource = resourceContainer.getResource();

        @SuppressWarnings("unchecked")
        ResourceDiscoveryComponent<ResourceComponent<T>> discoveryComponent = PluginContainer.getInstance()
            .getPluginComponentFactory().getDiscoveryComponent(resource.getResourceType(), parentResourceContainer);

        if (!(discoveryComponent instanceof ResourceUpgradeFacet)) {
            //well, there's no point in continuing if the resource doesn't support the facet
            return false;
        }

        ResourceUpgradeContext<ResourceComponent<T>> upgradeContext = inventoryManager.createResourceUpgradeContext(
            resource, parentResourceComponent, discoveryComponent);

        ResourceUpgradeReport upgradeReport;
        try {
            upgradeReport = inventoryManager.invokeDiscoveryComponentResourceUpgradeFacet(resource.getResourceType(),
                discoveryComponent, upgradeContext);
        } catch (Throwable t) {
            log.error("ResourceUpgradeFacet threw an exception while upgrading resource [" + resource + "]", t);
            return false;
        }

        if (upgradeReport == null || !upgradeReport.hasSomethingToUpgrade()) {
            return false;
        }

        String upgradeErrors = null;
        if ((upgradeErrors = checkUpgradeValid(resource, upgradeReport)) != null) {
            log.error("Upgrading the resource [" + resource + "] using these updates [" + upgradeReport
                + "] would render the inventory invalid because of the following reasons: " + upgradeErrors);
            return false;
        }

        //everything went ok, let's queue a upgrade request that will be sent to the server
        requests.add(new ResourceUpgradeRequest(resource.getId(), upgradeReport));

        return true;
    }

    private String checkUpgradeValid(Resource resource, ResourceUpgradeReport upgradeReport) {
        StringBuilder bld = new StringBuilder();

        if (!checkResourceKeyUniqueAmongSiblings(resource, upgradeReport)) {
            bld.append("\nAnother inventoried sibling resource of the same type already has the proposed resource key.");
        }

        return bld.length() > 0 ? bld.toString() : null;
    }

    private boolean checkResourceKeyUniqueAmongSiblings(Resource resource, ResourceUpgradeReport upgradeReport) {
        Resource parent = resource.getParentResource();
        if (parent == null) {
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
}
