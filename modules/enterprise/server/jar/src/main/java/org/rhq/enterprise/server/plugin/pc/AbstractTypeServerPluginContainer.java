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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The abstract superclass for all plugin containers of the different {@link ServerPluginType plugin types}.
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractTypeServerPluginContainer {

    private final Log log = LogFactory.getLog(this.getClass());

    private final MasterServerPluginContainer master;
    private PluginManager pluginManager;

    /**
     * Instantiates the plugin container. All subclasses must support this and only this
     * constructor.
     * 
     * @param master the master plugin container that is creating this instance.
     */
    public AbstractTypeServerPluginContainer(MasterServerPluginContainer master) {
        this.master = master;
    }

    /**
     * Each plugin container will tell the master which plugins it can support via this method; this
     * method returns the type of plugin that the plugin container can process. Only one
     * plugin container can support a plugin type.
     * 
     * @return the type of plugin that this plugin container instance can support
     */
    public abstract ServerPluginType getSupportedServerPluginType();

    /**
     * Returns the master plugin container that is responsible for managing this instance.
     * 
     * @return this plugin container's master
     */
    public MasterServerPluginContainer getMasterServerPluginContainer() {
        return this.master;
    }

    /**
     * Returns the object that manages the plugins.
     * 
     * @return the plugin manager for this container
     */
    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    /**
     * The initialize method that prepares the plugin container.
     * 
     * Subclasses are free to perform additional tasks by overriding this method.
     *
     * @throws Exception if the plugin container failed to initialize for some reason
     */
    public void initialize() throws Exception {
        this.pluginManager = createPluginManager();
        this.pluginManager.initialize();
    }

    /**
     * The shutdown method that will stop and unload all plugins.
     * 
     * Subclasses are free to perform additional tasks by overriding this method.
     */
    public void shutdown() {
        Collection<ServerPluginEnvironment> envs = this.pluginManager.getPluginEnvironments();
        for (ServerPluginEnvironment env : envs) {
            try {
                unloadPlugin(env);
            } catch (Exception e) {
                this.log.warn("Failed to unload plugin [" + env.getPluginName() + "].", e);
            }
        }

        try {
            this.pluginManager.shutdown();
        } finally {
            this.pluginManager = null;
        }

        return;
    }

    /**
     * Informs the plugin container that it has a plugin that it must being to start managing.
     * 
     * @param env the plugin environment, including the plugin jar and its descriptor
     *
     * @throws Exception if failed to load the plugin 
     */
    public void loadPlugin(ServerPluginEnvironment env) throws Exception {
        this.pluginManager.loadPlugin(env);
    }

    /**
     * Informs the plugin container that a plugin should be unloaded and any of its resources
     * should be released.
     * 
     * @param env the plugin environment, including the plugin jar and its descriptor
     *
     * @throws Exception if failed to unload the plugin 
     */
    public void unloadPlugin(ServerPluginEnvironment env) throws Exception {
        this.pluginManager.unloadPlugin(env);
    }

    /**
     * This will be called when its time for this plugin container to create its plugin manager.
     * Subclasses are free to override this if they need their own specialized plugin manager.
     * 
     * @return the plugin manager for use by this plugin container
     */
    protected PluginManager createPluginManager() {
        return new PluginManager(this);
    }

    /**
     * Returns the logger that can be used to log messages. A convienence object so all
     * subclasses don't have to explicitly declare and create their own.
     * 
     * @return this instance's logger object
     */
    protected Log getLog() {
        return this.log;
    }
}
