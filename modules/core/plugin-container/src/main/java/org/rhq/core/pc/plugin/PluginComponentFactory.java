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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;

/**
 * This class builds and lifecycles the various plugin components for use by the other services.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class PluginComponentFactory implements ContainerService {
    private static final Log log = LogFactory.getLog(PluginComponentFactory.class);

    private Map<ResourceType, ResourceDiscoveryComponent> discoveryComponentsCache;

    /**
     * This will create a new {@link ResourceDiscoveryComponent} instance that can be used to discover and create
     * {@link Resource}s of the given <code>resourceType</code>. The new discovery component will be loaded in the
     * plugin classloader that belongs to the plugin responsible for handling that specific resource type.
     *
     * @param  resourceType the type of resource that is to be discovered
     *
     * @return a new discover component loaded in the proper plugin classloader that can discover resources of the given
     *         type
     *
     * @throws PluginContainerException if failed to create the discovery component instance
     */
    public ResourceDiscoveryComponent getDiscoveryComponent(ResourceType resourceType) throws PluginContainerException {
        // This is an exception for PC unit tests which use a fake platform type.
        if (resourceType.equals(PluginMetadataManager.TEST_PLATFORM_TYPE)) {
            return null;
        }

        ResourceDiscoveryComponent discoveryComponent = discoveryComponentsCache.get(resourceType);

        if (discoveryComponent == null) {
            PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
            PluginEnvironment pluginEnvironment = pluginManager.getPlugin(resourceType.getPlugin());
            String className = pluginManager.getMetadataManager().getDiscoveryClass(resourceType);
            String typeName = resourceType.getName();
            log.debug("Creating discovery component [" + className + "] for resource type [" + typeName + "]");
            discoveryComponent = (ResourceDiscoveryComponent) instantiateClass(pluginEnvironment, className);
            this.discoveryComponentsCache.put(resourceType, discoveryComponent);
            log.debug("Created discovery component [" + className + "] for resource type [" + typeName + "]");
        }

        return discoveryComponent;
    }

    /**
     * This will create a new {@link ResourceComponent} instance that will wrap a {@link Resource} of the given <code>
     * resourceType</code>. The new component will be loaded in the plugin classloader that belongs to the plugin
     * responsible for handling that specific resource type.
     *
     * @param  resourceType the type of Resource that the component will wrap
     *
     * @return a new resource component loaded in the proper plugin classloader
     *
     * @throws PluginContainerException if failed to create the component instance
     */
    public ResourceComponent buildResourceComponent(ResourceType resourceType) throws PluginContainerException {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(resourceType.getPlugin());
        String className = pluginManager.getMetadataManager().getComponentClass(resourceType);
        ResourceComponent component = (ResourceComponent) instantiateClass(pluginEnvironment, className);

        log.debug("Created resource component [" + className + "] of resource type [" + resourceType + "]");

        return component;
    }

    /**
     * This will load the class definition of <code>className</code> using the
     * {@link PluginEnvironment#getPluginClassLoader() plugin classloader} found in the given <code>environment</code>.
     *
     * @param  environment
     * @param  className
     *
     * @return the new object of type <code>className</code> that was loaded via the plugin classloader
     *
     * @throws PluginContainerException if the class could not be instantiated for some reason
     */
    private Object instantiateClass(PluginEnvironment environment, String className) throws PluginContainerException {
        ClassLoader loader = environment.getPluginClassLoader();
        log.debug("Loading class '" + className + "' via " + loader + "...");

        try {
            Class<?> clazz = Class.forName(className, true, loader);
            log.debug("Loaded class: " + clazz);
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

    /**
     * Creates our (initially empty) cache of discovery components.
     *
     * @see ContainerService#initialize()
     */
    public void initialize() {
        this.discoveryComponentsCache = new HashMap<ResourceType, ResourceDiscoveryComponent>();
    }

    /**
     * Clears our cache of discovery components.
     *
     * @see ContainerService#shutdown()
     */
    public void shutdown() {
        this.discoveryComponentsCache.clear();
        this.discoveryComponentsCache = null;
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        // we don't need the configuration for anything
        return;
    }
}