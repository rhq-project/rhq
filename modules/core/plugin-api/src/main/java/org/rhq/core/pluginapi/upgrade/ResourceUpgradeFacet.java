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

package org.rhq.core.pluginapi.upgrade;

import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * This interface is to be implemented by discovery classes if they want to support
 * upgrading the existing resources to a new format needed by an updated resource component.
 * This is useful for example in the case when a new version of plugin redefined a resource
 * key generation algorithm and wants to upgrade the legacy resources to use the new format.
 * 
 * @author Lukas Krejci
 */
public interface ResourceUpgradeFacet<T extends ResourceComponent> {
    /**
     * Specifies what data should change on the provided resource to upgrade it to the current version.
     * 
     * @param inventoriedResource a context representing the resource to be upgraded
     * @return a report specifying what aspects of the resource should be changed or null if there is nothing to upgrade
     */
    ResourceUpgradeReport upgrade(ResourceUpgradeContext<T> inventoriedResource);
}
