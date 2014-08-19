/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.inventory;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.inventory.InventoryContext;

/**
 * @author Stefan Negrea
 */
public class InventoryContextImpl implements InventoryContext {
    private final Resource resource;
    private final InventoryManager inventoryManager;

    public InventoryContextImpl(Resource resource, InventoryManager inventoryManager) {
        this.resource = resource;
        this.inventoryManager = inventoryManager;
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.InventoryContext#discoverChildResources()
     */
    @Override
    public void requestDeferredChildResourcesDiscovery() {
        inventoryManager.executeServiceScanDeferred(resource.getId());
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.InventoryContext#requestChildResourcesDiscovery()
     */
    @Override
    public void requestChildResourcesDiscovery() {
        inventoryManager.executeServiceScanImmediately(resource);
    }
}
