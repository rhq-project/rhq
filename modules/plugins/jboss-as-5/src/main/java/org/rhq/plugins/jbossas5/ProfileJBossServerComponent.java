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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.*;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
//import org.rhq.core.pluginapi.content.ContentContext;
//import org.rhq.core.pluginapi.operation.OperationContext;
//import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.rhq.plugins.jbossas5.util.ConversionUtil;

import java.util.Collection;
import java.util.Map;
import java.io.*;

/**
 * Component for JBoss AS Server.
 *
 * @author: Jason Dobies
 * @author: Mark Spritzler
 */
public class ProfileJBossServerComponent
        implements ResourceComponent, CreateChildResourceFacet, ConfigurationFacet, DeleteResourceFacet, ProgressListener
{
    private final Log LOG = LogFactory.getLog(ProfileJBossServerComponent.class);

    // Constants  --------------------------------------------

    private static final String TEMPLATE_NAME_PROPERTY = "templateName";
    private static final String RESOURCE_NAME_PROPERTY = "resourceName";
    private static final String MANAGED_PROPERTY_GROUP = "managedPropertyGroup";

    private static final String RESOURCE_TYPE_EAR = "Enterprise Application (EAR)";
    private static final String RESOURCE_TYPE_WAR = "Web Application (WAR)";
    private static final String RESOURCE_TYPE_JAR = "EJB Application (JAR)";

    private ResourceContext resourceContext;
    //private ContentContext contentContext;
    //private OperationContext operationContext;
    //private EventContext eventContext;


    public AvailabilityType getAvailability()
    {
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

    }

    public boolean isConnected()
    {
        // Not fully sure what this method's purpose is.
        return false;
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

    // ServiceFactoryComponent  --------------------------------------------


    public CreateResourceReport createResource(CreateResourceReport createResourceReport)
    {

        ResourceType resourceType = createResourceReport.getResourceType();

        if (resourceType.getName().equals(RESOURCE_TYPE_EAR) || resourceType.getName().equals(RESOURCE_TYPE_WAR) || resourceType.getName().equals(RESOURCE_TYPE_JAR))
        {
            createContentBasedResource(createResourceReport, resourceType);
        }
        else
        {
            createConfigurationBasedResource(createResourceReport, resourceType);
        }

        return createResourceReport;
    }

    public void deleteResource()
    {
        // Deletes are done on the resource itself.
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
                propertyAdapter.setMetaValues(property, (MetaValue) managedProperty.getValue(), propertyDefinition);
            }
        }
    }

    private String getResourceName(PropertySimple resourceNameProperty, Configuration configuration)
    {
        String resourceName = "bar";

        if (resourceNameProperty != null)
        {
            String value = resourceNameProperty.getStringValue();
            if (value != null)
            {
                PropertySimple propertyToUseAsResourceName = configuration.getSimple(value);
                if (propertyToUseAsResourceName != null)
                {
                    resourceName = propertyToUseAsResourceName.getStringValue();
                }
            }
        }
        return resourceName;
    }

    private String getResourceKey(ResourceType resourceType, String resourceName)
    {
        String resourceKey = "foo";
        ComponentType componentType = ConversionUtil.getComponentType(resourceType);
        if (componentType != null)
        {
            resourceKey = componentType.getType() + ":" + componentType.getSubtype() + ":" + resourceName;
        }
        return resourceKey;
    }

    private void createConfigurationBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType)
    {
        Configuration configuration = createResourceReport.getResourceConfiguration();
        ManagementView profileView = ProfileServiceFactory.getCurrentProfileView();

        // Convert resource type into template name
        ConfigurationDefinition configDef = resourceType.getPluginConfigurationDefinition();
        ConfigurationTemplate template = configDef.getDefaultTemplate();
        Configuration pluginConfiguration = template.getConfiguration();
        PropertySimple resourceNameProperty = pluginConfiguration.getSimple(RESOURCE_NAME_PROPERTY);
        PropertySimple templateProperty = pluginConfiguration.getSimple(TEMPLATE_NAME_PROPERTY);
        Collection<PropertyDefinition> managedPropertyGroup = configDef.getPropertiesInGroup(MANAGED_PROPERTY_GROUP);

        String resourceName = getResourceName(resourceNameProperty, configuration);
        if (!ProfileServiceFactory.isManagedComponent(resourceName, ConversionUtil.getComponentType(resourceType)))
        {

            createResourceReport.setResourceName(resourceName);

            String resourceKey = getResourceKey(resourceType, resourceName);
            createResourceReport.setResourceKey(resourceKey);

            String templateName = templateProperty.getStringValue();

            DeploymentTemplateInfo deploymentTemplateInfo;
            try
            {
                deploymentTemplateInfo = profileView.getTemplate(templateName);
                Map<String, ManagedProperty> managedProperties = deploymentTemplateInfo.getProperties();

                ConversionUtil.convertConfigurationToManagedProperties(managedProperties, configuration, resourceType);
                handleMiscManagedProperties(managedPropertyGroup, managedProperties, pluginConfiguration);

                try
                {
                    profileView.applyTemplate(ManagedDeployment.DeploymentPhase.APPLICATION, resourceName, deploymentTemplateInfo);
                    profileView.process();
                    createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
                }
                catch (Exception e)
                {
                    LOG.error("Unable to apply Template and process through Profile View", e);
                    createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                    createResourceReport.setException(e);
                }

            }
            catch (NoSuchDeploymentException e)
            {
                LOG.error("Unable to find Template " + templateName + " In profile view", e);
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setException(e);
            }
            catch (Exception e)
            {
                LOG.error("Unable to process create request", e);
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setException(e);
            }
        }
        else
        {
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setErrorMessage("Duplicate JNDI Name, a resource with that name already exists");
        }
    }

    private void createContentBasedResource(CreateResourceReport createResourceReport, ResourceType resourceType)
    {
        ResourcePackageDetails details = createResourceReport.getPackageDetails();
        PackageDetailsKey key = details.getKey();
        String archiveName = key.getName();

        try {
            if (isCorrectExtension(resourceType, archiveName)) {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage("Incorrect extension specified on filename [" + archiveName +
                    "]");
                return;
            }

            File tempFile = new File(archiveName);

            deployArchive(tempFile, createResourceReport);

            createResourceReport.setResourceKey(archiveName);
            createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
        } catch (Throwable t) {
            LOG.error("Error deploying application for report: " + createResourceReport, t);
            createResourceReport.setException(t);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
        }
    }

    private boolean isCorrectExtension(ResourceType resourceType, String archiveName)
    {
        String resourceTypeName = resourceType.getName();
        String expectedExtension;
        if (resourceTypeName.equals(RESOURCE_TYPE_EAR)) {
            expectedExtension = "ear";
        } else if (resourceTypeName.equals(RESOURCE_TYPE_WAR)){
            expectedExtension = "war";
        } else {
            expectedExtension = "jar";
        }

        int lastPeriod = archiveName.lastIndexOf(".");
        String extension = archiveName.substring(lastPeriod + 1);
        return (lastPeriod == -1 || !expectedExtension.equals(extension));
    }

    private void deployArchive(File tempFile, CreateResourceReport createResourceReport) throws Exception
    {
        DeploymentManager manager = ProfileServiceFactory.getDeploymentManager();
        String archiveName = tempFile.getName();
        DeploymentProgress progress = manager.distribute(archiveName, ManagedDeployment.DeploymentPhase.APPLICATION, tempFile.toURL());
        progress.addProgressListener(this);
        progress.run();        

        DeploymentStatus status = progress.getDeploymentStatus();
        if (status.getState().equals(DeploymentStatus.StateType.FAILED))
        {
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            Exception exceptionThrown = status.getFailure();
            if (exceptionThrown != null)
            {
                createResourceReport.setException(exceptionThrown);
                LOG.error("", exceptionThrown);
            }
            else
            {
                createResourceReport.setErrorMessage(status.getMessage());
            }
        }

        String[] names = {archiveName};
        progress = manager.start(ManagedDeployment.DeploymentPhase.APPLICATION, names);
        progress.addProgressListener(this);
        progress.run();

        status = progress.getDeploymentStatus();
        if (status.getState().equals(DeploymentStatus.StateType.FAILED))
        {
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setErrorMessage(status.getMessage());
        }
    }

    public void progressEvent(ProgressEvent eventInfo) {
        
    }
}

