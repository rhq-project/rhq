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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.domain.resource.ClassLoaderType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;

/**
 * Manages the classloaders created and used by the plugin container and all plugins/resources.
 * 
 * @author John Mazzitelli
 */
public class ClassLoaderManager {
    private final Log log = LogFactory.getLog(ClassLoaderManager.class);

    /**
     * Directory where temporary files can be stored. Used to extract jars embedded in plugin jars.
     */
    private final File tmpDir;

    /**
     * Indicates what plugins are deployed and their hierarchies.
     */
    private final PluginDependencyGraph pluginDependencyGraph;

    /**
     * Provides a map keyed on plugin name whose values are the URLs to those plugin jars.
     */
    private final Map<String, URL> pluginNamesUrls;

    /**
     * The parent classloader for those classloaders at the top of the classloader hierarchy.
     */
    private final ClassLoader rootClassLoader;

    /**
     * These are the classloaders that are built that follow the plugin hierarchy as found in the
     * dependency graph. These classloaders follow the hierarchy as defined by the plugin descriptors
     * and their required dependencies. This map is keyed on plugin name.
     * See {@link #obtainPluginClassLoader(String)}.
     */
    private final Map<String, ClassLoader> pluginClassLoaders;

    /**
     * These are the classloaders that are to be used to load discovery components whose discovered
     * resources have parents that contain connection classes necessary for the discovery components
     * to do their job. This map is keyed on a hash calculated from plugin name and parent classloader.
     * See {@link #obtainDiscoveryClassLoader(String, ClassLoader)}.
     */
    private final Map<String, ClassLoader> discoveryClassLoaders;

    /**
     * Contains all classloaders for all individual resources that got classloaders created for it.
     * The map is keyed on a hash built from data belonging to a resource and its parent resource.
     * See {@link #obtainResourceClassLoader(Resource, ResourceContainer, List)}.
     */
    private final Map<CanonicalResourceKey, ClassLoader> resourceClassLoaders;

    /**
     * If <code>true</code>, then this manager will create instances of classloaders for those
     * individual resources that require it. If <code>false</code>, this manager will never create
     * individual classloaders for resources; it will only ever obtain plugin classloaders for resources.
     * This means that <code>false</code> will force {@link #obtainResourceClassLoader(Resource, ResourceContainer, List)}
     * to only ever return plugin classloaders.
     */
    private final boolean createResourceClassLoaders;

    /**
     * Creates the object that will manage all classloaders for the plugins deployed in the given plugin deployment graph.
     * 
     * @param pluginNamesUrls maps a plugin name with the URL to that plugin's jar file
     * @param graph the graph that provides plugin dependency information for all plugins that are deployed
     * @param rootClassLoader the classloader at the top of the classloader hierarchy to be used as the parent classloader
     *                        for those classloaders that are not children to other shared/resource classloaders.
     * @param tmpDir where the classloaders can write out the jars that are embedded in the plugin jars
     * @param createResourceClassLoaders if <code>true</code>, the classloader manager will create resource classloader
     *                                   instances when appropriate. If <code>false</code>, this classloader manager
     *                                   will never create classloaders on a per-resource instance basis. It will only
     *                                   ever create and return plugin classloaders. This will be <code>false</code> when
     *                                   the plugin container is running embedded inside a managed resource and that
     *                                   managed resource will provide the necessary client jars via the root classloader.
     */
    public ClassLoaderManager(Map<String, URL> pluginNamesUrls, PluginDependencyGraph graph,
        ClassLoader rootClassLoader, File tmpDir, boolean createResourceClassLoaders) {

        this.rootClassLoader = rootClassLoader;
        this.pluginClassLoaders = new HashMap<String, ClassLoader>();
        this.resourceClassLoaders = new HashMap<CanonicalResourceKey, ClassLoader>();
        this.discoveryClassLoaders = new HashMap<String, ClassLoader>();

        this.pluginNamesUrls = pluginNamesUrls;
        this.pluginDependencyGraph = graph;
        this.tmpDir = tmpDir;
        this.createResourceClassLoaders = createResourceClassLoaders;
    }

    /**
     * Cleans up this object and all classloaders it has created.
     */
    public void destroy() {
        // destroy any resource classloaders we've created
        for (ClassLoader doomedCL : getUniqueResourceClassLoaders()) {
            if (doomedCL instanceof PluginClassLoader) {
                ((PluginClassLoader) doomedCL).destroy();
            }
        }
        this.resourceClassLoaders.clear();

        // destroy any discovery classloaders we've created
        for (ClassLoader doomedCL : getUniqueDiscoveryClassLoaders()) {
            if (doomedCL instanceof PluginClassLoader) {
                ((PluginClassLoader) doomedCL).destroy();
            }
        }
        this.discoveryClassLoaders.clear();

        // destroy any plugin classloaders we've created
        for (ClassLoader doomedCL : getUniquePluginClassLoaders()) {
            if (doomedCL instanceof PluginClassLoader) {
                ((PluginClassLoader) doomedCL).destroy();
            }
        }
        this.pluginClassLoaders.clear();

        return;
    }

    @Override
    public String toString() {
        Set<ClassLoader> classLoaders;
        StringBuilder str = new StringBuilder(this.getClass().getSimpleName());

        classLoaders = getUniquePluginClassLoaders();
        str.append(" [#plugin CLs=").append(classLoaders.size());
        classLoaders.clear(); // help out the GC, clear out the shallow copy container

        classLoaders = getUniqueDiscoveryClassLoaders();
        str.append(", #discovery CLs=").append(classLoaders.size());
        classLoaders.clear();

        classLoaders = getUniqueResourceClassLoaders();
        str.append(", #resource CLs=").append(classLoaders.size());
        classLoaders.clear();

        str.append(']');
        return str.toString();
    }

    /**
     * Returns the classloader that should be the ancestor (i.e. top most parent) of all plugin classloaders.
     * 
     * @return the root plugin classloader for all plugins
     */
    public ClassLoader getRootClassLoader() {
        return this.rootClassLoader;
    }

    /**
     * Returns the graph of all the plugins and their dependencies.
     * 
     * @return plugin dependency graph
     */
    public PluginDependencyGraph getPluginDependencyGraph() {
        return pluginDependencyGraph;
    }

    /**
     * Returns a plugin classloader (creating it if necessary) that contains the plugin jar and whose parent
     * classloader is that of the the classloader for the required (&ltdepends>) plugin. In other words,
     * this follows the plugin dependency hierarchy as defined in the given
     * {@link #getPluginDependencyGraph() dependency graph}.
     * 
     * @param pluginName the plugin whose classloader is to be created
     * @return the plugin classloader
     * @throws PluginContainerException
     */
    public synchronized ClassLoader obtainPluginClassLoader(String pluginName) throws PluginContainerException {

        ClassLoader cl = this.pluginClassLoaders.get(pluginName);
        if (cl == null) {
            URL pluginJarUrl = this.pluginNamesUrls.get(pluginName);
            String useClassesDep = this.pluginDependencyGraph.getUseClassesDependency(pluginName);
            ClassLoader parentClassLoader;
            if (useClassesDep != null) {
                parentClassLoader = obtainPluginClassLoader(useClassesDep);

                if (log.isDebugEnabled()) {
                    List<String> dependencies = this.pluginDependencyGraph.getPluginDependencies(pluginName);
                    log.debug("Creating classloader for dependent plugin [" + pluginName + "] from URL ["
                        + pluginJarUrl + "] that has the following dependencies: " + dependencies);
                }
            } else {
                parentClassLoader = this.rootClassLoader;

                if (log.isDebugEnabled()) {
                    log.debug("Creating classloader for independent plugin [" + pluginName + "] from URL ["
                        + pluginJarUrl + ']');
                }
            }
            cl = createClassLoader(pluginJarUrl, null, parentClassLoader);
            this.pluginClassLoaders.put(pluginName, cl);
        }

        return cl;
    }

    /**
     * Similar to {@link #obtainPluginClassLoader(String)}, however, the classloader to be returned
     * will have the given parent classloader (as opposed to having parent classloaders that follow the
     * plugin dependency graph hierarchy). This is used to support loading discovery components where
     * those discovery components need to use connections to the parent resource in order to perform their
     * discovery.
     * 
     * @param pluginName the name of the plugin where the discovery component can be found
     * @param parentClassLoader the parent classloader of the new classloader being created
     * @return the new plugin classloader
     * @throws PluginContainerException 
     */
    public synchronized ClassLoader obtainDiscoveryClassLoader(String pluginName, ClassLoader parentClassLoader)
        throws PluginContainerException {

        String hash = pluginName + '-' + Integer.toHexString(parentClassLoader.hashCode());
        ClassLoader cl = this.discoveryClassLoaders.get(hash);
        if (cl == null) {
            URL pluginJarUrl = this.pluginNamesUrls.get(pluginName);
            if (log.isDebugEnabled()) {
                log.debug("Creating discovery classloader [" + hash + "] from URL [" + pluginJarUrl + ']');
            }
            cl = createClassLoader(pluginJarUrl, null, parentClassLoader);
            this.discoveryClassLoaders.put(hash, cl);
        }
        return cl;
    }

    /**
     * Returns the classloader that the given resource should use when being invoked.
     * Note that the parent container should never be <code>null</code>; if it is, the caller should
     * just assume the resource is the top-level platform and just get that platform resource's
     * plugin classloader.
     * 
     * @param resource the resource whose classloader is to be obtained
     * @param parent the container for the parent of the given resource (must never be <code>null</code>)
     * @param additionalJars additional jars to put into the classloader
     * @return the resource's classloader - this will be newly created if this is the first time we've been asked to obtain it
     * @throws PluginContainerException
     */
    public synchronized ClassLoader obtainResourceClassLoader(Resource resource, ResourceContainer parent,
        List<URL> additionalJars) throws PluginContainerException {

        if (resource == null) {
            throw new PluginContainerException("resource must not be null");
        }
        if (parent == null) {
            throw new PluginContainerException("parent must not be null");
        }

        CanonicalResourceKey mapKey = new CanonicalResourceKey(resource, parent.getResource());
        ClassLoader resourceCL = this.resourceClassLoaders.get(mapKey);
        if (resourceCL == null) {

            if (this.createResourceClassLoaders) {
                ResourceType resourceType = resource.getResourceType();
                String resourcePlugin = resourceType.getPlugin();
                ClassLoaderType resourceClassLoaderType = resourceType.getClassLoaderType();

                ResourceType parentResourceType = parent.getResource().getResourceType();
                String parentPlugin = parentResourceType.getPlugin();
                ClassLoaderType parentClassLoaderType = parentResourceType.getClassLoaderType();

                if (resourcePlugin.equals(parentPlugin)) {
                    // both resource and parent are from the same plugin, resource uses the same CL as its parent
                    resourceCL = parent.getResourceClassLoader();
                } else {
                    // resource and parent are from different plugins

                    // Determine if we reached the top of the resource hierarchy (i.e. the parent is the top platform resource)
                    // This will only happen if the classloader type is SHARED and it equals the platform resource.
                    boolean isParentTopPlatform = false;
                    if (parentClassLoaderType == ClassLoaderType.SHARED) {
                        InventoryManager inventoryMgr = PluginContainer.getInstance().getInventoryManager();
                        isParentTopPlatform = parent.getResource().equals(inventoryMgr.getPlatform());
                    }

                    if (resourceClassLoaderType == ClassLoaderType.SHARED
                        && parentClassLoaderType == ClassLoaderType.SHARED) {

                        // Both resource and parent are willing to share their classloader.
                        // If we reached the top of the resource hierarchy, our resource's classloader needs only
                        // be its own plugin classloader.
                        // If we are not at the top, the resource is running inside its parent resource and thus needs to have
                        // that parent resource's classloader, but it also needs to have its own classes from its own plugin.
                        // Therefore, in both cases, the resource gets assigned its own plugin classloader. This means that
                        // its plugin must also have the parent plugin as a required dependency.
                        // (e.g. RHQ-Server plugin resource runs inside JBossAS server)
                        // So, yes this if-else does the same, but it is here in case we need to add additional functionality later.
                        if (isParentTopPlatform) {
                            resourceCL = obtainPluginClassLoader(resourcePlugin);
                        } else {
                            resourceCL = obtainPluginClassLoader(resourcePlugin);
                        }
                    } else if (resourceClassLoaderType == ClassLoaderType.INSTANCE
                        && parentClassLoaderType == ClassLoaderType.SHARED) {

                        // Resource wants its own classloader, even though the parent is willing to share its classloader.
                        // If we reached the top of the resource hierarchy, our resource's new classloader needs to follow
                        // up its plugin classloader hierachy. If we are not at the top, the resource is running inside
                        // its parent resource and thus needs to have that parent resource's classloader. 
                        if (isParentTopPlatform) {
                            resourceCL = createClassLoader(this.pluginNamesUrls.get(resourcePlugin), additionalJars,
                                obtainPluginClassLoader(resourcePlugin).getParent());
                        } else {
                            resourceCL = createClassLoader(this.pluginNamesUrls.get(resourcePlugin), additionalJars,
                                obtainPluginClassLoader(parentPlugin));
                        }
                    } else if (resourceClassLoaderType == ClassLoaderType.SHARED
                        && parentClassLoaderType == ClassLoaderType.INSTANCE) {

                        // Resource is willing to share its own classloader, but the parent has created its own instance.
                        // So, even though this resource says it can be shared, it really needs to have its own instance
                        // because the parent has its own instance. Therefore, the resource has its own instance of a
                        // classloader whose parent classloader is that of its parent resource (e.g. Hibernate running in JBossAS).
                        URL resourcePluginUrl = this.pluginNamesUrls.get(resourcePlugin);
                        ClassLoader parentClassLoader = parent.getResourceClassLoader();
                        resourceCL = createClassLoader(resourcePluginUrl, additionalJars, parentClassLoader);

                    } else if (resourceClassLoaderType == ClassLoaderType.INSTANCE
                        && parentClassLoaderType == ClassLoaderType.INSTANCE) {

                        // Both the resource and parent want their own classloader instance.
                        // This is effectively the same as in the SHARED/INSTANCE case above.
                        URL resourcePluginUrl = this.pluginNamesUrls.get(resourcePlugin);
                        ClassLoader parentClassLoader = parent.getResourceClassLoader();
                        resourceCL = createClassLoader(resourcePluginUrl, additionalJars, parentClassLoader);
                    } else {
                        throw new IllegalStateException("Classloader type was never set. rclt=["
                            + resourceClassLoaderType + "], pclt=[" + parentClassLoaderType + "]");
                    }
                }
            } else {
                // The plugin container has told us to not create individual resource classloaders, so just return
                // the resource's plugin classloader and assume the root classloader will give us the extra classes needed. 
                ResourceType resourceType = resource.getResourceType();
                String resourcePlugin = resourceType.getPlugin();
                resourceCL = obtainPluginClassLoader(resourcePlugin);
            }

            this.resourceClassLoaders.put(mapKey, resourceCL);
        }

        return resourceCL;
    }

    /**
     * Returns the total number of plugin classloaders that have been created and managed.
     * This method is here just to support the plugin container management MBean.
     * 
     * @return number of plugin classloaders that are currently created and being used
     */
    public synchronized int getNumberOfPluginClassLoaders() {
        return this.pluginClassLoaders.size();
    }

    /**
     * Returns the total number of discovery classloaders that have been created and managed.
     * This method is here just to support the plugin container management MBean.
     * 
     * @return number of discovery classloaders that are currently created and being used
     */
    public synchronized int getNumberOfDiscoveryClassLoaders() {
        return this.discoveryClassLoaders.size();
    }

    /**
     * Returns the total number of resource classloaders that have been created and managed.
     * This is the count of unique classloader instances that have been created - each resource
     * classloader could potentially be assigned to multiple resources.
     * This method is here just to support the plugin container management MBean.
     * 
     * @return number of unique resource classloaders that are currently created and assigned to resources
     */
    public synchronized int getNumberOfResourceClassLoaders() {
        Set<ClassLoader> uniqueClassLoaders = getUniqueResourceClassLoaders();
        int size = uniqueClassLoaders.size();
        uniqueClassLoaders.clear(); // this is a shallow copy, help out the GC by nulling it out
        return size;
    }

    /**
     * Returns a shallow copy of the plugin classloaders keyed on plugin name. This method is here
     * just to support the plugin container management MBean.
     * 
     * Do not use this method to obtain a plugin's classloader, instead, you want to use
     * {@link #obtainPluginClassLoader(String)}.
     * 
     * @return all plugin classloaders currently assigned to plugins (will never be <code>null</code>)
     */
    public synchronized Map<String, ClassLoader> getPluginClassLoaders() {
        return new HashMap<String, ClassLoader>(this.pluginClassLoaders);
    }

    /**
     * Returns a shallow copy of the discovery classloaders keyed on a hash calculated from
     * plugin name and parent classloader. This method is here just to support the plugin
     * container management MBean.
     * 
     * Do not use this method to obtain a discovery classloader, instead, you want to use
     * {@link #obtainDiscoveryClassLoader(String, ClassLoader)}.
     * 
     * @return all discovery classloaders currently created (will never be <code>null</code>)
     */
    public synchronized Map<String, ClassLoader> getDiscoveryClassLoaders() {
        return new HashMap<String, ClassLoader>(this.discoveryClassLoaders);
    }

    /**
     * Returns a shallow copy of the resource classloaders keyed on a canonical keys.
     * This method is here just to support the plugin container management MBean.
     * 
     * Do not use this method to obtain a resource's classloader, instead, you want to use
     * {@link #obtainResourceClassLoader(Resource, ResourceContainer, List)}.
     * 
     * @return all resource classloaders currently assigned to resources (will never be <code>null</code>)
     */
    public synchronized Map<CanonicalResourceKey, ClassLoader> getResourceClassLoaders() {
        return new HashMap<CanonicalResourceKey, ClassLoader>(this.resourceClassLoaders);
    }

    /**
     * Returns <code>true</code> if this manager will create instances of classloaders for those
     * individual Resources that require it, or <code>false</code> if this manager will never create
     * individual classloaders for Resources (i.e. {@link #obtainResourceClassLoader(Resource, ResourceContainer, List)}
     * will always just return plugin classloaders).
     *
     * @return <code>true</code> if this manager will create instances of classloaders for those
     * individual Resources that require it, or <code>false</code> if this manager will never create
     * individual classloaders for Resources (i.e. {@link #obtainResourceClassLoader(Resource, ResourceContainer, List)}
     * will always just return plugin classloaders)
     */
    public boolean isCreateResourceClassLoaders() {
        return this.createResourceClassLoaders;
    }

    private Set<ClassLoader> getUniquePluginClassLoaders() {
        HashSet<ClassLoader> uniqueClassLoaders = new HashSet<ClassLoader>(this.pluginClassLoaders.values());
        return uniqueClassLoaders;
    }

    private Set<ClassLoader> getUniqueDiscoveryClassLoaders() {
        HashSet<ClassLoader> uniqueClassLoaders = new HashSet<ClassLoader>(this.discoveryClassLoaders.values());
        return uniqueClassLoaders;
    }

    private Set<ClassLoader> getUniqueResourceClassLoaders() {
        HashSet<ClassLoader> uniqueClassLoaders = new HashSet<ClassLoader>(this.resourceClassLoaders.values());
        return uniqueClassLoaders;
    }

    private ClassLoader createClassLoader(URL mainJarUrl, List<URL> additionalJars, ClassLoader parentClassLoader)
        throws PluginContainerException {

        ClassLoader classLoader;
        if (parentClassLoader == null) {
            parentClassLoader = this.getClass().getClassLoader();
        }

        if (mainJarUrl != null) {
            // Note that we don't really care if the URL uses "file:" or not,
            // we just use File to parse the name from the path.
            String pluginJarName = new File(mainJarUrl.getPath()).getName();

            if (additionalJars == null || additionalJars.size() == 0) {
                classLoader = PluginClassLoader.create(pluginJarName, mainJarUrl, true, parentClassLoader, this.tmpDir);
            } else {
                List<URL> allJars = new ArrayList<URL>(additionalJars.size() + 1);
                allJars.add(mainJarUrl);
                allJars.addAll(additionalJars);
                classLoader = PluginClassLoader.create(pluginJarName, allJars.toArray(new URL[allJars.size()]), true,
                    parentClassLoader, this.tmpDir);
            }

            if (log.isDebugEnabled()) {
                log.debug("Created classloader for plugin jar [" + mainJarUrl + "] with additional jars ["
                    + additionalJars + "]");
            }
        } else {
            // this is mainly to support tests
            classLoader = parentClassLoader;
        }

        return classLoader;
    }
}
