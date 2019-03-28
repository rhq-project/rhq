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

import java.util.Map;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.wildfly10.json.ComplexResult;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.Result;

public class WorkerComponent extends BaseComponent {

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // BZ 1655950 and  WFCORE-4398
        // Providing a workaround to allow io-threads to be set to undefined for EAP 7.1 and 7.2
        // task-max-threads is also affected. stack-size and task-keepalive are not affected.

        final String IO_THREAD_ATTRIBUTE = "io-threads";
        final String TASK_MAX_THREADS_ATTRIBUTE = "task-max-threads";

        final String ioThreadsValue = report.getConfiguration().getSimpleValue(IO_THREAD_ATTRIBUTE);
        final String taskMaxThreadsValue = report.getConfiguration().getSimpleValue(TASK_MAX_THREADS_ATTRIBUTE);

        final ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition().copy();
        Map<String, PropertyDefinition> definitions = configDef.getPropertyDefinitions();

        // If any of these values is null, we need to check if they are null inside, els
        if (ioThreadsValue == null || taskMaxThreadsValue == null) {
            try {
                // Do a read-resource, read-attribute gives us the default values, even if we use "include-default=false"
                final Operation readResourceOperation = new Operation("read-resource", address);
                final Result readResourceOperationResult = getASConnection().execute(readResourceOperation);
                if (!readResourceOperationResult.isSuccess()) {
                    getLog().error("Unable to read resource description, fail the operation BZ-1655950");
                    report.setStatus(FAILURE);
                    report.setErrorMessage(readResourceOperationResult.getFailureDescription());
                }
                Map<String, Object> resourceData = (Map<String, Object>) readResourceOperationResult.getResult();
                String writeDummyValueForAttribute = null;
                if (ioThreadsValue == null) {
                    if (resourceData.get(IO_THREAD_ATTRIBUTE) == null) {
                        definitions.remove(IO_THREAD_ATTRIBUTE);
                    } else {
                        writeDummyValueForAttribute = IO_THREAD_ATTRIBUTE;
                    }
                }
                if (writeDummyValueForAttribute == null && taskMaxThreadsValue == null) {
                    if (resourceData.get(TASK_MAX_THREADS_ATTRIBUTE) == null) {
                        definitions.remove(TASK_MAX_THREADS_ATTRIBUTE);
                    } else {
                        writeDummyValueForAttribute = TASK_MAX_THREADS_ATTRIBUTE;
                    }
                }

                if (writeDummyValueForAttribute != null) {
                    // Force reload-required by setting the attribute to a dummy value
                    // It accepts undefined values in this mode.
                    final Operation operation = new Operation("write-attribute", address);
                    operation.addAdditionalProperty("name", writeDummyValueForAttribute);
                    operation.addAdditionalProperty("value", "1");
                    final Result result = getASConnection().execute(operation);
                    if (!result.isSuccess()) {
                        report.setStatus(FAILURE);
                        report.setErrorMessage(result.getFailureDescription());
                        return;
                    }
                }
            } catch (Exception ex) {
                getLog().error("Unable to read value to know how to proceed BZ-1655950", ex);
                report.setStatus(FAILURE);
                report.setErrorMessage(ex.getMessage());
                return;
            }
        }


        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }
}
