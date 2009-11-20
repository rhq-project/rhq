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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.generic.GenericServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.perspective.PerspectiveServerPluginContainer;
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

    private MasterServerPluginContainerConfiguration configuration;
    private Map<ServerPluginType, AbstractTypeServerPluginContainer> pluginContainers = new HashMap<ServerPluginType, AbstractTypeServerPluginContainer>();
    private ClassLoaderManager classLoaderManager;

    /**
     * Starts the master plugin container, which will load all plugins and begin managing them.
     *
     * @param config the master configuration
     */
    public synchronized void initialize(MasterServerPluginContainerConfiguration config) {
        try {
            log.debug("Master server plugin container is being initialized with config: " + config);

            this.configuration = config;

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
                } catch (Exception e) {
                    log.warn("Failed to initialize server plugin container for plugin type [" + pluginType + "]", e);
                    iterator.remove();
                }
            }

            // Create classloaders/environments for all plugins and load plugins into their plugin containers.
            // Note that we do not care what order we load plugins - in the future we may want dependencies.
            for (Map.Entry<URL, ? extends ServerPluginDescriptorType> entry : plugins.entrySet()) {
                URL pluginUrl = entry.getKey();
                ServerPluginDescriptorType descriptor = entry.getValue();
                String pluginName = descriptor.getName();
                ClassLoader classLoader = this.classLoaderManager.obtainServerPluginClassLoader(pluginName);
                AbstractTypeServerPluginContainer pc = getPluginContainerByDescriptor(descriptor);
                if (pc != null) {
                    log.debug("Loading server plugin [" + pluginUrl + "] into its plugin container");
                    try {
                        ServerPluginEnvironment env = new ServerPluginEnvironment(pluginUrl, classLoader, descriptor);
                        pc.loadPlugin(env);
                        log.info("Loaded server plugin [" + pluginUrl + "]");
                    } catch (Exception e) {
                        log.warn("Failed to load server plugin [" + pluginUrl + "]", e);
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
                } catch (Exception e) {
                    log.warn("Failed to start server plugin container for plugin type [" + pluginType + "]", e);
                }
            }

            log.info("Master server plugin container has been initialized");

        } catch (Throwable t) {
            shutdown();
            log.error("Failed to initialize master plugin container! Server side plugins will not start.", t);
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

        // now shutdown the classloader manager, destroying the classloaders it created
        if (this.classLoaderManager != null) {
            this.classLoaderManager.shutdown();
            log.debug("Shutdown classloader manager: " + this.classLoaderManager);
        }

        this.pluginContainers.clear();
        this.classLoaderManager = null;
        this.configuration = null;

        log.info("Master server plugin container has been shutdown");
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
                pc.schedulePluginJobs();
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
     * Get the plugin container of the given class. This method provides a strongly typed return value,
     * based on the type of plugin container the caller wants returned.
     * 
     * @param clazz the class name of the plugin container that the caller wants
     * @return the plugin container of the given class (<code>null</code> if none found)
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends AbstractTypeServerPluginContainer> T getPluginContainer(Class<T> clazz) {
        for (AbstractTypeServerPluginContainer pc : this.pluginContainers.values()) {
            if (clazz.isInstance(pc)) {
                return (T) pc;
            }
        }
        return null;
    }

    /**
     * Given a plugin's descriptor, this will return the plugin container that can manage the plugin.
     * 
     * @param descriptor descriptor to identify a plugin whose container is to be returned
     * @return a plugin container that can handle the plugin with the given descriptor
     */
    protected synchronized AbstractTypeServerPluginContainer getPluginContainerByPluginType(ServerPluginType pluginType) {
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
     * If a plugin fails to load, it will be ignored - other plugins will still load.
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

                List<String> disabledPlugins = getDisabledPluginNames();

                for (File pluginFile : pluginFiles) {
                    if (pluginFile.getName().endsWith(".jar")) {
                        URL pluginUrl = pluginFile.toURI().toURL();

                        try {
                            ServerPluginDescriptorType descriptor;
                            descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
                            if (descriptor != null) {
                                if (!disabledPlugins.contains(descriptor.getName())) {
                                    log.debug("pre-loaded server plugin from URL: " + pluginUrl);
                                    plugins.put(pluginUrl, descriptor);
                                } else {
                                    log.info("Server plugin [" + descriptor.getName()
                                        + "] is disabled and will not be initialized");
                                }
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
     * This will return a list of plugin names that represent all the plugins that are to be
     * disabled. If a plugin jar is found on the filesystem, its plugin name should be checked with
     * this "blacklist" if it its name is found, that plugin should not be loaded.
     * 
     * @return names of "blacklisted" plugins that should not be loaded
     */
    protected List<String> getDisabledPluginNames() {
        List<String> disabledPlugins = LookupUtil.getServerPlugins().getPluginNamesByEnabled(false);
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
        ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(4);
        pcs.add(new GenericServerPluginContainer(this));
        pcs.add(new ContentServerPluginContainer(this));
        pcs.add(new PerspectiveServerPluginContainer(this));
        pcs.add(new AlertServerPluginContainer(this));
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
}