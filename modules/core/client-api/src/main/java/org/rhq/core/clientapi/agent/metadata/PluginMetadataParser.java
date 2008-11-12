/*
 * RHQ Management Platform
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
package org.rhq.core.clientapi.agent.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.descriptor.plugin.ContentDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.EventDescriptor;
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
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceType;

/**
 * This is a stateful class intended to hold the related metadata for a single plugin descriptor. It is designed to be
 * used by the PluginMetadataManager that will load multiple plugin descriptors and coordinate their metadata.
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 */
public class PluginMetadataParser {
    private static final Log LOG = LogFactory.getLog(PluginMetadataParser.class);

    private PluginDescriptor pluginDescriptor;

    private List<ResourceType> resourceTypes = new ArrayList<ResourceType>();

    private LinkedHashSet<ResourceType> rootResourceTypes = new LinkedHashSet<ResourceType>();

    // TODO: this isn't the most elegant... should we put these in the domain objects? or perhaps build another place for them to live?
    private Map<ResourceType, String> discoveryClasses = new HashMap<ResourceType, String>();
    private Map<ResourceType, String> componentClasses = new HashMap<ResourceType, String>();

    // a map keyed on plugin name that contains the parsers for all other known plugin descriptors
    // this map is managed by this parser's PluginMetadataManager and is how the manager shares information
    // from other plugins to this parser instance
    private final Map<String, PluginMetadataParser> parsersByPlugin;

    public PluginMetadataParser(PluginDescriptor descriptor, Map<String, PluginMetadataParser> parsersByPlugin)
        throws InvalidPluginDescriptorException {
        this.pluginDescriptor = descriptor;
        this.parsersByPlugin = parsersByPlugin;
        parseDescriptor();
    }

    public List<ResourceType> getAllTypes() {
        return new ArrayList<ResourceType>(resourceTypes);
    }

    /**
     * @return the root types for this plugin (not necessarily root for the whole system)
     */
    public Set<ResourceType> getRootResourceTypes() {
        return this.rootResourceTypes;
    }

    public void parseDescriptor() throws InvalidPluginDescriptorException {
        // Plugin's Platforms
        for (PlatformDescriptor serverDescriptor : pluginDescriptor.getPlatforms()) {
            rootResourceTypes.add(parsePlatformDescriptor(serverDescriptor));
        }

        // Plugin's Servers
        for (ServerDescriptor serverDescriptor : pluginDescriptor.getServers()) {
            rootResourceTypes.add(parseServerDescriptor(serverDescriptor, null, null));
        }

        // Plugin's Services
        for (ServiceDescriptor serviceDescriptor : pluginDescriptor.getServices()) {
            rootResourceTypes.add(parseServiceDescriptor(serviceDescriptor, null, null));
        }
    }

    private ResourceType parsePlatformDescriptor(PlatformDescriptor platformDescriptor)
        throws InvalidPluginDescriptorException {
        ResourceType platformResourceType = new ResourceType(platformDescriptor.getName(), pluginDescriptor.getName(),
            ResourceCategory.PLATFORM, null);
        platformResourceType.setSubCategory(SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(
            platformResourceType, platformDescriptor.getSubCategory()));
        platformResourceType.setDescription(platformDescriptor.getDescription());
        LOG.debug("Parsed platform resource type: " + platformResourceType);
        parseResourceDescriptor(platformDescriptor, platformResourceType, null, null, null);

        if ((platformResourceType.getProcessScans() != null) && (platformResourceType.getProcessScans().size() > 0)) {
            LOG.warn("Platforms are not auto-discovered via process scans. "
                + "The <process-scan> elements will be ignored in resource type: " + platformResourceType);
        }

        return platformResourceType;
    }

    private ResourceType parseServerDescriptor(ServerDescriptor serverDescriptor, ResourceType parentServerType,
        String parentSource) throws InvalidPluginDescriptorException {
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

            LOG.debug("Parsed server resource type: " + serverResourceType);
            parseResourceDescriptor(serverDescriptor, serverResourceType, null, null, null);
        } else if ((sourcePlugin.length() > 0) && (sourceServer.length() > 0)) {
            // using Embedded extension model - the defined type is actually a copy of another plugin's server type
            Map<String, ServerDescriptor> pluginServerDescriptors = getPluginServerDescriptors(sourcePlugin);
            ServerDescriptor sourceServerDescriptor = pluginServerDescriptors.get(sourceServer);

            if (sourceServerDescriptor == null) {
                throw new InvalidPluginDescriptorException("There is no server type named [" + sourceServer
                    + "] from a plugin named [" + sourcePlugin + "]; please check the descriptor of type ["
                    + serverDescriptor.getName() + "]");
            }

            serverResourceType = new ResourceType(serverDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVER, parentServerType);

            // let the plugin writer override these, if not, pick up the source type's values
            serverResourceType.setDescription((serverDescriptor.getDescription() != null) ? serverDescriptor
                .getDescription() : sourceServerDescriptor.getDescription());
            serverResourceType.setSubCategory((serverDescriptor.getSubCategory() != null) ? SubCategoriesMetadataParser
                .findSubCategoryOnResourceTypeAncestor(serverResourceType, serverDescriptor.getSubCategory())
                : SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(serverResourceType,
                    sourceServerDescriptor.getSubCategory()));
            serverResourceType.setCreationDataType(convertCreationDataType(serverDescriptor.getCreationDataType()));
            serverResourceType
                .setCreateDeletePolicy(convertCreateDeletePolicy(serverDescriptor.getCreateDeletePolicy()));
            serverResourceType.setSingleton(serverDescriptor.isSingleton());

            String discoveryClass;
            String componentClass;

            if (serverDescriptor.getDiscovery() != null) {
                discoveryClass = getFullyQualifiedComponentClassName(pluginDescriptor.getPackage(), serverDescriptor
                    .getDiscovery());
            } else {
                discoveryClass = getFullyQualifiedComponentClassName(getPluginPackage(sourcePlugin),
                    sourceServerDescriptor.getDiscovery());
            }

            if (serverDescriptor.getClazz() != null) {
                componentClass = getFullyQualifiedComponentClassName(pluginDescriptor.getPackage(), serverDescriptor
                    .getClazz());
            } else {
                componentClass = getFullyQualifiedComponentClassName(getPluginPackage(sourcePlugin),
                    sourceServerDescriptor.getClazz());
            }

            parseResourceDescriptor(sourceServerDescriptor, serverResourceType, discoveryClass, componentClass,
                sourcePlugin);
            // The above incorporates children from the source descriptor. The following incorporates
            // children from this descriptor

            // Look for child server types
            for (ServerDescriptor childServerDescriptor : serverDescriptor.getServers()) {
                parseServerDescriptor(childServerDescriptor, serverResourceType, null);
            }

            // Look for child server types
            for (ServiceDescriptor childServiceDescriptor : serverDescriptor.getServices()) {
                parseServiceDescriptor(childServiceDescriptor, serverResourceType, null);
            }
        } else {
            // this should never happen - the XML parser should have failed to even get this far
            throw new InvalidPluginDescriptorException("Both sourcePlugin and sourceType must be defined: "
                + serverDescriptor.getName());
        }

        serverResourceType.setSupportsManualAdd(serverDescriptor.isSupportsManualAdd());

        // now see if we are using the Injection extension model
        // if so, we need to inject the new resource type as a child to the parent plugin's types
        RunsInsideType runsInside = serverDescriptor.getRunsInside();
        if (runsInside != null) {
            List<ParentResourceType> parentTypesDescriptor = runsInside.getParentResourceType();
            for (ParentResourceType parentTypeDescriptor : parentTypesDescriptor) {
                String parentTypeName = parentTypeDescriptor.getName();
                String parentTypePlugin = parentTypeDescriptor.getPlugin();
                ResourceType parentTypeToInjectInto = getResourceTypeFromPlugin(parentTypeName, parentTypePlugin);

                if (parentTypeToInjectInto == null) {
                    throw new InvalidPluginDescriptorException("There is no resource type named [" + parentTypeName
                        + "] from a plugin named [" + parentTypePlugin + "]; please check the descriptor of type ["
                        + serverDescriptor.getName() + "]");
                }

                // inject our new server resource type as a child type to the parent plugin's type
                parentTypeToInjectInto.addChildResourceType(serverResourceType);
            }
        }

        return serverResourceType;
    }

    private ResourceType parseServiceDescriptor(ServiceDescriptor serviceDescriptor, ResourceType parentType,
        String parentSource) throws InvalidPluginDescriptorException {
        ResourceType serviceResourceType;
        String sourcePlugin = serviceDescriptor.getSourcePlugin();
        String sourceService = serviceDescriptor.getSourceType();

        // Fallback to using the source type of your parent if you don't override
        if (sourcePlugin == null) {
            sourcePlugin = parentSource;
        }

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

            LOG.debug("Parsed service resource type: " + serviceResourceType);
            parseResourceDescriptor(serviceDescriptor, serviceResourceType, null, null, null);

            if ((serviceResourceType.getProcessScans() != null) && (serviceResourceType.getProcessScans().size() > 0)) {
                LOG.warn("Child services are not auto-discovered via process scans. "
                    + "The <process-scan> elements will be ignored in resource type: " + serviceResourceType);
            }
        } else if (sourcePlugin.length() > 0) {
            // using Embedded extension model - the defined type is actually a copy of another plugin's service type

            ServiceDescriptor sourceServiceDescriptor;
            if (sourceService.length() == 0) {
                sourceServiceDescriptor = serviceDescriptor;
            } else {
                Map<String, ServiceDescriptor> pluginServiceDescriptors = getPluginServiceDescriptors(sourcePlugin);

                sourceServiceDescriptor = pluginServiceDescriptors.get(sourceService);
            }

            if (sourceServiceDescriptor == null) {
                throw new InvalidPluginDescriptorException("There is no service type named [" + sourceService
                    + "] from a plugin named [" + sourcePlugin + "]; please check the descriptor of type ["
                    + serviceDescriptor.getName() + "]");
            }

            serviceResourceType = new ResourceType(serviceDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVICE, parentType);

            // let the plugin writer override these, if not, pick up the source type's values
            serviceResourceType.setDescription((serviceDescriptor.getDescription() != null) ? serviceDescriptor
                .getDescription() : sourceServiceDescriptor.getDescription());
            serviceResourceType
                .setSubCategory((serviceDescriptor.getSubCategory() != null) ? SubCategoriesMetadataParser
                    .findSubCategoryOnResourceTypeAncestor(serviceResourceType, serviceDescriptor.getSubCategory())
                    : SubCategoriesMetadataParser.findSubCategoryOnResourceTypeAncestor(serviceResourceType,
                        sourceServiceDescriptor.getSubCategory()));
            serviceResourceType.setCreationDataType(convertCreationDataType(serviceDescriptor.getCreationDataType()));
            serviceResourceType.setCreateDeletePolicy(convertCreateDeletePolicy(serviceDescriptor
                .getCreateDeletePolicy()));
            serviceResourceType.setSingleton(serviceDescriptor.isSingleton());

            String discoveryClass;
            String componentClass;

            if (serviceDescriptor.getDiscovery() != null) {
                discoveryClass = getFullyQualifiedComponentClassName(getPluginPackage(sourcePlugin), serviceDescriptor
                    .getDiscovery());
            } else {
                discoveryClass = getFullyQualifiedComponentClassName(getPluginPackage(sourcePlugin),
                    sourceServiceDescriptor.getDiscovery());
            }

            if (serviceDescriptor.getClazz() != null) {
                componentClass = getFullyQualifiedComponentClassName(getPluginPackage(sourcePlugin), serviceDescriptor
                    .getClazz());
            } else {
                componentClass = getFullyQualifiedComponentClassName(getPluginPackage(sourcePlugin),
                    sourceServiceDescriptor.getClazz());
            }

            parseResourceDescriptor(sourceServiceDescriptor, serviceResourceType, discoveryClass, componentClass,
                sourcePlugin);
        } else {
            // this should never happen - the XML parser should have failed to even get this far
            throw new InvalidPluginDescriptorException("Both sourcePlugin and sourceType must be defined: "
                + serviceDescriptor.getName());
        }

        serviceResourceType.setSupportsManualAdd(serviceDescriptor.isSupportsManualAdd());

        // now see if we are using the Injection extension model
        // if so, we need to inject the new resource type as a child to the parent plugin's types
        // note that the Injection model only allows for root-level services to be injected
        if (parentType == null) {
            RunsInsideType runsInside = serviceDescriptor.getRunsInside();
            if (runsInside != null) {
                List<ParentResourceType> parentTypesDescriptor = runsInside.getParentResourceType();
                for (ParentResourceType parentTypeDescriptor : parentTypesDescriptor) {
                    String parentTypeName = parentTypeDescriptor.getName();
                    String parentTypePlugin = parentTypeDescriptor.getPlugin();
                    ResourceType parentTypeToInjectInto = getResourceTypeFromPlugin(parentTypeName, parentTypePlugin);

                    if (parentTypeToInjectInto == null) {
                        throw new InvalidPluginDescriptorException("There is no resource type named [" + parentTypeName
                            + "] from a plugin named [" + parentTypePlugin + "]; please check the descriptor of type ["
                            + serviceDescriptor.getName() + "]");
                    }

                    // inject our new server resource type as a child type to the parent plugin's type
                    parentTypeToInjectInto.addChildResourceType(serviceResourceType);
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
                List<MeasurementDefinition> measurementDefinition = MetricsMetadataParser.parseMetricsMetadata(
                    metricDescriptor, resourceType);
                for (MeasurementDefinition def : measurementDefinition) {
                    def.setDisplayOrder(displayPosition++);
                    resourceType.addMetricDefinition(def);
                }
            }

            for (EventDescriptor eventDescriptor : resourceDescriptor.getEvent()) {
                EventDefinition eventDefinition = EventsMetadataParser.parseEventsMetadata(eventDescriptor, resourceType);
                resourceType.addEventDefinition(eventDefinition);
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
                    resourceType.addSubCategory(SubCategoriesMetadataParser.getSubCategory(subCategoryDescriptor,
                        resourceType));
                }
            }
        } catch (InvalidPluginDescriptorException e) {
            // TODO: Should we be storing these for viewing in server? Breaking deployment? What?
            throw e;
        }

        // The type is built, register it
        registerResourceTypeAndComponentClasses(resourceType, discoveryClass, componentClass);

        // Look for child types
        if (resourceDescriptor instanceof PlatformDescriptor) {
            for (ServerDescriptor serverDescriptor : ((PlatformDescriptor) resourceDescriptor).getServers()) {
                parseServerDescriptor(serverDescriptor, resourceType, sourcePlugin);
            }

            for (ServiceDescriptor serviceDescriptor : ((PlatformDescriptor) resourceDescriptor).getServices()) {
                parseServiceDescriptor(serviceDescriptor, resourceType, sourcePlugin);
            }
        }

        if (resourceDescriptor instanceof ServerDescriptor) {
            for (ServerDescriptor serverDescriptor : ((ServerDescriptor) resourceDescriptor).getServers()) {
                parseServerDescriptor(serverDescriptor, resourceType, sourcePlugin);
            }

            for (ServiceDescriptor serviceDescriptor : ((ServerDescriptor) resourceDescriptor).getServices()) {
                parseServiceDescriptor(serviceDescriptor, resourceType, sourcePlugin);
            }
        }

        if (resourceDescriptor instanceof ServiceDescriptor) {
            for (ServiceDescriptor serviceDescriptor : ((ServiceDescriptor) resourceDescriptor).getServices()) {
                parseServiceDescriptor(serviceDescriptor, resourceType, sourcePlugin);
            }
        }
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

    public String getDiscoveryComponentClass(ResourceType resourceType) {
        return this.discoveryClasses.get(resourceType);
    }

    public String getComponentClass(ResourceType resourceType) {
        return this.componentClasses.get(resourceType);
    }

    private void registerResourceTypeAndComponentClasses(ResourceType resourceType, String discoveryClass,
        String componentClass) {
        this.resourceTypes.add(resourceType);
        this.componentClasses.put(resourceType, componentClass);
        if (discoveryClass != null) {
            this.discoveryClasses.put(resourceType, discoveryClass);
        }
    }

    private Map<String, ServerDescriptor> getPluginServerDescriptors(String pluginName) {
        Map<String, ServerDescriptor> pluginServerDescriptors = new HashMap<String, ServerDescriptor>();

        PluginMetadataParser pluginParser = null;

        // get the plugin parser that corresponds to the plugin (which could be this plugin parser)
        if (pluginName.equals(this.pluginDescriptor.getName())) {
            pluginParser = this;
        } else {
            pluginParser = parsersByPlugin.get(pluginName);
        }

        if (pluginParser != null) {
            for (ServerDescriptor server : pluginParser.pluginDescriptor.getServers()) {
                pluginServerDescriptors.put(server.getName(), server);
            }
        }

        return pluginServerDescriptors;
    }

    private Map<String, ServiceDescriptor> getPluginServiceDescriptors(String pluginName) {
        Map<String, ServiceDescriptor> pluginServiceDescriptors = new HashMap<String, ServiceDescriptor>();

        PluginMetadataParser pluginParser = null;

        // get the plugin parser that corresponds to the plugin (which could be this plugin parser)
        if (pluginName.equals(this.pluginDescriptor.getName())) {
            pluginParser = this;
        } else {
            pluginParser = parsersByPlugin.get(pluginName);
        }

        if (pluginParser != null) {
            addPluginServiceDescriptors(pluginParser.pluginDescriptor.getServices(), pluginServiceDescriptors);
            addPluginServiceDescriptors(pluginParser.pluginDescriptor.getServers(), pluginServiceDescriptors);
        }

        return pluginServiceDescriptors;
    }

    private String getPluginPackage(String pluginName) {
        String packageName = null;
        PluginMetadataParser pluginParser = null;

        // get the plugin parser that corresponds to the plugin (which could be this plugin parser)
        if (pluginName.equals(this.pluginDescriptor.getName())) {
            pluginParser = this;
        } else {
            pluginParser = parsersByPlugin.get(pluginName);
        }

        if (pluginParser != null) {
            packageName = pluginParser.pluginDescriptor.getPackage();
        }

        return packageName;
    }

    private void addPluginServiceDescriptors(Collection<? extends ResourceDescriptor> parents,
        Map<String, ServiceDescriptor> descriptors) {
        if (parents != null) {
            for (ResourceDescriptor parent : parents) {
                List<ServiceDescriptor> services = null;
                if (parent instanceof ServerDescriptor) {
                    services = ((ServerDescriptor) parent).getServices();
                }

                if (parent instanceof ServiceDescriptor) {
                    services = ((ServiceDescriptor) parent).getServices();
                }

                for (ServiceDescriptor service : services) {
                    descriptors.put(service.getName(), service);
                    addPluginServiceDescriptors(service.getServices(), descriptors); // recurse down the hierarchy
                }
            }
        }
    }

    private ResourceType getResourceTypeFromPlugin(String resourceTypeName, String pluginName) {
        if ((resourceTypeName != null) && (pluginName != null)) {
            PluginMetadataParser pluginParser = null;

            // get the plugin parser that corresponds to the plugin that has the type (which could be this plugin parser)
            if (pluginName.equals(this.pluginDescriptor.getName())) {
                pluginParser = this;
            } else {
                pluginParser = parsersByPlugin.get(pluginName);
            }

            // go through the plugin's entire list of resource types to find the one we are looking for
            if (pluginParser != null) {
                for (ResourceType type : pluginParser.resourceTypes) {
                    if (resourceTypeName.equals(type.getName())) {
                        return type;
                    }
                }
            }
        }

        return null;
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