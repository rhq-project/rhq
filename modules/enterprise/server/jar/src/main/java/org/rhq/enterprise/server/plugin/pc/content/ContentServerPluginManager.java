/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.enterprise.server.plugin.pc.PluginManager;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.content.metadata.ContentSourcePluginMetadataManager;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.content.ContentPluginDescriptorType;

/**
 * This loads in all content source server plugins that can be found and will maintain the complete set of
 * {@link #getMetadataManager() metadata} found in all plugin descriptors from all loaded plugins. You can obtain a
 * loaded plugin's {@link ServerPluginEnvironment environment}, including its classloader, from this object as
 * well.
 *
 * @author John Mazzitelli
 */
public class ContentServerPluginManager extends PluginManager {

    private ContentSourcePluginMetadataManager metadataManager;

    public ContentServerPluginManager(ContentServerPluginContainer pc) {
        super(pc);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        metadataManager = new ContentSourcePluginMetadataManager();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        metadataManager = null;
    }

    /**
     * Gets the plugin environment for the plugin responsible for managing the given content source type.
     *
     * @param  type
     *
     * @return plugin environment
     */
    public ServerPluginEnvironment getPluginEnvironment(ContentSourceType type) {
        String pluginName = this.metadataManager.getPluginNameFromContentSourceType(type);
        return getPluginEnvironment(pluginName);
    }

    /**
     * An object that can be used to process and store all metadata from all content plugins. This object will contain all the
     * metadata found in all loaded content plugins.
     *
     * @return object to retrieve plugin metadata from
     */
    public ContentSourcePluginMetadataManager getMetadataManager() {
        return metadataManager;
    }

    @Override
    public void loadPlugin(ServerPluginEnvironment env) throws Exception {

        super.loadPlugin(env);

        Collection<ContentSourceType> newTypes;
        newTypes = this.metadataManager.loadPlugin((ContentPluginDescriptorType) env.getPluginDescriptor());

        // double check that the api classes are loadable, if not, very bad
        Set<ContentSourceType> newTypesCopy = new HashSet<ContentSourceType>(newTypes);
        for (ContentSourceType newType : newTypesCopy) {
            try {
                String className = newType.getContentSourceApiClass();
                Class<?> apiClass = Class.forName(className, false, env.getClassLoader());

                if (!ContentProvider.class.isAssignableFrom(apiClass)) {
                    throw new Exception("The API class [" + className + "] should implement ["
                        + ContentProvider.class.getName() + "] but does not");
                }
            } catch (Exception e) {
                // do not deploy this plugin - its stinky
                try {
                    unloadPlugin(env);
                } catch (Exception ignore) {
                }
                throw e;
            }
        }

        return;
    }

    @Override
    public void unloadPlugin(ServerPluginEnvironment env) throws Exception {
        metadataManager.unloadPlugin(env.getPluginName());
        super.unloadPlugin(env);
    };
}