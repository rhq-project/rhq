/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.bundle;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.hibernate.LazyInitializationException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings( { "unchecked", "unused" })
@Test
public class BundleManagerBeanTest extends AbstractEJB3Test {

    private static final String TEST_PREFIX = "bundletest";

    private static final boolean ENABLED = true;

    private BundleManagerLocal bundleManagerBean;

    private TestBundleServerPluginService ps;
    private MasterServerPluginContainer pc;

    @BeforeMethod
    public void beforeMethod() {
        super.startTest();

        this.ps = new TestBundleServerPluginService();
        prepareCustomServerPluginService(this.ps);
        bundleManagerBean = LookupUtil.getBundleManager();
        this.ps.startMasterPluginContainer();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {

        EntityManager em = null;

        try {
            getTransactionManager().begin();
            em = getEntityManager();

            Query q;
            List doomed;

            // clean up any tests that don't already clean up after themselves
            q = em.createQuery("SELECT bv FROM BundleVersion bv WHERE bv.bundle.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleVersion.class, ((BundleVersion) removeMe).getId()));
            }

            q = em.createQuery("SELECT b FROM Bundle b WHERE b.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Bundle.class, ((Bundle) removeMe).getId()));
            }

            q = em.createQuery("SELECT r FROM Repo r WHERE r.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Repo.class, ((Repo) removeMe).getId()));
            }

            q = em.createQuery("SELECT bt FROM BundleType bt WHERE bt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleType.class, ((BundleType) removeMe).getId()));
            }

            q = em.createQuery("SELECT rt FROM ResourceType rt WHERE rt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(ResourceType.class, ((ResourceType) removeMe).getId()));
            }

            getTransactionManager().commit();
            em.close();
            em = null;
        } catch (Exception e) {
            try {
                System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        } finally {
            if (null != em) {
                em.close();
            }
        }

        unprepareServerPluginService();
        this.ps = null;

        super.endTest();
    }

    @Test(enabled = ENABLED)
    public void testGetBundleTypes() throws Exception {
        BundleType bt1 = createBundleType("one");
        BundleType bt2 = createBundleType("two");
        List<BundleType> bts = bundleManagerBean.getAllBundleTypes(LookupUtil.getSubjectManager().getOverlord());
        assert bts.size() >= 2 : "should have at least 2 bundle types";

        List<String> btNames = new ArrayList<String>();
        for (BundleType bundleType : bts) {
            btNames.add(bundleType.getName());
        }

        assert btNames.contains(bt1.getName());
        assert btNames.contains(bt2.getName());
    }

    @Test(enabled = ENABLED)
    public void testfindBundlesByCriteria() throws Exception {
        Bundle b1 = createBundle("one");
        Bundle b2 = createBundle("two");
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        BundleVersion bv2 = createBundleVersion(b2.getName(), "1.0", b2);
        BundleCriteria c = new BundleCriteria();
        PageList<Bundle> bundles = null;
        Bundle b = null;
        String name = null;

        // return all with no optional data
        bundles = bundleManagerBean.findBundlesByCriteria(getOverlord(), c);
        assertNotNull(bundles);
        assertEquals(2, bundles.size());
        b = bundles.get(0);
        name = "one";
        assertNotNull(b);
        assertTrue(b.getBundleType().getName(), b.getName().contains(name));
        assertTrue(b.getBundleType().getName(), b.getBundleType().getName().contains(name));
        try {
            assertTrue(b.getBundleVersions().isEmpty());
            fail("Should have thrown LazyInitializationException");
        } catch (LazyInitializationException e) {
            // expected
        } catch (Exception e) {
            fail("Should have thrown LazyInitializationException");
        }

        b = bundles.get(1);
        name = "two";
        assertNotNull(b);
        assertTrue(b.getBundleType().getName(), b.getName().contains(name));
        assertTrue(b.getBundleType().getName(), b.getBundleType().getName().contains(name));

        // return bundle "two" using all criteria and with all optional data
        c.addFilterId(b.getId());
        c.addFilterName(b.getName());
        c.addFilterBundleTypeName(b.getName());
        c.fetchBundleVersions(true);
        c.fetchRepo(true);
        bundles = bundleManagerBean.findBundlesByCriteria(getOverlord(), c);
        assertNotNull(bundles);
        assertEquals(1, bundles.size());
        b = bundles.get(0);
        assertTrue(b.getBundleType().getName(), b.getName().contains(name));
        assertTrue(b.getBundleType().getName(), b.getBundleType().getName().contains(name));
        assertNotNull(b.getBundleVersions());
        assertEquals(1, b.getBundleVersions().size());
        BundleVersion bv = b.getBundleVersions().get(0);
        assertEquals(bv2, bv);
        assertEquals(b, bv.getBundle());
        Repo r = b.getRepo();
        assertNotNull(r);
        assertEquals(b.getName(), r.getName());
    }

    @Test(enabled = ENABLED)
    public void testfindBundleVersionsByCriteria() throws Exception {
        Bundle b1 = createBundle("one");
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        BundleVersion bv2 = createBundleVersion(b1.getName(), "2.0", b1);
        BundleVersion bv3 = createBundleVersion(b1.getName(), "2.1", b1);
        BundleVersionCriteria c = new BundleVersionCriteria();
        PageList<BundleVersion> bvs = null;
        BundleVersion bv = null;

        // return all with no optional data
        bvs = bundleManagerBean.findBundleVersionsByCriteria(getOverlord(), c);
        bv = bvs.get(1);
        assertNotNull(bvs);
        assertEquals(3, bvs.size());
        assertEquals(bv2, bv);

        // return bundle version using all criteria and with all optional data
        c.addFilterId(bv.getId());
        c.addFilterName(bv.getName());
        c.addFilterBundleName("one");
        c.addFilterVersion(bv.getVersion());
        c.fetchBundle(true);
        c.fetchDistribution(true);
        c.fetchBundleDeployDefinitions(true);
        bvs = bundleManagerBean.findBundleVersionsByCriteria(getOverlord(), c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        bv = bvs.get(0);
        assertEquals(bv2, bv);
        assertEquals(bv.getBundle(), b1);
        assertNull(bv.getDistribution());
        assertNotNull(bv.getBundleDeployDefinitions());
        assertTrue(bv.getBundleDeployDefinitions().isEmpty());
    }

    private BundleType createBundleType(String name) throws Exception {
        final String fullName = TEST_PREFIX + "-type-" + name;
        BundleType bt = bundleManagerBean.createBundleType(getOverlord(), fullName, createResourceType(name).getId());

        assert bt.getId() > 0;
        assert bt.getName().endsWith(fullName);
        return bt;
    }

    private Bundle createBundle(String name) throws Exception {
        BundleType bt = createBundleType(name);
        return createBundle(name, bt);
    }

    private Bundle createBundle(String name, BundleType bt) throws Exception {
        final String fullName = TEST_PREFIX + "-bundle-" + name;
        Bundle b = bundleManagerBean.createBundle(getOverlord(), fullName, bt.getId());

        assert b.getId() > 0;
        assert b.getName().endsWith(fullName);
        return b;
    }

    private BundleVersion createBundleVersion(String name, String version, Bundle bundle) throws Exception {
        final String fullName = TEST_PREFIX + "-version-" + version + "-" + name;
        final String recipe = "deploy -f " + TEST_PREFIX + ".zip -d <% test.path %>";
        BundleVersion bv = bundleManagerBean.createBundleVersion(getOverlord(), bundle.getId(), fullName, version,
            recipe);

        assert bv.getId() > 0;
        assert bv.getName().endsWith(fullName);
        return bv;
    }

    private ResourceType createResourceType(String name) throws Exception {
        final String fullName = TEST_PREFIX + "-resourcetype-" + name;
        ResourceType rt = new ResourceType(fullName, "BundleManagerBeanTest", ResourceCategory.PLATFORM, null);

        TransactionManager txMgr = getTransactionManager();
        txMgr.begin();
        EntityManager em = getEntityManager();
        em.persist(rt);
        em.close();
        txMgr.commit();
        return rt;
    }

    private Subject getOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }
}
