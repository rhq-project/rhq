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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Manages the context of a loaded plugin.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ServerPluginEnvironment {
    private final Log log = LogFactory.getLog(ServerPluginEnvironment.class);

    private URL pluginUrl;
    private String pluginName;
    private ClassLoader pluginClassLoader;
    private ServerPluginDescriptorType pluginDescriptor;

    /**
     * Creates a new plugin environment.
     *
     * @param pluginUrl         where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param parentClassLoader the parent classloader to be assigned to the plugin's new classloader
     * @param tmpdir            a directory where the jars are placed when unpacked
     * @param descriptor        the plugin descriptor that was found and parsed in the plugin jar at the given URL
     *
     * @throws Exception
     */
    public ServerPluginEnvironment(URL pluginUrl, ClassLoader parentClassLoader, File tmpdir,
        ServerPluginDescriptorType descriptor) throws Exception {
        initialize(pluginUrl, null, parentClassLoader, true, tmpdir, descriptor);
    }

    public void destroy() {
        // if its our own plugin classloader, let's tell it to clean up its directory where the jars were unpacked
        if (this.pluginClassLoader instanceof ServerPluginClassLoader) {
            log.debug("Destroying classloader: " + this.toString());
            ((ServerPluginClassLoader) this.pluginClassLoader).destroy();
        }
    }

    /**
     * This will create the plugin's {@link #getClassLoader() classloader}.
     * If <code>classLoader</code> is specified, it will be used as the environment's
     * classloader and <code>parentClassLoader</code> will be ignored.
     * If <code>classLoader</code> is <code>null</code>, then a {@link ServerPluginClassLoader}
     * will be created with the given parent classloader and it will
     * unpack all embedded jars found in the given plugin URL's <code>/lib</code> directory.
     * 
     * @param pluginJarUrl      where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param classLoader       the classloader used by this environment
     * @param parentClassLoader the parent classloader, if <code>null</code>, will use this class's classloader - this
     *                          is ignored if <code>classloader</code> is not <code>null</code>
     * @param unpackJars        if <code>true</code> and plugin jar has embedded jars, they will be unpacked - this is
     *                          ignored if <code>classloader</code> is not <code>null</code>
     * @param tmpDirectory      a directory where the jars are placed if they are to be unpacked; only meaningful when
     *                          <code>unpackJars</code> is <code>true</code>
     * @param descriptor         the plugin descriptor that was found and parsed in the plugin jar at the given URL
     *
     * @throws Exception
     */
    protected void initialize(URL pluginJarUrl, ClassLoader classLoader, ClassLoader parentClassLoader,
        boolean unpackJars, File tmpDirectory, ServerPluginDescriptorType descriptor) throws Exception {
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

        this.pluginUrl = pluginJarUrl;
        this.pluginDescriptor = descriptor;
        this.pluginClassLoader = classLoader;
        this.pluginName = this.pluginDescriptor.getName();
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