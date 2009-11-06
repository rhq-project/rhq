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

package org.rhq.enterprise.server.plugin.pc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides functionality to manage plugins for a plugin container. Plugin containers
 * can install their own plugin managers that are extensions to this class if they need to.
 * 
 * @author John Mazzitelli
 */
public class PluginManager {
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * The map of all plugins keyed on plugin name.
     */
    private Map<String, ServerPluginEnvironment> loadedPlugins = new HashMap<String, ServerPluginEnvironment>();
    private AbstractTypeServerPluginContainer parentPluginContainer;

    /**
     * Creates a plugin manager for the given plugin container.
     * 
     * @param pc the plugin manager's owning plugin container
     */
    public PluginManager(AbstractTypeServerPluginContainer pc) {
        this.parentPluginContainer = pc;
    }

    /**
     * Initializes the plugin manager to prepare it to start loading plugins.
     * 
     * @throws Exception if failed to initialize
     */
    public void initialize() throws Exception {
        return; // no-op
    }

    /**
     * Shuts down this manager. This should be called only after all of its plugins
     * have been {@link #unloadPlugin(ServerPluginEnvironment) unloaded}.
     */
    public void shutdown() {
        if (this.loadedPlugins.size() > 0) {
            log.warn("Server plugin manager is being shutdown while some plugins are still loaded: "
                + this.loadedPlugins);
        }

        this.loadedPlugins.clear();
        return;
    }

    /**
     * Informs the plugin manager that a plugin with the given environment needs to be loaded.
     * Once this method returns, the plugin's components are ready to be created and used.
     *
     * @param env the environment of the plugin to be loaded
     *
     * @throws Exception if the plugin manager cannot load the plugin or deems the plugin invalid
     */
    public void loadPlugin(ServerPluginEnvironment env) throws Exception {
        log.debug("Loading server plugin [" + env.getPluginName() + "] from: " + env.getPluginUrl());
        this.loadedPlugins.put(env.getPluginName(), env);
        return;
    }

    /**
     * Informs the plugin manager that a plugin with the given environment is to be unloaded.
     * Once this method returns, the plugin's components are should no longer be created or used.
     *
     * @param env the environment of the plugin to be unloaded
     *
     * @throws Exception if the plugin manager cannot unload the plugin
     */
    public void unloadPlugin(ServerPluginEnvironment env) throws Exception {
        log.debug("Unloading server plugin [" + env.getPluginName() + "]");
        this.loadedPlugins.remove(env.getPluginName());
        return;
    }

    /**
     * Returns the {@link ServerPluginEnvironment}s for every plugin this manager has loaded.
     *
     * @return environments for all the plugins
     */
    public Collection<ServerPluginEnvironment> getPluginEnvironments() {
        return this.loadedPlugins.values();
    }

    /**
     * Given a plugin name, this returns that plugin's environment.
     * 
     * <p>The plugin's name is defined in its plugin descriptor - specifically the XML root node's "name" attribute
     * (e.g. &ltserver-plugin name="thePluginName").</p>
     *
     * @param pluginName the plugin whose environment is to be returned
     * @return given plugin's environment
     */
    public ServerPluginEnvironment getPluginEnvironment(String pluginName) {
        return this.loadedPlugins.get(pluginName);
    }

    public AbstractTypeServerPluginContainer getParentPluginContainer() {
        return this.parentPluginContainer;
    }

    protected Log getLog() {
        return this.log;
    }
}
