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
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class RepoManagerBeanTest extends AbstractEJB3Test {
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

    @Test
    public void testCreateDeleteRepo() throws Exception {
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

    @Test
    public void testCreateDeleteRepoGroup() throws Exception {
        // Setup
        EntityManager entityManager = getEntityManager();

        RepoGroupType groupType = new RepoGroupType("testCreateDeleteRepoGroupType");
        entityManager.persist(groupType);

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

    public void testGetRepoGroupByNameNoGroup() throws Exception {
        // Test
        RepoGroup group = repoManager.getRepoGroupByName("foo");

        assert group == null;
    }
}