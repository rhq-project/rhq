/*

 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.sonarqube;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Jeremie Lagarde
 */
public class SonarQubeServerComponent implements ResourceComponent<ResourceComponent<?>> {

    private ResourceContext resourceContext;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            String status = SonarQubeJSONUtility.getStatus(this.resourceContext.getPluginConfiguration()
                .getSimple("urlBase").getStringValue());

            if (AvailabilityType.UP.getName().equalsIgnoreCase(status)) {
                return AvailabilityType.UP;
            } else {
                return AvailabilityType.DOWN;
            }
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    public String getPath() {
        return this.resourceContext.getPluginConfiguration().getSimple("urlBase").getStringValue();
    }

}
