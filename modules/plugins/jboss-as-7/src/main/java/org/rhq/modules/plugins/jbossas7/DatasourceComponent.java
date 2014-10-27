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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.FAILURE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.NOCHANGE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.SUCCESS;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.json.WriteAttribute;

/**
 * Handle Datasources (possibly jdbc-driver) related stuff
 * @author Heiko W. Rupp
 */
public class DatasourceComponent extends BaseComponent<DatasourcesComponent> {
    private static final String ALLOW_MULTIPLE_USERS_ATTRIBUTE = "allow-multiple-users";
    private static final String TRACK_STATEMENTS_ATTRIBUTE = "track-statements";
    private static final String MAX_POOL_SIZE_ATTRIBUTE = "max-pool-size";
    private static final String MIN_POOL_SIZE_ATTRIBUTE = "min-pool-size";
    /**
     * list of metrics where we can expect expresion value instead of just number (such expression has to be resolved)
     */
    private static final Set<String> EXPRESSION_METRICS = Collections.unmodifiableSet(new HashSet<String>(Arrays
        .asList(MAX_POOL_SIZE_ATTRIBUTE, MIN_POOL_SIZE_ATTRIBUTE)));
    private static final Set<String> UNSET_FORBIDDEN_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList(MAX_POOL_SIZE_ATTRIBUTE, MIN_POOL_SIZE_ATTRIBUTE, "pool-prefill", "pool-use-strict-min",
            "blocking-timeout-wait-millis", "idle-timeout-minutes", "background-validation-millis",
            "background-validation")));

    static final String ENABLED_ATTRIBUTE = "enabled";
    static final String ENABLE_OPERATION = "enable";
    static final String DISABLE_OPERATION = "disable";
    static final String CONNECTION_PROPERTIES_ATTRIBUTE = "*1";
    static final String XA_DATASOURCE_PROPERTIES_ATTRIBUTE = "*2";

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals(ENABLE_OPERATION)) {
            Operation operation = new Operation(ENABLE_OPERATION, getAddress());
            Boolean persistent = Boolean.valueOf(parameters.getSimpleValue("persistent", TRUE.toString()));
            operation.addAdditionalProperty("persistent", persistent);
            Result res = getASConnection().execute(operation);
            if (res.isSuccess()) {
                return new OperationResult();
            } else {
                OperationResult operationResult = new OperationResult();
                operationResult.setErrorMessage(res.getFailureDescription());
                return operationResult;
            }
        }
        if (name.equals(DISABLE_OPERATION)) {
            Operation operation = new Operation(DISABLE_OPERATION, getAddress());
            boolean allowResourceServiceRestart = Boolean.parseBoolean(parameters.getSimpleValue(
                "allow-resource-service-restart", FALSE.toString()));
            if (allowResourceServiceRestart) {
                operation.allowResourceServiceRestart();
            }
            Result res = getASConnection().execute(operation);
            if (res.isSuccess()) {
                return new OperationResult();
            } else {
                OperationResult operationResult = new OperationResult();
                operationResult.setErrorMessage(res.getFailureDescription());
                return operationResult;
            }
        }
        return super.invokeOperation(name, parameters);
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

        Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request : requests) {
            if (request.getName().equals("connectionAvailable")) {
                report.addData(getConnectionAvailable(request));
            } else if (request.getName().equals(MAX_POOL_SIZE_ATTRIBUTE)) {
                getRCAsMetric(report, request);
            } else if (request.getName().equals(MIN_POOL_SIZE_ATTRIBUTE)) {
                getRCAsMetric(report, request);
            } else {
                metrics.add(request);
            }
        }

        /*
         * Remainder here are metrics that can be read from the resource.
         * Those
         */
        ReadResource op = new ReadResource(address);
        op.includeRuntime(true);
        op.recursive(true);
        ComplexResult res = getASConnection().executeComplex(op);
        if (!res.isSuccess())
            return;

        Map<String, Object> results = new HashMap<String, Object>();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> statistics = (Map<String, Map<String, Object>>) res.getResult().get(
            "statistics");
        if (statistics != null) {
            results.putAll(statistics.get("pool"));
            results.putAll(statistics.get("jdbc"));

            for (MeasurementScheduleRequest metric : metrics) {
                String name = metric.getName();

                Object o = results.get(name);
                if (o != null) {
                    String tmp = (String) o;
                    Double val = Double.valueOf(tmp);
                    MeasurementDataNumeric data = new MeasurementDataNumeric(metric, val);
                    report.addData(data);
                }
            }
        }
    }

    @Override
    public void updateResourceConfiguration(final ConfigurationUpdateReport configurationUpdateReport) {
        ResourceType resourceType = context.getResourceType();
        ConfigurationDefinition configDef = resourceType.getResourceConfigurationDefinition();
        Configuration newConfig = configurationUpdateReport.getConfiguration();

        // remove special property being possibly sent from server in case EAP requires reload/restart
        newConfig.remove("__OOB");

        // 1. First of all, read the current configuration
        ConfigurationLoadDelegate readDelegate = new ConfigurationLoadDelegate(configDef, getASConnection(), address,
            includeRuntime);
        Configuration currentConfig;
        try {
            currentConfig = readDelegate.loadResourceConfiguration();
        } catch (Exception e) {
            getLog().error("Could not read current configuration before update", e);
            configurationUpdateReport.setStatus(FAILURE);
            configurationUpdateReport.setErrorMessage("Could not read current configuration before update: "
                + ThrowableUtil.getRootMessage(e));
            return;
        }

        // 2. We will wrap all property-simple and connection properties changes in a composite operation
        CompositeOperation updateOperation = new CompositeOperation();

        // 3. Capture property-simple changes
        Map<String, PropertySimple> newConfigSimpleProperties = newConfig.getSimpleProperties();
        Map<String, PropertySimple> currentConfigSimpleProperties = currentConfig.getSimpleProperties();

        Set<String> allSimplePropertyNames = new HashSet<String>(newConfigSimpleProperties.size()
            + currentConfigSimpleProperties.size());
        allSimplePropertyNames.addAll(newConfigSimpleProperties.keySet());
        allSimplePropertyNames.addAll(currentConfigSimpleProperties.keySet());
        // Read-only
        allSimplePropertyNames.remove(ENABLED_ATTRIBUTE);
        if (getServerComponent().getServerPluginConfiguration().getProductType() == JBossProductType.AS) {
            // Only supported on EAP
            allSimplePropertyNames.remove(ALLOW_MULTIPLE_USERS_ATTRIBUTE);
            allSimplePropertyNames.remove(TRACK_STATEMENTS_ATTRIBUTE);
        }

        for (String simplePropertyName : allSimplePropertyNames) {
            PropertySimple newConfigPropertySimple = newConfigSimpleProperties.get(simplePropertyName);
            String newConfigPropertySimpleValue = newConfigPropertySimple == null ? null : newConfigPropertySimple
                .getStringValue();
            PropertySimple currentConfigPropertySimple = currentConfigSimpleProperties.get(simplePropertyName);
            String currentConfigPropertySimpleValue = currentConfigPropertySimple == null ? null
                : currentConfigPropertySimple.getStringValue();
            boolean canUnset = !UNSET_FORBIDDEN_ATTRIBUTES.contains(simplePropertyName);

            if (newConfigPropertySimpleValue == null) {
                if (currentConfigPropertySimpleValue != null) {
                    String val;
                    if (canUnset) {
                        val = null;
                    } else {
                        val = configDef.getPropertyDefinitionSimple(simplePropertyName).getDefaultValue();
                    }
                    updateOperation.addStep(new WriteAttribute(getAddress(), simplePropertyName, val));
                }
            } else if (!newConfigPropertySimpleValue.equals(currentConfigPropertySimpleValue)) {
                updateOperation.addStep(new WriteAttribute(getAddress(), simplePropertyName,
                    newConfigPropertySimpleValue));
            }
        }

        // 4. Capture connection property changes
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

        Map<String, String> newConfigConnectionProperties = getConnectionPropertiesAsMap(
            newConfig.getList(connPropPluginConfigPropertyName), keyName);
        Map<String, String> currentConfigConnectionProperties = getConnectionPropertiesAsMap(
            currentConfig.getList(connPropPluginConfigPropertyName), keyName);
        Set<String> allConnectionPropertyNames = new HashSet<String>(newConfigConnectionProperties.size()
            + currentConfigConnectionProperties.size());
        allConnectionPropertyNames.addAll(newConfigConnectionProperties.keySet());
        allConnectionPropertyNames.addAll(currentConfigConnectionProperties.keySet());

        for (String connectionPropertyName : allConnectionPropertyNames) {
            Address propertyAddress = new Address(getAddress());
            propertyAddress.add(connPropAttributeNameOnServer, connectionPropertyName);

            String newConfigConnectionPropertyValue = newConfigConnectionProperties.get(connectionPropertyName);
            String currentConfigConnectionPropertyValue = currentConfigConnectionProperties.get(connectionPropertyName);

            if (newConfigConnectionPropertyValue == null) {
                updateOperation.addStep(new Operation("remove", propertyAddress));
            } else if (currentConfigConnectionPropertyValue == null) {
                Operation addOperation = new Operation("add", propertyAddress);
                addOperation.addAdditionalProperty("value", newConfigConnectionPropertyValue);
                updateOperation.addStep(addOperation);
            } else if (!newConfigConnectionPropertyValue.equals(currentConfigConnectionPropertyValue)) {
                updateOperation.addStep(new Operation("remove", propertyAddress));
                Operation addOperation = new Operation("add", propertyAddress);
                addOperation.addAdditionalProperty("value", newConfigConnectionPropertyValue);
                updateOperation.addStep(addOperation);
            }
        }

        // 5. Update config if needed
        if (updateOperation.numberOfSteps() > 0) {
            Result res = getASConnection().execute(updateOperation);
            if (res.isSuccess()) {
                configurationUpdateReport.setStatus(SUCCESS);
            } else {
                configurationUpdateReport.setStatus(FAILURE);
                configurationUpdateReport.setErrorMessage(res.getFailureDescription());
            }
        } else {
            configurationUpdateReport.setStatus(NOCHANGE);
        }
    }

    private void getRCAsMetric(MeasurementReport report, MeasurementScheduleRequest request) {
        ReadMetricResult result = getMetricValue(report, request, EXPRESSION_METRICS);

        if (result.equals(ReadMetricResult.Null)) { // server
            Double val = Double.valueOf(-1);
            if (request.getName().equals(MAX_POOL_SIZE_ATTRIBUTE))
                val = Double.valueOf(20); // The default value
            else if (request.getName().equals(MIN_POOL_SIZE_ATTRIBUTE))
                val = Double.valueOf(0); // The default value

            MeasurementDataNumeric data = new MeasurementDataNumeric(request, val);
            report.addData(data);
        }
    }

    private MeasurementDataTrait getConnectionAvailable(MeasurementScheduleRequest request) {
        Result res = getASConnection().execute(new Operation("test-connection-in-pool", getAddress()));
        return new MeasurementDataTrait(request, String.valueOf(res.isSuccess()));
    }

    static boolean isXADatasourceResource(ResourceType resourceType) {
        return resourceType.getName().toLowerCase().contains("xa");
    }

    static Map<String, String> getConnectionPropertiesAsMap(PropertyList listPropertyWrapper, String keyName) {
        if (listPropertyWrapper == null) {
            return Collections.emptyMap();
        }
        List<Property> propertyList = listPropertyWrapper.getList();
        if (propertyList.size() == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<String, String>(propertyList.size());
        for (Property p : propertyList) {
            PropertyMap map = (PropertyMap) p;
            String key = map.getSimpleValue(keyName, null);
            String value = map.getSimpleValue("value", null);
            if (key == null || value == null) {
                continue;
            }
            result.put(key, value);
        }
        return result;
    }
}
