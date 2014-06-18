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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.PackageTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.generic.GenericServerPluginContainer;
import org.rhq.enterprise.server.scheduler.EnhancedScheduler;
import org.rhq.enterprise.server.scheduler.EnhancedSchedulerImpl;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * The container responsible for managing all the plugin containers for all the
 * different plugin types.
 *
 * @author John Mazzitelli
 */
public class MasterServerPluginContainer {
    private static final Log log = LogFactory.getLog(MasterServerPluginContainer.class);

    /** The configuration for the master plugin container itself. */
    private MasterServerPluginContainerConfiguration configuration;

    /** the plugin containers for all the different types of plugins that are supported */
    private Map<ServerPluginType, AbstractTypeServerPluginContainer> pluginContainers = new HashMap<ServerPluginType, AbstractTypeServerPluginContainer>();

    /** the object that provides all the classloaders for all plugins */
    private ClassLoaderManager classLoaderManager;

    /** this is used to obtain an in-memory, non-persistent scheduler used to schedule jobs to run only within this JVM */
    private SchedulerFactory nonClusteredSchedulerFactory;

    /**
     * Because the individual plugin containers are only managing enabled plugins (they are never told about plugins that disabled).
     * this map contains the lists of plugins that are disabled so others can find out what plugins are registered but not running.
     */
    private Map<ServerPluginType, List<PluginKey>> disabledPlugins = new HashMap<ServerPluginType, List<PluginKey>>();

    /**
     * Starts the master plugin container, which will load all plugins and begin managing them.
     *
     * @param config the master configuration
     */
    public synchronized void initialize(MasterServerPluginContainerConfiguration config) {
        try {
            List<Throwable> caughtExceptions = new ArrayList<Throwable>();
            log.debug("Master server plugin container is being initialized with config: " + config);

            this.configuration = config;

            initializeNonClusteredScheduler();

            // load all server-side plugins - this just parses their descriptors and confirms they are valid server plugins
            Map<URL, ? extends ServerPluginDescriptorType> plugins = preloadAllPlugins();

            // create the root classloader to be used as the top classloader for all plugins
            ClassLoader rootClassLoader = createRootServerPluginClassLoader();
            File tmpDir = this.configuration.getTemporaryDirectory();
            this.classLoaderManager = createClassLoaderManager(plugins, rootClassLoader, tmpDir);
            log.debug("Created classloader manager: " + this.classLoaderManager);

            // create all known child plugin containers and map them to their supported plugin types
            List<AbstractTypeServerPluginContainer> pcs = createPluginContainers();
            for (AbstractTypeServerPluginContainer pc : pcs) {
                this.pluginContainers.put(pc.getSupportedServerPluginType(), pc);
            }
            log.debug("Created server plugin containers: " + this.pluginContainers.keySet());

            // initialize all the plugin containers
            Iterator<Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer>> iterator;
            iterator = this.pluginContainers.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry = iterator.next();

                ServerPluginType pluginType = entry.getKey();
                AbstractTypeServerPluginContainer pc = entry.getValue();

                log.debug("Master PC is initializing server plugin container for plugin type [" + pluginType + "]");
                try {
                    pc.initialize();
                    log.debug("Master PC initialized server plugin container for plugin type [" + pluginType + "]");
                } catch (Throwable e) {
                    log.warn("Failed to initialize server plugin container for plugin type [" + pluginType + "]", e);
                    caughtExceptions.add(e);
                    iterator.remove();
                }
            }

            // Create classloaders/environments for all plugins and load plugins into their plugin containers.
            // Note that we do not care what order we load plugins - in the future we may want dependencies.
            List<PluginKey> allDisabledPlugins = getDisabledPluginKeys();

            for (Map.Entry<URL, ? extends ServerPluginDescriptorType> entry : plugins.entrySet()) {
                URL pluginUrl = entry.getKey();
                ServerPluginDescriptorType descriptor = entry.getValue();
                AbstractTypeServerPluginContainer pc = getPluginContainerByDescriptor(descriptor);
                if (pc != null) {
                    String pluginName = descriptor.getName();
                    ServerPluginType pluginType = new ServerPluginType(descriptor);
                    PluginKey pluginKey = PluginKey.createServerPluginKey(pluginType.stringify(), pluginName);
                    try {
                        ClassLoader classLoader = this.classLoaderManager.obtainServerPluginClassLoader(pluginKey);
                        log.debug("Pre-loading server plugin [" + pluginKey + "] from [" + pluginUrl
                            + "] into its plugin container");
                        try {
                            ServerPluginEnvironment env = new ServerPluginEnvironment(pluginUrl, classLoader,
                                descriptor);
                            boolean enabled = !allDisabledPlugins.contains(pluginKey);
                            pc.loadPlugin(env, enabled);
                            log.info("Preloaded server plugin [" + pluginName + "]");
                        } catch (Throwable e) {
                            log.warn("Failed to preload server plugin [" + pluginName + "] from URL [" + pluginUrl
                                + "]", e);
                            caughtExceptions.add(e);
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to preload server plugin [" + pluginName
                            + "]; cannot get its classloader from URL [ " + pluginUrl + "]", e);
                        caughtExceptions.add(e);
                    }
                } else {
                    log.warn("There is no server plugin container to support plugin: " + pluginUrl);
                }
            }

            // now that all plugins have been loaded, we need to tell all the plugin containers to start
            iterator = this.pluginContainers.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry = iterator.next();

                ServerPluginType pluginType = entry.getKey();
                AbstractTypeServerPluginContainer pc = entry.getValue();

                log.debug("Master PC is starting server plugin container for plugin type [" + pluginType + "]");
                try {
                    pc.start();
                    log.info("Master PC started server plugin container for plugin type [" + pluginType + "]");
                } catch (Throwable e) {
                    log.warn("Failed to start server plugin container for plugin type [" + pluginType + "]", e);
                    caughtExceptions.add(e);
                }
            }

            if (caughtExceptions.isEmpty()) {
                log.info("Master server plugin container has been initialized");
            } else {
                log.warn("Master server plugin container has been initialized but it detected some problems. "
                    + "Parts of the server may not operate correctly due to these errors.");
                int i = 1;
                for (Throwable t : caughtExceptions) {
                    log.warn("Problem #" + (i++) + ": " + ThrowableUtil.getAllMessages(t));
                }
            }

        } catch (Throwable t) {
            shutdown();
            log.error("Failed to initialize master plugin container! Server side plugins will not start.", t);
            throw new RuntimeException(t);
        }

        return;
    }

    /**
     * Stops all plugins and cleans up after them.
     */
    public synchronized void shutdown() {
        log.debug("Master server plugin container is being shutdown");

        // stop all the plugin containers, giving them a chance to do things like stop threads they have running
        for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : this.pluginContainers.entrySet()) {
            ServerPluginType pluginType = entry.getKey();
            AbstractTypeServerPluginContainer pc = entry.getValue();

            log.debug("Master PC is stopping server plugin container for plugin type [" + pluginType + "]");
            try {
                pc.stop();
                log.debug("Master PC stopped server plugin container for plugin type [" + pluginType + "]");
            } catch (Exception e) {
                log.error("Failed to stop server plugin container for plugin type [" + pluginType + "]", e);
            }
        }

        // shutdown all the plugin containers which in turn shuts down all their plugins.
        for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : this.pluginContainers.entrySet()) {
            ServerPluginType pluginType = entry.getKey();
            AbstractTypeServerPluginContainer pc = entry.getValue();

            log.debug("Master PC is shutting down server plugin container for plugin type [" + pluginType + "]");
            try {
                pc.shutdown();
                log.info("Master PC shutdown server plugin container for plugin type [" + pluginType + "]");
            } catch (Exception e) {
                log.error("Failed to shutdown server plugin container for plugin type [" + pluginType + "]", e);
            }
        }

        shutdownNonClusteredScheduler();

        // now shutdown the classloader manager, destroying the classloaders it created
        if (this.classLoaderManager != null) {
            this.classLoaderManager.shutdown();
            log.debug("Shutdown classloader manager: " + this.classLoaderManager);
        }

        this.pluginContainers.clear();
        this.disabledPlugins.clear();
        this.classLoaderManager = null;
        this.configuration = null;

        log.info("Master server plugin container has been shutdown");
    }

    /**
     * Loads a plugin into the appropriate plugin container.
     * 
     * @param pluginUrl the location where the new plugin is found
     * @param enabled indicates if the plugin should be enabled as soon as its loaded
     * @throws Exception if the plugin's descriptor could not be parsed or could not be loaded into the plugin container 
     */
    public synchronized void loadPlugin(URL pluginUrl, boolean enabled) throws Exception {
        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
        ServerPluginType pluginType = new ServerPluginType(descriptor);
        PluginKey pluginKey = PluginKey.createServerPluginKey(pluginType.stringify(), descriptor.getName());
        this.classLoaderManager.loadPlugin(pluginUrl, descriptor);
        ClassLoader classLoader = this.classLoaderManager.obtainServerPluginClassLoader(pluginKey);
        log.debug("Loading server plugin [" + pluginKey + "] from [" + pluginUrl + "] into its plugin container");
        try {
            ServerPluginEnvironment env = new ServerPluginEnvironment(pluginUrl, classLoader, descriptor);
            AbstractTypeServerPluginContainer pc = getPluginContainerByDescriptor(descriptor);
            if (pc != null) {
                pc.loadPlugin(env, enabled);
                log.info("Loaded server plugin [" + pluginKey.getPluginName() + "]");
            } else {
                throw new Exception("No plugin container can load server plugin [" + pluginKey + "]");
            }
        } catch (Exception e) {
            log.warn("Failed to load server plugin file [" + pluginUrl + "]", e);
        }

        return;
    }

    /**
     * Asks that all plugin containers schedule jobs now, if needed.
     * Note that this is separate from the {@link #initialize(MasterServerPluginContainerConfiguration)}
     * method because it is possible that the master plugin container has been
     * initialized before the scheduler is started. In this case, the caller must wait for the scheduler to
     * be started before this method is called to schedule jobs.
     */
    public synchronized void scheduleAllPluginJobs() {
        log.debug("Master server plugin container will schedule all jobs now");

        for (AbstractTypeServerPluginContainer pc : this.pluginContainers.values()) {
            try {
                pc.scheduleAllPluginJobs();
            } catch (Exception e) {
                log.error("Server plugin container for plugin type [" + pc.getSupportedServerPluginType()
                    + "] failed to scheduled some or all of its jobs", e);
            }
        }

        log.info("Master server plugin container scheduled all jobs");
        return;
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
     * Returns the manager that is responsible for created classloaders for plugins.
     *
     * @return classloader manager
     */
    public ClassLoaderManager getClassLoaderManager() {
        return this.classLoaderManager;
    }

    /**
     * This will return all known server plugins types. These are the types of plugins
     * that are supported by a server plugin container. You can obtain the server
     * plugin container that manages a particular server plugin type via
     * {@link #getPluginContainerByPluginType(ServerPluginType)}.
     *
     * @return all known server plugin types
     */
    public synchronized List<ServerPluginType> getServerPluginTypes() {
        return new ArrayList<ServerPluginType>(this.pluginContainers.keySet());
    }

    /**
     * Get the plugin container of the given class. This method provides a strongly typed return value,
     * based on the type of plugin container the caller wants returned.
     *
     * @param clazz the class name of the plugin container that the caller wants
     * @return the plugin container of the given class (<code>null</code> if none found)
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends AbstractTypeServerPluginContainer> T getPluginContainerByClass(Class<T> clazz) {
        for (AbstractTypeServerPluginContainer pc : this.pluginContainers.values()) {
            if (clazz.isInstance(pc)) {
                return (T) pc;
            }
        }
        return null;
    }

    /**
     * Given the key of a deployed plugin, this returns the plugin container that is hosting
     * that plugin. If there is no plugin with the given key or that plugin is not
     * loaded in any plugin container (e.g. when it is disabled), then <code>null</code> is returned.
     *
     * @param pluginKey
     * @return the plugin container that is managing the named plugin or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends AbstractTypeServerPluginContainer> T getPluginContainerByPlugin(PluginKey pluginKey) {
        for (AbstractTypeServerPluginContainer pc : this.pluginContainers.values()) {
            try {
                if (pc.getSupportedServerPluginType().equals(new ServerPluginType(pluginKey.getPluginType()))) {
                    if (null != pc.getPluginManager().getPluginEnvironment(pluginKey.getPluginName())) {
                        return (T) pc;
                    }
                }
            } catch (Exception skip) {
                // should never really happen
                log.error("Bad plugin key: " + pluginKey);
            }
        }
        return null;
    }

    /**
     * Given a server plugin type, this will return the plugin container that can manage that type of plugin.
     * If the server plugin type is unknown to the master, or if the master plugin is not started, this will
     * return <code>null</code>.
     *
     * @param pluginType the type of server plugin whose PC is to be returned
     * @return a plugin container that can handle the given type of server plugin
     */
    public synchronized AbstractTypeServerPluginContainer getPluginContainerByPluginType(ServerPluginType pluginType) {
        AbstractTypeServerPluginContainer pc = this.pluginContainers.get(pluginType);
        return pc;
    }

    /**
     * Given a plugin's descriptor, this will return the plugin container that can manage the plugin.
     *
     * @param descriptor descriptor to identify a plugin whose container is to be returned
     * @return a plugin container that can handle the plugin with the given descriptor
     */
    protected synchronized AbstractTypeServerPluginContainer getPluginContainerByDescriptor(
        ServerPluginDescriptorType descriptor) {

        ServerPluginType pluginType = new ServerPluginType(descriptor.getClass());
        AbstractTypeServerPluginContainer pc = getPluginContainerByPluginType(pluginType);
        return pc;
    }

    /**
     * Finds all plugins and parses their descriptors. This is only called during
     * this master plugin container's {@link #initialize(MasterServerPluginContainerConfiguration) initialization}.
     *
     * If a plugin fails to preload, it will be ignored - other plugins will still preload.
     *
     * @return a map of plugins, keyed on the plugin jar URL whose values are the parsed descriptors
     *
     * @throws Exception on catastrophic failure. Note that if a plugin failed to load,
     *                   that plugin will simply be ignored and no exception will be thrown
     */
    protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
        Map<URL, ServerPluginDescriptorType> plugins;

        plugins = new HashMap<URL, ServerPluginDescriptorType>();

        File pluginDirectory = this.configuration.getPluginDirectory();

        if (pluginDirectory != null) {
            File[] pluginFiles = pluginDirectory.listFiles();

            if (pluginFiles != null) {
                for (File pluginFile : pluginFiles) {
                    if (pluginFile.getName().endsWith(".jar")) {
                        URL pluginUrl = pluginFile.toURI().toURL();
                        try {
                            ServerPluginDescriptorType descriptor;
                            descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
                            if (descriptor != null) {
                                log.debug("pre-loaded server plugin from URL: " + pluginUrl);
                                plugins.put(pluginUrl, descriptor);
                            }
                        } catch (Throwable t) {
                            // for some reason, the plugin failed to load - it will be ignored
                            log.error("Plugin at [" + pluginUrl + "] could not be pre-loaded. Ignoring it.", t);
                        }
                    }
                }
            }
        }

        return plugins;
    }

    /**
     * This will return a list of plugin keys that represent all the plugins that are to be
     * disabled. If a plugin jar is found on the filesystem, its plugin key should be checked with
     * this "blacklist" - if its key is found, that plugin should be disabled.
     *
     * @return names of "blacklisted" plugins that should not be started (i.e. loaded as a disabled plugin)
     */
    protected List<PluginKey> getDisabledPluginKeys() {
        List<PluginKey> disabledPlugins = LookupUtil.getServerPluginManager().getServerPluginKeysByEnabled(false);
        return disabledPlugins;
    }

    /**
     * Creates the individual plugin containers that can be used to deploy different plugin types.
     *
     * <p>This is protected to allow subclasses to override the PCs that are created by this service (mainly to support tests).</p>
     *
     * @return the new plugin containers created by this method
     */
    protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
        ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(5);
        pcs.add(new GenericServerPluginContainer(this));
        pcs.add(new ContentServerPluginContainer(this));
        pcs.add(new AlertServerPluginContainer(this));
        pcs.add(new BundleServerPluginContainer(this));
        pcs.add(new PackageTypeServerPluginContainer(this));
        pcs.add(new DriftServerPluginContainer(this));
        return pcs;
    }

    /**
     * Create the root classloader that will be the ancester to all plugin classloaders.
     *
     * @return the root server plugin classloader
     */
    protected ClassLoader createRootServerPluginClassLoader() {
        ClassLoader thisClassLoader = this.getClass().getClassLoader();
        String classesToHideRegexStr = this.configuration.getRootServerPluginClassLoaderRegex();
        RootServerPluginClassLoader root = new RootServerPluginClassLoader(null, thisClassLoader, classesToHideRegexStr);
        return root;
    }

    /**
     * Creates the manager that will be responsible for instantiating plugin classloaders.
     * @param plugins maps plugin URLs with their parsed descriptors
     * @param rootClassLoader the classloader at the top of the classloader hierarchy
     * @param tmpDir where the classloaders can write out the jars that are embedded in the plugin jars
     *
     * @return the classloader manager instance
     */
    protected ClassLoaderManager createClassLoaderManager(Map<URL, ? extends ServerPluginDescriptorType> plugins,
        ClassLoader rootClassLoader, File tmpDir) {
        return new ClassLoaderManager(plugins, rootClassLoader, tmpDir);
    }

    /**
     * Some schedule jobs may want to run on all machines in the RHQ cluster. We can't use our
     * normal persistent, clustered scheduler for these jobs. Instead, each master plugin container
     * running in each RHQ server will have their own internal scheduler that can be used to run
     * jobs for this purpose.
     * 
     * @throws Exception if failed to initialize the internal scheduler
     */
    protected void initializeNonClusteredScheduler() throws Exception {
        Properties schedulerConfig = new Properties();
        schedulerConfig.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, RAMJobStore.class.getName());
        schedulerConfig.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "RHQServerPluginsJobs");
        schedulerConfig.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
        schedulerConfig.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX + ".threadCount", "5");
        schedulerConfig.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX + ".threadNamePrefix",
            "RHQServerPluginsJob");

        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(schedulerConfig);

        Scheduler scheduler = factory.getScheduler();
        scheduler.start();

        this.nonClusteredSchedulerFactory = factory;
        return;
    }

    /**
     * This will stop the internal, non-clustered scheduler running in the master plugin container.
     * This tells all jobs to shut down. 
     */
    protected void shutdownNonClusteredScheduler() {
        if (this.nonClusteredSchedulerFactory != null) {
            try {
                Scheduler scheduler = this.nonClusteredSchedulerFactory.getScheduler();
                if (scheduler != null) {
                    scheduler.shutdown(false);
                }
            } catch (Exception e) {
                log.warn("Failed to shutdown master plugin container nonclustered scheduler", e);
            } finally {
                this.nonClusteredSchedulerFactory = null;
            }
        }
        return;
    }

    /**
     * Some schedule jobs may want to run on all machines in the RHQ cluster. We can't use our
     * normal persistent, clustered scheduler for these jobs. Instead, each master plugin container
     * running in each RHQ server will have their own internal scheduler that can be used to run
     * jobs for this purpose.
     * 
     * @throws Exception if failed to obtain the internal scheduler
     * 
     * @see #getClusteredScheduler()
     */
    protected EnhancedScheduler getNonClusteredScheduler() throws Exception {
        if (this.nonClusteredSchedulerFactory == null) {
            throw new NullPointerException("The non-clustered scheduler has not be initialized");
        }
        return new EnhancedSchedulerImpl(this.nonClusteredSchedulerFactory.getScheduler());
    }

    /**
     * Most jobs need to be scheduled using the clustered scheduler so they can be run on different
     * machines in the RHQ cluster in order to balance the load across the cluster. Use this
     * scheduler when scheduling those jobs.
     * 
     * @return the clustered scheduler
     * 
     * @see #getNonClusteredScheduler()
     */
    protected EnhancedScheduler getClusteredScheduler() {
        return LookupUtil.getSchedulerBean();
    }
}
