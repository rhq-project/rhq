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

package org.rhq.core.pc.upgrade.plugins.multi.base;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

/**
 *
 * @author Lukas Krejci
 */
public class BaseUpgradingDiscoveryComponent<T extends BaseResourceComponent> extends BaseDiscoveryComponent<T> implements ResourceUpgradeFacet<T> {

    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<T> inventoriedResource) {
        Configuration pluginConfig = inventoriedResource.getPluginConfiguration();
        
        BaseResourceComponent<?> parent = inventoriedResource.getParentResourceComponent();
        int ordinal = pluginConfig.getSimple("ordinal").getIntegerValue();
        int parentOrdinal = parent == null ? 0 : parent.getOrdinal();
        
        boolean fail = Boolean.getBoolean(pluginConfig.getSimpleValue("failUpgrade", "false"));
        
        if (!fail && parent != null) {
            fail = parent.getChildrenToFailUpgrade().contains(Integer.valueOf(ordinal));
        }
        
        if (fail) {
            throw new RuntimeException("Failing the resource upgrade purposefully.");
        }
           
        String newKey = pluginConfig.getSimpleValue("upgradedKey", null);
        
        if (newKey == null) {
            return null;
        }
                
        ResourceUpgradeReport report = new ResourceUpgradeReport();
        
        report.setNewResourceKey(getResourceKey(newKey, ordinal, parentOrdinal));
        return null;
    }
}
