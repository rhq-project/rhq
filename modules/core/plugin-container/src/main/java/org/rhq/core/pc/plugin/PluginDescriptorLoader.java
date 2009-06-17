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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;

/**
 * This class handles parsing and validating an RHQ plugin's descriptor.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class PluginDescriptorLoader {

    private final Log log = LogFactory.getLog(this.getClass());

    private final URL pluginJarUrl;
    private ClassLoader pluginClassLoader;

    /**
     * This will create the plugin's {@link #getPluginClassLoader() classloader}. If <code>classLoader</code> is
     * specified, it will be used as the environment's classloader, and <code>parentClassLoader</code> will be ignored.
     * If <code>classLoader</code> is <code>null</code>, then a {@link PluginClassLoader} will be created with the given
     * parent classloader and it will unpack all embedded jars found in the given plugin URL's <code>/lib</code>
     * directory.The {@link #loadPluginDescriptor()} method can then be called to parse ands validate the XML plugin
     * descriptor.
     *
     * @param  pluginJarUrl      where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  unpackJars        if <code>true</code> and plugin jar has embedded jars, they will be unpacked - this is
     *                           ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpDir            a directory where the jars are placed if they are to be unpacked; only meaningful when
     *                           <code>unpackJars</code> is <code>true</code>
     *
     * @throws PluginContainerException on failure to load the descriptor
     */
    public PluginDescriptorLoader(URL pluginJarUrl, ClassLoader classLoader, ClassLoader parentClassLoader,
        boolean unpackJars, File tmpDir) throws PluginContainerException {

        this.pluginJarUrl = pluginJarUrl;

        if (classLoader != null) {
            this.pluginClassLoader = classLoader;
        } else {
            this.pluginClassLoader = createPluginClassLoader(pluginJarUrl, parentClassLoader, unpackJars, tmpDir);
        }
    }

    private ClassLoader createPluginClassLoader(URL jarUrl, ClassLoader parentClassLoader, boolean unpackJars,
        File tmpDir) throws PluginContainerException {

        ClassLoader classLoader;
        if (parentClassLoader == null) {
            parentClassLoader = this.getClass().getClassLoader();
        }

        if (jarUrl != null) {
            // Note that we don't really care if the URL uses "file:" or not, we just use File to parse the name from
            // the path.
            String pluginJarName = new File(jarUrl.getPath()).getName();
            classLoader = PluginClassLoader.create(pluginJarName, jarUrl, unpackJars, parentClassLoader, tmpDir);
            log.debug("Created classloader for plugin [" + jarUrl + "]");
        } else {
            // this is mainly to support tests
            classLoader = parentClassLoader;
        }

        return classLoader;
    }

    public ClassLoader getPluginClassLoader() {
        return this.pluginClassLoader;
    }

    /**
     * Loads and validates the plugin descriptor from the plugin jar associated with this loader.
     *
     * @return the JAXB plugin descriptor object
     * @throws PluginContainerException on failure to load the descriptor
     */
    public PluginDescriptor loadPluginDescriptor() throws PluginContainerException {
        return AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(this.pluginJarUrl);
    }

    public void destroy() {
        // If it's our own plugin classloader, let's tell it to clean up its directory where the jars were unpacked.
        if (this.pluginClassLoader instanceof PluginClassLoader) {
            ((PluginClassLoader) this.pluginClassLoader).destroy();
        }
        this.pluginClassLoader = null;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pluginJarUrl=" + this.pluginJarUrl + ", pluginClassLoader="
            + this.pluginClassLoader + "]";
    }
}
