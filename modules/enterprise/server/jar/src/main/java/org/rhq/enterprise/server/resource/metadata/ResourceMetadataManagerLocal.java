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

import java.util.List;

import javax.ejb.Local;
import javax.persistence.NoResultException;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.plugin.Plugin;

/**
 */
@Local
public interface ResourceMetadataManagerLocal {
    /**
     * For server-side registration of plugin archives. At server startup or as new plugins are runtime deployed the jar
     * will have its descriptor read and parsed and the metadata for the plugin will be updated in the db.
     *
     * @param plugin   The plugin object being deployed
     * @param metadata The plugin descriptor file
     */
    public void registerPlugin(Plugin plugin, PluginDescriptor metadata);

    /**
     * Returns the list of all plugins deployed in the server.
     * 
     * @return list of plugins deployed
     */
    public List<Plugin> getPlugins();

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
}