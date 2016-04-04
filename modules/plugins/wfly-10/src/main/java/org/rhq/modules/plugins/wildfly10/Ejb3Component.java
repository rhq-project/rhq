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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Result;
import org.rhq.modules.plugins.wildfly10.json.UndefineAttribute;
import org.rhq.modules.plugins.wildfly10.json.WriteAttribute;

import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.FAILURE;

public class Ejb3Component extends BaseComponent<BaseComponent<?>> {

    private static final String DERIVE_ATTRIBUTE = "derive-size";
    private static final String MAX_POOL_SIZE = "max-pool-size";

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration config = report.getConfiguration();

        PropertySimple derive = config.getSimple("derive-size");
        PropertySimple maxPoolSize = config.getSimple("max-pool-size");

        CompositeOperation cop = new CompositeOperation();

        if((derive == null || derive.getStringValue() == null) && maxPoolSize != null) {
            cop.addStep(new UndefineAttribute(address, DERIVE_ATTRIBUTE));
            cop.addStep(new WriteAttribute(address, MAX_POOL_SIZE, config.getSimpleValue(MAX_POOL_SIZE)));
        } else if(derive != null && (maxPoolSize == null || maxPoolSize.getStringValue() == null)) {
            cop.addStep(new UndefineAttribute(address, MAX_POOL_SIZE));
            cop.addStep(new WriteAttribute(address, DERIVE_ATTRIBUTE, config.getSimpleValue(DERIVE_ATTRIBUTE)));
        } else {
            report.setStatus(FAILURE);
            report.setErrorMessage("You have to select between derive-size and max-pool-size, both can't be set or unset at the same time");
            return;
        }

        Result result = getASConnection().execute(cop);
        if (!result.isSuccess()) {
            report.setStatus(FAILURE);
            report.setErrorMessage(result.getFailureDescription());
            return;
        }

        // Update resource configuration
        ConfigurationDefinition configDefCopy = context.getResourceType().getResourceConfigurationDefinition().copy();
        configDefCopy.getPropertyDefinitions().remove(MAX_POOL_SIZE);
        configDefCopy.getPropertyDefinitions().remove(DERIVE_ATTRIBUTE);

        ConfigurationReadWriteDelegate delegate = new ConfigurationReadWriteDelegate(configDefCopy, getASConnection(),
                address);
        delegate.updateResourceConfiguration(report);
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = super.loadResourceConfiguration();

        PropertySimple derive = configuration.getSimple(DERIVE_ATTRIBUTE);
        if(derive != null && "none".equals(derive.getStringValue())) {
            derive.setValue(null);
        }
        return configuration;
    }
}
