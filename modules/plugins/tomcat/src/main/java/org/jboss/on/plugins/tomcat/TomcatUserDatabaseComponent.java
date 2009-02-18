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

import org.jboss.on.plugins.tomcat.helper.CreateResourceHelper;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Manage a Tomcat User Database
 * 
 * @author Jay Shaughnessy
 */
public class TomcatUserDatabaseComponent extends MBeanResourceComponent<TomcatServerComponent> implements CreateChildResourceFacet {

    public CreateResourceReport createResource(CreateResourceReport report) {
        String resourceTypeName = report.getResourceType().getName();
        try {
            if (TomcatGroupComponent.RESOURCE_TYPE_NAME.equals(resourceTypeName)) {
                this.invokeOperation("createGroup", report.getPluginConfiguration());
            } else if (TomcatRoleComponent.RESOURCE_TYPE_NAME.equals(resourceTypeName)) {
                this.invokeOperation("createRole", report.getPluginConfiguration());
            } else if (TomcatUserComponent.RESOURCE_TYPE_NAME.equals(resourceTypeName)) {
                Configuration opConfig = report.getPluginConfiguration();
                opConfig.put(report.getResourceConfiguration().get(TomcatUserComponent.PROPERTY_FULL_NAME));
                opConfig.put(report.getResourceConfiguration().get(TomcatUserComponent.PROPERTY_PASSWORD));
            } else {
                throw new UnsupportedOperationException("Unsupported Resource type: " + resourceTypeName);
            }
        } catch (Exception e) {
            CreateResourceHelper.setErrorOnReport(report, e);
        }
        return report;

    }

}
