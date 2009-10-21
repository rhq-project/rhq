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

import org.rhq.core.clientapi.server.plugin.content.ContentProvider;
import org.rhq.core.clientapi.server.plugin.content.ContentProviderPackageDetails;
import org.rhq.core.clientapi.server.plugin.content.PackageSource;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.clientapi.server.plugin.content.RepoDetails;
import org.rhq.core.clientapi.server.plugin.content.RepoGroupDetails;
import org.rhq.core.clientapi.server.plugin.content.RepoImportReport;
import org.rhq.core.clientapi.server.plugin.content.RepoSource;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoRepoGroup;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestContentSourcePluginService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Jason Dobies
 */
public class ContentProviderManagerTest extends AbstractEJB3Test {

    // The following variables need to be cleaned up at the end of the test
    private ContentSourceType testSourceType;
    private ContentSource testSource;
    private List<Integer> reposToDelete = new ArrayList<Integer>();
    private List<Integer> repoGroupsToDelete = new ArrayList<Integer>();

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {

        // Plugin service setup
        prepareScheduler();
        TestContentSourcePluginService pluginService = prepareContentSourcePluginService();
        pluginService.startPluginContainer();

        // Because of the (current) transaction settings of some of the nested methods (i.e. REQUIRES_NEW),
        // this test must commit its data and clean up after itself, as compared to simply rolling back the
        // transaction at the end.
        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        ContentSourceManagerLocal contentManager = LookupUtil.getContentSourceManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        // Create a sample content source type that will be used in this test
        testSourceType = new ContentSourceType("testType");
        entityManager.persist(testSourceType);

        // Add a content source to the DB to hang the sync against
        testSource = new ContentSource("testSource", testSourceType);
        contentManager.simpleCreateContentSource(overlord, testSource);

        tx.commit();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {

        // Transactional stuff
        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        // Delete any repos that were created in this test
        for (Integer repoId : reposToDelete) {
            repoManager.deleteRepo(overlord, repoId);
        }

        // Delete any repo groups that were created in this test
        for (Integer repoGroupId : repoGroupsToDelete) {
            repoManager.deleteRepoGroup(overlord, repoGroupId);
        }

        // Delete the source that was created
        testSource = entityManager.find(ContentSource.class, testSource.getId());
        entityManager.remove(testSource);

        // Delete the type that was created
        testSourceType = entityManager.find(ContentSourceType.class, testSourceType.getId());
        entityManager.remove(testSourceType);

        tx.commit();

        // Plugin service teardown
        unprepareContentSourcePluginService();
        unprepareScheduler();
    }

    @Test
    public void synchronizeContentSource() throws Exception {
        // Setup
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        TestContentProviderManager providerManager = new TestContentProviderManager();

        // Test
        boolean completed = providerManager.synchronizeContentSource(testSource.getId());
        assert completed;

        // Verify RepoGroups
        RepoGroup repoGroup = repoManager.getRepoGroupByName("testRepoGroup");
        assert repoGroup != null;
        repoGroupsToDelete.add(repoGroup.getId());

        // Verify Repos
        List<Repo> retrievedRepos;

        // -> Simple Repo
        retrievedRepos = repoManager.getRepoByName("testRepo1");
        assert retrievedRepos.size() == 1;
        reposToDelete.add(retrievedRepos.get(0).getId());

        // -> Repo in a group
        retrievedRepos = repoManager.getRepoByName("testRepo2");
        assert retrievedRepos.size() == 1;
        reposToDelete.add(retrievedRepos.get(0).getId());

        Repo repoInGroup = retrievedRepos.get(0);

        RepoCriteria findWithRepoGroup = new RepoCriteria();
        findWithRepoGroup.fetchRepoRepoGroups(true);
        findWithRepoGroup.addFilterId(repoInGroup.getId());

        PageList<Repo> repoPageList = repoManager.findReposByCriteria(overlord, findWithRepoGroup);
        repoInGroup = repoPageList.get(0);

        Set<RepoRepoGroup> repoGroups = repoInGroup.getRepoRepoGroups();
        assert repoGroups.size() == 1;

        RepoRepoGroup repoRepoGroup = repoGroups.iterator().next();
        assert repoRepoGroup.getRepoRepoGroupPK().getRepoGroup().getName().equals("testRepoGroup");
        assert repoRepoGroup.getRepoRepoGroupPK().getRepo().getName().equals("testRepo2");

        // -> Non-existent repo
        retrievedRepos = repoManager.getRepoByName("testRepoFoo");
        assert retrievedRepos.size() == 0;
    }

    /**
     * Mock implementation of a content provider that will return known data.
     */
    private class TestContentProvider implements ContentProvider, PackageSource, RepoSource {

        public RepoImportReport importRepos() throws Exception {
            RepoImportReport report = new RepoImportReport();

            // Create a repo group in the system
            RepoGroupDetails group1 = new RepoGroupDetails("testRepoGroup", "family");
            report.addRepoGroup(group1);

            // Simple repo
            RepoDetails repo1 = new RepoDetails("testRepo1");
            repo1.setDescription("First test repo");
            report.addRepo(repo1);

            // Repo belonging to a group that was created in the sync
            RepoDetails repo2 = new RepoDetails("testRepo2");
            repo2.setRepoGroup("testRepoGroup");
            report.addRepo(repo2);

            // Repo with a parent repo created in this sync
            RepoDetails repo3 = new RepoDetails("testRepo3");
            repo3.setParentRepoName("testRepo1");

            return report;
        }

        public void synchronizePackages(PackageSyncReport report,
                                        Collection<ContentProviderPackageDetails> existingPackages) throws Exception {

        }

        public void initialize(Configuration configuration) throws Exception {
            // No-op
        }

        public void shutdown() {
            // No-op
        }

        public void testConnection() throws Exception {
            // No-op
        }


        public InputStream getInputStream(String location) throws Exception {
            // No-op
            return null;
        }

    }

    /**
     * Stubs out the methods in {@link ContentProviderManager} that would go out and expect a fully packaged plugin.
     * Instead, use the mock implementation provided above.
     */
    private class TestContentProviderManager extends ContentProviderManager {

        private ContentProvider testProvider = new TestContentProvider();

        protected ContentProvider getIsolatedContentProvider(int contentProviderId) throws RuntimeException {
            return testProvider;
        }
    }

}
