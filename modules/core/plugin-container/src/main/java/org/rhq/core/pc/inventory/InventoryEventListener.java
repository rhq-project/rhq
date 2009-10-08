/*
 * RHQ Management Platform
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
package org.rhq.core.pc.inventory;

import java.util.Set;

import org.rhq.core.domain.resource.Resource;

/**
 * Implementations of this class are notified of changes to the inventory model. Registration of these listeners is done
 * through the {@link InventoryManager#addInventoryEventListener(InventoryEventListener)}.
 *
 * @author Jason Dobies
 */
public interface InventoryEventListener {
    /**
     * Indicates the specified resources were added to the inventory.
     *
     * @param resources resources added to trigger this event; cannot be <code>null</code>
     */
    void resourcesAdded(Set<Resource> resources);

    /**
     * Indicates the specified resources were removed from the inventory.
     *
     * @param resources resources removed to trigger this event; cannot be <code>null</code>
     */
    void resourcesRemoved(Set<Resource> resources);

    /**
     * Indicates a resource has passed all of the necessary approvals and synchronizations to be activated in the plugin
     * container.
     *
     * @param resource
     *
     * @see   InventoryManager#activateResource(Resource)
     */
    void resourceActivated(Resource resource);

    /**
     * Indicates a resource's component has been stopped and deactivated within the plugin container.
     *
     * @param resource
     *
     * @see   InventoryManager#deactivateResource(Resource)
     */
    void resourceDeactivated(Resource resource);
}