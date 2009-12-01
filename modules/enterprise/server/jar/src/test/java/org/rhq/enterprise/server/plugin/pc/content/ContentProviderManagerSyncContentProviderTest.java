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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoRelationship;
import org.rhq.core.domain.content.RepoRepoGroup;
import org.rhq.core.domain.content.RepoRepoRelationship;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ContentProviderManagerSyncContentProviderTest extends AbstractEJB3Test {

    private static final String PLUGIN_NAME = "testPlugin";

    private static final String CUSTOM_IMPORTED_REPO_NAME = "customImportedRepo";
    public static final String EXISTING_IMPORTED_REPO_NAME = TestContentProvider.EXISTING_IMPORTED_REPO_NAME;
    public static final String EXISTING_CANDIDATE_REPO_NAME = TestContentProvider.EXISTING_CANDIDATE_REPO_NAME;

    private static final String PREVIOUS_CANDIDATE_REPO_NAME = "testPreviousCandidate";

    private TestContentServerPluginService pluginService;

    // The following variables need to be cleaned up at the end of the test

    private ContentSourceType testSourceType;

    private ContentSource syncSource; // Source that is syncced in this test
    private ContentSource nonSyncSource; // Existing source NOT being syncced in this test

    private Repo nonCandidateOnOtherSource; // Should NOT appear in the sync packages call since its a different source

    private List<Integer> reposToDelete = new ArrayList<Integer>();
    private List<Integer> repoGroupsToDelete = new ArrayList<Integer>();
    private Integer repoId;
    private Integer relatedRepoId;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {

        // Plugin service setup
        prepareScheduler();
        pluginService = new TestContentServerPluginService(this);

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
        reposToDelete.clear();

        // Delete any repo groups that were created in this test
        for (Integer repoGroupId : repoGroupsToDelete) {
            repoManager.deleteRepoGroup(overlord, repoGroupId);
        }
        repoGroupsToDelete.clear();

        // First disassociate packages from the content source
        entityManager.createNamedQuery(PackageVersionContentSource.DELETE_BY_CONTENT_SOURCE_ID).setParameter(
            "contentSourceId", syncSource.getId()).executeUpdate();

        // Delete the existing repos
        nonCandidateOnOtherSource = entityManager.find(Repo.class, nonCandidateOnOtherSource.getId());
        entityManager.remove(nonCandidateOnOtherSource);

        // Delete the source that was created
        syncSource = entityManager.find(ContentSource.class, syncSource.getId());
        entityManager.remove(syncSource);

        nonSyncSource = entityManager.find(ContentSource.class, nonSyncSource.getId());
        entityManager.remove(nonSyncSource);

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

        // -> Add an already imported repo to the system so it already exists when the report introduces it
        Repo existingImportedRepo = new Repo(EXISTING_IMPORTED_REPO_NAME);
        existingImportedRepo.setCandidate(false);
        existingImportedRepo.addContentSource(syncSource);
        repoManager.createRepo(overlord, existingImportedRepo);

        // -> Add an already imported repo that wasn't introduced from the report; a user created repo
        Repo customImportedRepo = new Repo(CUSTOM_IMPORTED_REPO_NAME);
        customImportedRepo.setCandidate(false);
        customImportedRepo.addContentSource(syncSource);
        repoManager.createRepo(overlord, customImportedRepo);

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
        // TestContentProviderManager providerManager = new TestContentProviderManager();
        boolean tested = pluginService.getContentProviderManager().testConnection(syncSource.getId());
        assert tested;

        boolean completed = pluginService.getContentProviderManager().synchronizeContentProvider(syncSource.getId());
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

    }
}
