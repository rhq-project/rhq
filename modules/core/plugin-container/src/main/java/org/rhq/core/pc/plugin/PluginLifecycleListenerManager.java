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

import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.agent.PluginContainerException;

/**
 * A manager API that provides operations for loading and instantiating
 * {@link org.rhq.core.pluginapi.plugin.PluginLifecycleListener} classes and caching of those objects. This API is
 * is used by {@link org.rhq.core.pc.plugin.PluginManager} as it the plugin container service that is responsible
 * loading plugins and their corresponding classes.
 */
public interface PluginLifecycleListenerManager {

    /**
     * Loads and instantiates the listener class.
     *
     * @param pluginDescriptor The parsed plugin descriptor
     * @param pluginEnvironment The plugin environment which is needed to perform the class loading
     * @return An instance of the listener class or <code>null</code> if there is no listener
     * @throws PluginContainerException if there is an error instantiating the listener class
     */
    PluginLifecycleListener loadListener(PluginDescriptor pluginDescriptor, PluginEnvironment pluginEnvironment)
        throws PluginContainerException;

    /**
     * Retrieves the cached listener for the specified plugin.
     *
     * @param pluginName The name of the plugin
     * @return The cached listener object or <code>null</code> if the plugin has not declared a listener or if the
     * listener has not yet been loaded.
     */
    PluginLifecycleListener getListener(String pluginName);

    /**
     * Caches the listener for the specified plugin. Listeners should be cached only after having been successfully
     * initialized.
     *
     * @param pluginName The name of the plugin
     * @param listener The initialized plugin lifecycle listener
     */
    void setListener(String pluginName, PluginLifecycleListener listener);

    /**
     * Clears and destroys the cache of listeners
     */
    void shutdown();

}
