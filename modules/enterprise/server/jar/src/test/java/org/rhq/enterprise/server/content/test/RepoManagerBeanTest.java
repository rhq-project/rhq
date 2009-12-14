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
package org.rhq.enterprise.server.content.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.RepoRelationshipType;
import org.rhq.core.domain.content.RepoRepoRelationship;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.plugin.pc.content.TestContentServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoManagerBeanTest extends AbstractEJB3Test {

    private final static boolean ENABLED = true;

    private RepoManagerLocal repoManager;
    private ContentSourceManagerLocal contentSourceManager;
    private ContentSourceMetadataManagerLocal contentSourceMetadataManager;

    private Subject overlord;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {

        TransactionManager tx = getTransactionManager();
        tx.begin();

        repoManager = LookupUtil.getRepoManagerLocal();
        contentSourceManager = LookupUtil.getContentSourceManager();
        contentSourceMetadataManager = LookupUtil.getContentSourceMetadataManager();

        TestContentServerPluginService pluginService = new TestContentServerPluginService(this);
        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
        unprepareServerPluginService();

        TransactionManager tx = getTransactionManager();
        if (tx != null) {
            tx.rollback();
        }
    }

    @Test(enabled = ENABLED)
    public void createABunchOfRepos() throws Exception {
        PageList<Repo> repos = repoManager.findRepos(overlord, new PageControl());
        int origsize = 0;
        if (repos != null) {
            origsize = repos.size();
        }
        for (int i = 0; i < 10; i++) {
            Random r = new Random(System.currentTimeMillis());
            Repo repo = new Repo(r.nextLong() + "");
            repoManager.createRepo(overlord, repo);
        }
        repos = repoManager.findRepos(overlord, new PageControl());

        assert repos.size() == (origsize + 10);

    }

    @Test(enabled = ENABLED)
    public void testSyncStatus() throws Exception {
        Repo repo = new Repo("testSyncStatus");
        repo = repoManager.createRepo(overlord, repo);

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 10; i++) {
            cal.roll(Calendar.DATE, false);
            RepoSyncResults results = new RepoSyncResults(repo);
            if (i % 2 == 0 && i != 0) {
                System.out.println("Setting failed: i: [" + i + "]");
                results.setStatus(ContentSyncStatus.FAILURE);
            } else {
                results.setStatus(ContentSyncStatus.SUCCESS);
            }

            results.setEndTime(cal.getTimeInMillis());
            System.out.println("EndTime: " + new Date(results.getEndTime().longValue()));
            repo.addSyncResult(results);
        }

        // Add one with no end time.  This is to test NPE during sorting
        RepoSyncResults results = new RepoSyncResults(repo);
        results.setStartTime(cal.getTimeInMillis());
        repo.addSyncResult(results);

        // Check sync status
        repo.getSyncResults();
        String status = repoManager.calculateSyncStatus(overlord, repo.getId());
        assert status.equals(ContentSyncStatus.SUCCESS.toString());
    }

    @Test(enabled = ENABLED)
    public void createDeleteRepo() throws Exception {
        Repo repo = new Repo("testCreateDeleteRepo");
        int id = repoManager.createRepo(overlord, repo).getId();
        Repo lookedUp = repoManager.getRepo(overlord, id);
        assert lookedUp != null;
        Repo lookedUp2 = repoManager.getRepoByName(lookedUp.getName()).get(0);
        assert lookedUp2 != null;
        assert id == lookedUp.getId();
        assert id == lookedUp2.getId();

        repoManager.deleteRepo(overlord, id);
        lookedUp = repoManager.getRepo(overlord, id);
        assert lookedUp == null;
    }

    @Test(enabled = ENABLED)
    public void createDeleteRepoGroup() throws Exception {
        // Setup
        EntityManager entityManager = getEntityManager();

        RepoGroupType groupType = new RepoGroupType("testCreateDeleteRepoGroupType");
        entityManager.persist(groupType);
        entityManager.flush();

        String groupName = "testCreateDeleteRepoGroup";
        RepoGroup group = repoManager.getRepoGroupByName(groupName);
        assert group == null;

        // Test
        group = new RepoGroup(groupName);
        group.setRepoGroupType(groupType);
        group = repoManager.createRepoGroup(overlord, group);

        // Verify
        int id = group.getId();
        group = repoManager.getRepoGroup(overlord, id);
        assert group != null;
        assert group.getName().equals(groupName);

        // Cleanup
        repoManager.deleteRepoGroup(overlord, id);
        group = repoManager.getRepoGroup(overlord, id);
        assert group == null;

        entityManager.remove(groupType);
    }

    @Test(enabled = ENABLED)
    public void createFindDeleteCandidateRepo() throws Exception {
        // Setup
        Repo repo = new Repo("test candidate repo");

        PageList<Repo> importedRepos = repoManager.findRepos(overlord, new PageControl());
        int origSize = 0;
        if (importedRepos != null) {
            origSize = importedRepos.size();
        }

        // Test
        repo = repoManager.createCandidateRepo(overlord, repo);

        // Verify
        try {
            assert repo.isCandidate();

            // Should not be returned from this call since its a candidate repo
            importedRepos = repoManager.findRepos(overlord, new PageControl());
            assert importedRepos.size() == origSize;
            assert repoManager.getRepo(overlord, repo.getId()) != null;
        } finally {
            repoManager.deleteRepo(overlord, repo.getId());
        }
    }

    @Test(enabled = ENABLED)
    public void createDuplicateRepoGroup() throws Exception {
        // Setup
        EntityManager entityManager = getEntityManager();

        RepoGroupType groupType = new RepoGroupType("testCreateDuplicateRepoGroup");
        entityManager.persist(groupType);
        entityManager.flush();

        String groupName = "testCreateDuplicateRepoGroup";

        RepoGroup existing = new RepoGroup(groupName);
        existing.setRepoGroupType(groupType);
        repoManager.createRepoGroup(overlord, existing);

        existing = repoManager.getRepoGroupByName(groupName);
        assert existing != null;

        // Test
        RepoGroup duplicate = new RepoGroup(groupName);
        duplicate.setRepoGroupType(groupType);

        try {
            repoManager.createRepoGroup(overlord, existing);
            assert false;
        } catch (RepoException e) {
            // Expected
        }

        // Cleanup
        repoManager.deleteRepoGroup(overlord, existing.getId());
        existing = repoManager.getRepoGroup(overlord, existing.getId());
        assert existing == null;

        entityManager.remove(groupType);
    }

    @Test(enabled = ENABLED)
    public void getRepoGroupByNameNoGroup() throws Exception {
        // Test
        RepoGroup group = repoManager.getRepoGroupByName("foo");

        assert group == null;
    }

    @Test(enabled = ENABLED)
    public void getRepoGroupTypeByName() throws Exception {
        // Setup
        EntityManager entityManager = getEntityManager();
        String name = "test-repo-type";

        RepoGroupType groupType = new RepoGroupType(name);
        entityManager.persist(groupType);
        entityManager.flush();

        // Test
        RepoGroupType type = repoManager.getRepoGroupTypeByName(overlord, name);
        assert type != null;
        assert type.getName().equals(name);

        // Cleanup
        type = entityManager.find(RepoGroupType.class, type.getId());
        entityManager.remove(type);
        entityManager.flush();
    }

    @Test(enabled = ENABLED)
    public void addRepoRelationship() throws Exception {
        // Setup
        EntityManager entityManager = getEntityManager();

        Repo repo = new Repo("repo1");
        Repo relatedRepo = new Repo("repo2");

        repo = repoManager.createRepo(overlord, repo);
        relatedRepo = repoManager.createRepo(overlord, relatedRepo);

        String relationshipTypeName = "testRelationshipType";
        RepoRelationshipType relationshipType = new RepoRelationshipType(relationshipTypeName);
        entityManager.persist(relationshipType);
        entityManager.flush();

        // Test
        repoManager.addRepoRelationship(overlord, repo.getId(), relatedRepo.getId(), relationshipTypeName);

        // Verify
        RepoCriteria repoCriteria = new RepoCriteria();
        repoCriteria.fetchRepoRepoGroups(true);
        repoCriteria.addFilterId(repo.getId());

        PageList<Repo> repoPageList = repoManager.findReposByCriteria(overlord, repoCriteria);
        assert repoPageList.size() == 1;

        Repo persistedRepo = repoPageList.get(0);
        Set<RepoRepoRelationship> relationships = persistedRepo.getRepoRepoRelationships();
        assert relationships.size() == 1;

        RepoRepoRelationship relationship = relationships.iterator().next();
        assert relationship.getRepoRepoRelationshipPK().getRepo().getName().equals("repo1");
        assert relationship.getRepoRepoRelationshipPK().getRepoRelationship().getRelatedRepo().getName()
            .equals("repo2");
        assert relationship.getRepoRepoRelationshipPK().getRepoRelationship().getRepoRelationshipType().getName()
            .equals(relationshipTypeName);

        // Cleanup handled by rollback in tear down method
    }

    @Test(enabled = ENABLED)
    public void findCandidatesByContentProvider() throws Exception {
        // Setup
        String candidateRepoName = "candidate with source";

        //   Create a content source type and a content source
        ContentSourceType type = new ContentSourceType("testGetSyncResultsListCST");
        Set<ContentSourceType> types = new HashSet<ContentSourceType>();
        types.add(type);
        contentSourceMetadataManager.registerTypes(types); // this blows away any previous existing types
        ContentSource contentSource = new ContentSource("testGetSyncResultsListCS", type);
        contentSource = contentSourceManager.simpleCreateContentSource(overlord, contentSource);

        //   Create an imported (non-candidate) repo associated with the source
        Repo importedRepo = new Repo("imported repo");
        importedRepo.addContentSource(contentSource);
        importedRepo = repoManager.createRepo(overlord, importedRepo);

        //   Create a candidate repo associated with that source
        Repo candidateRepo = new Repo(candidateRepoName);
        candidateRepo.addContentSource(contentSource);
        candidateRepo = repoManager.createCandidateRepo(overlord, candidateRepo);

        // Test
        RepoCriteria criteria = new RepoCriteria();
        criteria.addFilterCandidate(true);
        criteria.addFilterContentSourceIds(contentSource.getId());
        criteria.fetchRepoContentSources(true);

        PageList<Repo> foundRepos = repoManager.findReposByCriteria(overlord, criteria);

        // Verify

        //   Make sure only one of the two repos from above came back
        assert foundRepos.size() == 1;

        Repo foundRepo = foundRepos.get(0);
        assert foundRepo.getName().equals(candidateRepoName);
        assert foundRepo.isCandidate();

        // Cleanup handled by rollback in tear down method
    }

    @Test(enabled = ENABLED)
    public void importCandidateRepo() throws Exception {
        // Setup
        Repo candidate = new Repo("create me");
        Repo created = repoManager.createCandidateRepo(overlord, candidate);

        // Test
        List<Integer> repoIds = new ArrayList<Integer>(1);
        repoIds.add(created.getId());
        repoManager.importCandidateRepo(overlord, repoIds);

        // Verify
        RepoCriteria repoCriteria = new RepoCriteria();
        repoCriteria.addFilterId(created.getId());

        PageList<Repo> repoList = repoManager.findReposByCriteria(overlord, repoCriteria);
        assert repoList.size() == 1;

        Repo verify = repoList.get(0);
        assert verify != null;
        assert !verify.isCandidate();
    }

    @Test(enabled = ENABLED)
    public void importCandidateRepoBadId() throws Exception {
        // Test
        try {
            List<Integer> repoIds = new ArrayList<Integer>(1);
            repoIds.add(12345);
            repoManager.importCandidateRepo(overlord, repoIds);
            assert false;
        } catch (RepoException e) {
            // Expected
        }
    }

    @Test(enabled = ENABLED)
    public void importNonCandidateRepo() throws Exception {
        // Setup
        Repo nonCandidate = new Repo("create me");
        Repo created = repoManager.createRepo(overlord, nonCandidate);

        // Test
        try {
            List<Integer> repoIds = new ArrayList<Integer>(1);
            repoIds.add(created.getId());
            repoManager.importCandidateRepo(overlord, repoIds);
            assert false;
        } catch (RepoException e) {
            // Expected
        }

    }

    @Test(enabled = ENABLED)
    public void deleteCandidatesForContentSource() throws Exception {
        // Setup
        ContentSourceType contentSourceType = new ContentSourceType("testSourceType");
        Set<ContentSourceType> types = new HashSet<ContentSourceType>(1);
        types.add(contentSourceType);
        contentSourceMetadataManager.registerTypes(types);

        ContentSource source1 = new ContentSource("testSource1", contentSourceType);
        source1 = contentSourceManager.simpleCreateContentSource(overlord, source1);

        ContentSource source2 = new ContentSource("testSource2", contentSourceType);
        source2 = contentSourceManager.simpleCreateContentSource(overlord, source2);

        // -> Only has source to delete, should be deleted
        Repo repo1 = new Repo("repo1");
        repo1.addContentSource(source1);

        // -> Has different source, should not be deleted
        Repo repo2 = new Repo("repo2");
        repo2.addContentSource(source2);

        // -> Has source to delete and another source, should not be deleted
        Repo repo3 = new Repo("repo3");
        repo3.addContentSource(source1);
        repo3.addContentSource(source2);

        // -> No sources, should not be deleted
        Repo repo4 = new Repo("repo4");

        repo1 = repoManager.createCandidateRepo(overlord, repo1);
        repo2 = repoManager.createCandidateRepo(overlord, repo2);
        repo3 = repoManager.createCandidateRepo(overlord, repo3);
        repo4 = repoManager.createCandidateRepo(overlord, repo4);

        // Test
        repoManager.deleteCandidatesWithOnlyContentSource(overlord, source1.getId());

        // Verify
        assert repoManager.getRepo(overlord, repo1.getId()) == null;
        assert repoManager.getRepo(overlord, repo2.getId()) != null;
        assert repoManager.getRepo(overlord, repo3.getId()) != null;
        assert repoManager.getRepo(overlord, repo4.getId()) != null;
    }

    @Test(enabled = ENABLED)
    public void updateRepoWithProvider() throws Exception {
        // See BZ 537216 for more details

        // Setup
        String newName = "newRepoName";
        String oldName = "testRepo";

        ContentSourceType contentSourceType = new ContentSourceType("testSourceType");

        Set<ContentSourceType> types = new HashSet<ContentSourceType>(1);
        types.add(contentSourceType);
        contentSourceMetadataManager.registerTypes(types);

        ContentSource source = new ContentSource("testSource1", contentSourceType);
        source = contentSourceManager.simpleCreateContentSource(overlord, source);

        Repo repo = new Repo(oldName);
        repo = repoManager.createRepo(overlord, repo);

        repoManager.simpleAddContentSourcesToRepo(overlord, repo.getId(), new int[] {source.getId()});

        // Test
        repo.setName(newName);
        repoManager.updateRepo(overlord, repo);

        // Verify
        RepoCriteria byName = new RepoCriteria();
        byName.addFilterName(newName);
        PageList<Repo> reposWithNewName = repoManager.findReposByCriteria(overlord, byName);

        assert reposWithNewName.size() == 1;

        byName = new RepoCriteria();
        byName.addFilterName(oldName);
        PageList<Repo> reposWithOldName = repoManager.findReposByCriteria(overlord, byName);

        assert reposWithOldName.size() == 0;
    }
}