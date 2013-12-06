/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5;

import static org.rhq.core.domain.resource.CreateResourceStatus.SUCCESS;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jboss.jbossnetwork.product.jbpm.handlers.ControlActionFacade;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.ProgressEvent;
import org.jboss.deployers.spi.management.deploy.ProgressListener;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;
import org.jboss.on.common.jbossas.JBossASPaths;
import org.jboss.on.common.jbossas.JmxConnectionHelper;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
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
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.rhq.plugins.jbossas5.connection.LocalProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.RemoteProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.helper.CreateChildResourceFacetDelegate;
import org.rhq.plugins.jbossas5.helper.InPluginControlActionFacade;
import org.rhq.plugins.jbossas5.helper.JBossAS5ConnectionTypeDescriptor;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

/**
 * ResourceComponent for a JBoss AS, 5.1.0.CR1 or later, Server.
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class ApplicationServerComponent<T extends ResourceComponent<?>> implements ResourceComponent<T>,
    ProfileServiceComponent<T>, CreateChildResourceFacet, MeasurementFacet, ConfigurationFacet, ProgressListener,
    ContentFacet, OperationFacet {

    private static final Pattern METRIC_NAME_PATTERN = Pattern.compile("(.*)\\|(.*)\\|(.*)\\|(.*)");

    private static final Map<String, String> ALTERNATE_METRIC_NAMES = new HashMap<String, String>();
    static {
        ALTERNATE_METRIC_NAMES.put("MCBean|JTA|*|transactionCount", "MCBean|JTA|*|numberOfTransactions");
        ALTERNATE_METRIC_NAMES.put("MCBean|JTA|*|commitCount", "MCBean|JTA|*|numberOfCommittedTransactions");
        ALTERNATE_METRIC_NAMES.put("MCBean|JTA|*|rollbackCount", "MCBean|JTA|*|numberOfApplicationRollbacks");
        ALTERNATE_METRIC_NAMES.put("MCBean|ServerConfig|*|partitionName", "MCBean|HAPartition|*|partitionName");
    }

    private static final Map<String, String> VERIFIED_METRIC_NAMES = new HashMap<String, String>();

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;
    private ProfileServiceConnection connection;
    private JmxConnectionHelper jmxConnectionHelper;

    private ApplicationServerContentFacetDelegate contentFacetDelegate;
    private ApplicationServerOperationsDelegate operationDelegate;
    private LogFileEventResourceComponentHelper logFileEventDelegate;
    private CreateChildResourceFacetDelegate createChildResourceDelegate;

    public AvailabilityType getAvailability() {
        connectToProfileService();
        AvailabilityType availability;
        if (this.connection != null) {
            try {
                ManagementView managementView = this.connection.getManagementView();
                managementView.load();

                //let's see if the connection corresponds to the server
                //this component represents. This is to prevent 2 servers
                //with the same JNP URL to be reported as UP when just one
                //of them can be up at a time.
                ManagedComponent serverConfig = managementView
                    .getComponentsForType(new ComponentType("MCBean", "ServerConfig")).iterator().next();

                String reportedServerHomeDirPath = (String) ((SimpleValue) serverConfig.getProperty("serverHomeDir")
                    .getValue()).getValue();

                String configuredServerHomeDirPath = resourceContext.getPluginConfiguration().getSimpleValue(
                    ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR, null);

                //the paths might be symlinked
                File reportedServerHomeDir = new File(reportedServerHomeDirPath);
                File configuredServerHomeDir = new File(configuredServerHomeDirPath);

                availability = reportedServerHomeDir.getCanonicalPath().equals(
                    configuredServerHomeDir.getCanonicalPath()) ? AvailabilityType.UP : AvailabilityType.DOWN;
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
        this.operationDelegate = new ApplicationServerOperationsDelegate(this);
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

        this.createChildResourceDelegate = new CreateChildResourceFacetDelegate(this, this.getResourceContext());

        return;
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
            String requestName = request.getName();
            String verifiedMetricName = VERIFIED_METRIC_NAMES.get(requestName);
            String metricName = (verifiedMetricName != null) ? verifiedMetricName : requestName;
            try {
                Serializable value = null;
                boolean foundProperty = false;
                try {
                    value = getMetric(managementView, metricName);
                    foundProperty = true;
                } catch (ManagedComponentUtils.PropertyNotFoundException e) {
                    // ignore
                }

                if (value == null) {
                    metricName = ALTERNATE_METRIC_NAMES.get(metricName);
                    if (metricName != null) {
                        try {
                            value = getMetric(managementView, metricName);
                            foundProperty = true;
                        } catch (ManagedComponentUtils.PropertyNotFoundException e) {
                            // ignore
                        }
                    }
                }

                if (!foundProperty) {
                    List<String> propertyNames = new ArrayList<String>(2);
                    propertyNames.add(requestName);
                    if (ALTERNATE_METRIC_NAMES.containsKey(requestName)) {
                        propertyNames.add(ALTERNATE_METRIC_NAMES.get(requestName));
                    }
                    throw new IllegalStateException("A property was not found with any of the following names: "
                        + propertyNames);
                }

                if (value != null) {
                    VERIFIED_METRIC_NAMES.put(requestName, metricName);
                } else {
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
                log.error("Failed to obtain metric '" + requestName + "'.", e);
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

    public CreateResourceReport createResource(CreateResourceReport report) {
        if (creatingDatasourceOrConnectionFactory(report)) {
            report = preCreateDatasourceOrConnectionFactory(report);
        }
        report = this.createChildResourceDelegate.createResource(report);
        if (report.getStatus() != SUCCESS) {
            return report;
        }
        if (creatingDatasourceOrConnectionFactory(report)) {
            report = postCreateDatasourceOrConnectionFactory(report);
        }
        return report;
    }

    private CreateResourceReport preCreateDatasourceOrConnectionFactory(CreateResourceReport report) {
        Configuration resourceConfiguration = report.getResourceConfiguration();
        PropertyMap securityDomainPropertyMap = (PropertyMap) resourceConfiguration.get("security-domain");
        PropertySimple securityDeploymentType = securityDomainPropertyMap.getSimple("securityDeploymentType");
        if (securityDeploymentType.getStringValue() == null) {
            securityDeploymentType.setValue("NONE");
        }
        return report;
    }

    private CreateResourceReport postCreateDatasourceOrConnectionFactory(CreateResourceReport report) {
        Configuration resourceConfiguration = report.getResourceConfiguration();
        ConfigurationDefinition resourceConfigurationDefinition = report.getResourceType()
            .getResourceConfigurationDefinition();
        ComponentType componentType = ConversionUtils.getComponentType(report.getResourceType());
        String componentName = report.getResourceKey();
        try {
            ManagementView managementView = getConnection().getManagementView();
            ManagedComponent managedComponent = managementView.getComponent(componentName, componentType);
            Map<String, ManagedProperty> managedProperties = managedComponent.getProperties();
            ManagedProperty managedProperty = managedProperties.get("security-domain");
            MetaValue metaValue = managedProperty.getValue();
            PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);
            propertyAdapter.populateMetaValueFromProperty(resourceConfiguration.get("security-domain"), metaValue,
                    resourceConfigurationDefinition.get("security-domain"));
            managementView.updateComponent(managedComponent);
            managementView.load();
        } catch (Exception e) {
            report.setErrorMessage("Resource was created but an error occured while updating security-domain property");
            report.setException(e);
        }
        return report;
    }

    private boolean creatingDatasourceOrConnectionFactory(CreateResourceReport report) {
        String resourceTypeName = report.getResourceType().getName();
        return Arrays.asList("No Tx Datasource", "Local Tx Datasource", "XA Datasource", "No Tx ConnectionFactory",
            "Tx ConnectionFactory").contains(resourceTypeName);
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
        if (this.connection != null) {
            return;
        }
        // TODO: Check for a defunct connection and if found try to reconnect?
        ProfileServiceConnectionProvider connectionProvider;
        if (runningEmbedded()) {
            connectionProvider = new LocalProfileServiceConnectionProvider();
        } else {
            Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
            String namingURL = pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.NAMING_URL,
                null);
            validateNamingURL(namingURL);
            String principal = pluginConfig.getSimpleValue(ApplicationServerPluginConfigurationProperties.PRINCIPAL,
                null);
            String credentials = pluginConfig.getSimpleValue(
                ApplicationServerPluginConfigurationProperties.CREDENTIALS, null);
            connectionProvider = new RemoteProfileServiceConnectionProvider(namingURL, principal, credentials);
        }
        if (Thread.interrupted()) {
            // In case we've been timed out by the component facet invoker, clear the interrupted status,
            // so that when the below call to connect() tried to make a remote call, JBoss Remoting
            // doesn't throw an InterruptedException and short circuit our attempts to reconnect.
            log.debug("Ignoring facet timeout in order to reconnect to Profile Service.");
        }
        try {
            this.connection = connectionProvider.connect();
        } catch (RuntimeException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            rootCause = rootCause == null ? e : rootCause;
            if (rootCause instanceof SecurityException) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to connect to Profile Service.", e);
                } else {
                    log.warn("Failed to connect to Profile Service - cause: " + rootCause);
                }
                throw new InvalidPluginConfigurationException(
                    "Values of 'principal' and/or 'credentials' connection properties are invalid.", rootCause);
            }
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

    @NotNull
    private JBossASPaths getJBossASPaths() {
        Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();

        String homeDir = pluginConfiguration.getSimpleValue(ApplicationServerPluginConfigurationProperties.HOME_DIR,
            null);
        String serverHomeDir = pluginConfiguration.getSimpleValue(
            ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR, null);

        return new JBossASPaths(homeDir, serverHomeDir);
    }

    private boolean runningEmbedded() {
        Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();
        String namingUrl = pluginConfiguration.getSimpleValue(
            ApplicationServerPluginConfigurationProperties.NAMING_URL, null);
        return namingUrl == null;
    }

    @NotNull
    private File resolvePathRelativeToHomeDir(@NotNull String path) {
        File configDir = new File(path);
        if (!FileUtil.isAbsolutePath(path)) {
            Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
            String homeDir = pluginConfig.getSimple(ApplicationServerPluginConfigurationProperties.HOME_DIR)
                .getStringValue();
            configDir = new File(homeDir, path);
        }

        // BZ 903402 - get the real absolute path - under most conditions, it's the same thing, but if on windows
        //             the drive letter might not have been specified - this makes sure the drive letter is specified.
        return configDir.getAbsoluteFile();
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

    @NotNull
    public ResourceContext getResourceContext() {
        return this.resourceContext;
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {

        ApplicationServerSupportedOperations operation = Enum.valueOf(ApplicationServerSupportedOperations.class,
            name.toUpperCase());
        return this.operationDelegate.invoke(operation, parameters);
    }

    private void initializeEmsConnection() {
        Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();

        Configuration jmxConfig = new Configuration();

        String jbossHomeDir = pluginConfiguration.getSimpleValue(
            ApplicationServerPluginConfigurationProperties.HOME_DIR, null);

        String connectorDescriptorType;
        boolean runningEmbedded = runningEmbedded();
        if (runningEmbedded) {
            connectorDescriptorType = InternalVMTypeDescriptor.class.getName();
        } else {
            String connectorAddress = pluginConfiguration.getSimpleValue(
                ApplicationServerPluginConfigurationProperties.NAMING_URL, null);
            String connectorPrincipal = pluginConfiguration.getSimpleValue(
                ApplicationServerPluginConfigurationProperties.PRINCIPAL, null);
            String connectorCredentials = pluginConfiguration.getSimpleValue(
                ApplicationServerPluginConfigurationProperties.CREDENTIALS, null);

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
        JBPMWorkflowManager workflowManager = new JBPMWorkflowManager(contentContext, controlActionFacade,
            this.getJBossASPaths());
        return workflowManager;
    }

    private ControlActionFacade initControlActionFacade() {
        // Until the bugs get worked out of the calls back into the PC's operation framework, use the implementation
        // that will simply make calls directly in the plugin.
        // return new PluginContainerControlActionFacade(operationContext, this);
        return new InPluginControlActionFacade(this);
    }

    private File getConfigurationPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String serverHomeDir = getRequiredPropertyValue(pluginConfig,
            ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR);
        File configPath = resolvePathRelativeToHomeDir(serverHomeDir);
        if (!configPath.isDirectory()) {
            throw new InvalidPluginConfigurationException("Configuration path '" + configPath + "' does not exist.");
        }
        return configPath;
    }

    private static Serializable getMetric(ManagementView managementView, String metricName)
        throws ManagedComponentUtils.PropertyNotFoundException {
        // All metric names are expected to have the following syntax:
        // "<componentType>|<componentSubType>|<componentName>|<propertyName>"
        Matcher matcher = METRIC_NAME_PATTERN.matcher(metricName);
        if (!matcher.matches()) {
            throw new IllegalStateException("Metric name '" + metricName + "' does not match pattern '"
                + METRIC_NAME_PATTERN + "'.");
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
            //component = ManagedComponentUtils.getManagedComponent(managementView, componentType, componentName);
            try {
                component = managementView.getComponent(componentName, componentType);
            } catch (Exception e) {
                throw new IllegalStateException("Error fetching component " + componentName + "of type "
                    + componentType);
            }
        }

        return ManagedComponentUtils.getSimplePropertyValue(component, propertyName);
    }

    /**
     * Find the parent {@link ApplicationServerComponent} of the specified {@link ProfileServiceComponent}.
     *
     * @param profileServiceComponent
     * @return the parent {@link ApplicationServerComponent} or null
     */
    public static ApplicationServerComponent<?> findApplicationServerComponent(ProfileServiceComponent profileServiceComponent) {
        ProfileServiceComponent component = profileServiceComponent;
        while (component != null && !(component instanceof ApplicationServerComponent)) {
            ResourceComponent parent = component.getResourceContext().getParentResourceComponent();
            component = (parent instanceof ProfileServiceComponent) ? (ProfileServiceComponent) parent : null;
        }
        return (ApplicationServerComponent<?>) component;
    }
}
