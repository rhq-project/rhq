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
package org.rhq.enterprise.server.plugin.pc;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.generic.GenericServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.perspective.PerspectiveServerPluginContainer;

/**
 * The container responsible for managing all the plugin containers for all the
 * different plugin types.
 *
 * @author John Mazzitelli
 */
public class MasterServerPluginContainer {
    private static final Log log = LogFactory.getLog(MasterServerPluginContainer.class);

    private MasterServerPluginContainerConfiguration configuration;
    private Map<ServerPluginType, AbstractTypeServerPluginContainer> pluginContainers = new HashMap<ServerPluginType, AbstractTypeServerPluginContainer>();

    /**
     * Starts the master plugin container, which will load all plugins and begin managing them.
     *
     * @param config
     */
    public void initialize(MasterServerPluginContainerConfiguration config) {
        log.debug("Master server plugin container has been initialized with config: " + config);

        this.configuration = config;

        // create all known child plugin containers
        createPluginContainers(this.pluginContainers);

        // initialize all the plugin containers
        for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : this.pluginContainers.entrySet()) {
            log.info("Master PC is initializing plugin container for plugin type [" + entry.getKey() + "]");
            try {
                entry.getValue().initialize();
            } catch (Exception e) {
                log.error("Failed to initialize plugin container for plugin type [" + entry.getKey() + "]", e);
            }
        }

        return;
    }

    /**
     * Create the individual plugin containers that can be used to deploy different plugin types.
     * 
     * <p>This is protected to allow subclasses to override the PCs that are created by this service (mainly to support tests).</p>
     * 
     * @param pcs the map to store references to the new plugin containers created by this method
     */
    protected void createPluginContainers(Map<ServerPluginType, AbstractTypeServerPluginContainer> pcs) {
        pcs.put(ServerPluginType.GENERIC, new GenericServerPluginContainer(this));
        pcs.put(ServerPluginType.CONTENT, new ContentServerPluginContainer(this));
        pcs.put(ServerPluginType.PERSPECTIVE, new PerspectiveServerPluginContainer(this));
        pcs.put(ServerPluginType.ALERT, new AlertServerPluginContainer(this));
    }

    /**
     * Stops all plugins and cleans up after them.
     */
    public void shutdown() {
        log.debug("Master server plugin container is being shutdown");

        // shutdown all the plugin containers
        for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : this.pluginContainers.entrySet()) {
            log.info("Master PC is shutting down plugin container for plugin type [" + entry.getKey() + "]");
            try {
                entry.getValue().shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown plugin container for plugin type [" + entry.getKey() + "]", e);
            }
        }

        this.pluginContainers.clear();
        this.configuration = null;
    }

    /**
     * Returns the configuration that this object was initialized with. If this plugin container was not
     * {@link #initialize(MasterServerPluginContainerConfiguration) initialized} or has been {@link #shutdown() shutdown},
     * this will return <code>null</code>.
     *
     * @return the configuration
     */
    public MasterServerPluginContainerConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Get the plugin container that supports the given type of plugins.
     * 
     * @param serverPluginType the type of plugin whose plugin container is to be returned
     * @return the plugin container that manages plugins of the given type (<code>null</code> if none found)
     */
    public AbstractTypeServerPluginContainer getPluginContainer(ServerPluginType serverPluginType) {
        return this.pluginContainers.get(serverPluginType);
    }

    /**
     * Get the plugin container of the given class. This method provides a strongly typed return value,
     * based on the type of plugin container the caller wants returned.
     * 
     * @param clazz the class name of the plugin container that the caller wants
     * @return the plugin container of the given class (<code>null</code> if none found)
     */
    public <T> T getPluginContainer(Class<T> clazz) {
        for (AbstractTypeServerPluginContainer pc : this.pluginContainers.values()) {
            if (clazz.isInstance(pc)) {
                return (T) pc;
            }
        }
        return null;
    }
}