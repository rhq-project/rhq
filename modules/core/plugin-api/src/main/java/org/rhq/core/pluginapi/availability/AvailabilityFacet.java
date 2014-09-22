/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pluginapi.availability;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Provides the basic availability checking for all managed resources.
 *
 * @author John Mazzitelli
 */
public interface AvailabilityFacet {

    /**
     * The plugin container will occasionally call this method at the server level to see if the server is available.
     * This method is intended to attempt a remote connection to the resource. When a sever is in the down state, no
     * child discoveries will be performed. A downed resource also shuts down access at the child level - for example,
     * if a JBossAS instance is down, the plugin container will not try to collect availabilities or metrics on an EJB
     * entity running inside it. Availability for all child resources would automatically be set to
     * {@link AvailabilityType#DOWN down} in this case.
     *
     * @return {@link AvailabilityType#UP} if the resource is responding as expected; {@link AvailabilityType#DOWN} if
     * it is not responding as expected; {@link AvailabilityType#MISSING} if the resource type supports this feature
     * and the resource is physically missing. Any other return value is invalid.
     */
    AvailabilityType getAvailability();
}
