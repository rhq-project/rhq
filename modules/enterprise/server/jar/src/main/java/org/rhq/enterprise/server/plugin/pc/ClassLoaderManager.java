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
package org.rhq.enterprise.server.plugin.pc;

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

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Manages the classloaders created and used by the master plugin container and all plugins.
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
     * Provides a map keyed on plugin name whose values are the URLs to those plugin jars.
     */
    private final Map<String, URL> pluginNamesUrls;

    /**
     * The parent classloader for those classloaders at the top of the classloader hierarchy.
     */
    private final ClassLoader rootClassLoader;

    /**
     * These are the classloaders that are built for each plugin.
     * This map is keyed on plugin name.
     * 
     * @see #obtainServerPluginClassLoader(String)
     */
    private final Map<String, ClassLoader> serverPluginClassLoaders;

    /**
     * Creates the object that will manage all classloaders for the plugins deployed in the server.
     * 
     * @param plugins maps a plugin URL to that plugin's descriptor
     * @param rootClassLoader the classloader at the top of the classloader hierarchy
     * @param tmpDir where the classloaders can write out the jars that are embedded in the plugin jars
     */
    public ClassLoaderManager(Map<URL, ? extends ServerPluginDescriptorType> plugins, ClassLoader rootClassLoader,
        File tmpDir) {

        this.rootClassLoader = rootClassLoader;
        this.tmpDir = tmpDir;
        this.serverPluginClassLoaders = new HashMap<String, ClassLoader>();

        this.pluginNamesUrls = new HashMap<String, URL>(plugins.size());
        for (Map.Entry<URL, ? extends ServerPluginDescriptorType> entry : plugins.entrySet()) {
            this.pluginNamesUrls.put(entry.getValue().getName(), entry.getKey());
        }

        return;
    }

    /**
     * Cleans up this object and all classloaders it has created.
     */
    public void shutdown() {
        for (ClassLoader doomedCL : getUniqueServerPluginClassLoaders()) {
            if (doomedCL instanceof ServerPluginClassLoader) {
                try {
                    ((ServerPluginClassLoader) doomedCL).destroy();
                } catch (Exception e) {
                    log.warn("Failed to destroy classloader: " + doomedCL, e);
                }
            }
        }
        this.serverPluginClassLoaders.clear();
        return;
    }

    @Override
    public String toString() {
        Set<ClassLoader> classLoaders;
        StringBuilder str = new StringBuilder(this.getClass().getSimpleName());

        str.append(" tmp-dir=[").append(this.tmpDir).append(']');

        classLoaders = getUniqueServerPluginClassLoaders();
        str.append(", #plugin CLs=[").append(classLoaders.size());
        classLoaders.clear(); // help out the GC, clear out the shallow copy container

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
     * Returns a plugin classloader (creating it if necessary).
     * 
     * @param pluginName the plugin whose classloader is to be created
     * @return the plugin classloader
     * @throws Exception
     */
    public synchronized ClassLoader obtainServerPluginClassLoader(String pluginName) throws Exception {

        ClassLoader cl = this.serverPluginClassLoaders.get(pluginName);

        if (cl == null) {
            URL pluginJarUrl = this.pluginNamesUrls.get(pluginName);

            if (log.isDebugEnabled()) {
                log.debug("Creating classloader for plugin [" + pluginName + "] from URL [" + pluginJarUrl + ']');
            }

            ClassLoader parentClassLoader = this.rootClassLoader;
            cl = createClassLoader(pluginJarUrl, null, parentClassLoader);
            this.serverPluginClassLoaders.put(pluginName, cl);
        }

        return cl;
    }

    /**
     * Returns the total number of plugin classloaders that have been created and managed.
     * This method is here just to support a plugin container management MBean.
     * 
     * @return number of plugin classloaders that are currently created and being used
     */
    public synchronized int getNumberOfServerPluginClassLoaders() {
        return this.serverPluginClassLoaders.size();
    }

    /**
     * Returns a shallow copy of the plugin classloaders keyed on plugin name. This method is here
     * just to support a plugin container management MBean.
     * 
     * Do not use this method to obtain a plugin's classloader, instead, you want to use
     * {@link #obtainServerPluginClassLoader(String)}.
     * 
     * @return all plugin classloaders currently assigned to plugins (will never be <code>null</code>)
     */
    public synchronized Map<String, ClassLoader> getServerPluginClassLoaders() {
        return new HashMap<String, ClassLoader>(this.serverPluginClassLoaders);
    }

    private Set<ClassLoader> getUniqueServerPluginClassLoaders() {
        HashSet<ClassLoader> uniqueClassLoaders = new HashSet<ClassLoader>(this.serverPluginClassLoaders.values());
        return uniqueClassLoaders;
    }

    private ClassLoader createClassLoader(URL mainJarUrl, List<URL> additionalJars, ClassLoader parentClassLoader)
        throws Exception {

        ClassLoader classLoader;
        if (parentClassLoader == null) {
            parentClassLoader = this.getClass().getClassLoader();
        }

        if (mainJarUrl != null) {
            // Note that we don't really care if the URL uses "file:" or not,
            // we just use File to parse the name from the path.
            String pluginJarName = new File(mainJarUrl.getPath()).getName();

            if (additionalJars == null || additionalJars.size() == 0) {
                classLoader = ServerPluginClassLoader.create(pluginJarName, mainJarUrl, true, parentClassLoader,
                    this.tmpDir);
            } else {
                List<URL> allJars = new ArrayList<URL>(additionalJars.size() + 1);
                allJars.add(mainJarUrl);
                allJars.addAll(additionalJars);
                classLoader = ServerPluginClassLoader.create(pluginJarName, allJars.toArray(new URL[allJars.size()]),
                    true, parentClassLoader, this.tmpDir);
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
