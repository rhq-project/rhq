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

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.jboss.managed.api.ManagedComponent;

/**
 * @author Ian Springer
 */
public class Ejb3BeanDiscoveryComponent extends ManagedComponentDiscoveryComponent {
    @Override
    protected String getResourceName(ManagedComponent component) {
        // e.g. "jboss.j2ee:service=EJB3,name=SecureProfileService-metrics-instance"
        String componentName = component.getName();
        ObjectName objectName;
        try {
            objectName = new ObjectName(componentName);
        }
        catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Component name '" + componentName + "' is not a valid ObjectName.", e);
        }
        // e.g. "SecureProfileService-metrics-instance"
        String rawName = objectName.getKeyProperty("name");
        // e.g. "SecureProfileService"
        return rawName.substring(0, rawName.indexOf("-metrics-instance"));
    }
}
