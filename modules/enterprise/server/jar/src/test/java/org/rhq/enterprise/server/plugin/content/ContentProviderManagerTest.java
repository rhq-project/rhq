/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.content;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import org.rhq.core.clientapi.server.plugin.content.ContentProvider;
import org.rhq.core.clientapi.server.plugin.content.ContentProviderPackageDetails;
import org.rhq.core.clientapi.server.plugin.content.PackageSource;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.clientapi.server.plugin.content.RepoImportReport;
import org.rhq.core.clientapi.server.plugin.content.RepoSource;
import org.rhq.core.clientapi.server.plugin.content.RepoDetails;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestContentSourcePluginService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

/** @author Jason Dobies */
public class ContentProviderManagerTest extends AbstractEJB3Test {

    private ContentSourceManagerLocal contentManager;
    private SubjectManagerLocal subjectManager;
    private RepoManagerLocal repoManager;

    private TestContentSourcePluginService pluginService;

    private Subject overlord;
    private ContentSourceType testSourceType;
    private ContentSource testSource;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        prepareScheduler();
        pluginService = prepareContentSourcePluginService();
        pluginService.startPluginContainer();

        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        contentManager = LookupUtil.getContentSourceManager();
        subjectManager = LookupUtil.getSubjectManager();
        repoManager = LookupUtil.getRepoManagerLocal();

        overlord = subjectManager.getOverlord();

        // Make a sample content source type that will be used in this test
        testSourceType = new ContentSourceType("testType");
        entityManager.persist(testSourceType);
        entityManager.flush();

        // Add a content source to the DB to hang the lookups against
        testSource = new ContentSource("testSource", testSourceType);

        contentManager.simpleCreateContentSource(overlord, testSource);
        entityManager.flush();

        tx.commit();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();

        // Delete the source that was created
        EntityManager entityManager = getEntityManager();

        testSource = entityManager.find(ContentSource.class, testSource.getId());
        entityManager.remove(testSource);

        // Delete the type that was created
        testSourceType = entityManager.find(ContentSourceType.class, testSourceType.getId());
        entityManager.remove(testSourceType);

        tx.commit();

        unprepareContentSourcePluginService();
        unprepareScheduler();
    }

    @Test
    public void test() throws Exception {
        TestContentProviderManager providerManager = new TestContentProviderManager();

        boolean completed = providerManager.synchronizeContentSource(testSource.getId());
        assert completed;

        // Make sure the repos were created
        List<Repo> retrievedRepos;

        retrievedRepos = repoManager.getRepoByName("testRepo1");
        assert retrievedRepos.size() == 1;

        retrievedRepos = repoManager.getRepoByName("testRepo2");
        assert retrievedRepos.size() == 1;

        retrievedRepos= repoManager.getRepoByName("testRepoFoo");
        assert retrievedRepos.size() == 0;
    }

    /** Mock implementation of a content provider that will return known data. */
    private class TestContentProvider implements ContentProvider, PackageSource, RepoSource {
        public void initialize(Configuration configuration) throws Exception {
            // No-op
        }

        public void shutdown() {
            // No-op
        }

        public void testConnection() throws Exception {
            // No-op
        }

        public void synchronizePackages(PackageSyncReport report, Collection<ContentProviderPackageDetails> existingPackages) throws Exception {
            
        }

        public InputStream getInputStream(String location) throws Exception {
            // No-op
            return null;
        }

        public RepoImportReport importRepos() throws Exception {
            RepoImportReport report = new RepoImportReport();

            RepoDetails repo1 = new RepoDetails("testRepo1");
            RepoDetails repo2 = new RepoDetails("testRepo2");

            report.addRepo(repo1);
            report.addRepo(repo2);

            return report;
        }
    }

    /**
     * Stubs out the methods in {@link ContentProviderManager} that would go out and expect a fully
     * packaged plugin. Instead, use the mock implementation provided above.
     */
    private class TestContentProviderManager extends ContentProviderManager {

        private ContentProvider testProvider = new TestContentProvider();

        protected ContentProvider getIsolatedContentProvider(int contentProviderId) throws RuntimeException {
            return testProvider;
        }
    }


}
