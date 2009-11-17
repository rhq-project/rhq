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
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceSyncStatus;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.RepoRelationshipType;
import org.rhq.core.domain.content.RepoRepoRelationship;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoException;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
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

        overlord = LookupUtil.getSubjectManager().getOverlord();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {
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
            repoManager.createRepo(overlord, repo).getId();
        }
        repos = repoManager.findRepos(overlord, new PageControl());

        assert repos.size() == (origsize + 10);

    }

    @Test(enabled = ENABLED)
    public void testSyncRepos() throws Exception {
        Repo repo = new Repo("testSyncStatus");
        repoManager.createRepo(overlord, repo);
        Integer[] ids = { repo.getId() };
        int syncCount = repoManager.synchronizeRepos(overlord, ids);

        assert syncCount == 0;

        ContentSourceType cst = new ContentSourceType("testSyncStatus");
        ContentSource cs = new ContentSource("testSyncStatus", cst);
        repo.addContentSource(cs);
        syncCount = repoManager.synchronizeRepos(overlord, ids);

        assert syncCount == 1;
    }

    @Test(enabled = ENABLED)
    public void testSyncStatus() throws Exception {
        Repo repo = new Repo("testSyncStatus");
        repoManager.createRepo(overlord, repo).getId();

        Calendar cal = Calendar.getInstance();

        ContentSourceType cst = new ContentSourceType("testSyncStatus");
        ContentSource cs = new ContentSource("testSyncStatus", cst);
        EntityManager em = getEntityManager();
        em.persist(cst);
        em.persist(cs);

        for (int i = 0; i < 10; i++) {
            cal.roll(Calendar.DATE, false);
            ContentSourceSyncResults results = new ContentSourceSyncResults(cs);
            if (i % 2 == 0 && i != 0) {
                System.out.println("Setting failed: i: [" + i + "]");
                results.setStatus(ContentSourceSyncStatus.FAILURE);
            } else {
                results.setStatus(ContentSourceSyncStatus.SUCCESS);
            }

            results.setEndTime(cal.getTimeInMillis());
            System.out.println("EndTime: " + new Date(results.getEndTime().longValue()));
            em.persist(results);
            cs.addSyncResult(results);
        }

        // Add one with no end time.  This is to test NPE during sorting
        ContentSourceSyncResults results = new ContentSourceSyncResults(cs);
        results.setStartTime(cal.getTimeInMillis());
        em.persist(results);
        cs.addSyncResult(results);

        repo.addContentSource(cs);

        em.flush();
        em.close();

        // Check sync status
        cs.getSyncResults();
        String status = repoManager.calculateSyncStatus(overlord, repo.getId());
        assert status.equals(ContentSourceSyncStatus.SUCCESS.toString());
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
}