/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

public class Ejb3Component extends BaseComponent<BaseComponent<?>> {

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();

        PropertySimple derive = report.getConfiguration().getSimple("derive-size");
        PropertySimple maxPoolSize = report.getConfiguration().getSimple("max-pool-size:expr");
        if(derive != null) {
            if("none".equals(derive.getStringValue()) && (maxPoolSize != null && maxPoolSize.getStringValue() != null)) {
                configDef.getPropertyDefinitions().remove(derive.getName());
            } else if(derive.getStringValue() != null && maxPoolSize != null) { // Verify this
                configDef.getPropertyDefinitions().remove(maxPoolSize.getName());
            }
        }

        ConfigurationReadWriteDelegate delegate = new ConfigurationReadWriteDelegate(configDef, getASConnection(),
                address);
        delegate.updateResourceConfiguration(report);
    }

}
