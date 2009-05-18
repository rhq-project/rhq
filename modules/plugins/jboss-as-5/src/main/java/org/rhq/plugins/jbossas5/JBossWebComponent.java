/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

/**
 * A ResourceComponent for managing the JBoss Web servlet container within a JBoss AS instance.
 *
 * @author Ian Springer
 */
public class JBossWebComponent implements ProfileServiceComponent<ProfileServiceComponent>
{
    private ResourceContext<ProfileServiceComponent> resourceContext;

    public void start(ResourceContext<ProfileServiceComponent> resourceContext) throws Exception
    {
        this.resourceContext = resourceContext;
    }

    public void stop()
    {
        return;
    }

    public AvailabilityType getAvailability()
    {
        return AvailabilityType.UP;
    }

    public ProfileServiceConnection getConnection()
    {
        return this.resourceContext.getParentResourceComponent().getConnection();
    }
}