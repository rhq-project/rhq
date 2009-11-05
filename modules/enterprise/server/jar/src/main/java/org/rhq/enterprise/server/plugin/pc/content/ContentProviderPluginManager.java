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
package org.rhq.enterprise.server.plugin.pc.content;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.content.metadata.ContentSourcePluginMetadataManager;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.content.ContentPluginDescriptorType;

/**
 * This loads in all content source server plugins that can be found and will maintain the complete set of
 * {@link #getMetadataManager() metadata} found in all plugin descriptors from all loaded plugins. You can obtain a
 * loaded plugin's {@link ServerPluginEnvironment environment}, including its classloader, from this object as
 * well - see {@link #getPlugin(String)}.
 *
 * @author John Mazzitelli
 */
public class ContentProviderPluginManager {
    private final Log log = LogFactory.getLog(ContentProviderPluginManager.class);

    /**
     * The map of all plugins keyed on plugin name.
     */
    private Map<String, ServerPluginEnvironment> loadedPlugins = new HashMap<String, ServerPluginEnvironment>();
    private ContentSourcePluginMetadataManager metadataManager = new ContentSourcePluginMetadataManager();
    private ContentServerPluginContainer parentPluginContainer;

    public ContentProviderPluginManager(ContentServerPluginContainer pc) {
        this.parentPluginContainer = pc;
    }

    /**
     * Finds all plugins and {@link #loadPlugin(URL, ClassLoader) loads} each plugin found.
     *
     * @throws RuntimeException if failed catastrophically; will not happen if a plugin failed to load - it will just be
     *                          ignored
     */
    public void initialize() {

        try {
            MasterServerPluginContainer master = this.parentPluginContainer.getMasterServerPluginContainer();
            MasterServerPluginContainerConfiguration config = master.getConfiguration();
            File[] pluginFiles = config.getPluginDirectory().listFiles();

            for (File pluginFile : pluginFiles) {
                if (pluginFile.getName().endsWith(".jar")) {
                    URL pluginUrl = pluginFile.toURI().toURL();

                    try {
                        log.debug("Loading content source server plugin from URL: " + pluginUrl);
                        loadPlugin(pluginUrl, null);
                    } catch (Throwable t) {
                        // for some reason, the plugin failed to load - it will be ignored
                        log.error("Plugin at [" + pluginUrl + "] could not be loaded", t);
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            shutdown(); // have to clean up the environments (e.g. unpacked jars) we might have already created
            log.error("Error initializing content provider plugin manager", e);
            throw new RuntimeException("Cannot initialize content provider plugin manager", e);
        }
    }

    /**
     * Ensures that all the plugin classloaders clean up.
     */
    public void shutdown() {
        for (ServerPluginEnvironment pluginEnvironment : loadedPlugins.values()) {
            log.debug("Cleaning up plugin environment for [" + pluginEnvironment.getPluginName() + "]");
            pluginEnvironment.destroy();
        }

        loadedPlugins.clear();
        metadataManager = new ContentSourcePluginMetadataManager();

        return;
    }

    /**
     * Returns the {@link ServerPluginEnvironment}s for every plugin this manager found and loaded.
     *
     * @return environments for all the plugins
     */
    public Collection<ServerPluginEnvironment> getPlugins() {
        return this.loadedPlugins.values();
    }

    /**
     * Returns the {@link ServerPluginEnvironment} for the specific plugin with the given name.
     *
     * <p>The plugin's name is defined in its plugin descriptor - specifically the XML root node's "name" attribute
     * (e.g. &ltplugin name="thePluginName").</p>
     *
     * @param  pluginName plugin name as defined in the plugin's descriptor
     *
     * @return the environment of the loaded plugin with the given name (<code>null</code> if there is no loaded plugin
     *         with the given name)
     */
    @Nullable
    public ServerPluginEnvironment getPlugin(String pluginName) {
        return this.loadedPlugins.get(pluginName);
    }

    /**
     * Gets the plugin environment for the plugin responsible for managing the given content source type.
     *
     * @param  type
     *
     * @return plugin environment
     */
    public ServerPluginEnvironment getPlugin(ContentSourceType type) {
        String pluginName = this.metadataManager.getPluginNameFromContentSourceType(type);
        return this.loadedPlugins.get(pluginName);
    }

    /**
     * An object that can be used to process and store all metadata from all plugins. This object will contain all the
     * metadata found in all loaded plugins.
     *
     * @return object to retrieve plugin metadata from
     */
    public ContentSourcePluginMetadataManager getMetadataManager() {
        return metadataManager;
    }

    /**
     * This will create a {@link ServerPluginEnvironment} for the plugin at the given URL. What this means is a
     * classloader is created for the plugin (if <code>classLoader</code> is <code>null</code>) and the plugin's
     * descriptor is parsed. Once this method returns, the plugin's components are ready to be created and used.
     *
     * @param  pluginUrl   the new plugin's jar location
     * @param  classLoader the new plugin's classloader - if <code>null</code>, one will be created for it
     *
     * @throws Exception
     */
    private void loadPlugin(URL pluginUrl, ClassLoader classLoader) throws Exception {
        log.info("Loading content server plugin from: " + pluginUrl);

        ServerPluginEnvironment pluginEnvironment;

        MasterServerPluginContainer master = this.parentPluginContainer.getMasterServerPluginContainer();
        MasterServerPluginContainerConfiguration config = master.getConfiguration();
        pluginEnvironment = new ServerPluginEnvironment(pluginUrl, classLoader, null, config.getTemporaryDirectory());

        loadedPlugins.put(pluginEnvironment.getPluginName(), pluginEnvironment);
        Collection<ContentSourceType> newTypes = metadataManager
            .loadPlugin((ContentPluginDescriptorType) pluginEnvironment.getPluginDescriptor());

        // double check that the api classes are loadable, if not, very bad
        Set<ContentSourceType> newTypesCopy = new HashSet<ContentSourceType>(newTypes);
        for (ContentSourceType newType : newTypesCopy) {
            try {
                String className = newType.getContentSourceApiClass();
                Class<?> apiClass = Class.forName(className, false, pluginEnvironment.getClassLoader());

                if (!ContentProvider.class.isAssignableFrom(apiClass)) {
                    throw new Exception("The API class [" + className + "] should implement ["
                        + ContentProvider.class.getName() + "] but does not");
                }
            } catch (Exception e) {
                // do not deploy this plugin - its stinky
                metadataManager.unloadPlugin(pluginEnvironment.getPluginName());
                loadedPlugins.remove(pluginEnvironment.getPluginName());
                pluginEnvironment.destroy();
                throw e;
            }
        }

        return;
    }
}