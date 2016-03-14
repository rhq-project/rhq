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

import static java.lang.Boolean.TRUE;
import static org.rhq.core.domain.resource.CreateResourceStatus.FAILURE;
import static org.rhq.core.domain.resource.CreateResourceStatus.INVALID_ARTIFACT;
import static org.rhq.core.domain.resource.CreateResourceStatus.SUCCESS;
import static org.rhq.modules.plugins.wildfly10.DatasourceComponent.CONNECTION_PROPERTIES_ATTRIBUTE;
import static org.rhq.modules.plugins.wildfly10.DatasourceComponent.DISABLE_OPERATION;
import static org.rhq.modules.plugins.wildfly10.DatasourceComponent.ENABLED_ATTRIBUTE;
import static org.rhq.modules.plugins.wildfly10.DatasourceComponent.ENABLE_OPERATION;
import static org.rhq.modules.plugins.wildfly10.DatasourceComponent.XA_DATASOURCE_PROPERTIES_ATTRIBUTE;
import static org.rhq.modules.plugins.wildfly10.DatasourceComponent.getConnectionPropertiesAsMap;
import static org.rhq.modules.plugins.wildfly10.DatasourceComponent.isXADatasourceResource;

import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * A component for Datasources resources (parent type of Datasource and XA Datasource resources).
 *
 * @author Thomas Segismont
 */
public class DatasourcesComponent extends BaseComponent<BaseComponent<?>> {

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        ResourceType resourceType = report.getResourceType();
        ConfigurationDefinition configDef = resourceType.getResourceConfigurationDefinition().copy();
        Configuration config = report.getResourceConfiguration().deepCopy(false);

        // Check if the users wants the datasource to be enabled
        PropertySimple enabledProperty = config.getSimple(ENABLED_ATTRIBUTE);
        // Let's assume the user wants the datasource enabled if the attribute is not present
        Boolean enableDatasource = enabledProperty == null ? TRUE : enabledProperty.getBooleanValue();

        // Remove this attribute which is manually managed
        configDef.getPropertyDefinitions().remove(ENABLED_ATTRIBUTE);
        config.remove(ENABLED_ATTRIBUTE);

        List<String> validationErrors = ConfigurationUtility.validateConfiguration(config, configDef);

        if (!validationErrors.isEmpty()) {
            report.setErrorMessage(validationErrors.toString());
            report.setStatus(FAILURE);
            return report;
        }

        CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, getASConnection(), getAddress());
        Address datasourceAddress = delegate.getCreateAddress(report);
        Operation baseOperation = delegate.getOperation(report, datasourceAddress);

        CompositeOperation cop = new CompositeOperation();
        cop.addStep(baseOperation);

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
        PropertyList listPropertyWrapper = config.getList(connPropPluginConfigPropertyName);
        Map<String, String> connectionPropertiesAsMap = getConnectionPropertiesAsMap(listPropertyWrapper, keyName);
        // if no conn or xa props supplied in the create resource request, skip and continue
        if (!connectionPropertiesAsMap.isEmpty()) {
            for (Map.Entry<String, String> connectionProperty : connectionPropertiesAsMap.entrySet()) {
                Address propertyAddress = new Address(datasourceAddress);
                propertyAddress.add(connPropAttributeNameOnServer, connectionProperty.getKey());
                Operation op = new Operation("add", propertyAddress);
                op.addAdditionalProperty("value", connectionProperty.getValue());
                cop.addStep(op);
            }
        }

        Result res = getASConnection().execute(cop);
        if (!res.isSuccess()) {
            report.setErrorMessage(res.getFailureDescription());
            report.setStatus(FAILURE);
            return report;
        }

        // Now enable/disable datasource as required

        ReadAttribute readEnabledAttribute = new ReadAttribute(datasourceAddress, ENABLED_ATTRIBUTE);
        Result readEnabledAttributeResult = getASConnection().execute(readEnabledAttribute);
        if (!readEnabledAttributeResult.isSuccess()) {
            report.setStatus(INVALID_ARTIFACT);
            report.setErrorMessage("Datasource was added but the agent could not read its configuration: "
                + readEnabledAttributeResult.getFailureDescription());
            return report;
        }
        Boolean enabledPropertyCurrentValue = (Boolean) readEnabledAttributeResult.getResult();
        if (enabledPropertyCurrentValue != enableDatasource) {
            if (enableDatasource == TRUE) {
                Operation operation = new Operation(ENABLE_OPERATION, datasourceAddress);
                Result result = getASConnection().execute(operation);
                if (!result.isSuccess()) {
                    report.setStatus(INVALID_ARTIFACT);
                    report.setErrorMessage("Datasource was added but not enabled: " + result.getFailureDescription());
                }
            } else {
                Operation operation = new Operation(DISABLE_OPERATION, datasourceAddress);
                Result result = getASConnection().execute(operation);
                if (!result.isSuccess()) {
                    report.setStatus(INVALID_ARTIFACT);
                    report.setErrorMessage("Datasource was added but not disabled: " + result.getFailureDescription());
                }
            }
        } else {
            report.setStatus(SUCCESS);
            report.setResourceKey(datasourceAddress.getPath());
            report.setResourceName(report.getUserSpecifiedResourceName());
        }

        return report;
    }
}
