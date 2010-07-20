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
package org.rhq.enterprise.server.resource.metadata;

import java.io.File;
import java.util.List;

import javax.ejb.Local;
import javax.persistence.NoResultException;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;

/**
 * Provides functionality surrounding agent plugins and their resource metadata.
 */
@Local
public interface ResourceMetadataManagerLocal {

    List<Plugin> getAllPluginsById(List<Integer> pluginIds);

    void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void setPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception;

    /**
     * For server-side registration of plugin archives. At server startup or as new plugins are runtime deployed the jar
     * will have its descriptor read and parsed and the metadata for the plugin will be updated in the db.
     * If you provide a non-null <code>pluginFile</code>, and the plugin is deemed to be new or updated, the content
     * of the file will be streamed to the database. Note that if you provide a non-null file, you must ensure
     * its MD5 matches that of the file (i.e. this method will not attempt to recompute the file's MD5, it will assume
     * the caller has already done that and provided the proper MD5 in <code>plugin</code>).
     * <br/><br/>
     * NOTE ** This call will register the plugin in a new transaction.
     * 
     * @param plugin   The plugin object being deployed
     * @param metadata The plugin descriptor file
     * @param pluginFile the actual plugin file whose content will be stored in the database (will be ignored if null)
     * @param forceUpdate if <code>true</code>, the plugin's types will be updated, even if the plugin hasn't changed since
     *                    the last time it was registered
     */
    void registerPlugin(Subject whoami, Plugin plugin, PluginDescriptor metadata, File pluginFile, boolean forceUpdate)
        throws Exception;

    /**
     * Returns the list of all plugins deployed in the server.
     * 
     * @return list of plugins deployed
     */
    List<Plugin> getPlugins();

    List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName, ResourceCategory resourceCategory);

    /**
     * Given the plugin name, will return that plugin.  The name is defined in the plugin descriptor.
     * 
     * @param  name name of plugin as defined in plugin descriptor.
     *
     * @return the plugin
     *
     * @throws NoResultException when no plugin with that name exists
     */
    Plugin getPlugin(String name);

    /** Exists only to have code execute within its own transaction. Not for general consumption. */
    boolean registerPluginInNewTransaction(Subject whoami, Plugin plugin, PluginDescriptor pluginDescriptor,
        File pluginFile, boolean forceUpdate) throws Exception;

    /** Exists only to have code execute within its own transaction. Not for general consumption. */
    void removeObsoleteTypesInNewTransaction(String pluginName);

    /** Method to add a runtime-created resourceType and one/more metric(s) to an existing plugin */
    void addNewResourceType(String newResourceTypeName, String metricName);
}
