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
package org.rhq.plugins.jmx;

import org.mc4j.ems.connection.EmsConnection;

import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * Any server or service in an EMS-based plugin would likely implement this interface to allow access to the underlying
 * EMS JMX system. Any EMS-based plugin should be able to use this for interoperable JMX support between plugins.
 *
 * @author Greg Hinkle
 */
public interface JMXComponent<T extends ResourceComponent> extends ResourceComponent<T> {
    public static final String PRINCIPAL_CONFIG_PROP = "principal";
    public static final String CREDENTIALS_CONFIG_PROP = "credentials";

    EmsConnection getEmsConnection();
}