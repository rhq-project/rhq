/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.plugin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * This container service will load in all plugins that can be found and will maintain the complete set of
 * {@link #getMetadataManager() metadata} found in all plugin descriptors from all loaded plugins. You can obtain a
 * loaded plugin's {@link PluginEnvironment environment}, including its classloader, from this object as well - see
 * {@link #getPlugin(String)}.
 *
 * @author Greg Hinkle
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public class PluginManager implements ContainerService {
    private static final Log log = LogFactory.getLog(PluginManager.class);

    /**
     * The map of all plugins keyed on plugin name.
     */
    private Map<String, PluginEnvironment> loadedPluginEnvironments;

    /**
     * A list of loaded plugin names in the order in which they were loaded. 
     */
    private List<String> loadedPlugins;

    /**
     * Cached instances of objects used to initialize and shutdown individual plugins.
     * Only plugins that declare their own lifecycle listener will have objects in this cache.
     */
    private Map<String, PluginLifecycleListener> pluginLifecycleListenerCache;

    private PluginMetadataManager metadataManager;
    private PluginContainerConfiguration configuration;

    /**
     * Finds all plugins using the plugin finder defined in the
     * {@link #setConfiguration(PluginContainerConfiguration) plugin container configuration} and
     * {@link #loadPlugin(URL, ClassLoader) loads} each plugin found.
     *
     * @see ContainerService#initialize()
     */
    public void initialize() {
        loadedPluginEnvironments = new HashMap<String, PluginEnvironment>();
        loadedPlugins = new ArrayList<String>();
        pluginLifecycleListenerCache = new HashMap<String, PluginLifecycleListener>();
        metadataManager = new PluginMetadataManager();

        PluginFinder finder = configuration.getPluginFinder();
        File tmpDir = configuration.getTemporaryDirectory();

        try {
            if (finder != null) {
                Collection<URL> pluginUrls = finder.findPlugins();

                // first, we need to parse all descriptors so we can build the dependency graph
                Map<String, URL> pluginNamesUrls = new HashMap<String, URL>();
                PluginDependencyGraph graph = new PluginDependencyGraph();

                for (URL url : pluginUrls) {
                    log.debug("Plugin found at: " + url);

                    PluginDescriptor descriptor;
                    try {
                        descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
                    } catch (Throwable t) {
                        // probably due to invalid XML syntax in the deployment descriptor - the plugin will be ignored
                        log.error("Plugin at [" + url + "] could not be loaded and will therefore not be deployed.", t);
                        continue;
                    }

                    String pluginName = descriptor.getName();

                    List<PluginDependencyGraph.PluginDependency> dependencies = new ArrayList<PluginDependencyGraph.PluginDependency>();
                    for (PluginDescriptor.Depends dependency : descriptor.getDepends()) {
                        dependencies.add(new PluginDependencyGraph.PluginDependency(dependency.getPlugin(), dependency
                            .isUseClasses()));
                    }

                    graph.addPlugin(pluginName, dependencies);
                    pluginNamesUrls.put(pluginName, url);
                }

                // get the order that we have to deploy the plugins
                List<String> deploymentOrder = graph.getDeploymentOrder();

                // now deploy the plugins in the proper order, making sure we build the proper classloaders
                for (String nextPlugin : deploymentOrder) {
                    URL pluginUrl = pluginNamesUrls.get(nextPlugin);
                    List<String> dependencies = graph.getPluginDependencies(nextPlugin);

                    try {
                        if (dependencies.size() == 0) {
                            log.debug("Loading independent plugin [" + nextPlugin + "] from URL: " + pluginUrl);
                            loadPlugin(pluginUrl, null);
                        } else {
                            // Set<URL> allUrls = new HashSet<URL>();
                            // getDependentUrls( nextPlugin, pluginNamesUrls, graph, allUrls );

                            log.debug("Loading dependent plugin [" + nextPlugin + "] from URL [" + pluginUrl
                                + "] that has the following dependencies: " + dependencies);

                            String lastDependency = graph.getUseClassesDependency(nextPlugin);
                            PluginEnvironment lastDepEnvironment = this.loadedPluginEnvironments.get(lastDependency);
                            
                            // create the URL array where all classes can be found - make sure we put the plugin's URL
                            // up at the front so its plugin descriptor is found before any other dependent plugin descriptors
                            //  URL[] allUrlsArray = new URL[allUrls.size() + 1];
                            //  System.arraycopy( allUrls.toArray( new URL[allUrls.size()] ), 0,
                            //                    allUrlsArray, 1,
                            //                    allUrls.size() );
                            //  allUrlsArray[0] = pluginUrl;

                            ClassLoader classloader = lastDepEnvironment.getPluginClassLoader();

                            // Note that we don't really care if the URL uses "file:" or not, we just use File to parse
                            // the name from the path.
                            String pluginJarName = new File(pluginUrl.getPath()).getName();

                            loadPlugin(pluginUrl, PluginClassLoader.create(pluginJarName, pluginUrl, true, classloader,
                                tmpDir));
                        }
                    } catch (Throwable t) {
                        // for some reason, the plugin failed to load - it will be ignored, its depending plugins will also fail later
                        log.error("Plugin [" + nextPlugin + "] at [" + pluginUrl
                            + "] could not be loaded and will therefore not be deployed.", t);
                        continue;
                    }
                }
            } else {
                // Loading a null plugin loads the plugin using the current classloader
                // This is useful for testing plugins in unit tests.
                log.info("Loading the null plugin which uses non-isolated classloader");
                loadPlugin(null, null);
            }
        } catch (Exception e) {
            shutdown(); // have to clean up the environments (e.g. unpacked jars) we might have already created
            log.error("Error initializing plugin container", e);
            throw new RuntimeException("Cannot initialize the plugin container", e);
        }
    }

    /**
     * @see ContainerService#shutdown()
     */
    public void shutdown() {
        // Inform the plugins we are shutting them down.
        // We want to shut them down in the reverse order that we initialized them.
        Collections.reverse(this.loadedPlugins);
        for (String pluginName : this.loadedPlugins) {
            PluginLifecycleListener listener = this.pluginLifecycleListenerCache.get(pluginName);
            if (listener != null) {
                try {
                    ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(
                        this.loadedPluginEnvironments.get(pluginName).getPluginClassLoader());
                    try {
                        listener.shutdown();
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalCL);
                    }
                } catch (Throwable t) {
                    log.warn("Failed to get lifecycle listener to shutdown [" + pluginName + "]. Cause: "
                        + ThrowableUtil.getAllMessages(t));
                }
            }
        }

        // Clean up the plugin environment and the temp dirs that were used by the plugin classloaders.
        for (PluginEnvironment pluginEnvironment : this.loadedPluginEnvironments.values()) {
            pluginEnvironment.destroy();
        }

        this.loadedPluginEnvironments.clear();
        this.loadedPluginEnvironments = null;

        this.loadedPlugins.clear();
        this.loadedPlugins = null;

        this.pluginLifecycleListenerCache.clear();
        this.pluginLifecycleListenerCache = null;

        this.metadataManager = null;
    }

    /**
     * @see ContainerService#setConfiguration(PluginContainerConfiguration)
     */
    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the {@link PluginEnvironment}s for every plugin this manager found and loaded.
     *
     * @return environments for all the plugins
     */
    public Collection<PluginEnvironment> getPlugins() {
        return this.loadedPluginEnvironments.values();
    }

    /**
     * Returns the {@link PluginEnvironment} for the specific plugin with the given name.
     *
     * <p>The plugin's name is defined in its plugin descriptor - specifically the XML root node's "name" attribute
     * (e.g. &ltplugin name="thePluginName").</p>
     *
     * @param  name plugin name as defined in the plugin's descriptor
     *
     * @return the environment of the loaded plugin with the given name (<code>null</code> if there is no loaded plugin
     *         with the given name)
     */
    @Nullable
    public PluginEnvironment getPlugin(String name) {
        return this.loadedPluginEnvironments.get(name);
    }

    /**
     * An object that can be used to process and store all metadata from all plugins. This object will contain all the
     * metadata found in all loaded plugins.
     *
     * @return object to retrieve plugin metadata from
     */
    public PluginMetadataManager getMetadataManager() {
        return metadataManager;
    }

    /**
     * This will create a {@link PluginEnvironment} for the plugin at the given URL. What this means is a classloader is
     * created for the plugin (if <code>classLoader</code> is <code>null</code>) and the plugin's descriptor is parsed.
     * Once this method returns, the plugin's components are ready to be created and used.
     *
     * @param  pluginUrl   the new plugin's jar location
     * @param  classLoader the new plugin's classloader - if <code>null</code>, one will be created for it
     *
     * @throws PluginContainerException if the plugin fails to load
     */
    private void loadPlugin(URL pluginUrl, ClassLoader classLoader) throws PluginContainerException {
        log.debug("Loading plugin from [" + pluginUrl + "]...");

        PluginDescriptorLoader pluginDescriptorLoader = new PluginDescriptorLoader(pluginUrl, classLoader, null, true,
            this.configuration.getTemporaryDirectory());
        PluginDescriptor pluginDescriptor = pluginDescriptorLoader.loadPluginDescriptor();
        PluginEnvironment pluginEnvironment = new PluginEnvironment(pluginDescriptor.getName(), pluginDescriptorLoader);
        String pluginName = pluginEnvironment.getPluginName();

        // tell the plugin we have loaded it
        PluginLifecycleListener overseer = getPluginLifecycleListener(pluginName, pluginEnvironment, pluginDescriptor);
        if (overseer != null) {
            PluginContext context = createPluginContext(pluginName);
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(pluginEnvironment.getPluginClassLoader());
                overseer.initialize(context);
            } catch (Throwable t) {
                throw new PluginContainerException("Plugin Lifecycle Listener failed to initialize plugin", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
            this.pluginLifecycleListenerCache.put(pluginName, overseer);
        }

        // everything is loaded and initialized
        this.loadedPluginEnvironments.put(pluginName, pluginEnvironment);
        this.metadataManager.loadPlugin(pluginDescriptor);
        this.loadedPlugins.add(pluginName);

        return;
    }

    /**
     * This will create a new {@link PluginLifecycleListener} instance for that is used to
     * initialize and shutdown a particular plugin. If there is no plugin lifecycle listener
     * configured for the given plugin, <code>null</code> is returned.
     *
     * The new object will be loaded in the plugin's specific classloader.
     *
     * @param pluginName the name of the plugin whose {@link PluginLifecycleListener} is to be retrieved
     * @param pluginEnvironment the environment in which the plugin will execute
     * @param pluginDescriptor the plugin's descriptor
     *
     * @return a new object loaded in the proper plugin classloader that can initialize/shutdown the plugin,
     *         or <code>null</code> if there is no plugin lifecycle listener to be associated with the given plugin
     *
     * @throws PluginContainerException if failed to create the instance
     */
    private PluginLifecycleListener getPluginLifecycleListener(String pluginName, PluginEnvironment pluginEnvironment,
        PluginDescriptor pluginDescriptor) throws PluginContainerException {

        PluginLifecycleListener instance = pluginLifecycleListenerCache.get(pluginName);

        if (instance == null) {
            String className = getPluginLifecycleListenerClass(pluginDescriptor);

            if (className != null) {
                log.debug("Creating plugin lifecycle listener [" + className + "] for plugin [" + pluginName + "]");
                instance = (PluginLifecycleListener) instantiateClass(pluginEnvironment, className);
                log.debug("Created plugin lifecycle listener [" + className + "] for plugin [" + pluginName + "]");
            }
        }

        return instance;
    }

    private String getPluginLifecycleListenerClass(PluginDescriptor pluginDescriptor) {
        String className = pluginDescriptor.getPluginLifecycleListener();
        if (className != null) {
            String pkg = pluginDescriptor.getPackage();
            if ((className.indexOf('.') == -1) && (pkg != null)) {
                className = pkg + '.' + className;
            }
        }
        return className;
    }

    private PluginContext createPluginContext(String pluginName) {
        SystemInfo sysInfo = SystemInfoFactory.createSystemInfo();
        File dataDir = new File(this.configuration.getDataDirectory(), pluginName);
        File tmpDir = this.configuration.getTemporaryDirectory();
        String pcName = this.configuration.getContainerName();

        //noinspection UnnecessaryLocalVariable
        PluginContext context = new PluginContext(pluginName, sysInfo, tmpDir, dataDir, pcName);

        return context;
    }

    private Object instantiateClass(PluginEnvironment environment, String className) throws PluginContainerException {
        ClassLoader loader = environment.getPluginClassLoader();

        log.debug("Loading class [" + className + "]...");

        try {
            Class<?> clazz = Class.forName(className, true, loader);
            log.debug("Loaded class [" + clazz + "].");
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new PluginContainerException("Could not instantiate plugin class [" + className
                + "] from plugin environment [" + environment + "]", e);
        } catch (IllegalAccessException e) {
            throw new PluginContainerException("Could not access plugin class " + className
                + "] from plugin environment [" + environment + "]", e);
        } catch (ClassNotFoundException e) {
            throw new PluginContainerException("Could not find plugin class " + className
                + "] from plugin environment [" + environment + "]", e);
        } catch (NullPointerException npe) {
            throw new PluginContainerException("Plugin class was 'null' in plugin environment [" + environment + "]",
                npe);
        }
    }

    /**
     * Given a plugin name, this will get all of its dependencies' plugin URLs and add them to <code>allUrls</code>.
     * This recursively travels N levels deep in the dependency graph.
     *
     * @param pluginName      the name of the plugin to obtain dependency URLs for
     * @param pluginNamesUrls map of all known plugin names and their plugin jar URLs
     * @param graph           the dependency graph
     * @param allUrls         where the results will be stored
     *
     * TODO: Use it or lose it.
     */
    private void getDependentUrls(String pluginName, Map<String, URL> pluginNamesUrls, PluginDependencyGraph graph,
        Set<URL> allUrls) {
        List<String> deps = graph.getPluginDependencies(pluginName);

        for (String dep : deps) {
            getDependentUrls(dep, pluginNamesUrls, graph, allUrls);
            allUrls.add(pluginNamesUrls.get(dep));
        }

        return;
    }
}