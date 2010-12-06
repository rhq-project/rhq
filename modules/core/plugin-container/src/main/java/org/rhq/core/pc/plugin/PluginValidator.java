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
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;

/**
 * A utility to test a set of plugins are valid.
 *
 * @author John Mazzitelli
 */
public class PluginValidator {
    private static final Log LOG = LogFactory.getLog(PluginValidator.class);
    private static final String PLUGIN_DESCRIPTOR_PATH = "META-INF/rhq-plugin.xml";

    /**
     * If no args are passed in, the current thread's classloader will be used to find the plugins to validate.
     * If one or more argument strings are provided, they will be assumed to be paths to the plugin jars to validate
     * (in which case the thread's classloader will be ignored and not searched for plugins).
     *
     * The last line this will output will be "!OK!" and exit with an exit code of 0 if everything is OK.
     * The last line this will output will be "!FAILURE!" and exit with an exit code of 1 if one or more plugins failed validation.
     * 
     * @param args 0 or more plugin jar file paths
     */
    public static void main(String[] args) {
        SimplePluginFinder finder;

        try {
            if (args.length > 0) {
                finder = new SimplePluginFinder();
                for (String arg : args) {
                    URL jarUrl = new File(arg).toURI().toURL();
                    finder.addUrl(jarUrl);
                    LOG.info("Plugin jar: " + jarUrl);
                }
            } else {
                finder = findPluginJars();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (validatePlugins(finder)) {
            System.out.println("!OK!");
            System.exit(0);
        } else {
            System.out.println("!FAILED!");
            System.exit(1);
        }
    }

    public static boolean validatePlugins(PluginFinder finder) {
        PluginContainerConfiguration configuration = new PluginContainerConfiguration();
        configuration.setPluginFinder(finder);
        configuration.setTemporaryDirectory(new File(System.getProperty("java.io.tmpdir")));
        configuration.setDataDirectory(new File(System.getProperty("java.io.tmpdir")));

        PluginManager manager = new PluginManager();
        manager.setConfiguration(configuration);
        manager.initialize();

        boolean success = true; // assume all goes well; we'll set this to false if we hit any error

        try {
            // make sure we successfully processed all the plugins that are in our finder
            boolean sizesMatch = (manager.getPlugins().size() == finder.findPlugins().size());
            if (!sizesMatch) {
                success = false;
                LOG.error("Only [" + manager.getPlugins().size() + "] out of [" + finder.findPlugins().size()
                    + "] plugin descriptors are valid.");
            } else {
                LOG.info("All [" + finder.findPlugins().size() + "] plugin descriptors are valid.");
            }

            PluginMetadataManager metadataManager = manager.getMetadataManager();

            // examine all the resource types defined in all plugins and validate some things about them
            for (ResourceType resourceType : metadataManager.getAllTypes()) {
                PluginEnvironment pluginEnvironment = manager.getPlugin(resourceType.getPlugin());

                LOG.info("Validating resource type [" + resourceType.getName() + "] from plugin ["
                    + resourceType.getPlugin() + "]...");

                // make sure the component class was specified and can be loaded by the plugin classloader
                success = success && validateResourceComponentClass(metadataManager, resourceType, pluginEnvironment);

                // if the optional discovery class was specified, make sure it can be loaded by the plugin classloader
                success = success
                    && validateResourceDiscoveryComponentClass(metadataManager, resourceType, pluginEnvironment);

                success = success
                    && validatePluginLifecycleListenerClass(metadataManager, resourceType, pluginEnvironment);
            }
        } finally {
            manager.shutdown();
        }

        return success;
    }

    private static boolean validateResourceComponentClass(PluginMetadataManager metadataManager,
        ResourceType resourceType, PluginEnvironment pluginEnvironment) {
        boolean success = true;
        String componentClass = metadataManager.getComponentClass(resourceType);
        if (componentClass == null) {
            success = false;
            LOG.error("Missing component class in resource type [" + resourceType.getName() + "] from plugin ["
                + resourceType.getPlugin() + "]");
        } else {
            try {
                Class componentClazz = Class.forName(componentClass, false, pluginEnvironment.getPluginClassLoader());
                if (!ResourceComponent.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin() + "] does not implement "
                        + ResourceComponent.class);
                }
                if (!resourceType.getMetricDefinitions().isEmpty()
                    && !MeasurementFacet.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin()
                        + "] does not support the measurement collection facet but defines metrics.");
                }
                if (!resourceType.getOperationDefinitions().isEmpty()
                    && !OperationFacet.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin()
                        + "] does not support the operations facet but defines operations.");
                }
                if (resourceType.getBundleType() != null && !BundleFacet.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin()
                        + "] does not support the bundle facet but defines a bundle type.");
                }
                if (!resourceType.getPackageTypes().isEmpty() && !ContentFacet.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin()
                        + "] does not support the content management facet but defines package types.");
                }
                if (resourceType.getResourceConfigurationDefinition() != null
                    && !ConfigurationFacet.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin()
                        + "] does not support the configuration facet but defines resource configuration.");
                }
                boolean hasCreatableChild = false;
                for (ResourceType childResourceType : resourceType.getChildResourceTypes()) {
                    if (childResourceType.isCreatable()) {
                        hasCreatableChild = true;
                        break;
                    }
                }
                if (hasCreatableChild && !CreateChildResourceFacet.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin()
                        + "] does not support the child creation facet but has metadata saying it can.");
                }
                if (resourceType.isDeletable() && !DeleteResourceFacet.class.isAssignableFrom(componentClazz)) {
                    success = false;
                    LOG.error("Component class [" + componentClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin()
                        + "] does not support delete resource facet but has metadata saying it can delete children.");
                }
            } catch (Exception e) {
                success = false;
                LOG.error("Cannot find component class [" + componentClass + "] for resource type ["
                    + resourceType.getName() + "] from plugin [" + resourceType.getPlugin() + "].");
            }
        }
        return success;
    }

    private static boolean validateResourceDiscoveryComponentClass(PluginMetadataManager metadataManager,
        ResourceType resourceType, PluginEnvironment pluginEnvironment) {
        boolean success = true;
        String discoveryClass = metadataManager.getDiscoveryClass(resourceType);
        if (discoveryClass != null) {
            try {
                Class discoveryClazz = Class.forName(discoveryClass, false, pluginEnvironment.getPluginClassLoader());
                if (!ResourceDiscoveryComponent.class.isAssignableFrom(discoveryClazz)) {
                    success = false;
                    LOG.error("Discovery class [" + discoveryClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin() + "] does not implement "
                        + ResourceDiscoveryComponent.class);
                }
                if (resourceType.isSupportsManualAdd() && !ManualAddFacet.class.isAssignableFrom(discoveryClazz)) {
                    LOG.warn("Discovery class [" + discoveryClass + "] for resource type [" + resourceType.getName()
                        + "] from plugin [" + resourceType.getPlugin() + "] does not implement " + ManualAddFacet.class
                        + " - implementing manual-add in discoverResources() is deprecated.");
                }
            } catch (Exception e) {
                success = false;
                LOG.error("Cannot find discovery class [" + discoveryClass + "] for resource type ["
                    + resourceType.getName() + "] from plugin [" + resourceType.getPlugin() + "].");
            }
        }
        return success;
    }

    private static boolean validatePluginLifecycleListenerClass(PluginMetadataManager metadataManager,
        ResourceType resourceType, PluginEnvironment pluginEnvironment) {
        boolean success = true;
        String overseerClass = metadataManager.getPluginLifecycleListenerClass(resourceType.getPlugin());
        if (overseerClass != null) {
            try {
                Class overseerClazz = Class.forName(overseerClass, false, pluginEnvironment.getPluginClassLoader());
                if (!PluginLifecycleListener.class.isAssignableFrom(overseerClazz)) {
                    success = false;
                    LOG.error("Plugin Lifecycle Listener class [" + overseerClass + "] for plugin ["
                        + resourceType.getPlugin() + "] does not implement " + PluginLifecycleListener.class);
                }
            } catch (Exception e) {
                success = false;
                LOG.error("Cannot find Plugin Lifecycle Listener class [" + overseerClass + "] for plugin ["
                    + resourceType.getPlugin() + "].");
            }
        }
        return success;
    }

    private static SimplePluginFinder findPluginJars() throws Exception {
        SimplePluginFinder pluginFinder = new SimplePluginFinder();
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> descriptorUrls = classloader.getResources(PLUGIN_DESCRIPTOR_PATH);
        while (descriptorUrls.hasMoreElements()) {
            URL descriptorUrl = descriptorUrls.nextElement();
            URLConnection connection = descriptorUrl.openConnection();
            if (connection instanceof JarURLConnection) {
                URL jarUrl = ((JarURLConnection) connection).getJarFileURL();
                pluginFinder.addUrl(jarUrl);
                LOG.info("Found plugin jar: " + jarUrl);
            } else {
                LOG.warn("Found a plugin descriptor outside of a jar, skipping: " + descriptorUrl);
            }
        }

        return pluginFinder;
    }
}