/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.tool.plugindoc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.ContentMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.agent.metadata.MetricsMetadataParser;
import org.rhq.core.clientapi.agent.metadata.OperationsMetadataParser;
import org.rhq.core.clientapi.agent.metadata.SubCategoriesMetadataParser;
import org.rhq.core.clientapi.descriptor.plugin.ContentDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.MetricDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.OperationDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ParentResourceType;
import org.rhq.core.clientapi.descriptor.plugin.PlatformDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ProcessScanDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceCreateDeletePolicy;
import org.rhq.core.clientapi.descriptor.plugin.ResourceCreationData;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.RunsInsideType;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.SubCategoryDescriptor;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Ian Springer
 */
public class PluginDescriptorProcessor {
    private final Log log = LogFactory.getLog(PluginDescriptorProcessor.class);

    private PluginDescriptor pluginDescriptor;
    private Set<ResourceType> resourceTypes = new LinkedHashSet<ResourceType>();

    public PluginDescriptorProcessor(PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    public Set<ResourceType> processPluginDescriptor() throws InvalidPluginDescriptorException {
        for (PlatformDescriptor serverDescriptor : pluginDescriptor.getPlatforms()) {
            parsePlatformDescriptor(serverDescriptor);
        }

        for (ServerDescriptor serverDescriptor : pluginDescriptor.getServers()) {
            parseServerDescriptor(serverDescriptor, null);
        }

        for (ServiceDescriptor serviceDescriptor : pluginDescriptor.getServices()) {
            parseServiceDescriptor(serviceDescriptor, null);
        }

        return this.resourceTypes;
    }

    private ResourceType parsePlatformDescriptor(PlatformDescriptor platformDescriptor)
        throws InvalidPluginDescriptorException {
        ResourceType platformResourceType = new ResourceType(platformDescriptor.getName(), pluginDescriptor.getName(),
            ResourceCategory.PLATFORM, null);
        platformResourceType.setSubCategory(SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
            platformResourceType, platformDescriptor.getSubCategory()));
        platformResourceType.setDescription(platformDescriptor.getDescription());
        log.debug("Parsed platform resource type: " + platformResourceType);
        parseResourceDescriptor(platformDescriptor, platformResourceType, null, null, null);

        if ((platformResourceType.getProcessScans() != null) && (platformResourceType.getProcessScans().size() > 0)) {
            log.warn("Platforms are not auto-discovered via process scans. "
                + "The <process-scan> elements will be ignored in resource type: " + platformResourceType);
        }

        return platformResourceType;
    }

    private ResourceType parseServerDescriptor(ServerDescriptor serverDescriptor, ResourceType parentServerType)
        throws InvalidPluginDescriptorException {
        ResourceType serverResourceType;
        String sourcePlugin = serverDescriptor.getSourcePlugin();
        String sourceServer = serverDescriptor.getSourceType();

        sourcePlugin = (sourcePlugin == null) ? "" : sourcePlugin.trim();
        sourceServer = (sourceServer == null) ? "" : sourceServer.trim();

        if ((sourcePlugin.length() == 0) && (sourceServer.length() == 0)) {
            // not using Embedded extension model
            serverResourceType = new ResourceType(serverDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVER, parentServerType);
            serverResourceType.setSubCategory(SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
                serverResourceType, serverDescriptor.getSubCategory()));
            serverResourceType.setDescription(serverDescriptor.getDescription());
            serverResourceType.setCreationDataType(convertCreationDataType(serverDescriptor.getCreationDataType()));
            serverResourceType
                .setCreateDeletePolicy(convertCreateDeletePolicy(serverDescriptor.getCreateDeletePolicy()));
            serverResourceType.setSingleton(serverDescriptor.isSingleton());

            log.debug("Parsed server resource type: " + serverResourceType);
            parseResourceDescriptor(serverDescriptor, serverResourceType, null, null, null);
        } else if ((sourcePlugin.length() > 0) && (sourceServer.length() > 0)) {
            // using Embedded extension model - the defined type is actually a copy of another plugin's server type
            // TODO

            serverResourceType = new ResourceType(serverDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVER, parentServerType);

            // let the plugin writer override these, if not, pick up the source type's values
            serverResourceType.setDescription(serverDescriptor.getDescription());
            serverResourceType.setSubCategory(SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
                serverResourceType, serverDescriptor.getSubCategory()));
            serverResourceType.setCreationDataType(convertCreationDataType(serverDescriptor.getCreationDataType()));
            serverResourceType
                .setCreateDeletePolicy(convertCreateDeletePolicy(serverDescriptor.getCreateDeletePolicy()));
            serverResourceType.setSingleton(serverDescriptor.isSingleton());

            String discoveryClass = getFullyQualifiedComponentClassName(pluginDescriptor.getPackage(), serverDescriptor
                .getDiscovery());
            String componentClass = getFullyQualifiedComponentClassName(pluginDescriptor.getPackage(), serverDescriptor
                .getClazz());
        } else {
            // this should never happen - the XML parser should have failed to even get this far
            throw new InvalidPluginDescriptorException("Both sourcePlugin and sourceType must be defined: "
                + serverDescriptor.getName());
        }

        // now see if we are using the Injection extension model
        // if so, we need to inject the new resource type as a child to the parent plugin's types
        RunsInsideType runsInside = serverDescriptor.getRunsInside();
        if (runsInside != null) {
            List<ParentResourceType> parentTypesDescriptor = runsInside.getParentResourceType();
            for (ParentResourceType parentTypeDescriptor : parentTypesDescriptor) {
                ResourceType parentResourceType = new ResourceType(parentTypeDescriptor.getName(), parentTypeDescriptor
                    .getPlugin(), ResourceCategory.SERVER, ResourceType.ANY_PLATFORM_TYPE);
                serverResourceType.addParentResourceType(parentResourceType);
            }
        }

        // Look for child server types
        for (ServerDescriptor childServerDescriptor : serverDescriptor.getServers()) {
            parseServerDescriptor(childServerDescriptor, serverResourceType);
        }

        return serverResourceType;
    }

    private ResourceType parseServiceDescriptor(ServiceDescriptor serviceDescriptor, ResourceType parentType)
        throws InvalidPluginDescriptorException {
        ResourceType serviceResourceType;
        String sourcePlugin = serviceDescriptor.getSourcePlugin();
        String sourceService = serviceDescriptor.getSourceType();

        sourcePlugin = (sourcePlugin == null) ? "" : sourcePlugin.trim();
        sourceService = (sourceService == null) ? "" : sourceService.trim();

        if ((sourcePlugin.length() == 0) && (sourceService.length() == 0)) {
            // not using Embedded extension model
            serviceResourceType = new ResourceType(serviceDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVICE, parentType);
            serviceResourceType.setSubCategory(SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
                serviceResourceType, serviceDescriptor.getSubCategory()));
            serviceResourceType.setDescription(serviceDescriptor.getDescription());
            serviceResourceType.setCreationDataType(convertCreationDataType(serviceDescriptor.getCreationDataType()));
            serviceResourceType.setCreateDeletePolicy(convertCreateDeletePolicy(serviceDescriptor
                .getCreateDeletePolicy()));
            serviceResourceType.setSingleton(serviceDescriptor.isSingleton());

            log.debug("Parsed service resource type: " + serviceResourceType);
            parseResourceDescriptor(serviceDescriptor, serviceResourceType, null, null, null);

            if ((serviceResourceType.getProcessScans() != null) && (serviceResourceType.getProcessScans().size() > 0)) {
                log.warn("Child services are not auto-discovered via process scans. "
                    + "The <process-scan> elements will be ignored in resource type: " + serviceResourceType);
            }
        } else if (sourcePlugin.length() > 0) {
            // using Embedded extension model - the defined type is actually a copy of another plugin's service type
            // TODO

            ServiceDescriptor sourceServiceDescriptor;
            if (sourceService.length() == 0) {
                sourceServiceDescriptor = serviceDescriptor;
            }

            serviceResourceType = new ResourceType(serviceDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVICE, parentType);

            // let the plugin writer override these, if not, pick up the source type's values
            serviceResourceType.setDescription(serviceDescriptor.getDescription());
            serviceResourceType.setSubCategory(SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
                serviceResourceType, serviceDescriptor.getSubCategory()));
            serviceResourceType.setCreationDataType(convertCreationDataType(serviceDescriptor.getCreationDataType()));
            serviceResourceType.setCreateDeletePolicy(convertCreateDeletePolicy(serviceDescriptor
                .getCreateDeletePolicy()));
            serviceResourceType.setSingleton(serviceDescriptor.isSingleton());

            String pluginPackage = ""; // TODO
            String discoveryClass = getFullyQualifiedComponentClassName(pluginPackage, serviceDescriptor.getDiscovery());
            String componentClass = getFullyQualifiedComponentClassName(pluginPackage, serviceDescriptor.getClazz());
        } else {
            // this should never happen - the XML parser should have failed to even get this far
            throw new InvalidPluginDescriptorException("Both sourcePlugin and sourceType must be defined: "
                + serviceDescriptor.getName());
        }

        // now see if we are using the Injection extension model
        // if so, we need to inject the new resource type as a child to the parent plugin's types
        // note that the Injection model only allows for root-level services to be injected
        if (parentType == null) {
            RunsInsideType runsInside = serviceDescriptor.getRunsInside();
            if (runsInside != null) {
                List<ParentResourceType> parentTypesDescriptor = runsInside.getParentResourceType();
                for (ParentResourceType parentTypeDescriptor : parentTypesDescriptor) {
                    ResourceType parentResourceType = new ResourceType(parentTypeDescriptor.getName(),
                        parentTypeDescriptor.getPlugin(), ResourceCategory.SERVER, ResourceType.ANY_PLATFORM_TYPE);
                    serviceResourceType.addParentResourceType(parentResourceType);
                }
            }
        }

        return serviceResourceType;
    }

    /**
     * Parses the resource descriptor and registers the type and its component classes.
     *
     * @param  resourceDescriptor
     * @param  resourceType       the new resource type that will get the new data found in the descriptor added to it
     * @param  discoveryClass     if <code>null</code>, will get the classname from the resourceDescriptor; otherwise,
     *                            is used as-is
     * @param  componentClass     if <code>null</code>, will get the classname from the resourceDescriptor; otherwise,
     *                            is used as-is
     *
     * @throws InvalidPluginDescriptorException
     */
    private void parseResourceDescriptor(ResourceDescriptor resourceDescriptor, ResourceType resourceType,
        String discoveryClass, String componentClass, String sourcePlugin) throws InvalidPluginDescriptorException {
        // 0) classes
        // 1) Plugin config
        // 2) Resource config
        // 3) Metrics
        // 4) Control operations
        // 5) Process matches (for process scan auto-discovery)
        // 6) Artifacts
        // 7) Sub categories

        if (discoveryClass == null) {
            discoveryClass = getFullyQualifiedComponentClassName(pluginDescriptor.getPackage(), resourceDescriptor
                .getDiscovery());
        }

        if (componentClass == null) {
            componentClass = getFullyQualifiedComponentClassName(pluginDescriptor.getPackage(), resourceDescriptor
                .getClazz());
        }

        try {
            resourceType.setPlugin(pluginDescriptor.getName());

            if (resourceDescriptor.getPluginConfiguration() != null) {
                resourceType.setPluginConfigurationDefinition(ConfigurationMetadataParser.parse(resourceType.getName(),
                    resourceDescriptor.getPluginConfiguration()));
            }

            if (resourceDescriptor.getResourceConfiguration() != null) {
                resourceType.setResourceConfigurationDefinition(ConfigurationMetadataParser.parse(resourceType
                    .getName(), resourceDescriptor.getResourceConfiguration()));
            }

            int displayPosition = 1;
            for (MetricDescriptor metricDescriptor : resourceDescriptor.getMetric()) {
                List<MeasurementDefinition> measurementDefinitions = MetricsMetadataParser.parseMetricsMetadata(
                    metricDescriptor, resourceType);
                for (MeasurementDefinition measurementDefinition : measurementDefinitions) {
                    measurementDefinition.setDisplayOrder(displayPosition++);
                    resourceType.addMetricDefinition(measurementDefinition);
                }
            }

            for (OperationDescriptor operationDescriptor : resourceDescriptor.getOperation()) {
                resourceType.addOperationDefinition(OperationsMetadataParser
                    .parseOperationDescriptor(operationDescriptor));
            }

            for (ProcessScanDescriptor processMatch : resourceDescriptor.getProcessScan()) {
                resourceType.addProcessScan(new ProcessScan(processMatch.getQuery(), processMatch.getName()));
            }

            for (ContentDescriptor contentDescriptor : resourceDescriptor.getContent()) {
                resourceType.addPackageType(ContentMetadataParser.parseContentDescriptor(contentDescriptor));
            }

            // TODO not sure we really want this wrapping <subcategories> element since no one else uses it
            if (resourceDescriptor.getSubcategories() != null) {
                for (SubCategoryDescriptor subCategoryDescriptor : resourceDescriptor.getSubcategories()
                    .getSubcategory()) {
                    resourceType.addChildSubCategory(SubCategoriesMetadataParser.getSubCategory(subCategoryDescriptor,
                        resourceType));
                }
            }

            if (resourceDescriptor.getHelp() != null && !resourceDescriptor.getHelp().getContent().isEmpty())
            {
                resourceType.setHelpText(String.valueOf(resourceDescriptor.getHelp().getContent().get(0)));
            }
        } catch (InvalidPluginDescriptorException e) {
            // TODO: Should we be storing these for viewing in server? Breaking deployment? What?
            throw e;
        }

        // The type is built, register it
        //registerResourceTypeAndComponentClasses(resourceType, discoveryClass, componentClass);

        // Look for child types
        if (resourceDescriptor instanceof PlatformDescriptor) {
            for (ServerDescriptor serverDescriptor : ((PlatformDescriptor) resourceDescriptor).getServers()) {
                parseServerDescriptor(serverDescriptor, resourceType);
            }

            for (ServiceDescriptor serviceDescriptor : ((PlatformDescriptor) resourceDescriptor).getServices()) {
                parseServiceDescriptor(serviceDescriptor, resourceType);
            }
        }

        if (resourceDescriptor instanceof ServerDescriptor) {
            for (ServerDescriptor serverDescriptor : ((ServerDescriptor) resourceDescriptor).getServers()) {
                parseServerDescriptor(serverDescriptor, resourceType);
            }

            for (ServiceDescriptor serviceDescriptor : ((ServerDescriptor) resourceDescriptor).getServices()) {
                parseServiceDescriptor(serviceDescriptor, resourceType);
            }
        }

        if (resourceDescriptor instanceof ServiceDescriptor) {
            for (ServiceDescriptor serviceDescriptor : ((ServiceDescriptor) resourceDescriptor).getServices()) {
                parseServiceDescriptor(serviceDescriptor, resourceType);
            }
        }

        this.resourceTypes.add(resourceType);
    }

    private String getFullyQualifiedComponentClassName(String packageName, String baseClassName) {
        if (baseClassName == null) {
            return null;
        }

        if ((baseClassName.indexOf('.') > -1) || (packageName == null)) {
            return baseClassName; // looks like must already be fully qualified
        }

        return packageName + '.' + baseClassName;
    }

    /**
     * Converts the creation data descriptor (JAXB) object into the domain enumeration.
     *
     * @param  creationType descriptor read creation data object
     *
     * @return domain enumeration value; <code>null</code> if the JAXB representation is <code>null</code> or does not
     *         correspond to any domain enumerated value
     */
    private ResourceCreationDataType convertCreationDataType(ResourceCreationData creationType) {
        switch (creationType) {
        case CONTENT: {
            return ResourceCreationDataType.CONTENT;
        }

        case CONFIGURATION: {
            return ResourceCreationDataType.CONFIGURATION;
        }
        }

        return null;
    }

    /**
     * Converts the create and delete policy descriptor (JAXB) object into the domain enumeration.
     *
     * @param  policy descriptor read policy
     *
     * @return domain enumeration value; <code>null</code> if the JAXB representation is <code>null</code> or does not
     *         correspond to any domain enumerated value
     */
    private CreateDeletePolicy convertCreateDeletePolicy(ResourceCreateDeletePolicy policy) {
        switch (policy) {
        case BOTH: {
            return CreateDeletePolicy.BOTH;
        }

        case CREATE_ONLY: {
            return CreateDeletePolicy.CREATE_ONLY;
        }

        case DELETE_ONLY: {
            return CreateDeletePolicy.DELETE_ONLY;
        }

        case NEITHER: {
            return CreateDeletePolicy.NEITHER;
        }
        }

        return null;
    }
}