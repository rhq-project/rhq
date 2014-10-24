/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import java.util.HashMap;
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
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
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

    private static final Log LOG = LogFactory.getLog(ManagedComponentComponent.class);

    public static interface Config {
        String COMPONENT_TYPE = "componentType";
        String COMPONENT_SUBTYPE = "componentSubtype";
        String COMPONENT_NAME = "componentName";
        String TEMPLATE_NAME = "templateName";
        String COMPONENT_NAME_PROPERTY = "componentNameProperty";
    }

    protected static final char PREFIX_DELIMITER = '|';

    /**
     * The availability refresh interval specifies a duration that if exceeded means a managed
     * component refresh is needed to perform an availability check. That duration is set
     * to 15 minutes.
     */
    private static final long AVAIL_REFRESH_INTERVAL = 1000 * 60 * 15; // 15 minutes

    private long availRefreshInterval = AVAIL_REFRESH_INTERVAL;

    /**
     * The ManagedComponent is fetched from the server in {@link #getManagedComponent} throughout
     * the life cycle of this resource component. For example during metrics collections
     * when getValues() is invoked, getManagedComponent() is called. Any time getManagedComponent()
     * is called the lastComponentRefresh timestamp is updated. This timestamp is used in
     * {@link #getAvailability} to determine whether or not a component is needed to
     * perform an availability check.
     */
    private volatile long lastComponentRefresh = 0L;

    // The last known runState for the component.  This is used to determine the result of getAvailability(). We
    // do *not* cache the entire ManagedComponent because it is potentially a huge object that would eat too much memory.
    private RunState runState;

    private String componentName;
    private ComponentType componentType;

    // ResourceComponent Implementation  --------------------------------------------

    @Override
    public AvailabilityType getAvailability() {
        long timeSinceComponentRefresh = System.currentTimeMillis() - lastComponentRefresh;
        boolean refresh = timeSinceComponentRefresh > availRefreshInterval;

        if (runState == null || refresh) {
            if (LOG.isDebugEnabled() && runState != null && lastComponentRefresh > 0L) {
                LOG.debug("The availability refresh interval for [resourceKey: "
                    + getResourceContext().getResourceKey() + ", type: " + componentType + ", name: " + componentName
                    + "] has been exceeded by " + (timeSinceComponentRefresh - availRefreshInterval)
                    + " ms. Reloading managed component...");
            }

            ManagedComponent managedComponent = getManagedComponent();
            runState = managedComponent.getRunState();
        }

        return getAvailabilityForRunState(runState);
    }

    protected AvailabilityType getAvailabilityForRunState(RunState runState) {
        if (runState == RunState.RUNNING) {
            return AvailabilityType.UP;

        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Returning DOWN avail for " + componentType + " component '" + componentName
                    + "' with runState [" + runState + "].");
            }

            return AvailabilityType.DOWN;
        }
    }

    @Override
    public void start(ResourceContext<ProfileServiceComponent<?>> resourceContext) throws Exception {
        super.start(resourceContext);
        componentType = ConversionUtils.getComponentType(getResourceContext().getResourceType());
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        componentName = pluginConfig.getSimple(Config.COMPONENT_NAME).getStringValue();
        initAvailRefreshInterval(resourceContext);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Started ResourceComponent for " + getResourceDescription() + ", managing " + this.componentType
                + " component '" + this.componentName + "'.");
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    // ConfigurationComponent Implementation  --------------------------------------------

    @Override
    public Configuration loadResourceConfiguration() {
        Configuration resourceConfig;
        ManagedComponent managedComponent = getManagedComponent();
        try {
            Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(getResourceContext()
                .getPluginConfiguration());
            if (LOG.isDebugEnabled()) {
                LOG.debug("*** AFTER LOAD:\n" + DebugUtils.convertPropertiesToString(managedProperties));
            }
            resourceConfig = ConversionUtils.convertManagedObjectToConfiguration(managedProperties, customProps,
                getResourceContext().getResourceType());
        } catch (Exception e) {
            RunState runState = managedComponent.getRunState();
            if (runState == RunState.RUNNING) {
                LOG.error("Failed to load configuration for " + getResourceDescription() + ".", e);
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to load configuration for " + getResourceDescription()
                    + ", but managed component is not in the RUNNING state.", e);
            }
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return resourceConfig;
    }

    /**
     * updates resource configuration, but only changes. This is done by loading configuration first and then comparing
     * all the simple properties (if existing and new value equals, property is skipped)
     *
     * @param configurationUpdateReport report
     */
    public void updateResourceConfigurationChangesOnly(ConfigurationUpdateReport configurationUpdateReport) {
        Configuration existing = loadResourceConfiguration();
        Configuration config = configurationUpdateReport.getConfiguration();
        ConfigurationDefinition configDefCopy = copyConfigurationDefinition(getResourceContext().getResourceType()
            .getResourceConfigurationDefinition());
        // filter out unchanged values
        for (Property prop : config.getAllProperties().values()) {
            if (prop instanceof PropertySimple) {
                if (prop instanceof PropertySimple) {
                    PropertySimple propSimple = (PropertySimple) prop;
                    String val1 = propSimple.getStringValue();
                    String val2 = existing.getSimpleValue(propSimple.getName());
                    if (val1 == null && val2 == null) {
                        configDefCopy.getPropertyDefinitions().remove(propSimple.getName());
                    }
                    if (val1 != null) {
                        if (val1.equals(val2)) {
                            configDefCopy.getPropertyDefinitions().remove(propSimple.getName());
                        }
                    } else if (val2 != null) {
                        if (val2.equals(val1)) {
                            configDefCopy.getPropertyDefinitions().remove(propSimple.getName());
                        }
                    }
                }
            }
        }
        updateResourceConfiguration(configurationUpdateReport, configDefCopy);
    }

    /**
     * update resource configuration. Given resourceConfigurationDefinition defines which properties will be updated. Use this
     * method in case you don't want to update all properties defined by resource type and supply resourceConfigurationDefinition
     * consisting of stuff you want.
     * @param configurationUpdateReport
     * @param resourceConfigurationDefinition
     */
    protected void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport,
        ConfigurationDefinition resourceConfigurationDefinition) {
        Configuration resourceConfig = configurationUpdateReport.getConfiguration();
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        try {
            ManagedComponent managedComponent = getManagedComponent();
            Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(pluginConfig);
            if (LOG.isDebugEnabled()) {
                LOG.debug("*** BEFORE UPDATE:\n" + DebugUtils.convertPropertiesToString(managedProperties));
            }
            ConversionUtils.convertConfigurationToManagedProperties(managedProperties, resourceConfig,
                resourceConfigurationDefinition, customProps, true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("*** AFTER UPDATE:\n" + DebugUtils.convertPropertiesToString(managedProperties));
            }
            updateComponent(managedComponent);
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            LOG.error("Failed to update configuration for " + getResourceDescription() + ".", e);
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
            configurationUpdateReport.setErrorMessage(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        updateResourceConfiguration(configurationUpdateReport, getResourceContext().getResourceType()
            .getResourceConfigurationDefinition());
    }

    // DeleteResourceFacet Implementation  --------------------------------------------

    @Override
    public void deleteResource() throws Exception {
        DeploymentManager deploymentManager = getConnection().getDeploymentManager();
        if (!deploymentManager.isRedeploySupported())
            throw new UnsupportedOperationException("Deletion of " + getResourceContext().getResourceType().getName()
                + " Resources is not currently supported.");
        ManagedComponent managedComponent = getManagedComponent();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing " + getResourceDescription() + " with component " + toString(managedComponent) + "...");
        }
        ManagementView managementView = getConnection().getManagementView();
        managementView.removeComponent(managedComponent);
        ManagedDeployment parentDeployment = managedComponent.getDeployment();

        if (parentDeployment.getComponents().size() > 1 || !parentDeployment.getChildren().isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Redeploying parent deployment '" + parentDeployment.getName()
                    + "' in order to complete removal of component " + toString(managedComponent) + "...");
            }
            DeploymentProgress progress = deploymentManager.redeploy(parentDeployment.getName());
            DeploymentStatus status = DeploymentUtils.run(progress);
            if (status.isFailed()) {
                LOG.error("Failed to redeploy parent deployment '" + parentDeployment.getName()
                    + "during removal of component " + toString(managedComponent)
                    + " - removal may not persist when the app server is restarted.", status.getFailure());
            }
        } else {
            //this is the last component of the deployment and nothing would be left there after
            //the component was removed. Let's just undeploy it in addition to removing the component.
            //This will make sure that the deployment doesn't leave behind any defunct config files, etc.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Undeploying parent deployment '" + parentDeployment.getName()
                    + "' in order to complete removal of component " + toString(managedComponent) + "...");
            }
            parentDeployment = managementView.getDeployment(parentDeployment.getName());
            DeploymentProgress progress = deploymentManager.remove(parentDeployment.getName());
            DeploymentStatus status = DeploymentUtils.run(progress);
            if (status.isFailed()) {
                LOG.error("Failed to undeploy parent deployment '" + parentDeployment.getName()
                    + "during removal of component " + toString(managedComponent)
                    + " - removal may not persist when the app server is restarted.", status.getFailure());
            }
        }

        managementView.load();
    }

    // OperationFacet Implementation  --------------------------------------------

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        return invokeOperation(getManagedComponent(), name, parameters);
    }

    protected OperationResult invokeOperation(ManagedComponent managedComponent, String name, Configuration parameters)
        throws Exception {
        OperationDefinition operationDefinition = getOperationDefinition(name);
        ManagedOperation managedOperation = getManagedOperation(managedComponent, operationDefinition);
        // Convert parameters into MetaValue array.
        MetaValue[] parameterMetaValues = ConversionUtils.convertOperationsParametersToMetaValues(managedOperation,
            parameters, operationDefinition);
        // invoke() takes a varargs, so we need to pass an empty array, rather than null.
        MetaValue resultMetaValue = managedOperation.invoke(parameterMetaValues);
        OperationResult result = new OperationResult();
        // Convert result MetaValue to corresponding Property type.
        ConversionUtils.convertManagedOperationResults(managedOperation, resultMetaValue, result.getComplexResults(),
            operationDefinition);
        // If this is a lifecycle operation ask for an avail check
        boolean availCheck = name.toLowerCase().equals("stop") || name.toLowerCase().contains("start");
        if (availCheck) {
            getResourceContext().getAvailabilityContext().requestAvailabilityCheck();
        }

        return result;
    }

    // MeasurementFacet Implementation  --------------------------------------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        getValues(getManagedComponent(), report, metrics);
    }

    protected void getValues(ManagedComponent managedComponent, MeasurementReport report,
        Set<MeasurementScheduleRequest> metrics) throws Exception {
        RunState runState = managedComponent.getRunState();
        for (MeasurementScheduleRequest request : metrics) {
            try {
                String value = getMeasurement(managedComponent, request.getName());
                addValueToMeasurementReport(report, request, value);
            } catch (Exception e) {
                if (runState == RunState.RUNNING) {
                    LOG.error("Failed to collect metric for " + request, e);
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to collect metric for " + request
                        + ", but managed component is not in the RUNNING state.", e);
                }
            }
        }
    }

    protected String getMeasurement(ManagedComponent component, String metricName) throws Exception {
        if ("runState".equals(metricName)) {
            return component.getRunState().name();
        } else {
            Object value = getSimpleValue(component, metricName);
            return value == null ? null : toString(value);
        }
    }

    protected void updateComponent(ManagedComponent managedComponent) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Updating " + getResourceDescription() + " with component " + toString(managedComponent) + "...");
        }
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
        return getSimpleValue(managedComponent, metricName);
    }

    @Nullable
    protected Object getSimpleValue(ManagedComponent managedComponent, String metricName) {
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
    protected ManagedProperty getManagedProperty(ManagedComponent managedComponent, MeasurementScheduleRequest request) {
        String metricName = request.getName();
        int pipeIndex = metricName.indexOf(PREFIX_DELIMITER);
        // Remove the prefix if there is one (e.g. "ThreadPool|currentThreadCount" -> "currentThreadCount").
        String compositePropName = (pipeIndex == -1) ? metricName : metricName.substring(pipeIndex + 1);
        int dotIndex = compositePropName.indexOf('.');
        String metricPropName = (dotIndex == -1) ? compositePropName : compositePropName.substring(0, dotIndex);
        return managedComponent.getProperty(metricPropName);
    }

    // TODO: Move this to a utility class.
    @Nullable
    protected static Object getInnerValue(MetaValue metaValue) {
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
            List<Object> list = new ArrayList<Object>();
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
                LOG.error("Profile service did not return a numeric value as expected for metric [" + request.getName()
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

    /**
     * This method should most likely not be overridden. Instead, override {@link #getManagedComponent(ManagementView)}.
     * <br/><br/>
     * IMPORTANT!!! The returned ManagedComponent SHOULD NOT be cached in the instance. It is potentially a memory hog.
     *
     * @return The ManagedComponent
     * @throws RuntimeException if fetching the ManagementView or getting the component fails
     * @throws IllegalStateException if the managedComponent is null/not found
     */
    @NotNull
    protected ManagedComponent getManagedComponent() {
        ManagedComponent managedComponent;

        try {
            ManagementView managementView = getConnection().getManagementView();
            managedComponent = getManagedComponent(managementView);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load [" + this.componentType + "] ManagedComponent ["
                + this.componentName + "].", e);
        }

        // Even if not found, update the refresh time. It will avoid too many costly, and potentially fruitless, fetches
        lastComponentRefresh = System.currentTimeMillis();

        if (managedComponent == null) {
            throw new IllegalStateException("Failed to find [" + this.componentType + "] ManagedComponent named ["
                + this.componentName + "].");
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Retrieved " + toString(managedComponent) + ".");
        }

        return managedComponent;
    }

    /**
     * This is an override point. When actually fetching the managed component, this entry point should not be
     * used. Instead, access should be via {@link #getManagedComponent()}.
     *
     * @param managementView for querying profile service
     * @return the ManagedComponent. Null if not found.
     * @throws Exception if there is a problem getting the component.
     */
    protected ManagedComponent getManagedComponent(ManagementView managementView) throws Exception {
        if (null == managementView) {
            throw new IllegalArgumentException("managementView can not be null");
        }

        return managementView.getComponent(this.componentName, this.componentType);
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
    private ManagedOperation getManagedOperation(ManagedComponent managedComponent,
        OperationDefinition operationDefinition) {
        Set<ManagedOperation> operations = managedComponent.getOperations();
        for (ManagedOperation operation : operations) {
            ConfigurationDefinition paramsConfigDef = operationDefinition.getParametersConfigurationDefinition();
            int paramCount = (paramsConfigDef != null) ? paramsConfigDef.getPropertyDefinitions().size() : 0;
            if (operation.getName().equals(operationDefinition.getName())
                && (operation.getParameters().length == paramCount))
                return operation;
        }
        throw new IllegalStateException("ManagedOperation named '" + operationDefinition.getName()
            + "' not found on ManagedComponent [" + managedComponent + "].");
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

    private void initAvailRefreshInterval(ResourceContext<ProfileServiceComponent<?>> context) {
        ProfileServiceComponent<?> component = context.getParentResourceComponent();
        while (component != null) {
            if (component instanceof ApplicationServerComponent) {
                break;
            }
            component = (ProfileServiceComponent<?>) component.getResourceContext().getParentResourceComponent();
        }

        if (component == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to find parent " + ApplicationServerComponent.class.getSimpleName()
                    + ". Using default component refresh interval, " + AVAIL_REFRESH_INTERVAL + " ms");
            }
            return;
        }

        String interval = component.getResourceContext().getPluginConfiguration()
            .getSimpleValue("serviceAvailabilityRefreshInterval", Long.toString(AVAIL_REFRESH_INTERVAL));
        availRefreshInterval = Long.parseLong(interval) * 1000 * 60;
    }

    static ConfigurationDefinition copyConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        ConfigurationDefinition configDefCopy = new ConfigurationDefinition(configurationDefinition.getName(),
            configurationDefinition.getDescription());
        configDefCopy.setConfigurationFormat(configurationDefinition.getConfigurationFormat());
        configDefCopy.setPropertyDefinitions(new HashMap<String, PropertyDefinition>(configurationDefinition
            .getPropertyDefinitions()));
        return configDefCopy;
    }
}
