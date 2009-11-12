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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.LifecycleListenerType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ScheduleType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Provides functionality to manage plugins for a plugin container. Plugin containers
 * can install their own plugin managers that are extensions to this class if they need to.
 * 
 * @author John Mazzitelli
 */
public class ServerPluginManager {
    private final Log log = LogFactory.getLog(this.getClass());

    private final AbstractTypeServerPluginContainer parentPluginContainer;

    /**
     * The map of all plugin environments keyed on plugin name.
     */
    private final Map<String, ServerPluginEnvironment> loadedPlugins;

    /**
     * Cached instances of objects used to initialize and shutdown individual plugins.
     * Only plugins that declare their own lifecycle listener will have objects in this cache.
     * This is keyed on plugin name.
     */
    private final Map<String, ServerPluginLifecycleListener> pluginLifecycleListenerCache;

    /**
     * Creates a plugin manager for the given plugin container.
     * 
     * @param pc the plugin manager's owning plugin container
     */
    public ServerPluginManager(AbstractTypeServerPluginContainer pc) {
        this.parentPluginContainer = pc;
        this.loadedPlugins = new HashMap<String, ServerPluginEnvironment>();
        this.pluginLifecycleListenerCache = new HashMap<String, ServerPluginLifecycleListener>();
    }

    /**
     * Initializes the plugin manager to prepare it to start loading plugins.
     * 
     * @throws Exception if failed to initialize
     */
    public synchronized void initialize() throws Exception {
        log.debug("Plugin manager initializing");
        return; // no-op
    }

    /**
     * Shuts down this manager. This should be called only after all of its plugins
     * have been {@link #unloadPlugin(ServerPluginEnvironment) unloaded}.
     */
    public synchronized void shutdown() {
        log.debug("Plugin manager shutting down");

        if (this.loadedPlugins.size() > 0) {
            log.warn("Server plugin manager is being shutdown while some plugins are still loaded: "
                + this.loadedPlugins);
            this.loadedPlugins.clear();
        }

        this.pluginLifecycleListenerCache.clear();

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
    public synchronized void loadPlugin(ServerPluginEnvironment env) throws Exception {
        String pluginName = env.getPluginName();
        log.debug("Loading server plugin [" + pluginName + "] from: " + env.getPluginUrl());

        // tell the plugin we are loading it
        ServerPluginLifecycleListener listener = createServerPluginLifecycleListener(env);
        if (listener != null) {
            ServerPluginContext context = createServerPluginContext(env);
            log.debug("Initializing lifecycle listener for server plugin [" + pluginName + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                listener.initialize(context);
            } catch (Throwable t) {
                throw new Exception("Lifecycle listener failed to initialize plugin", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
            this.pluginLifecycleListenerCache.put(pluginName, listener);
        }

        // note that we only cache it if the lifecycle listener was successful
        this.loadedPlugins.put(pluginName, env);

        return;
    }

    public synchronized void startPlugins() {
        log.debug("Starting plugins");

        // tell all lifecycle listeners to start
        for (Entry<String, ServerPluginLifecycleListener> entry : this.pluginLifecycleListenerCache.entrySet()) {
            String pluginName = entry.getKey();
            ServerPluginLifecycleListener listener = entry.getValue();
            ServerPluginEnvironment env = this.loadedPlugins.get(pluginName);
            log.debug("Starting lifecycle listener for server plugin [" + pluginName + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                listener.start();
            } catch (Throwable t) {
                log.warn("Lifecycle listener failed to start plugin [" + pluginName + "]", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        }

        return;
    }

    public synchronized void stopPlugins() {
        log.debug("Stopping plugins");

        // tell all lifecycle listeners to stop
        for (Entry<String, ServerPluginLifecycleListener> entry : this.pluginLifecycleListenerCache.entrySet()) {
            String pluginName = entry.getKey();
            ServerPluginLifecycleListener listener = entry.getValue();
            ServerPluginEnvironment env = this.loadedPlugins.get(pluginName);
            log.debug("Stopping lifecycle listener for server plugin [" + pluginName + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                listener.stop();
            } catch (Throwable t) {
                log.warn("Lifecycle listener failed to stop plugin [" + pluginName + "]", t);
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
    public synchronized void unloadPlugin(ServerPluginEnvironment env) throws Exception {
        String pluginName = env.getPluginName();
        log.debug("Unloading server plugin [" + pluginName + "]");

        try {
            // tell the plugin we are unloading it
            ServerPluginLifecycleListener listener = this.pluginLifecycleListenerCache.get(pluginName);
            if (listener != null) {
                log.debug("Shutting down lifecycle listener for server plugin [" + pluginName + "]");
                ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                    listener.shutdown();
                } catch (Throwable t) {
                    throw new Exception("Plugin Lifecycle Listener failed to initialize plugin", t);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                    this.pluginLifecycleListenerCache.remove(pluginName);
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
    public synchronized Collection<ServerPluginEnvironment> getPluginEnvironments() {
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
    public synchronized ServerPluginEnvironment getPluginEnvironment(String pluginName) {
        return this.loadedPlugins.get(pluginName);
    }

    /**
     * Returns the lifecycle listener instance that is responsible for initializing and managing
     * the plugin. This will return <code>null</code> if a plugin has not defined a lifecycle listener.
     * 
     * @param pluginName the name of the plugin whose lifecycle listener is to be returned
     * 
     * @return the lifecycle listener instance that initialized and is managing a plugin. Will
     *         return <code>null</code> if the plugin has not defined a lifecycle listener. 
     *         <code>null</code> is also returned if the plugin is not initialized yet. 
     */
    public synchronized ServerPluginLifecycleListener getServerPluginLifecycleListener(String pluginName) {
        return this.pluginLifecycleListenerCache.get(pluginName);
    }

    public AbstractTypeServerPluginContainer getParentPluginContainer() {
        return this.parentPluginContainer;
    }

    protected Log getLog() {
        return this.log;
    }

    protected ServerPluginContext createServerPluginContext(ServerPluginEnvironment env) {
        String pluginName = env.getPluginName();
        ServerPluginDescriptorType pluginDescriptor = env.getPluginDescriptor();

        MasterServerPluginContainer masterPC = this.parentPluginContainer.getMasterServerPluginContainer();
        MasterServerPluginContainerConfiguration masterConfig = masterPC.getConfiguration();
        File dataDir = new File(masterConfig.getDataDirectory(), pluginName);
        File tmpDir = masterConfig.getTemporaryDirectory();

        // TODO: today we have no way in the UI to customize the plugin config values
        //       for now, just use the defaults as defined in the descriptor
        Configuration config = null;
        try {
            ConfigurationDefinition configDef = ConfigurationMetadataParser.parse(pluginName, pluginDescriptor
                .getPluginConfiguration());
            config = configDef.getDefaultTemplate().createConfiguration();
        } catch (InvalidPluginDescriptorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Schedule schedule = null;
        if (pluginDescriptor.getLifecycleListener() != null) {
            ScheduleType scheduleType = pluginDescriptor.getLifecycleListener().getSchedule();
            if (scheduleType != null) {
                boolean concurrent = scheduleType.isConcurrent();
                if (scheduleType.getPeriod() != null) {
                    schedule = new PeriodicSchedule(concurrent, scheduleType.getPeriod().longValue());
                } else {
                    schedule = new CronSchedule(concurrent, scheduleType.getCron());
                }
            }
        }

        ServerPluginContext context = new ServerPluginContext(env, dataDir, tmpDir, config, schedule);
        return context;
    }

    /**
     * This will create a new {@link ServerPluginLifecycleListener} instance for that is used to
     * initialize and shutdown a particular server plugin. If there is no plugin lifecycle listener
     * configured for the given plugin, <code>null</code> is returned.
     *
     * The new object will be loaded in the plugin's specific classloader.
     *
     * @param pluginName the name of the plugin whose listener is to be retrieved
     * @param environment the environment in which the plugin will execute
     *
     * @return a new object loaded in the proper plugin classloader that can initialize/shutdown the plugin,
     *         or <code>null</code> if there is no plugin lifecycle listener to be associated with the given plugin
     *
     * @throws Exception if failed to create the instance
     */
    protected ServerPluginLifecycleListener createServerPluginLifecycleListener(ServerPluginEnvironment environment)
        throws Exception {

        String pluginName = environment.getPluginName();
        ServerPluginLifecycleListener instance = null;

        LifecycleListenerType lifecycleListener = environment.getPluginDescriptor().getLifecycleListener();
        if (lifecycleListener != null) {
            String className = lifecycleListener.getClazz();
            String pkg = environment.getPluginDescriptor().getPackage();
            if ((className.indexOf('.') == -1) && (pkg != null)) {
                className = pkg + '.' + className;
            }

            log.debug("Creating plugin lifecycle listener [" + className + "] for plugin [" + pluginName + "]");
            instance = (ServerPluginLifecycleListener) instantiatePluginClass(environment, className);
            log.debug("Created plugin lifecycle listener [" + className + "] for plugin [" + pluginName + "]");
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
