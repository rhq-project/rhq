/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
public class ConnectorDiscoveryGroupValidatorComponent extends BaseComponent<ResourceComponent<?>> {

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration resourceConfiguration = report.getConfiguration();
        ResourceType resourceType = this.context.getResourceType();

        // we need to check that a connector XOR a discovery-group-name is given
        int configuredItemsFound = 0;
        String errorMessage = "";

        if (resourceType.getName().equals("Connection Factory")
            || resourceType.getName().equals("Pooled Connection Factory")) {

            PropertyMap connector = resourceConfiguration.getMap("connector:collapsed");
            if (connector != null) {
                String name = connector.getSimpleValue("name:0", "");
                if (!name.isEmpty()) {
                    configuredItemsFound++;
                }
            }

            errorMessage = "You need to provide either a connector name OR a discovery-group-name. ";
        } else if (resourceType.getName().equals("Bridge") || resourceType.getName().equals("Cluster Connection")) {

            PropertyList staticConnectors = resourceConfiguration.getList("static-connectors:nullable");
            if (staticConnectors != null) {
                if (!staticConnectors.getList().isEmpty()) {
                    configuredItemsFound++;
                }
            }

            errorMessage = "You need to provide either static connectors name OR a discovery-group-name. ";
        }

        String discoveryGroup = resourceConfiguration.getSimpleValue("discovery-group-name", "");
        if (!discoveryGroup.isEmpty()) {
            configuredItemsFound++;
        }

        if (configuredItemsFound != 1) {
            errorMessage += (configuredItemsFound == 0) ? "You provided none." : "You provided both.";
            report.setErrorMessage(errorMessage);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        } else {
            super.updateResourceConfiguration(report);
        }
    }
}
