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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Handle Datasorces (possibly jdbc-driver) related stuff
 * @author Heiko W. Rupp
 */
public class DatasourceComponent extends BaseComponent<BaseComponent<?>> implements OperationFacet, ConfigurationFacet,
    CreateChildResourceFacet {

    private static final Log LOG = LogFactory.getLog(DatasourceComponent.class);

    private static final String NOTSET = "-notset-";
    private static final String ENABLED_ATTRIBUTE = "enabled";
    private static final String ENABLE_OPERATION = "enable";
    private static final String DISABLE_OPERATION = "disable";
    private static final String ALLOW_MULTIPLE_USERS_ATTRIBUTE = "allow-multiple-users";
    private static final String TRACK_STATEMENTS_ATTRIBUTE = "track-statements";
    /**
     * list of metrics where we can expect expresion value instead of just number (such expression has to be resolved)
     */
    private static final Set<String> expressionMetrics = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("max-pool-size","min-pool-size")));

    @Override
    public OperationResult invokeOperation(String operationName, Configuration parameters) throws Exception { // TODO still needed ? Check with plugin descriptor

        OperationResult operationResult = new OperationResult();
        ASConnection connection = getASConnection();
        Operation op;

        if (operationName.equals("addDriver")) { // TODO decide if we need this at all. See also the plugin-descriptor
            String drivername = parameters.getSimpleValue("driver-name", NOTSET);

            Address theAddress = new Address(address);
            theAddress.add("jdbc-driver", drivername);

            op = new Operation("add", theAddress);
            op.addAdditionalProperty("driver-name", drivername);
            op.addAdditionalProperty("deployment-name", parameters.getSimpleValue("deployment-name", NOTSET));
            op.addAdditionalProperty("driver-class-name", parameters.getSimpleValue("driver-class-name", NOTSET));
        } else {
            /*
             * This is a catch all for operations that are not explicitly treated above.
             */
            op = new Operation(operationName, address);
        }

        Result res = connection.execute(op);
        if (res.isSuccess()) {
            operationResult.setSimpleResult("Success");
        } else {
            operationResult.setErrorMessage(res.getFailureDescription());
        }

        return operationResult;
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport createResourceReport) {

        List<String> validationErrors = ConfigurationUtility.validateConfiguration(createResourceReport
            .getResourceConfiguration(), createResourceReport.getResourceType().getResourceConfigurationDefinition());

        if (!validationErrors.isEmpty()) {
            createResourceReport.setErrorMessage(validationErrors.toString());
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            return createResourceReport;
        }

        final CreateResourceReport resourceReport = super.createResource(createResourceReport);

        // No success -> no point in continuing
        if (resourceReport.getStatus() != CreateResourceStatus.SUCCESS)
            return resourceReport;

        // outer create resource did not cater for the connection or xa properties, so lets add them now
        String connPropAttributeNameOnAS7, connPropPluginConfigPropertyName, keyName;
        if (createResourceReport.getResourceType().getName().toLowerCase().contains("xa")) {
            connPropAttributeNameOnAS7 = "xa-datasource-properties";
            connPropPluginConfigPropertyName = "*2";
            keyName = "key";
        } else {
            connPropAttributeNameOnAS7 = "connection-properties";
            connPropPluginConfigPropertyName = "*1";
            keyName = "pname";
        }
        PropertyList listPropertyWrapper = createResourceReport.getResourceConfiguration().getList(
            connPropPluginConfigPropertyName);
        CompositeOperation cop = new CompositeOperation();
        for (Property p : listPropertyWrapper.getList()) {
            PropertyMap map = (PropertyMap) p;
            String key = map.getSimpleValue(keyName, null);
            String value = map.getSimpleValue("value", null);
            if (key == null || value == null) {
                continue;
            }
            Address propertyAddress = new Address(resourceReport.getResourceKey());
            propertyAddress.add(connPropAttributeNameOnAS7, key);
            Operation op = new Operation("add", propertyAddress);
            op.addAdditionalProperty("value", value);
            cop.addStep(op);
        }
        if (cop.numberOfSteps() > 0) {
            Result res = getASConnection().execute(cop);
            if (!res.isSuccess()) {
                resourceReport.setErrorMessage("Datasource was added, but setting " + connPropAttributeNameOnAS7
                    + " failed: " + res.getFailureDescription());
                resourceReport.setStatus(CreateResourceStatus.INVALID_ARTIFACT);
                return resourceReport;
            }
        }

        // Handle the 'enabled' property (it's a read-only attribute in management interface)
        // See https://bugzilla.redhat.com/show_bug.cgi?id=854773

        // What did the user say in the datasource creation form?
        PropertySimple enabledProperty = createResourceReport.getResourceConfiguration().getSimple(ENABLED_ATTRIBUTE);
        // Let's assume the user wants the datasource enabled if he/she said nothing
        Boolean enabledPropertyValue = enabledProperty == null ? TRUE : enabledProperty.getBooleanValue();

        EnabledAttributeHelper.on(new Address(resourceReport.getResourceKey())).with(getASConnection())
            .setAttributeValue(enabledPropertyValue, new EnabledAttributeHelperCallbacks() {
                @Override
                public void onReadAttributeFailure(Result opResult) {
                    resourceReport.setStatus(CreateResourceStatus.INVALID_ARTIFACT);
                    resourceReport.setErrorMessage("Datasource was added, "
                        + "but could not read its configuration after creation: " + opResult.getFailureDescription());
                }

                @Override
                public void onEnableOperationFailure(Result opResult) {
                    resourceReport.setStatus(CreateResourceStatus.INVALID_ARTIFACT);
                    resourceReport.setErrorMessage("Datasource was added but not enabled: "
                        + opResult.getFailureDescription());
                }

                @Override
                public void onDisableOperationFailure(Result opResult) {
                    resourceReport.setStatus(CreateResourceStatus.INVALID_ARTIFACT);
                    resourceReport.setErrorMessage("Datasource was added but not disabled: "
                        + opResult.getFailureDescription());
                }
            });

        return resourceReport;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

        Set<MeasurementScheduleRequest> metrics = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest request : requests) {
            if (request.getName().equals("connectionAvailable")) {
                report.addData(getConnectionAvailable(request));
            } else if (request.getName().equals("max-pool-size")) {
                getRCAsMetric(report, request);
            } else if (request.getName().equals("min-pool-size")) {
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
        Map<String, Object> statistics = (Map<String, Object>) res.getResult().get("statistics");
        if (statistics != null) {
            results.putAll((Map<? extends String, ? extends Object>) statistics.get("pool"));
            results.putAll((Map<? extends String, ? extends Object>) statistics.get("jdbc"));

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
        Configuration config = configurationUpdateReport.getConfiguration();
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();

        replacePropertiesWithDefaultValueWhenNull(config, configDef);

        // Make a copy of the config definition and make it look like there was no 'enabled' attribute, which is
        // manually managed by this component.
        ConfigurationDefinition configDefCopy = copyConfigurationDefinition(configDef);
        configDefCopy.getPropertyDefinitions().remove(ENABLED_ATTRIBUTE);

        if (getServerComponent().getServerPluginConfiguration().getProductType() == JBossProductType.AS) {
            // AS7 management model has no 'enabled' attribute so we do not take care of it
            doUpdateAS7ResourceConfiguration(configurationUpdateReport, configDefCopy);
            return;
        }

        Boolean enabledAttributeConfigValue = config.getSimple(ENABLED_ATTRIBUTE).getBooleanValue();
        if (enabledAttributeConfigValue == null) {
            // True if unset
            enabledAttributeConfigValue = TRUE;
        }

        // Check if the datasource is currently enabled
        ReadAttribute readEnabledAttributeOperation = new ReadAttribute(address, ENABLED_ATTRIBUTE);
        Result readEnabledAttributeResult = getASConnection().execute(readEnabledAttributeOperation);
        if (!readEnabledAttributeResult.isSuccess()) {
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
            configurationUpdateReport.setErrorMessage("Could not determine if the Datasource is currently enabled: "
                + readEnabledAttributeResult.getFailureDescription());
            return;
        }
        Boolean datasourceEnabled = (Boolean) readEnabledAttributeResult.getResult();
        if (datasourceEnabled == TRUE) {
            if (enabledAttributeConfigValue == TRUE) {
                configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                configurationUpdateReport.setErrorMessage("You must disable the Datasource "
                    + "before editing its configuration");
                return;
            } else {
                EnabledAttributeHelper.on(getAddress()).with(getASConnection())
                    .setAttributeValue(FALSE, new EnabledAttributeHelperCallbacks() {
                        @Override
                        public void onReadAttributeFailure(Result opResult) {
                            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                            configurationUpdateReport
                                .setErrorMessage("Could not determine if the Datasource is currently enabled: "
                                    + opResult.getFailureDescription());
                        }

                        @Override
                        public void onEnableOperationFailure(Result opResult) {
                            // Will not be called
                        }

                        @Override
                        public void onDisableOperationFailure(Result opResult) {
                            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                            configurationUpdateReport.setErrorMessage("Could not disable the Datasource: "
                                + opResult.getFailureDescription());
                        }
                    });
                if (configurationUpdateReport.getStatus() == ConfigurationUpdateStatus.FAILURE) {
                    // No success, return immediatly
                    return;
                }
            }
        }

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDefCopy, getASConnection(), address);
        delegate.updateResourceConfiguration(configurationUpdateReport);

        if (configurationUpdateReport.getStatus() != ConfigurationUpdateStatus.SUCCESS) {
            // No success, return immediatly
            return;
        }

        EnabledAttributeHelper.on(address).with(getASConnection())
            .setAttributeValue(enabledAttributeConfigValue, new EnabledAttributeHelperCallbacks() {
                @Override
                public void onReadAttributeFailure(Result opResult) {
                    configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                    configurationUpdateReport.setErrorMessage("Datasource was updated, "
                        + "but could not read its configuration after the update: " + opResult.getFailureDescription());
                }

                @Override
                public void onEnableOperationFailure(Result opResult) {
                    configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                    configurationUpdateReport.setErrorMessage("Datasource was updated but not enabled: "
                        + opResult.getFailureDescription());
                }

                @Override
                public void onDisableOperationFailure(Result opResult) {
                    configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                    configurationUpdateReport.setErrorMessage("Datasource was updated but not disabled: "
                        + opResult.getFailureDescription());
                }
            });
    }

    private void replacePropertiesWithDefaultValueWhenNull(Configuration config, ConfigurationDefinition configDef) {
        // These properties cannot be undefined once set.
        // Also the AS7 server does not accept null values even if the properties are still unset.
        replaceWithDefaultIfNull("max-pool-size", config, configDef);
        replaceWithDefaultIfNull("min-pool-size", config, configDef);
        replaceWithDefaultIfNull("pool-prefill", config, configDef);
        replaceWithDefaultIfNull("pool-use-strict-min", config, configDef);
        replaceWithDefaultIfNull("blocking-timeout-wait-millis", config, configDef);
        replaceWithDefaultIfNull("idle-timeout-minutes", config, configDef);
        replaceWithDefaultIfNull("background-validation-millis", config, configDef);
        replaceWithDefaultIfNull("background-validation-minutes", config, configDef);
        replaceWithDefaultIfNull("background-validation", config, configDef);
    }

    /**
     * Replace the value configured by the user with the default value from the resource descriptor if
     * the value to be sent to the server is null or empty.
     *
     * @param propertyName property name
     * @param config configuration update
     * @param configDef configuration definition
     */
    private void replaceWithDefaultIfNull(String propertyName, Configuration config, ConfigurationDefinition configDef) {
        PropertyDefinitionSimple propertyDefinition = configDef.getPropertyDefinitionSimple(propertyName);

        if (propertyDefinition != null) {
            String propertyValue = config.getSimpleValue(propertyName);
            if (propertyValue == null || propertyValue.isEmpty()) {
                config.put(new PropertySimple(propertyName, propertyDefinition.getDefaultValue()));
            }
        }
    }

    private void doUpdateAS7ResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport,
        ConfigurationDefinition configDefCopy) {
        // Remove these properties which are only supported on EAP.
        configDefCopy.getPropertyDefinitions().remove(ALLOW_MULTIPLE_USERS_ATTRIBUTE);
        configDefCopy.getPropertyDefinitions().remove(TRACK_STATEMENTS_ATTRIBUTE);
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDefCopy, getASConnection(), address);
        delegate.updateResourceConfiguration(configurationUpdateReport);
    }

    private void getRCAsMetric(MeasurementReport report, MeasurementScheduleRequest request) {
        ReadMetricResult result = getMetricValue(report, request, expressionMetrics);

        if (result.equals(ReadMetricResult.Null)) { // server
            Double val = Double.valueOf(-1);
            if (request.getName().equals("max-pool-size"))
                val = Double.valueOf(20); // The default value
            else if (request.getName().equals("min-pool-size"))
                val = Double.valueOf(0); // The default value

            MeasurementDataNumeric data = new MeasurementDataNumeric(request, val);
            report.addData(data);
        }
    }

    private MeasurementDataTrait getConnectionAvailable(MeasurementScheduleRequest request) {
        Operation op = new Operation("test-connection-in-pool", getAddress());
        Result res = getASConnection().execute(op);

        MeasurementDataTrait trait = new MeasurementDataTrait(request, String.valueOf(res.isSuccess()));

        return trait;
    }

    private ConfigurationDefinition copyConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        ConfigurationDefinition configDefCopy = new ConfigurationDefinition(configurationDefinition.getName(),
            configurationDefinition.getDescription());
        configDefCopy.setConfigurationFormat(configurationDefinition.getConfigurationFormat());
        configDefCopy.setPropertyDefinitions(new HashMap<String, PropertyDefinition>(configurationDefinition
            .getPropertyDefinitions()));
        return configDefCopy;
    }

    // 'enabled' attribute is read/write in the plugin descriptor, but not in EAP management interface.
    // This helper queries the current attribute value and, as needed, will invoke whether the 'enable' or 'disable'
    // operation.
    private static class EnabledAttributeHelper {

        Address datasourceAddress;
        ASConnection asConnection;

        EnabledAttributeHelper(Address datasourceAddress) {
            this.datasourceAddress = datasourceAddress;
        }

        static EnabledAttributeHelper on(Address datasourceAddress) {
            return new EnabledAttributeHelper(datasourceAddress);
        }

        EnabledAttributeHelper with(ASConnection asConnection) {
            this.asConnection = asConnection;
            return this;
        }

        void setAttributeValue(Boolean attributeValue, EnabledAttributeHelperCallbacks callbacks) {
            if (asConnection == null) {
                throw new IllegalStateException("No ASConnection instance provided");
            }
            if (attributeValue == null) {
                throw new IllegalArgumentException("Argument attributeValue is null");
            }
            ReadAttribute readAttribute = new ReadAttribute(datasourceAddress, ENABLED_ATTRIBUTE);
            Result readAttributeResult = asConnection.execute(readAttribute);
            if (!readAttributeResult.isSuccess()) {
                if (callbacks != null) {
                    callbacks.onReadAttributeFailure(readAttributeResult);
                }
                return;
            }
            Boolean currentAttributeValue = (Boolean) readAttributeResult.getResult();
            if (currentAttributeValue != attributeValue) {
                if (attributeValue == TRUE) {
                    Operation operation = new Operation(ENABLE_OPERATION, datasourceAddress);
                    Result res = asConnection.execute(operation);
                    if (!res.isSuccess()) {
                        if (callbacks != null) {
                            callbacks.onEnableOperationFailure(res);
                        }
                        return;
                    }
                } else {
                    Operation operation = new Operation(DISABLE_OPERATION, datasourceAddress);
                    Result res = asConnection.execute(operation);
                    if (!res.isSuccess()) {
                        if (callbacks != null) {
                            callbacks.onDisableOperationFailure(res);
                        }
                        return;
                    }
                }
            }
        }

    }

    interface EnabledAttributeHelperCallbacks {
        void onReadAttributeFailure(Result opResult);

        void onEnableOperationFailure(Result opResult);

        void onDisableOperationFailure(Result opResult);
    }
}
