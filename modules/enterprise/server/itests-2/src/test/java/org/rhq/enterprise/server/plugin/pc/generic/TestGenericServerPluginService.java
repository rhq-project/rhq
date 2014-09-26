/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Used as a mock service for the generic server plugin container.
 */
public class TestGenericServerPluginService extends ServerPluginService implements TestGenericServerPluginServiceMBean {
    public enum State {
        INITIALIZED, STARTED, STOPPED, UNINITIALIZED
    }

    // public so tests can directly set these
    public TestMasterServerPluginContainer master;
    public TestGenericServerPluginContainer genericPC;
    public MasterServerPluginContainerConfiguration masterConfig;

    public TestGenericServerPluginService(File tmpdir) {
        // build the config at constructor time so tests have it even before the PC is initialized
        this.masterConfig = new MasterServerPluginContainerConfiguration(tmpdir, tmpdir, tmpdir, null);
    }

    @Override
    public MasterServerPluginContainer createMasterPluginContainer() {
        this.master = new TestMasterServerPluginContainer();
        this.master.initialize(this.masterConfig);
        return this.master;
    }

    @Override
    public File getServerPluginsDirectory() {
        return masterConfig.getPluginDirectory();
    }

    /**
     * The test master PC
     */
    class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        @Override
        protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
            ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(1);
            genericPC = new TestGenericServerPluginContainer(this);
            pcs.add(genericPC);
            return pcs;
        }

        @Override
        protected ClassLoader createRootServerPluginClassLoader() {
            return this.getClass().getClassLoader();
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
    }

    /**
     * The test generic PC.
     */
    class TestGenericServerPluginContainer extends GenericServerPluginContainer {
        public State state = State.UNINITIALIZED;

        public TestGenericServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        @Override
        protected ServerPluginManager createPluginManager() {
            TestGenericPluginManager pm = new TestGenericPluginManager(this);
            return pm;
        }

        @Override
        public synchronized void initialize() throws Exception {
            if (state == State.UNINITIALIZED) {
                state = State.INITIALIZED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not uninitialized: " + state);
            }
            super.initialize();
        }

        @Override
        public synchronized void start() {
            if (state == State.INITIALIZED) {
                state = State.STARTED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not initialized: " + state);
            }
            super.start();
        }

        @Override
        public synchronized void stop() {
            if (state == State.STARTED) {
                state = State.STOPPED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not started: " + state);
            }
            super.stop();
        }

        @Override
        public synchronized void shutdown() {
            if (state == State.STOPPED) {
                state = State.UNINITIALIZED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not stopped: " + state);
            }
            super.shutdown();
        }
    }

    /**
     * The test plugin manager.
     */
    class TestGenericPluginManager extends ServerPluginManager {
        public final Map<String, ServerPluginComponent> components;

        public TestGenericPluginManager(TestGenericServerPluginContainer pc) {
            super(pc);
            components = new HashMap<String, ServerPluginComponent>();
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
                    pluginDescriptor.getDisplayName(), true, PluginStatusType.INSTALLED,
                    pluginDescriptor.getDescription(), "", MessageDigestGenerator.getDigestString(pluginFile),
                    pluginDescriptor.getVersion(), pluginDescriptor.getVersion(), pluginConfig, scheduledJobsConfig,
                    new ServerPluginType(pluginDescriptor).stringify(), System.currentTimeMillis(),
                    System.currentTimeMillis());
                return plugin;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected ServerPluginComponent createServerPluginComponent(ServerPluginEnvironment environment)
            throws Exception {
            ServerPluginComponent component = super.createServerPluginComponent(environment);
            components.put(environment.getPluginKey().getPluginName(), component);
            return component;
        }
    }
}