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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentHistory;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.bundle.BundleGroupDeployment;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleFileCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
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

            // remove bundleversions which cascade remove bundlefiles and bundledeploydefs
            // bundledeploydefs cascade remove bundledeployments and bundlegroupdeployments 
            // bundledeployments cascade remove bundledeploymenthistory            
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
            // remove any orphaned deployment history 
            q = em.createQuery("SELECT bdh FROM BundleDeploymentHistory bdh ");//WHERE bdh.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDeploymentHistory.class, ((BundleDeploymentHistory) removeMe).getId()));
            }
            // remove any orphaned bds
            q = em.createQuery("SELECT bd FROM BundleDeployment bd WHERE bd.bundleDeployDefinition.name LIKE '"
                + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDeployment.class, ((BundleDeployment) removeMe).getId()));
            }
            // remove any orphaned bgds
            q = em.createQuery("SELECT bgd FROM BundleGroupDeployment bgd WHERE bgd.bundleDeployDefinition.name LIKE '"
                + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleGroupDeployment.class, ((BundleGroupDeployment) removeMe).getId()));
            }
            // remove any orphaned bdds
            q = em.createQuery("SELECT bdd FROM BundleDeployDefinition bdd WHERE bdd.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleDeployDefinition.class, ((BundleDeployDefinition) removeMe).getId()));
            }

            // remove bundles which cascade remove packageTypes
            // packagetypes cascade remove packages
            // package cascade remove packageversions            
            q = em.createQuery("SELECT b FROM Bundle b WHERE b.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Bundle.class, ((Bundle) removeMe).getId()));
            }
            em.flush();
            // remove any orphaned pvs
            q = em.createQuery("SELECT pv FROM PackageVersion pv WHERE pv.generalPackage.name LIKE '" + TEST_PREFIX
                + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(PackageVersion.class, ((PackageVersion) removeMe).getId()));
            }
            // remove any oprphaned packages
            q = em.createQuery("SELECT p FROM Package p WHERE p.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Package.class, ((Package) removeMe).getId()));
            }
            // remove any orphaned packagetypes            
            q = em.createQuery("SELECT pt FROM PackageType pt WHERE pt.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(PackageType.class, ((PackageType) removeMe).getId()));
            }

            // remove repos no longer referenced by bundles            
            q = em.createQuery("SELECT r FROM Repo r WHERE r.name LIKE '" + TEST_PREFIX + "%'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(Repo.class, ((Repo) removeMe).getId()));
            }

            // remove ResourceTypes which cascade remove BundleTypes
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
        assert 0 == bv1.getVersionOrder();
        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());
        assert 1 == bv2.getVersionOrder();
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeleteBundle() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());

        // let's add a bundle file so we can ensure our deletion will also delete the file too
        bundleManager.addBundleFileViaByteArray(overlord, bv2.getId(), "testDeleteBundle", "1.0", new Architecture(
            "noarch"), "content".getBytes(), false);
        BundleFileCriteria bfCriteria = new BundleFileCriteria();
        bfCriteria.addFilterBundleVersionId(bv2.getId());
        bfCriteria.fetchPackageVersion(true);
        PageList<BundleFile> files = bundleManager.findBundleFilesByCriteria(overlord, bfCriteria);
        assert files.size() == 1 : files;
        assert files.get(0).getPackageVersion().getGeneralPackage().getName().equals("testDeleteBundle") : files;

        bundleManager.deleteBundle(overlord, b1.getId());

        BundleCriteria bCriteria = new BundleCriteria();
        bCriteria.addFilterId(b1.getId());
        PageList<Bundle> bResults = bundleManager.findBundlesByCriteria(overlord, bCriteria);
        assert bResults.size() == 0;
    }

    @Test(enabled = TESTS_ENABLED)
    public void testDeleteBundleVersion() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", null, b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", null, b1);
        assertNotNull(bv2);
        assertEquals("1.1", bv2.getVersion());

        // let's add a bundle file so we can ensure our deletion will also delete the file too
        bundleManager.addBundleFileViaByteArray(overlord, bv2.getId(), "testDeleteBundleVersion", "1.0",
            new Architecture("noarch"), "content".getBytes(), false);
        BundleFileCriteria bfCriteria = new BundleFileCriteria();
        bfCriteria.addFilterBundleVersionId(bv2.getId());
        bfCriteria.fetchPackageVersion(true);
        PageList<BundleFile> files = bundleManager.findBundleFilesByCriteria(overlord, bfCriteria);
        assert files.size() == 1 : files;
        assert files.get(0).getPackageVersion().getGeneralPackage().getName().equals("testDeleteBundleVersion") : files;

        BundleVersionCriteria bvCriteria = new BundleVersionCriteria();
        BundleCriteria bCriteria = new BundleCriteria();

        // delete the first one - this deletes the BV but the bundle should remain intact
        bundleManager.deleteBundleVersion(overlord, bv2.getId(), true);
        bvCriteria.addFilterId(bv2.getId());
        PageList<BundleVersion> bvResults = bundleManager.findBundleVersionsByCriteria(overlord, bvCriteria);
        assert bvResults.size() == 0;
        bCriteria.addFilterId(b1.getId());
        PageList<Bundle> bResults = bundleManager.findBundlesByCriteria(overlord, bCriteria);
        assert bResults.size() == 1 : "Should not have deleted bundle yet, 1 version still exists";

        // delete the second one - this deletes last BV thus the bundle should also get deleted
        bundleManager.deleteBundleVersion(overlord, bv1.getId(), true);
        bvCriteria.addFilterId(bv1.getId());
        bvResults = bundleManager.findBundleVersionsByCriteria(overlord, bvCriteria);
        assert bvResults.size() == 0;
        bCriteria.addFilterId(b1.getId());
        bResults = bundleManager.findBundlesByCriteria(overlord, bCriteria);
        assert bResults.size() == 0 : "Should have deleted bundle since no versions exists anymore";
    }

    @Test(enabled = TESTS_ENABLED)
    public void testCreateBundleVersionOrdering() throws Exception {
        Bundle b1 = createBundle("one");
        assertNotNull(b1);

        BundleCriteria criteria = new BundleCriteria();
        PageList<BundleWithLatestVersionComposite> results;

        // verify there are no bundle versions yet
        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion() == null;
        assert results.get(0).getVersionsCount().longValue() == 0L;

        BundleVersion bv1 = createBundleVersion(b1.getName() + "-1", "1.0", b1);
        assertNotNull(bv1);
        assertEquals("1.0", bv1.getVersion());
        assert 0 == bv1.getVersionOrder();
        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("1.0");
        assert results.get(0).getVersionsCount().longValue() == 1L;

        BundleVersion bv2 = createBundleVersion(b1.getName() + "-2", "2.0", b1);
        assertNotNull(bv2);
        assertEquals("2.0", bv2.getVersion());
        assert 1 == bv2.getVersionOrder();
        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 2L;

        BundleVersion bv3 = createBundleVersion(b1.getName() + "-3", "1.5", b1);
        assertNotNull(bv3);
        assertEquals("1.5", bv3.getVersion());
        assert 1 == bv3.getVersionOrder();
        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 3L;

        BundleVersionCriteria c = new BundleVersionCriteria();
        PageList<BundleVersion> bvs = null;

        c.addFilterId(bv1.getId()); // 1.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 0; // 1st is the 1.0 version

        c.addFilterId(bv2.getId()); // 2.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 2; // 3rd is the 2.0 version

        c.addFilterId(bv3.getId()); // 1.5
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 1; // 2nd is the 1.5 version

        // see that we can create a really old bundle and versionOrder gets updated properly
        BundleVersion bv4 = createBundleVersion(b1.getName() + "-4", "0.5", b1);
        assertNotNull(bv4);
        assertEquals("0.5", bv4.getVersion());

        c.addFilterId(bv4.getId()); //0.5
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 0; // 1st is the 0.5 version

        c.addFilterId(bv1.getId()); // 1.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 1; // 2nd is the 1.0 version

        c.addFilterId(bv3.getId()); // 1.5
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 2; // 3nd is the 1.5 version

        c.addFilterId(bv2.getId()); // 2.0
        bvs = bundleManager.findBundleVersionsByCriteria(overlord, c);
        assertNotNull(bvs);
        assertEquals(1, bvs.size());
        assert bvs.get(0).getVersionOrder() == 3; // 4th is the 2.0 version

        // verify our composite criteria query can return more than one item
        Bundle b2 = createBundle("two");
        assertNotNull(b2);
        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 4L;
        assert results.get(1).getBundleId().equals(b2.getId());
        assert results.get(1).getBundleName().equals(b2.getName());
        assert results.get(1).getBundleDescription().equals(b2.getDescription());
        assert results.get(1).getLatestVersion() == null;
        assert results.get(1).getVersionsCount().longValue() == 0L;

        BundleVersion b2_bv1 = createBundleVersion(b2.getName() + "-5", "9.1", b2);
        assertNotNull(b2_bv1);
        assertEquals("9.1", b2_bv1.getVersion());

        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 4L;
        assert results.get(1).getBundleId().equals(b2.getId());
        assert results.get(1).getBundleName().equals(b2.getName());
        assert results.get(1).getBundleDescription().equals(b2.getDescription());
        assert results.get(1).getLatestVersion().equals("9.1");
        assert results.get(1).getVersionsCount().longValue() == 1L;

        // test sorting of the BundleWithLastestVersionComposite
        criteria.addSortName(PageOrdering.DESC);
        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(1).getBundleId().equals(b1.getId());
        assert results.get(1).getBundleName().equals(b1.getName());
        assert results.get(1).getBundleDescription().equals(b1.getDescription());
        assert results.get(1).getLatestVersion().equals("2.0");
        assert results.get(1).getVersionsCount().longValue() == 4L;
        assert results.get(0).getBundleId().equals(b2.getId());
        assert results.get(0).getBundleName().equals(b2.getName());
        assert results.get(0).getBundleDescription().equals(b2.getDescription());
        assert results.get(0).getLatestVersion().equals("9.1");
        assert results.get(0).getVersionsCount().longValue() == 1L;

        criteria.addSortName(PageOrdering.ASC);
        results = bundleManager.findBundlesWithLastestVersionCompositesByCriteria(overlord, criteria);
        assert results.size() == 2 : results;
        assert results.get(0).getBundleId().equals(b1.getId());
        assert results.get(0).getBundleName().equals(b1.getName());
        assert results.get(0).getBundleDescription().equals(b1.getDescription());
        assert results.get(0).getLatestVersion().equals("2.0");
        assert results.get(0).getVersionsCount().longValue() == 4L;
        assert results.get(1).getBundleId().equals(b2.getId());
        assert results.get(1).getBundleName().equals(b2.getName());
        assert results.get(1).getBundleDescription().equals(b2.getDescription());
        assert results.get(1).getLatestVersion().equals("9.1");
        assert results.get(1).getVersionsCount().longValue() == 1L;
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
    public void testAddBundleFilesToDifferentBundles() throws Exception {
        // create a bundle type to use for both bundles.
        BundleType bt = createBundleType("one");
        Bundle b1 = createBundle("one", bt);
        assertNotNull(b1);
        BundleVersion bv1 = createBundleVersion(b1.getName(), "1.0", b1);
        assertNotNull(bv1);
        BundleFile b1f1 = bundleManager.addBundleFileViaByteArray(overlord, bv1.getId(), TEST_PREFIX + "-file1", "1.0",
            null, "Bundle #1 File # 1".getBytes(), false);

        // create a second bundle but create file of the same name as above
        Bundle b2 = createBundle("two", bt);
        assertNotNull(b2);
        BundleVersion bv2 = createBundleVersion(b2.getName(), "1.0", b2);
        assertNotNull(bv2);
        BundleFile b2f1 = bundleManager.addBundleFileViaByteArray(overlord, bv2.getId(), TEST_PREFIX + "-file1", "1.0",
            null, "Bundle #2 File # 1".getBytes(), false);

        BundleFileCriteria bfc = new BundleFileCriteria();
        bfc.addFilterBundleVersionId(bv1.getId());
        PageList<BundleFile> bundleFiles = bundleManager.findBundleFilesByCriteria(overlord, bfc);
        assert bundleFiles.size() == 1 : bundleFiles;
        assert bundleFiles.get(0).getId() == b1f1.getId() : bundleFiles;

        bfc = new BundleFileCriteria();
        bfc.addFilterBundleVersionId(bv2.getId());
        bundleFiles = bundleManager.findBundleFilesByCriteria(overlord, bfc);
        assert bundleFiles.size() == 1 : bundleFiles;
        assert bundleFiles.get(0).getId() == b2f1.getId() : bundleFiles;

        assert b1f1.getId() != b2f1.getId() : "should have been different bundle files";
        assert b1f1.getPackageVersion().getId() != b2f1.getPackageVersion().getId() : "should be different PV";
        assert b1f1.getPackageVersion().getGeneralPackage().getId() != b2f1.getPackageVersion().getGeneralPackage()
            .getId() : "package IDs should be different";
        assert !b1f1.getPackageVersion().getGeneralPackage().equals(b2f1.getPackageVersion().getGeneralPackage()) : "should be different packages";
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
        BundleDeployment bd = bundleManager.scheduleBundleDeployment(overlord, bdd1.getId(), platformResource.getId());
        assertNotNull(bd);
        assertEquals(bdd1.getId(), bd.getBundleDeployDefinition().getId());
        assertEquals(platformResource.getId(), bd.getResource().getId());
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

    private BundleDeployment createDeployment() {
        Resource resource = new Resource();

        BundleDeployDefinition def = new BundleDeployDefinition();

        return new BundleDeployment(def, resource);
    }

    private BundleType createBundleType(String name) throws Exception {
        final String fullName = TEST_PREFIX + "-type-" + name;
        ResourceType rt = createResourceType(name);
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
        Bundle b = bundleManager.createBundle(overlord, fullName, fullName + "-desc", bt.getId());

        assert b.getId() > 0;
        assert b.getName().endsWith(fullName);
        return b;
    }

    private BundleVersion createBundleVersion(String name, String version, Bundle bundle) throws Exception {
        final String fullName = TEST_PREFIX + "-bundleversion-" + version + "-" + name;
        final String recipe = "deploy -f " + TEST_PREFIX + ".zip -d <% test.path %>";
        BundleVersion bv = bundleManager.createBundleVersion(overlord, bundle.getId(), fullName, fullName + "-desc",
            version, recipe);

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
            resource.setUuid("" + System.currentTimeMillis());
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
