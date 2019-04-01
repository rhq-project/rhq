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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10;

import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.FAILURE;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.Result;

public class WorkerComponent extends BaseComponent {

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // When testing EAP 7.1 and 7.2, you cannot set to undefined io-threads and task-max-threads, doing so
        // causes an IllegalArgumentException, but if the mode is reload-required, it allows to do so.
        // Updating io-threads get us into that mode, workarounds the problem by setting a dummy value for io-threads prior
        // to the real update. This allows us to write these values. io-threads is going to set it to reload-required anyway
        // so it seems safe to do
        Operation operation = new Operation("write-attribute", address);
        operation.addAdditionalProperty("name", "io-threads");
        operation.addAdditionalProperty("value", "1");
        Result result = getASConnection().execute(operation);
        if (!result.isSuccess()) {
            report.setStatus(FAILURE);
            report.setErrorMessage(result.getFailureDescription());
            return;
        }

        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }
}
