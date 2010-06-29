/*
* Jopr Management Platform
* Copyright (C) 2005-2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.plugins.jbossas5;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.RunState;
import org.jboss.metatype.api.values.ArrayValue;
import org.jboss.metatype.api.values.CollectionValue;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.EnumValue;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DebugUtils;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;
import org.rhq.plugins.jbossas5.util.ResourceTypeUtils;

/**
 * Service ResourceComponent for all {@link ManagedComponent}s in a Profile.
 *
 * @author Ian Springer
 * @author Jason Dobies
 * @author Mark Spritzler
 */
public class ManagedComponentComponent extends AbstractManagedComponent implements ConfigurationFacet,
    DeleteResourceFacet, OperationFacet, MeasurementFacet {
    public static interface Config {
        String COMPONENT_TYPE = "componentType";
        String COMPONENT_SUBTYPE = "componentSubtype";
        String COMPONENT_NAME = "componentName";
        String TEMPLATE_NAME = "templateName";
        String COMPONENT_NAME_PROPERTY = "componentNameProperty";
    }

    protected static final char PREFIX_DELIMITER = '|';

    private final Log log = LogFactory.getLog(this.getClass());

    private String componentName;
    private ComponentType componentType;

    // ResourceComponent Implementation  --------------------------------------------

    public AvailabilityType getAvailability() {
        RunState runState = null;
        try {
            runState = getManagedComponent().getRunState();
        } catch (Throwable t) {
            log.debug("Could not get component state, cause: ", t);
            return AvailabilityType.DOWN;
        }

        if (runState == RunState.RUNNING) {
            return AvailabilityType.UP;
        } else {
            log.debug("Component was not running, state was: " + runState);
            return AvailabilityType.DOWN;
        }
    }

    public void start(ResourceContext<ProfileServiceComponent> resourceContext) throws Exception {
        super.start(resourceContext);
        this.componentType = ConversionUtils.getComponentType(getResourceContext().getResourceType());
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        this.componentName = pluginConfig.getSimple(Config.COMPONENT_NAME).getStringValue();
        log.trace("Started ResourceComponent for " + getResourceDescription() + ", managing " + this.componentType
            + " component '" + this.componentName + "'.");
    }

    public void stop() {
        return;
    }

    // ConfigurationComponent Implementation  --------------------------------------------

    public Configuration loadResourceConfiguration() {
        Configuration resourceConfig;
        try {
            Map<String, ManagedProperty> managedProperties = getManagedComponent().getProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(getResourceContext()
                .getPluginConfiguration());
            if (this.log.isDebugEnabled())
                this.log.debug("*** AFTER LOAD:\n" + DebugUtils.convertPropertiesToString(managedProperties));
            resourceConfig = ConversionUtils.convertManagedObjectToConfiguration(managedProperties, customProps,
                getResourceContext().getResourceType());
        } catch (Exception e) {
            RunState runState = getManagedComponent().getRunState();
            if (runState == RunState.RUNNING) {
                this.log.error("Failed to load configuration for " + getResourceDescription() + ".", e);
            } else {
                this.log.debug("Failed to load configuration for " + getResourceDescription()
                    + ", but managed component is not in the RUNNING state.", e);
            }
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        Configuration resourceConfig = configurationUpdateReport.getConfiguration();
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        try {
            ManagedComponent managedComponent = getManagedComponent();
            Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(pluginConfig);
            if (this.log.isDebugEnabled())
                this.log.debug("*** BEFORE UPDATE:\n" + DebugUtils.convertPropertiesToString(managedProperties));
            ConversionUtils.convertConfigurationToManagedProperties(managedProperties, resourceConfig,
                getResourceContext().getResourceType(), customProps);
            if (this.log.isDebugEnabled())
                this.log.debug("*** AFTER UPDATE:\n" + DebugUtils.convertPropertiesToString(managedProperties));
            updateComponent(managedComponent);
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            this.log.error("Failed to update configuration for " + getResourceDescription() + ".", e);
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
            configurationUpdateReport.setErrorMessage(ThrowableUtil.getAllMessages(e));
        }
    }

    // DeleteResourceFacet Implementation  --------------------------------------------

    public void deleteResource() throws Exception {
        DeploymentManager deploymentManager = getConnection().getDeploymentManager();
        if (!deploymentManager.isRedeploySupported())
            throw new UnsupportedOperationException("Deletion of " + getResourceContext().getResourceType().getName()
                + " Resources is not currently supported.");
        ManagedComponent managedComponent = getManagedComponent();
        log.debug("Removing " + getResourceDescription() + " with component " + toString(managedComponent) + "...");
        ManagementView managementView = getConnection().getManagementView();
        managementView.removeComponent(managedComponent);
        ManagedDeployment parentDeployment = managedComponent.getDeployment();
        log.debug("Redeploying parent deployment '" + parentDeployment.getName()
            + "' in order to complete removal of component " + toString(managedComponent) + "...");
        DeploymentProgress progress = deploymentManager.redeploy(parentDeployment.getName());
        DeploymentStatus redeployStatus = DeploymentUtils.run(progress);
        if (redeployStatus.isFailed()) {
            log.error("Failed to redeploy parent deployment '" + parentDeployment.getName()
                + "during removal of component " + toString(managedComponent)
                + " - removal may not persist when the app server is restarted.", redeployStatus.getFailure());
        }
        managementView.load();
    }

    // OperationFacet Implementation  --------------------------------------------

    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        OperationDefinition operationDefinition = getOperationDefinition(name);
        ManagedOperation managedOperation = getManagedOperation(operationDefinition);
        // Convert parameters into MetaValue array.
        MetaValue[] parameterMetaValues = ConversionUtils.convertOperationsParametersToMetaValues(managedOperation,
            parameters, operationDefinition);
        // invoke() takes a varargs, so we need to pass an empty array, rather than null.
        MetaValue resultMetaValue = managedOperation.invoke(parameterMetaValues);
        OperationResult result = new OperationResult();
        // Convert result MetaValue to corresponding Property type.
        ConversionUtils.convertManagedOperationResults(managedOperation, resultMetaValue, result.getComplexResults(),
            operationDefinition);
        return result;
    }

    // MeasurementFacet Implementation  --------------------------------------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        ManagedComponent managedComponent = getManagedComponent();
        RunState runState = managedComponent.getRunState();
        for (MeasurementScheduleRequest request : metrics) {
            try {
                if (request.getName().equals("runState")) {
                    report.addData(new MeasurementDataTrait(request, runState.name()));
                } else {
                    Object value = getSimpleValue(managedComponent, request);
                    addValueToMeasurementReport(report, request, value);
                }
            } catch (Exception e) {
                if (runState == RunState.RUNNING) {
                    log.error("Failed to collect metric for " + request, e);
                } else {
                    log.debug("Failed to collect metric for " + request
                        + ", but managed component is not in the RUNNING state.", e);
                }
            }
        }
    }

    protected void updateComponent(ManagedComponent managedComponent) throws Exception {
        log.trace("Updating " + getResourceDescription() + " with component " + toString(managedComponent) + "...");
        ManagementView managementView = getConnection().getManagementView();
        managementView.updateComponent(managedComponent);
        managementView.load();
    }

    // ------------------------------------------------------------------------------

    /**
     * The name of the measurement schedule request (i.e. the metric name) can be in one of two forms:
     * <p/>
     * [prefix'|']simplePropertyName (e.g. "maxTime" or "ThreadPool|currentThreadCount")
     * [prefix'|']compositePropertyName'.'key (e.g. "consumerCount" or "messageStatistics.count")
     *
     * @param managedComponent a managed component
     * @param request          a measurement schedule request
     * @return the metric value
     */
    @Nullable
    protected Object getSimpleValue(ManagedComponent managedComponent, MeasurementScheduleRequest request) {
        String metricName = request.getName();
        int pipeIndex = metricName.indexOf(PREFIX_DELIMITER);
        // Remove the prefix if there is one (e.g. "ThreadPool|currentThreadCount" -> "currentThreadCount").
        String compositePropName = (pipeIndex == -1) ? metricName : metricName.substring(pipeIndex + 1);
        int dotIndex = compositePropName.indexOf('.');
        String metricPropName = (dotIndex == -1) ? compositePropName : compositePropName.substring(0, dotIndex);
        ManagedProperty metricProp = managedComponent.getProperty(metricPropName);
        if (metricProp == null) {
            return null;
        }
        MetaValue metaValue;
        if (dotIndex == -1) {
            metaValue = metricProp.getValue();
        } else {
            CompositeValue compositeValue = (CompositeValue) metricProp.getValue();
            String key = compositePropName.substring(dotIndex + 1);
            metaValue = compositeValue.get(key);
        }
        return getInnerValue(metaValue);
    }

    // TODO: Move this to a utility class.
    @Nullable
    private static Object getInnerValue(MetaValue metaValue) {
        if (metaValue == null) {
            return null;
        }
        Object value;
        if (metaValue.getMetaType().isSimple()) {
            SimpleValue simpleValue = (SimpleValue) metaValue;
            value = simpleValue.getValue();
        } else if (metaValue.getMetaType().isEnum()) {
            EnumValue enumValue = (EnumValue) metaValue;
            value = enumValue.getValue();
        } else if (metaValue.getMetaType().isArray()) {
            ArrayValue arrayValue = (ArrayValue) metaValue;
            value = arrayValue.getValue();
        } else if (metaValue.getMetaType().isCollection()) {
            CollectionValue collectionValue = (CollectionValue) metaValue;
            List list = new ArrayList();
            for (MetaValue element : collectionValue.getElements()) {
                list.add(getInnerValue(element));
            }
            value = list;
        } else {
            value = metaValue.toString();
        }
        return value;
    }

    protected void addValueToMeasurementReport(MeasurementReport report, MeasurementScheduleRequest request,
        Object value) {
        if (value == null) {
            return;
        }
        String stringValue = toString(value);

        DataType dataType = request.getDataType();
        switch (dataType) {
        case MEASUREMENT:
            try {
                MeasurementDataNumeric dataNumeric = new MeasurementDataNumeric(request, Double.valueOf(stringValue));
                report.addData(dataNumeric);
            } catch (NumberFormatException e) {
                log.error("Profile service did not return a numeric value as expected for metric [" + request.getName()
                    + "] - value returned was " + value + ".", e);
            }
            break;
        case TRAIT:
            MeasurementDataTrait dataTrait = new MeasurementDataTrait(request, stringValue);
            report.addData(dataTrait);
            break;
        default:
            throw new IllegalStateException("Unsupported measurement data type: " + dataType);
        }
    }

    protected ComponentType getComponentType() {
        return componentType;
    }

    protected String getComponentName() {
        return componentName;
    }

    protected ManagedComponent getManagedComponent() {
        ManagedComponent managedComponent;
        try {
            ManagementView managementView = getConnection().getManagementView();
            managedComponent = ManagedComponentUtils.getManagedComponent(managementView, this.componentType,
                this.componentName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load [" + this.componentType + "] ManagedComponent ["
                + this.componentName + "].", e);
        }
        if (managedComponent == null)
            throw new IllegalStateException("Failed to find [" + this.componentType + "] ManagedComponent named ["
                + this.componentName + "].");
        log.trace("Retrieved " + toString(managedComponent) + ".");
        return managedComponent;
    }

    @NotNull
    private OperationDefinition getOperationDefinition(String operationName) {
        ResourceType resourceType = getResourceContext().getResourceType();
        OperationDefinition operationDefinition = ResourceTypeUtils.getOperationDefinition(resourceType, operationName);
        if (operationDefinition == null)
            throw new IllegalStateException("Operation named '" + operationName
                + "' is not defined for Resource type '" + resourceType.getName() + "' in the '"
                + resourceType.getPlugin() + "' plugin's descriptor.");
        return operationDefinition;
    }

    @NotNull
    private ManagedOperation getManagedOperation(OperationDefinition operationDefinition) {
        ManagedComponent managedComponent = getManagedComponent();
        Set<ManagedOperation> operations = managedComponent.getOperations();
        for (ManagedOperation operation : operations) {
            ConfigurationDefinition paramsConfigDef = operationDefinition.getParametersConfigurationDefinition();
            int paramCount = (paramsConfigDef != null) ? paramsConfigDef.getPropertyDefinitions().size() : 0;
            if (operation.getName().equals(operationDefinition.getName())
                && (operation.getParameters().length == paramCount))
                return operation;
        }
        throw new IllegalStateException("ManagedOperation named '" + operationDefinition.getName()
            + "' not found on ManagedComponent [" + getManagedComponent() + "].");
    }

    private static String toString(ManagedComponent managedComponent) {
        Map<String, ManagedProperty> properties = managedComponent.getProperties();
        return managedComponent.getClass().getSimpleName() + "@" + System.identityHashCode(managedComponent) + "["
            + "type=" + managedComponent.getType() + ", name=" + managedComponent.getName() + ", properties="
            + properties.getClass().getSimpleName() + "@" + System.identityHashCode(properties) + "]";
    }

    private static String toString(@NotNull Object value) {
        if (value.getClass().isArray()) {
            StringBuilder buffer = new StringBuilder();
            int lastIndex = Array.getLength(value) - 1;
            for (int i = 0; i < Array.getLength(value); i++) {
                buffer.append(String.valueOf(Array.get(value, i)));
                if (i == lastIndex) {
                    break;
                }
                buffer.append(", ");
            }
            return buffer.toString();
        } else {
            return value.toString();
        }
    }
}
