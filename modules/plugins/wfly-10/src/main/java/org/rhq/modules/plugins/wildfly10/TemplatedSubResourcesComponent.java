/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
public class TemplatedSubResourcesComponent extends BaseComponent<ResourceComponent<?>> {

    private final static String TYPE_CONFIGURATION = "__type";
    private final static String NAME_CONFIGURATION = "__name";

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        if (report.getResourceConfiguration().get(TYPE_CONFIGURATION) != null) {
            //Need to munge the report and configuration definition for with type in configuration:
            //1) Remove type from the properties and configuration.
            //2) Update path to the selected type.
            ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition();
            configDef.getPropertyDefinitions().remove(TYPE_CONFIGURATION);
            CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, getASConnection(), address);

            Configuration configuration = report.getResourceConfiguration();
            PropertySimple typeProperty = (PropertySimple) configuration.get(TYPE_CONFIGURATION);
            configuration.remove(TYPE_CONFIGURATION);
            report.getPluginConfiguration().put(new PropertySimple("path", typeProperty.getStringValue()));

            return delegate.createResource(report);
        } else if (report.getResourceConfiguration().get(NAME_CONFIGURATION) != null) {
            //Need to munge the report and configuration definition for with name in configuration:
            //1) Remove name from the properties and configuration.
            //2) Update user specified name to the implicitly selected name.
            ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition();
            configDef.getPropertyDefinitions().remove(NAME_CONFIGURATION);
            CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, getASConnection(), address);

            Configuration configuration = report.getResourceConfiguration();
            PropertySimple nameProperty = (PropertySimple) configuration.get(NAME_CONFIGURATION);
            configuration.remove(NAME_CONFIGURATION);
            report.setUserSpecifiedResourceName(nameProperty.getStringValue());

            return delegate.createResource(report);
        } else {
            return super.createResource(report);
        }
    }
}
