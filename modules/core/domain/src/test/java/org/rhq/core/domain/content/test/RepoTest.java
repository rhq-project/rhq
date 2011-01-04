/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.content.test;

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test(groups = "integration.ejb3")
public class RepoTest extends AbstractEJB3Test {
    public void testInsert() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();
            Repo repo = new Repo("testRepoTest");
            repo.setSyncSchedule("0 0 5 * * ?");
            em.persist(repo);
            em.flush();
            em.close();
            em = getEntityManager();

            Repo lookedUp = em.find(Repo.class, repo.getId());
            assert lookedUp != null;
            assert lookedUp.getSyncSchedule().equals("0 0 5 * * ?");

        } finally {
            getTransactionManager().rollback();
        }
    }

}
