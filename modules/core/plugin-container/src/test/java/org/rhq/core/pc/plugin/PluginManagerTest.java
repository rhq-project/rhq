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

import org.apache.commons.io.FileUtils;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PluginManagerTest {

    static final String PLUGIN_A = "plugin-A.jar";

    static final String PLUGIN_B = "plugin-B-depends-on-A.jar";

    static final String PLUGIN_C = "plugin-C-depends-on-B.jar";

    PluginDescriptor descriptorA;

    PluginDescriptor descriptorB;

    PluginDescriptor descriptorC;

    @BeforeClass
    public void initClass() throws Exception {
        verifyPluginsExist();
        initPluginDescriptors();
    }

    void verifyPluginsExist() {
        assertPluginJarFileExists(PLUGIN_A);
        assertPluginJarFileExists(PLUGIN_B);
        assertPluginJarFileExists(PLUGIN_C);
    }

    void initPluginDescriptors() throws Exception {
        descriptorA = loadPluginDescriptor(getClass().getResource(PLUGIN_A));
        descriptorB = loadPluginDescriptor(getClass().getResource(PLUGIN_B));
        descriptorC = loadPluginDescriptor(getClass().getResource(PLUGIN_C));
    }

    void assertPluginJarFileExists(String pluginPath) {
        URL pluginURL = getClass().getResource(pluginPath);
        File pluginJarFile = FileUtils.toFile(pluginURL);

        assertTrue(pluginJarFile.exists(), pluginJarFile.getAbsolutePath() + " does not exist and is needed for " +
                "tests in " + PluginManagerTest.class.getName());
    }

    @Test
    public void initializeShouldLoadDiscoveredPlugins() throws Exception {
        PluginContainerConfiguration configuration = createConfiguration();

        PluginManager pluginMgr = new PluginManager();
        pluginMgr.setConfiguration(configuration);
        pluginMgr.initialize();

        verifyThatPluginsAreLoaded(pluginMgr);
    }

    @Test
    public void pluginsShouldBeLoadedInCorrectOrder() throws Exception {
        PluginContainerConfiguration configuration = createConfiguration();

        FakePluginLifecycleListenerManager pluginLifecycleListenerMgr = new FakePluginLifecycleListenerManager();

        PluginManager pluginMgr = new PluginManager();
        pluginMgr.setConfiguration(configuration);
        pluginMgr.setPluginLifecycleListenerManager(pluginLifecycleListenerMgr);
        pluginMgr.initialize();

        verifyPluginsLoadedInCorrectOrder(pluginLifecycleListenerMgr);
    }

    @Test
    public void pluginsShouldBeShutdownInCorrectOrder() throws Exception {
        PluginContainerConfiguration configuration = createConfiguration();

        FakePluginLifecycleListenerManager pluginLifecycleListenerMgr = new FakePluginLifecycleListenerManager();

        PluginManager pluginMgr = new PluginManager();
        pluginMgr.setConfiguration(configuration);
        pluginMgr.setPluginLifecycleListenerManager(pluginLifecycleListenerMgr);
        pluginMgr.initialize();

        pluginMgr.shutdown();

        verifyPluginsShutdownInCorrectOrder(pluginLifecycleListenerMgr);
    }

    private PluginContainerConfiguration createConfiguration() {
        PluginContainerConfiguration configuration = new PluginContainerConfiguration();
        configuration.setPluginFinder(new PluginFinder() {
            public Collection<URL> findPlugins() {
                List<URL> pluginURLs = new ArrayList<URL>();
                pluginURLs.add(getClass().getResource(PLUGIN_A));
                pluginURLs.add(getClass().getResource(PLUGIN_B));
                pluginURLs.add(getClass().getResource(PLUGIN_C));

                return pluginURLs;
            }
        });
        return configuration;
    }

    void verifyThatPluginsAreLoaded(PluginManager pluginMgr) throws Exception {
        assertPluginLoaded(descriptorA, pluginMgr);
        assertPluginLoaded(descriptorB, pluginMgr);
        assertPluginLoaded(descriptorC, pluginMgr);
    }

    void assertPluginLoaded(PluginDescriptor pluginDescriptor, PluginManager pluginMgr) throws Exception {
        PluginEnvironment pluginEnvironment = pluginMgr.getPlugin(pluginDescriptor.getName());

        assertNotNull(pluginEnvironment, "Expected the '" + pluginDescriptor.getName() + "' plugin to be loaded.");
    }

    void verifyPluginsLoadedInCorrectOrder(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        assertPluginALoadedFirst(pluginLifecycleListenerMgr);
        assertPluginBLoadedSecond(pluginLifecycleListenerMgr);
        assertPluginCLoadedThird(pluginLifecycleListenerMgr);
    }

    void assertPluginALoadedFirst(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int initOrder = pluginLifecycleListenerMgr.lifecycleTracker.getInitializationOrder(descriptorA.getName());

        assertEquals(initOrder, 0, "Expected " + descriptorA.getName() + " to be initialized first.");
    }

    void assertPluginBLoadedSecond(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int initOrder = pluginLifecycleListenerMgr.lifecycleTracker.getInitializationOrder(descriptorB.getName());

        assertEquals(initOrder, 1, "Expected " + descriptorB.getName() + " to be initialized second.");
    }

    void assertPluginCLoadedThird(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int initOrder = pluginLifecycleListenerMgr.lifecycleTracker.getInitializationOrder(descriptorC.getName());

        assertEquals(initOrder, 2, "Expected " + descriptorC.getName() + " to be initialized third.");
    }

    void verifyPluginsShutdownInCorrectOrder(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        assertPluginCShutdownFirst(pluginLifecycleListenerMgr);
        assertPluginBShutdownSecond(pluginLifecycleListenerMgr);
        assertPluginAShutdownThird(pluginLifecycleListenerMgr);
    }

    void assertPluginCShutdownFirst(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int shutdownOrder = pluginLifecycleListenerMgr.lifecycleTracker.getShutdownOrder(descriptorC.getName());

        assertEquals(shutdownOrder, 0, "Expected " + descriptorC.getName() + " to be shutdown first.");
    }

    void assertPluginBShutdownSecond(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int shutdownOrder = pluginLifecycleListenerMgr.lifecycleTracker.getShutdownOrder(descriptorB.getName());

        assertEquals(shutdownOrder, 1, "Expected " + descriptorB.getName() + " to be shutdown second.");
    }

    void assertPluginAShutdownThird(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int shutdownOrder = pluginLifecycleListenerMgr.lifecycleTracker.getShutdownOrder(descriptorA.getName());

        assertEquals(shutdownOrder, 2, "Expected " + descriptorA.getName() + " to be shutdown third.");
    }

    PluginDescriptor loadPluginDescriptor(URL pluginURL) throws PluginContainerException {
        return AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginURL);
    }

    static class FakePluginLifecycleListenerManager implements PluginLifecycleListenerManager {

        Map<String, PluginLifecycleListener> listeners = new HashMap<String, PluginLifecycleListener>();

        PluginLifecycleTracker lifecycleTracker = new PluginLifecycleTracker();

        public PluginLifecycleListener loadListener(PluginDescriptor pluginDescriptor, PluginEnvironment pluginEnvironment)
            throws PluginContainerException {

            FakePluginLifecycleListener listener = new FakePluginLifecycleListener(pluginDescriptor.getName(),
                lifecycleTracker);
            listeners.put(pluginDescriptor.getName(), listener);

            return listener;
        }

        public PluginLifecycleListener getListener(String pluginName) {
            return listeners.get(pluginName);
        }

        public void setListener(String pluginName, PluginLifecycleListener listener) {
            listeners.put(pluginName, listener);
        }

        public void shutdown() {
        }
    }

    /**
     * This is helper class that is used to track the order of start up and shutdown for the plugins used by these
     * tests.
     */
    static class PluginLifecycleTracker {
        private List<String> initializedPlugins = new LinkedList<String>();

        private List<String> shutdownPlugins = new LinkedList<String>();

        public void initialize(String plugin) {
            initializedPlugins.add(plugin);
        }

        public void shutdown(String plugin) {
            shutdownPlugins.add(plugin);
        }

        public int getInitializationOrder(String plugin) {
            return initializedPlugins.indexOf(plugin);
        }

        public int getShutdownOrder(String plugin) {
            return shutdownPlugins.indexOf(plugin);
        }
    }

    static class FakePluginLifecycleListener implements PluginLifecycleListener {
        String pluginName;

        PluginLifecycleTracker lifecycleTracker;

        public FakePluginLifecycleListener(String pluginName, PluginLifecycleTracker lifecycleTracker) {
            this.pluginName = pluginName;
            this.lifecycleTracker = lifecycleTracker;
        }

        public void initialize(PluginContext context) throws Exception {
            lifecycleTracker.initialize(pluginName);
        }

        public void shutdown() {
            lifecycleTracker.shutdown(pluginName);
        }
    }

}
