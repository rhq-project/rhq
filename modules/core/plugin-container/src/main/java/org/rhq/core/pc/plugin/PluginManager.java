/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
import org.rhq.core.clientapi.descriptor.PluginTransformer;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.plugin.Plugin;
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

    /**
     * A callback interface for updating the loadedPlugins list. When running inside of an agent, loadedPlugins gets
     * updated with Plugin objects received from the RHQ server. When running in embedded mode, we have to create the
     * Plugin objects ourselves.
     */
    private static interface UpdateLoadedPlugins {
        void execute(PluginDescriptor pluginDescriptor, URL pluginURL);
    }

    private static final Log log = LogFactory.getLog(PluginManager.class);

    /**
     * The map of all plugins keyed on plugin name.
     */
    private Map<String, PluginEnvironment> loadedPluginEnvironments;

    /**
     * A list of loaded plugins in the order in which they were loaded.
     */
    private List<Plugin> loadedPlugins;

    private PluginMetadataManager metadataManager;
    private ClassLoaderManager classLoaderManager;
    private PluginContainerConfiguration configuration;
    private UpdateLoadedPlugins updateLoadedPlugins;

    private PluginLifecycleListenerManager pluginLifecycleListenerMgr = new PluginLifecycleListenerManagerImpl();

    /**
     * Finds all plugins using the plugin finder defined in the
     * {@link #setConfiguration(PluginContainerConfiguration) plugin container configuration} and
     * {@link #loadPlugin(java.net.URL, ClassLoader, org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor) loads} each plugin found.
     *
     * @see ContainerService#initialize()
     */
    public void initialize() {
        loadedPluginEnvironments = new HashMap<String, PluginEnvironment>();
        loadedPlugins = new ArrayList<Plugin>();
        metadataManager = new PluginMetadataManager();

        initUpdateLoadedPluginsCallback();

        PluginFinder finder = configuration.getPluginFinder();
        File tmpDir = configuration.getTemporaryDirectory();
        List<String> disabledPlugins = configuration.getDisabledPlugins();

        // The root classloader for all plugins will have all classes hidden except for those configured in the regex.
        // Notice this root classloader has no jar URLs - it will provide no classes except for what it will allow
        // the parent to expose as dictated by the regex.
        ClassLoader thisClassLoader = this.getClass().getClassLoader();
        String rootPluginClassLoaderRegex = configuration.getRootPluginClassLoaderRegex();
        ClassLoader rootCL = new RootPluginClassLoader(new URL[] {}, thisClassLoader, rootPluginClassLoaderRegex);

        // build our empty class loader manager - we use it to create and manage our plugin's classloaders
        Map<String, URL> pluginNamesUrls = new HashMap<String, URL>();
        // Save descriptors for later use, so we don't need to parse them twice.
        Map<URL, PluginDescriptor> descriptors = new HashMap<URL, PluginDescriptor>();
        PluginDependencyGraph graph = new PluginDependencyGraph();
        boolean createResourceCL = configuration.isCreateResourceClassloaders();
        this.classLoaderManager = new ClassLoaderManager(pluginNamesUrls, graph, rootCL, tmpDir, createResourceCL);

        if (finder == null) {
            log.warn("No plugin finder was specified in the plugin container configuration - this should only occur within test environments.");
            return;
        }

        try {
            Collection<URL> pluginUrls = finder.findPlugins();

            // first, we need to parse all descriptors so we can build the dependency graph
            for (URL url : pluginUrls) {
                log.debug("Plugin found at: " + url);
                try {
                    PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(url);
                    if (!disabledPlugins.contains(descriptor.getName())) {
                        AgentPluginDescriptorUtil.addPluginToDependencyGraph(graph, descriptor);
                        pluginNamesUrls.put(descriptor.getName(), url);
                        descriptors.put(url, descriptor);
                    } else {
                        log.info("Not loading disabled plugin: " + url);
                    }
                } catch (Throwable t) {
                    // probably due to invalid XML syntax in the deployment descriptor - the plugin will be ignored
                    log.error("Plugin at [" + url + "] could not be loaded and will therefore not be deployed.", t);
                    continue;
                }
            }

            // our graph is complete, get the order that we have to deploy the plugins
            List<String> deploymentOrder = graph.getDeploymentOrder();

            // now deploy the plugins in the proper order, making sure we build the proper classloaders
            for (String nextPlugin : deploymentOrder) {
                URL pluginUrl = pluginNamesUrls.get(nextPlugin);

                try {
                    ClassLoader pluginClassLoader = this.classLoaderManager.obtainPluginClassLoader(nextPlugin);
                    PluginDescriptor descriptor = descriptors.get(pluginUrl);
                    loadPlugin(pluginUrl, pluginClassLoader, descriptor);
                } catch (Throwable t) {
                    // for some reason, the plugin failed to load - it will be ignored, and its depending plugins will also fail later
                    log.error("Plugin [" + nextPlugin + "] at [" + pluginUrl
                        + "] could not be loaded and will therefore not be deployed.", t);
                    continue;
                }
            }
            log.info("Deployed plugins: " + this.loadedPlugins);
        } catch (Exception e) {
            shutdown(); // have to clean up the environments (e.g. unpacked jars) we might have already created
            log.error("Error initializing plugin container", e);
            throw new RuntimeException("Cannot initialize the plugin container", e);
        }

        return;
    }

    private void initUpdateLoadedPluginsCallback() {
        updateLoadedPlugins = new UpdateLoadedPlugins() {
            PluginTransformer transformer = new PluginTransformer();

            public void execute(PluginDescriptor pluginDescriptor, URL pluginURL) {
                Plugin plugin = transformer.toPlugin(pluginDescriptor, pluginURL);
                loadedPlugins.add(plugin);
            }
        };
    }

    /**
     * @see ContainerService#shutdown()
     */
    public void shutdown() {
        // Inform the plugins we are shutting them down.
        // We want to shut them down in the reverse order that we initialized them.
        Collections.reverse(this.loadedPlugins);
        for (Plugin plugin : loadedPlugins) {
            PluginLifecycleListener listener = pluginLifecycleListenerMgr.getListener(plugin.getName());
            if (listener != null) {
                try {
                    ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(
                        this.classLoaderManager.obtainPluginClassLoader(plugin.getName()));
                    try {
                        listener.shutdown();
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalCL);
                    }
                } catch (Throwable t) {
                    log.warn("Failed to get lifecycle listener to shutdown [" + plugin.getName() + "]. Cause: "
                        + ThrowableUtil.getAllMessages(t));
                }
            }
        }

        // Clean up the plugin environment and the temp dirs that were used by the plugin classloaders.
        for (PluginEnvironment pluginEnvironment : this.loadedPluginEnvironments.values()) {
            pluginEnvironment.destroy();
        }
        this.classLoaderManager.destroy();

        this.loadedPluginEnvironments.clear();
        this.loadedPluginEnvironments = null;

        this.loadedPlugins.clear();
        this.loadedPlugins = null;

        pluginLifecycleListenerMgr.shutdown();

        this.metadataManager = null;
        this.classLoaderManager = null;
    }

    /**
     * @see ContainerService#setConfiguration(PluginContainerConfiguration)
     */
    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setPluginLifecycleListenerManager(PluginLifecycleListenerManager pluginLifecycleListenerManager) {
        this.pluginLifecycleListenerMgr = pluginLifecycleListenerManager;
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
     * Returns the manager of all classloaders created for the plugin manager.
     *
     * @return the classloader manager for all plugins
     */
    public ClassLoaderManager getClassLoaderManager() {
        return this.classLoaderManager;
    }

    public String getAmpsVersion(String pluginName) {
        for (Plugin plugin : loadedPlugins) {
            if (plugin.getName().equals(pluginName)) {
                return plugin.getAmpsVersion();
            }
        }
        return null;
    }

    /**
     * This will create a {@link PluginEnvironment} for the plugin at the given URL. The plugin's descriptor is parsed.
     * Once this method returns, the plugin's components are ready to be created and used.
     *
     * @param  pluginUrl   the new plugin's jar location
     * @param  classLoader the new plugin's classloader
     * @param  pluginDescriptor the already parsed plugin descriptor for this plugin
     * @throws PluginContainerException if the plugin fails to load
     */
    private void loadPlugin(URL pluginUrl, ClassLoader classLoader, PluginDescriptor pluginDescriptor) throws PluginContainerException {

        if (log.isDebugEnabled()) {
            log.debug("Loading plugin from [" + pluginUrl + "] in classloader [" + classLoader + "]...");
        }

        PluginDescriptorLoader pluginDescriptorLoader = new PluginDescriptorLoader(pluginUrl, classLoader);
        PluginEnvironment pluginEnvironment = new PluginEnvironment(pluginDescriptor.getName(), pluginDescriptorLoader);
        String pluginName = pluginEnvironment.getPluginName();

        // tell the plugin we have loaded it
        PluginLifecycleListener overseer = getPluginLifecycleListener(pluginName, pluginEnvironment, pluginDescriptor);
        if (overseer != null) {
            PluginContext context = createPluginContext(pluginName);
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                overseer.initialize(context);
            } catch (Throwable t) {
                throw new PluginContainerException("Plugin Lifecycle Listener failed to initialize plugin", t);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        }

        // everything is loaded and initialized
        this.loadedPluginEnvironments.put(pluginName, pluginEnvironment);
        this.metadataManager.loadPlugin(pluginDescriptor);
        pluginLifecycleListenerMgr.setListener(pluginDescriptor.getName(), overseer);
        updateLoadedPlugins.execute(pluginDescriptor, pluginUrl);

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

        return pluginLifecycleListenerMgr.loadListener(pluginDescriptor, pluginEnvironment);
    }

    private PluginContext createPluginContext(String pluginName) {
        SystemInfo sysInfo = SystemInfoFactory.createSystemInfo();
        File dataDir = new File(this.configuration.getDataDirectory(), pluginName);
        File tmpDir = this.configuration.getTemporaryDirectory();
        String pcName = this.configuration.getContainerName();

        PluginContext context = new PluginContext(pluginName, sysInfo, tmpDir, dataDir, pcName);
        return context;
    }

    /**
     * Given a plugin name, this will get all of its dependencies' plugin URLs and add them to <code>allUrls</code>.
     * This recursively travels N levels deep in the dependency graph.
     *
     * @param pluginName      the name of the plugin to obtain dependency URLs for
     * @param pluginNamesUrls map of all known plugin names and their plugin jar URLs
     * @param allUrls         where the results will be stored
     *
     * TODO: Use it or lose it - this used to be needed, keeping this around just in case it needs to be resurrected
     *       This could be useful if we want to somehow implement multiple plugin classloader inheritance.
     */
    private void getDependentUrls(String pluginName, Map<String, URL> pluginNamesUrls, Set<URL> allUrls) {
        List<String> deps = this.classLoaderManager.getPluginDependencyGraph().getPluginDependencies(pluginName);

        for (String dep : deps) {
            getDependentUrls(dep, pluginNamesUrls, allUrls);
            allUrls.add(pluginNamesUrls.get(dep));
        }

        return;
    }
}
