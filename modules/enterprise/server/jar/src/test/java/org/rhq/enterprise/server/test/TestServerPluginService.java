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
package org.rhq.enterprise.server.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.content.ContentSource;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.content.ContentProvider;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderManager;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPluginManager;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.PackageSource;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Used as a mock service for the content source plugin container.
 */
public class TestServerPluginService extends ServerPluginService implements TestServerPluginServiceMBean {
    // public so tests can directly set these
    public Map<ContentSource, ContentProvider> testAdapters;
    public PackageSyncReport testLastSyncReport;
    public Map<ContentSource, Collection<ContentProviderPackageDetails>> testExistingPackages;
    public TestMasterServerPluginContainer master;

    @Override
    protected MasterServerPluginContainer createMasterPluginContainer() {
        this.master = new TestMasterServerPluginContainer();
        MasterServerPluginContainerConfiguration config = new MasterServerPluginContainerConfiguration();
        this.master.initialize(config);
        return this.master;
    }

    /**
     * The test master PC
     */
    class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        @Override
        protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
            ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(1);
            pcs.add(new TestContentServerPluginContainer(this));
            return pcs;
        }

        @Override
        protected ClassLoader createRootServerPluginClassLoader() {
            return this.getClass().getClassLoader();
        }

        @Override
        protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
            return new HashMap<URL, ServerPluginDescriptorType>();
        }
    }

    /**
     * The test content PC.
     */
    class TestContentServerPluginContainer extends ContentServerPluginContainer {
        public TestContentServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        @Override
        protected ContentProviderManager createAdapterManager(ContentProviderPluginManager pluginManager) {
            TestContentProviderManager am = new TestContentProviderManager();
            am.initialize(pluginManager);
            return am;
        }

        @Override
        protected ContentProviderPluginManager createPluginManager() {
            TestContentProviderPluginManager pm = new TestContentProviderPluginManager(this);
            return pm;
        }
    }

    /**
     * The test plugin manager.
     */
    class TestContentProviderPluginManager extends ContentProviderPluginManager {
        public TestContentProviderPluginManager(ContentServerPluginContainer pc) {
            super(pc);
        }
    }

    /**
     * The test adapter manager.
     */
    class TestContentProviderManager extends ContentProviderManager {
        @Override
        public Set<ContentSource> getAllContentSources() {
            return (testAdapters != null) ? testAdapters.keySet() : new HashSet<ContentSource>();
        }

        @Override
        protected void initialize(ContentProviderPluginManager pluginManager) {
            createInitialAdaptersMap();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public InputStream loadPackageBits(int contentSourceId, String location) throws Exception {
            if (testAdapters != null) {
                for (ContentSource cs : testAdapters.keySet()) {
                    if (cs.getId() == contentSourceId) {
                        PackageSource packageSource = (PackageSource) testAdapters.get(cs);
                        return packageSource.getInputStream(location);
                    }
                }
            }

            System.out.println("!!!!!!!!!!");
            System.out.println("STREAM: TEST DID NOT SETUP ADAPTER - EMPTY STREAM FOR [" + contentSourceId + "]");
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public boolean synchronizeContentSource(int contentSourceId) throws Exception {
            if (testAdapters != null) {
                for (ContentSource cs : testAdapters.keySet()) {
                    if (cs.getId() == contentSourceId) {
                        testLastSyncReport = new PackageSyncReport();
                        PackageSource packageSource = (PackageSource) testAdapters.get(cs);
                        packageSource.synchronizePackages(testLastSyncReport, testExistingPackages.get(cs));
                        return true;
                    }
                }
            }

            System.out.println("!!!!!!!!!!");
            System.out.println("SYNC: TEST DID NOT SETUP ADAPTER - NO-OP FOR [" + contentSourceId + "]");
            return true;
        }

        @Override
        public boolean testConnection(int contentSourceId) throws Exception {
            if (testAdapters != null) {
                for (ContentSource cs : testAdapters.keySet()) {
                    if (cs.getId() == contentSourceId) {
                        testAdapters.get(cs).testConnection();
                        return true;
                    }
                }
            }

            System.out.println("!!!!!!!!!!");
            System.out.println("CONN: TEST DID NOT SETUP ADAPTER - NO-OP FOR [" + contentSourceId + "]");
            return true;
        }
    }
}