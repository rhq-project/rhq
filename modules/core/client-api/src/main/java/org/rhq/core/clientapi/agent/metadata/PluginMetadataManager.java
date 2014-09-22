/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.domain.resource.ClassLoaderType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * This is meant to provide an interface to the underlying metadata of a plugin. It will load, translate and cache the
 * metadata for the rest of the services in the form of the domain object classes and the jaxb version of the
 * descriptors.
 *
 * This object can also be used to separately store plugin descriptors without converting them into types (i.e.
 * the descriptor staging area).
 * The thinking here is the server has the ability to get all the plugin descriptors early on and in any order;
 * only later does it load/register those plugins (because it needs to order them via the proper dependency graph.
 * There may be times when we need a plugin's descriptor but before that plugin has been loaded/registered. This
 * manager lets us stage those descriptors prior to converting them into types.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class PluginMetadataManager {
    public static final ResourceType TEST_PLATFORM_TYPE = new ResourceType("Anonymous", "test",
        ResourceCategory.PLATFORM, null);
    static {
        TEST_PLATFORM_TYPE.setClassLoaderType(ClassLoaderType.SHARED);
    }

    private Log log = LogFactory.getLog(PluginMetadataManager.class);

    private Map<ResourceCategory, LinkedHashSet<ResourceType>> typesByCategory = new HashMap<ResourceCategory, LinkedHashSet<ResourceType>>();
    private Set<ResourceType> types = new HashSet<ResourceType>();
    private final Object typesLock = new Object();

    private Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>();

    private Map<String, PluginDescriptor> descriptorsByPlugin = new HashMap<String, PluginDescriptor>();

    // "disabled" types are disabled entirely at initialization time and will be disable for the lifetime of this manager.
    // "ignored" types are ignored but can be unignored at a later time.
    private List<String> disabledResourceTypesAsStrings = null;
    private Map<ResourceType, String> disabledResourceTypes = null;
    private Set<ResourceType> ignoredResourceTypes = null;
    private final Object disabledIgnoredTypesLock = new Object(); // used when accessing disabled and ignored collections

    // these define the discovery callbacks per resource type. The key is the resource type whose discovered details
    // need to be funneled through callbacks. The value is a map whose key is plugin names and whose values are
    // discovery callback implementation classes defined in the plugins.
    private Map<ResourceType, Map<String, List<String>>> discoveryCallbacks = new HashMap<ResourceType, Map<String, List<String>>>();
    // similar map for the resource upgrade callbacks
    private Map<ResourceType, Map<String, List<String>>> resourceUpgradeCallbacks = new HashMap<ResourceType, Map<String, List<String>>>();

    //this lock is shared for filling up both the discoveryCallbacks and resourceUpgradeCallbacks.
    private final Object discoveryAndResourceUpgradeCallbacksLock = new Object();

    public PluginMetadataManager() {
    }

    /**
     * This will simply squirrel away the given plugin descriptor for later retrieval
     * via {@link #getPluginDescriptor(String)}. Use this as a simple storage
     * mechanism for descriptors. Nothing is done with descriptor other than store it
     * in memory for later retrieval.
     * @param descriptor the descriptor to store
     */
    public void storePluginDescriptor(PluginDescriptor descriptor) {
        this.descriptorsByPlugin.put(descriptor.getName(), descriptor);
    }

    /**
     * Get the plugin descriptor for the named plugin. If the descriptor was previously staged
     * via {@link #storePluginDescriptor(PluginDescriptor)}, it will be used. If a new descriptor
     * hasn't been staged, but a previous descriptor was loaded and converted into types,
     * via {@link #loadPlugin(PluginDescriptor)}, it will be used.
     * If the descriptor cannot be found anywhere, returns null.
     *
     * @param pluginName name of the plugin whose descriptor is to be returned.
     * @return the descriptor or null if not available
     */
    public PluginDescriptor getPluginDescriptor(String pluginName) {
        PluginDescriptor descriptor = this.descriptorsByPlugin.get(pluginName);
        if (descriptor == null) {
            PluginMetadataParser parser = this.parsersByPlugin.get(pluginName);
            if (parser != null) {
                descriptor = parser.getDescriptor();
            }
        }
        return descriptor;
    }

    private void addType(ResourceType type) {
        ResourceCategory category = type.getCategory();

        synchronized (typesLock) {
            if (!typesByCategory.containsKey(category)) {
                typesByCategory.put(category, new LinkedHashSet<ResourceType>());
            }

            typesByCategory.get(category).add(type);
            types.add(type);
        }
    }

    /**
     * Adds a platform resource type to represent an "anonymous" platform. This should really only ever be used in a
     * test scenario when the platform plugin is unavailable.
     *
     * @return the very thin anonymous resource type object
     */
    public ResourceType addTestPlatformType() {
        addType(TEST_PLATFORM_TYPE);
        return TEST_PLATFORM_TYPE;
    }

    public String getPluginLifecycleListenerClass(String pluginName) {
        PluginMetadataParser parser = this.parsersByPlugin.get(pluginName);
        return (parser != null) ? parser.getPluginLifecycleListenerClass() : null;
    }

    public String getDiscoveryClass(ResourceType resourceType) throws ResourceTypeNotEnabledException {
        if (isDisabledOrIgnoredResourceType(resourceType)) {
            log.debug("resource type is disabled - rejecting request for discovery class name: " + resourceType);
            throw new ResourceTypeNotEnabledException(resourceType);
        }
        PluginMetadataParser parser = this.parsersByPlugin.get(resourceType.getPlugin());
        return (parser != null) ? parser.getDiscoveryComponentClass(resourceType) : null;
    }

    public String getComponentClass(ResourceType resourceType) throws ResourceTypeNotEnabledException {
        if (isDisabledOrIgnoredResourceType(resourceType)) {
            log.debug("resource type is disabled - rejecting request for component class name: " + resourceType);
            throw new ResourceTypeNotEnabledException(resourceType);
        }
        PluginMetadataParser parser = this.parsersByPlugin.get(resourceType.getPlugin());
        return (parser != null) ? parser.getComponentClass(resourceType) : null;
    }
    
    /**
     * Transforms the pluginDescriptor into domain object form and stores into this object's type system.
     *
     * @param  pluginDescriptor the descriptor to transform
     *
     * @return the root resource types represented by this descriptor, or null on failure
     */
    public synchronized Set<ResourceType> loadPlugin(PluginDescriptor pluginDescriptor) {
        try {
            PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);
            PluginMetadataParser oldParser = this.parsersByPlugin.get(pluginDescriptor.getName());

            if (oldParser != null) {
                // This is a redeploy, first delete all the original types
                synchronized (this.typesLock) {
                    for (ResourceType oldType : oldParser.getAllTypes()) {
                        this.typesByCategory.get(oldType.getCategory()).remove(oldType);
                        this.types.remove(oldType);
                    }
                }
            }

            this.parsersByPlugin.put(pluginDescriptor.getName(), parser);
            this.descriptorsByPlugin.remove(pluginDescriptor.getName()); // don't need it here anymore if its there

            synchronized (this.typesLock) {
                for (ResourceType resourceType : parser.getAllTypes()) {
                    if (types.contains(resourceType)) {
                        throw new InvalidPluginDescriptorException("Type [" + resourceType
                            + "] is duplicate for this plugin. This is illegal.");
                    }
                    addType(resourceType);
                }
            }

            synchronized (disabledIgnoredTypesLock) {
                findDisabledResourceTypesInAllPlugins();
            }

            synchronized (discoveryAndResourceUpgradeCallbacksLock) {
                // squirrel away all the discovery callbacks
                Map<ResourceType, List<String>> discoveryCallbacksMap = parser.getDiscoveryCallbackClasses();
                addCallbacks(discoveryCallbacksMap, pluginDescriptor.getName(), discoveryCallbacks);

                //and the same for the resource upgrade callbacks
                Map<ResourceType, List<String>> resourceUpgradCallbacksMap = parser.getResourceUpgradeCallbackClasses();
                addCallbacks(resourceUpgradCallbacksMap, pluginDescriptor.getName(), resourceUpgradeCallbacks);
            }

            // return the top root types from the descriptor
            Set<ResourceType> rootTypes = parser.getRootResourceTypes();
            return rootTypes;

        } catch (InvalidPluginDescriptorException e) {
            // TODO Should we throw back or log partial failures or store them against the definitions?
            log.error("Error transforming plugin descriptor [" + pluginDescriptor.getName() + "].", e);
        }

        return null;
    }

    /**
     * Returns the Resource type with the specified name and plugin, or null if no such Resource type exists.
     *
     * @param resourceTypeName the Resource type name
     * @param pluginName the name of the plugin that defines the Resource type
     *
     * @return the Resource type with the specified name and plugin, or null if no such Resource type exists
     */
    @Nullable
    public ResourceType getType(String resourceTypeName, String pluginName) {
        ResourceType searchType = new ResourceType(resourceTypeName, pluginName, null, null);
        synchronized (this.typesLock) {
            for (ResourceType type : types) {
                if (type.equals(searchType)) {
                    return type;
                }
            }
        }
        return null;
    }

    @Nullable
    public ResourceType getType(ResourceType resourceType) {
        if (TEST_PLATFORM_TYPE.equals(resourceType)) {
            return TEST_PLATFORM_TYPE;
        }
        return getType(resourceType.getName(), resourceType.getPlugin());
    }

    /**
     * Return the Resource types applicable for a category
     * @param category ResourceCategory to look up
     * @return the types for this category or an empty Set
     */
    public Set<ResourceType> getTypesForCategory(ResourceCategory category) {
        synchronized (this.typesLock) {
            LinkedHashSet<ResourceType> types = this.typesByCategory.get(category);
            return (types != null) ? types : new HashSet<ResourceType>();
        }
    }

    public Set<ResourceType> getAllTypes() {
        synchronized (this.typesLock) {
            return new HashSet<ResourceType>(types);
        }
    }

    public Set<ResourceType> getRootTypes() {
        Set<ResourceType> rootTypes = new HashSet<ResourceType>();
        for (ResourceType type : getAllTypes()) {
            if (type.getParentResourceTypes().size() == 0) {
                rootTypes.add(type);
            }
        }

        return rootTypes;
    }

    public Set<String> getPluginNames() {
        return this.parsersByPlugin.keySet();
    }

    /**
     * Builds the dependency graph using all known descriptors.
     *
     * @return dependency graph
     */
    public PluginDependencyGraph buildDependencyGraph() {
        PluginDependencyGraph dependencyGraph = new PluginDependencyGraph();
        for (PluginDescriptor descriptor : getAllKnownPluginDescriptors().values()) {
            AgentPluginDescriptorUtil.addPluginToDependencyGraph(dependencyGraph, descriptor);
        }
        return dependencyGraph;
    }

    /**
     * Returns a map of plugins and their descriptors where those plugins are child extensions of the given
     * parent plugin. The child extensions are those that used the "embedded" plugin extension model (that is,
     * those whose types used sourcePlugin attribute in their type metedata).
     *
     * Note that this will examine all known descriptors, those that were {@link #loadPlugin(PluginDescriptor) loaded}
     * and those that were merely {@link #storePluginDescriptor(PluginDescriptor) stored}.
     *
     * @param parentPlugin the parent plugin
     * @return a map of child plugin info where the children are those that extended the given parent plugin.
     *         If the given parent plugin was not extended by any other plugin, the map will be empty.
     */
    public Map<String, PluginDescriptor> getEmbeddedExtensions(String parentPlugin) {
        // get all the descriptors we are going to look at
        Map<String, PluginDescriptor> allDescriptors = getAllKnownPluginDescriptors();

        // look at all the descriptors
        Map<String, PluginDescriptor> map = new HashMap<String, PluginDescriptor>();
        for (PluginDescriptor descriptor : allDescriptors.values()) {
            String pluginName = descriptor.getName();

            if (parentPlugin.equals(pluginName)) {
                continue; // ignore itself, go on to the next
            }

            // let's see if any servers extend the parent plugin...
            if (doServersExtendParent(descriptor.getServers(), parentPlugin)) {
                map.put(pluginName, descriptor);
                continue; // no need to keep checking this plugin, go on to the next
            }

            // if no servers extended the parent plugin, let's check to see if any services do...
            if (!map.containsKey(pluginName)) {
                if (doServicesExtendParent(descriptor.getServices(), parentPlugin)) {
                    map.put(pluginName, descriptor);
                    continue; // no need to keep checking this plugin, go on to the next
                }
            }
        }
        return map;
    }

    private Map<String, PluginDescriptor> getAllKnownPluginDescriptors() {
        Map<String, PluginDescriptor> allDescriptors = new HashMap<String, PluginDescriptor>();
        allDescriptors.putAll(descriptorsByPlugin);
        for (PluginMetadataParser parser : parsersByPlugin.values()) {
            allDescriptors.put(parser.getDescriptor().getName(), parser.getDescriptor());
        }
        return allDescriptors;
    }

    private boolean doServersExtendParent(List<ServerDescriptor> servers, String parentPlugin) {
        if (servers != null && !servers.isEmpty()) {
            for (ServerDescriptor serverDescriptor : servers) {
                if (doServersExtendParent(serverDescriptor.getServers(), parentPlugin)) {
                    return true;
                }
                if (doServicesExtendParent(serverDescriptor.getServices(), parentPlugin)) {
                    return true;
                }
                if (parentPlugin.equals(serverDescriptor.getSourcePlugin())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doServicesExtendParent(List<ServiceDescriptor> services, String parentPlugin) {
        if (services != null && !services.isEmpty()) {
            for (ServiceDescriptor serviceDescriptor : services) {
                if (doServicesExtendParent(serviceDescriptor.getServices(), parentPlugin)) {
                    return true;
                }
                if (parentPlugin.equals(serviceDescriptor.getSourcePlugin())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This will set the given resource types to be the set of types that are to be ignored.
     *
     * @param ignoredTypes all the types to now ignore - overrides any set of types that previously were ignored
     */
    public void setIgnoredResourceTypes(Collection<ResourceType> ignoredTypes) {
        synchronized (disabledIgnoredTypesLock) {
            if (ignoredTypes == null || ignoredTypes.isEmpty()) {
                this.ignoredResourceTypes = null;
            } else {
                if (this.ignoredResourceTypes == null) {
                    this.ignoredResourceTypes = new HashSet<ResourceType>(ignoredTypes);
                } else {
                    this.ignoredResourceTypes.clear();
                    this.ignoredResourceTypes.addAll(ignoredTypes);
                }
            }
        }
    }

    /**
     * This will define resource types that will be disabled.
     *
     * Owners of this metadata manager need to set this during initialization prior to
     * loading any plugins. Calling this after this manager has been initialized will have no effect.
     *
     * @param disabledTypesAsStrings list of types, in the form "pluginName>parentType>childType"
     */
    public void setDisabledResourceTypes(List<String> disabledTypesAsStrings) {

        synchronized (disabledIgnoredTypesLock) {
            if (disabledTypesAsStrings != null && !disabledTypesAsStrings.isEmpty()) {
                this.disabledResourceTypesAsStrings = new ArrayList<String>(disabledTypesAsStrings);
                log.info("Will disable the following resource types: " + this.disabledResourceTypesAsStrings);
            } else {
                this.disabledResourceTypesAsStrings = null;
            }
        }
        return;
    }

    /**
     * Returns true if the given resource type was either disabled at initialization time
     * or subsequently ignored. Both have the same effect, a disabled or ignored resource type
     * means there should not be resources of that type in inventory.
     *
     * @param resourceType the type to check
     * @return true if no resources of this type should be in inventory; if resourceType is null
     *         then false is returned
     */
    public boolean isDisabledOrIgnoredResourceType(ResourceType resourceType) {
        if (resourceType == null) {
            return false;
        }

        synchronized (disabledIgnoredTypesLock) {
            // is it ignored?
            if (this.ignoredResourceTypes != null) {
                if (this.ignoredResourceTypes.contains(resourceType)) {
                    return true;
                }
            }
            // is it disabled?
            if (this.disabledResourceTypes != null) {
                if (this.disabledResourceTypes.containsKey(resourceType)) {
                    return true;
                }
            }
        }
        // its neither disabled nor ignored
        return false;
    }

    // finds if type or its children are disabled and if so adds them to the map with their string representation
    private void findDisabledResourceTypes(String parentHierarchy, ResourceType type,
    // NOTE: we don't synchronize on disabledTypesLock because we ensured our calling context already has the lock
        HashMap<ResourceType, String> disabledTypes) {
        // this is the current level we are at in the type hierarchy, as written in string form ("plugin>type>type...")
        String typeHierarchy = parentHierarchy + '>' + type.getName();

        // see if the given type is to be disabled - if so, add it to the map
        if (this.disabledResourceTypesAsStrings.contains(typeHierarchy)) {
            log.debug("Disabling resource type: " + type);
            disabledTypes.put(type, typeHierarchy);
        }

        // recursively call ourselves to see if any child types are to be disabled
        Set<ResourceType> childTypes = type.getChildResourceTypes();
        if (childTypes != null) {
            for (ResourceType childType : childTypes) {
                findDisabledResourceTypes(typeHierarchy, childType, disabledTypes);
            }
        }

        return;
    }

    private void findDisabledResourceTypesInAllPlugins() {
        // NOTE: we don't synchronize on disabledTypesLock because we ensured our calling context already has the lock
        if (this.disabledResourceTypesAsStrings != null) {
            int totalToBeDisabled = this.disabledResourceTypesAsStrings.size();
            // we have to do it all over again over all plugins because we need to support the injection extension model
            // that is, <runs-inside> - new plugins coming online might have injected types in previously loaded plugins
            HashMap<ResourceType, String> disabledTypes = new HashMap<ResourceType, String>();
            for (Map.Entry<String, PluginMetadataParser> entry : parsersByPlugin.entrySet()) {
                PluginMetadataParser parser = entry.getValue();
                PluginDescriptor pluginDescriptor = parser.getDescriptor();
                Set<ResourceType> rootTypes = parser.getRootResourceTypes();
                String hierarchyStart = pluginDescriptor.getName();
                for (ResourceType rootType : rootTypes) {
                    findDisabledResourceTypes(hierarchyStart, rootType, disabledTypes);
                }
                if (disabledTypes.size() == totalToBeDisabled) {
                    break; // we found them all, no need to look at any more plugins
                }
            }
            this.disabledResourceTypes = (!disabledTypes.isEmpty()) ? disabledTypes : null;
        }
        return;
    }

    /**
     * Given a resource type, this will return any discovery callbacks that are required to be invoked
     * when that resource type is being discovered.
     * @param resourceType the type whose discovery callbacks should be returned
     * @return the collection of callbacks, grouped by the plugins that defined them (may be null)
     */
    public Map<String, List<String>> getDiscoveryCallbacks(ResourceType resourceType) {
        synchronized (discoveryAndResourceUpgradeCallbacksLock) {
            Map<String, List<String>> map = discoveryCallbacks.get(resourceType);
            return map;
        }
    }

    /**
     * Given a resource type, this will return any resource upgrade callbacks that are required to be invoked
     * when a resource of that resource type is being upgraded.
     * @param resourceType the type whose resource upgrade callbacks should be returned
     * @return the collection of callbacks, grouped by the plugins that defined them (may be null)
     */
    public Map<String, List<String>> getResourceUpgradeCallbacks(ResourceType resourceType) {
        synchronized(disabledIgnoredTypesLock) {
            return resourceUpgradeCallbacks.get(resourceType);
        }
    }

    private void addCallbacks(Map<ResourceType, List<String>> callbacksMap, String pluginName,
        Map<ResourceType, Map<String, List<String>>> targetMap) {

        if (callbacksMap != null) {
            for (Map.Entry<ResourceType, List<String>> entry : callbacksMap.entrySet()) {
                ResourceType resourceType = entry.getKey();
                for (String className : entry.getValue()) {
                    addCallbackClassName(resourceType, pluginName, className, targetMap);
                }
            }
        }
    }

    private void addCallbackClassName(ResourceType resourceType, String pluginName, String className, Map<ResourceType,
        Map<String, List<String>>> callbacks) {

        Map<String, List<String>> map = callbacks.get(resourceType);
        if (map == null) {
            map = new HashMap<String, List<String>>(1);
            callbacks.put(resourceType, map);
        }

        List<String> callbackListForPlugin = map.get(pluginName);
        if (callbackListForPlugin == null) {
            callbackListForPlugin = new ArrayList<String>(1);
            map.put(pluginName, callbackListForPlugin);
        }

        callbackListForPlugin.add(className);
    }

    public void cleanupDescriptors() {
        descriptorsByPlugin.clear();
        for (PluginMetadataParser parser : parsersByPlugin.values()) {
            parser.cleanDescriptor();
        }
    }

}
