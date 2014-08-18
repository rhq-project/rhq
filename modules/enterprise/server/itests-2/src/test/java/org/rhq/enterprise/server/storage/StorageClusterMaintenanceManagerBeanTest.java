package org.rhq.enterprise.server.storage;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertNotEquals;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.storage.MaintenanceStep;
import org.rhq.enterprise.server.storage.maintenance.StorageMaintenanceJob;
import org.rhq.enterprise.server.storage.maintenance.job.StepCalculator;
import org.rhq.enterprise.server.storage.maintenance.step.StepFailureStrategy;
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
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
                        MaintenanceStep step1 = new MaintenanceStep()
                            .setName(step1Name)
                            .setDescription(step1Name);
                        MaintenanceStep step2 = new MaintenanceStep()
                            .setName(step2Name)
                            .setDescription(step2Name);

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
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
                        MaintenanceStep step1 = new MaintenanceStep()
                            .setName("Job " + jobCount.get() + " Step 1")
                            .setDescription("Job " + jobCount.get() + " Step 1");
                        MaintenanceStep step2 = new MaintenanceStep()
                            .setName("Job " + jobCount.get() + " Step 2")
                            .setDescription("Job " + jobCount.get() + " Step 2");
                        jobCount.incrementAndGet();
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
        }, "Failed to create jobs");

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

    @Test
    public void abortJobThatFails() throws Exception {
        final AtomicInteger failedJobNumber = new AtomicInteger();

        CalculatorLookup calculatorLookup = new CalculatorLookup() {
            @Override
            public StepCalculator lookup(MaintenanceStep.JobType jobType) {
                return new TestStepCalculator() {
                    @Override
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
                        List<MaintenanceStep> steps;
                        if (job.getBaseStep().getName().equals("FailedJob")) {
                            steps = asList(
                                new MaintenanceStep()
                                    .setName("FailedJobStep1")
                                    .setDescription("FailedJobStep1"),
                                new MaintenanceStep()
                                    .setName("FailedJobStep2")
                                    .setDescription("FailedJobStep2"),
                                new MaintenanceStep()
                                    .setName("FailedJobStep3")
                                    .setDescription("FailedJobStep3")
                            );
                        } else {
                            steps = asList(
                                new MaintenanceStep()
                                    .setName("SuccessfulJobStep1")
                                    .setDescription("SuccessfulJobStep1"),
                                new MaintenanceStep()
                                    .setName("SuccessfulJobStep2")
                                    .setDescription("SuccessfulJobStep2")
                            );
                        }
                        for (MaintenanceStep step : steps) {
                            job.addStep(step);
                        }
                        return job;
                    }
                };
            }
        };

        maintenanceManager.init(calculatorLookup, new TestStepRunnerFactory(
            new FakeStepRunner("FailedJobStep1", 1),
            new FailedStepRunner("FailedJobStep2", 2, StepFailureStrategy.ABORT),
            new FakeStepRunner("SuccessfulJobStep1", 1),
            new FakeStepRunner("SuccessfulJobStep2", 2)
        ));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                StorageMaintenanceJob job1 = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "FailedJob",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job1);

                failedJobNumber.set(job1.getJobNumber());

                StorageMaintenanceJob job2 = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "SuccessfulJob",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job2);
            }
        }, "Failed to create jobs");

        maintenanceManager.execute();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                List<StorageMaintenanceJob> jobs = maintenanceManager.loadQueue();
                assertEquals("There should only be one job left in the queue", 1, jobs.size());

                StorageMaintenanceJob job = jobs.get(0);
                assertEquals("The job name is wrong", "FailedJob", job.getJobName());
                assertNotEquals(job.getJobNumber(), failedJobNumber.get(), "The job number should be different " +
                        "since the job is added back to the maintenance queue");

                List<MaintenanceStep> steps = job.getSteps();
                assertEquals("Expected the failed job to have two remaining steps", 2, steps.size());

                assertEquals("The step name for the first step is wrong", "FailedJobStep2", steps.get(0).getName());
                assertEquals("The step number for the first step is wrong", 2, steps.get(0).getStepNumber());

                assertEquals("The step name for the second step is wrong", "FailedJobStep3", steps.get(1).getName());
                assertEquals("The step number for the second step is wrong", 3, steps.get(1).getStepNumber());
            }
        }, "Failed to verify whether or not jobs were run");
    }

    @Test
    public void continueJobThatFails() throws Exception {
        final AtomicBoolean failed3Executed = new AtomicBoolean();
        final AtomicBoolean failed2Executed = new AtomicBoolean();
        final AtomicBoolean failed4Executed = new AtomicBoolean();

        CalculatorLookup calculatorLookup = new CalculatorLookup() {
            @Override
            public StepCalculator lookup(MaintenanceStep.JobType jobType) {
                return new TestStepCalculator() {
                    @Override
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob job) {
                        if (job.getJobName().equals("FailedJob")) {
                            List<MaintenanceStep> steps = asList(
                                new MaintenanceStep()
                                    .setName("FailedJobStep1")
                                    .setDescription("FailedJobStep1"),
                                new MaintenanceStep()
                                    .setName("FailedJobStep2")
                                    .setDescription("FailedJobStep2"),
                                new MaintenanceStep()
                                    .setName("FailedJobStep3")
                                    .setDescription("FailedJobStep3"),
                                new MaintenanceStep()
                                    .setName("FailedJobStep4")
                                    .setDescription("FailedJobStep4")
                            );
                            for (MaintenanceStep step : steps) {
                                job.addStep(step);
                            }
//                            stepsCalculated.set(true);
                            return job;
                        }

                        if (job.getJobName().equals("RetryJob")) {
                            job.addStep(new MaintenanceStep()
                                    .setName("FailedJobStep2")
                                    .setDescription("FailedJobStep2"))
                                .addStep(new MaintenanceStep()
                                    .setName("FailedJobStep3")
                                    .setDescription("FailedJobStep3"));
                            return job;
                        }

                        fail(job + " is not a recognized job");
                        return null;
                    }

                    @Override
                    public StorageMaintenanceJob calculateSteps(StorageMaintenanceJob originalJob,
                        MaintenanceStep failedStep) {
                        // Step 2 of the original job failed. We want to create a new job
                        // that consists of steps 2 and 3.
                        Iterator<MaintenanceStep> iterator = originalJob.getSteps().iterator();
                        while (iterator.hasNext()) {
                            MaintenanceStep step = iterator.next();
                            if (step.getName().equals("FailedJobStep2") || step.getName().equals("FailedJobStep3")) {
                                iterator.remove();
                            }
                        }

                        return new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY,
                            "RetryJob", new Configuration());
                    }
                };
            }
        };

        maintenanceManager.init(calculatorLookup, new TestStepRunnerFactory(
            // The job fails at step 2, and a new job should be created that consists of
            // steps 2 and 3; therefore,  step 4 should be executed after step 2 fails.
            // Then after step 4 we should execute steps 2 and 3 as part of the new job.
            new FakeStepRunner("FailedJobStep1", 1),
            new FailedStepRunner("FailedJobStep2", 2, StepFailureStrategy.CONTINUE),
            new FakeStepRunner(failed4Executed, "FailedJobStep4", 4),
            new FakeStepRunner(failed2Executed, "FailedJobStep2", 1),
            new FakeStepRunner(failed3Executed, "FailedJobStep3", 2)
        ));

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                StorageMaintenanceJob job = new StorageMaintenanceJob(MaintenanceStep.JobType.DEPLOY, "FailedJob",
                    new Configuration());
                maintenanceManager.scheduleMaintenance(job);
            }
        }, "Failed to create job");

        maintenanceManager.execute();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                assertTrue("Step FailedJobStep4 should have been executed since the failure strategy for the " +
                        "previous failed step was " + StepFailureStrategy.CONTINUE, failed4Executed.get());

                List<StorageMaintenanceJob> jobs = maintenanceManager.loadQueue();
                assertEquals("There should be one remaining job in the queue", 1, jobs.size());


                StorageMaintenanceJob job = jobs.get(0);
                assertEquals("The job name is wrong", "RetryJob", job.getJobName());

                List<MaintenanceStep> steps = job.getSteps();
                assertTrue("RetryJob should have no steps but found " + steps, steps.isEmpty());
//                assertEquals("The number of steps for RetryJob is wrong", 2, steps.size());
//                assertEquals("The step name for the first step is wrong", "FailedJobStep2", steps.get(0).getName());
//                assertEquals("The step number for the first step is wrong", 1, steps.get(0).getStepNumber());
//
//                assertEquals("The step name for the second step is wrong", "FailedJobStep3", steps.get(1).getName());
//                assertEquals("The step number for the second step is wrong", 2, steps.get(1).getStepNumber());
            }
        }, "Failed to verify whether or not FailedJob was run");

        maintenanceManager.execute();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                assertEquals("The job queue should be empty", 0, maintenanceManager.loadQueue().size());
                assertTrue("FailedJobStep2 should have been executed", failed2Executed.get());
                assertTrue("FailedJobStep2 should have been executed", failed3Executed.get());
            }
        }, "Failed to verify whether or not RetryJob was run");
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
