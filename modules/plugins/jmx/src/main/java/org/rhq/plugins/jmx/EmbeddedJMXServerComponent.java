/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.plugins.jmx;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * A component for an "embedded" JVM Resource, which lets its parent component manage the
 * {@link org.mc4j.ems.connection.EmsConnection}.
 *
 * @author Ian Springer
 */
public class EmbeddedJMXServerComponent<T extends JMXComponent<?>> extends JMXServerComponent<T> {

    @Override
    public void start(ResourceContext context) throws Exception {
        Configuration pluginConfig = context.getPluginConfiguration();
        String connectionType = pluginConfig.getSimpleValue(JMXDiscoveryComponent.CONNECTION_TYPE, "");
        if (!connectionType.equals(JMXDiscoveryComponent.PARENT_TYPE)) {
            throw new InvalidPluginConfigurationException("The only legal connection type for embedded JVM Resources is \""
                    + JMXDiscoveryComponent.PARENT_TYPE + "\".");
        }

        super.start(context);
    }

    @Override
    public AvailabilityType getAvailability() {
        // Our parent manages the EMS connection, so if it's UP, then we're UP too.
        return AvailabilityType.UP;
    }

}
