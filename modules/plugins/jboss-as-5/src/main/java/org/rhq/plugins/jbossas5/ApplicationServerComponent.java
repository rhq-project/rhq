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

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.ProgressEvent;
import org.jboss.deployers.spi.management.deploy.ProgressListener;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;
import org.jboss.on.common.jbossas.JBossASPaths;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.rhq.plugins.jbossas5.connection.LocalProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.RemoteProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.deploy.Deployer;
import org.rhq.plugins.jbossas5.deploy.LocalDeployer;
import org.rhq.plugins.jbossas5.deploy.RemoteDeployer;
import org.rhq.plugins.jbossas5.helper.JBossAS5ConnectionTypeDescriptor;
import org.rhq.plugins.jbossas5.helper.JmxConnectionHelper;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DebugUtils;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

import com.jboss.jbossnetwork.product.jbpm.handlers.ControlActionFacade;

/**
 * ResourceComponent for a JBoss AS, 5.1.0.CR1 or later, Server.
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class ApplicationServerComponent implements ResourceComponent, ProfileServiceComponent,
    CreateChildResourceFacet, MeasurementFacet, ConfigurationFacet, ProgressListener, ContentFacet, OperationFacet {

    private static final String MANAGED_PROPERTY_GROUP = "managedPropertyGroup";

    private static final Pattern METRIC_NAME_PATTERN = Pattern.compile("(.*)\\|(.*)\\|(.*)\\|(.*)");

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;
    private ProfileServiceConnection connection;
    private JmxConnectionHelper jmxConnectionHelper;

    private ApplicationServerContentFacetDelegate contentFacetDelegate;
    private ApplicationServerOperationsDelegate operationDelegate;
    private LogFileEventResourceComponentHelper logFileEventDelegate;

    public AvailabilityType getAvailability() {
        connectToProfileService();
        AvailabilityType availability;
        if (this.connection != null) {
            try {
                // Ping the connection to make sure it's not defunct.
                this.connection.getManagementView().getComponentTypes();
                availability = AvailabilityType.UP;
            } catch (Exception e) {
                availability = AvailabilityType.DOWN;
            }
        } else {
            availability = AvailabilityType.DOWN;
        }
        return availability;
    }

    public void start(ResourceContext resourceContext) {
        this.resourceContext = resourceContext;

        // Connect to the JBAS instance's Profile Service and JMX MBeanServer.
        connectToProfileService();
        initializeEmsConnection();

        // Now create all our helpers and delegates.
        this.logFileEventDelegate = new LogFileEventResourceComponentHelper(this.resourceContext);
        this.logFileEventDelegate.startLogFileEventPollers();

        JBPMWorkflowManager workflowManager = createJbpmWorkflowManager(resourceContext);
        File configPath = getConfigurationPath();

        this.contentFacetDelegate = new ApplicationServerContentFacetDelegate(workflowManager, configPath,
            resourceContext.getContentContext());
    }

    public void stop() {
        this.logFileEventDelegate.stopLogFileEventPollers();
        disconnectFromProfileService();
        this.jmxConnectionHelper.closeConnection();
    }

    // ------------ MeasurementFacet Implementation ------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        ManagementView managementView = getConnection().getManagementView();
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            try {
                // All other metric names are expected to have the following syntax:
                // "<componentType>|<componentSubType>|<componentName>|<propertyName>"
                Matcher matcher = METRIC_NAME_PATTERN.matcher(metricName);
                if (!matcher.matches()) {
                    log.error("Metric name '" + metricName + "' does not match pattern '" + METRIC_NAME_PATTERN + "'.");
                    continue;
                }
                String componentCategory = matcher.group(1);
                String componentSubType = matcher.group(2);
                String componentName = matcher.group(3);
                String propertyName = matcher.group(4);
                ComponentType componentType = new ComponentType(componentCategory, componentSubType);
                ManagedComponent component;
                if (componentName.equals("*")) {
                    component = ManagedComponentUtils.getSingletonManagedComponent(managementView, componentType);
                } else {
                    component = ManagedComponentUtils.getManagedComponent(managementView, componentType, componentName);
                }
                Serializable value = ManagedComponentUtils.getSimplePropertyValue(component, propertyName);
                if (value == null) {
                    log.debug("Null value returned for metric '" + metricName + "'.");
                    continue;
                }
                if (request.getDataType() == DataType.MEASUREMENT) {
                    Number number = (Number) value;
                    report.addData(new MeasurementDataNumeric(request, number.doubleValue()));
                } else if (request.getDataType() == DataType.TRAIT) {
                    report.addData(new MeasurementDataTrait(request, value.toString()));
                }
            } catch (RuntimeException e) {
                log.error("Failed to obtain metric '" + metricName + "'.", e);
            }
        }
    }

    // ------------ ConfigurationFacet Implementation ------------

    public Configuration loadResourceConfiguration() {
        /*
         * Need to determine what we consider server configuration to return.
         * Also need to understand what ComponentType the profile service would
         * use to retrieve "server" level configuration.
         */

        return null;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        // See above comment on server configuration.
    }

    // CreateChildResourceFacet --------------------------------------------

    public CreateResourceReport createResource(CreateResourceReport createResourceReport) {
        // ProfileServiceFactory.refreshCurrentProfileView();
        ResourceType resourceType = createResourceReport.getResourceType();
        if (resourceType.getCreationDataType() == ResourceCreationDataType.CONTENT)
            createContentBasedResource(createResourceReport, resourceType);
        else
            createConfigurationBasedResource(createResourceReport, resourceType);
        return createResourceReport;
    }

    // ProgressListener --------------------------------------------

    public void progressEvent(ProgressEvent eventInfo) {
        log.debug(eventInfo);
    }

    @Nullable
    public ProfileServiceConnection getConnection() {
        connectToProfileService();
        return this.connection;
    }

    // ContentFacet -------------------------------------------------

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        return contentFacetDelegate.deployPackages(packages, contentServices);
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        return contentFacetDelegate.discoverDeployedPackages(type);
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return contentFacetDelegate.generateInstallationSteps(packageDetails);
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return contentFacetDelegate.removePackages(packages);
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return contentFacetDelegate.retrievePackageBits(packageDetails);
    }

    // ---------------------------------------------------------------

    private void connectToProfileService() {
        if (this.connection != null)
            return;
        // TODO: Check for a defunct connection and if found try to reconnect.
        ProfileServiceConnectionProvider connectionProvider;
        if (runningEmbedded()) {
            connectionProvider = new LocalProfileServiceConnectionProvider();
        } else {
            Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
            String namingURL = pluginConfig.getSimpleValue(PluginConfigUtil.NAMING_URL, null);
            validateNamingURL(namingURL);
            String principal = pluginConfig.getSimpleValue(PluginConfigUtil.PRINCIPAL, null);
            String credentials = pluginConfig.getSimpleValue(PluginConfigUtil.CREDENTIALS, null);
            connectionProvider = new RemoteProfileServiceConnectionProvider(namingURL, principal, credentials);
        }
        try {
            this.connection = connectionProvider.connect();
        } catch (RuntimeException e) {
            log.debug("Failed to connect to Profile Service.", e);
        }
    }

    private void disconnectFromProfileService() {
        if (this.connection != null) {
            try {
                this.connection.getConnectionProvider().disconnect();
            } catch (RuntimeException e) {
                log.debug("Failed to disconnect from Profile Service.", e);
            } finally {
                this.connection = null;
            }
        }
    }

    private void handleMiscManagedProperties(Collection<PropertyDefinition> managedPropertyGroup,
        Map<String, ManagedProperty> managedProperties, Configuration pluginConfiguration) {
        for (PropertyDefinition propertyDefinition : managedPropertyGroup) {
            String propertyKey = propertyDefinition.getName();
            Property property = pluginConfiguration.get(propertyKey);
            ManagedProperty managedProperty = managedProperties.get(propertyKey);
            if (managedProperty != null && property != null) {
                PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(managedProperty
                    .getMetaType());
                propertyAdapter.populateMetaValueFromProperty(property, managedProperty.getValue(), propertyDefinition);
            }
        }
    }

    private static String getComponentName(Configuration pluginConfig, Configuration resourceConfig) {
        PropertySimple componentNameProp =
                pluginConfig.getSimple(ManagedComponentComponent.Config.COMPONENT_NAME_PROPERTY);
        if (componentNameProp == null || componentNameProp.getStringValue() == null) {
            throw new IllegalStateException("Property [" + ManagedComponentComponent.Config.COMPONENT_NAME_PROPERTY
                + "] is not defined in the default plugin configuration.");
        }
        String componentNamePropName = componentNameProp.getStringValue();
        PropertySimple propToUseAsComponentName = resourceConfig.getSimple(componentNamePropName);
        if (propToUseAsComponentName == null) {
            throw new IllegalStateException("Property [" + componentNamePropName
                + "] is not defined in user-specified initial Resource configuration.");
        }
        return propToUseAsComponentName.getStringValue();
    }

    private void createConfigurationBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType) {
        Configuration defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
        Configuration resourceConfig = createResourceReport.getResourceConfiguration();
        String componentName = getComponentName(defaultPluginConfig, resourceConfig);
        ComponentType componentType = ConversionUtils.getComponentType(resourceType);
        ManagementView managementView = getConnection().getManagementView();
        if (ManagedComponentUtils.isManagedComponent(managementView, componentName, componentType)) {
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setErrorMessage("A " + resourceType.getName() + " named '" + componentName
                + "' already exists.");
            return;
        }

        // The PC doesn't use the Resource name or key for anything, but set them anyway to make it happy.
        createResourceReport.setResourceName(componentName);
        createResourceReport.setResourceKey(componentName);

        PropertySimple templateNameProperty = defaultPluginConfig
            .getSimple(ManagedComponentComponent.Config.TEMPLATE_NAME);
        String templateName = templateNameProperty.getStringValue();

        DeploymentTemplateInfo template;
        try {
            template = managementView.getTemplate(templateName);
            Map<String, ManagedProperty> managedProperties = template.getProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(defaultPluginConfig);

            if (log.isDebugEnabled())
                log.debug("BEFORE CREATE:\n" + DebugUtils.convertPropertiesToString(template));
            ConversionUtils.convertConfigurationToManagedProperties(managedProperties, resourceConfig, resourceType,
                customProps);
            if (log.isDebugEnabled())
                log.debug("AFTER CREATE:\n" + DebugUtils.convertPropertiesToString(template));

            ConfigurationDefinition pluginConfigDef = resourceType.getPluginConfigurationDefinition();
            Collection<PropertyDefinition> managedPropertyGroup = pluginConfigDef
                .getPropertiesInGroup(MANAGED_PROPERTY_GROUP);
            handleMiscManagedProperties(managedPropertyGroup, managedProperties, defaultPluginConfig);
            log.debug("Applying template [" + templateName + "] to create ManagedComponent of type [" + componentType
                + "]...");
            try {
                managementView.applyTemplate(componentName, template);
                managementView.process();
                createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
            } catch (Exception e) {
                log.error("Unable to apply template [" + templateName + "] to create ManagedComponent of type "
                    + componentType + ".", e);
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setException(e);
            }
        } catch (NoSuchDeploymentException e) {
            log.error("Unable to find template [" + templateName + "].", e);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setException(e);
        } catch (Exception e) {
            log.error("Unable to process create request", e);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setException(e);
        }
    }

    private void createContentBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType) {
        getDeployer().deploy(createResourceReport, resourceType);
    }

    private Deployer getDeployer() {
        ProfileServiceConnection profileServiceConnection = getConnection();
        if (runningEmbedded()) {
            return new LocalDeployer(profileServiceConnection);
        } else {
            return new RemoteDeployer(profileServiceConnection, this.resourceContext);
        }
    }

    private static Configuration getDefaultPluginConfiguration(ResourceType resourceType) {
        ConfigurationTemplate pluginConfigDefaultTemplate = resourceType.getPluginConfigurationDefinition()
            .getDefaultTemplate();
        return (pluginConfigDefaultTemplate != null) ? pluginConfigDefaultTemplate.createConfiguration()
            : new Configuration();
    }

    @NotNull
    private JBossASPaths getJBossASPaths() {
        Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();

        String homeDir = pluginConfiguration.getSimpleValue(PluginConfigUtil.HOME_DIR, null);
        String serverHomeDir = pluginConfiguration.getSimpleValue(PluginConfigUtil.SERVER_HOME_DIR, null);

        return new JBossASPaths(homeDir, serverHomeDir);
    }

    private boolean runningEmbedded() {
        Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();
        String namingUrl = pluginConfiguration.getSimpleValue(PluginConfigUtil.NAMING_URL, null);
        return namingUrl == null;
    }

    @NotNull
    private File resolvePathRelativeToHomeDir(@NotNull String path) {
        return PluginConfigUtil.resolvePathRelativeToHomeDir(this.resourceContext.getPluginConfiguration(), path);
    }

    private static void validateNamingURL(String namingURL) {
        URI namingURI;
        try {
            namingURI = new URI(namingURL);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Naming URL '" + namingURL + "' is not valid: " + e.getLocalizedMessage());
        }
        if (!namingURI.isAbsolute())
            throw new RuntimeException("Naming URL '" + namingURL + "' is not absolute.");
        if (!namingURI.getScheme().equals("jnp"))
            throw new RuntimeException("Naming URL '" + namingURL
                + "' has an invalid protocol - the only valid protocol is 'jnp'.");
    }

    public EmsConnection getEmsConnection() {
        return jmxConnectionHelper.getEmsConnection();
    }

    @NotNull
    private static String getRequiredPropertyValue(@NotNull Configuration config, @NotNull String propName) {
        String propValue = config.getSimpleValue(propName, null);
        if (propValue == null) {
            // Something's not right - neither autodiscovery, nor the config
            // edit GUI, should ever allow this.
            throw new IllegalStateException("Required property '" + propName + "' is not set.");
        }

        return propValue;
    }

    public ResourceContext getResourceContext() {
        return this.resourceContext;
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        if (this.operationDelegate == null) {
            this.operationDelegate = new ApplicationServerOperationsDelegate(this, this.resourceContext
                .getSystemInformation());
        }
        ApplicationServerSupportedOperations operation = Enum.valueOf(ApplicationServerSupportedOperations.class, name
            .toUpperCase());
        return this.operationDelegate.invoke(operation, parameters);
    }

    private void initializeEmsConnection() {
        Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();

        Configuration jmxConfig = new Configuration();

        String jbossHomeDir = pluginConfiguration.getSimpleValue(PluginConfigUtil.HOME_DIR, null);

        String connectorDescriptorType;
        boolean runningEmbedded = runningEmbedded();
        if (runningEmbedded) {
            connectorDescriptorType = InternalVMTypeDescriptor.class.getName();
        } else {
            String connectorAddress = pluginConfiguration.getSimpleValue(PluginConfigUtil.NAMING_URL, null);
            String connectorPrincipal = pluginConfiguration.getSimpleValue(PluginConfigUtil.PRINCIPAL, null);
            String connectorCredentials = pluginConfiguration.getSimpleValue(PluginConfigUtil.CREDENTIALS, null);

            connectorDescriptorType = JBossAS5ConnectionTypeDescriptor.class.getName();

            jmxConfig.put(new PropertySimple(JmxConnectionHelper.CONNECTOR_ADDRESS, connectorAddress));
            jmxConfig.put(new PropertySimple(JmxConnectionHelper.CONNECTOR_CREDENTIALS, connectorCredentials));
            jmxConfig.put(new PropertySimple(JmxConnectionHelper.CONNECTOR_PRINCIPAL, connectorPrincipal));
        }
        jmxConfig.put(new PropertySimple(JmxConnectionHelper.CONNECTOR_DESCRIPTOR_TYPE, connectorDescriptorType));
        jmxConfig.put(new PropertySimple(JmxConnectionHelper.JBOSS_HOME_DIR, jbossHomeDir));

        this.jmxConnectionHelper = new JmxConnectionHelper(!runningEmbedded, resourceContext.getTemporaryDirectory());
        EmsConnection conn = this.jmxConnectionHelper.getEmsConnection(jmxConfig);
        if (conn != null) {
            log.info("Successfully obtained a JMX connection to "
                + jmxConfig.getSimpleValue(JmxConnectionHelper.CONNECTOR_ADDRESS, "-n/a-"));
        }
    }

    private JBPMWorkflowManager createJbpmWorkflowManager(ResourceContext resourceContext) {
        ContentContext contentContext = resourceContext.getContentContext();
        ControlActionFacade controlActionFacade = initControlActionFacade();
        JBPMWorkflowManager workflowManager = new JBPMWorkflowManager(contentContext, controlActionFacade, this
            .getJBossASPaths());
        return workflowManager;
    }

    private ControlActionFacade initControlActionFacade() {
        // TODO define the control action facade once we support operations on the app server.
        // OperationContext operationContext = resourceContext.getOperationContext();
        //
        // ControlActionFacade controlActionFacade = new PluginContainerControlActionFacade(operationContext, this);
        ControlActionFacade controlActionFacade = null;
        return controlActionFacade;
    }

    private File getConfigurationPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        File configPath = resolvePathRelativeToHomeDir(getRequiredPropertyValue(pluginConfig,
            PluginConfigUtil.SERVER_HOME_DIR));
        if (!configPath.isDirectory()) {
            throw new InvalidPluginConfigurationException("Configuration path '" + configPath + "' does not exist.");
        }
        return configPath;
    }
}
