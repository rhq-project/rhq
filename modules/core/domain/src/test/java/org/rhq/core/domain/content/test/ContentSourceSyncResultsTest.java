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

import java.util.List;

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test(groups = "integration.ejb3")
public class ContentSourceSyncResultsTest extends AbstractEJB3Test {
    public void testInsert() throws Exception {
        getTransactionManager().begin();
        try {
            EntityManager em = getEntityManager();

            ResourceType rt = new ResourceType("testCSSRResourceType", "testPlugin", ResourceCategory.PLATFORM, null);
            Resource resource = new Resource("testCSSRResource", "testCSSRResource", rt);
            resource.setUuid("uuid");
            Architecture arch = new Architecture("testCSSRInsertArch");
            PackageType pt = new PackageType("testCSSRInsertPT", resource.getResourceType());
            Package pkg = new Package("testCSSRInsertPackage", pt);
            PackageVersion pv = new PackageVersion(pkg, "version", arch);
            ContentSourceType cst = new ContentSourceType("testCSSRContentSourceType");
            ContentSource cs = new ContentSource("testCSSRContentSource", cst);
            ContentSourceSyncResults results = new ContentSourceSyncResults(cs);
            Repo repo = new Repo("testCSSRRepo");
            repo.addContentSource(cs);

            em.persist(rt);
            em.persist(resource);
            em.persist(arch);
            em.persist(pt);
            em.persist(pkg);
            em.persist(pv);
            em.persist(cst);
            em.persist(cs);
            em.persist(repo);
            em.persist(results);
            cs.addSyncResult(results);
            em.flush();
            em.close();
            em = getEntityManager();

            cs = em.find(ContentSource.class, cs.getId());
            assert cs != null;
            List<ContentSourceSyncResults> syncResults = cs.getSyncResults();
            assert syncResults != null;
            assert syncResults.size() == 1;
            results = syncResults.get(0);
            assert results.getContentSource().equals(cs);
            assert results.getStatus() == ContentSyncStatus.INPROGRESS;
            assert results.getResults() == null;
            assert results.getEndTime() == null;
            assert results.getStartTime() <= System.currentTimeMillis();

            results.setEndTime(System.currentTimeMillis());
            results.setStatus(ContentSyncStatus.FAILURE);
            results.setResults("dummy failure");
            results = em.merge(results);

            // add another (make sure the start time is long enough to pass the time check below
            Thread.sleep(100);
            results = new ContentSourceSyncResults(cs);
            em.persist(results);
            cs.addSyncResult(results);
            em.flush();
            em.close();
            em = getEntityManager();

            cs = em.find(ContentSource.class, cs.getId());
            assert cs != null;
            syncResults = cs.getSyncResults();
            assert syncResults != null;
            assert syncResults.size() == 2;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            getTransactionManager().rollback();
        }
    }
}