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
import org.testng.annotations.BeforeMethod;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.agent.PluginContainerException;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class PluginManagerTest {

    static final String PLATFORM_PLUGIN = "org/rhq/rhq-platform-plugin/1.4.0-SNAPSHOT/rhq-platform-plugin-1.4.0-SNAPSHOT.jar";

    static final String HOSTS_PLUGIN = "org/rhq/rhq-hosts-plugin/1.4.0-SNAPSHOT/rhq-hosts-plugin-1.4.0-SNAPSHOT.jar";

    static final String AUGEAS_PLUGIN = "org/rhq/rhq-augeas-plugin/1.4.0-SNAPSHOT/rhq-augeas-plugin-1.4.0-SNAPSHOT.jar";

    String m2RepoDir;

    @BeforeClass
    public void initClass() {
        initM2RepoDir();
        verifyPluginsExist();
    }

    void initM2RepoDir() {
        String userHomeDir = System.getProperty("user.home");
        m2RepoDir = System.getProperty("m2.repo", userHomeDir + "/.m2/repository");
    }

    void verifyPluginsExist() {
        assertPluginJarFileExists(PLATFORM_PLUGIN);
        assertPluginJarFileExists(HOSTS_PLUGIN);
        assertPluginJarFileExists(AUGEAS_PLUGIN);
    }

    void assertPluginJarFileExists(String pluginPath) {
        File pluginJarFile = new File(m2RepoDir, pluginPath);
        assertTrue(pluginJarFile.exists(), pluginJarFile.getAbsolutePath() + " does not exist and is needed for " +
                "tests in " + PluginManagerTest.class.getName());
    }

    @Test
    public void initializeShouldLoadDiscoveredPlugins() throws Exception {
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

        PluginManager pluginMgr = new PluginManager();
        pluginMgr.setConfiguration(configuration);
        pluginMgr.initialize();

        verifyThatPluginsAreLoaded(pluginMgr);
    }

    void verifyThatPluginsAreLoaded(PluginManager pluginMgr) throws Exception {
        assertPluginLoaded(getPlatformPluginURL(), pluginMgr);
        assertPluginLoaded(getHostsPluginURL(), pluginMgr);
        assertPluginLoaded(getAugeasPluginURL(), pluginMgr);
    }

    void assertPluginLoaded(URL pluginURL, PluginManager pluginMgr) throws Exception {
        PluginDescriptor pluginDescriptor = loadPluginDescriptor(pluginURL);
        PluginEnvironment pluginEnvironment = pluginMgr.getPlugin(pluginDescriptor.getName());

        assertNotNull(pluginEnvironment, "Expected the '" + pluginDescriptor.getName() + "' plugin to be loaded.");
    }

    PluginDescriptor loadPluginDescriptor(URL pluginURL) throws PluginContainerException {
        return AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginURL);
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

}
