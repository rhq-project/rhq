/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

package org.rhq.core.pluginapi.upgrade;

import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * A resource upgrade callback can be used to further modify the results of resource upgrade done by the discovery
 * component of the originating resource type.
 * <p/>
 * The changes made should usually be additive as the originating resource type might have added requirements that
 * it expects to be present after the upgrade.
 *
 * @author Lukas Krejci
 * @since 4.13
 */
public interface ResourceUpgradeCallback<T extends ResourceComponent<?>> {

    /**
     * Can update the upgrade report with additional details.
     *
     * @see org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet#upgrade(ResourceUpgradeContext)
     *
     * @param upgradeReport the upgrade report returned from the originating discovery component
     * @param inventoriedResource the resource being upgraded
     */
    void upgrade(ResourceUpgradeReport upgradeReport, ResourceUpgradeContext<T> inventoriedResource);
}
