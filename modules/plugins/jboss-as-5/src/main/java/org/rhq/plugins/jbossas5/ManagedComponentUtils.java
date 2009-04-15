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
package org.rhq.plugins.jbossas5;

import java.util.Set;

import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.SimpleValue;

/**
 * @author Ian Springer
 */
public class ManagedComponentUtils
{
    public static ManagedComponent getSingletonManagedComponent(ComponentType componentType)
    {
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        Set<ManagedComponent> components;
        try
        {
            components = managementView.getComponentsForType(componentType);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
        if (components.size() != 1)
            throw new IllegalStateException("Found more than one component of type " + componentType + ": "
                    + components);
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        ManagedComponent component = components.iterator().next();
        return component;
    }

    public static String getSimplePropertyStringValue(ManagedComponent component, String propertyName)
    {
        ManagedProperty versionManagedProperty = component.getProperty(propertyName);
        SimpleValue versionSimpleValue = (SimpleValue)versionManagedProperty.getValue();
        return (versionSimpleValue != null) ? (String)versionSimpleValue.getValue() : null;
    }
}