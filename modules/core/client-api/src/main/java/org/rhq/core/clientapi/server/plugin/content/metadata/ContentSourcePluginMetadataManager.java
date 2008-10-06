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
package org.rhq.core.clientapi.server.plugin.content.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.descriptor.serverplugin.content.ContentSourcePluginDescriptor;
import org.rhq.core.clientapi.descriptor.serverplugin.content.ContentSourceTypeDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;

/**
 * This is meant to provide an interface to the underlying metadata of a server plugin for content. It will load,
 * translate and cache the metadata for the rest of the services in the form of the domain object classes and the jaxb
 * version of the descriptors.
 *
 * @author John Mazzitelli
 */
public class ContentSourcePluginMetadataManager {
    private Log log = LogFactory.getLog(ContentSourcePluginMetadataManager.class);

    // outer map is keyed by plugin name; inner map keyed by content source type name
    private Map<String, Map<String, ContentSourceType>> loadedPlugins;

    public ContentSourcePluginMetadataManager() {
        loadedPlugins = new HashMap<String, Map<String, ContentSourceType>>();
    }

    /**
     * This will remove all knowledge of the given plugin - that is, all content source types deployed by the plugin
     * will be removed from this manager.
     *
     * @param pluginName the plugin whose types are to be removed
     */
    public synchronized void unloadPlugin(String pluginName) {
        loadedPlugins.remove(pluginName);
    }

    /**
     * Transforms the pluginDescriptor into domain object form and stores those objects.
     *
     * @param  pluginDescriptor the descriptor to transform
     *
     * @return the new content source types represented by this descriptor (not a copy)
     */
    public synchronized Collection<ContentSourceType> loadPlugin(ContentSourcePluginDescriptor pluginDescriptor) {
        try {
            Map<String, ContentSourceType> pluginTypes = loadedPlugins.get(pluginDescriptor.getName());

            if (pluginTypes != null) {
                pluginTypes.clear(); // this is a redeploy, delete the original types
            } else {
                pluginTypes = new HashMap<String, ContentSourceType>();
                loadedPlugins.put(pluginDescriptor.getName(), pluginTypes);
            }

            for (ContentSourceTypeDefinition newType : pluginDescriptor.getContentSourceType()) {
                // TODO: right now, content source types cannot have the same name as any other one
                //       even if defined in another plugin (i.e plugin's content types are not scoped)
                //       Do we care?  This just means one plugin dev cannot name a content source type
                //       something that another plugin dev used already in another plugin.
                if (getContentSourceType(newType.getName()) == null) {
                    // create the new domain object represented by the XML
                    ContentSourceType contentSourceType = new ContentSourceType();
                    contentSourceType.setName(newType.getName());
                    contentSourceType.setDisplayName(newType.getDisplayName());
                    contentSourceType.setDescription(newType.getDescription());
                    contentSourceType.setPluginName(pluginDescriptor.getName());
                    contentSourceType.setDefaultLazyLoad(newType.isLazyLoad());
                    contentSourceType.setDefaultDownloadMode(DownloadMode.valueOf(newType.getDownloadMode()
                        .toUpperCase()));
                    contentSourceType.setDefaultSyncSchedule(newType.getSyncSchedule());
                    contentSourceType.setContentSourceApiClass(newType.getApiClass());

                    ConfigurationDefinition configDef = ConfigurationMetadataParser.parse(newType.getName(), newType
                        .getConfiguration());
                    contentSourceType.setContentSourceConfigurationDefinition(configDef);

                    // add the new domain object to the list of domain objects for this plugin
                    pluginTypes.put(contentSourceType.getName(), contentSourceType);
                } else {
                    throw new Exception("Cannot redefine an existing content source type:" + newType.getName());
                }
            }

            return pluginTypes.values();
        } catch (Exception e) {
            log.error("Error transforming plugin descriptor [" + pluginDescriptor.getName() + "]", e);
        }

        return null;
    }

    /**
     * Returns the {@link ContentSourceType} that has the given name. If no plugin defined a type with that name, <code>
     * null</code> is returned.
     *
     * @param  typeName the name of the content source type to return
     *
     * @return the content source type with the given name or <code>null</code> if none exists
     */
    public synchronized ContentSourceType getContentSourceType(String typeName) {
        for (Map<String, ContentSourceType> pluginTypes : loadedPlugins.values()) {
            ContentSourceType type = pluginTypes.get(typeName);
            if (type != null) {
                return type;
            }
        }

        return null;
    }

    /**
     * Returns all the {@link ContentSourceType}s that have been defined in all plugins.
     *
     * @return all content source types defined; will be empty if there are none
     */
    public synchronized Set<ContentSourceType> getAllContentSourceTypes() {
        Set<ContentSourceType> allTypes = new HashSet<ContentSourceType>();

        for (Map<String, ContentSourceType> pluginTypes : loadedPlugins.values()) {
            allTypes.addAll(pluginTypes.values());
        }

        return allTypes;
    }

    /**
     * Returns the names of all plugins that defined content source types.
     *
     * @return all plugin names
     */
    public synchronized Set<String> getPluginNames() {
        return this.loadedPlugins.keySet();
    }

    /**
     * Returns the {@link ContentSourceType}s defined by the given plugin. If there is no plugin with that name, <code>
     * null</code> is returned.
     *
     * @param  pluginName the name of the plugin whose types are to be returned
     *
     * @return the content source types defined by the given plugin or <code>null</code> if no plugin exists with that
     *         name - the returned collection is not a copy, its backed by this manager
     */
    public synchronized Collection<ContentSourceType> getContentSourceTypesByPlugin(String pluginName) {
        Map<String, ContentSourceType> pluginTypes = loadedPlugins.get(pluginName);

        if (pluginTypes == null) {
            return null;
        }

        return pluginTypes.values();
    }

    /**
     * Returns the name of the plugin that deployed the given content source type. <code>null</code> will be returned if
     * unknown.
     *
     * @param  type the name of the plugin that deployed this type is to be returned
     *
     * @return name of the plugin
     */
    public synchronized String getPluginNameFromContentSourceType(ContentSourceType type) {
        Set<String> pluginNames = loadedPlugins.keySet();
        for (String pluginName : pluginNames) {
            if (loadedPlugins.get(pluginName).containsValue(type)) {
                return pluginName;
            }
        }

        return null;
    }
}