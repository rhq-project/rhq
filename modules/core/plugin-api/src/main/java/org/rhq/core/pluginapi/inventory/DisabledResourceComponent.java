/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.pluginapi.inventory;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * This is used when a resource type is disabled. It simply will do no work
 * and {@link #getAvailability()} will never show UP.
 *
 * @author John Mazzitelli
 */
@SuppressWarnings("rawtypes")
public class DisabledResourceComponent implements ResourceComponent {

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.DOWN;
    }

    @Override
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
    }

    @Override
    public void stop() {
    }
}
