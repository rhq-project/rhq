/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * A simple utility class to extract the name of a JMX server from the parent resource's plugin configuration.
 * 
 * @author Lukas Krejci
 */
public class ParentDefinedJMXServerNamingUtility {
    public static final String PROPERTY_CHILD_JMX_SERVER_NAME = "childJmxServerName";

    /**
     * Checks if the parent resource's plugin configuration contains a property called {@link #PROPERTY_CHILD_JMX_SERVER_NAME}.
     * If such property exists and its value is non-empty, its value is returned. Otherwise the name of the provided
     * resource type is returned.
     * 
     * @param context the discovery context to get the parent plugin configuration and current resource type from.
     * @return the name that can be used for the JVM
     */
    public static String getJVMName(ResourceDiscoveryContext<?> context) {
        Configuration parentPluginConfiguration = context.getParentResourceContext().getPluginConfiguration();
        PropertySimple nameProperty = parentPluginConfiguration.getSimple(PROPERTY_CHILD_JMX_SERVER_NAME);
        if (nameProperty == null || nameProperty.getStringValue() == null
            || nameProperty.getStringValue().trim().length() == 0) {

            return context.getResourceType().getName();
        } else {
            return nameProperty.getStringValue();
        }
    }
}
