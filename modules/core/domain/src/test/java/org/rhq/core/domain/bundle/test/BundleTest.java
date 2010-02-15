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
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionType;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test
public class BundleTest extends AbstractEJB3Test {
    public void testBundleVersionRepo() throws Throwable {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundleVersionRepo";
            String action = "action/script/recipe is here";

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

            BundleType bundleType = new BundleType(name + "-Type", createResourceType(em));
            Bundle bundle = new Bundle(name + "-Bundle", bundleType);
            em.persist(bundle);
            BundleVersion bundleVersion = new BundleVersion(name, "1.0.0.BETA", bundle);
            bundleVersion.setAction(action);
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

    public void testBundleVersion() throws Throwable {
        boolean done = false;
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundleVersion";
            String action = "action/script/recipe is here";

            BundleType bundleType = new BundleType(name + "-Type", createResourceType(em));
            Bundle bundle = new Bundle(name + "-Bundle", bundleType);
            em.persist(bundle);
            id = bundle.getId();
            assert id > 0;
            assert bundle.getBundleType().getId() != 0 : "bundleType should have been cascade persisted too";

            BundleVersion bv = new BundleVersion(name, "1.0.0.BETA", bundle);
            bv.setAction(action);
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
            assert bvFind.getAction().equals(bv.getAction());
            assert bvFind.getBundle().equals(bv.getBundle());
            assert bvFind.equals(bv);
            assert bvFind.hashCode() == bv.hashCode();
            assert bvFind.getDistribution() == null;

            // update it with a distro, then null out the distro, checking along the way
            DistributionType distroType = new DistributionType(name + "-DistroType");
            Distribution distro = new Distribution(name + "-Distro", "basepathIn", distroType);
            em.persist(distro);
            assert distro.getId() > 0;
            assert distro.getDistributionType().getId() > 0;
            bvFind.setDistribution(distro);
            em.flush();
            bvFind = em.find(BundleVersion.class, id);
            assert bvFind.getDistribution().equals(distro);
            bvFind.setDistribution(null);
            em.flush();
            bvFind = em.find(BundleVersion.class, id);
            assert bvFind.getDistribution() == null;
            em.remove(distro);
            em.remove(distroType);

            // clean up - delete our test entity
            em.close();
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            q = em.createNamedQuery(BundleVersion.QUERY_FIND_BY_NAME);
            q.setParameter("name", bv.getName());
            BundleVersion doomed = (BundleVersion) q.getSingleResult();
            doomed = em.getReference(BundleVersion.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the entity";
            em.close();
            getTransactionManager().commit();

            // make sure we didn't delete the bundle - it should not be cascade deleted
            getTransactionManager().begin();
            em = getEntityManager();
            q = em.createNamedQuery(Bundle.QUERY_FIND_BY_NAME);
            q.setParameter("name", bundle.getName());
            assert q.getResultList().size() == 1;
            bundle = (Bundle) q.getSingleResult();
            bundleType = bundle.getBundleType();
            em.remove(bundle);
            em.remove(bundleType);
            assert q.getResultList().size() == 0 : "didn't clean up test bundle";
            assert em.find(BundleType.class, bundleType.getId()) == null : "didn't clean up bundle type";

            deleteResourceType(em, bundleType.getResourceType());
            em.close();
            getTransactionManager().commit();

            done = true;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if (!done) {
                getTransactionManager().rollback();
            }
        }
    }

    public void testBundle() throws Throwable {
        boolean done = false;
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundle";

            Query q = em.createNamedQuery(Bundle.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 0; // not in the db yet

            BundleType bundleType = new BundleType(name + "-Type", createResourceType(em));
            Bundle b = new Bundle(name, bundleType);

            em.persist(b);
            id = b.getId();
            assert id > 0;
            assert b.getBundleType().getId() != 0 : "bundleType should have been cascade persisted too";

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
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            q = em.createNamedQuery(Bundle.QUERY_FIND_BY_NAME);
            q.setParameter("name", b.getName());
            Bundle doomed = (Bundle) q.getSingleResult();
            doomed = em.getReference(Bundle.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the entity";
            em.close();
            getTransactionManager().commit();

            // make sure we didn't delete the bundle type - it should not be cascade deleted
            getTransactionManager().begin();
            em = getEntityManager();
            q = em.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
            q.setParameter("name", bFind.getBundleType().getName());
            assert q.getResultList().size() == 1;
            BundleType bt = (BundleType) q.getSingleResult();
            em.remove(bt);
            assert q.getResultList().size() == 0 : "didn't clean up test bundle type";

            deleteResourceType(em, bundleType.getResourceType());
            em.close();
            getTransactionManager().commit();

            done = true;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if (!done) {
                getTransactionManager().rollback();
            }
        }
    }

    public void testBundleType() throws Throwable {
        boolean done = false;
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            int id;

            String name = "BundleTest-testBundleType";

            Query q = em.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
            q.setParameter("name", name);
            assert q.getResultList().size() == 0; // not in the db yet

            BundleType bt = new BundleType(name, createResourceType(em));

            em.persist(bt);
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
            getTransactionManager().commit();
            getTransactionManager().begin();
            em = getEntityManager();
            q = em.createNamedQuery(BundleType.QUERY_FIND_BY_NAME);
            q.setParameter("name", bt.getName());
            BundleType doomed = (BundleType) q.getSingleResult();
            doomed = em.getReference(BundleType.class, doomed.getId());
            em.remove(doomed);
            assert q.getResultList().size() == 0 : "didn't remove the entity";

            deleteResourceType(em, bt.getResourceType());
            em.close();
            getTransactionManager().commit();
            done = true;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            if (!done) {
                getTransactionManager().rollback();
            }
        }
    }

    private ResourceType createResourceType(EntityManager em) {
        ResourceType rt = new ResourceType("BundleTest", "BundleTestPlugin", ResourceCategory.PLATFORM, null);
        em.persist(rt);
        return rt;
    }

    private ResourceType deleteResourceType(EntityManager em, ResourceType rt) {
        rt = em.find(ResourceType.class, rt.getId());
        em.remove(rt);
        return rt;
    }
}
