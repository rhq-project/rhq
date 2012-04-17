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

package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
public class ThreadPoolComponent extends BaseComponent<ResourceComponent<?>> {

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        //type is a fake property, do not attempt to load it from the managed server
        configDef.getPropertyDefinitions().remove("type");
        ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef, connection, address);
        Configuration configuration = delegate.loadResourceConfiguration();

        //manually load type based on the resource path
        PropertySimple pathProperty = (PropertySimple) context.getPluginConfiguration().get("path");
        String type = pathProperty.getStringValue();
        type = type.substring(path.lastIndexOf(',') + 1, type.lastIndexOf('='));
        configuration.put(new PropertySimple("type", type));

        return configuration;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        //type is a fake property, do not attempt to save it.
        configDef.getPropertyDefinitions().remove("type");
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, connection, address);

        //type is a fake property, do not attempt to save it.
        report.getConfiguration().remove("type");
        delegate.updateResourceConfiguration(report);
    }
}
