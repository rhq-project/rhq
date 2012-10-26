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
package org.rhq.enterprise.server.bundle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.updater.DeploymentProperties;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.content.PackageTypeServerPluginContainer;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Used as a mock service for the bundle server plugin container.
 */
public class TestBundleServerPluginService extends ServerPluginService implements TestBundleServerPluginServiceMBean {

    // public so tests can directly set these
    public TestMasterServerPluginContainer master;
    public TestBundleServerPluginContainer bundlePC;
    public MasterServerPluginContainerConfiguration masterConfig;

    public RecipeParseResults parseRecipe_returnValue = null;
    public BundleDistributionInfo processBundleDistributionFile_returnValue;

    public TestBundleServerPluginService() {
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

    /**
     * The test master PC
     */
    class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        @Override
        protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
            ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(1);
            bundlePC = new TestBundleServerPluginContainer(this);
            pcs.add(bundlePC);
            pcs.add(new TestContentServerPluginContainer(this));
            
            //needed internally by the server, so let's provide the standard impl.
            pcs.add(new PackageTypeServerPluginContainer(this));
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

    public class TestBundleServerPluginContainer extends BundleServerPluginContainer {
        public TestBundleServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        @Override
        protected ServerPluginManager createPluginManager() {
            TestBundlePluginManager pm = new TestBundlePluginManager(this);
            return pm;
        }
    }

    public class TestContentServerPluginContainer extends ContentServerPluginContainer {
        public TestContentServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        @Override
        protected ServerPluginManager createPluginManager() {
            ContentServerPluginManager pm = new ContentServerPluginManager(this);
            return pm;
        }
    }

    /**
     * The test plugin manager.
     */
    class TestBundlePluginManager extends BundleServerPluginManager {
        public final Map<String, ServerPluginComponent> components;

        public TestBundlePluginManager(TestBundleServerPluginContainer pc) {
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

        @Override
        protected ServerPluginComponent createServerPluginComponent(ServerPluginEnvironment environment)
            throws Exception {
            ServerPluginComponent component = super.createServerPluginComponent(environment);
            components.put(environment.getPluginKey().getPluginName(), component);
            return component;
        }

        @Override
        public RecipeParseResults parseRecipe(String bundleTypeName, String recipe) throws Exception {
            return new TestBundlePluginComponent().parseRecipe(recipe);
        }

        @Override
        public BundleDistributionInfo processBundleDistributionFile(File distributionFile) throws Exception {
            return new TestBundlePluginComponent().processBundleDistributionFile(distributionFile);
        }
    }

    class TestBundlePluginComponent implements BundleServerPluginFacet {

        public TestBundlePluginComponent() {
        };

        public RecipeParseResults parseRecipe(String recipe) throws Exception {

            if (parseRecipe_returnValue != null) {
                return parseRecipe_returnValue;
            }

            ConfigurationDefinition configDef;
            Set<String> bundleFileNames;
            DeploymentProperties metadata;

            metadata = new DeploymentProperties(0, "bundletest", "1.0", "bundle test description");

            configDef = new ConfigurationDefinition("bundletest-configdef", "Test Config Def for testing BundleVersion");
            configDef.put(new PropertyDefinitionSimple("bundletest.property",
                "Test property for BundleVersion Config Def testing", true, PropertySimpleType.STRING));

            bundleFileNames = new HashSet<String>();
            bundleFileNames.add("bundletest-bundlefile-1");
            bundleFileNames.add("bundletest-bundlefile-2");

            return new RecipeParseResults(metadata, configDef, bundleFileNames);
        }

        public BundleDistributionInfo processBundleDistributionFile(File uberBundleFile) throws Exception {
            if (processBundleDistributionFile_returnValue != null) {
                return processBundleDistributionFile_returnValue;
            }

            throw new UnsupportedOperationException("this mock object cannot do this");
        }
    }
}