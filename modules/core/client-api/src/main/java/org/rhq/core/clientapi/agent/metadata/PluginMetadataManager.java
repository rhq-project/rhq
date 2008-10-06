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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.jetbrains.annotations.Nullable;

/**
 * This is meant to provide an interface to the underlying metadata of a plugin. It will load, translate and cache the
 * metadata for the rest of the services in the form of the domain object classes and the jaxb version of the
 * descriptors.
 *
 * <p/>
 *
 * @author Greg Hinkle
 */
public class PluginMetadataManager {
    public static final ResourceType TEST_PLATFORM_TYPE = new ResourceType("Anonymous", "test",
        ResourceCategory.PLATFORM, null);

    private Log log = LogFactory.getLog(PluginMetadataManager.class);

    private Map<ResourceCategory, LinkedHashSet<ResourceType>> typesByCategory = new HashMap<ResourceCategory, LinkedHashSet<ResourceType>>();

    private Set<ResourceType> types = new HashSet<ResourceType>();

    private Map<String, PluginMetadataParser> parsersByPlugin = new HashMap<String, PluginMetadataParser>();

    public PluginMetadataManager() {
    }

    private void addType(ResourceType type) {
        ResourceCategory category = type.getCategory();
        if (!typesByCategory.containsKey(category)) {
            typesByCategory.put(category, new LinkedHashSet<ResourceType>());
        }

        typesByCategory.get(category).add(type);
        types.add(type);
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

    // views

    public String getDiscoveryClass(ResourceType resourceType) {
        PluginMetadataParser parser = this.parsersByPlugin.get(resourceType.getPlugin());
        return (parser != null) ? parser.getDiscoveryComponentClass(resourceType) : null;
    }

    public String getComponentClass(ResourceType resourceType) {
        PluginMetadataParser parser = this.parsersByPlugin.get(resourceType.getPlugin());
        return (parser != null) ? parser.getComponentClass(resourceType) : null;
    }

    /**
     * Transforms the pluginDescriptor into domain object form and stores into this objects type system.
     *
     * @param  pluginDescriptor the descriptor to transform
     *
     * @return the root resource types represented by this descriptor
     */
    public synchronized Set<ResourceType> loadPlugin(PluginDescriptor pluginDescriptor) {
        try {
            PluginMetadataParser parser = new PluginMetadataParser(pluginDescriptor, parsersByPlugin);

            PluginMetadataParser oldParser = this.parsersByPlugin.get(pluginDescriptor.getName());
            if (oldParser != null) {
                // This is a redeploy, first delete all the original types
                for (ResourceType oldType : oldParser.getAllTypes()) {
                    this.typesByCategory.get(oldType.getCategory()).remove(oldType);
                    this.types.remove(oldType);
                }
            }

            this.parsersByPlugin.put(pluginDescriptor.getName(), parser);
            for (ResourceType resourceType : parser.getAllTypes()) {
                if (types.contains(resourceType)) {
                    throw new InvalidPluginDescriptorException("Type [" + resourceType
                        + "] is duplicate for this plugin. This is illegal.");
                }
                addType(resourceType);
            }

            return parser.getRootResourceTypes();
        } catch (InvalidPluginDescriptorException e) {
            // TODO Should we throw back or log partial failures or store them against the definitions?
            log.error("Error transforming plugin descriptor [" + pluginDescriptor.getName() + "].", e);
        }

        return null;
    }

    // TODO Is this really appropriate? Its inconvient to look up including the parent but
    // that is currently the accurate limiter... need to consider type keys
    // Otherwise we could use the business key which is the "plugin" name and the type name.
    public ResourceType getType(String typeName, ResourceCategory category) {
        for (ResourceType type : getTypesForCategory(category)) {
            if (type.getName().equals(typeName)) {
                return type;
            }
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
        for (ResourceType t : types) {
            if (t.equals(searchType)) {
                return t;
            }
        }
        return null;
    }

    @Nullable
    public ResourceType getType(ResourceType resourceType) {
        return getType(resourceType.getName(), resourceType.getPlugin());
    }

    public Set<ResourceType> getTypesForCategory(ResourceCategory category) {
        LinkedHashSet<ResourceType> types = this.typesByCategory.get(category);
        return (types != null) ? types : new HashSet<ResourceType>();
    }

    public Set<ResourceType> getAllTypes() {
        return types;
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
}