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
package org.rhq.enterprise.server.plugin.pc.content;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoRelationship;
import org.rhq.core.domain.content.RepoRepoGroup;
import org.rhq.core.domain.content.RepoRepoRelationship;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ContentProviderManagerTest extends AbstractEJB3Test {

    private static final String PACKAGE_TYPE_NAME = "testPackageType";
    private static final String RESOURCE_TYPE_NAME = "testResourceType";
    private static final String PLUGIN_NAME = "testPlugin";
    private static final String PACKAGE_NAME = "package1";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_ARCH = "noarch";

    private static final String CUSTOM_IMPORTED_REPO_NAME = "customImportedRepo";
    private static final String EXISTING_IMPORTED_REPO_NAME = "testRepoImportedExisting";
    private static final String EXISTING_CANDIDATE_REPO_NAME = "testRepoCandidateExisting";

    private static final String PREVIOUS_CANDIDATE_REPO_NAME = "testPreviousCandidate";

    private TestContentProvider testProvider = new TestContentProvider();

    // The following variables need to be cleaned up at the end of the test

    private ContentSourceType testSourceType;

    private ContentSource syncSource; // Source that is syncced in this test
    private ContentSource nonSyncSource; // Existing source NOT being syncced in this test

    private Repo nonCandidateOnOtherSource; // Should NOT appear in the sync packages call since its a different source

    private PackageType testPackageType;

    private ResourceType testResourceType;
    private List<Integer> reposToDelete = new ArrayList<Integer>();
    private List<Integer> repoGroupsToDelete = new ArrayList<Integer>();
    private List<Integer> packagesToDelete = new ArrayList<Integer>();
    private Integer repoId;
    private Integer relatedRepoId;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {

        // Plugin service setup
        prepareScheduler();
        TestContentServerPluginService pluginService = new TestContentServerPluginService();
        prepareCustomServerPluginService(pluginService);
        pluginService.startMasterPluginContainer();

        // Because of the (current) transaction settings of some of the nested methods (i.e. REQUIRES_NEW),
        // this test must commit its data and clean up after itself, as compared to simply rolling back the
        // transaction at the end.
        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        ContentSourceManagerLocal contentManager = LookupUtil.getContentSourceManager();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        // Create a sample content source type that will be used in this test
        testSourceType = new ContentSourceType("testType");
        entityManager.persist(testSourceType);

        // Add a content source to sync in this test
        syncSource = new ContentSource("testSource1", testSourceType);
        contentManager.simpleCreateContentSource(overlord, syncSource);
        entityManager.flush();

        // Add an extra content source that isn't being syncced
        nonSyncSource = new ContentSource("testSource2", testSourceType);
        contentManager.simpleCreateContentSource(overlord, nonSyncSource);
        entityManager.flush();

        // Add existing repo against other source (this shouldn't show up in the request for packages)
        nonCandidateOnOtherSource = new Repo("nonCandidateOnOtherSource");
        nonCandidateOnOtherSource.setCandidate(false);
        nonCandidateOnOtherSource.addContentSource(nonSyncSource);
        repoManager.createRepo(overlord, nonCandidateOnOtherSource);

        // Create a sample package type that will be used for packages sync'd in the test
        testResourceType = new ResourceType(RESOURCE_TYPE_NAME, PLUGIN_NAME, ResourceCategory.PLATFORM, null);
        entityManager.persist(testResourceType);

        testPackageType = new PackageType(PACKAGE_TYPE_NAME, testResourceType);
        entityManager.persist(testPackageType);

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

        // Delete the repo relationships
        entityManager.createNamedQuery(RepoRepoRelationship.DELETE_BY_REPO_ID).setParameter("repoId", repoId)
            .executeUpdate();

        entityManager.createNamedQuery(RepoRelationship.DELETE_BY_RELATED_REPO_ID).setParameter("relatedRepoId",
            relatedRepoId).executeUpdate();

        // Delete any repos that were created in this test
        for (Integer repoId : reposToDelete) {
            repoManager.deleteRepo(overlord, repoId);
        }

        // Delete any repo groups that were created in this test
        for (Integer repoGroupId : repoGroupsToDelete) {
            repoManager.deleteRepoGroup(overlord, repoGroupId);
        }

        // First disassociate packages from the content source
        entityManager.createNamedQuery(PackageVersionContentSource.DELETE_BY_CONTENT_SOURCE_ID).setParameter(
            "contentSourceId", syncSource.getId()).executeUpdate();

        // Delete any packages created in this test
        for (Integer packageId : packagesToDelete) {
            PackageVersion deleteMe = entityManager.find(PackageVersion.class, packageId);
            entityManager.remove(deleteMe);
        }
        entityManager.flush();

        // Delete the existing repos
        nonCandidateOnOtherSource = entityManager.find(Repo.class, nonCandidateOnOtherSource.getId());
        entityManager.remove(nonCandidateOnOtherSource);

        // Delete the source that was created
        syncSource = entityManager.find(ContentSource.class, syncSource.getId());
        entityManager.remove(syncSource);

        nonSyncSource = entityManager.find(ContentSource.class, nonSyncSource.getId());
        entityManager.remove(nonSyncSource);

        // Delete the fake package type
        testPackageType = entityManager.find(PackageType.class, testPackageType.getId());
        entityManager.remove(testPackageType);

        // Delete the fake resource type
        testResourceType = entityManager.find(ResourceType.class, testResourceType.getId());
        entityManager.remove(testResourceType);

        // Delete the fake source type
        testSourceType = entityManager.find(ContentSourceType.class, testSourceType.getId());
        entityManager.remove(testSourceType);

        tx.commit();

        // Plugin service teardown
        unprepareServerPluginService();
        unprepareScheduler();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void synchronizeContentSource() throws Exception {

        // Setup
        // --------------------------------------------
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        TestContentProviderManager providerManager = new TestContentProviderManager();

        Set<String> reposThatShouldBeSyncced = new HashSet();

        // -> Add an already imported repo to the system so it already exists when the report introduces it
        Repo existingImportedRepo = new Repo(EXISTING_IMPORTED_REPO_NAME);
        existingImportedRepo.setCandidate(false);
        existingImportedRepo.addContentSource(syncSource);
        repoManager.createRepo(overlord, existingImportedRepo);

        reposThatShouldBeSyncced.add(EXISTING_IMPORTED_REPO_NAME);

        // -> Add an already imported repo that wasn't introduced from the report; a user created repo
        Repo customImportedRepo = new Repo(CUSTOM_IMPORTED_REPO_NAME);
        customImportedRepo.setCandidate(false);
        customImportedRepo.addContentSource(syncSource);
        repoManager.createRepo(overlord, customImportedRepo);

        reposThatShouldBeSyncced.add(CUSTOM_IMPORTED_REPO_NAME);

        // -> Simulate a candidate repo from a previous import that will be in this report as well
        Repo existingCandidateRepo = new Repo(EXISTING_CANDIDATE_REPO_NAME);
        existingCandidateRepo.setCandidate(true);
        existingCandidateRepo.addContentSource(syncSource);
        repoManager.createCandidateRepo(overlord, existingCandidateRepo);

        // -> Simulate a candidate repo from a previous import that will *NOT* be in this report
        Repo previousRepo = new Repo(PREVIOUS_CANDIDATE_REPO_NAME);
        previousRepo.setCandidate(true);
        previousRepo.addContentSource(syncSource);
        repoManager.createCandidateRepo(overlord, previousRepo);

        // Test
        // --------------------------------------------
        boolean completed = providerManager.synchronizeContentSource(syncSource.getId());
        assert completed;

        // Verify RepoGroups
        RepoGroup repoGroup = repoManager.getRepoGroupByName("testRepoGroup");
        assert repoGroup != null;
        repoGroupsToDelete.add(repoGroup.getId());

        // Verify Repos
        // --------------------------------------------
        List<Repo> retrievedRepos;

        // -> Simple Repo
        retrievedRepos = repoManager.getRepoByName("testRepo1");
        assert retrievedRepos.size() == 1;
        assert retrievedRepos.get(0).isCandidate();
        reposToDelete.add(retrievedRepos.get(0).getId());

        // -> Repo in a group
        retrievedRepos = repoManager.getRepoByName("testRepo2");
        assert retrievedRepos.size() == 1;
        assert retrievedRepos.get(0).isCandidate();
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

        // -> Repo with a parent
        retrievedRepos = repoManager.getRepoByName("testRepo3");
        assert retrievedRepos.size() == 1;
        assert retrievedRepos.get(0).isCandidate();
        reposToDelete.add(retrievedRepos.get(0).getId());
        relatedRepoId = retrievedRepos.get(0).getId();

        retrievedRepos = repoManager.getRepoByName("testRepo4");
        assert retrievedRepos.size() == 1;
        assert retrievedRepos.get(0).isCandidate();
        reposToDelete.add(retrievedRepos.get(0).getId());
        repoId = retrievedRepos.get(0).getId();

        RepoCriteria findWithRelationships = new RepoCriteria();
        findWithRelationships.addFilterName("testRepo4");
        findWithRelationships.fetchRepoRepoRelationships(true);
        PageList<Repo> childRepoList = repoManager.findReposByCriteria(overlord, findWithRelationships);
        assert childRepoList.size() == 1;

        Repo childRepo = childRepoList.get(0);
        Set<RepoRepoRelationship> childRepoRepoRelationship = childRepo.getRepoRepoRelationships();
        assert childRepoRepoRelationship.size() == 1;

        // -> Repo that was already imported in the system (make sure there is still only one)
        retrievedRepos = repoManager.getRepoByName(EXISTING_IMPORTED_REPO_NAME);
        assert retrievedRepos.size() == 1;
        reposToDelete.add(retrievedRepos.get(0).getId());

        // -> Repo that was imported but not in the report (i.e. a user created repo)
        retrievedRepos = repoManager.getRepoByName(CUSTOM_IMPORTED_REPO_NAME);
        assert retrievedRepos.size() == 1;
        reposToDelete.add(retrievedRepos.get(0).getId());

        // -> Repo that was already a candidate in the system (make sure it's not added again)
        retrievedRepos = repoManager.getRepoByName(EXISTING_CANDIDATE_REPO_NAME);
        assert retrievedRepos.size() == 1;
        reposToDelete.add(retrievedRepos.get(0).getId());

        // -> Make sure a repo that was previously a candidate of this content provider but did not
        //    come back in the latest sync is removed
        retrievedRepos = repoManager.getRepoByName(PREVIOUS_CANDIDATE_REPO_NAME);
        assert retrievedRepos.size() == 0;

        // -> Non-existent repo
        retrievedRepos = repoManager.getRepoByName("testRepoFoo");
        assert retrievedRepos.size() == 0;

        // Verify Packages
        // --------------------------------------------
        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        try {
            Query packageQuery = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_PACKAGE_DETAILS_KEY);
            packageQuery.setParameter("packageName", PACKAGE_NAME);
            packageQuery.setParameter("packageTypeName", PACKAGE_TYPE_NAME);
            packageQuery.setParameter("resourceTypeId", testResourceType.getId());
            packageQuery.setParameter("architectureName", PACKAGE_ARCH);
            packageQuery.setParameter("version", PACKAGE_VERSION);

            List<PackageVersion> packageList = packageQuery.getResultList();
            assert packageList.size() == 1;
            packagesToDelete.add(packageList.get(0).getId());
        } finally {
            tx.rollback();
        }

        // -> Verify the correct repos were requested in the package sync
        List<String> reposRequestedInSync = testProvider.getReposRequestedInSync();
        assert reposRequestedInSync.size() == 2;
        assert reposRequestedInSync.containsAll(reposThatShouldBeSyncced);
    }

    /**
     * Mock implementation of a content provider that will return known data.
     */
    private class TestContentProvider implements ContentProvider, PackageSource, RepoSource {

        private List<String> reposRequestedInSync = new ArrayList<String>();

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
            // Parent explicitly added to this list *after* this child to ensure that's not a problem
            RepoDetails repo3 = new RepoDetails("testRepo3");
            report.addRepo(repo3);

            RepoDetails repo4 = new RepoDetails("testRepo4");
            repo4.setParentRepoName("testRepo3");
            report.addRepo(repo4);

            // Repo that was already imported in the system
            RepoDetails repo5 = new RepoDetails(EXISTING_IMPORTED_REPO_NAME);
            report.addRepo(repo5);

            // Repo that was already a candidate in the system
            RepoDetails repo6 = new RepoDetails(EXISTING_CANDIDATE_REPO_NAME);
            report.addRepo(repo6);

            return report;
        }

        public void synchronizePackages(String repoName, PackageSyncReport report,
                                        Collection<ContentProviderPackageDetails> existingPackages) throws Exception {
            reposRequestedInSync.add(repoName);

            ContentProviderPackageDetailsKey key1 = new ContentProviderPackageDetailsKey(PACKAGE_NAME, PACKAGE_VERSION,
                PACKAGE_TYPE_NAME, PACKAGE_ARCH, RESOURCE_TYPE_NAME, PLUGIN_NAME);
            ContentProviderPackageDetails pkg1 = new ContentProviderPackageDetails(key1);
            pkg1.setLocation("repo/foo");

            report.addNewPackage(pkg1);
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
            String fakeBits = "Totally fake bits";
            ByteArrayInputStream bis = new ByteArrayInputStream(fakeBits.getBytes());

            return bis;
        }

        public List<String> getReposRequestedInSync() {
            return reposRequestedInSync;
        }
    }

    /**
     * Stubs out the methods in {@link ContentProviderManager} that would go out and expect a fully packaged plugin.
     * Instead, use the mock implementation provided above.
     */
    private class TestContentProviderManager extends ContentProviderManager {

        protected ContentProvider getIsolatedContentProvider(int contentProviderId) throws RuntimeException {
            return testProvider;
        }
    }

}
