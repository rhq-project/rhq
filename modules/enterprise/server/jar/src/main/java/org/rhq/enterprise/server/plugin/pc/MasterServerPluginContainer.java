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

    // this class is implemented such that it is subclassable - mainly to support tests;
    // as such, methods should not directly access these private fields, instead, use the getters and setters
    private MasterServerPluginContainerConfiguration configuration;
    private Map<ServerPluginType, AbstractTypeServerPluginContainer> pluginContainers;

    /**
     * Starts the master plugin container, which will load all plugins and begin managing them.
     *
     * @param config
     */
    public void initialize(MasterServerPluginContainerConfiguration config) {
        log.debug("Master server plugin container has been initialized with config: " + config);

        setConfiguration(config);

        // create all known child plugin containers
        HashMap<ServerPluginType, AbstractTypeServerPluginContainer> pcs = new HashMap<ServerPluginType, AbstractTypeServerPluginContainer>();
        pcs.put(ServerPluginType.GENERIC, new GenericServerPluginContainer(this));
        pcs.put(ServerPluginType.CONTENT, new ContentServerPluginContainer(this));
        pcs.put(ServerPluginType.PERSPECTIVE, new PerspectiveServerPluginContainer(this));
        pcs.put(ServerPluginType.ALERT, new AlertServerPluginContainer(this));
        setPluginContainers(pcs);

        initializePluginContainers();

        return;
    }

    protected void initializePluginContainers() {
        // initialize all the plugin containers
        for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : getPluginContainers().entrySet()) {
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
     * Stops all plugins and cleans up after them.
     */
    public void shutdown() {
        log.debug("Master server plugin container is being shutdown");

        shutdownPluginContainers();
        getPluginContainers().clear();
        setPluginContainers(null);
        setConfiguration(null);
    }

    protected void shutdownPluginContainers() {
        // shutdown all the plugin containers
        for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : getPluginContainers().entrySet()) {
            log.info("Master PC is shutting down plugin container for plugin type [" + entry.getKey() + "]");
            try {
                entry.getValue().shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown plugin container for plugin type [" + entry.getKey() + "]", e);
            }
        }
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

    protected void setConfiguration(MasterServerPluginContainerConfiguration config) {
        this.configuration = config;
    }

    public AbstractTypeServerPluginContainer getPluginContainer(ServerPluginType serverPluginType) {
        return getPluginContainers().get(serverPluginType);
    }

    public <T> T getPluginContainer(Class<T> clazz) {
        for (AbstractTypeServerPluginContainer pc : getPluginContainers().values()) {
            if (clazz.isInstance(pc)) {
                return (T) pc;
            }
        }
        return null;
    }

    protected Map<ServerPluginType, AbstractTypeServerPluginContainer> getPluginContainers() {
        if (this.pluginContainers == null) {
            throw new IllegalStateException("Plugin containers is null; is the master plugin container started?");
        }
        return this.pluginContainers;
    }

    protected void setPluginContainers(Map<ServerPluginType, AbstractTypeServerPluginContainer> pcs) {
        this.pluginContainers = pcs;
    }
}