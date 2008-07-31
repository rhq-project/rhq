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
package org.rhq.plugins.hardware;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class HardwareDiscoveryComponent implements ResourceDiscoveryComponent {

    public static enum HardwareType { ACPI_IBM }


    public static final String PROC_ACPI_IBM = "/proc/acpi/ibm";

    public Set<DiscoveredResourceDetails>  discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
            throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();

        Configuration pluginConfig = resourceDiscoveryContext.getDefaultPluginConfiguration();

        if (new File(PROC_ACPI_IBM).exists()) {

            File info = new File("/proc/acpi/info");

            pluginConfig.put(new PropertySimple("type", HardwareType.ACPI_IBM));

            DiscoveredResourceDetails details =
                    new DiscoveredResourceDetails(
                            resourceDiscoveryContext.getResourceType(),
                            PROC_ACPI_IBM,
                            "IBM Hardware",
                            null,
                            "IBM Hardware Monitoring",
                            pluginConfig,
                            null);
                            
            found.add(details);
        }
        ProcessInfo.class.getName();


        return found;
    }
}
