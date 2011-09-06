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
package org.rhq.enterprise.server.drift;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.JPADriftChangeSetCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfiguration.BaseDirectory;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;

/**
 * Test for {@link DriftManagerBean} SLSB.
 * 
 * !!! Actually, this is really testing only the JPA impl, it does not actually go through the
 * !!! configured drift server plugin.  To enhance this to do that then you may need to model this
 * !!! mode like BundleManagerBeanTest 
 */
@Test(groups = "drift-manager")
public class DriftManagerBeanTest extends AbstractEJB3Test {

    private static final boolean ENABLE_TESTS = true;

    private JPADriftServerLocal jpaDriftServer;
    private DriftManagerLocal driftManager;
    private Subject overlord;
    private Resource newResource;
    @SuppressWarnings("unused")
    private DriftServerService driftServerService;

    MessageDigestGenerator digestGenerator;

    /**
     * Prepares things for the entire test class.
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        jpaDriftServer = LookupUtil.getJPADriftServer();
        driftManager = LookupUtil.getDriftManager();
        overlord = LookupUtil.getSubjectManager().getOverlord();

        driftServerService = new DriftServerServiceImpl();

        TestServerCommunicationsService agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.driftService = new TestConfigService();
    }

    @AfterClass
    public void afterClass() {
        driftServerService = null;
        unprepareForTestAgents();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        prepareScheduler();

        deleteDriftFiles();

        newResource = createNewResource();
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        try {
            deleteNewResource(newResource);
        } finally {
            unprepareScheduler();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testStoreChangeSet() throws Exception {
        File rootDir = toFile(getClass().getResource("."));
        deleteDirectory(rootDir);

        File changeSetsDir = new File(rootDir, "changesets");
        changeSetsDir.mkdirs();

        Headers headers = new Headers();
        headers.setResourceId(newResource.getId());
        headers.setDriftCofigurationId(1);
        headers.setDriftConfigurationName("test-1");
        headers.setBasedir(rootDir.getAbsolutePath());
        headers.setType(COVERAGE);

        String file1Hash = sha256("test-1-file-1");

        File changeSet1 = new File(changeSetsDir, "changeset-1.txt");
        ChangeSetWriter writer = new ChangeSetWriterImpl(changeSet1, headers);
        writer.write(addedFileEntry("test/file-1", file1Hash));
        writer.close();

        File changeSet1Zip = new File(changeSetsDir, "changeset-1.zip");
        ZipUtil.zipFileOrDirectory(changeSet1, changeSet1Zip);

        assertTrue("Expected to find change set zip file: " + changeSet1Zip.getPath(), changeSet1Zip.exists());

        jpaDriftServer.storeChangeSet(overlord, newResource.getId(), changeSet1Zip);

        JPADriftChangeSetCriteria c = new JPADriftChangeSetCriteria();
        c.addFilterResourceId(newResource.getId());
        c.fetchDrifts(true);
        List<? extends DriftChangeSet<?>> changeSets = jpaDriftServer.findDriftChangeSetsByCriteria(overlord, c);
        assertEquals(1, changeSets.size());
        DriftChangeSet<?> changeSet = changeSets.get(0);
        assertEquals(0, changeSet.getVersion());
        assertEquals("Expected to find one entry in change set", 1, changeSet.getDrifts().size());

        DriftFile driftFile = jpaDriftServer.getDriftFile(overlord, file1Hash);
        assertNotNull(driftFile);
        assertEquals(DriftFileStatus.REQUESTED, driftFile.getStatus());

        // the second change set should report drift
        String modifiedFile1Hash = sha256("test-2-file-1-modified");
        headers.setType(DRIFT);
        File changeSet2 = new File(changeSetsDir, "changeset-2.txt");

        writer = new ChangeSetWriterImpl(changeSet2, headers);
        writer.write(changedFileEntry("test/file-1", file1Hash, modifiedFile1Hash));
        writer.close();

        File changeSet2Zip = new File(changeSetsDir, "changeset-2.zip");
        ZipUtil.zipFileOrDirectory(changeSet2, changeSet2Zip);

        assertTrue("Expected to find change set file: " + changeSet2Zip.getPath(), changeSet2Zip.exists());

        jpaDriftServer.storeChangeSet(overlord, newResource.getId(), changeSet2Zip);
        c.addSortVersion(PageOrdering.ASC);
        c.addFilterCategory(DRIFT);
        changeSets = jpaDriftServer.findDriftChangeSetsByCriteria(overlord, c);
        assertEquals(1, changeSets.size());
        changeSet = changeSets.get(0);
        assertEquals("The change set version is wrong", 1, changeSet.getVersion());
        assertEquals("Expected to find one entry in change set", 1, changeSet.getDrifts().size());
        changeSet = changeSets.get(0);
        assertEquals(1, changeSet.getVersion());
        assertEquals(1, changeSet.getDrifts().size());
        Drift<?, ?> drift = changeSet.getDrifts().iterator().next();
        assertEquals("test/file-1", drift.getPath());
        assertEquals(file1Hash, drift.getOldDriftFile().getHashId());
        assertEquals(modifiedFile1Hash, drift.getNewDriftFile().getHashId());
        assertEquals(DriftCategory.FILE_CHANGED, drift.getCategory());

        driftFile = jpaDriftServer.getDriftFile(overlord, modifiedFile1Hash);
        assertNotNull(driftFile);
        assertEquals(DriftFileStatus.REQUESTED, driftFile.getStatus());
    }

    @Test(enabled = ENABLE_TESTS)
    public void testDriftConfig() throws Exception {
        Configuration config = new Configuration();
        DriftConfiguration driftConfigPojo = new DriftConfiguration(config);
        driftConfigPojo.setName("testDriftConfig");
        driftConfigPojo.setInterval(60L);
        driftConfigPojo.setBasedir(new BaseDirectory(BaseDirValueContext.fileSystem, "foo/bar"));

        driftManager
            .updateDriftConfiguration(overlord, EntityContext.forResource(newResource.getId()), driftConfigPojo);

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        ResourceCriteria c = new ResourceCriteria();
        c.addFilterId(newResource.getId());
        c.fetchDriftConfigurations(true);
        List<Resource> resources = resourceManager.findResourcesByCriteria(overlord, c);
        assertEquals(1, resources.size());
        Set<DriftConfiguration> driftConfigs = resources.get(0).getDriftConfigurations();
        assertNotNull(driftConfigs);
        assertEquals(3, driftConfigs.size());
        DriftConfiguration driftConfig = null;
        for (Iterator<DriftConfiguration> i = driftConfigs.iterator(); i.hasNext();) {
            driftConfig = i.next();
            if (driftConfigPojo.getName().equals(driftConfig.getName()))
                break;
        }
        assertTrue(driftConfig.getConfiguration().getId() > 0); // persisted
        assertEquals(driftConfigPojo.getName(), driftConfig.getName());
        assertEquals(driftConfigPojo.getBasedir(), driftConfig.getBasedir());
        assertEquals(driftConfigPojo.getInterval(), driftConfig.getInterval());

        driftConfigPojo.setInterval(120L);
        driftManager
            .updateDriftConfiguration(overlord, EntityContext.forResource(newResource.getId()), driftConfigPojo);

        resources = resourceManager.findResourcesByCriteria(overlord, c);
        assertEquals(1, resources.size());
        driftConfigs = resources.get(0).getDriftConfigurations();
        assertNotNull(driftConfigs);
        assertEquals(3, driftConfigs.size());
        driftConfig = null;
        for (Iterator<DriftConfiguration> i = driftConfigs.iterator(); i.hasNext();) {
            driftConfig = i.next();
            if (driftConfigPojo.getName().equals(driftConfig.getName()))
                break;
        }
        assertEquals(driftConfigPojo.getName(), driftConfig.getName());
        assertTrue(driftConfig.getConfiguration().getId() > 0); // persisted
        assertEquals(driftConfigPojo.getBasedir(), driftConfig.getBasedir());
        assertEquals(120L, driftConfig.getInterval());

        driftConfigPojo.setName("testDriftConfig-2");
        driftConfigPojo.setInterval(30L);
        driftConfigPojo.setBasedir(new BaseDirectory(BaseDirValueContext.fileSystem, "foo/baz"));

        driftManager
            .updateDriftConfiguration(overlord, EntityContext.forResource(newResource.getId()), driftConfigPojo);

        resources = resourceManager.findResourcesByCriteria(overlord, c);
        assertEquals(1, resources.size());
        driftConfigs = resources.get(0).getDriftConfigurations();
        assertNotNull(driftConfigs);
        assertEquals(4, driftConfigs.size());
        for (Iterator<DriftConfiguration> i = driftConfigs.iterator(); i.hasNext();) {
            driftConfig = i.next();
            if ("testDriftConfig".equals(driftConfig.getName())) {
                assertTrue(driftConfig.getConfiguration().getId() > 0); // persisted
                assertEquals("foo/bar", driftConfig.getBasedir().getValueName());
                assertEquals(BaseDirValueContext.fileSystem, driftConfig.getBasedir().getValueContext());
                assertEquals(120L, driftConfig.getInterval());
            } else if ("testDriftConfig-2".equals(driftConfig.getName())) {
                assertTrue(driftConfig.getConfiguration().getId() > 0); // persisted
                assertEquals(driftConfigPojo.getBasedir(), driftConfig.getBasedir());
                assertEquals(driftConfigPojo.getInterval(), driftConfig.getInterval());
            } else if (!"test-1".equals(driftConfig.getName()) && !"test-2".equals(driftConfig.getName())) {
                fail("Unexpected drift config name: " + driftConfig.getName());
            }
        }

        driftManager.deleteDriftConfiguration(overlord, EntityContext.forResource(newResource.getId()),
            "testDriftConfig");
        resources = resourceManager.findResourcesByCriteria(overlord, c);
        assertEquals(1, resources.size());
        driftConfigs = resources.get(0).getDriftConfigurations();
        assertNotNull(driftConfigs);
        assertEquals(3, driftConfigs.size());
        for (Iterator<DriftConfiguration> i = driftConfigs.iterator(); i.hasNext();) {
            driftConfig = i.next();
            if (driftConfigPojo.getName().equals(driftConfig.getName()))
                break;
        }
        assertTrue(driftConfig.getConfiguration().getId() > 0); // persisted
        assertEquals(driftConfigPojo.getName(), driftConfig.getName());
        assertEquals(driftConfigPojo.getBasedir(), driftConfig.getBasedir());
        assertEquals(driftConfigPojo.getInterval(), driftConfig.getInterval());
    }

    private void deleteDriftFiles() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            try {
                // wipe out any test DriftFiles (the test files have sha256 0,1,...)
                for (int i = 0, numDeleted = 1; (numDeleted > 0); ++i) {
                    numDeleted = getEntityManager().createQuery("delete from JPADriftFile where hash_id = '" + i + "'")
                        .executeUpdate();
                }
            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            em.flush();
            getTransactionManager().commit();
        } finally {
            em.close();
        }
    }

    private Resource createNewResource() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        Resource resource;

        try {
            try {
                ResourceType resourceType = new ResourceType("plat" + System.currentTimeMillis(), "test",
                    ResourceCategory.PLATFORM, null);

                em.persist(resourceType);

                Agent agent = new Agent("testagent", "testaddress", 1, "", "testtoken");
                em.persist(agent);
                em.flush();

                DriftConfiguration test1Config = new DriftConfiguration(new Configuration());
                test1Config.setName("test-1");

                DriftConfiguration test2Config = new DriftConfiguration(new Configuration());
                test2Config.setName("test-2");

                resource = new Resource("reskey" + System.currentTimeMillis(), "resname", resourceType);
                resource.setUuid("" + new Random().nextInt());
                resource.setAgent(agent);
                resource.setInventoryStatus(InventoryStatus.COMMITTED);
                resource.addDriftConfiguration(test1Config);
                resource.addDriftConfiguration(test2Config);
                em.persist(resource);

            } catch (Exception e) {
                System.out.println("CANNOT PREPARE TEST: " + e);
                getTransactionManager().rollback();
                throw e;
            }

            em.flush();
            getTransactionManager().commit();
        } finally {
            em.close();
        }

        return resource;
    }

    private void deleteNewResource(Resource resource) throws Exception {
        if (null != resource) {
            EntityManager em = null;

            try {
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

                // invoke bulk delete on the resource to remove any dependencies not defined in the hibernate entity model
                // perform in-band and out-of-band work in quick succession
                List<Integer> deletedIds = resourceManager.uninventoryResource(overlord, resource.getId());
                for (Integer deletedResourceId : deletedIds) {
                    resourceManager.uninventoryResourceAsyncWork(overlord, deletedResourceId);
                }

                // now dispose of other hibernate entities
                getTransactionManager().begin();
                em = getEntityManager();

                ResourceType type = em.find(ResourceType.class, resource.getResourceType().getId());
                Agent agent = em.find(Agent.class, resource.getAgent().getId());
                if (null != agent) {
                    em.remove(agent);
                }
                if (null != type) {
                    em.remove(type);
                }

                getTransactionManager().commit();
            } catch (Exception e) {
                try {
                    System.out.println("CANNOT CLEAN UP TEST (" + this.getClass().getSimpleName() + ") Cause: " + e);
                    getTransactionManager().rollback();
                } catch (Exception ignore) {
                }
            } finally {
                if (null != em) {
                    em.close();
                }
            }
        }
    }

    String sha256(String s) throws Exception {
        return digestGenerator.calcDigestString(s);
    }

    private class TestConfigService implements DriftAgentService {

        @Override
        public boolean requestDriftFiles(int resourceId, Headers headers, List<? extends DriftFile> driftFiles) {
            return true;
        }

        @Override
        public void scheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {

        }

        @Override
        public void detectDrift(int resourceId, DriftConfiguration driftConfiguration) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void unscheduleDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        }

        @Override
        public void updateDriftDetection(int resourceId, DriftConfiguration driftConfiguration) {
        }
    }
}