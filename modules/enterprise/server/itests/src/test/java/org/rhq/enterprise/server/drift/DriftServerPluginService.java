/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.drift;

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
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginManager;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

public class DriftServerPluginService extends ServerPluginService implements DriftServerPluginServiceMBean {

    public TestMasterServerPluginContainer master;
    public MasterServerPluginContainerConfiguration masterConfig;

    public DriftServerPluginService() {
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

    class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        @Override
        protected ClassLoader createRootServerPluginClassLoader() {
            return getClass().getClassLoader();
        }

        @Override
        protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
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
            ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(1);
            DriftServerPluginContainer driftPC = new TestDriftServerPluginContainer(this);
            pcs.add(driftPC);

            return pcs;
        }
    }

    class TestDriftServerPluginContainer extends DriftServerPluginContainer {
        public TestDriftServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        @Override
    protected ServerPluginManager createPluginManager() {
        return new TestDriftServerPluginManager(this);
    }
    }

    class TestDriftServerPluginManager extends DriftServerPluginManager {
        public TestDriftServerPluginManager(DriftServerPluginContainer pc) {
            super(pc);
        }

        @Override
        protected ServerPlugin getPlugin(ServerPluginEnvironment env) {
            try {
                Configuration pluginConfig = null;
                Configuration scheduledJobsConfig = null;
                ConfigurationDefinition configDef;

                ServerPluginDescriptorType pluginDescriptor = env.getPluginDescriptor();

                configDef = ServerPluginDescriptorMetadataParser.getPluginConfigurationDefinition(pluginDescriptor);
                if (configDef != null) {
                    pluginConfig = configDef.getDefaultTemplate().createConfiguration();
                }

                configDef = ServerPluginDescriptorMetadataParser.getScheduledJobsDefinition(pluginDescriptor);
                if (configDef != null) {
                    scheduledJobsConfig = configDef.getDefaultTemplate().createConfiguration();
                }

                File pluginFile = new File(env.getPluginUrl().toURI());
                ServerPlugin plugin = new ServerPlugin(0, env.getPluginKey().getPluginName(), pluginFile.getName(),
                    pluginDescriptor.getDisplayName(), true, PluginStatusType.INSTALLED, pluginDescriptor
                        .getDescription(), "", MessageDigestGenerator.getDigestString(pluginFile), pluginDescriptor
                        .getVersion(), pluginDescriptor.getVersion(), pluginConfig, scheduledJobsConfig,
                    new ServerPluginType(pluginDescriptor).stringify(), System.currentTimeMillis(), System
                        .currentTimeMillis());
                return plugin;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
