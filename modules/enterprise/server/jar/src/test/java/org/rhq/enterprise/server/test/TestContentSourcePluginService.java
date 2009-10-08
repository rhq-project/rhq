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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.core.clientapi.server.plugin.content.ContentSourceAdapter;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.enterprise.server.plugin.content.ContentSourceAdapterManager;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginContainer;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginManager;
import org.rhq.enterprise.server.plugin.content.ContentSourcePluginService;

/**
 * Used as a mock service for the content source plugin containr.
 */
public class TestContentSourcePluginService extends ContentSourcePluginService implements
    TestContentSourcePluginServiceMBean {
    // public so tests can directly set these
    public Map<ContentSource, ContentSourceAdapter> testAdapters;
    public PackageSyncReport testLastSyncReport;
    public Map<ContentSource, Collection<ContentSourcePackageDetails>> testExistingPackages;

    @Override
    protected ContentSourcePluginContainer createPluginContainer() {
        ContentSourcePluginContainer pc = new TestContentSourcePluginContainer();
        ContentSourcePluginContainerConfiguration config = new ContentSourcePluginContainerConfiguration();
        pc.initialize(config);
        return pc;
    }

    /**
     * The test PC.
     */
    class TestContentSourcePluginContainer extends ContentSourcePluginContainer {
        @Override
        protected ContentSourceAdapterManager createAdapterManager(ContentSourcePluginManager pluginManager) {
            TestContentSourceAdapterManager am = new TestContentSourceAdapterManager();
            am.initialize(pluginManager);
            return am;
        }

        @Override
        protected ContentSourcePluginManager createPluginManager() {
            TestContentSourcePluginManager pm = new TestContentSourcePluginManager();
            pm.initialize();
            return pm;
        }
    }

    /**
     * The test plugin manager.
     */
    class TestContentSourcePluginManager extends ContentSourcePluginManager {
        @Override
        protected void setConfiguration(ContentSourcePluginContainerConfiguration configuration) {
            super.setConfiguration(configuration);
        }

        @Override
        public void initialize() {
        }

        @Override
        public void shutdown() {
        }
    }

    /**
     * The test adapter manager.
     */
    class TestContentSourceAdapterManager extends ContentSourceAdapterManager {
        @Override
        public Set<ContentSource> getAllContentSources() {
            return (testAdapters != null) ? testAdapters.keySet() : new HashSet<ContentSource>();
        }

        @Override
        protected void initialize(ContentSourcePluginManager pluginManager) {
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
                        return testAdapters.get(cs).getInputStream(location);
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
                        testAdapters.get(cs).synchronizePackages(testLastSyncReport, testExistingPackages.get(cs));
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

        @Override
        protected void setConfiguration(ContentSourcePluginContainerConfiguration config) {
            super.setConfiguration(config);
        }
    }
}