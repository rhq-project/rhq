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

package org.rhq.enterprise.server.plugin.pc.generic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService.State;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService.TestGenericPluginManager;
import org.rhq.enterprise.server.plugin.pc.generic.TestGenericServerPluginService.TestGenericServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.generic.TestLifecycleListener.LifecycleState;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.generic.GenericPluginDescriptorType;

@Test
public class GenericServerPluginTest extends AbstractEJB3Test {
    private TestGenericServerPluginService pluginService;

    @Override
    protected void beforeMethod() throws Exception {
        this.pluginService = new TestGenericServerPluginService(getTempDir());
        deleteAllTestPluginJars(); // remove any old server plugins that might be still around
        prepareCustomServerPluginService(this.pluginService);
    }

    @Override
    protected void afterMethod() throws Exception {
        unprepareServerPluginService();
        deleteAllTestPluginJars(); // remove any server plugins that tests created
        this.pluginService = null;
    }

    public void testSimpleGenericPlugin() throws Exception {
        createPluginJar("testSimpleGenericPlugin.jar", "serverplugins/simple-generic-serverplugin.xml");
        this.pluginService.startMasterPluginContainer();

        // make sure everything is started
        TestGenericServerPluginContainer pc = this.pluginService.genericPC;
        assert pc.state == State.STARTED;
        TestGenericPluginManager pm = (TestGenericPluginManager) pc.getPluginManager();
        TestLifecycleListener component = (TestLifecycleListener) pm.components.values().iterator().next();
        assert component.state == LifecycleState.STARTED;

        // make sure the context is correct
        Configuration config = component.context.getPluginConfiguration();
        assert config != null;
        assert config.getSimple("plugin-simple-prop-1") != null;

        List<ScheduledJobDefinition> schedules = component.context.getSchedules();
        assert schedules == null;

        // make sure everything is shutdown
        this.pluginService.stopMasterPluginContainer();
        assert pc.state == State.UNINITIALIZED;
        assert component.state == LifecycleState.UNINITIALIZED;
    }

    public void testGetInstalledPlugins() throws Exception {
        File jar = createPluginJar("testSimpleGenericPlugin.jar", "serverplugins/simple-generic-serverplugin.xml");
        ServerPluginType type = new ServerPluginType(GenericPluginDescriptorType.class);

        ServerPluginDescriptorType descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(jar.toURI()
            .toURL());

        ServerPlugin plugin = new ServerPlugin(0, descriptor.getName(), jar.getName(), descriptor.getDisplayName(),
            true, PluginStatusType.INSTALLED, descriptor.getDescription(), null, MessageDigestGenerator
                .getDigestString(jar), descriptor.getVersion(), descriptor.getApiVersion(), null, null, type
                .stringify(), System.currentTimeMillis(), System.currentTimeMillis());

        ServerPluginsLocal serverPluginsLocal = LookupUtil.getServerPlugins();
        Map<ServerPluginType, List<PluginKey>> original = serverPluginsLocal.getInstalledServerPluginsGroupedByType();
        serverPluginsLocal.registerServerPlugin(LookupUtil.getSubjectManager().getOverlord(), plugin, jar);

        try {
            Map<ServerPluginType, List<PluginKey>> map = serverPluginsLocal.getInstalledServerPluginsGroupedByType();
            List<PluginKey> pluginKeys = map.get(type);

            if (original.containsKey(type)) {
                assert map.size() == original.size();
                assert pluginKeys.size() == original.get(type).size() + 1;
            } else {
                assert map.size() == original.size() + 1;
                assert pluginKeys.size() == 1;
            }
            boolean got_it = false;
            for (PluginKey pluginKey : pluginKeys) {
                if (pluginKey.getPluginName().equals("TestSimpleGenericPlugin")) {
                    got_it = true;
                    break;
                }
            }
            assert got_it == true;
        } finally {
            // make sure we clean this up, even on error
            serverPluginsLocal.purgeServerPlugin(plugin.getId());
        }

        // test that purge really deleted it
        Map<ServerPluginType, List<PluginKey>> map = serverPluginsLocal.getInstalledServerPluginsGroupedByType();
        List<PluginKey> pluginKeys = map.get(type);

        assert map.size() == original.size();
        if (pluginKeys == null) {
            assert !original.containsKey(type) : "we dont have any plugins of this type, neither should original";
        } else {
            assert pluginKeys.size() == original.get(type).size();
            boolean got_it = false;
            for (PluginKey pluginKey : pluginKeys) {
                if (pluginKey.getPluginName().equals("TestSimpleGenericPlugin")) {
                    got_it = true;
                    break;
                }
            }
            assert got_it == false : "we still have the plugin, but it should have been purged";
        }

        return;
    }

    private File createPluginJar(String jarName, String descriptorXmlFilename) throws Exception {
        FileOutputStream stream = null;
        JarOutputStream out = null;
        InputStream in = null;

        try {
            this.pluginService.masterConfig.getPluginDirectory().mkdirs();
            File jarFile = new File(this.pluginService.masterConfig.getPluginDirectory(), jarName);
            stream = new FileOutputStream(jarFile);
            out = new JarOutputStream(stream);

            // Add archive entry for the descriptor
            JarEntry jarAdd = new JarEntry("META-INF/rhq-serverplugin.xml");
            jarAdd.setTime(System.currentTimeMillis());
            out.putNextEntry(jarAdd);

            // Write the descriptor - note that we assume the xml file is in the test classloader
            in = getClass().getClassLoader().getResourceAsStream(descriptorXmlFilename);
            StreamUtil.copy(in, out, false);

            return jarFile;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }

    private void deleteAllTestPluginJars() {
        if (this.pluginService==null) {
            System.err.println("Plugin service was null !!");
            return;
        }
        MasterServerPluginContainerConfiguration masterConfig = this.pluginService.masterConfig;
        if (masterConfig==null)
            return;

        File pluginDirectory = masterConfig.getPluginDirectory();

        File[] files = pluginDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".jar")) {
                    file.delete();
                }
            }
        }
        return;
    }

}
