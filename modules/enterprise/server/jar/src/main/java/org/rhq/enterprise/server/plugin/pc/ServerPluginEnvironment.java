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

import java.net.URL;

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Manages the context of a loaded plugin.
 * 
 * @author John Mazzitelli
 */
public class ServerPluginEnvironment {
    private final URL pluginUrl;
    private final String pluginName;
    private final ClassLoader pluginClassLoader;
    private final ServerPluginDescriptorType pluginDescriptor;

    /**
     * Creates a new plugin environment.
     *
     * @param pluginUrl   where the plugin jar is located (may be <code>null</code>, mainly to support tests)
     * @param classLoader the plugin's new classloader
     * @param descriptor  the plugin descriptor that was found and parsed in the plugin jar at the given URL
     *
     * @throws Exception
     */
    public ServerPluginEnvironment(URL pluginUrl, ClassLoader classLoader, ServerPluginDescriptorType descriptor)
        throws Exception {
        this.pluginUrl = pluginUrl;
        this.pluginClassLoader = classLoader;
        this.pluginDescriptor = descriptor;
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