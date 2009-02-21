/*
 * Jopr Management Platform
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

package org.jboss.on.plugins.tomcat;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Manage a Tomcat Role
 * 
 * @author Jay Shaughnessy
 */
public class TomcatRoleComponent extends MBeanResourceComponent<TomcatUserDatabaseComponent> implements DeleteResourceFacet {

    public static final String CONFIG_DESCRIPTION = "description";
    public static final String CONFIG_ROLE_NAME = "rolename";
    public static final String PLUGIN_CONFIG_NAME = "name";
    public static final String RESOURCE_TYPE_NAME = "Tomcat Role";

    public void deleteResource() throws Exception {
        Configuration opConfig = new Configuration();
        ResourceContext<TomcatUserDatabaseComponent> resourceContext = getResourceContext();
        PropertySimple nameProperty = resourceContext.getPluginConfiguration().getSimple(PLUGIN_CONFIG_NAME);
        String name = nameProperty.getStringValue();
        nameProperty = new PropertySimple(CONFIG_ROLE_NAME, name);
        opConfig.put(nameProperty);
        resourceContext.getParentResourceComponent().invokeOperation("removeRole", opConfig);
    }

}
