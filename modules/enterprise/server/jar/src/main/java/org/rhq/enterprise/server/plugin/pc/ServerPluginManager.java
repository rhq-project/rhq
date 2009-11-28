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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginComponentType;

/**
 * Provides functionality to manage plugins for a plugin container. Plugin containers
 * can install their own plugin managers that are extensions to this class if they need to.
 * 
 * @author John Mazzitelli
 */
//TODO: need a R/W lock to make this class thread safe
public class ServerPluginManager {
    private final Log log = LogFactory.getLog(this.getClass());

    private final AbstractTypeServerPluginContainer parentPluginContainer;

    /**
     * The map of all plugin environments keyed on plugin name.
     */
    private final Map<String, ServerPluginEnvironment> loadedPlugins;

    /**
     * To avoid having to create contexts multiple times for the same plugin,
     * contexts are cached here, keyed on plugin name.
     */
    private final Map<String, ServerPluginContext> pluginContextCache;

    /**
     * Cached instances of objects used to initialize and shutdown individual plugins.
     * Only plugins that declare their own plugin component will have objects in this cache.
     * This is keyed on plugin name.
     */
    private final Map<String, ServerPluginComponent> pluginComponentCache;

    /**
     * Creates a plugin manager for the given plugin container.
     * 
     * @param pc the plugin manager's owning plugin container
     */
    public ServerPluginManager(AbstractTypeServerPluginContainer pc) {
        this.parentPluginContainer = pc;
        this.loadedPlugins = new HashMap<String, ServerPluginEnvironment>();
        this.pluginContextCache = new HashMap<String, ServerPluginContext>();
        this.pluginComponentCache = new HashMap<String, ServerPluginComponent>();
    }

    /**
     * Initializes the plugin manager to prepare it to start loading plugins.
     * 
     * @throws Exception if failed to initialize
     */
    public void initialize() throws Exception {
        log.debug("Plugin manager initializing");
        return; // no-op
    }

    /**
     * Shuts down this manager. This should be called only after all of its plugins
     * have been {@link #unloadPlugin(ServerPluginEnvironment) unloaded}.
     */
    public void shutdown() {
        log.debug("Plugin manager shutting down");

        if (this.loadedPlugins.size() > 0) {
            log.warn("Server plugin manager is being shutdown while some plugins are still loaded: "
                + this.loadedPlugins);
        }

        this.loadedPlugins.clear();
        this.pluginContextCache.clear();
        this.pluginComponentCache.clear();

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
        String pluginName = env.getPluginName();
        log.debug("Loading server plugin [" + pluginName + "] from: " + env.getPluginUrl());

        // tell the plugin we are loading it
        ServerPluginComponent component = createServerPluginComponent(env);
        if (component != null) {
            ServerPluginContext context = getServerPluginContext(env);
            log.debug("Initializing plugin component for server plugin [" + pluginName + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                component.initialize(context);
            } catch (Throwable t) {
                throw new Exception("Plugin component failed to initialize plugin", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
            this.pluginComponentCache.put(pluginName, component);
        }

        // note that we only cache it if the plugin component was successful
        this.loadedPlugins.put(pluginName, env);

        return;
    }

    public void startPlugins() {
        log.debug("Starting plugins");

        // tell all plugin components to start
        for (Map.Entry<String, ServerPluginComponent> entry : this.pluginComponentCache.entrySet()) {
            String pluginName = entry.getKey();
            ServerPluginComponent component = entry.getValue();
            ServerPluginEnvironment env = this.loadedPlugins.get(pluginName);
            log.debug("Starting plugin component for server plugin [" + pluginName + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                component.start();
            } catch (Throwable t) {
                log.warn("Plugin component failed to start plugin [" + pluginName + "]", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        }

        return;
    }

    public void stopPlugins() {
        log.debug("Stopping plugins");

        // tell all plugin components to stop
        for (Map.Entry<String, ServerPluginComponent> entry : this.pluginComponentCache.entrySet()) {
            String pluginName = entry.getKey();
            ServerPluginComponent component = entry.getValue();
            ServerPluginEnvironment env = this.loadedPlugins.get(pluginName);
            log.debug("Stopping plugin component for server plugin [" + pluginName + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                component.stop();
            } catch (Throwable t) {
                log.warn("Plugin component failed to stop plugin [" + pluginName + "]", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        }

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
        String pluginName = env.getPluginName();
        log.debug("Unloading server plugin [" + pluginName + "]");

        try {
            // tell the plugin we are unloading it
            ServerPluginComponent component = this.pluginComponentCache.get(pluginName);
            if (component != null) {
                log.debug("Shutting down plugin componentfor server plugin [" + pluginName + "]");
                ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                    component.shutdown();
                } catch (Throwable t) {
                    throw new Exception("Plugin plugin componentfailed to initialize plugin", t);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                    this.pluginComponentCache.remove(pluginName);
                }
            }
        } finally {
            this.loadedPlugins.remove(pluginName);
        }

        return;
    }

    /**
     * Returns the {@link ServerPluginEnvironment}s for every plugin this manager has loaded.
     * The returned collection is a copy and not backed by this manager.
     *
     * @return environments for all the plugins
     */
    public Collection<ServerPluginEnvironment> getPluginEnvironments() {
        return new ArrayList<ServerPluginEnvironment>(this.loadedPlugins.values());
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

    /**
     * Returns the main plugin component instance that is responsible for initializing and managing
     * the plugin. This will return <code>null</code> if a plugin has not defined a plugin component.
     * 
     * @param pluginName the name of the plugin whose plugin component is to be returned
     * 
     * @return the plugin component instance that initialized and is managing a plugin. Will
     *         return <code>null</code> if the plugin has not defined a plugin component. 
     *         <code>null</code> is also returned if the plugin is not initialized yet. 
     */
    public ServerPluginComponent getServerPluginComponent(String pluginName) {
        return this.pluginComponentCache.get(pluginName);
    }

    public AbstractTypeServerPluginContainer getParentPluginContainer() {
        return this.parentPluginContainer;
    }

    protected Log getLog() {
        return this.log;
    }

    protected ServerPluginContext getServerPluginContext(ServerPluginEnvironment env) {

        String pluginName = env.getPluginName();
        ServerPluginContext context = this.pluginContextCache.get(pluginName);

        // if we already created it, return it immediately and don't create another
        if (context != null) {
            return context;
        }

        MasterServerPluginContainer masterPC = this.parentPluginContainer.getMasterServerPluginContainer();
        MasterServerPluginContainerConfiguration masterConfig = masterPC.getConfiguration();
        File dataDir = new File(masterConfig.getDataDirectory(), pluginName);
        File tmpDir = masterConfig.getTemporaryDirectory();

        Configuration plugnConfig;
        List<ScheduledJobDefinition> schedules;

        try {
            ServerPlugin plugin = getPlugin(env);
            plugnConfig = plugin.getPluginConfiguration();
            Configuration scheduledJobsConfig = plugin.getScheduledJobsConfiguration();
            schedules = ServerPluginDescriptorMetadataParser.getScheduledJobs(scheduledJobsConfig);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get plugin config/schedules from the database", e);
        }

        context = new ServerPluginContext(env, dataDir, tmpDir, plugnConfig, schedules);
        this.pluginContextCache.put(pluginName, context);
        return context;
    }

    /**
     * Given a plugin environment, return its Plugin representation, which should also include
     * the plugin configuration and scheduled jobs configuration.
     * 
     * @param pluginEnv
     * @return the ServerPlugin object for the given plugin
     */
    protected ServerPlugin getPlugin(ServerPluginEnvironment pluginEnv) {
        // get the plugin data from the database
        ServerPluginsLocal serverPluginsManager = LookupUtil.getServerPlugins();
        ServerPlugin plugin = serverPluginsManager.getServerPlugin(pluginEnv.getPluginName());
        return plugin;
    }

    /**
     * This will create a new {@link ServerPluginComponent} instance for that is used to
     * initialize and shutdown a particular server plugin. If there is no plugin component
     * configured for the given plugin, <code>null</code> is returned.
     *
     * The new object will be loaded in the plugin's specific classloader.
     *
     * @param pluginName the name of the plugin whose main component is to be retrieved
     * @param environment the environment in which the plugin will execute
     *
     * @return a new object loaded in the proper plugin classloader that can initialize/shutdown the plugin,
     *         or <code>null</code> if there is no plugin component to be associated with the given plugin
     *
     * @throws Exception if failed to create the instance
     */
    protected ServerPluginComponent createServerPluginComponent(ServerPluginEnvironment environment) throws Exception {

        String pluginName = environment.getPluginName();
        ServerPluginComponent instance = null;

        ServerPluginComponentType componentXml = environment.getPluginDescriptor().getPluginComponent();
        if (componentXml != null) {
            String className = componentXml.getClazz();
            log.debug("Creating plugin component [" + className + "] for plugin [" + pluginName + "]");
            instance = (ServerPluginComponent) instantiatePluginClass(environment, className);
            log.debug("Plugin component created [" + instance.getClass() + "] for plugin [" + pluginName + "]");
        }

        return instance;
    }

    /**
     * Instantiates a class with the given name within the given environment's classloader using
     * the class' no-arg constructor.
     * 
     * @param environment the environment that has the classloader where the class will be loaded
     * @param className the class to instantiate
     * @return the new object that is an instance of the given class
     * @throws Exception if failed to instantiate the class
     */
    protected Object instantiatePluginClass(ServerPluginEnvironment environment, String className) throws Exception {

        ClassLoader loader = environment.getPluginClassLoader();

        String pkg = environment.getPluginDescriptor().getPackage();
        if ((className.indexOf('.') == -1) && (pkg != null)) {
            className = pkg + '.' + className;
        }

        log.debug("Loading server plugin class [" + className + "]...");

        try {
            Class<?> clazz = Class.forName(className, true, loader);
            log.debug("Loaded server plugin class [" + clazz + "].");
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new Exception("Could not instantiate plugin class [" + className + "] from plugin environment ["
                + environment + "]", e);
        } catch (IllegalAccessException e) {
            throw new Exception("Could not access plugin class " + className + "] from plugin environment ["
                + environment + "]", e);
        } catch (ClassNotFoundException e) {
            throw new Exception("Could not find plugin class " + className + "] from plugin environment ["
                + environment + "]", e);
        } catch (NullPointerException npe) {
            throw new Exception("Plugin class was 'null' in plugin environment [" + environment + "]", npe);
        }
    }
}
