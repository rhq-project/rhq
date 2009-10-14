/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.hosts.helper.NonAugeasHostsConfigurationDelegate;

/**
 * The ResourceComponent for the "Hosts File" ResourceType.
 *
 * @author Ian Springer
 */
public class HostsComponent extends AugeasConfigurationComponent {
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {                        
        super.start(resourceContext);
    }

    public void stop() {
        return;
    }

    public AvailabilityType getAvailability() {
        return super.getAvailability();
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception
    {
        Configuration resourceConfig;
        if (getAugeas() != null) {
            resourceConfig = super.loadResourceConfiguration();
        } else {
            resourceConfig = new NonAugeasHostsConfigurationDelegate(this).loadResourceConfiguration();
        }
        return resourceConfig;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report)
    {
        if (getAugeas() != null) {
            super.updateResourceConfiguration(report);
        } else {
            new NonAugeasHostsConfigurationDelegate(this).updateResourceConfiguration(report);
        }
    }
}
