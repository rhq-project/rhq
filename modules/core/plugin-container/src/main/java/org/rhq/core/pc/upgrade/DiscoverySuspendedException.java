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

import org.rhq.core.pc.inventory.InventoryManager;

/**
 * This exception is thrown from the {@link InventoryManager#invokeDiscoveryComponent(org.rhq.core.pc.inventory.ResourceContainer, org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent, org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)} if the
 * discovery is suspended for given resource type due to a resource upgrade failure.
 * 
 * @author Lukas Krejci
 */
public class DiscoverySuspendedException extends Exception {

    private static final long serialVersionUID = 1L;

    public DiscoverySuspendedException() {
        super();
    }

    public DiscoverySuspendedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiscoverySuspendedException(String message) {
        super(message);
    }

    public DiscoverySuspendedException(Throwable cause) {
        super(cause);
    }

}
