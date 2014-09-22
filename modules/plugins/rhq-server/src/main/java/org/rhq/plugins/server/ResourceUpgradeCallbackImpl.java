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

package org.rhq.plugins.server;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeCallback;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.system.ProcessInfo;

/**
 * @author Lukas Krejci
 * @since 4.13
 */
public final class ResourceUpgradeCallbackImpl implements ResourceUpgradeCallback<ResourceComponent<?>> {

    @Override
    public void upgrade(ResourceUpgradeReport upgradeReport,
        ResourceUpgradeContext<ResourceComponent<?>> inventoriedResource) {

        ProcessInfo processInfo = inventoriedResource.getNativeProcess();

        if (DiscoveryCallbackImpl.isRhqServer(processInfo)) {
            Configuration pluginConfiguration = upgradeReport.getNewPluginConfiguration();
            if (pluginConfiguration == null) {
                pluginConfiguration = inventoriedResource.getPluginConfiguration();
            }

            String currentValue = pluginConfiguration.getSimpleValue("supportsPatching");
            if (currentValue == null) {
                // we don't change the value if one is provided even if it is set to true.
                // if that's the case, the user apparently wants to shoot herself in the foot, which we should not
                // prevent.
                pluginConfiguration.setSimpleValue("supportsPatching", "false");
                upgradeReport.setNewPluginConfiguration(pluginConfiguration);
            }

            String resourceName = upgradeReport.getNewName();
            if (resourceName == null) {
                resourceName = inventoriedResource.getName();
            }

            //this is not critical, we can live with the server being called a "wrong" name.
            //report, but not enforce the change.
            if (!resourceName.endsWith(" RHQ Server")) {
                resourceName += " RHQ Server";

                upgradeReport.setNewName(resourceName);
            }
        }
    }
}
