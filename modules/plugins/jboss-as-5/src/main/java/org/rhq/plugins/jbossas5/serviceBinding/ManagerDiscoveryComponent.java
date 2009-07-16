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
package org.rhq.plugins.jbossas5.serviceBinding;

import org.jboss.managed.api.ManagedComponent;
import org.rhq.plugins.jbossas5.ManagedComponentDiscoveryComponent;

/**
 * Manager component for the Service Binding Manager.
 * 
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public class ManagerDiscoveryComponent extends ManagedComponentDiscoveryComponent<ManagerComponent> {

    /**
     * Return a pretty human readable resource name. 
     */
    @Override
    protected String getResourceName(ManagedComponent component) {
        return "Service Binding Manager";
    }

    
}
