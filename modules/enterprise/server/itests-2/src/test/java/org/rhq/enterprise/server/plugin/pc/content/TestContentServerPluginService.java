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

    private TestContentProviderManager contentProviderManager;

    private Map<Integer, ContentProvider> providers = new HashMap<Integer, ContentProvider>();
    private TestContentProvider defaultTestContentProvider;

    /**
     * Creates and initializes the content server plugin container for use with the given test case.
     *
     * @param testContainer cannot be <code>null</code>
     */
    public TestContentServerPluginService(AbstractEJB3Test testContainer) {
        super();
        testContainer.prepareCustomServerPluginService(this);
        startMasterPluginContainer();

        defaultTestContentProvider = new TestContentProvider();
    }

    /**
     * Returns a subclass of {@link ContentProviderManager} that overrides the calls to retrieve a content
     * provider, instead returning mock instances as configured through calls to
     * {@link #associateContentProvider(ContentSource, ContentProvider)}.
     *
     * @return instance of {@link ContentProviderManager} with only its provider retrieval methods overwritten
     */
    public TestContentProviderManager getContentProviderManager() {
        return contentProviderManager;
    }

    /**
     * Configures the providers retrieved and used by test subclass of {@link ContentProviderManager}. When that
     * class needs to retrieve a {@link ContentProvider} to do some work, it will check these associations for
     * the proper source -> provider mapping.
     * <p/>
     * If this method is never called, in other words no specific associations are made, an instance of the default
     * content source ({@link TestContentProvider}) will be used by the <code>ContentProviderManager</code>
     * regardless of content source requested.
     *
     * @param source   source that will be handed to the ContentProviderManager and used to retrieve a specific
     *                 content source
     * @param provider provider to use when the given source is requested for operations
     */
    public void associateContentProvider(ContentSource source, ContentProvider provider) {
        providers.put(source.getId(), provider);
    }

    protected MasterServerPluginContainer createMasterPluginContainer() {
        MasterServerPluginContainer master = new TestMasterServerPluginContainer();
        File dir = new File(System.getProperty("java.io.tmpdir"), "test-server-plugins");
        MasterServerPluginContainerConfiguration config = new MasterServerPluginContainerConfiguration(dir, dir, dir,
            null);
        master.initialize(config);
        return master;
    }

    /**
     * The test master plugin container.
     */
    private class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
            ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(1);
            pcs.add(new TestContentServerPluginContainer(this));
            return pcs;
        }

        protected ClassLoader createRootServerPluginClassLoader() {
            return this.getClass().getClassLoader();
        }

        protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
            return new HashMap<URL, ServerPluginDescriptorType>();
        }
    }

    /**
     * The test content plugin container.
     */
    private class TestContentServerPluginContainer extends ContentServerPluginContainer {
        public TestContentServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        protected ContentProviderManager createAdapterManager() {
            contentProviderManager = new TestContentProviderManager();
            return contentProviderManager;
        }

        protected ContentServerPluginManager createPluginManager() {
            TestContentProviderPluginManager pm = new TestContentProviderPluginManager(this);
            return pm;
        }
    }

    /**
     * The test plugin manager.
     */
    private class TestContentProviderPluginManager extends ContentServerPluginManager {
        public TestContentProviderPluginManager(ContentServerPluginContainer pc) {
            super(pc);
        }
    }

    /**
     * The test adapter manager.
     */
    public class TestContentProviderManager extends ContentProviderManager {

        public ContentProvider getIsolatedContentProvider(int contentProviderId) throws RuntimeException {
            return getContentProvider(contentProviderId);
        }

        protected ContentProvider getIsolatedContentSourceAdapter(ContentSource contentSource) throws RuntimeException {
            return getContentProvider(contentSource.getId());
        }

        protected ContentProvider instantiateAdapter(ContentSource contentSource) {
            return getContentProvider(contentSource.getId());
        }

        private ContentProvider getContentProvider(int contentSourceId) {
            if (providers.size() == 0) {
                return defaultTestContentProvider;
            } else {
                return providers.get(contentSourceId);
            }
        }
    }
}