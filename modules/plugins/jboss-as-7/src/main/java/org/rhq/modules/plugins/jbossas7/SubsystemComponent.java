/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Resource component for subsystems
 *
 * Deprecated - use BaseComponent
 * @author Heiko W. Rupp
 */
@Deprecated
public class SubsystemComponent  implements ResourceComponent<BaseComponent> {

    ResourceContext<BaseComponent> context;
    Configuration config;
    String path;
    String key ;

    public void start(ResourceContext<BaseComponent> context) throws InvalidPluginConfigurationException, Exception {
        this.config = context.getPluginConfiguration();
        path = config.getSimpleValue("path", null);
        this.context = context;
        key = context.getResourceKey();

    }

    public void stop() {
        // TODO: Customise this generated block
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;  // TODO: Customise this generated block
    }


}
