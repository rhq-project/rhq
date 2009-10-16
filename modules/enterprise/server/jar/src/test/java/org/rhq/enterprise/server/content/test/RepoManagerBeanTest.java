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

import javax.transaction.TransactionManager;
import javax.persistence.EntityManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.content.RepoException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class RepoManagerBeanTest extends AbstractEJB3Test {

    private final static boolean ENABLED = true;

    private RepoManagerLocal repoManager;
    private Subject overlord;

    @BeforeMethod
    public void setupBeforeMethod() throws Exception {
        TransactionManager tx = getTransactionManager();
        tx.begin();
        repoManager = LookupUtil.getRepoManagerLocal();
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
        }
        catch (RepoException e) {
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
}