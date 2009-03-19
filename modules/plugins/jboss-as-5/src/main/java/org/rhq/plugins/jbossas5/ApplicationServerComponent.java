 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.deployers.spi.management.deploy.ProgressEvent;
import org.jboss.deployers.spi.management.deploy.ProgressListener;
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
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DebugUtils;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

 /**
 * ResourceComponent for a JBossAS 5.x Server.
 *
 * @author Jason Dobies
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class ApplicationServerComponent
        implements ResourceComponent, CreateChildResourceFacet, ConfigurationFacet, ProgressListener
{
    private final Log log = LogFactory.getLog(this.getClass());

    // Constants  --------------------------------------------

    private static final String TEMPLATE_NAME_PROPERTY = "templateName";
    private static final String RESOURCE_NAME_PROPERTY = "resourceName";
    private static final String MANAGED_PROPERTY_GROUP = "managedPropertyGroup";

    private ResourceContext resourceContext;
    //private ContentContext contentContext;
    //private OperationContext operationContext;
    //private EventContext eventContext;

    public AvailabilityType getAvailability()
    {
        // TODO: Always returning UP is fine for Embedded, but we'll need to actually check avail for Enterprise.
        return AvailabilityType.UP;
    }

    public void start(ResourceContext resourceContext)
    {
        this.resourceContext = resourceContext;
        //this.eventContext = resourceContext.getEventContext();
        //this.contentContext = resourceContext.getContentContext();
        //this.operationContext = resourceContext.getOperationContext();
    }

    public void stop()
    {
        return;
    }

    // ConfigurationComponent  --------------------------------------------

    public Configuration loadResourceConfiguration()
    {
        /* Need to determine what we consider server configuration to return. Also need to understand
           what ComponentType the profile service would use to retrieve "server" level configuration.
        */

        return null;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport)
    {
        // See above comment on server configuration.
    }

    // CreateChildResourceFacet  --------------------------------------------

    public CreateResourceReport createResource(CreateResourceReport createResourceReport)
    {
        //ProfileServiceFactory.refreshCurrentProfileView();
        ResourceType resourceType = createResourceReport.getResourceType();
        if (resourceType.getCreationDataType() == ResourceCreationDataType.CONTENT)
            createContentBasedResource(createResourceReport, resourceType);
        else
            createConfigurationBasedResource(createResourceReport, resourceType);
        return createResourceReport;
    }

    // ProgressListener  --------------------------------------------

    public void progressEvent(ProgressEvent eventInfo) {
        log.debug(eventInfo);
    }

    private void handleMiscManagedProperties(Collection<PropertyDefinition> managedPropertyGroup,
                                             Map<String, ManagedProperty> managedProperties,
                                             Configuration pluginConfiguration)
    {
        for (PropertyDefinition propertyDefinition : managedPropertyGroup)
        {
            String propertyKey = propertyDefinition.getName();
            Property property = pluginConfiguration.get(propertyKey);
            ManagedProperty managedProperty = managedProperties.get(propertyKey);
            if (managedProperty != null && property != null)
            {
                PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(managedProperty.getMetaType());
                propertyAdapter.populateMetaValueFromProperty(property, managedProperty.getValue(), propertyDefinition);
            }
        }
    }

    private static String getResourceName(Configuration pluginConfig, Configuration resourceConfig)
    {
        PropertySimple resourceNameProp = pluginConfig.getSimple(RESOURCE_NAME_PROPERTY);
        if (resourceNameProp == null || resourceNameProp.getStringValue() == null)
            throw new IllegalStateException("Property [" + RESOURCE_NAME_PROPERTY
                    + "] is not defined in the default plugin configuration.");
        String resourceNamePropName = resourceNameProp.getStringValue();
        PropertySimple propToUseAsResourceName = resourceConfig.getSimple(resourceNamePropName);
        if (propToUseAsResourceName == null)
            throw new IllegalStateException("Property [" + resourceNamePropName
                    + "] is not defined in initial Resource configuration.");
        return propToUseAsResourceName.getStringValue();
    }

    private String getResourceKey(ResourceType resourceType, String resourceName)
    {
        ComponentType componentType = ConversionUtils.getComponentType(resourceType);
        if (componentType == null)
            throw new IllegalStateException("Unable to map " + resourceType + " to a ComponentType.");
        // TODO (ips): I think the key can just be the resource name.
        return componentType.getType() + ":" + componentType.getSubtype() + ":" + resourceName;
    }

    private void createConfigurationBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType)
    {
        Configuration defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
        Configuration resourceConfig = createResourceReport.getResourceConfiguration();
        String resourceName = getResourceName(defaultPluginConfig, resourceConfig);
        if (ProfileServiceFactory.isManagedComponent(resourceName, ConversionUtils.getComponentType(resourceType))) {
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setErrorMessage("A " + resourceType.getName() + " named '" + resourceName
                    + "' already exists.");
            return;
        }

        createResourceReport.setResourceName(resourceName);
        String resourceKey = getResourceKey(resourceType, resourceName);
        createResourceReport.setResourceKey(resourceKey);

        PropertySimple templateNameProperty = defaultPluginConfig.getSimple(TEMPLATE_NAME_PROPERTY);
        String templateName = templateNameProperty.getStringValue();

        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        DeploymentTemplateInfo template;
        try
        {
            template = managementView.getTemplate(templateName);
            Map<String, ManagedProperty> managedProperties = template.getProperties();
            Map<String, PropertySimple> customProps = ResourceComponentUtils.getCustomProperties(defaultPluginConfig);

            if (log.isDebugEnabled()) log.debug("BEFORE CREATE:\n" + DebugUtils.convertPropertiesToString(template));
            ConversionUtils.convertConfigurationToManagedProperties(managedProperties, resourceConfig, resourceType, customProps);
            if (log.isDebugEnabled()) log.debug("AFTER CREATE:\n" + DebugUtils.convertPropertiesToString(template));

            ConfigurationDefinition pluginConfigDef = resourceType.getPluginConfigurationDefinition();
            Collection<PropertyDefinition> managedPropertyGroup = pluginConfigDef.getPropertiesInGroup(MANAGED_PROPERTY_GROUP);
            handleMiscManagedProperties(managedPropertyGroup, managedProperties, defaultPluginConfig);
            try
            {
                managementView.applyTemplate(resourceName, template);
                managementView.process();
                createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
            }
            catch (Exception e)
            {
                log.error("Unable to apply template [" + templateName + "].", e);
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setException(e);
            }
        }
        catch (NoSuchDeploymentException e)
        {
            log.error("Unable to find template [" + templateName + "].", e);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setException(e);
        }
        catch (Exception e)
        {
            log.error("Unable to process create request", e);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setException(e);
        }
    }

    private static Configuration getDefaultPluginConfiguration(ResourceType resourceType) {
        ConfigurationTemplate pluginConfigDefaultTemplate =
                resourceType.getPluginConfigurationDefinition().getDefaultTemplate();
        return (pluginConfigDefaultTemplate != null) ?
                pluginConfigDefaultTemplate.createConfiguration() : new Configuration();
    }

    private void createContentBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType)
    {
        ResourcePackageDetails details = createResourceReport.getPackageDetails();
        PackageDetailsKey key = details.getKey();
        String archiveName = key.getName();

        try {
            if (DeploymentUtils.isCorrectExtension(resourceType, archiveName)) {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage("Incorrect extension specified on filename [" + archiveName +
                    "]");
                return;
            }

            File tempFile = new File(archiveName);

            DeploymentStatus status = DeploymentUtils.deployArchive(tempFile);

            if (status.getState() == DeploymentStatus.StateType.COMPLETED) {
                createResourceReport.setResourceName(archiveName);
                createResourceReport.setResourceKey(archiveName);
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
}

