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
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.hibernate.LazyInitializationException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.bundle.BundleScheduleResponse;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentAction;
import org.rhq.core.domain.bundle.BundleDeploymentHistory;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentHistoryCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.metadata.test.UpdateSubsytemTestBase;
import org.rhq.enterprise.server.test.TestAgentClient;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings( { "unchecked", "unused" })
@Test
public class BundleManagerBeanTest extends UpdateSubsytemTestBase {

    private static final boolean TESTS_ENABLED = true;

    private static final String TEST_PREFIX = "bundletest";

    private BundleManagerLocal bundleManager;
    private ResourceManagerLocal resourceManager;
    private static final boolean ENABLED = true;
    private static final boolean DISABLED = false;

    private TestBundleServerPluginService ps;
    private MasterServerPluginContainer pc;
    private Subject overlord;
    TestServerCommunicationsService agentServiceContainer;

    @Override
    @BeforeClass
    public void beforeClass() {
        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.bundleService = new TestAgentClient(null, new TestServerCommunicationsService());
    }

    @AfterClass
    public void afterClass() throws Exception {
        unprepareForTestAgents();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {

        this.ps = new TestBundleServerPluginService();
        prepareCustomServerPluginService(this.ps);
        bundleManager = LookupUtil.getBundleManager();
        resourceManager = LookupUtil.getResourceManager();
        overlord = LookupUtil.getSubjectManager().getOverlord();
        this.ps.startMasterPluginContainer();

        // try and clean up any junk that may be lying around from a failed run
        cleanupDatabase();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        cleanupDatabase();
        unprepareServerPluginService();
        this.ps = null;
    }

    private void cleanupDatabase() {
        EntityManager em = null;

        try {
            getTransactionManager().begin();
            em = getEntityManager();

            Query q;
            List doomed;

            // clean up any tests that don't already clean up after themselves

            q = em.createQuery("SELECT bdh FROM BundleDeploymentHistory bdh ");//WHERE bdh.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDeploymentHistory.class, ((BundleDeploymentHistory) removeMe).getId()));
            }

            // remove bundleversions which cascade remove bundlefiles and bundledeploydefs  
            q = em.createQuery("SELECT bv FROM BundleVersion bv WHERE bv.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleVersion.class, ((BundleVersion) removeMe).getId()));
            }
            em.flush();
            // remove any orphaned bfs
            q = em.createQuery("SELECT bf FROM BundleFile bf WHERE bf.generalPackage.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleFile.class, ((BundleFile) removeMe).getId()));
            }
            // remove any orphaned bds
            q = em.createQuery("SELECT bd FROM BundleDeployment bd WHERE bd.bundleDeployDefinition.name LIKE '"
                + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDeployment.class, ((BundleDeployment) removeMe).getId()));
            }
            // remove any orphaned bdds
            q = em.createQuery("SELECT bdd FROM BundleDeployDefinition bdd WHERE bdd.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDeployDefinition.class, ((BundleDeployDefinition) removeMe).getId()));
            }
            // remove packages which cascade remove packageversions
            q = em.createQuery("SELECT p FROM Package p WHERE p.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Package.class, ((Package) removeMe).getId()));
            }
            em.flush();
            // remove any orphaned pvs
            q = em.createQuery("SELECT pv FROM PackageVersion pv WHERE pv.generalPackage.name LIKE '" + TEST_PREFIX
                + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(PackageVersion.class, ((PackageVersion) removeMe).getId()));
            }
            // remove bundles which cascade remove repos            
            q = em.createQuery("SELECT b FROM Bundle b WHERE b.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Bundle.class, ((Bundle) removeMe).getId()));
            }
            em.flush();
            // remove any orphaned repos            
            q = em.createQuery("SELECT r FROM Repo r WHERE r.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Repo.class, ((Repo) removeMe).getId()));
            }
            // remove ResourceTypes which cascade remove BundleTypes and PackageTypes            
            q = em.createQuery("SELECT rt FROM ResourceType rt WHERE rt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(ResourceType.class, ((ResourceType) removeMe).getId()));
            }
            em.flush();
            //  remove any orphaned BundleTypes
            q = em.createQuery("SELECT bt FROM BundleType bt WHERE bt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleType.class, ((BundleType) removeMe).getId()));
            }
            // remove any orphaned packagetypes            
            q = em.createQuery("SELECT pt FROM PackageType pt WHERE pt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(PackageType.class, ((PackageType) removeMe).getId()));
            }
            // remove Agents left over from test resources            
            q = em.createQuery("SELECT a FROM Agent a WHERE a.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Agent.class, ((Agent) removeMe).getId()));
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
    }

    @Test(enabled = TESTS_ENABLED)
    public void testGetBundleTypes() throws Exception {
        BundleType bt1 = createBundleType("one");
        BundleType bt2 = createBundleType("two");
        List<BundleType> bts = bundleManager.getAllBundleTypes(overlord);
        assert bts.size() >= 2 : "should have at least 2 bundle types";

        List<String> btNames = new ArrayList<String>();
        for (BundleType bundleType : bts) {
            btNames.add(bundleType.getName());
        }

        assert btNames.contains(bt1.getName());
        assert btNames.contains(bt2.getName());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundle() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleVersion() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testAddBundleFiles() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        assertNotNull(bv1);
        BundleFile bf1 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), TEST_PREFIX + "-bundlefile-1",
            "1.0", null, "Test Bundle File # 1".getBytes(), false);
        BundleFile bf2 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), TEST_PREFIX + "-bundlefile-2",
            "1.0", null, "Test Bundle File # 2".getBytes(), false);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleDeploymentDef() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        Configuration config = new Configuration();
        BundleDeployDefinition bdd1;
        try {
            bdd1 = createDeployDefinition("one", bv1, config);
            fail("Bad config was accepted");
        } catch (Exception e) {
            // expected due to bad config
        }
        config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        bdd1 = createDeployDefinition("one", bv1, config);
        assertNotNull(bdd1);
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeployBundle() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        Configuration config = new Configuration();
        config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
        BundleDeployDefinition bdd1 = createDeployDefinition("one", bv1, config);
        assertNotNull(bdd1);
        Resource platformResource = createTestResource();
        BundleScheduleResponse bsr = bundleManager.scheduleBundleDeployment(overlord, bdd1.getId(), platformResource
            .getId());
        assertNotNull(bsr);
        assertEquals(bdd1.getId(), bsr.getBundleDeployment().getBundleDeployDefinition().getId());
        assertEquals(platformResource.getId(), bsr.getBundleDeployment().getResource().getId());
    }

    @Test(enabled = TESTS_ENABLED)
    public void testGetBundleFilenames() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        assertNotNull(bv1);
        Set<String> filenames = bundleManager.getBundleVersionFilenames(overlord, bv1.getId(), true);
        assertNotNull(filenames);
        assertEquals(2, filenames.size());
        BundleFile bf1 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), TEST_PREFIX + "-bundlefile-1",
            "1.0", null, "Test Bundle File # 1".getBytes(), false);
        filenames = bundleManager.getBundleVersionFilenames(overlord, bv1.getId(), true);
        assertNotNull(filenames);
        assertEquals(1, filenames.size());
        BundleFile bf2 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), TEST_PREFIX + "-bundlefile-2",
            "1.0", null, "Test Bundle File # 2".getBytes(), false);
        filenames = bundleManager.getBundleVersionFilenames(overlord, bv1.getId(), true);
        assertNotNull(filenames);
        assertEquals(0, filenames.size());
    }

    @Test(enabled = TESTS_ENABLED)
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
        c.addFilterName(TEST_PREFIX);
        bundles = bundleManager.findBundlesByCriteria(overlord, c);
        assertNotNull(bundles);
        assertEquals(2, bundles.size());
        b = bundles.get(0);
        name = "one";
        assertNotNull(b);
        assertTrue(b.getBundleType().getName(), b.getName().contains(name));
        assertTrue(b.getBundleType().getName(), b.getBundleType().getName().contains(name));
        try {
            b.getBundleVersions().isEmpty();
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
        bundles = bundleManager.findBundlesByCriteria(overlord, c);
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

    @Test(enabled = TESTS_ENABLED)
    public void testfindBundleVersionsByCriteria() throws Exception {
        Bundle b1 = createBundle("one");
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        BundleVersion bv2 = createBundleVersion(b1.getName(), "2.0", b1);
        BundleVersion bv3 = createBundleVersion(b1.getName(), "2.1", b1);
        BundleVersionCriteria c = new BundleVersionCriteria();
        PageList<BundleVersion> bvs = null;
        BundleVersion bv = null;

        // return all with no optional data
        c.addFilterName(TEST_PREFIX);
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
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
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        bv = bvs.get(0);
        assertEquals(bv2, bv);
        assertEquals(bv.getBundle(), b1);
        assertNull(bv.getDistribution());
        assertNotNull(bv.getBundleDeployDefinitions());
        assertTrue(bv.getBundleDeployDefinitions().isEmpty());
    }

    @Test(enabled = DISABLED)
    public void testInsertAndRetrieve() throws Exception {
        assertNotNull(null);
    }

    @Test(enabled = DISABLED)
    public void testFindByPlatformId() throws Exception {
        assertNotNull(null);
    }

    @Test(enabled = DISABLED)
    public void testFindByBundleId() throws Exception {
        assertNotNull(null);
    }

    @Test(enabled = DISABLED)
    public void testFindByBundleDeploymentId() throws Exception {
        assertNotNull(null);
    }

    @Test(enabled = DISABLED)
    public void testFindHistoryByCriteria() throws Exception {

        Long auditTime = new Date().getTime();
        BundleDeploymentAction auditAction = BundleDeploymentAction.DEPLOYMENT_START;
        String auditMessage = "This is my message";

        BundleDeployment bundleDeployment = createDeployment();
        BundleDeploymentHistory history = new BundleDeploymentHistory(bundleDeployment, overlord.getName(), auditTime,
            auditAction, auditMessage);

        Bundle bundle = createBundle("deleteThisBundle");

        history.setBundleDeployment(bundleDeployment);

        bundleManager.addBundleDeploymentHistoryByBundleDeployment(overlord, history);

        BundleDeploymentHistoryCriteria criteria = new BundleDeploymentHistoryCriteria();
        List<BundleDeploymentHistory> histories = bundleManager.findBundleDeploymentHistoryByCriteria(overlord,
            criteria);

        assertNotNull(histories);
        assertTrue(histories.size() > 0);

    }

    private BundleDeployment createDeployment() {
        Resource resource = new Resource();

        BundleDeployDefinition def = new BundleDeployDefinition();

        return new BundleDeployment(def, resource);
    }

    private BundleType createBundleType(String name) throws Exception {
        final String fullName = TEST_PREFIX + "-type-" + name;
        ResourceType rt = createResourceType(name);
        PackageType pt = createPackageType(name, rt);
        BundleType bt = bundleManager.createBundleType(overlord, fullName, rt.getId());

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
        Bundle b = bundleManager.createBundle(overlord, fullName, bt.getId());

        assert b.getId() > 0;
        assert b.getName().endsWith(fullName);
        return b;
    }

    private BundleVersion createBundleVersion(String name, String version, Bundle bundle) throws Exception {
        final String fullName = TEST_PREFIX + "-bundleversion-" + version + "-" + name;
        final String recipe = "deploy -f " + TEST_PREFIX + ".zip -d <% test.path %>";
        BundleVersion bv = bundleManager.createBundleVersion(overlord, bundle.getId(), fullName, version, recipe);

        assert bv.getId() > 0;
        assert bv.getName().endsWith(fullName);
        return bv;
    }

    private BundleDeployDefinition createDeployDefinition(String name, BundleVersion bv, Configuration config)
        throws Exception {
        final String fullName = TEST_PREFIX + "-bundledeploydef-" + name;
        BundleDeployDefinition bdd = bundleManager.createBundleDeployDefinition(overlord, bv.getId(), fullName,
            fullName, config, false, -1, false);

        assert bdd.getId() > 0;
        assert bdd.getName().endsWith(fullName);
        return bdd;
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

    private PackageType createPackageType(String name, ResourceType rt) throws Exception {
        // the package type is named the same as the bundle type
        final String fullName = TEST_PREFIX + "-type-" + name;
        PackageType pt = new PackageType(fullName, rt);

        TransactionManager txMgr = getTransactionManager();
        txMgr.begin();
        EntityManager em = getEntityManager();
        em.persist(pt);
        em.close();
        txMgr.commit();
        return pt;
    }

    // lifted from ResourceManagerBeanTest
    private Resource createTestResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource = null;

        try {
            // Naming this with TEST_PREFIX allows cleanupDatabase to blow away these test resources along
            // with the bundle resource type
            ResourceType resourceType = new ResourceType(TEST_PREFIX + "-platform-" + System.currentTimeMillis(),
                "test", ResourceCategory.PLATFORM, null);

            em.persist(resourceType);

            Agent agent = new Agent(TEST_PREFIX + "-testagent", "testaddress", 1, "", "testtoken");
            em.persist(agent);
            em.flush();

            resource = new Resource("reskey" + System.currentTimeMillis(), TEST_PREFIX + "-resname", resourceType);
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
            resource.setAgent(agent);
            em.persist(resource);

            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                System.out.println("CANNOT PREPARE TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
                //
            }
        } finally {
            em.close();
        }

        return resource;
    }

    // lifted from ResourceManagerBeanTest
    private void deleteTestResource(Resource resource) throws Exception {
        if (resource != null) {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();
            try {
                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Resource res = em.find(Resource.class, resource.getId());

                List<Integer> deletedIds = resourceManager.deleteResource(overlord, res.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.deleteSingleResourceInNewTransaction(overlord, deletedResourceId);
                }
                em.remove(type);

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                    //
                }
            } finally {
                em.close();
            }
        }
    }

}
