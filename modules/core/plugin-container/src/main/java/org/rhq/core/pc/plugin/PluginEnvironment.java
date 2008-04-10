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
package org.rhq.core.pc.plugin;

import org.jetbrains.annotations.NotNull;

/**
 * Stores the context of a loaded plugin. This currently includes the plugin's name and the loader that was used to
 * load its plugin descriptor.
 *
 * @author Ian Springer
 */
public class PluginEnvironment {

    protected String pluginName;
    protected PluginDescriptorLoader pluginDescriptorLoader;

    /**
     * Creates a new plugin environment.
     *
     * @param  pluginName the plugin's name
     * @param  pluginDescriptorLoader the loader that was used to load the plugin's descriptor
     */
    public PluginEnvironment(@NotNull String pluginName,
                             @NotNull PluginDescriptorLoader pluginDescriptorLoader) {
        this.pluginName = pluginName;
        this.pluginDescriptorLoader = pluginDescriptorLoader;
    }

    public void destroy() {
        // Clean up the temp dir that was used by the plugin classloader.
        this.pluginDescriptorLoader.destroy();
    }

    public String getPluginName() {
        return this.pluginName;
    }

    public ClassLoader getPluginClassLoader() {
        return this.pluginDescriptorLoader.getPluginClassLoader();
    }

    @Override
    public String toString() {
        return "PluginEnvironment[pluginName=" + this.pluginName + ", pluginDesciptorLoader=" +
                this.pluginDescriptorLoader + "]";
    }
}