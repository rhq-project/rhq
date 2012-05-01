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

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;

/**
 * This class builds and lifecycles the various plugin components for use by the other services.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class PluginComponentFactory implements ContainerService {
    private static final Log log = LogFactory.getLog(PluginComponentFactory.class);

    private PluginContainerConfiguration configuration;
    private InventoryManager inventoryManager;
    private PluginManager pluginManager;

    /**
     * This will create a new {@link ResourceDiscoveryComponent} instance that can be used to discover and create
     * {@link Resource}s of the given <code>resourceType</code>. The new discovery component will be loaded in the
     * plugin classloader that belongs to the plugin responsible for handling that specific resource type.
     *
     * @param resourceType the type of resource that is to be discovered
     * @param parentResourceContainer represents the parent resource for any newly discovered resources that may be found
     *                                by the discovery component that is created by this method. This can be <code>null</code>,
     *                                but ONLY in the case when the resourceType is that of a root platform type.
     * @return a new discover component loaded in the proper plugin classloader that can discover resources of the given
     *         type
     *
     * @throws PluginContainerException if failed to create the discovery component instance
     */
    public ResourceDiscoveryComponent getDiscoveryComponent(ResourceType resourceType,
        ResourceContainer parentResourceContainer) throws PluginContainerException {

        // This is an exception for PC unit tests which use a fake platform type.
        if (resourceType.equals(PluginMetadataManager.TEST_PLATFORM_TYPE)) {
            return null;
        }

        PluginManager pluginMgr = getPluginManager();
        PluginMetadataManager metadataManager = pluginMgr.getMetadataManager();
        String className = metadataManager.getDiscoveryClass(resourceType);
        String typeName = resourceType.getName();
        String pluginName = resourceType.getPlugin();

        if (log.isDebugEnabled()) {
            log.debug("Creating discovery component [" + className + "] for resource type [" + typeName + ']');
        }

        ClassLoader classLoader = getDiscoveryComponentClassLoader(parentResourceContainer, pluginName);

        ResourceDiscoveryComponent discoveryComponent = (ResourceDiscoveryComponent) instantiateClass(classLoader,
            className);

        if (log.isDebugEnabled()) {
            log.debug("Created discovery component [" + className + "] for resource type [" + typeName + ']');
        }

        return discoveryComponent;
    }

    public ClassLoader getDiscoveryComponentClassLoader(ResourceContainer parentResourceContainer, String pluginName)
            throws PluginContainerException {
        // Determine what classloader to use to load the discovery component class. If the parent resource for newly
        // discovered resources is the root platform (or if the discovered resource is going TO BE the root platform),
        // we can just use the plugin classloader. If discovered resources will be children of a top level server or
        // of a low-level resource, the classloader will be that of the discovery plugin but will have a parent
        // classloader that is the classloader of the parent resource in order for the discovery component to talk
        // to its parent resource using connection classes provided by the parent resource classloader.
        ClassLoaderManager classLoaderMgr = pluginManager.getClassLoaderManager();
        ClassLoader classLoader;
        if (parentResourceContainer == null
            || getInventoryManager().getPlatform().equals(parentResourceContainer.getResource())) {
            classLoader = classLoaderMgr.obtainPluginClassLoader(pluginName);
        } else {
            ClassLoader parentClassLoader = parentResourceContainer.getResourceClassLoader();
            // only create if plugins are different, otherwise, use parent classloader as is
            if (pluginName.equals(parentResourceContainer.getResource().getResourceType().getPlugin())) {
                classLoader = parentClassLoader;
            } else {
                classLoader = classLoaderMgr.obtainDiscoveryClassLoader(pluginName, parentClassLoader);
            }
        }
        return classLoader;
    }

    /**
     * This will create a new {@link ResourceComponent} instance that will represent the given {@link Resource}.
     * The new component will be loaded in the proper plugin classloader based on its specific resource type.
     *
     * @param  resource the resource that the component will wrap
     *
     * @return a new resource component loaded in the proper plugin classloader
     *
     * @throws PluginContainerException if failed to create the component instance
     */
    public ResourceComponent buildResourceComponent(Resource resource) throws PluginContainerException {
        ResourceType resourceType = resource.getResourceType();
        if (PluginMetadataManager.TEST_PLATFORM_TYPE.equals(resourceType)) {
            return new ResourceComponent() {
                public AvailabilityType getAvailability() {
                    return AvailabilityType.UP;
                }

                public void start(ResourceContext context) {
                }

                public void stop() {
                }
            };            
        }
        String className = getPluginManager().getMetadataManager().getComponentClass(resourceType);
        ClassLoader resourceClassloader = getResourceClassloader(resource);
        ResourceComponent component = (ResourceComponent) instantiateClass(resourceClassloader, className);

        if (log.isDebugEnabled()) {
            log.debug("Created resource component [" + className + "] of resource type [" + resourceType + ']');
        }
        return component;
    }

    /**
     * Given a resource, this will return the appropriate classloader for that resource.
     * If no classloader has been created for it yet, one will be created by this method.
     * 
     * @param resource the resource whose classloader is to be returned (and possibly created if needed)
     *
     * @return the resource's classloader
     *
     * @throws PluginContainerException if the resource's classloader could not be created
     */
    public ClassLoader getResourceClassloader(Resource resource) throws PluginContainerException {

        try {
            InventoryManager inventoryMgr = getInventoryManager();
            PluginManager pluginMgr = getPluginManager();
            ClassLoaderManager classLoaderMgr = pluginMgr.getClassLoaderManager();

            ResourceType resourceType = resource.getResourceType();

            // supports tests
            if (resourceType.equals(PluginMetadataManager.TEST_PLATFORM_TYPE)) {
                return Thread.currentThread().getContextClassLoader();
            }

            // information about the resource's parent
            Resource parentResource = resource.getParentResource();
            ResourceContainer parentContainer;

            if (parentResource != null) {
                parentContainer = inventoryMgr.getResourceContainer(parentResource);
                if (parentContainer == null) {
                    throw new PluginContainerException("Missing parent resource container for parent resource="
                        + parentResource);
                }
            } else if (resource.equals(inventoryMgr.getPlatform())) {
                // the given resource is our top platform resource - just use its plugin classloader
                return classLoaderMgr.obtainPluginClassLoader(resourceType.getPlugin());
            } else {
                throw new PluginContainerException("Missing parent resource for resource=" + resource);
            }

            // get the classloader the resource should use
            List<URL> additionalJars = (classLoaderMgr.isCreateResourceClassLoaders()) ?
                askDiscoveryComponentForAdditionalClasspathUrls(resource, parentContainer) :
                Collections.<URL>emptyList();
            ClassLoader cl = classLoaderMgr.obtainResourceClassLoader(resource, parentContainer, additionalJars);
            return cl;
        } catch (Throwable t) {
            throw new PluginContainerException("Failed to obtain classloader for resource: " + resource, t);
        }
    }

    private List<URL> askDiscoveryComponentForAdditionalClasspathUrls(Resource resource,
        ResourceContainer parentContainer) throws Throwable {

        List<URL> additionalJars = null;
        ResourceDiscoveryComponent discoveryComponent = getDiscoveryComponent(resource.getResourceType(),
            parentContainer);
        if (discoveryComponent != null && discoveryComponent instanceof ClassLoaderFacet) {
            InventoryManager inventoryMgr = getInventoryManager();
            additionalJars = inventoryMgr.invokeDiscoveryComponentClassLoaderFacet(resource, discoveryComponent,
                parentContainer);
        }

        return additionalJars;
    }

    /**
     * This will load the class definition of <code>className</code> the given <code>classLoader</code>.
     *
     * @param  loader the classloader where the component is to be loaded from
     * @param  className the class name of the resource component to be instantiated
     *
     * @return the new object of type <code>className</code> that was loaded via the classloader
     *
     * @throws PluginContainerException if the class could not be instantiated for some reason
     */
    private Object instantiateClass(ClassLoader loader, String className) throws PluginContainerException {

        if (log.isDebugEnabled()) {
            log.debug("Loading class [" + className + "] via classloader [" + loader + ']');
        }

        if (loader == null) {
            throw new PluginContainerException("Cannot load class [" + className + "] with null classloader");
        }

        try {
            Class<?> clazz = Class.forName(className, true, loader);
            if (log.isDebugEnabled()) {
                log.debug("Loaded class [" + clazz + "] from classloader [" + loader + ']');
            }
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new PluginContainerException("Could not instantiate plugin class [" + className
                + "] from classloader [" + loader + "]", e);
        } catch (IllegalAccessException e) {
            throw new PluginContainerException("Could not access plugin class [" + className + "] from classloader ["
                + loader + "]", e);
        } catch (ClassNotFoundException e) {
            throw new PluginContainerException("Could not find plugin class [" + className + "] from classloader ["
                + loader + "]", e);
        } catch (NullPointerException npe) {
            throw new PluginContainerException("Plugin class was 'null' using loader [" + loader + "]", npe);
        }
    }

    private InventoryManager getInventoryManager() {
        if (this.inventoryManager == null) {
            this.inventoryManager = PluginContainer.getInstance().getInventoryManager();
        }
        return this.inventoryManager;
    }

    private PluginManager getPluginManager() {
        if (this.pluginManager == null) {
            this.pluginManager = PluginContainer.getInstance().getPluginManager();
        }
        return this.pluginManager;
    }

    /**
     * Creates our (initially empty) cache of discovery components.
     *
     * @see ContainerService#initialize()
     */
    public void initialize() {
        return;
    }

    /**
     * Clears our cache of discovery components.
     *
     * @see ContainerService#shutdown()
     */
    public void shutdown() {
        this.inventoryManager = null;
        this.pluginManager = null;
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }
}