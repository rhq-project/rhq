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

package org.rhq.core.pc.upgrade.plugins.failing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 *
 * @author Lukas Krejci
 */
public class ResComponent<T extends ResourceComponent<?>> implements ResourceComponent<T> {
    private static final Log log = LogFactory.getLog(ResComponent.class);

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception {
        log.info("~~~ starting the resource upgrade test failing component with resource key: "
            + context.getResourceKey());
    }

    public void stop() {
    }
}
