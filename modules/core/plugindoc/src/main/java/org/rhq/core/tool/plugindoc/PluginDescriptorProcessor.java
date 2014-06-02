/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.ContentMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.agent.metadata.MetricsMetadataParser;
import org.rhq.core.clientapi.agent.metadata.OperationsMetadataParser;
import org.rhq.core.clientapi.descriptor.plugin.ContentDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.Help;
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
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.plugin.Plugin;
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
    private Plugin plugin;
    private Map<ResourceType, ResourceType> allTypes = new LinkedHashMap<ResourceType, ResourceType>();
    private Set<ResourceType> resourceTypes = new LinkedHashSet<ResourceType>();

    public PluginDescriptorProcessor(PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
        this.plugin = createPlugin();
    }

    public Set<ResourceType> processPluginDescriptor() throws InvalidPluginDescriptorException {
        for (PlatformDescriptor serverDescriptor : pluginDescriptor.getPlatforms()) {
            resourceTypes.add(parsePlatformDescriptor(serverDescriptor));
        }

        for (ServerDescriptor serverDescriptor : pluginDescriptor.getServers()) {
            ResourceType serverType = parseServerDescriptor(serverDescriptor, null);
            if (isRootType(serverType)) {
                resourceTypes.add(serverType);
            }
        }

        for (ServiceDescriptor serviceDescriptor : pluginDescriptor.getServices()) {
            ResourceType serviceType = parseServiceDescriptor(serviceDescriptor, null, null);
            if (isRootType(serviceType)) {
                resourceTypes.add(serviceType);
            }
        }

        return this.resourceTypes;
    }

    private static boolean isRootType(ResourceType type) {
        boolean result;
        if ((type.getParentResourceTypes() == null) || (type.getParentResourceTypes().isEmpty())) {
            result = true;
        } else {
            result = false;
            for (ResourceType parentType : type.getParentResourceTypes()) {
                if (!parentType.getPlugin().equals(type.getPlugin())) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public PluginDescriptor getPluginDescriptor() {
        return this.pluginDescriptor;
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    private Plugin createPlugin() {
        Plugin plugin = new Plugin(this.pluginDescriptor.getName(), null);
        plugin.setDisplayName(this.pluginDescriptor.getDisplayName());
        plugin.setDescription(this.pluginDescriptor.getDescription());
        plugin.setVersion(this.pluginDescriptor.getVersion());
        Help help = this.pluginDescriptor.getHelp();
        if ((help != null) && !help.getContent().isEmpty()) {
            plugin.setHelpContentType(help.getContentType());
            plugin.setHelp(String.valueOf(help.getContent().get(0)));
        }
        return plugin;
    }

    private ResourceType parsePlatformDescriptor(PlatformDescriptor platformDescriptor)
        throws InvalidPluginDescriptorException {
        ResourceType platformResourceType = new ResourceType(platformDescriptor.getName(), pluginDescriptor.getName(),
            ResourceCategory.PLATFORM, null);
        platformResourceType.setSubCategory(platformDescriptor.getSubCategory());
        platformResourceType.setDescription(platformDescriptor.getDescription());
        log.debug("Parsed platform resource type: " + platformResourceType);
        parseResourceDescriptor(platformDescriptor, platformResourceType, null, null, null);

        if ((platformResourceType.getProcessScans() != null) && (platformResourceType.getProcessScans().size() > 0)) {
            log.warn("Platforms are not auto-discovered via process scans. "
                + "The <process-scan> elements will be ignored in resource type: " + platformResourceType);
        }

        return platformResourceType;
    }

    private ResourceType parseServerDescriptor(ServerDescriptor serverDescriptor, ResourceType parentType)
        throws InvalidPluginDescriptorException {
        ResourceType serverResourceType;
        String sourcePlugin = serverDescriptor.getSourcePlugin();
        String sourceServer = serverDescriptor.getSourceType();

        sourcePlugin = (sourcePlugin == null) ? "" : sourcePlugin.trim();
        sourceServer = (sourceServer == null) ? "" : sourceServer.trim();

        if (sourcePlugin.isEmpty() && sourceServer.isEmpty()) {
            // not using Embedded extension model
            serverResourceType = new ResourceType(serverDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVER, parentType);
            serverResourceType.setSubCategory(serverDescriptor.getSubCategory());
            serverResourceType.setDescription(serverDescriptor.getDescription());
            serverResourceType.setCreationDataType(convertCreationDataType(serverDescriptor.getCreationDataType()));
            serverResourceType
                .setCreateDeletePolicy(convertCreateDeletePolicy(serverDescriptor.getCreateDeletePolicy()));
            serverResourceType.setSingleton(serverDescriptor.isSingleton());

            log.debug("Parsed server resource type: " + serverResourceType);
            parseResourceDescriptor(serverDescriptor, serverResourceType, null, null, null);
        } else if ((sourcePlugin.length() > 0) && (sourceServer.length() > 0)) {
            // using Embedded extension model - the defined type is actually a copy of another plugin's server type
            log.debug("Parsing embedded server type {" + pluginDescriptor.getName() + "}"
                    + serverDescriptor.getName() + ", which extends server type {" + sourcePlugin + "}" + sourceServer + "...");

            Map<String, ServerDescriptor> pluginServerDescriptors = getPluginServerDescriptors(sourcePlugin);
            ServerDescriptor sourceServerDescriptor = pluginServerDescriptors.get(sourceServer);

            if (sourceServerDescriptor == null) {
                log.warn("There is no server type named [" + sourceServer + "] from a plugin named [" + sourcePlugin
                        + "]. This is probably because that plugin is missing. Resource Type [{"
                        + pluginDescriptor.getName() + "}" + serverDescriptor.getName() + "] will be ignored.");
                return null;
            }

            serverResourceType = new ResourceType(serverDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVER, parentType);

            // Let the plugin writer override these, or if not, parseResourceDescriptor() will pick up the source type's
            // values.
            serverResourceType.setDescription(serverDescriptor.getDescription());
            serverResourceType.setSubCategory(serverDescriptor.getSubCategory());

            serverResourceType.setCreationDataType(convertCreationDataType(serverDescriptor.getCreationDataType()));
            serverResourceType
                .setCreateDeletePolicy(convertCreateDeletePolicy(serverDescriptor.getCreateDeletePolicy()));
            serverResourceType.setSingleton(serverDescriptor.isSingleton());

            parseResourceDescriptor(sourceServerDescriptor, serverResourceType, null, null, sourcePlugin);
            // The above incorporates children from the source descriptor. The following incorporates
            // children from this descriptor

            // Look for child server types
            for (ServerDescriptor childServerDescriptor : serverDescriptor.getServers()) {
                parseServerDescriptor(childServerDescriptor, serverResourceType);
            }

            // Look for child service types
            for (ServiceDescriptor childServiceDescriptor : serverDescriptor.getServices()) {
                parseServiceDescriptor(childServiceDescriptor, serverResourceType, null);
            }
        } else {
            // this should never happen - the XML parser should have failed to even get this far
            throw new InvalidPluginDescriptorException("Both sourcePlugin and sourceType must be defined: "
                + serverDescriptor.getName());
        }

        // now see if we are using the Injection extension model
        // if so, we need to inject the new resource type as a child to the parent plugin's types
        if (parentType == null) {
            addRunsInsideParentTypes(serverDescriptor, serverResourceType);
        }

        // Look for child server types
        for (ServerDescriptor childServerDescriptor : serverDescriptor.getServers()) {
            parseServerDescriptor(childServerDescriptor, serverResourceType);
        }

        return serverResourceType;
    }

    private ResourceType parseServiceDescriptor(ServiceDescriptor serviceDescriptor, ResourceType parentType,
                                                String parentSourcePlugin)
        throws InvalidPluginDescriptorException {
        ResourceType serviceResourceType;
        String sourcePlugin = serviceDescriptor.getSourcePlugin();
        // Fallback to using the source plugin of your parent if you don't override.
        if (sourcePlugin == null) {
            sourcePlugin = parentSourcePlugin;
        }
        String sourceType = serviceDescriptor.getSourceType();

        sourcePlugin = (sourcePlugin == null) ? "" : sourcePlugin.trim();
        sourceType = (sourceType == null) ? "" : sourceType.trim();

        if (sourcePlugin.isEmpty() && sourceType.isEmpty()) {
            // not using Embedded extension model
            serviceResourceType = new ResourceType(serviceDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVICE, parentType);
            serviceResourceType.setSubCategory(serviceDescriptor.getSubCategory());
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
            // Using Embedded extension model - the defined type is actually a copy of another plugin's service or server type.
            log.debug("Parsing embedded service type {" + pluginDescriptor.getName() + "}"
                    + serviceDescriptor.getName() + ", which extends type {" + sourcePlugin + "}" + sourceType + "...");

            ResourceDescriptor sourceTypeDescriptor;
            if (sourceType.isEmpty()) {
                sourceTypeDescriptor = serviceDescriptor;
            } else {
                Map<String, ServiceDescriptor> pluginServiceDescriptors = getPluginServiceDescriptors(sourcePlugin);
                sourceTypeDescriptor = pluginServiceDescriptors.get(sourceType);
                if (sourceTypeDescriptor == null) {
                    Map<String, ServerDescriptor> pluginServerDescriptors = getPluginServerDescriptors(sourcePlugin);
                    sourceTypeDescriptor = pluginServerDescriptors.get(sourceType);
                }
            }

            if (sourceTypeDescriptor == null) {
                log.warn("There is no service or server type named [" + sourceType + "] from a plugin named ["
                        + sourcePlugin + "]. This is probably because that plugin is missing. Resource Type [{"
                        + pluginDescriptor.getName() + "}" + serviceDescriptor.getName() + "] will be ignored.");
                return null;
            }

            serviceResourceType = new ResourceType(serviceDescriptor.getName(), pluginDescriptor.getName(),
                ResourceCategory.SERVICE, parentType);

            // Let the plugin writer override these, or if not, parseResourceDescriptor() will pick up the source type's
            // values.
            serviceResourceType.setDescription(serviceDescriptor.getDescription());
            serviceResourceType.setSubCategory(serviceDescriptor.getSubCategory());

            serviceResourceType.setCreationDataType(convertCreationDataType(serviceDescriptor.getCreationDataType()));
            serviceResourceType.setCreateDeletePolicy(convertCreateDeletePolicy(serviceDescriptor
                .getCreateDeletePolicy()));
            serviceResourceType.setSingleton(serviceDescriptor.isSingleton());

            parseResourceDescriptor(sourceTypeDescriptor, serviceResourceType, null, null, sourcePlugin);
        } else {
            // this should never happen - the XML parser should have failed to even get this far
            throw new InvalidPluginDescriptorException("Both sourcePlugin and sourceType must be defined: "
                + serviceDescriptor.getName());
        }

        // now see if we are using the Injection extension model
        // if so, we need to inject the new resource type as a child to the parent plugin's types
        // note that the Injection model only allows for root-level services to be injected
        if (parentType == null) {
            addRunsInsideParentTypes(serviceDescriptor, serviceResourceType);
        }

        return serviceResourceType;
    }

    private void addRunsInsideParentTypes(ResourceDescriptor resourceDescriptor, ResourceType resourceType) {
        RunsInsideType runsInside = resourceDescriptor.getRunsInside();
        if (runsInside != null) {
            List<ParentResourceType> parentTypesDescriptor = runsInside.getParentResourceType();
            for (ParentResourceType parentTypeDescriptor : parentTypesDescriptor) {
                ResourceCategory parentResourceCategory = parentTypeDescriptor.getPlugin().equals("Platforms") ? ResourceCategory.PLATFORM
                    : ResourceCategory.SERVER;
                ResourceType parentResourceType = new ResourceType(parentTypeDescriptor.getName(),
                    parentTypeDescriptor.getPlugin(), parentResourceCategory, ResourceType.ANY_PLATFORM_TYPE);
                ResourceType fullParentType = allTypes.get(parentResourceType);
                if (fullParentType != null) {
                    parentResourceType = fullParentType;
                }
                resourceType.addParentResourceType(parentResourceType);
            }
        }
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
            System.out.println(resourceType.getName() + ": "
                + new ProcessScan(processMatch.getQuery(), processMatch.getName()));
            resourceType.addProcessScan(new ProcessScan(processMatch.getQuery(), processMatch.getName()));
        }

        for (ContentDescriptor contentDescriptor : resourceDescriptor.getContent()) {
            resourceType.addPackageType(ContentMetadataParser.parseContentDescriptor(contentDescriptor));
        }

        Help help = resourceDescriptor.getHelp();
        if ((help != null) && !help.getContent().isEmpty()) {
            resourceType.setHelpTextContentType(help.getContentType());
            resourceType.setHelpText(String.valueOf(help.getContent().get(0)));
        }

        allTypes.put(resourceType, resourceType);

        // The type is built, register it
        //registerResourceTypeAndComponentClasses(resourceType, discoveryClass, componentClass);

        // Look for child types
        if (resourceDescriptor instanceof PlatformDescriptor) {
            for (ServerDescriptor serverDescriptor : ((PlatformDescriptor) resourceDescriptor).getServers()) {
                parseServerDescriptor(serverDescriptor, resourceType);
            }

            for (ServiceDescriptor serviceDescriptor : ((PlatformDescriptor) resourceDescriptor).getServices()) {
                parseServiceDescriptor(serviceDescriptor, resourceType, sourcePlugin);
            }
        }

        if (resourceDescriptor instanceof ServerDescriptor) {
            for (ServerDescriptor serverDescriptor : ((ServerDescriptor) resourceDescriptor).getServers()) {
                parseServerDescriptor(serverDescriptor, resourceType);
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

    private Map<String, ServerDescriptor> getPluginServerDescriptors(String pluginName) {
        Map<String, ServerDescriptor> pluginServerDescriptors = new HashMap<String, ServerDescriptor>();

        // In plugindoc, we only support embedding types from this plugin.
        if (pluginName.equals(this.pluginDescriptor.getName())) {
            for (ServerDescriptor server : pluginDescriptor.getServers()) {
                pluginServerDescriptors.put(server.getName(), server);
            }
        }

        return pluginServerDescriptors;
    }

    private Map<String, ServiceDescriptor> getPluginServiceDescriptors(String pluginName) {
        Map<String, ServiceDescriptor> pluginServiceDescriptors = new HashMap<String, ServiceDescriptor>();

        // In plugindoc, we only support embedding types from this plugin.
        if (pluginName.equals(this.pluginDescriptor.getName())) {
            addPluginServiceDescriptors(pluginDescriptor.getServices(), pluginServiceDescriptors);
            addPluginServiceDescriptors(pluginDescriptor.getServers(), pluginServiceDescriptors);
        }

        return pluginServiceDescriptors;
    }

    private void addPluginServiceDescriptors(Collection<? extends ResourceDescriptor> parents,
        Map<String, ServiceDescriptor> descriptors) {
        if (parents != null) {
            for (ResourceDescriptor parent : parents) {
                List<ServiceDescriptor> services;
                if (parent instanceof ServerDescriptor) {
                    services = ((ServerDescriptor) parent).getServices();
                } else if (parent instanceof ServiceDescriptor) {
                    services = ((ServiceDescriptor) parent).getServices();
                } else {
                    throw new IllegalStateException("Unsupported parent descriptor type: "
                            + parent.getClass().getName());
                }

                for (ServiceDescriptor service : services) {
                    descriptors.put(service.getName(), service);
                    addPluginServiceDescriptors(service.getServices(), descriptors); // recurse down the hierarchy
                }
            }
        }
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
