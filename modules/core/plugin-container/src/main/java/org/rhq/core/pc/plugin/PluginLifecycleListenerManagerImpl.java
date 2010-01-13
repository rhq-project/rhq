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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;

public class PluginLifecycleListenerManagerImpl implements PluginLifecycleListenerManager {

    private static final Log log = LogFactory.getLog(PluginLifecycleListenerManagerImpl.class);

    /**
     * Cached instances of objects used to initialize and shutdown individual plugins.
     * Only plugins that declare their own lifecycle listener will have objects in this cache.
     */
    private Map<String, PluginLifecycleListener> cache = new HashMap<String, PluginLifecycleListener>();

    public PluginLifecycleListener loadListener(PluginDescriptor pluginDescriptor, PluginEnvironment pluginEnvironment)
        throws PluginContainerException {
        
        PluginLifecycleListener listener = cache.get(pluginDescriptor.getName());

        if (listener == null) {
            String listenerClassName = getPluginLifecycleListenerClass(pluginDescriptor);

            if (listenerClassName != null) {
                log.debug("Creating plugin lifecycle listener [" + listenerClassName + "] for plugin [" +
                    pluginDescriptor.getName() + "]");
                listener = (PluginLifecycleListener) instantiatePluginClass(pluginEnvironment, listenerClassName);
                log.debug("Created plugin lifecycle listener [" + listenerClassName + "] for plugin [" +
                    pluginDescriptor.getName() + "]");
            }
        }

        return listener;
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

    private Object instantiatePluginClass(PluginEnvironment environment, String className)
        throws PluginContainerException {

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

    public PluginLifecycleListener getListener(String pluginName) {
        return cache.get(pluginName);
    }

    public void setListener(String pluginName, PluginLifecycleListener listener) {
        cache.put(pluginName, listener);
    }

    public void shutdown() {
        cache.clear();
        cache = null;
    }
}
