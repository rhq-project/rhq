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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jboss.jbossnetwork.product.jbpm.handlers.ControlActionFacade;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.deployers.spi.management.deploy.ProgressEvent;
import org.jboss.deployers.spi.management.deploy.ProgressListener;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;
import org.jboss.on.common.jbossas.JBossASPaths;
import org.jboss.profileservice.spi.NoSuchDeploymentException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.content.PackageDetailsKey;
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
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.rhq.plugins.jbossas5.connection.LocalProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.RemoteProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DebugUtils;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;
import org.rhq.plugins.jbossas5.util.JBossASContentFacetDelegate;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

/**
 * ResourceComponent for a JBoss AS, 5.1.0.CR1 or later, Server.
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class ApplicationServerComponent implements ResourceComponent, ProfileServiceComponent,
    CreateChildResourceFacet, MeasurementFacet, ConfigurationFacet, ProgressListener, ContentFacet {
    private static final String MANAGED_PROPERTY_GROUP = "managedPropertyGroup";

    private static final Pattern METRIC_NAME_PATTERN = Pattern.compile("(.*)\\|(.*)\\|(.*)\\|(.*)");

    private static final String HOME_DIR_PROP_NAME = "homeDir";
    private static final String SERVER_HOME_DIR_PROP_NAME = "serverHomeDir";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;
    private ProfileServiceConnection connection;
    private File deployDirectory;

    private JBossASContentFacetDelegate contentFacetDelegate;

    //private LogFileEventResourceComponentHelper logFileEventDelegate;

    public AvailabilityType getAvailability() {
        connect();
        // TODO: Ping the connection to make sure it's not defunct.
        return (this.connection != null) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public void start(ResourceContext resourceContext) {
        this.resourceContext = resourceContext;
        connect();
        //this.logFileEventDelegate = new LogFileEventResourceComponentHelper(this.resourceContext);
        //this.logFileEventDelegate.startLogFileEventPollers();

        JBossASPaths paths = getJBossASPaths();
        ContentContext contentContext = resourceContext.getContentContext();

        //TODO define the control action facade once we support operations on the app server.
        //OperationContext operationContext = resourceContext.getOperationContext();
        //
        //ControlActionFacade controlActionFacade = 
        //	new PluginContainerControlActionFacade(operationContext, this);
        ControlActionFacade controlActionFacade = null;

        JBPMWorkflowManager workflowManager = new JBPMWorkflowManager(contentContext, controlActionFacade, paths);

        contentFacetDelegate = new JBossASContentFacetDelegate(workflowManager);
    }

    public void stop() {
        //this.logFileEventDelegate.stopLogFileEventPollers();
    }

    // ------------ MeasurementFacet Implementation ------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        ManagementView managementView = getConnection().getManagementView();
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            try {
                // Metric names are expected to have the following syntax:
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
        /* Need to determine what we consider server configuration to return. Also need to understand
           what ComponentType the profile service would use to retrieve "server" level configuration.
        */

        return null;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        // See above comment on server configuration.
    }

    // CreateChildResourceFacet  --------------------------------------------

    public CreateResourceReport createResource(CreateResourceReport createResourceReport) {
        //ProfileServiceFactory.refreshCurrentProfileView();
        ResourceType resourceType = createResourceReport.getResourceType();
        if (resourceType.getCreationDataType() == ResourceCreationDataType.CONTENT)
            createContentBasedResource(createResourceReport, resourceType);
        else
            createConfigurationBasedResource(createResourceReport, resourceType);
        return createResourceReport;
    }

    public File getDeployDirectory() {
        if (this.deployDirectory == null)
            this.deployDirectory = computeDeployDirectory();
        return this.deployDirectory;
    }

    // ProgressListener  --------------------------------------------

    public void progressEvent(ProgressEvent eventInfo) {
        log.debug(eventInfo);
    }

    @Nullable
    public ProfileServiceConnection getConnection() {
        connect();
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

    private void connect() {
        if (this.connection != null)
            return;
        // TODO: Check for a defunct connection and if found try to reconnect.
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String namingURL = pluginConfig.getSimpleValue(PluginConfigPropNames.NAMING_URL, null);
        ProfileServiceConnectionProvider connectionProvider;
        if (namingURL != null) {
            validateNamingURL(namingURL);
            String principal = pluginConfig.getSimpleValue(PluginConfigPropNames.PRINCIPAL, null);
            String credentials = pluginConfig.getSimpleValue(PluginConfigPropNames.CREDENTIALS, null);
            connectionProvider = new RemoteProfileServiceConnectionProvider(namingURL, principal, credentials);
        } else {
            connectionProvider = new LocalProfileServiceConnectionProvider();
        }
        try {
            this.connection = connectionProvider.connect();
        } catch (RuntimeException e) {
            log.debug("Failed to connect to Profile Service.", e);
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

    private static String getResourceName(Configuration pluginConfig, Configuration resourceConfig) {
        PropertySimple resourceNameProp = pluginConfig.getSimple(ManagedComponentComponent.Config.RESOURCE_NAME);
        if (resourceNameProp == null || resourceNameProp.getStringValue() == null)
            throw new IllegalStateException("Property [" + ManagedComponentComponent.Config.RESOURCE_NAME
                + "] is not defined in the default plugin configuration.");
        String resourceNamePropName = resourceNameProp.getStringValue();
        PropertySimple propToUseAsResourceName = resourceConfig.getSimple(resourceNamePropName);
        if (propToUseAsResourceName == null)
            throw new IllegalStateException("Property [" + resourceNamePropName
                + "] is not defined in initial Resource configuration.");
        return propToUseAsResourceName.getStringValue();
    }

    private String getResourceKey(ResourceType resourceType, String resourceName) {
        ComponentType componentType = ConversionUtils.getComponentType(resourceType);
        if (componentType == null)
            throw new IllegalStateException("Unable to map " + resourceType + " to a ComponentType.");
        // TODO (ips): I think the key can just be the resource name.
        return componentType.getType() + ":" + componentType.getSubtype() + ":" + resourceName;
    }

    private void createConfigurationBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType) {
        Configuration defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
        Configuration resourceConfig = createResourceReport.getResourceConfiguration();
        String resourceName = getResourceName(defaultPluginConfig, resourceConfig);
        ComponentType componentType = ConversionUtils.getComponentType(resourceType);
        ManagementView managementView = getConnection().getManagementView();
        if (ManagedComponentUtils.isManagedComponent(managementView, resourceName, componentType)) {
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setErrorMessage("A " + resourceType.getName() + " named '" + resourceName
                + "' already exists.");
            return;
        }

        createResourceReport.setResourceName(resourceName);
        String resourceKey = getResourceKey(resourceType, resourceName);
        createResourceReport.setResourceKey(resourceKey);

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
                managementView.applyTemplate(resourceName, template);
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
        ResourcePackageDetails details = createResourceReport.getPackageDetails();
        PackageDetailsKey key = details.getKey();
        // This is the full path to a temporary file which was written by the UI layer.
        String archivePath = key.getName();

        try {
            File archiveFile = new File(archivePath);

            if (!DeploymentUtils.hasCorrectExtension(archiveFile, resourceType)) {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage("Incorrect extension specified on filename [" + archivePath + "]");
                return;
            }

            abortIfApplicationAlreadyDeployed(resourceType, archiveFile);

            Configuration deployTimeConfig = details.getDeploymentTimeConfiguration();
            @SuppressWarnings( { "ConstantConditions" })
            boolean deployExploded = deployTimeConfig.getSimple("deployExploded").getBooleanValue();

            DeploymentManager deploymentManager = getConnection().getDeploymentManager();
            DeploymentStatus status = DeploymentUtils.deployArchive(deploymentManager, archiveFile,
                getDeployDirectory(), deployExploded);

            if (status.getState() == DeploymentStatus.StateType.COMPLETED) {
                createResourceReport.setResourceName(archivePath);
                createResourceReport.setResourceKey(archivePath);
                createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
            } else {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage(status.getMessage());
                //noinspection ThrowableResultOfMethodCallIgnored
                createResourceReport.setException(status.getFailure());
            }
        } catch (Throwable t) {
            log.error("Error deploying application for report: " + createResourceReport, t);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setException(t);
        }
    }

    private static Configuration getDefaultPluginConfiguration(ResourceType resourceType) {
        ConfigurationTemplate pluginConfigDefaultTemplate = resourceType.getPluginConfigurationDefinition()
            .getDefaultTemplate();
        return (pluginConfigDefaultTemplate != null) ? pluginConfigDefaultTemplate.createConfiguration()
            : new Configuration();
    }

    private void abortIfApplicationAlreadyDeployed(ResourceType resourceType, File archiveFile) throws Exception {
        String archiveFileName = archiveFile.getName();
        KnownDeploymentTypes deploymentType = ConversionUtils.getDeploymentType(resourceType);
        String deploymentTypeString = deploymentType.getType();
        ManagementView managementView = getConnection().getManagementView();
        managementView.load();
        Set<ManagedDeployment> managedDeployments = managementView.getDeploymentsForType(deploymentTypeString);
        for (ManagedDeployment managedDeployment : managedDeployments) {
            if (managedDeployment.getSimpleName().equals(archiveFileName))
                throw new IllegalArgumentException("An application named '" + archiveFileName
                    + "' is already deployed.");
        }
    }

    private File computeDeployDirectory() {
        ManagementView managementView = getConnection().getManagementView();
        Set<ManagedDeployment> warDeployments;
        try {
            warDeployments = managementView.getDeploymentsForType(KnownDeploymentTypes.JavaEEWebApplication.getType());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        ManagedDeployment standaloneWarDeployment = null;
        for (ManagedDeployment warDeployment : warDeployments) {
            if (warDeployment.getParent() == null) {
                standaloneWarDeployment = warDeployment;
                break;
            }
        }
        if (standaloneWarDeployment == null)
            // This could happen if no standalone WARs, including the admin console WAR, have been fully deployed yet.
            return null;
        log.debug("Standalone WAR deployment: " + standaloneWarDeployment.getName());
        URL warUrl;
        try {
            warUrl = new URL(standaloneWarDeployment.getName());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        File warFile = new File(warUrl.getPath());
        File deployDir = warFile.getParentFile();
        log.debug(">>>>> Deploy directory: " + deployDir);
        return deployDir;
    }

    @NotNull
    private JBossASPaths getJBossASPaths() {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        String homeDir = pluginConfiguration.getSimpleValue(HOME_DIR_PROP_NAME, null);
        String serverHomeDir = pluginConfiguration.getSimpleValue(SERVER_HOME_DIR_PROP_NAME, null);

        return new JBossASPaths(homeDir, serverHomeDir);
    }

    @NotNull
    static File resolvePathRelativeToHomeDir(Configuration pluginConfig, @NotNull String path) {
        File configDir = new File(path);
        if (!configDir.isAbsolute()) {
            String homeDir = pluginConfig.getSimple(PluginConfigPropNames.HOME_DIR).getStringValue();
            configDir = new File(homeDir, path);
        }

        return configDir;
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

    static abstract class PluginConfigPropNames {
        static final String SERVER_NAME = "serverName";
        static final String NAMING_URL = "namingURL";
        static final String PRINCIPAL = "principal";
        static final String CREDENTIALS = "credentials";
        static final String HOME_DIR = "homeDir";
        static final String SERVER_HOME_DIR = "serverHomeDir";
        static final String JAVA_HOME = "javaHome";
        static final String BIND_ADDRESS = "bindAddress";
    }
}
