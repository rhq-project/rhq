/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Manages the context of a loaded plugin. This includes a classloader created specifically for the plugin and access to
 * its XML descriptor file.
 * 
 * This is an abstract superclass that all plugin types must extend to provide the access to the proper type
 * of descriptor.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ServerPluginEnvironment {
    private final Log log = LogFactory.getLog(ServerPluginEnvironment.class);

    // make these protected mainly to allow unit tests to extend this class to customize these
    protected URL pluginUrl;
    protected String pluginName;
    protected ClassLoader pluginClassLoader;
    protected ServerPluginDescriptorType pluginDescriptor;

    /**
     * Creates a new plugin environment. If <code>classLoader</code> is specified, it will be used as the environment's
     * classloader and <code>parentClassLoader</code> will be ignored. If <code>classLoader</code> is <code>null</code>,
     * then a {@link ServerPluginClassLoader} will be created with the given parent classloader and it will
     * unpack all embedded jars found in the given plugin URL's <code>/lib</code> directory.
     *
     * @param  pluginUrl         where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpdir            a directory where the jars are placed when unpacked
     *
     * @throws Exception
     */
    public ServerPluginEnvironment(URL pluginUrl, ClassLoader classLoader, ClassLoader parentClassLoader, File tmpdir)
        throws Exception {
        initialize(pluginUrl, classLoader, parentClassLoader, true, tmpdir);
    }

    /**
     * This creates a {@link ServerPluginEnvironment} that will allow you to specify the plugin's new class
     * loader to not unpack its embedded jars, if they exist. You normally only pass in <code>false</code> for <code>
     * unpackJars</code> if you only want to process/parse the plugin descriptor but do not plan on using this
     * environment instance for actual runtime of the plugin (because it won't work since you haven't unpacked the
     * jars).
     *
     * <p>If you pass in a non-<code>null</code> <code>classLoader</code>, then both <code>parentClassLoader</code> and
     * <code>unpackJars</code> is ignored since you are saying you already know how to find the classes for this
     * environment.</p>
     *
     * @param  pluginUrl         where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  unpackJars        if <code>true</code> and plugin jar has embedded jars, they will be unpacked - this is
     *                           ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpdir            a directory where the jars are placed if they are to be unpacked; only meaningful when
     *                           <code>unpackJars</code> is <code>true</code>
     *
     * @throws Exception
     */
    public ServerPluginEnvironment(URL pluginUrl, ClassLoader classLoader, ClassLoader parentClassLoader,
        boolean unpackJars, File tmpdir) throws Exception {
        initialize(pluginUrl, classLoader, parentClassLoader, unpackJars, tmpdir);
    }

    public void destroy() {
        // if its our own plugin classloader, let's tell it to clean up its directory where the jars were unpacked
        if (this.pluginClassLoader instanceof ServerPluginClassLoader) {
            log.debug("Destroying classloader: " + this.toString());
            ((ServerPluginClassLoader) this.pluginClassLoader).destroy();
        }
    }

    /**
     * This will create the plugin's {@link #getClassLoader() classloader}, then read and parse the descriptor to make sure its valid.
     *
     * @param  pluginJarUrl      where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param  classLoader       the classloader used by this environment
     * @param  parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                           is ignored if <code>classloader</code> is not <code>null</code>
     * @param  unpackJars        if <code>true</code> and plugin jar has embedded jars, they will be unpacked - this is
     *                           ignored if <code>classloader</code> is not <code>null</code>
     * @param  tmpDirectory      a directory where the jars are placed if they are to be unpacked; only meaningful when
     *                           <code>unpackJars</code> is <code>true</code>
     *
     * @throws Exception
     */
    protected void initialize(URL pluginJarUrl, ClassLoader classLoader, ClassLoader parentClassLoader,
        boolean unpackJars, File tmpDirectory) throws Exception {
        if (classLoader == null) {
            if (parentClassLoader == null) {
                parentClassLoader = this.getClass().getClassLoader();
            }

            if (pluginJarUrl != null) {
                classLoader = ServerPluginClassLoader.create(new File(pluginJarUrl.getPath()).getName(), pluginJarUrl,
                    unpackJars, parentClassLoader, tmpDirectory);
            } else {
                // this is mainly to support tests
                classLoader = parentClassLoader;
            }
        }

        initializeDescriptor(classLoader, pluginJarUrl);

        this.pluginUrl = pluginJarUrl;
        this.pluginClassLoader = classLoader;
        this.pluginName = this.pluginDescriptor.getName();
    }

    private void initializeDescriptor(ClassLoader classloader, URL pluginJarUrl) throws Exception {

        if (pluginJarUrl == null) {
            throw new Exception("A valid plugin JAR URL must be supplied in order to initialize the descriptor");
        }

        log.debug("Initializing descriptor from plugin URL [" + pluginJarUrl + ']');
        this.pluginDescriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginJarUrl);
        return;
    }

    public String getPluginName() {
        return this.pluginName;
    }

    public URL getPluginUrl() {
        return this.pluginUrl;
    }

    public ClassLoader getClassLoader() {
        return this.pluginClassLoader;
    }

    public ServerPluginDescriptorType getPluginDescriptor() {
        return this.pluginDescriptor;
    }

    @Override
    public String toString() {
        return this.pluginName + ": url=[" + this.pluginUrl + "]";
    }
}