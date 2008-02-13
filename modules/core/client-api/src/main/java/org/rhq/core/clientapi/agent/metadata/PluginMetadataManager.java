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
        ResourceType type = new ResourceType("Anonymous", "test", ResourceCategory.PLATFORM, null);
        addType(type);
        return type;
    }

    // views

    public String getDiscoveryClass(ResourceType resourceType) {
        PluginMetadataParser parser = this.parsersByPlugin.get(resourceType.getPlugin());
        return parser.getDiscoveryComponentClass(resourceType);
    }

    public String getComponentClass(ResourceType resourceType) {
        PluginMetadataParser parser = this.parsersByPlugin.get(resourceType.getPlugin());
        return parser.getComponentClass(resourceType);
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

    public ResourceType getType(String resourceTypeName, String pluginName) {
        ResourceType searchType = new ResourceType(resourceTypeName, pluginName, null, null);
        for (ResourceType t : types) {
            if (t.equals(searchType)) {
                return t;
            }
        }

        return null;
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