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

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 *
 * The special case is:
 *   Adds support to the creation of servlet-container children settings (jsp and websockets)
 *
 *
 * @author Josejulio Martínez
 */
public class UndertowComponent extends BaseComponent<ResourceComponent<?>> {

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        String path = report.getPluginConfiguration().getSimpleValue("path", "");
        if (path.equals("servlet-container")) {
            report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);

            ASConnection connection = getASConnection();
            ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition();
            CreateResourceDelegate createResourceDelegate = new CreateResourceDelegate(configDef, connection, address);

            Address createAddressServlet = createResourceDelegate.getCreateAddress(report);
            Address createAddressSettingJSP = new Address(createAddressServlet);
            createAddressSettingJSP.add("setting", "jsp");
            Address createAddressSettingWebSockets = new Address(createAddressServlet);
            createAddressSettingWebSockets.add("setting", "websockets");

            CompositeOperation op = new CompositeOperation();
            op.addStep(createResourceDelegate.getOperation(report, createAddressServlet));
            op.addStep(createResourceDelegate.getOperation(report, createAddressSettingJSP));
            op.addStep(createResourceDelegate.getOperation(report, createAddressSettingWebSockets));

            Result result = connection.execute(op);
            if (result.isSuccess()) {
                report.setStatus(CreateResourceStatus.SUCCESS);
                report.setResourceKey(createAddressServlet.getPath());
                report.setResourceName(report.getUserSpecifiedResourceName());
            } else {
                report.setStatus(CreateResourceStatus.FAILURE);
                report.setErrorMessage(result.getFailureDescription());
            }

            return report;
        } else {
            return super.createResource(report);
        }
    }

}
