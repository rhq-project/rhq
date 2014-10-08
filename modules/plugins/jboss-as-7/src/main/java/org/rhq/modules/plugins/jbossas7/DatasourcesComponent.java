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

package org.rhq.modules.plugins.jbossas7;

import static java.lang.Boolean.TRUE;
import static org.rhq.core.domain.resource.CreateResourceStatus.FAILURE;
import static org.rhq.core.domain.resource.CreateResourceStatus.INVALID_ARTIFACT;
import static org.rhq.core.domain.resource.CreateResourceStatus.SUCCESS;
import static org.rhq.modules.plugins.jbossas7.DatasourceComponent.CONNECTION_PROPERTIES_ATTRIBUTE;
import static org.rhq.modules.plugins.jbossas7.DatasourceComponent.ENABLED_ATTRIBUTE;
import static org.rhq.modules.plugins.jbossas7.DatasourceComponent.XA_DATASOURCE_PROPERTIES_ATTRIBUTE;
import static org.rhq.modules.plugins.jbossas7.DatasourceComponent.getConnectionPropertiesAsMap;
import static org.rhq.modules.plugins.jbossas7.DatasourceComponent.isXADatasourceResource;

import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * A component for Datasources resources (parent type of Datasource and XA Datasource resources).
 *
 * @author Thomas Segismont
 */
public class DatasourcesComponent extends BaseComponent<BaseComponent<?>> {

    @Override
    public CreateResourceReport createResource(CreateResourceReport createResourceReport) {
        ResourceType resourceType = createResourceReport.getResourceType();

        List<String> validationErrors = ConfigurationUtility.validateConfiguration(
            createResourceReport.getResourceConfiguration(), resourceType.getResourceConfigurationDefinition());

        if (!validationErrors.isEmpty()) {
            createResourceReport.setErrorMessage(validationErrors.toString());
            createResourceReport.setStatus(FAILURE);
            return createResourceReport;
        }

        final CreateResourceReport resourceReport = super.createResource(createResourceReport);

        // No success -> no point in continuing
        if (resourceReport.getStatus() != SUCCESS) {
            return resourceReport;
        }

        // outer create resource did not cater for the connection or xa properties, so lets add them now
        String connPropAttributeNameOnServer, connPropPluginConfigPropertyName, keyName;
        if (isXADatasourceResource(resourceType)) {
            connPropAttributeNameOnServer = "xa-datasource-properties";
            connPropPluginConfigPropertyName = XA_DATASOURCE_PROPERTIES_ATTRIBUTE;
            keyName = "key";
        } else {
            connPropAttributeNameOnServer = "connection-properties";
            connPropPluginConfigPropertyName = CONNECTION_PROPERTIES_ATTRIBUTE;
            keyName = "pname";
        }
        PropertyList listPropertyWrapper = createResourceReport.getResourceConfiguration().getList(
            connPropPluginConfigPropertyName);
        Map<String, String> connectionPropertiesAsMap = getConnectionPropertiesAsMap(listPropertyWrapper, keyName);
        // if no conn or xa props supplied in the create resource request, skip and continue
        if (!connectionPropertiesAsMap.isEmpty()) {
            CompositeOperation cop = new CompositeOperation();
            for (Map.Entry<String, String> connectionProperty : connectionPropertiesAsMap.entrySet()) {
                Address propertyAddress = new Address(resourceReport.getResourceKey());
                propertyAddress.add(connPropAttributeNameOnServer, connectionProperty.getKey());
                Operation op = new Operation("add", propertyAddress);
                op.addAdditionalProperty("value", connectionProperty.getValue());
                cop.addStep(op);
            }
            Result res = getASConnection().execute(cop);
            if (!res.isSuccess()) {
                resourceReport.setErrorMessage("Datasource was added, but setting " + connPropAttributeNameOnServer
                    + " failed: " + res.getFailureDescription());
                resourceReport.setStatus(INVALID_ARTIFACT);
                return resourceReport;
            }
        }

        // Handle the 'enabled' property (it's a read-only attribute in management interface)
        // See https://bugzilla.redhat.com/show_bug.cgi?id=854773

        // What did the user say in the datasource creation form?
        PropertySimple enabledProperty = createResourceReport.getResourceConfiguration().getSimple(ENABLED_ATTRIBUTE);
        // Let's assume the user wants the datasource enabled if he/she said nothing
        Boolean enabledPropertyValue = enabledProperty == null ? TRUE : enabledProperty.getBooleanValue();

        DatasourceEnabledAttributeHelper
            .on(new Address(resourceReport.getResourceKey()))
            .with(getASConnection())
            .setAttributeValue(enabledPropertyValue,
                new DatasourceEnabledAttributeHelper.EnabledAttributeHelperCallbacks() {
                    @Override
                    public void onReadAttributeFailure(Result opResult) {
                        resourceReport.setStatus(INVALID_ARTIFACT);
                        resourceReport.setErrorMessage("Datasource was added, "
                            + "but could not read its configuration after creation: "
                            + opResult.getFailureDescription());
                    }

                    @Override
                    public void onEnableOperationFailure(Result opResult) {
                        resourceReport.setStatus(INVALID_ARTIFACT);
                        resourceReport.setErrorMessage("Datasource was added but not enabled: "
                            + opResult.getFailureDescription());
                    }

                    @Override
                    public void onDisableOperationFailure(Result opResult) {
                        resourceReport.setStatus(INVALID_ARTIFACT);
                        resourceReport.setErrorMessage("Datasource was added but not disabled: "
                            + opResult.getFailureDescription());
                    }
                });

        return resourceReport;
    }
}
