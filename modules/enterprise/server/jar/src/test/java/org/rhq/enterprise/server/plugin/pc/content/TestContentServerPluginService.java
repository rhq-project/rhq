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
package org.rhq.enterprise.server.plugin.pc.content;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.content.ContentSource;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Used as a mock service for the content source plugin container.
 */
public class TestContentServerPluginService extends ServerPluginService implements TestContentServerPluginServiceMBean {
    // public so tests can directly set these
    public Map<ContentSource, ContentProvider> testAdapters;
    public PackageSyncReport testLastSyncReport;
    public Map<ContentSource, Collection<ContentProviderPackageDetails>> testExistingPackages;
    public TestMasterServerPluginContainer master;
    public TestContentProviderManager contentProviderManager;
    public TestContentProvider testContentProvider;

    // AbstractEJB3Test
    public TestContentServerPluginService(AbstractEJB3Test testContainer) {
        super();
        testContentProvider = new TestContentProvider();
        testContainer.prepareCustomServerPluginService(this);
        this.startMasterPluginContainer();
    }

    public TestContentServerPluginService() {
        super();
    }

    public TestContentProviderManager getContentProviderManager() {
        return contentProviderManager;
    }

    @Override
    protected MasterServerPluginContainer createMasterPluginContainer() {
        this.master = new TestMasterServerPluginContainer();
        File dir = new File(System.getProperty("java.io.tmpdir"), "test-server-plugins");
        MasterServerPluginContainerConfiguration config = new MasterServerPluginContainerConfiguration(dir, dir, dir,
            null);
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
        protected ContentProviderManager createAdapterManager() {
            contentProviderManager = new TestContentProviderManager();
            return contentProviderManager;
        }

        @Override
        protected ContentServerPluginManager createPluginManager() {
            TestContentProviderPluginManager pm = new TestContentProviderPluginManager(this);
            return pm;
        }
    }

    /**
     * The test plugin manager.
     */
    class TestContentProviderPluginManager extends ContentServerPluginManager {
        public TestContentProviderPluginManager(ContentServerPluginContainer pc) {
            super(pc);
        }
    }

    /**
     * The test adapter manager.
     */
    public class TestContentProviderManager extends ContentProviderManager {

        public ContentProvider getIsolatedContentProvider(int contentProviderId) throws RuntimeException {
            return testContentProvider;
        }

        protected ContentProvider getIsolatedContentSourceAdapter(ContentSource contentSource) throws RuntimeException {
            return testContentProvider;
        }

        protected ContentProvider instantiateAdapter(ContentSource contentSource) {
            return testContentProvider;
        }
    }
}