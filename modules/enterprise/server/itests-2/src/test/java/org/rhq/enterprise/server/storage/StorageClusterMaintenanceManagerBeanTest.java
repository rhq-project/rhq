package org.rhq.enterprise.server.storage;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.testng.annotations.Test;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;

/**
 * @author John Sanda
 */
public class StorageClusterMaintenanceManagerBeanTest extends AbstractEJB3Test {

    @EJB
    private StorageClusterMaintenanceManagerLocal maintenanceManager;

    @Override
    protected void beforeMethod() throws Exception {
        resetDB();
    }

    @Override
    protected void afterMethod() throws Exception {
        resetDB();
    }

    @Test
    public void runMaintenanceWhenQueueIsEmpty() {
        maintenanceManager.execute();
    }

    @Test
    public void runMaintenanceWhenQueueHasOneJob() throws Exception {
        final AtomicBoolean step1Executed = new AtomicBoolean(false);
        final AtomicBoolean step2Executed = new AtomicBoolean(false);
        final String step1Name = "FakeStep1";
        final String step2Name = "FakeStep2";

        CalculatorLookup calculatorLookup = new CalculatorLookup() {
            @Override
            public StepCalculator lookup(MaintenanceStep.JobType jobType) {
                return new TestStepCalculator() {
                    @Override
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster) {
                        MaintenanceStep step1 = new MaintenanceStep()
                            .setJobNumber(job.getJobNumber())
                            .setJobType(job.getJobType())
                            .setName(step1Name)
                            .setDescription(step1Name)
                            .setStepNumber(1);
                        MaintenanceStep step2 = new MaintenanceStep()
                            .setJobNumber(job.getJobNumber())
                            .setJobType(job.getJobType())
                            .setName(step2Name)
                            .setDescription(step2Name)
                            .setStepNumber(2);

                        entityManager.persist(step1);
                        entityManager.persist(step2);
                        job.addStep(step1);
                        job.addStep(step2);

                        return job;
                    }
                };
            }
        };

        maintenanceManager.init(calculatorLookup, new TestStepRunnerFactory(
            new FakeStepRunner(step1Executed, step1Name, 1), new FakeStepRunner(step2Executed, step2Name, 2)));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                StorageMaintenanceJob job = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "test deploy",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job);
            }
        }, "Failed to create test deploy job");

        maintenanceManager.execute();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                assertEquals("The job queue should be empty", 0, maintenanceManager.loadQueue().size());
                assertTrue(step1Name + " was not executed", step1Executed.get());
                assertTrue(step2Executed + " was not executed", step2Executed.get());
            }
        }, "Failed to verify whether the test deploy job was run");
    }

    @Test
    public void runMaintenanceWhenQueueHasMultipleJobs() throws Exception {
        final AtomicBoolean job1Step1Executed = new AtomicBoolean();
        final AtomicBoolean job1Step2Executed = new AtomicBoolean();
        final AtomicBoolean job2Step1Executed = new AtomicBoolean();
        final AtomicBoolean job2Step2Executed = new AtomicBoolean();
        final AtomicInteger jobCount = new AtomicInteger(1);

        CalculatorLookup calculatorLookup = new CalculatorLookup() {
            @Override
            public StepCalculator lookup(MaintenanceStep.JobType jobType) {
                return new TestStepCalculator() {
                    @Override
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job, List<StorageNode> cluster) {
                        MaintenanceStep step1 = new MaintenanceStep()
                            .setJobNumber(job.getJobNumber())
                            .setJobType(job.getJobType())
                            .setName("Job " + jobCount.get() + " Step 1")
                            .setDescription("Job " + jobCount.get() + " Step 1")
                            .setStepNumber(1);
                        MaintenanceStep step2 = new MaintenanceStep()
                            .setJobNumber(job.getJobNumber())
                            .setJobType(job.getJobType())
                            .setName("Job " + jobCount.get() + " Step 2")
                            .setDescription("Job " + jobCount.get() + " Step 2")
                            .setStepNumber(2);
                        jobCount.incrementAndGet();
                        entityManager.persist(step1);
                        entityManager.persist(step2);
                        job.addStep(step1);
                        job.addStep(step2);

                        return job;
                    }
                };
            }
        };

        maintenanceManager.init(calculatorLookup, new TestStepRunnerFactory(
            new FakeStepRunner(job1Step1Executed, "Job 1 Step 1", 1),
            new FakeStepRunner(job1Step2Executed, "Job 1 Step 2", 2),
            new FakeStepRunner(job2Step1Executed, "Job 2 Step 1", 1),
            new FakeStepRunner(job2Step2Executed, "Job 2 Step 2", 2)
        ));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                StorageMaintenanceJob job1 = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "test deploy 1",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job1);

                StorageMaintenanceJob job2 = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "test deploy 2",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job2);
            }
        }, "Failed to create test deploy job");

        maintenanceManager.execute();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                assertEquals("The job queue should be empty", 0, maintenanceManager.loadQueue().size());
                assertTrue("Job 1 Step 1 was not executed", job1Step1Executed.get());
                assertTrue("Job 1 Step 2 was not executed", job1Step2Executed.get());
                assertTrue("Job 2 Step 1 was not executed", job2Step1Executed.get());
                assertTrue("Job 2 Step 2 was not executed", job2Step2Executed.get());
            }
        }, "Failed to verify whether or not jobs were run");
    }

    private void executeInTransaction(TransactionCallback callback, String errorMsg) {
        try {
            getTransactionManager().begin();
            callback.execute();
            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                getTransactionManager().rollback();
                org.testng.Assert.fail(errorMsg, e);
            } catch (SystemException e1) {
                org.testng.Assert.fail(errorMsg + " - Failed to rollback transaction", e1);
            }
        } catch (AssertionError e) {
            try {
                getTransactionManager().rollback();
                throw e;
            } catch (SystemException e1) {
                throw new AssertionError("Failed to rollback transaction: " + e1.getMessage(), e);
            }
        }
    }

    private void resetDB() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeTable(MaintenanceStep.class);
                purgeTable(Configuration.class);
            }
        }, "Failed to clean up database");
    }

    private void purgeTable(Class clazz) {
        EntityManager em = getEntityManager();
        em.createQuery("DELETE FROM " + clazz.getSimpleName()).executeUpdate();
    }

}
