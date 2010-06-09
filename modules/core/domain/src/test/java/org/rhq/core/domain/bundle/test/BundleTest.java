/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.bundle.test;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.testng.annotations.Test;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.BundleVersionRepo;
import org.rhq.core.domain.bundle.BundleVersionRepoPK;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test
@SuppressWarnings("unchecked")
public class BundleTest extends AbstractEJB3Test {

    private static final boolean ENABLED = true;

    @Test(enabled = ENABLED)
    public void testBundleVersionRepo() throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundleVersionRepo";
            String recipe = "action/script/recipe is here";

            Repo repo1 = new Repo(name + "-Repo1");
            Repo repo2 = new Repo(name + "-Repo2");
            em.persist(repo1);
            em.persist(repo2);
            assert repo1.getId() > 0;
            assert repo2.getId() > 0;

            Query q = em.createNamedQuery(BundleVersionRepo.QUERY_FIND_BY_REPO_ID_NO_FETCH);
            q.setParameter("id", repo1.getId());
            assert q.getResultList().size() == 0 : "should not have repo1 mapping in the db yet";
            q.setParameter("id", repo2.getId());
            assert q.getResultList().size() == 0 : "should not have repo2 mapping in the db yet";

            BundleType bundleType = createBundleType(em, name + "-Type", createResourceType(em));
            Bundle bundle = createBundle(em, name + "-Bundle", bundleType);

            BundleVersion bundleVersion = new BundleVersion(name, "1.0.0.BETA", bundle, recipe);
            bundleVersion.setVersionOrder(0);
            em.persist(bundleVersion);
            id = bundleVersion.getId();
            assert id > 0;
            assert bundleVersion.getBundle().getId() != 0 : "bundle should have been cascade persisted too";
            assert bundleVersion.getBundle().getBundleType().getId() != 0 : "bundleType should have been cascade persisted too";

            BundleVersionRepo bvr1 = new BundleVersionRepo(bundleVersion, repo1);
            BundleVersionRepo bvr2 = new BundleVersionRepo(bundleVersion, repo2);
            em.persist(bvr1);
            em.persist(bvr2);

            q = em.createNamedQuery(BundleVersionRepo.QUERY_FIND_BY_REPO_ID_NO_FETCH);
            q.setParameter("id", repo1.getId());
            assert q.getResultList().size() == 1;
            assert ((BundleVersionRepo) q.getSingleResult()).getBundleVersionRepoPK().getBundleVersion().equals(
                bundleVersion);
            assert ((BundleVersionRepo) q.getSingleResult()).getBundleVersionRepoPK().getRepo().equals(repo1);

            q.setParameter("id", repo2.getId());
            assert q.getResultList().size() == 1;
            assert ((BundleVersionRepo) q.getSingleResult()).getBundleVersionRepoPK().getBundleVersion().equals(
                bundleVersion);
            assert ((BundleVersionRepo) q.getSingleResult()).getBundleVersionRepoPK().getRepo().equals(repo2);

            q = em.createNamedQuery(BundleVersionRepo.QUERY_FIND_BY_BUNDLE_VERSION_ID_NO_FETCH);
            q.setParameter("id", bundleVersion.getId());
            List<BundleVersionRepo> resultList = q.getResultList();
            assert resultList.size() == 2;
            BundleVersionRepoPK pk1 = new BundleVersionRepoPK(bundleVersion, repo1);
            BundleVersionRepoPK pk2 = new BundleVersionRepoPK(bundleVersion, repo2);
            if (resultList.get(0).getBundleVersionRepoPK().getRepo().equals(repo1)) {
                assert bvr1.equals(resultList.get(0));
                assert bvr2.equals(resultList.get(1));
                assert pk1.equals(resultList.get(0).getBundleVersionRepoPK());
                assert pk2.equals(resultList.get(1).getBundleVersionRepoPK());
            } else {
                assert bvr1.equals(resultList.get(1));
                assert bvr2.equals(resultList.get(0));
                assert pk1.equals(resultList.get(1).getBundleVersionRepoPK());
                assert pk2.equals(resultList.get(0).getBundleVersionRepoPK());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLED)
    public void testBundleVersion() throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundleVersion";
            String recipe = "action/script/recipe is here";

            BundleType bundleType = createBundleType(em, name + "-Type", createResourceType(em));
            Bundle bundle = createBundle(em, name + "-Bundle", bundleType);
            id = bundle.getId();
            assert id > 0;
            assert bundle.getBundleType().getId() != 0 : "bundleType should have been cascade persisted too";

            BundleVersion bv = new BundleVersion(name, "1.0.0.BETA", bundle, recipe);
            bv.setVersionOrder(777);
            Query q = em.createNamedQuery(BundleVersion.QUERY_FIND_BY_NAME);
            q.setParameter("name", bv.getName());
            assert q.getResultList().size() == 0; // not in the db yet

            em.persist(bv);
            id = bv.getId();
            assert id > 0;

            q = em.createNamedQuery(BundleVersion.QUERY_FIND_BY_NAME);
            q.setParameter("name", bv.getName());
            assert q.getResultList().size() == 1;
            assert ((BundleVersion) q.getSingleResult()).getName().equals(bv.getName());

            BundleVersion bvFind = em.find(BundleVersion.class, id);
            assert bvFind != null;
            assert bvFind.getId() == bv.getId();
            assert bvFind.getName().equals(bv.getName());
            assert bvFind.getVersion().equals(bv.getVersion());
            assert bvFind.getVersionOrder() == bv.getVersionOrder();
            assert bvFind.getRecipe().equals(bv.getRecipe());
            assert bvFind.getBundle().equals(bv.getBundle());
            assert bvFind.equals(bv);
            assert bvFind.hashCode() == bv.hashCode();

            // clean up - delete our test entity
            em.close();

            em = getEntityManager();
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_BY_NAME);
            q.setParameter("name", bv.getName());
            BundleVersion doomed = (BundleVersion) q.getSingleResult();
            doomed = em.getReference(BundleVersion.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the entity";
            em.close();

            // make sure we didn't delete the bundle - it should not be cascade deleted
            em = getEntityManager();
            q = em.createNamedQuery(Bundle.QUERY_FIND_BY_NAME);
            q.setParameter("name", bundle.getName());
            assert q.getResultList().size() == 1;
            bundle = (Bundle) q.getSingleResult();
            bundleType = bundle.getBundleType();
            Repo repo = bundle.getRepo();
            em.remove(bundle);
            em.remove(repo);

            deleteResourceType(em, bundleType.getResourceType());

            assert q.getResultList().size() == 0 : "didn't clean up test bundle";
            assert em.find(BundleType.class, bundleType.getId()) == null : "didn't clean up bundle type";

            em.close();

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLED)
    public void testMultipleBundleVersions() throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testMultipleBundleVersions";
            String recipe = "action/script/recipe is here";

            BundleType bundleType = createBundleType(em, name + "-Type", createResourceType(em));
            Bundle bundle = createBundle(em, name + "-Bundle", bundleType);
            id = bundle.getId();
            assert id > 0;
            assert bundle.getBundleType().getId() != 0 : "bundleType should have been cascade persisted too";

            // make sure these queries can return empty lists
            Query q = em.createNamedQuery(BundleVersion.QUERY_FIND_LATEST_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            assert q.getResultList().size() == 0;
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            assert q.getResultList().size() == 0;
            q = em.createNamedQuery(BundleVersion.UPDATE_VERSION_ORDER_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            q.setParameter("versionOrder", 0);
            assert q.executeUpdate() == 0 : "should not have updated anything";

            BundleVersion bv = new BundleVersion(name, "1.0", bundle, recipe);
            bv.setVersionOrder(0);
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 0; // not in the db yet
            em.persist(bv);
            id = bv.getId();
            assert id > 0;

            BundleVersion bv2 = new BundleVersion(name, "2.0", bundle, recipe);
            bv2.setVersionOrder(1);
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 1;
            em.persist(bv2);
            id = bv2.getId();
            assert id > 0;

            q = em.createNamedQuery(BundleVersion.QUERY_FIND_LATEST_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            assert q.getResultList().size() == 1;
            assert ((BundleVersion) q.getSingleResult()).getVersion().equals(bv2.getVersion());
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            List<Object[]> versionsArrays = q.getResultList(); // returns in DESC sort order!
            assert versionsArrays.size() == 2;
            assert ((String) versionsArrays.get(0)[0]).equals(bv2.getVersion());
            assert ((Number) versionsArrays.get(0)[1]).intValue() == bv2.getVersionOrder();
            assert ((String) versionsArrays.get(1)[0]).equals(bv.getVersion());
            assert ((Number) versionsArrays.get(1)[1]).intValue() == bv.getVersionOrder();

            // increment all version orders, starting at order #1
            q = em.createNamedQuery(BundleVersion.UPDATE_VERSION_ORDER_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            q.setParameter("versionOrder", 1);
            assert q.executeUpdate() == 1 : "should have auto-incremented version order in one row";
            em.flush();
            em.clear();
            bv = em.find(BundleVersion.class, bv.getId());
            assert bv.getVersionOrder() == 0 : "should not have incremented version order";
            bv2 = em.find(BundleVersion.class, bv2.getId());
            assert bv2.getVersionOrder() == 2 : "didn't increment version order";

            BundleVersion bv3 = new BundleVersion(name, "1.5", bundle, recipe);
            bv3.setVersionOrder(1);
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 2;
            em.persist(bv3);
            id = bv3.getId();
            assert id > 0;

            q = em.createNamedQuery(BundleVersion.QUERY_FIND_LATEST_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            assert q.getResultList().size() == 1;
            assert ((BundleVersion) q.getSingleResult()).getVersion().equals(bv2.getVersion());
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            versionsArrays = q.getResultList(); // returns in DESC sort order!
            assert versionsArrays.size() == 3;
            assert ((String) versionsArrays.get(0)[0]).equals(bv2.getVersion()); // 2.0
            assert ((Number) versionsArrays.get(0)[1]).intValue() == bv2.getVersionOrder();
            assert ((String) versionsArrays.get(1)[0]).equals(bv3.getVersion()); // 1.5
            assert ((Number) versionsArrays.get(1)[1]).intValue() == bv3.getVersionOrder();
            assert ((String) versionsArrays.get(2)[0]).equals(bv.getVersion()); // 1.0
            assert ((Number) versionsArrays.get(2)[1]).intValue() == bv.getVersionOrder();

            // increment all version orders, starting at order #0 - makes sure we can update >1 rows
            q = em.createNamedQuery(BundleVersion.UPDATE_VERSION_ORDER_BY_BUNDLE_ID);
            q.setParameter("bundleId", bundle.getId());
            q.setParameter("versionOrder", 0);
            assert q.executeUpdate() == 3 : "should have auto-incremented version orders";
            em.flush();
            em.clear();
            bv = em.find(BundleVersion.class, bv.getId());
            assert bv.getVersionOrder() == 1 : "didn't increment version order: " + bv.getVersionOrder();
            bv2 = em.find(BundleVersion.class, bv2.getId());
            assert bv2.getVersionOrder() == 3 : "didn't increment version order: " + bv2.getVersionOrder();
            bv3 = em.find(BundleVersion.class, bv3.getId());
            assert bv3.getVersionOrder() == 2 : "didn't increment version order: " + bv3.getVersionOrder();

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLED)
    public void testBundle() throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundle-Bundle";

            Query q = em.createNamedQuery(Bundle.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 0; // not in the db yet

            BundleType bundleType = createBundleType(em, name + "-Type", createResourceType(em));
            Bundle b = createBundle(em, name, bundleType);
            id = b.getId();
            assert id > 0;
            assert b.getBundleType().getId() != 0 : "bundleType should have been persisted independently";
            assert b.getRepo().getId() != 0 : "bundle's repo should have been cascade persisted with the bundle";
            assert b.getName().equals(b.getRepo().getName()) : "bundle's repo should have same name as bundle";

            q = em.createNamedQuery(Bundle.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 1;
            assert ((Bundle) q.getSingleResult()).getName().equals(b.getName());

            Bundle bFind = em.find(Bundle.class, id);
            assert bFind != null;
            assert bFind.getId() == b.getId();
            assert bFind.getName().equals(b.getName());
            assert bFind.getBundleType().equals(b.getBundleType());
            assert bFind.equals(b);
            assert bFind.hashCode() == b.hashCode();

            // clean up - delete our test entity
            em.close();

            em = getEntityManager();
            q = em.createNamedQuery(Bundle.QUERY_FIND_BY_NAME);
            q.setParameter("name", b.getName());
            Bundle doomed = (Bundle) q.getSingleResult();
            doomed = em.getReference(Bundle.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the entity";
            em.close();

            // make sure we didn't delete the bundle type - it should not be cascade deleted
            em = getEntityManager();
            q = em.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
            q.setParameter("name", bFind.getBundleType().getName());
            assert q.getResultList().size() == 1;
            BundleType bt = (BundleType) q.getSingleResult();
            em.remove(bt);
            assert q.getResultList().size() == 0 : "didn't clean up test bundle type";

            deleteResourceType(em, bundleType.getResourceType());
            em.close();

            // make sure we didn't cascade delete the repo, it must also be deleted manually
            em = getEntityManager();
            q = em.createNamedQuery(Repo.QUERY_FIND_BY_NAME);
            q.setParameter("name", bFind.getRepo().getName());
            assert q.getResultList().size() == 1 : "didn't clean up test repo";
            em.close();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    @Test(enabled = ENABLED)
    public void testBundleType() throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundleType";

            Query q = em.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 0; // not in the db yet

            BundleType bt = createBundleType(em, name, createResourceType(em));

            id = bt.getId();
            assert id > 0;

            q = em.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 1;
            assert ((BundleType) q.getSingleResult()).getName().equals(bt.getName());

            BundleType btFind = em.find(BundleType.class, id);
            assert btFind != null;
            assert btFind.getId() == bt.getId();
            assert btFind.getName().equals(bt.getName());
            assert btFind.equals(bt);
            assert btFind.hashCode() == bt.hashCode();

            // clean up - delete our test entity
            em.close();

            em = getEntityManager();
            q = em.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
            q.setParameter("name", bt.getName());
            BundleType doomed = (BundleType) q.getSingleResult();
            doomed = em.getReference(BundleType.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the entity";

            deleteResourceType(em, bt.getResourceType());
            em.close();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            getTransactionManager().rollback();
        }
    }

    private ResourceType createResourceType(EntityManager em) {
        ResourceType rt = new ResourceType("BundleTest", "BundleTestPlugin", ResourceCategory.PLATFORM, null);
        em.persist(rt);
        return rt;
    }

    private BundleType createBundleType(EntityManager em, String name, ResourceType rt) throws Exception {
        BundleType bt = new BundleType(name, rt);
        em.persist(bt);
        return bt;
    }

    private Bundle createBundle(EntityManager em, String name, BundleType bt) throws Exception {
        // implicit Bundle'sRepo
        Repo repo = new Repo(name);
        repo.setCandidate(false);
        repo.setSyncSchedule(null);
        em.persist(repo);

        // Bundle's packageType
        PackageType pt = new PackageType(name, bt.getResourceType());
        pt.setCategory(PackageCategory.BUNDLE);

        Bundle bundle = new Bundle(name, bt, repo, pt);
        em.persist(bundle);
        return bundle;
    }

    private ResourceType deleteResourceType(EntityManager em, ResourceType rt) {
        rt = em.find(ResourceType.class, rt.getId());
        em.remove(rt);
        return rt;
    }
}
