package org.rhq.plugins.jbossas5.helper;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.profileservice.spi.NoSuchDeploymentException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.jbossas5.ApplicationServerPluginConfigurationProperties;
import org.rhq.plugins.jbossas5.ManagedComponentComponent;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.deploy.Deployer;
import org.rhq.plugins.jbossas5.deploy.LocalDownloader;
import org.rhq.plugins.jbossas5.deploy.ManagedComponentDeployer;
import org.rhq.plugins.jbossas5.deploy.PackageDownloader;
import org.rhq.plugins.jbossas5.deploy.RemoteDownloader;
import org.rhq.plugins.jbossas5.deploy.ScriptDeployer;
import org.rhq.plugins.jbossas5.script.ScriptComponent;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DebugUtils;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;
import org.rhq.plugins.jbossas5.util.ResourceComponentUtils;

/**
 * @author Ian Springer
 */
public class CreateChildResourceFacetDelegate implements CreateChildResourceFacet {
    private static final String MANAGED_PROPERTY_GROUP = "managedPropertyGroup";

    private final Log log = LogFactory.getLog(this.getClass());

    private ProfileServiceComponent component;
    private ResourceContext<?> parentResourceContext;

    public CreateChildResourceFacetDelegate(ProfileServiceComponent component, ResourceContext<?> parentResourceContext) {
        this.component = component;
        this.parentResourceContext = parentResourceContext;
    }

    /**
     * @deprecated This constructor is deprecated because it does not handle properly the SHA256 computation for
     * exploded content. If used, this constructor will lead to a calculation of the SHA256 for exploded content
     * based on exploded files. This method of computation is not preferred due to changes in content archive
     * matching. Replaced by (@link {@link #CreateChildResourceFacetDelegate(ProfileServiceComponent, ResourceContext)}
     *
     * @param component component
     */
    @Deprecated
    public CreateChildResourceFacetDelegate(ProfileServiceComponent component) {
        this.component = component;
    }

    public CreateResourceReport createResource(CreateResourceReport createResourceReport) {
        // ProfileServiceFactory.refreshCurrentProfileView();
        ResourceType resourceType = createResourceReport.getResourceType();
        if (resourceType.getCreationDataType() == ResourceCreationDataType.CONTENT)
            createContentBasedResource(createResourceReport, resourceType);
        else
            createConfigurationBasedResource(createResourceReport, resourceType);
        return createResourceReport;
    }

    void createConfigurationBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType) {
        Configuration defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
        Configuration resourceConfig = createResourceReport.getResourceConfiguration();
        String componentName = getComponentName(defaultPluginConfig, resourceConfig);
        ComponentType componentType = ConversionUtils.getComponentType(resourceType);
        ManagementView managementView = this.component.getConnection().getManagementView();
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
            ConversionUtils.convertConfigurationToManagedProperties(managedProperties, resourceConfig,
                resourceType.getResourceConfigurationDefinition(),
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
        getDeployer(resourceType).deploy(createResourceReport, resourceType);
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


    private Deployer getDeployer(ResourceType resourceType) {
        ProfileServiceConnection profileServiceConnection = this.component.getConnection();

        PackageDownloader downloader = runningEmbedded() ? new LocalDownloader() : new RemoteDownloader(
            component.getResourceContext(), false, profileServiceConnection);

        if (ScriptComponent.TYPE_NAME.equals(resourceType.getName())) {
            String jbossHome = component.getResourceContext().getPluginConfiguration()
                .getSimpleValue(ApplicationServerPluginConfigurationProperties.HOME_DIR, null);
            SystemInfo systemInfo  = component.getResourceContext().getSystemInformation();
            return new ScriptDeployer(jbossHome, systemInfo, downloader);
        } else {
            return new ManagedComponentDeployer(profileServiceConnection, downloader, parentResourceContext);
        }
    }

    private boolean runningEmbedded() {
        Configuration pluginConfiguration = this.component.getResourceContext().getPluginConfiguration();
        String namingUrl = pluginConfiguration.getSimpleValue(ApplicationServerPluginConfigurationProperties.NAMING_URL,
                null);
        return namingUrl == null;
    }

    private static Configuration getDefaultPluginConfiguration(ResourceType resourceType) {
        ConfigurationTemplate pluginConfigDefaultTemplate = resourceType.getPluginConfigurationDefinition()
            .getDefaultTemplate();
        return (pluginConfigDefaultTemplate != null) ? pluginConfigDefaultTemplate.createConfiguration()
            : new Configuration();
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
}
