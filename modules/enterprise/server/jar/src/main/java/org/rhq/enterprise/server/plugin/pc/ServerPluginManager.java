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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Provides functionality to manage plugins for a plugin container. Plugin containers
 * can install their own plugin managers that are extensions to this class if they need to.
 *
 * Most of the methods here are protected; they are meant for the plugin container's use only.
 * Usually, anything an external client needs will be done through a delegation method
 * found on the plugin container.
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
     * Indicates which plugins are enabled and which are disabled; keyed on plugin name.
     */
    private final Map<String, Boolean> enabledPlugins;

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
        this.enabledPlugins = new HashMap<String, Boolean>();
    }

    /**
     * Returns the plugin container that whose plugins are managed by this manager.
     *
     * @return the plugin container that owns this plugin manager
     */
    public AbstractTypeServerPluginContainer getParentPluginContainer() {
        return this.parentPluginContainer;
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
     * Initializes the plugin manager to prepare it to start loading plugins.
     *
     * @throws Exception if failed to initialize
     */
    protected void initialize() throws Exception {
        log.debug("Plugin manager initializing");
        return; // no-op
    }

    /**
     * Shuts down this manager. This should be called only after all of its plugins
     * have been {@link #unloadPlugin(ServerPluginEnvironment) unloaded}.
     */
    protected void shutdown() {
        log.debug("Plugin manager shutting down");

        if (this.loadedPlugins.size() > 0) {
            log.warn("Server plugin manager is being shutdown while some plugins are still loaded: "
                + this.loadedPlugins);
        }

        this.loadedPlugins.clear();
        this.pluginContextCache.clear();
        this.pluginComponentCache.clear();
        this.enabledPlugins.clear();

        return;
    }

    /**
     * Informs the plugin manager that a plugin with the given environment needs to be loaded.
     * Once this method returns, the plugin's components are ready to be created and used, unless
     * <code>enabled</code> is <code>false</code>, in which case the plugin will not
     * be initialized.
     *
     * @param env the environment of the plugin to be loaded
     * @param enabled <code>true</code> if the plugin should be initialized; <code>false</code> if
     *        the plugin's existence should be noted but it should not be initialized or started
     *
     * @throws Exception if the plugin manager cannot load the plugin or deems the plugin invalid.
     *                   Typically, this method will not throw an exception unless enabled is
     *                   <code>true</code> - loading a disabled plugin is trivial and should not
     *                   fail or throw an exception.
     */
    protected void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        String pluginName = env.getPluginKey().getPluginName();
        log.debug("Loading server plugin [" + pluginName + "] from: " + env.getPluginUrl());

        if (enabled) {
            // tell the plugin we are loading it
            ServerPluginComponent component = null;
            try {
                component = createServerPluginComponent(env);
            } catch (Throwable t) {
                throw new Exception("Plugin component failed to be created for server plugin [" + pluginName + "]", t);
            }
            if (component != null) {
                ServerPluginContext context = getServerPluginContext(env);
                log.debug("Initializing plugin component for server plugin [" + pluginName + "]");
                ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                    component.initialize(context);
                } catch (Throwable t) {
                    throw new Exception("Plugin component failed to initialize server plugin [" + pluginName + "]", t);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                }
                this.pluginComponentCache.put(pluginName, component);
            }
        } else {
            log.info("Server plugin [" + pluginName + "] is loaded but disabled");
        }

        // note that we only cache it if the plugin component was successful
        this.loadedPlugins.put(pluginName, env);
        this.enabledPlugins.put(pluginName, Boolean.valueOf(enabled));

        return;
    }

    protected void startPlugins() {
        log.debug("Starting server plugins");
        for (String pluginName : this.pluginComponentCache.keySet()) {
            startPlugin(pluginName);
        }
        log.debug("Server plugins started.");
        return;
    }

    protected void startPlugin(String pluginName) {
        if (isPluginEnabled(pluginName)) {
            ServerPluginComponent component = this.pluginComponentCache.get(pluginName);
            if (component != null) {
                ServerPluginEnvironment env = this.loadedPlugins.get(pluginName);
                log.debug("Starting plugin component for server plugin [" + pluginName + "]");
                ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    if (env == null) {
                        throw new Exception("Plugin [" + pluginName + "] was never loaded");
                    }
                    Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                    component.start();
                } catch (Throwable t) {
                    log.warn("Plugin component failed to start plugin [" + pluginName + "]", t);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                }
            }
        }
        return;
    }

    protected void stopPlugins() {
        log.debug("Stopping server plugins");
        for (String pluginName : this.pluginComponentCache.keySet()) {
            stopPlugin(pluginName);
        }
        log.debug("Server plugins stopped.");
        return;
    }

    protected void stopPlugin(String pluginName) {
        ServerPluginComponent component = this.pluginComponentCache.get(pluginName);
        if (component != null) {
            ServerPluginEnvironment env = this.loadedPlugins.get(pluginName);
            log.debug("Stopping plugin component for server plugin [" + pluginName + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                if (env == null) {
                    throw new Exception("Plugin [" + pluginName + "] was never loaded");
                }
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
     * Informs the plugin manager that a plugin with the given name is to be unloaded.
     * The component's shutdown method will be called.
     * 
     * @param pluginName the name of the plugin to be unloaded
     *
     * @throws Exception if the plugin manager cannot unload the plugin
     */
    protected void unloadPlugin(String pluginName) throws Exception {
        try {
            ServerPluginEnvironment env = getPluginEnvironment(pluginName);
            if (env == null) {
                log.debug("Server plugin [" + pluginName + "] was never loaded, ignoring unload request");
                return;
            }

            log.debug("Unloading server plugin [" + pluginName + "]");

            // tell the plugin we are unloading it
            ServerPluginComponent component = this.pluginComponentCache.get(pluginName);
            if (component != null) {
                log.debug("Shutting down plugin component for server plugin [" + pluginName + "]");
                ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                    component.shutdown();
                } catch (Throwable t) {
                    throw new Exception("Plugin component failed to shutdown server plugin [" + pluginName + "]", t);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                    this.pluginComponentCache.remove(pluginName);
                }
            }
        } finally {
            this.loadedPlugins.remove(pluginName);
            this.enabledPlugins.remove(pluginName);
            this.pluginContextCache.remove(pluginName);
        }

        return;
    }

    /**
     * Informs the plugin manager that a plugin with the given name is to be unloaded.
     * Once this method returns, the plugin's components should not be created or used.
     *
     * If <code>keepClassLoader</code> is <code>true</code>, this is the same as
     * {@link #unloadPlugin(String)}.
     *
     * You want to keep the classloader if you are only temporarily unloading the plugin, and
     * will load it back soon.
     *
     * Subclasses of this plugin manager class will normally not override this method; instead,
     * they will typically want to override {@link #unloadPlugin(String)}.
     *
     * @param pluginName the name of the plugin to be unloaded
     * @param keepClassLoader if <code>true</code> the classloader is not destroyed
     * @throws Exception if the plugin manager cannot unload the plugin
     */
    protected void unloadPlugin(String pluginName, boolean keepClassLoader) throws Exception {
        try {
            unloadPlugin(pluginName);
        } finally {
            if (!keepClassLoader) {
                String pluginType = getParentPluginContainer().getSupportedServerPluginType().stringify();
                PluginKey pluginKey = PluginKey.createServerPluginKey(pluginType, pluginName);
                MasterServerPluginContainer master = this.parentPluginContainer.getMasterServerPluginContainer();
                master.getClassLoaderManager().unloadPlugin(pluginKey);
            }
        }

        return;
    }

    /**
     * This will reload a plugin allowing you to enable or disable it.
     * This will {@link #startPlugin(String) start the plugin component} if you enable it.
     * This will {@link #stopPlugin(String) stop the plugin component} if you disable it.
     * This will ensure any new plugin configuration will be re-loaded.
     *
     * @param pluginName the name of the loaded plugin that is to be enabled or disabled
     * @param enabled <code>true</code> if you want to enable the plugin; <code>false</code>
     *                if you want to disable it
     * @throws Exception if the plugin was never loaded before or the reload failed
     */
    protected void reloadPlugin(String pluginName, boolean enabled) throws Exception {
        if (enabled) {
            enablePlugin(pluginName);
        } else {
            disablePlugin(pluginName);
        }
        return;
    }

    protected void enablePlugin(String pluginName) throws Exception {
        log.info("Enabling server plugin [" + pluginName + "]");
        ServerPluginEnvironment env = getPluginEnvironment(pluginName);
        if (env == null) {
            throw new IllegalArgumentException("Server plugin [" + pluginName + "] was never loaded, cannot enable it");
        }
        stopPlugin(pluginName); // under normal circumstances, we should not need to do this, but just in case the plugin is somehow already started, stop it
        unloadPlugin(pluginName, true); // unloading it will clean up old data and force the plugin context to reload
        env = rebuildServerPluginEnvironment(env);
        try {
            // reload it in the enabled state.
            loadPlugin(env, true);
        } catch (Exception e) {
            // we've already unloaded it - so even though we failed to enable it, we need to load it back, albeit disabled
            loadPlugin(env, false);
            throw e;
        }
        startPlugin(pluginName); // since we are enabling the plugin, immediately start it
        return;
    }

    protected void disablePlugin(String pluginName) throws Exception {
        log.info("Disabling server plugin [" + pluginName + "]");
        ServerPluginEnvironment env = getPluginEnvironment(pluginName);
        if (env == null) {
            throw new IllegalArgumentException("Server plugin [" + pluginName + "] was never loaded, cannot disable it");
        }
        stopPlugin(pluginName);
        unloadPlugin(pluginName, true); // unloading it will clean up old data and force the plugin context to reload if we later re-enable it
        env = rebuildServerPluginEnvironment(env);
        loadPlugin(env, false); // re-load it in the disabled state
        return;
    }

    protected boolean isPluginLoaded(String pluginName) {
        return this.loadedPlugins.containsKey(pluginName);
    }

    protected boolean isPluginEnabled(String pluginName) {
        return Boolean.TRUE.equals(this.enabledPlugins.get(pluginName));
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
    protected ServerPluginComponent getServerPluginComponent(String pluginName) {
        return this.pluginComponentCache.get(pluginName);
    }

    protected Log getLog() {
        return this.log;
    }

    protected ServerPluginContext getServerPluginContext(ServerPluginEnvironment env) {

        String pluginName = env.getPluginKey().getPluginName();
        ServerPluginContext context = this.pluginContextCache.get(pluginName);

        // if we already created it, return it immediately and don't create another
        if (context != null) {
            return context;
        }

        MasterServerPluginContainer masterPC = this.parentPluginContainer.getMasterServerPluginContainer();
        MasterServerPluginContainerConfiguration masterConfig = masterPC.getConfiguration();
        File dataDir = new File(masterConfig.getDataDirectory(), pluginName);
        File tmpDir = masterConfig.getTemporaryDirectory();
        Configuration pluginConfig = null;
        List<ScheduledJobDefinition> schedules;
        try {
            ServerPlugin plugin = getPlugin(env);
            pluginConfig = plugin.getPluginConfiguration();
            Configuration scheduledJobsConfig = plugin.getScheduledJobsConfiguration();
            schedules = ServerPluginDescriptorMetadataParser.getScheduledJobs(scheduledJobsConfig);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get plugin config/schedules from the database", e);
        }

        context = new ServerPluginContext(env, dataDir, tmpDir, pluginConfig, schedules);
        this.pluginContextCache.put(pluginName, context);
        return context;
    }

    /**
     * Given a plugin environment, this will rebuild a new one with up-to-date information.
     * This means the descriptor will be reparsed.
     *
     * @param env the original environment
     * @return the new environment that has been rebuild from the original but has newer data
     * @throws Exception if the environment could not be rebuilt - probably due to an invalid descriptor
     *                   in the plugin jar or the plugin jar is now missing
     */
    protected ServerPluginEnvironment rebuildServerPluginEnvironment(ServerPluginEnvironment env) throws Exception {
        URL url = env.getPluginUrl();
        ClassLoader classLoader = env.getPluginClassLoader();
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
        ServerPluginEnvironment newEnv = new ServerPluginEnvironment(url, classLoader, descriptor);
        return newEnv;
    }

    /**
     * Given a plugin environment, return its {@link ServerPlugin} representation, which should also include
     * the plugin configuration and scheduled jobs configuration.
     *
     * @param pluginEnv
     * @return the ServerPlugin object for the given plugin
     */
    protected ServerPlugin getPlugin(ServerPluginEnvironment pluginEnv) {
        // get the plugin data from the database
        ServerPluginManagerLocal serverPluginsManager = LookupUtil.getServerPluginManager();
        ServerPlugin plugin = serverPluginsManager.getServerPlugin(pluginEnv.getPluginKey());
        plugin = serverPluginsManager.getServerPluginRelationships(plugin);
        return plugin;
    }

    /**
     * This will create a new {@link ServerPluginComponent} instance for that is used to
     * initialize and shutdown a particular server plugin. If there is no plugin component
     * configured for the given plugin, <code>null</code> is returned.
     *
     * The new object will be loaded in the plugin's specific classloader.
     *
     * @param environment the environment in which the plugin will execute
     *
     * @return a new object loaded in the proper plugin classloader that can initialize/shutdown the plugin,
     *         or <code>null</code> if there is no plugin component to be associated with the given plugin
     *
     * @throws Exception if failed to create the instance
     */
    protected ServerPluginComponent createServerPluginComponent(ServerPluginEnvironment environment) throws Exception {

        String pluginName = environment.getPluginKey().getPluginName();
        ServerPluginComponent instance = null;

        ServerPluginDescriptorType descriptor = environment.getPluginDescriptor();
        String className = ServerPluginDescriptorMetadataParser.getPluginComponentClassName(descriptor);

        if (className != null) {
            log.debug("Creating plugin component [" + className + "] for plugin [" + pluginName + "]");
            instance = (ServerPluginComponent) instantiatePluginClass(environment, className);
            log.debug("Plugin component created [" + instance.getClass() + "] for plugin [" + pluginName + "]");
        }

        return instance;
    }

    /**
     * Loads a class with the given name within the given environment's classloader.
     * The class will only be initialized if <code>initialize</code> is <code>true</code>.
     *
     * @param environment the environment that has the classloader where the class will be loaded
     * @param className the class to load
     * @param initialize whether the class must be initialized
     * @return the new class that has been loaded
     * @throws Exception if failed to load the class
     */
    protected Class<?> loadPluginClass(ServerPluginEnvironment environment, String className, boolean initialize)
        throws Exception {

        ClassLoader loader = environment.getPluginClassLoader();

        ServerPluginDescriptorType descriptor = environment.getPluginDescriptor();
        className = ServerPluginDescriptorMetadataParser.getFullyQualifiedClassName(descriptor, className);

        log.debug("Loading server plugin class [" + className + "]...");

        Class<?> clazz;
        try {
            clazz = Class.forName(className, initialize, loader);
        } catch (ClassNotFoundException e) {
            throw new Exception("Could not find plugin class [" + className + "] from plugin environment ["
                + environment + "]", e);
        } catch (NoClassDefFoundError e) {
            throw new Exception("No class definition for plugin class [" + className + "] from plugin environment ["
                + environment + "]", e);
        } catch (NullPointerException npe) {
            throw new Exception("Plugin class was 'null' in plugin environment [" + environment + "]", npe);
        } catch (Error e) {
            // wrap error (e.g. NoClassDefFoundError) so anyone catching Exception will catch this
            throw new Exception("Can not load plugin class [" + className + "] from plugin environment [" + environment
                + "]", e);
        }

        log.debug("Loaded server plugin class [" + clazz + "]. initialized=[" + initialize + ']');
        return clazz;
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
        try {
            Class<?> clazz = loadPluginClass(environment, className, true);
            log.debug("Instantiating server plugin class [" + clazz + "]");
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new Exception("Could not instantiate plugin class [" + className + "] from plugin environment ["
                + environment + "]", e);
        } catch (IllegalAccessException e) {
            throw new Exception("Could not access plugin class [" + className + "] from plugin environment ["
                + environment + "]", e);
        }
    }
}
