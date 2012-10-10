/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.enterprise.server;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * 
 *
 * @author Lukas Krejci
 */
public abstract class TestServerPluginService extends ServerPluginService implements TestServerPluginServiceMBean {
    public TestMasterServerPluginContainer master;
    public MasterServerPluginContainerConfiguration masterConfig;

    protected TestServerPluginService() {
        // build the config at constructor time so tests have it even before the PC is initialized
        File dir = new File(System.getProperty("java.io.tmpdir"), "test-server-plugins");
        this.masterConfig = new MasterServerPluginContainerConfiguration(dir, dir, dir, null);
    }

    @Override
    public MasterServerPluginContainer createMasterPluginContainer() {
        this.master = new TestMasterServerPluginContainer();
        this.master.initialize(this.masterConfig);
        return this.master;
    }

    protected abstract List<AbstractTypeServerPluginContainer> createPluginContainers(MasterServerPluginContainer master);
    
    public static ServerPlugin getPlugin(ServerPluginEnvironment env) {
        return getPlugin(env.getPluginUrl(), env.getPluginDescriptor());
    }
    
    public static ServerPlugin getPlugin(URL pluginUrl, ServerPluginDescriptorType pluginDescriptor) {
        try {
            Configuration pluginConfig = null;
            Configuration scheduledJobsConfig = null;
            ConfigurationDefinition configDef;

            configDef = ServerPluginDescriptorMetadataParser.getPluginConfigurationDefinition(pluginDescriptor);
            if (configDef != null) {
                pluginConfig = configDef.getDefaultTemplate().createConfiguration();
            }

            configDef = ServerPluginDescriptorMetadataParser.getScheduledJobsDefinition(pluginDescriptor);
            if (configDef != null) {
                scheduledJobsConfig = configDef.getDefaultTemplate().createConfiguration();
            }

            File pluginFile = new File(pluginUrl.toURI());
            PluginKey pluginKey = PluginKey.createServerPluginKey(new ServerPluginType(pluginDescriptor).stringify(), pluginDescriptor.getName());
            ServerPlugin plugin =
                new ServerPlugin(0, pluginKey.getPluginName(), pluginFile.getName(),
                    pluginDescriptor.getDisplayName(), true, PluginStatusType.INSTALLED,
                    pluginDescriptor.getDescription(), "", MessageDigestGenerator.getDigestString(pluginFile),
                    pluginDescriptor.getVersion(), pluginDescriptor.getVersion(), pluginConfig,
                    scheduledJobsConfig, new ServerPluginType(pluginDescriptor).stringify(),
                    System.currentTimeMillis(), System.currentTimeMillis());
            return plugin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static ServerPlugin getPlugin(URL pluginUrl) throws Exception {
        ServerPluginDescriptorType type = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
        
        return getPlugin(pluginUrl, type);
    }
    
    protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
        return null;
    }
    
    private class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        
        @Override
        protected ClassLoader createRootServerPluginClassLoader() {
            return getClass().getClassLoader();
        }

        @Override
        protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
            Map<URL, ? extends ServerPluginDescriptorType> plugins = TestServerPluginService.this.preloadAllPlugins();
            if (plugins != null) {
                return plugins;
            }
            
            // if our test never setup any plugins, ignore it and just return an empty map
            File pluginDir = getConfiguration().getPluginDirectory();
            if (pluginDir == null || pluginDir.listFiles() == null || pluginDir.listFiles().length == 0) {
                return new HashMap<URL, ServerPluginDescriptorType>();
            } else {
                return super.preloadAllPlugins();
            }
        }

        @Override
        protected List<PluginKey> getDisabledPluginKeys() {
            // in the real world, the db is checked for enable flag, here we say all plugins are enabled
            return new ArrayList<PluginKey>();
        }

        @Override
        protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
            return TestServerPluginService.this.createPluginContainers(this);
        }
    }
}
