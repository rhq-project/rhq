package org.rhq.core.domain.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

/**
 * @author John Sanda
 */
public class MaintenanceJobTest extends AbstractEJB3Test {

    private static Log log = LogFactory.getLog(MaintenanceJobTest.class);

    @Test(groups = "integration.ejb3")
    public void createJobWithNoSteps() throws Exception {
        resetDB();

        Agent agent = null;
        ResourceType resourceType = null;
        Resource resource = null;
        StorageNode storageNode = null;
        MaintenanceJob job = null;

        try {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();

            agent = new Agent("test-agent", "localhost", 1234, "", "test-token");
            em.persist(agent);

            resourceType = new ResourceType("Test Type", "test-plugin", ResourceCategory.PLATFORM, null);
            em.persist(resourceType);

            resource = new Resource("1", "StorageNode-1", resourceType);
            resource.setUuid(UUID.randomUUID().toString());
            resource.setAgent(agent);
            em.persist(resource);

            storageNode = new StorageNode();
            storageNode.setAddress("127.0.0.1");
            storageNode.setCqlPort(9142);
            storageNode.setResource(resource);
            storageNode.setOperationMode(StorageNode.OperationMode.NORMAL);
            em.persist(storageNode);

            job = new MaintenanceJob();
            job.setType(MaintenanceJob.Type.DEPLOY);
            job.setName("Deploy storage node " + storageNode.getAddress());
            job.setStorageNode(storageNode);
            em.persist(job);

            getTransactionManager().commit();
        } catch (Exception e) {
            getTransactionManager().rollback();
            org.testng.Assert.fail("Failed to persist entities", e);
        }

        resetDB();
    }

    @Test(groups = "integration.ejb3")
    public void addStepsToJob() throws Exception {
        resetDB();

        Agent agent = null;
        ResourceType resourceType = null;
        Resource resource = null;
        StorageNode storageNode = null;
        MaintenanceJob job = null;

        try {
            getTransactionManager().begin();
            EntityManager em = getEntityManager();

            agent = new Agent("test-agent", "localhost", 1234, "", "test-token");
            em.persist(agent);

            resourceType = new ResourceType("Test Type", "test-plugin", ResourceCategory.PLATFORM, null);
            em.persist(resourceType);

            resource = new Resource("1", "StorageNode-1", resourceType);
            resource.setUuid(UUID.randomUUID().toString());
            resource.setAgent(agent);
            em.persist(resource);

            storageNode = new StorageNode();
            storageNode.setAddress("127.0.0.1");
            storageNode.setCqlPort(9142);
            storageNode.setResource(resource);
            storageNode.setOperationMode(StorageNode.OperationMode.NORMAL);
            em.persist(storageNode);

            job = new MaintenanceJob();
            job.setType(MaintenanceJob.Type.DEPLOY);
            job.setName("Deploy storage node " + storageNode.getAddress());
            job.setStorageNode(storageNode);
            em.persist(job);

            getTransactionManager().commit();
        } catch (Exception e) {
            getTransactionManager().rollback();
            org.testng.Assert.fail("Failed to persist job", e);
        }

        MaintenanceStep step1 = null;
        MaintenanceStep step2 = null;

        try {
            getTransactionManager().begin();
            em = getEntityManager();

            job = em.find(MaintenanceJob.class, job.getId());

            step1 = new MaintenanceStep()
                .setStep(0)
                .setStorageNode(storageNode)
                .setName("Step 0")
                .setArgs("test args 1")
                .setMaintenanceJob(job);
            step2 = new MaintenanceStep()
                .setStep(1)
                .setName("Step 1")
                .setStorageNode(storageNode)
                .setArgs("test args 2")
                .setMaintenanceJob(job);
            List<MaintenanceStep> steps = new ArrayList<MaintenanceStep>();
            steps.add(step1);
            steps.add(step2);
            job.setSteps(steps);
            em.merge(job);

            getTransactionManager().commit();

            getTransactionManager().begin();
            em = getEntityManager();

            job = em.find(MaintenanceJob.class, job.getId());
            assertEquals("Expected the job to have two steps", 2, job.getMaintenanceSteps().size());
            assertEquals("Expected step 0 to be the first step", 0, job.getMaintenanceSteps().get(0).getStep());
            assertEquals("Expected step 1 to be the second step", 1, job.getMaintenanceSteps().get(1).getStep());

            getTransactionManager().commit();
        } catch (Exception e) {
            getTransactionManager().rollback();
            org.testng.Assert.fail("Failed to add steps to job", e);
        } finally {
            resetDB();
        }
    }

    private void resetDB() throws Exception {
        try {
            getTransactionManager().begin();
            purgeTable(MaintenanceStep.class);
            purgeTable(MaintenanceJob.class);
            purgeTable(StorageNode.class);
            purgeTable(Availability.class);
            purgeTable(Resource.class);
            purgeTable(ResourceType.class);
            purgeTable(Agent.class);
            getTransactionManager().commit();
        } catch (Exception e) {
            log.warn("Failed to clean up database");
            getTransactionManager().rollback();
            org.testng.Assert.fail("Failed to delete entities", e);
        }
    }

    private void purgeTable(Class clazz) {
        EntityManager em = getEntityManager();
        em.createQuery("DELETE FROM " + clazz.getSimpleName()).executeUpdate();
    }

}
