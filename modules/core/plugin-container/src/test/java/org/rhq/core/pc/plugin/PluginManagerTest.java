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

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;
import org.rhq.core.pluginapi.plugin.PluginContext;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.io.File;

public class PluginManagerTest {

    static final String PLATFORM_PLUGIN = "org/rhq/rhq-platform-plugin/1.4.0-SNAPSHOT/rhq-platform-plugin-1.4.0-SNAPSHOT.jar";

    static final String HOSTS_PLUGIN = "org/rhq/rhq-hosts-plugin/1.4.0-SNAPSHOT/rhq-hosts-plugin-1.4.0-SNAPSHOT.jar";

    static final String AUGEAS_PLUGIN = "org/rhq/rhq-augeas-plugin/1.4.0-SNAPSHOT/rhq-augeas-plugin-1.4.0-SNAPSHOT.jar";

    String m2RepoDir;

    PluginDescriptor platformDescriptor;

    PluginDescriptor hostsDescriptor;

    PluginDescriptor augeasDescriptor;

    @BeforeClass
    public void initClass() throws Exception {
        setM2RepoDir();
        verifyPluginsExist();
        initPluginDescriptors();
    }

    void setM2RepoDir() {
        String userHomeDir = System.getProperty("user.home");
        m2RepoDir = System.getProperty("m2.repo", userHomeDir + "/.m2/repository");
    }

    void verifyPluginsExist() {
        assertPluginJarFileExists(PLATFORM_PLUGIN);
        assertPluginJarFileExists(HOSTS_PLUGIN);
        assertPluginJarFileExists(AUGEAS_PLUGIN);
    }

    void initPluginDescriptors() throws Exception {
        platformDescriptor = loadPluginDescriptor(getPlatformPluginURL());
        augeasDescriptor = loadPluginDescriptor(getAugeasPluginURL());
        hostsDescriptor = loadPluginDescriptor(getHostsPluginURL());
    }

    void assertPluginJarFileExists(String pluginPath) {
        File pluginJarFile = new File(m2RepoDir, pluginPath);
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
                pluginURLs.add(getPlatformPluginURL());
                pluginURLs.add(getHostsPluginURL());
                pluginURLs.add(getAugeasPluginURL());

                return pluginURLs;
            }
        });
        return configuration;
    }

    void verifyThatPluginsAreLoaded(PluginManager pluginMgr) throws Exception {
        assertPluginLoaded(platformDescriptor, pluginMgr);
        assertPluginLoaded(hostsDescriptor, pluginMgr);
        assertPluginLoaded(augeasDescriptor, pluginMgr);
    }

    void assertPluginLoaded(PluginDescriptor pluginDescriptor, PluginManager pluginMgr) throws Exception {
        PluginEnvironment pluginEnvironment = pluginMgr.getPlugin(pluginDescriptor.getName());

        assertNotNull(pluginEnvironment, "Expected the '" + pluginDescriptor.getName() + "' plugin to be loaded.");
    }

    void verifyPluginsLoadedInCorrectOrder(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        assertPlatformPluginLoadedFirst(pluginLifecycleListenerMgr);
        assertAugeasPluginLoadedSecond(pluginLifecycleListenerMgr);
        assertHostsPluginLoadedThird(pluginLifecycleListenerMgr);
    }

    void assertPlatformPluginLoadedFirst(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int initOrder = pluginLifecycleListenerMgr.lifecycleTracker.getInitializationOrder(platformDescriptor.getName());

        assertEquals(initOrder, 0, "Expected " + platformDescriptor.getName() + " to be initialized first.");
    }

    void assertAugeasPluginLoadedSecond(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int initOrder = pluginLifecycleListenerMgr.lifecycleTracker.getInitializationOrder(augeasDescriptor.getName());

        assertEquals(initOrder, 1, "Expected " + augeasDescriptor.getName() + " to be initialized second.");
    }

    void assertHostsPluginLoadedThird(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int initOrder = pluginLifecycleListenerMgr.lifecycleTracker.getInitializationOrder(hostsDescriptor.getName());

        assertEquals(initOrder, 2, "Expected " + hostsDescriptor.getName() + " to be initialized third.");
    }

    void verifyPluginsShutdownInCorrectOrder(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        assertHostsPluginShutdownFirst(pluginLifecycleListenerMgr);
        assertAugeasPluginShutdownSecond(pluginLifecycleListenerMgr);
        assertPlatformPluginShutdownThird(pluginLifecycleListenerMgr);
    }

    void assertHostsPluginShutdownFirst(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int shutdownOrder = pluginLifecycleListenerMgr.lifecycleTracker.getShutdownOrder(hostsDescriptor.getName());

        assertEquals(shutdownOrder, 0, "Expected " + hostsDescriptor.getName() + " to be shutdown first.");
    }

    void assertAugeasPluginShutdownSecond(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int shutdownOrder = pluginLifecycleListenerMgr.lifecycleTracker.getShutdownOrder(augeasDescriptor.getName());

        assertEquals(shutdownOrder, 1, "Expected " + augeasDescriptor.getName() + " to be shutdown second.");
    }

    void assertPlatformPluginShutdownThird(FakePluginLifecycleListenerManager pluginLifecycleListenerMgr) throws Exception {
        int shutdownOrder = pluginLifecycleListenerMgr.lifecycleTracker.getShutdownOrder(platformDescriptor.getName());

        assertEquals(shutdownOrder, 2, "Expected " + platformDescriptor.getName() + " to be shutdown third.");
    }

    PluginDescriptor loadPluginDescriptor(URL pluginURL) throws PluginContainerException {
        return AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginURL);
    }

    String getPluginName(URL pluginURL) throws Exception {
        PluginDescriptor descriptor = loadPluginDescriptor(pluginURL);
        return descriptor.getName();
    }

    URL getPlatformPluginURL() {
        return toURL(new File(m2RepoDir, PLATFORM_PLUGIN));
    }

    URL getHostsPluginURL() {
        return toURL(new File(m2RepoDir, HOSTS_PLUGIN));
    }

    URL getAugeasPluginURL() {
        return toURL(new File(m2RepoDir, AUGEAS_PLUGIN));
    }

    URL toURL(File file) {
        try {
            return file.toURI().toURL();
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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
